package com.marketingagent.dto.audience;

import com.marketingagent.domain.audience.SegmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record SegmentCreateRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull SegmentType type,
        Map<String, Object> ruleDefinition
) {
}
