package com.marketingagent.dto.magazine;

import com.marketingagent.domain.magazine.Story;

import java.util.List;
import java.util.UUID;

public record StoryDto(
        UUID id,
        UUID magazineId,
        String title,
        String summary,
        List<String> keywords,
        Integer pageNumber,
        String section,
        String contentAngle
) {
    public static StoryDto from(Story story) {
        return new StoryDto(
                story.getId(),
                story.getMagazine().getId(),
                story.getTitle(),
                story.getSummary(),
                story.getKeywords(),
                story.getPageNumber(),
                story.getSection(),
                story.getContentAngle()
        );
    }
}
