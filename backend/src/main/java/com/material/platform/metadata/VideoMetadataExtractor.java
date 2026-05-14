package com.material.platform.metadata;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.demux.Demuxer;
import org.jcodec.demux.DemuxerTrack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class VideoMetadataExtractor implements MetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(VideoMetadataExtractor.class);

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "mp4", "mov", "avi", "mkv", "webm"
    );

    @Override
    public Map<String, Object> extract(InputStream inputStream, String fileName) {
        Map<String, Object> metadata = new HashMap<>();
        Path tempFile = null;

        try {
            String extension = getFileExtension(fileName).toLowerCase();
            if (!SUPPORTED_EXTENSIONS.contains(extension)) {
                log.warn("不支持的视频格式: {}, 格式: {}", fileName, extension);
                return metadata;
            }

            tempFile = Files.createTempFile("video_", "_" + fileName);
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            metadata = extractVideoMetadata(tempFile.toFile());

        } catch (Exception e) {
            log.warn("提取视频元数据失败: {}, 错误: {}", fileName, e.getMessage());
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("删除临时文件失败: {}", tempFile);
                }
            }
        }

        return metadata;
    }

    private Map<String, Object> extractVideoMetadata(File file) {
        Map<String, Object> metadata = new HashMap<>();
        SeekableByteChannel ch = null;

        try {
            ch = NIOUtils.readableChannel(file);
            FrameGrab grab = FrameGrab.createFrameGrab(ch);
            Picture picture = grab.getNativeFrame();

            if (picture != null) {
                metadata.put("width", picture.getWidth());
                metadata.put("height", picture.getHeight());
                log.info("视频元数据提取成功: {}, 尺寸: {}x{}", file.getName(), picture.getWidth(), picture.getHeight());
            } else {
                log.warn("无法获取视频帧: {}", file.getName());
            }

        } catch (Exception e) {
            log.warn("提取视频信息失败: {}, 错误: {}", file.getName(), e.getMessage(), e);
        } finally {
            NIOUtils.closeQuietly(ch);
        }

        try {
            double duration = extractDuration(file);
            if (duration > 0) {
                metadata.put("duration", duration);
                log.info("视频时长提取成功: {}, 时长: {} 秒", file.getName(), duration);
            }
        } catch (Exception e) {
            log.warn("提取视频时长失败: {}, 错误: {}", file.getName(), e.getMessage());
        }

        return metadata;
    }

    private double extractDuration(File file) throws IOException, JCodecException {
        SeekableByteChannel ch = null;
        try {
            ch = NIOUtils.readableChannel(file);
            Demuxer demuxer = new Demuxer(ch);
            
            DemuxerTrack videoTrack = null;
            for (DemuxerTrack track : demuxer.getTracks()) {
                if (track.getMeta().getCodec() != null) {
                    videoTrack = track;
                    break;
                }
            }
            
            if (videoTrack == null) {
                return 0;
            }
            
            double totalDuration = 0;
            Packet packet;
            while ((packet = videoTrack.nextFrame()) != null) {
                totalDuration += packet.getDuration();
            }
            
            double fps = videoTrack.getMeta().getTotalDuration() > 0 
                ? (double) videoTrack.getMeta().getTotalFrames() / videoTrack.getMeta().getTotalDuration() 
                : 25.0;
            
            return totalDuration / fps;
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }
}
