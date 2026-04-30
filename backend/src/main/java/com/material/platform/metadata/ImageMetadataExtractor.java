package com.material.platform.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class ImageMetadataExtractor implements MetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(ImageMetadataExtractor.class);

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Override
    public Map<String, Object> extract(InputStream inputStream, String fileName) {
        Map<String, Object> metadata = new HashMap<>();
        try {
            byte[] imageBytes = inputStream.readAllBytes();
            log.info("开始提取图像元数据: {}, 文件大小: {} bytes, 前8字节: {}", 
                fileName, imageBytes.length, bytesToHex(imageBytes, 8));

            metadata = extractWithImageIO(new ByteArrayInputStream(imageBytes), fileName);
            log.info("ImageIO 提取结果: {}", metadata.isEmpty() ? "空" : metadata);

            if (metadata.isEmpty()) {
                log.info("尝试二进制头解析: {}", fileName);
                metadata = extractFromBinaryHeader(imageBytes, fileName);
                log.info("二进制头解析结果: {}", metadata.isEmpty() ? "空" : metadata);
            }
        } catch (Exception e) {
            log.warn("提取图像元数据失败: {}, 错误: {}", fileName, e.getMessage(), e);
        }
        return metadata;
    }

    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, length); i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString();
    }

    private Map<String, Object> extractWithImageIO(InputStream inputStream, String fileName) {
        Map<String, Object> metadata = new HashMap<>();
        try {
            ImageInputStream imageStream = ImageIO.createImageInputStream(inputStream);
            if (imageStream == null) {
                return metadata;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
            if (!readers.hasNext()) {
                return metadata;
            }
            ImageReader reader = readers.next();
            reader.setInput(imageStream);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            String format = reader.getFormatName();
            metadata.put("width", width);
            metadata.put("height", height);
            metadata.put("format", format.toLowerCase());
            log.info("图像元数据提取成功(ImageIO): {}, 尺寸: {}x{}, 格式: {}", fileName, width, height, format);
            reader.dispose();
            imageStream.close();
        } catch (Exception e) {
            log.debug("ImageIO 提取失败: {}", fileName);
        }
        return metadata;
    }

    private Map<String, Object> extractFromBinaryHeader(byte[] imageBytes, String fileName) {
        Map<String, Object> metadata = new HashMap<>();
        if (imageBytes.length < 24) {
            return metadata;
        }
        try {
            if (isWebP(imageBytes)) {
                metadata = parseWebPHeader(imageBytes, fileName);
            } else if (isPng(imageBytes)) {
                int width = readIntBigEndian(imageBytes, 16);
                int height = readIntBigEndian(imageBytes, 20);
                metadata.put("width", width);
                metadata.put("height", height);
                metadata.put("format", "png");
                log.info("图像元数据提取成功(PNG): {}, 尺寸: {}x{}", fileName, width, height);
            } else if (isJpeg(imageBytes)) {
                Map<String, Object> jpegInfo = parseJpegHeader(imageBytes, fileName);
                if (!jpegInfo.isEmpty()) {
                    metadata.putAll(jpegInfo);
                }
            } else if (isGif(imageBytes)) {
                int width = readShortLittleEndian(imageBytes, 6);
                int height = readShortLittleEndian(imageBytes, 8);
                metadata.put("width", width);
                metadata.put("height", height);
                metadata.put("format", "gif");
                log.info("图像元数据提取成功(GIF): {}, 尺寸: {}x{}", fileName, width, height);
            } else if (isBmp(imageBytes)) {
                int width = readIntLittleEndian(imageBytes, 18);
                int height = readIntLittleEndian(imageBytes, 22);
                metadata.put("width", Math.abs(width));
                metadata.put("height", Math.abs(height));
                metadata.put("format", "bmp");
                log.info("图像元数据提取成功(BMP): {}, 尺寸: {}x{}", fileName, Math.abs(width), Math.abs(height));
            }
        } catch (Exception e) {
            log.debug("二进制头解析失败: {}, 错误: {}", fileName, e.getMessage());
        }
        if (metadata.isEmpty()) {
            log.info("无法识别图像格式: {}, 前8字节: {}", fileName, bytesToHex(imageBytes, 8));
        }
        return metadata;
    }

    private boolean isPng(byte[] bytes) {
        if (bytes.length < 8) return false;
        for (int i = 0; i < 8; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) return false;
        }
        return true;
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8;
    }

    private Map<String, Object> parseJpegHeader(byte[] bytes, String fileName) {
        Map<String, Object> metadata = new HashMap<>();
        int pos = 2;
        while (pos < bytes.length - 1) {
            if (bytes[pos] != (byte) 0xFF) {
                pos++;
                continue;
            }
            byte marker = bytes[pos + 1];
            if (marker == (byte) 0xD9) break;
            if (marker >= (byte) 0xC0 && marker <= (byte) 0xCF &&
                marker != (byte) 0xC4 && marker != (byte) 0xC8 && marker != (byte) 0xCC) {
                if (pos + 8 < bytes.length) {
                    int h = readShortBigEndian(bytes, pos + 5);
                    int w = readShortBigEndian(bytes, pos + 7);
                    metadata.put("width", w);
                    metadata.put("height", h);
                    metadata.put("format", "jpeg");
                    log.info("图像元数据提取成功(JPEG): {}, 尺寸: {}x{}", fileName, w, h);
                }
                break;
            }
            if (pos + 3 < bytes.length) {
                int length = readShortBigEndian(bytes, pos + 2);
                pos += 2 + length;
            } else {
                break;
            }
        }
        return metadata;
    }

    private boolean isGif(byte[] bytes) {
        if (bytes.length < 6) return false;
        return bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' &&
               bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
    }

    private boolean isWebP(byte[] bytes) {
        if (bytes.length < 12) return false;
        return bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' &&
               bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }

    private Map<String, Object> parseWebPHeader(byte[] bytes, String fileName) {
        Map<String, Object> metadata = new HashMap<>();
        if (bytes.length < 30) return metadata;
        String fourcc = new String(bytes, 12, 4);
        if ("VP8 ".equals(fourcc)) {
            int w = ((bytes[26] & 0xFF) | ((bytes[27] & 0xFF) << 8)) & 0x3FFF;
            int h = ((bytes[28] & 0xFF) | ((bytes[29] & 0xFF) << 8)) & 0x3FFF;
            metadata.put("width", w);
            metadata.put("height", h);
        } else if ("VP8L".equals(fourcc)) {
            int b0 = bytes[21] & 0xFF;
            int b1 = bytes[22] & 0xFF;
            int b2 = bytes[23] & 0xFF;
            int b3 = bytes[24] & 0xFF;
            int w = (b0 | ((b1 & 0x3F) << 8)) + 1;
            int h = (((b1 & 0xC0) >> 6) | (b2 << 2) | ((b3 & 0x03) << 10)) + 1;
            metadata.put("width", w);
            metadata.put("height", h);
        } else if ("VP8X".equals(fourcc)) {
            int b0 = bytes[24] & 0xFF;
            int b1 = bytes[25] & 0xFF;
            int b2 = bytes[26] & 0xFF;
            int w = (b0 | (b1 << 8) | ((b2 & 0x0F) << 16)) + 1;
            int b3 = bytes[27] & 0xFF;
            int b4 = bytes[28] & 0xFF;
            int b5 = bytes[29] & 0xFF;
            int h = (b3 | (b4 << 8) | ((b5 & 0x0F) << 16)) + 1;
            metadata.put("width", w);
            metadata.put("height", h);
        }
        if (!metadata.isEmpty()) {
            metadata.put("format", "webp");
            log.info("图像元数据提取成功(WebP): {}, 尺寸: {}x{}", fileName, metadata.get("width"), metadata.get("height"));
        }
        return metadata;
    }

    private boolean isBmp(byte[] bytes) {
        return bytes.length >= 2 && bytes[0] == 'B' && bytes[1] == 'M';
    }

    private int readIntBigEndian(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) |
               ((bytes[offset + 1] & 0xFF) << 16) |
               ((bytes[offset + 2] & 0xFF) << 8) |
               (bytes[offset + 3] & 0xFF);
    }

    private int readIntLittleEndian(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) |
               ((bytes[offset + 1] & 0xFF) << 8) |
               ((bytes[offset + 2] & 0xFF) << 16) |
               ((bytes[offset + 3] & 0xFF) << 24);
    }

    private int readShortBigEndian(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
    }

    private int readShortLittleEndian(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }
}
