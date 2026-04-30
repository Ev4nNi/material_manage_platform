package com.material.platform.metadata;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MetadataExtractorTest {

    @Test
    void testImageExtractorForJpg() {
        MetadataExtractorFactory factory = new MetadataExtractorFactory();
        MetadataExtractor extractor = factory.getExtractor("test.jpg");
        assertNotNull(extractor);
        assertTrue(extractor instanceof ImageMetadataExtractor);
    }

    @Test
    void testImageExtractorForPng() {
        MetadataExtractorFactory factory = new MetadataExtractorFactory();
        MetadataExtractor extractor = factory.getExtractor("test.png");
        assertNotNull(extractor);
        assertTrue(extractor instanceof ImageMetadataExtractor);
    }

    @Test
    void testImageExtractorForGif() {
        MetadataExtractorFactory factory = new MetadataExtractorFactory();
        MetadataExtractor extractor = factory.getExtractor("test.gif");
        assertNotNull(extractor);
        assertTrue(extractor instanceof ImageMetadataExtractor);
    }

    @Test
    void testImageExtractorForWebp() {
        MetadataExtractorFactory factory = new MetadataExtractorFactory();
        MetadataExtractor extractor = factory.getExtractor("test.webp");
        assertNotNull(extractor);
        assertTrue(extractor instanceof ImageMetadataExtractor);
    }

    @Test
    void testVideoExtractorForMp4() {
        MetadataExtractorFactory factory = new MetadataExtractorFactory();
        MetadataExtractor extractor = factory.getExtractor("video.mp4");
        assertNotNull(extractor);
        assertTrue(extractor instanceof VideoMetadataExtractor);
    }

    @Test
    void testVideoExtractorForMov() {
        MetadataExtractorFactory factory = new MetadataExtractorFactory();
        MetadataExtractor extractor = factory.getExtractor("video.mov");
        assertNotNull(extractor);
        assertTrue(extractor instanceof VideoMetadataExtractor);
    }

    @Test
    void testUnknownExtractorReturnsNull() {
        MetadataExtractorFactory factory = new MetadataExtractorFactory();
        MetadataExtractor extractor = factory.getExtractor("unknown.xyz");
        assertNull(extractor);
    }

    @Test
    void testNullFileNameReturnsNull() {
        MetadataExtractorFactory factory = new MetadataExtractorFactory();
        MetadataExtractor extractor = factory.getExtractor(null);
        assertNull(extractor);
    }

    @Test
    void testEmptyFileNameReturnsNull() {
        MetadataExtractorFactory factory = new MetadataExtractorFactory();
        MetadataExtractor extractor = factory.getExtractor("");
        assertNull(extractor);
    }

    @Test
    void testImageMetadataExtractionWithFakeData() {
        ImageMetadataExtractor extractor = new ImageMetadataExtractor();
        try {
            InputStream is = new ByteArrayInputStream("not an image".getBytes());
            Map<String, Object> metadata = extractor.extract(is, "fake.png");
            assertNotNull(metadata);
            assertTrue(metadata.isEmpty());
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    void testImageMetadataExtractorIsNotNull() {
        ImageMetadataExtractor extractor = new ImageMetadataExtractor();
        assertNotNull(extractor);
    }

    @Test
    void testVideoMetadataExtractorIsNotNull() {
        VideoMetadataExtractor extractor = new VideoMetadataExtractor();
        assertNotNull(extractor);
    }

    @Test
    void testVideoMetadataExtractorWithUnsupportedFormat() {
        VideoMetadataExtractor extractor = new VideoMetadataExtractor();
        try {
            InputStream is = new ByteArrayInputStream("not a video".getBytes());
            Map<String, Object> metadata = extractor.extract(is, "test.avi");
            assertNotNull(metadata);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }
}
