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
    private final String uploadDir;

    public MagazineService(
            TenantService tenantService,
            MagazineRepository magazineRepository,
            StoryExtractionService storyExtractionService,
            StorageService storageService,
            com.marketingagent.repository.StoryRepository storyRepository,
            com.marketingagent.repository.ContentCalendarRepository contentCalendarRepository,
            @Value("${marketing-agent.storage.upload-dir:./uploads}") String uploadDir) {
        this.tenantService = tenantService;
        this.magazineRepository = magazineRepository;
        this.storyExtractionService = storyExtractionService;
        this.storageService = storageService;
        this.storyRepository = storyRepository;
        this.contentCalendarRepository = contentCalendarRepository;
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
        
        contentCalendarRepository.deleteByMagazine_Id(magazineId);
        storyRepository.deleteByMagazine_Id(magazineId);
        magazineRepository.delete(magazine);
        LOGGER.info("Successfully deleted magazine {}", magazineId);
    }

    @Transactional
    public MagazineDto uploadMagazine(UUID tenantId, MultipartFile file) {
        Tenant tenant = tenantService.getTenantEntity(tenantId);

        try {
            // Create tenant specific directory
            Path tenantDir = Paths.get(uploadDir, "magazines", tenantId.toString());
            Files.createDirectories(tenantDir);

            // Generate unique filename
            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path targetLocation = tenantDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation);

            // Upload to S3
            String s3Key = "magazines/" + tenantId.toString() + "/" + fileName;
            String fileUrl = targetLocation.toAbsolutePath().toString(); // Fallback to local
            try {
                fileUrl = storageService.uploadFile(s3Key, Files.readAllBytes(targetLocation), file.getContentType());
                LOGGER.info("Magazine PDF uploaded to S3 at key: {}", s3Key);
            } catch (Exception e) {
                LOGGER.error("Failed to upload magazine PDF to S3: {}", s3Key, e);
            }

            // Create Magazine record with the S3 URL (or local if S3 failed)
            Magazine magazine = new Magazine(tenant, file.getOriginalFilename(), fileUrl);
            magazine.setFileSize(file.getSize());
            magazine.setMimeType(file.getContentType());
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
