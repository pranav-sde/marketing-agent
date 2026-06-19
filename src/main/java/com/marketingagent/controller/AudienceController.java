package com.marketingagent.controller;

import com.marketingagent.dto.audience.AudienceSnapshotDto;
import com.marketingagent.dto.audience.SegmentCreateRequest;
import com.marketingagent.dto.audience.SegmentDto;
import com.marketingagent.service.AudienceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tenants/{tenantId}")
public class AudienceController {

    private final AudienceService audienceService;

    public AudienceController(AudienceService audienceService) {
        this.audienceService = audienceService;
    }

    @PostMapping("/segments")
    public ResponseEntity<SegmentDto> createSegment(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SegmentCreateRequest request
    ) {
        SegmentDto created = audienceService.createSegment(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/segments")
    public ResponseEntity<List<SegmentDto>> listSegments(@PathVariable UUID tenantId) {
        List<SegmentDto> segments = audienceService.listSegments(tenantId);
        return ResponseEntity.ok(segments);
    }

    @PostMapping("/segments/{segmentId}/snapshots")
    public ResponseEntity<AudienceSnapshotDto> createSnapshot(
            @PathVariable UUID tenantId,
            @PathVariable UUID segmentId
    ) {
        AudienceSnapshotDto created = audienceService.createEmptySnapshot(tenantId, segmentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/audience-snapshots/{snapshotId}")
    public ResponseEntity<AudienceSnapshotDto> getSnapshot(
            @PathVariable UUID tenantId,
            @PathVariable UUID snapshotId
    ) {
        AudienceSnapshotDto snapshot = AudienceSnapshotDto.from(audienceService.getSnapshotEntity(tenantId, snapshotId));
        return ResponseEntity.ok(snapshot);
    }
}
