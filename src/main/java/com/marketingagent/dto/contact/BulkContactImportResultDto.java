package com.marketingagent.dto.contact;

import java.util.List;

public record BulkContactImportResultDto(
        int createdCount,
        int skippedCount,
        List<ContactDto> createdContacts,
        List<String> skippedPhoneNumbers
) {
}
