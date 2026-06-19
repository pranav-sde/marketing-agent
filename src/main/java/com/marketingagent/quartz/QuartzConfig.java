package com.marketingagent.quartz;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MarketingAgentQuartzProperties.class)
public class QuartzConfig {

    @Bean
    public JobDetail broadcastSchedulerJobDetail() {
        return JobBuilder.newJob(BroadcastSchedulerJob.class)
                .withIdentity("broadcastSchedulerJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger broadcastSchedulerTrigger(
            JobDetail broadcastSchedulerJobDetail,
            MarketingAgentQuartzProperties properties
    ) {
        return TriggerBuilder.newTrigger()
                .forJob(broadcastSchedulerJobDetail)
                .withIdentity("broadcastSchedulerTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(properties.getBroadcastSchedulerCron()))
                .build();
    }

    @Bean
    public JobDetail webhookReconciliationJobDetail() {
        return JobBuilder.newJob(WebhookReconciliationJob.class)
                .withIdentity("webhookReconciliationJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger webhookReconciliationTrigger(
            JobDetail webhookReconciliationJobDetail,
            MarketingAgentQuartzProperties properties
    ) {
        return TriggerBuilder.newTrigger()
                .forJob(webhookReconciliationJobDetail)
                .withIdentity("webhookReconciliationTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(properties.getWebhookReconciliationCron()))
                .build();
    }

    @Bean
    public JobDetail dailyContentBroadcastJobDetail() {
        return JobBuilder.newJob(DailyContentBroadcastJob.class)
                .withIdentity("dailyContentBroadcastJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger dailyContentBroadcastTrigger(
            JobDetail dailyContentBroadcastJobDetail,
            MarketingAgentQuartzProperties properties
    ) {
        return TriggerBuilder.newTrigger()
                .forJob(dailyContentBroadcastJobDetail)
                .withIdentity("dailyContentBroadcastTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(properties.getDailyContentBroadcastCron()))
                .build();
    }
}
