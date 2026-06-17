package com.marketingagent.quartz;

import com.marketingagent.service.BroadcastService;
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
public class BroadcastSchedulerJob implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(BroadcastSchedulerJob.class);

    private final BroadcastService broadcastService;

    public BroadcastSchedulerJob(BroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            int queued = broadcastService.queueDueBroadcasts(Instant.now());
            LOGGER.info("Queued {} due broadcasts", queued);
        } catch (RuntimeException exception) {
            throw new JobExecutionException("Failed to queue due broadcasts", exception, false);
        }
    }
}
