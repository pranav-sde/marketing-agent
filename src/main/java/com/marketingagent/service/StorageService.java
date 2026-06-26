package com.marketingagent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    private Path rootLocation;

    @PostConstruct
    public void init() {
        this.rootLocation = Paths.get(uploadDirStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory: " + this.rootLocation, e);
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
            Files.copy(file.getInputStream(), this.rootLocation.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }
    }

    public String storeFile(byte[] bytes, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;
        try {
            Files.write(this.rootLocation.resolve(fileName), bytes);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store byte file: " + originalFilename, e);
        }
    }

    public Path load(String filename) {
        return rootLocation.resolve(filename).normalize();
    }

    public String getFileUrl(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        if (filename.startsWith("http://") || filename.startsWith("https://")) {
            return filename;
        }
        // Expose resource via backend upload endpoint
        return "http://localhost:" + serverPort + "/v1/uploads/" + filename;
    }

    public void deleteFile(String filename) {
        if (filename == null || filename.isEmpty() || filename.startsWith("http://") || filename.startsWith("https://")) {
            return;
        }
        try {
            Path file = this.load(filename);
            Files.deleteIfExists(file);
            System.out.println("Deleted storage file: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to delete file " + filename + ": " + e.getMessage());
        }
    }
}
