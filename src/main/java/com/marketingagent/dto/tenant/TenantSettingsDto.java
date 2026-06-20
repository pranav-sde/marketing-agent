package com.marketingagent.dto.tenant;

import jakarta.validation.constraints.Size;

public record TenantSettingsDto(
        @Size(max = 512)
        String whatsappAccessToken,

        @Size(max = 120)
        String whatsappPhoneNumberId
) {}
