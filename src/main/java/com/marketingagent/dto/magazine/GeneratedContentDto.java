package com.marketingagent.dto.magazine;

import com.marketingagent.domain.magazine.GeneratedContent;
import com.marketingagent.domain.magazine.ContentPlatform;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GeneratedContentDto(
        UUID id,
        UUID calendarEntryId,
        String messageText,
        List<String> hashtags,
        ContentPlatform platform,
        Instant generatedAt
) {
    public static GeneratedContentDto from(GeneratedContent content) {
        return new GeneratedContentDto(
                content.getId(),
                content.getCalendarEntry().getId(),
                content.getMessageText(),
                content.getHashtags(),
                content.getPlatform(),
                content.getGeneratedAt()
        );
    }
}
