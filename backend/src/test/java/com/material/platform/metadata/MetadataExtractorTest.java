package com.material.platform.metadata;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
    void testImageMetadataExtractionReadsOnlyPngHeader() {
        ImageMetadataExtractor extractor = new ImageMetadataExtractor();

        Map<String, Object> metadata = extractor.extract(
                new HeaderOnlyPngStream(800, 600, 64),
                "header-only.png"
        );

        assertEquals(800, metadata.get("width"));
        assertEquals(600, metadata.get("height"));
        assertEquals("png", metadata.get("format"));
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

    private static final class HeaderOnlyPngStream extends InputStream {

        private final byte[] header;
        private final int allowedBytes;
        private int position;

        private HeaderOnlyPngStream(int width, int height, int allowedBytes) {
            this.header = createPngHeader(width, height);
            this.allowedBytes = allowedBytes;
        }

        @Override
        public int read() throws IOException {
            if (position >= allowedBytes) {
                throw new IOException("Read exceeded header limit");
            }
            if (position >= header.length) {
                position++;
                return 0;
            }
            return header[position++] & 0xFF;
        }
    }

    private static byte[] createPngHeader(int width, int height) {
        byte[] pngHeader = new byte[32];
        pngHeader[0] = (byte) 0x89;
        pngHeader[1] = 0x50;
        pngHeader[2] = 0x4E;
        pngHeader[3] = 0x47;
        pngHeader[4] = 0x0D;
        pngHeader[5] = 0x0A;
        pngHeader[6] = 0x1A;
        pngHeader[7] = 0x0A;
        pngHeader[12] = 0x49;
        pngHeader[13] = 0x48;
        pngHeader[14] = 0x44;
        pngHeader[15] = 0x52;
        writeIntBigEndian(pngHeader, 16, width);
        writeIntBigEndian(pngHeader, 20, height);
        return pngHeader;
    }

    private static void writeIntBigEndian(byte[] target, int offset, int value) {
        target[offset] = (byte) ((value >>> 24) & 0xFF);
        target[offset + 1] = (byte) ((value >>> 16) & 0xFF);
        target[offset + 2] = (byte) ((value >>> 8) & 0xFF);
        target[offset + 3] = (byte) (value & 0xFF);
    }
}
