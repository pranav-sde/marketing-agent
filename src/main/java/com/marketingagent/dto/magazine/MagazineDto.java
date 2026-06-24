package com.marketingagent.dto.magazine;

import com.marketingagent.domain.magazine.Magazine;
import com.marketingagent.domain.magazine.MagazineStatus;

import java.time.Instant;
import java.util.UUID;

public record MagazineDto(
        UUID id,
        UUID tenantId,
        String title,
        Long fileSize,
        MagazineStatus processingStatus,
        Instant createdAt,
        String errorMessage
) {
    public static MagazineDto from(Magazine magazine) {
        return new MagazineDto(
                magazine.getId(),
                magazine.getTenant().getId(),
                magazine.getTitle(),
                magazine.getFileSize(),
                magazine.getProcessingStatus(),
                magazine.getCreatedAt(),
                magazine.getErrorMessage()
        );
    }
}
