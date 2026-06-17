package com.marketingagent.domain.message;

public enum MessageAttemptStatus {
    PENDING,
    ACCEPTED,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE
}
