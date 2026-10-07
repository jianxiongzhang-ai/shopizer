package com.salesmanager.test.business.search;

import com.salesmanager.core.business.services.search.SearchServiceImpl;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.*;

public class SearchServiceImplTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * This methods tests {@link SearchServiceImpl#loadClassPathResource(String)}
     * for a resource on the file system classpath
     */
    @Test
    public void testLoadClassPathResource_FromFileSystem() throws Exception {
        String result = new SearchServiceImpl().loadClassPathResource("search/MAPPINGS.json");
        assertTrue(result.contains("\"properties\""));
    }

    /**
     * This methods tests {@link SearchServiceImpl#loadClassPathResource(String)}
     * for a resource packaged inside a jar, as when running the executable shopizer.jar
     */
    @Test
    public void testLoadClassPathResource_FromJar() throws Exception {
        String content = "{\"properties\":{\"name\":{\"type\":\"text\"}}}";
        File jar = temporaryFolder.newFile("search-resources.jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jar))) {
            out.putNextEntry(new JarEntry("search/JAR_ONLY_MAPPINGS.json"));
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader jarClassLoader = new URLClassLoader(new URL[] { jar.toURI().toURL() }, original)) {
            Thread.currentThread().setContextClassLoader(jarClassLoader);
            String result = new SearchServiceImpl().loadClassPathResource("search/JAR_ONLY_MAPPINGS.json");
            assertEquals(content, result);
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
