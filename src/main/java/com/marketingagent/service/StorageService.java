package com.marketingagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class StorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageService.class);

    private final S3Service s3Service;

    @Value("${marketing-agent.storage.upload-dir:./uploads}")
    private String uploadDir;

    public StorageService(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    public String uploadFile(String key, byte[] content, String contentType) {
        return s3Service.uploadFile(key, content, contentType);
    }

    public boolean fileExists(String key) {
        return s3Service.fileExists(key);
    }

    public void deleteFile(String key) {
        s3Service.deleteFile(key);
    }

    public String getFileUrl(String key) {
        return s3Service.getFileUrl(key);
    }

    /**
     * Downloads file to a local destination path.
     * Keeps backward compatibility for services that still rely on local files (e.g. rendering PDF pages as images).
     */
    public void downloadFile(String fileUrl, Path destination) throws IOException {
        LOGGER.info("Downloading file from URL: {} to path: {}", fileUrl, destination);
        
        try (InputStream inputStream = s3Service.getObjectStream(fileUrl)) {
            Files.deleteIfExists(destination);
            Files.createDirectories(destination.getParent());
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Successfully downloaded file to {}", destination);
        } catch (Exception e) {
            LOGGER.error("Failed to download file from URL {}", fileUrl, e);
            throw new IOException("Failed to download file: " + e.getMessage(), e);
        }
    }
}
