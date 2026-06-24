package com.marketingagent.dto.contact;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkContactImportRequest(
        @NotEmpty List<@Valid ContactCreateRequest> contacts
) {
}
