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
        ContentCalendarStatus status,
        String messageText
) {
    public static ContentCalendarDto from(ContentCalendar calendar) {
        return from(calendar, null);
    }

    public static ContentCalendarDto from(ContentCalendar calendar, String messageText) {
        return new ContentCalendarDto(
                calendar.getId(),
                calendar.getMagazine().getId(),
                calendar.getStory().getId(),
                calendar.getDayNumber(),
                calendar.getContentAngle(),
                calendar.getScheduledDate(),
                calendar.getStatus(),
                messageText
        );
    }
}
