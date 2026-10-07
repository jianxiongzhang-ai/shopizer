package com.salesmanager.test.shop.controller;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;

import com.salesmanager.core.business.services.catalog.product.image.ProductImageService;
import com.salesmanager.shop.controller.ImagesController;

public class ImagesControllerTest {

    private static final String NOT_FOUND_IMAGE = "static/not-found.png";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * A missing product image is served as the not-found placeholder
     * when the placeholder is on the file system classpath
     */
    @Test
    public void testMissingProductImage_ServesPlaceholder_FromFileSystem() throws Exception {
        byte[] expected;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(NOT_FOUND_IMAGE)) {
            expected = StreamUtils.copyToByteArray(in);
        }

        ImagesController controller = newController();
        controller.init();

        byte[] result = controller.printImage("DEFAULT", "SKU1", "SMALL", "missing", "png", mock(HttpServletRequest.class));
        assertArrayEquals(expected, result);
    }

    /**
     * A missing product image is served as the not-found placeholder
     * when the placeholder is packaged inside a jar, as when running the executable shopizer.jar
     */
    @Test
    public void testMissingProductImage_ServesPlaceholder_FromJar() throws Exception {
        byte[] expected = new byte[] { (byte) 0x89, 'P', 'N', 'G', 1, 2, 3 };
        File jar = temporaryFolder.newFile("static-resources.jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            out.putNextEntry(new JarEntry(NOT_FOUND_IMAGE));
            out.write(expected);
            out.closeEntry();
        }

        ImagesController controller = newController();
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        // no parent, so the placeholder can only be found inside the jar
        try (URLClassLoader jarClassLoader = new URLClassLoader(new URL[] { jar.toURI().toURL() }, null)) {
            Thread.currentThread().setContextClassLoader(jarClassLoader);
            controller.init();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }

        byte[] result = controller.printImage("DEFAULT", "SKU1", "SMALL", "missing", "png", mock(HttpServletRequest.class));
        assertArrayEquals(expected, result);
    }

    private ImagesController newController() {
        ImagesController controller = new ImagesController();
        // an unstubbed mock returns null, i.e. the product image does not exist
        ReflectionTestUtils.setField(controller, "productImageService", mock(ProductImageService.class));
        return controller;
    }
}
