package com.marketingagent.service;

import com.marketingagent.domain.magazine.Magazine;
import com.marketingagent.domain.magazine.MagazineStatus;
import com.marketingagent.domain.tenant.Tenant;
import com.marketingagent.dto.magazine.MagazineDto;
import com.marketingagent.exception.ResourceNotFoundException;
import com.marketingagent.repository.MagazineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MagazineService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagazineService.class);

    private final TenantService tenantService;
    private final MagazineRepository magazineRepository;
    private final StoryExtractionService storyExtractionService;
    private final StorageService storageService;
    private final com.marketingagent.repository.StoryRepository storyRepository;
    private final com.marketingagent.repository.ContentCalendarRepository contentCalendarRepository;
    private final com.marketingagent.repository.GeneratedContentRepository generatedContentRepository;
    private final com.marketingagent.repository.ContentOutboundMessageRepository contentOutboundMessageRepository;
    private final String uploadDir;

    public MagazineService(
            TenantService tenantService,
            MagazineRepository magazineRepository,
            StoryExtractionService storyExtractionService,
            StorageService storageService,
            com.marketingagent.repository.StoryRepository storyRepository,
            com.marketingagent.repository.ContentCalendarRepository contentCalendarRepository,
            com.marketingagent.repository.GeneratedContentRepository generatedContentRepository,
            com.marketingagent.repository.ContentOutboundMessageRepository contentOutboundMessageRepository,
            @Value("${marketing-agent.storage.upload-dir:./uploads}") String uploadDir) {
        this.tenantService = tenantService;
        this.magazineRepository = magazineRepository;
        this.storyExtractionService = storyExtractionService;
        this.storageService = storageService;
        this.storyRepository = storyRepository;
        this.contentCalendarRepository = contentCalendarRepository;
        this.generatedContentRepository = generatedContentRepository;
        this.contentOutboundMessageRepository = contentOutboundMessageRepository;
        this.uploadDir = uploadDir;
    }

    @Transactional
    public void deleteMagazine(UUID tenantId, UUID magazineId) {
        Magazine magazine = getMagazineEntity(tenantId, magazineId);
        
        String filePath = magazine.getFilePath();
        if (filePath != null && filePath.startsWith("http")) {
            try {
                java.net.URL url = new java.net.URL(filePath);
                String path = url.getPath();
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                storageService.deleteFile(path);
            } catch (Exception e) {
                LOGGER.error("Failed to parse S3 URL for deletion: {}", filePath, e);
            }
        }
        
        contentOutboundMessageRepository.deleteByCalendarEntry_Magazine_Id(magazineId);
        generatedContentRepository.deleteByCalendarEntry_Magazine_Id(magazineId);
        contentCalendarRepository.deleteByMagazine_Id(magazineId);
        storyRepository.deleteByMagazine_Id(magazineId);
        magazineRepository.delete(magazine);
        LOGGER.info("Successfully deleted magazine {}", magazineId);
    }

    @Transactional
    public MagazineDto uploadMagazine(UUID tenantId, MultipartFile file) {
        Tenant tenant = tenantService.getTenantEntity(tenantId);

        try {
            byte[] fileBytes = file.getBytes();
            String fileHash = calculateSha256(fileBytes);

            // Idempotency check: if already processed or processing, return existing record
            java.util.Optional<Magazine> existingMagazine = magazineRepository.findByTenant_IdAndFileHash(tenantId, fileHash);
            if (existingMagazine.isPresent()) {
                Magazine existing = existingMagazine.get();
                if (existing.getProcessingStatus() == MagazineStatus.PROCESSED || 
                    existing.getProcessingStatus() == MagazineStatus.EXTRACTING) {
                    LOGGER.info("Magazine upload is idempotent. Reusing existing magazine ID: {}, status: {}", 
                            existing.getId(), existing.getProcessingStatus());
                    return MagazineDto.from(existing);
                }
            }

            // Create tenant specific directory
            Path tenantDir = Paths.get(uploadDir, "magazines", tenantId.toString());
            Files.createDirectories(tenantDir);

            // Generate unique filename
            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path targetLocation = tenantDir.resolve(fileName);
            Files.write(targetLocation, fileBytes);

            // Upload to S3
            String s3Key = "magazines/" + tenantId.toString() + "/" + fileName;
            String fileUrl = targetLocation.toAbsolutePath().toString(); // Fallback to local
            try {
                fileUrl = storageService.uploadFile(s3Key, fileBytes, file.getContentType());
                LOGGER.info("Magazine PDF uploaded to S3 at key: {}", s3Key);
            } catch (Exception e) {
                LOGGER.error("Failed to upload magazine PDF to S3: {}", s3Key, e);
            }

            // Create Magazine record with the S3 URL (or local if S3 failed) and hash
            Magazine magazine = new Magazine(tenant, file.getOriginalFilename(), fileUrl);
            magazine.setFileSize(file.getSize());
            magazine.setMimeType(file.getContentType());
            magazine.setFileHash(fileHash);
            magazine = magazineRepository.save(magazine);

            LOGGER.info("Magazine {} uploaded successfully for tenant {}", magazine.getId(), tenantId);

            // Trigger async story extraction after transaction commits
            final UUID finalMagazineId = magazine.getId();
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        storyExtractionService.extractStoriesAsync(finalMagazineId);
                    }
                });
            } else {
                storyExtractionService.extractStoriesAsync(finalMagazineId);
            }

            return MagazineDto.from(magazine);

        } catch (IOException e) {
            LOGGER.error("Failed to store uploaded magazine file", e);
            throw new RuntimeException("Could not store file " + file.getOriginalFilename() + ". Please try again!", e);
        }
    }

    private String calculateSha256(byte[] bytes) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate SHA-256 hash", e);
        }
    }

    @Transactional(readOnly = true)
    public MagazineDto getMagazine(UUID tenantId, UUID magazineId) {
        return MagazineDto.from(getMagazineEntity(tenantId, magazineId));
    }

    @Transactional(readOnly = true)
    public List<MagazineDto> listMagazines(UUID tenantId) {
        return magazineRepository.findByTenant_IdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(MagazineDto::from)
                .collect(Collectors.toList());
    }

    public Magazine getMagazineEntity(UUID tenantId, UUID magazineId) {
        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new ResourceNotFoundException("Magazine", magazineId));

        if (!magazine.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Magazine", magazineId);
        }

        return magazine;
    }
}
