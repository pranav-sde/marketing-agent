package com.marketingagent.service;

import com.marketingagent.domain.audience.AudienceSnapshot;
import com.marketingagent.domain.audience.Segment;
import com.marketingagent.domain.tenant.Tenant;
import com.marketingagent.dto.audience.AudienceSnapshotDto;
import com.marketingagent.dto.audience.SegmentCreateRequest;
import com.marketingagent.dto.audience.SegmentDto;
import com.marketingagent.exception.ResourceNotFoundException;
import com.marketingagent.repository.AudienceSnapshotRepository;
import com.marketingagent.repository.SegmentRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class AudienceService {

    private final TenantService tenantService;
    private final SegmentRepository segmentRepository;
    private final AudienceSnapshotRepository audienceSnapshotRepository;

    public AudienceService(
            TenantService tenantService,
            SegmentRepository segmentRepository,
            AudienceSnapshotRepository audienceSnapshotRepository
    ) {
        this.tenantService = tenantService;
        this.segmentRepository = segmentRepository;
        this.audienceSnapshotRepository = audienceSnapshotRepository;
    }

    @Transactional
    public SegmentDto createSegment(UUID tenantId, @Valid SegmentCreateRequest request) {
        Tenant tenant = tenantService.getTenantEntity(tenantId);
        Segment segment = new Segment(tenant, request.name(), request.type());
        segment.setRuleDefinition(request.ruleDefinition() == null ? Map.of() : request.ruleDefinition());
        return SegmentDto.from(segmentRepository.save(segment));
    }

    @Transactional(readOnly = true)
    public Segment getSegmentEntity(UUID tenantId, UUID segmentId) {
        Segment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Segment", segmentId));
        if (!segment.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Segment", segmentId);
        }
        return segment;
    }

    @Transactional
    public AudienceSnapshotDto createEmptySnapshot(UUID tenantId, UUID segmentId) {
        Tenant tenant = tenantService.getTenantEntity(tenantId);
        Segment segment = getSegmentEntity(tenantId, segmentId);
        AudienceSnapshot snapshot = new AudienceSnapshot(tenant, segment, Instant.now());
        return AudienceSnapshotDto.from(audienceSnapshotRepository.save(snapshot));
    }

    @Transactional(readOnly = true)
    public AudienceSnapshot getSnapshotEntity(UUID tenantId, UUID snapshotId) {
        AudienceSnapshot snapshot = audienceSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("AudienceSnapshot", snapshotId));
        if (!snapshot.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("AudienceSnapshot", snapshotId);
        }
        return snapshot;
    }

    @Transactional(readOnly = true)
    public List<SegmentDto> listSegments(UUID tenantId) {
        return segmentRepository.findByTenant_IdAndActiveTrue(tenantId).stream().map(SegmentDto::from).toList();
    }
}
