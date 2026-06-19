package com.marketingagent.controller;

import com.marketingagent.dto.magazine.ContentCalendarDto;
import com.marketingagent.dto.magazine.MagazineDto;
import com.marketingagent.dto.magazine.StoryDto;
import com.marketingagent.repository.StoryRepository;
import com.marketingagent.service.ContentCalendarService;
import com.marketingagent.service.MagazineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/magazines")
public class MagazineController {

    private final MagazineService magazineService;
    private final ContentCalendarService contentCalendarService;
    private final StoryRepository storyRepository;

    public MagazineController(
            MagazineService magazineService,
            ContentCalendarService contentCalendarService,
            StoryRepository storyRepository) {
        this.magazineService = magazineService;
        this.contentCalendarService = contentCalendarService;
        this.storyRepository = storyRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MagazineDto> uploadMagazine(
            @PathVariable UUID tenantId,
            @RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        MagazineDto dto = magazineService.uploadMagazine(tenantId, file);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<MagazineDto>> listMagazines(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(magazineService.listMagazines(tenantId));
    }

    @GetMapping("/{magazineId}")
    public ResponseEntity<MagazineDto> getMagazine(
            @PathVariable UUID tenantId,
            @PathVariable UUID magazineId) {
        return ResponseEntity.ok(magazineService.getMagazine(tenantId, magazineId));
    }

    @GetMapping("/{magazineId}/stories")
    public ResponseEntity<List<StoryDto>> getExtractedStories(
            @PathVariable UUID tenantId,
            @PathVariable UUID magazineId) {
        // validate ownership
        magazineService.getMagazineEntity(tenantId, magazineId);
        List<StoryDto> stories = storyRepository.findByMagazine_Id(magazineId)
                .stream()
                .map(StoryDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(stories);
    }

    @PostMapping("/{magazineId}/generate-calendar")
    public ResponseEntity<List<ContentCalendarDto>> generateCalendar(
            @PathVariable UUID tenantId,
            @PathVariable UUID magazineId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contentCalendarService.generateCalendar(tenantId, magazineId));
    }

    @GetMapping("/{magazineId}/calendar")
    public ResponseEntity<List<ContentCalendarDto>> getCalendar(
            @PathVariable UUID tenantId,
            @PathVariable UUID magazineId) {
        return ResponseEntity.ok(contentCalendarService.getCalendar(tenantId, magazineId));
    }
}
