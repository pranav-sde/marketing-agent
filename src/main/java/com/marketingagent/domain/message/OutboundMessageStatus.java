package com.marketingagent.domain.message;

public enum OutboundMessageStatus {
    PENDING,
    SKIPPED,
    QUEUED,
    SENT,
    DELIVERED,
    READ,
    FAILED,
    CANCELED
}
