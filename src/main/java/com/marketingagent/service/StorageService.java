package com.marketingagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class StorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageService.class);

    @Value("${marketing-agent.storage.s3.bucket:}")
    private String bucketName;

    @Value("${marketing-agent.storage.s3.access-key:}")
    private String accessKey;

    @Value("${marketing-agent.storage.s3.secret-key:}")
    private String secretKey;

    @Value("${marketing-agent.storage.s3.region:us-east-1}")
    private String region;

    @Value("${marketing-agent.storage.upload-dir:./uploads}")
    private String uploadDir;

    private S3Client s3Client;
    private boolean isS3Enabled = false;

    @PostConstruct
    public void init() {
        if (bucketName != null && !bucketName.isBlank() &&
                accessKey != null && !accessKey.isBlank() &&
                secretKey != null && !secretKey.isBlank()) {
            try {
                this.s3Client = S3Client.builder()
                        .region(Region.of(region))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        ))
                        .build();
                this.isS3Enabled = true;
                LOGGER.info("AWS S3 Storage initialized successfully. Bucket: {}", bucketName);
            } catch (Exception e) {
                LOGGER.error("Failed to initialize AWS S3 client. Falling back to local storage.", e);
            }
        } else {
            LOGGER.info("AWS S3 credentials not fully configured. Using local mock storage.");
        }
    }

    public String uploadFile(String key, byte[] content, String contentType) {
        if (isS3Enabled) {
            try {
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .build();
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(content));
                String encodedKey = java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
                String s3Url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, encodedKey);
                LOGGER.info("Successfully uploaded file to AWS S3: {}", s3Url);
                return s3Url;
            } catch (Exception e) {
                LOGGER.error("Failed to upload file to S3, falling back to local storage", e);
            }
        }

        // Fallback to local storage (mock S3)
        try {
            Path targetDir = Paths.get(uploadDir, "s3-mock");
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(key);
            Files.write(targetFile, content);

            // Serve file locally
            String localUrl = "http://localhost:8080/uploads/s3-mock/" + key;
            LOGGER.info("Successfully saved file to local mock storage: {}", localUrl);
            return localUrl;
        } catch (IOException e) {
            LOGGER.error("Failed to save file to local storage", e);
            throw new RuntimeException("Failed to store media file", e);
        }
    }

    public boolean fileExists(String key) {
        if (isS3Enabled) {
            try {
                software.amazon.awssdk.services.s3.model.HeadObjectRequest headObjectRequest = software.amazon.awssdk.services.s3.model.HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                s3Client.headObject(headObjectRequest);
                return true;
            } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        Path targetFile = Paths.get(uploadDir, "s3-mock", key);
        return Files.exists(targetFile);
    }

    public void deleteFile(String key) {
        if (isS3Enabled) {
            try {
                software.amazon.awssdk.services.s3.model.DeleteObjectRequest deleteRequest = software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                s3Client.deleteObject(deleteRequest);
                LOGGER.info("Successfully deleted file from AWS S3: {}", key);
            } catch (Exception e) {
                LOGGER.error("Failed to delete file from S3", e);
            }
        } else {
            try {
                Path targetFile = Paths.get(uploadDir, "s3-mock", key);
                Files.deleteIfExists(targetFile);
                LOGGER.info("Successfully deleted file from local mock storage: {}", key);
            } catch (IOException e) {
                LOGGER.error("Failed to delete file from local storage", e);
            }
        }
    }

    public String getFileUrl(String key) {
        if (isS3Enabled) {
            String encodedKey = java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, encodedKey);
        }
        return "http://localhost:8080/uploads/s3-mock/" + key;
    }
}
