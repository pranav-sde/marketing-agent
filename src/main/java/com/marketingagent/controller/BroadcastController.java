package com.marketingagent.controller;

import com.marketingagent.dto.broadcast.BroadcastCreateRequest;
import com.marketingagent.dto.broadcast.BroadcastDto;
import com.marketingagent.service.BroadcastService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tenants/{tenantId}/broadcasts")
public class BroadcastController {

    private final BroadcastService broadcastService;

    public BroadcastController(BroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @PostMapping
    public ResponseEntity<BroadcastDto> createBroadcast(
            @PathVariable UUID tenantId,
            @Valid @RequestBody BroadcastCreateRequest request
    ) {
        BroadcastDto created = broadcastService.createBroadcast(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{broadcastId}")
    public ResponseEntity<BroadcastDto> getBroadcast(
            @PathVariable UUID tenantId,
            @PathVariable UUID broadcastId
    ) {
        BroadcastDto broadcast = BroadcastDto.from(broadcastService.getBroadcastEntity(tenantId, broadcastId));
        return ResponseEntity.ok(broadcast);
    }

    @PostMapping("/{broadcastId}/start")
    public ResponseEntity<BroadcastDto> startBroadcast(
            @PathVariable UUID tenantId,
            @PathVariable UUID broadcastId
    ) {
        BroadcastDto updated = broadcastService.startBroadcast(tenantId, broadcastId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{broadcastId}/pause")
    public ResponseEntity<BroadcastDto> pauseBroadcast(
            @PathVariable UUID tenantId,
            @PathVariable UUID broadcastId
    ) {
        BroadcastDto updated = broadcastService.pauseBroadcast(tenantId, broadcastId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{broadcastId}/cancel")
    public ResponseEntity<BroadcastDto> cancelBroadcast(
            @PathVariable UUID tenantId,
            @PathVariable UUID broadcastId
    ) {
        BroadcastDto updated = broadcastService.cancelBroadcast(tenantId, broadcastId);
        return ResponseEntity.ok(updated);
    }
}
