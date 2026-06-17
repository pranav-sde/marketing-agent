package com.marketingagent.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactCreateRequest(
        @NotBlank @Size(max = 32) String phoneE164,
        @Size(max = 120) String firstName,
        @Size(max = 120) String lastName,
        @Email @Size(max = 254) String email
) {
}
