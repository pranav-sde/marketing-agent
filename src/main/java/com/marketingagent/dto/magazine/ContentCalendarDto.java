package com.marketingagent.dto.magazine;

import com.marketingagent.domain.magazine.ContentCalendar;
import com.marketingagent.domain.magazine.ContentCalendarStatus;

import java.time.LocalDate;
import java.util.UUID;

public record ContentCalendarDto(
        UUID id,
        UUID magazineId,
        UUID storyId,
        Integer dayNumber,
        String contentAngle,
        LocalDate scheduledDate,
        ContentCalendarStatus status
) {
    public static ContentCalendarDto from(ContentCalendar calendar) {
        return new ContentCalendarDto(
                calendar.getId(),
                calendar.getMagazine().getId(),
                calendar.getStory().getId(),
                calendar.getDayNumber(),
                calendar.getContentAngle(),
                calendar.getScheduledDate(),
                calendar.getStatus()
        );
    }
}
