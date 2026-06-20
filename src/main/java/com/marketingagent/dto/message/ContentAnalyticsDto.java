package com.marketingagent.dto.message;

public record ContentAnalyticsDto(
        long sentCount,
        long deliveredCount,
        long readCount,
        long failedCount
) {
    public static ContentAnalyticsDto empty() {
        return new ContentAnalyticsDto(0, 0, 0, 0);
    }
}
