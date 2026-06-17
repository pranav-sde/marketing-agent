package com.marketingagent.quartz;

import com.marketingagent.service.WebhookEventService;
import java.time.Instant;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
public class WebhookReconciliationJob implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookReconciliationJob.class);

    private final WebhookEventService webhookEventService;

    public WebhookReconciliationJob(WebhookEventService webhookEventService) {
        this.webhookEventService = webhookEventService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            int processed = webhookEventService.markPendingWebhooksProcessed(Instant.now());
            LOGGER.info("Marked {} webhook events processed", processed);
        } catch (RuntimeException exception) {
            throw new JobExecutionException("Failed to reconcile webhook events", exception, false);
        }
    }
}
