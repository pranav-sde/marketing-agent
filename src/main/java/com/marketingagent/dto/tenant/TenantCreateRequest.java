package com.marketingagent.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantCreateRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 120) String slug,
        @NotBlank @Size(max = 64) String timezone,
        @NotBlank @Size(max = 16) String defaultLocale
) {
}
