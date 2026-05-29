package com.material.platform.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class ImageMetadataExtractor implements MetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(ImageMetadataExtractor.class);
    private static final int HEADER_READ_LIMIT = 64;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Override
    public Map<String, Object> extract(InputStream inputStream, String fileName) {
        Map<String, Object> metadata = new HashMap<>();
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
            bufferedInputStream.mark(HEADER_READ_LIMIT);
            metadata = extractFromBinaryHeader(bufferedInputStream, fileName);
            if (metadata.isEmpty()) {
                bufferedInputStream.reset();
                metadata = extractWithImageIO(bufferedInputStream, fileName);
            }
        } catch (Exception e) {
            log.warn("Failed to extract image metadata for {}: {}", fileName, e.getMessage(), e);
        }
        return metadata;
    }

    private Map<String, Object> extractWithImageIO(InputStream inputStream, String fileName) {
        Map<String, Object> metadata = new HashMap<>();
        try (ImageInputStream imageStream = ImageIO.createImageInputStream(inputStream)) {
            if (imageStream == null) {
                return metadata;
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
            if (!readers.hasNext()) {
                return metadata;
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageStream);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                String format = reader.getFormatName();
                metadata.put("width", width);
                metadata.put("height", height);
                metadata.put("format", format.toLowerCase());
            } finally {
                reader.dispose();
            }
        } catch (Exception e) {
            log.debug("ImageIO extraction failed for {}: {}", fileName, e.getMessage());
        }
        return metadata;
    }

    private Map<String, Object> extractFromBinaryHeader(InputStream inputStream, String fileName) throws IOException {
        Map<String, Object> metadata = new HashMap<>();
        byte[] imageBytes = inputStream.readNBytes(HEADER_READ_LIMIT);
        if (imageBytes.length < 24) {
            return metadata;
        }

        try {
            if (isWebP(imageBytes)) {
                metadata = parseWebPHeader(imageBytes);
            } else if (isPng(imageBytes)) {
                int width = readIntBigEndian(imageBytes, 16);
                int height = readIntBigEndian(imageBytes, 20);
                metadata.put("width", width);
                metadata.put("height", height);
                metadata.put("format", "png");
            } else if (isJpeg(imageBytes)) {
                metadata.putAll(parseJpegHeader(imageBytes));
            } else if (isGif(imageBytes)) {
                int width = readShortLittleEndian(imageBytes, 6);
                int height = readShortLittleEndian(imageBytes, 8);
                metadata.put("width", width);
                metadata.put("height", height);
                metadata.put("format", "gif");
            } else if (isBmp(imageBytes)) {
                int width = readIntLittleEndian(imageBytes, 18);
                int height = readIntLittleEndian(imageBytes, 22);
                metadata.put("width", Math.abs(width));
                metadata.put("height", Math.abs(height));
                metadata.put("format", "bmp");
            }
        } catch (Exception e) {
            log.debug("Header extraction failed for {}: {}", fileName, e.getMessage());
        }

        return metadata;
    }

    private boolean isPng(byte[] bytes) {
        if (bytes.length < 8) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8;
    }

    private Map<String, Object> parseJpegHeader(byte[] bytes) {
        Map<String, Object> metadata = new HashMap<>();
        int position = 2;
        while (position < bytes.length - 1) {
            if (bytes[position] != (byte) 0xFF) {
                position++;
                continue;
            }

            byte marker = bytes[position + 1];
            if (marker == (byte) 0xD9) {
                break;
            }

            if (marker >= (byte) 0xC0 && marker <= (byte) 0xCF
                    && marker != (byte) 0xC4
                    && marker != (byte) 0xC8
                    && marker != (byte) 0xCC) {
                if (position + 8 < bytes.length) {
                    metadata.put("height", readShortBigEndian(bytes, position + 5));
                    metadata.put("width", readShortBigEndian(bytes, position + 7));
                    metadata.put("format", "jpeg");
                }
                break;
            }

            if (position + 3 >= bytes.length) {
                break;
            }

            int blockLength = readShortBigEndian(bytes, position + 2);
            position += 2 + blockLength;
        }
        return metadata;
    }

    private boolean isGif(byte[] bytes) {
        if (bytes.length < 6) {
            return false;
        }
        return bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
    }

    private boolean isWebP(byte[] bytes) {
        if (bytes.length < 12) {
            return false;
        }
        return bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private Map<String, Object> parseWebPHeader(byte[] bytes) {
        Map<String, Object> metadata = new HashMap<>();
        if (bytes.length < 30) {
            return metadata;
        }

        String fourcc = new String(bytes, 12, 4);
        if ("VP8 ".equals(fourcc)) {
            int width = ((bytes[26] & 0xFF) | ((bytes[27] & 0xFF) << 8)) & 0x3FFF;
            int height = ((bytes[28] & 0xFF) | ((bytes[29] & 0xFF) << 8)) & 0x3FFF;
            metadata.put("width", width);
            metadata.put("height", height);
        } else if ("VP8L".equals(fourcc)) {
            int b0 = bytes[21] & 0xFF;
            int b1 = bytes[22] & 0xFF;
            int b2 = bytes[23] & 0xFF;
            int b3 = bytes[24] & 0xFF;
            int width = (b0 | ((b1 & 0x3F) << 8)) + 1;
            int height = (((b1 & 0xC0) >> 6) | (b2 << 2) | ((b3 & 0x03) << 10)) + 1;
            metadata.put("width", width);
            metadata.put("height", height);
        } else if ("VP8X".equals(fourcc)) {
            int width = ((bytes[24] & 0xFF) | ((bytes[25] & 0xFF) << 8) | ((bytes[26] & 0x0F) << 16)) + 1;
            int height = ((bytes[27] & 0xFF) | ((bytes[28] & 0xFF) << 8) | ((bytes[29] & 0x0F) << 16)) + 1;
            metadata.put("width", width);
            metadata.put("height", height);
        }

        if (!metadata.isEmpty()) {
            metadata.put("format", "webp");
        }
        return metadata;
    }

    private boolean isBmp(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == 'B' && bytes[1] == 'M';
    }

    private int readIntBigEndian(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    private int readIntLittleEndian(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private int readShortBigEndian(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    private int readShortLittleEndian(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }
}
