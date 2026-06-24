package com.marketingagent.service;

import com.marketingagent.exception.PdfProcessingException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class S3Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3Service.class);

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
                LOGGER.info("AWS S3 client initialized successfully. Bucket: {}", bucketName);
            } catch (Exception e) {
                LOGGER.error("Failed to initialize AWS S3 client. Falling back to local storage.", e);
            }
        } else {
            LOGGER.info("AWS S3 credentials not fully configured. Using local mock storage.");
        }
    }

    public boolean isS3Enabled() {
        return isS3Enabled;
    }

    public String getBucketName() {
        return bucketName;
    }

    /**
     * Validates S3 object metadata before processing.
     * Checks if content length > 0 and if content type matches PDF.
     */
    public void validateS3Object(String fileUrl) {
        LOGGER.info("Validating storage object metadata for key/URL: {}", fileUrl);

        if (fileUrl != null && fileUrl.startsWith("http") && !isLocalUrl(fileUrl)) {
            try {
                java.net.URL url = new java.net.URL(fileUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                int responseCode = conn.getResponseCode();

                // If HEAD is not allowed, fallback to GET (common for some presigned S3 URLs)
                if (responseCode == 403 || responseCode == 405) {
                    conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    responseCode = conn.getResponseCode();
                }

                if (responseCode < 200 || responseCode >= 300) {
                    throw new PdfProcessingException("S3 read failure: HTTP validation returned status code " + responseCode);
                }

                long contentLength = conn.getContentLengthLong();
                String contentType = conn.getContentType();
                conn.disconnect();

                LOGGER.info("HTTP URL Metadata - Size: {} bytes, Type: {}", contentLength, contentType);

                if (contentLength <= 0) {
                    throw new PdfProcessingException("Invalid PDF: File size is 0 or empty.");
                }

                if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                    throw new PdfProcessingException("Invalid file type: Expected application/pdf but got " + contentType);
                }
            } catch (PdfProcessingException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.error("Failed to validate HTTP S3 URL: {}", fileUrl, e);
                throw new PdfProcessingException("S3 read failure: Unable to fetch metadata for URL: " + fileUrl, e);
            }
        } else {
            String key = extractKey(fileUrl);
            if (isS3Enabled && !isLocalUrl(fileUrl)) {
                try {
                    HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build();
                    HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);

                    long contentLength = headObjectResponse.contentLength();
                    String contentType = headObjectResponse.contentType();

                    LOGGER.info("S3 Object Metadata - Key: {}, Size: {} bytes, Type: {}", key, contentLength, contentType);

                    if (contentLength <= 0) {
                        throw new PdfProcessingException("Invalid PDF: File size is 0 or empty.");
                    }

                    if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                        throw new PdfProcessingException("Invalid file type: Expected application/pdf but got " + contentType);
                    }
                } catch (NoSuchKeyException e) {
                    LOGGER.error("S3 object does not exist for key: {}", key, e);
                    throw new PdfProcessingException("S3 file read failure: File does not exist for key " + key, e);
                } catch (S3Exception e) {
                    LOGGER.error("S3 metadata validation failed for key: {}", key, e);
                    throw new PdfProcessingException("S3 read failure: Unable to fetch metadata for key " + key, e);
                }
            } else {
                // Local fallback validation
                Path localPath = Paths.get(uploadDir, "s3-mock", key);
                if (!Files.exists(localPath)) {
                    // If it is a full local absolute path, try checking that directly
                    localPath = Paths.get(key);
                }

                if (!Files.exists(localPath)) {
                    throw new PdfProcessingException("Local file read failure: File does not exist at " + localPath);
                }

                try {
                    long size = Files.size(localPath);
                    String contentType = Files.probeContentType(localPath);
                    
                    // Fallback content type detection if probeContentType returns null
                    if (contentType == null && localPath.getFileName().toString().toLowerCase().endsWith(".pdf")) {
                        contentType = "application/pdf";
                    }

                    LOGGER.info("Local Object Metadata - Path: {}, Size: {} bytes, Type: {}", localPath, size, contentType);

                    if (size <= 0) {
                        throw new PdfProcessingException("Invalid PDF: File size is 0 or empty.");
                    }

                    if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
                        throw new PdfProcessingException("Invalid file type: Expected application/pdf but got " + contentType);
                    }
                } catch (IOException e) {
                    throw new PdfProcessingException("Failed to read local file metadata: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Downloads/streams the storage file directly as an InputStream.
     * The caller is responsible for closing the stream.
     */
    public InputStream getObjectStream(String fileUrl) {
        LOGGER.info("Opening stream for URL/key: {}", fileUrl);

        if (fileUrl != null && fileUrl.startsWith("http") && !isLocalUrl(fileUrl)) {
            try {
                java.net.URL url = new java.net.URL(fileUrl);
                return url.openStream();
            } catch (IOException e) {
                LOGGER.error("Failed to open HTTP stream for URL: {}", fileUrl, e);
                throw new PdfProcessingException("Failed to open stream for S3 URL: " + fileUrl + ". " + e.getMessage(), e);
            }
        }

        String key = extractKey(fileUrl);
        if (isS3Enabled) {
            try {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                return s3Client.getObject(getObjectRequest);
            } catch (Exception e) {
                LOGGER.error("Failed to open S3 stream for key: {}", key, e);
                throw new PdfProcessingException("S3 read failure: Failed to open stream for S3 object " + key, e);
            }
        } else {
            // Local fallback stream
            Path localPath = Paths.get(uploadDir, "s3-mock", key);
            if (!Files.exists(localPath)) {
                localPath = Paths.get(key);
            }
            try {
                return Files.newInputStream(localPath);
            } catch (IOException e) {
                LOGGER.error("Failed to open local stream for path: {}", localPath, e);
                throw new PdfProcessingException("Failed to open local file stream: " + e.getMessage(), e);
            }
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
                String encodedKey = java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8)
                        .replace("+", "%20")
                        .replace("%2F", "/");
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
            Path targetFile = targetDir.resolve(key);
            Files.createDirectories(targetFile.getParent());
            Files.write(targetFile, content);

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
                HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                s3Client.headObject(headObjectRequest);
                return true;
            } catch (NoSuchKeyException e) {
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
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
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
            String encodedKey = java.net.URLEncoder.encode(key, java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20")
                    .replace("%2F", "/");
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, encodedKey);
        }
        return "http://localhost:8080/uploads/s3-mock/" + key;
    }

    private String extractKey(String fileUrl) {
        try {
            if (fileUrl.startsWith("http")) {
                java.net.URL url = new java.net.URL(fileUrl);
                String path = url.getPath();
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                return java.net.URLDecoder.decode(path, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse URL for S3 key extraction: {}. Using URL as key directly.", fileUrl);
        }
        return fileUrl;
    }

    private boolean isLocalUrl(String fileUrl) {
        try {
            if (fileUrl != null && fileUrl.startsWith("http")) {
                java.net.URL url = new java.net.URL(fileUrl);
                String host = url.getHost();
                return host.equals("localhost") || host.equals("127.0.0.1");
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }
}
