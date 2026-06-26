package com.marketingagent.controller;

import com.marketingagent.model.Magazine;
import com.marketingagent.model.Tenant;
import com.marketingagent.repository.MagazineRepository;
import com.marketingagent.repository.TenantRepository;
import com.marketingagent.service.PDFProcessingService;
import com.marketingagent.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.marketingagent.model.CalendarEntry;
import com.marketingagent.repository.CalendarEntryRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/magazines")
public class MagazineController {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private MagazineRepository magazineRepository;

    @Autowired
    private CalendarEntryRepository calendarEntryRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private PDFProcessingService pdfProcessingService;

    public record MagazineResponse(UUID id, String title, String fileName, LocalDateTime createdAt) {}

    @GetMapping
    public ResponseEntity<List<MagazineResponse>> getMagazines(@PathVariable UUID tenantId) {
        List<Magazine> magazines = magazineRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        List<MagazineResponse> response = magazines.stream()
                .map(m -> new MagazineResponse(m.getId(), m.getTitle(), m.getFileName(), m.getCreatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> uploadMagazine(
            @PathVariable UUID tenantId,
            @RequestParam("file") MultipartFile file) {
        
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            // 1. Store the original PDF
            String fileName = storageService.storeFile(file);
            java.nio.file.Path pdfPath = storageService.load(fileName);

            // 2. Create the Magazine Entity
            Magazine magazine = new Magazine();
            magazine.setTenant(tenant);
            String title = file.getOriginalFilename();
            if (title != null && title.contains(".")) {
                title = title.substring(0, title.lastIndexOf("."));
            }
            magazine.setTitle(title);
            magazine.setFileName(file.getOriginalFilename());
            magazine.setStoragePath(fileName);
            
            Magazine saved = magazineRepository.save(magazine);

            // We do PDF processing synchronously here so the UI can immediately load the assets.
            // (The files are stored inside our upload folder, which makes them available for calendar generation).
            System.out.println("Processing magazine: " + saved.getTitle());
            
            // To ensure the files are generated, we can pre-extract them.
            // The list of images will be used by the Calendar Generation endpoint later.
            // For now we just return the saved magazine info.
            return ResponseEntity.ok(new MagazineResponse(saved.getId(), saved.getTitle(), saved.getFileName(), saved.getCreatedAt()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to process magazine upload: " + e.getMessage());
        }
    }

    @DeleteMapping("/{magazineId}")
    @Transactional
    public ResponseEntity<?> deleteMagazine(
            @PathVariable UUID tenantId,
            @PathVariable UUID magazineId) {
        
        return magazineRepository.findById(magazineId)
                .map(magazine -> {
                    // 1. Delete associated calendar entries and their files from storage
                    List<CalendarEntry> entries = calendarEntryRepository.findByTenantIdAndMagazineIdOrderByDayNumberAsc(tenantId, magazineId);
                    for (CalendarEntry entry : entries) {
                        if (entry.getMediaUrl() != null) {
                            storageService.deleteFile(entry.getMediaUrl());
                        }
                    }
                    calendarEntryRepository.deleteAll(entries);

                    // 2. Delete original PDF file
                    if (magazine.getStoragePath() != null) {
                        storageService.deleteFile(magazine.getStoragePath());
                    }

                    // 3. Delete magazine record
                    magazineRepository.delete(magazine);

                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
