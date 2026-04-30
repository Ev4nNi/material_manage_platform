package com.material.platform.metadata;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class MetadataExtractorFactory {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "avi", "mov", "mkv", "webm"
    );

    private final ImageMetadataExtractor imageMetadataExtractor;
    private final VideoMetadataExtractor videoMetadataExtractor;

    public MetadataExtractorFactory() {
        this.imageMetadataExtractor = new ImageMetadataExtractor();
        this.videoMetadataExtractor = new VideoMetadataExtractor();
    }

    public MetadataExtractor getExtractor(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        String extension = getFileExtension(fileName).toLowerCase();

        if (IMAGE_EXTENSIONS.contains(extension)) {
            return imageMetadataExtractor;
        }

        if (VIDEO_EXTENSIONS.contains(extension)) {
            return videoMetadataExtractor;
        }

        return null;
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }
}
