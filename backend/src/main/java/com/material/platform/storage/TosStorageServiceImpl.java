package com.material.platform.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service("tosStorageService")
public class TosStorageServiceImpl implements StorageService {

    @Value("${storage.tos.base-url:}")
    private String baseUrl;

    @Override
    public String upload(MultipartFile file, String storageKey) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuidFilename = UUID.randomUUID().toString() + extension;
        String objectKey = datePath + "/" + uuidFilename;

        // TODO: 初始化 TOS 客户端
        // TosClient client = new TosClient(accessKeyId, accessKeySecret, endpoint);

        // TODO: 将文件上传到 TOS
        // PutObjectRequest request = new PutObjectRequest(bucketName, objectKey, file.getInputStream());
        // client.putObject(request);

        // TODO: 处理上传结果并返回 objectKey
        // return objectKey;

        throw new UnsupportedOperationException("TOS 存储尚未配置，请添加 TOS SDK 依赖并配置相关参数");
    }

    @Override
    public void delete(String storageKey) throws IOException {
        // TODO: 从 TOS 删除文件
        // TosClient client = new TosClient(accessKeyId, accessKeySecret, endpoint);
        // client.deleteObject(bucketName, storageKey);

        throw new UnsupportedOperationException("TOS 存储尚未配置，请添加 TOS SDK 依赖并配置相关参数");
    }

    @Override
    public File getFile(String storageKey) {
        throw new UnsupportedOperationException("TOS 存储不支持本地文件获取，请使用预览 URL");
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }
}
