package com.marketingagent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class StorageService {

    @Value("${marketing-agent.storage.upload-dir:/tmp/marketing-agent-uploads}")
    private String uploadDirStr;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${marketing-agent.storage.s3.bucket:}")
    private String s3Bucket;

    @Value("${marketing-agent.storage.s3.access-key:}")
    private String s3AccessKey;

    @Value("${marketing-agent.storage.s3.secret-key:}")
    private String s3SecretKey;

    @Value("${marketing-agent.storage.s3.region:us-east-1}")
    private String s3Region;

    private Path rootLocation;
    private S3Client s3Client;
    private boolean useS3 = false;

    @PostConstruct
    public void init() {
        // Always create local dir (used as temp cache when downloading from S3)
        this.rootLocation = Paths.get(uploadDirStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory: " + this.rootLocation, e);
        }

        // Initialize S3 only if bucket name is provided
        if (s3Bucket != null && !s3Bucket.trim().isEmpty()) {
            try {
                if (s3AccessKey != null && !s3AccessKey.trim().isEmpty()
                        && s3SecretKey != null && !s3SecretKey.trim().isEmpty()) {
                    this.s3Client = S3Client.builder()
                            .region(Region.of(s3Region))
                            .credentialsProvider(
                                    StaticCredentialsProvider.create(
                                            AwsBasicCredentials.create(s3AccessKey, s3SecretKey)))
                            .build();
                } else {
                    this.s3Client = S3Client.builder()
                            .region(Region.of(s3Region))
                            .build();
                }
                this.useS3 = true;
                System.out.println("[StorageService] S3 enabled → bucket: " + s3Bucket + ", region: " + s3Region);
            } catch (Throwable t) {
                System.err.println("[StorageService] Failed to init S3, falling back to local: " + t.getMessage());
                t.printStackTrace();
                this.useS3 = false;
            }
        } else {
            System.out.println("[StorageService] S3 not configured, using local storage: " + this.rootLocation);
        }
    }

    public String storeFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;
        try {
            // Always save locally first (needed for PDF processing)
            Path localPath = this.rootLocation.resolve(fileName);
            Files.copy(file.getInputStream(), localPath, StandardCopyOption.REPLACE_EXISTING);

            // Then upload to S3 if configured
            if (useS3) {
                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(s3Bucket)
                                .key(fileName)
                                .contentType(file.getContentType())
                                .build(),
                        RequestBody.fromFile(localPath));
                System.out.println("[S3] Uploaded: " + fileName);
            }
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file: " + originalFilename + " - " + e.getMessage(), e);
        }
    }

    public String storeFile(byte[] bytes, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;
        try {
            // Always save locally first
            Path localPath = this.rootLocation.resolve(fileName);
            Files.write(localPath, bytes);

            // Then upload to S3 if configured
            if (useS3) {
                String contentType = "application/octet-stream";
                if (fileName.endsWith(".png")) contentType = "image/png";
                else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) contentType = "image/jpeg";
                else if (fileName.endsWith(".pdf")) contentType = "application/pdf";

                s3Client.putObject(
                        PutObjectRequest.builder()
                                .bucket(s3Bucket)
                                .key(fileName)
                                .contentType(contentType)
                                .build(),
                        RequestBody.fromBytes(bytes));
                System.out.println("[S3] Uploaded: " + fileName);
            }
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to store byte file: " + originalFilename + " - " + e.getMessage(), e);
        }
    }

    public Path load(String filename) {
        Path localFile = rootLocation.resolve(filename).normalize();

        // If file not on local disk but S3 is configured, download it
        if (useS3 && !Files.exists(localFile)) {
            System.out.println("[S3] Downloading to local cache: " + filename);
            try {
                s3Client.getObject(
                        GetObjectRequest.builder().bucket(s3Bucket).key(filename).build(),
                        localFile);
            } catch (Exception e) {
                System.err.println("[S3] Download failed for " + filename + ": " + e.getMessage());
            }
        }
        return localFile;
    }

    public String getFileUrl(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        if (filename.startsWith("http://") || filename.startsWith("https://")) {
            return filename;
        }
        // If S3 is enabled, return public S3 URL
        if (useS3) {
            return "https://" + s3Bucket + ".s3." + s3Region + ".amazonaws.com/" + filename;
        }
        // Local dev fallback
        return "http://localhost:" + serverPort + "/v1/uploads/" + filename;
    }

    public void deleteFile(String filename) {
        if (filename == null || filename.isEmpty() || filename.startsWith("http://") || filename.startsWith("https://")) {
            return;
        }
        try {
            // Delete from S3
            if (useS3) {
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(s3Bucket).key(filename).build());
                System.out.println("[S3] Deleted: " + filename);
            }
            // Also delete local cache
            Path file = rootLocation.resolve(filename).normalize();
            Files.deleteIfExists(file);
        } catch (Exception e) {
            System.err.println("Failed to delete file " + filename + ": " + e.getMessage());
        }
    }
}
