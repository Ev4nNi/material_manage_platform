package com.material.platform.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceImplTest {

    @TempDir
    Path tempDir;

    private LocalStorageServiceImpl storageService;

    @BeforeEach
    void setUp() throws Exception {
        storageService = new LocalStorageServiceImpl();
        java.lang.reflect.Field basePathField = LocalStorageServiceImpl.class.getDeclaredField("basePath");
        basePathField.setAccessible(true);
        basePathField.set(storageService, tempDir.toString());
    }

    @Test
    void testUploadCreatesDateBasedPath() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            "test content".getBytes()
        );

        String storageKey = "2026/04/24/test-uuid.jpg";
        String result = storageService.upload(file, storageKey);
        assertNotNull(result);
        assertTrue(result.contains("2026/04/24"));
        assertTrue(result.endsWith(".jpg"));
    }

    @Test
    void testDeleteRemovesFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "delete.jpg",
            "image/jpeg",
            "content".getBytes()
        );

        String storageKey = "2026/04/24/delete.jpg";
        storageService.upload(file, storageKey);
        storageService.delete(storageKey);

        Path filePath = tempDir.resolve(storageKey);
        assertFalse(filePath.toFile().exists());
    }

    @Test
    void testGetBaseUrl() {
        String baseUrl = storageService.getBaseUrl();
        assertNotNull(baseUrl);
        assertEquals(tempDir.toString(), baseUrl);
    }

    @Test
    void testStorageKeyFormat() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "video.mp4",
            "video/mp4",
            "video content".getBytes()
        );

        String storageKey = "2026/04/24/abc123.mp4";
        String result = storageService.upload(file, storageKey);
        assertTrue(result.matches("\\d{4}/\\d{2}/\\d{2}/[a-f0-9-]+\\.mp4"));
    }
}
