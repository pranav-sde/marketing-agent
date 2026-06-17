package com.marketingagent.dto.contact;

import com.marketingagent.domain.common.Channel;
import com.marketingagent.domain.contact.ConsentEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ConsentRecordRequest(
        @NotNull Channel channel,
        @NotNull ConsentEventType eventType,
        @NotBlank @Size(max = 160) String source,
        @NotNull Instant capturedAt,
        @Size(max = 80) String policyVersion,
        @Size(max = 512) String evidenceRef
) {
}
