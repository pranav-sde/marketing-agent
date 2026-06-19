package com.marketingagent.quartz;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "marketing-agent.quartz")
public class MarketingAgentQuartzProperties {

    private String broadcastSchedulerCron;
    private String webhookReconciliationCron;
    private String dailyContentBroadcastCron = "0 0 9 * * ?";

    public String getDailyContentBroadcastCron() {
        return dailyContentBroadcastCron;
    }

    public void setDailyContentBroadcastCron(String dailyContentBroadcastCron) {
        this.dailyContentBroadcastCron = dailyContentBroadcastCron;
    }

    public String getBroadcastSchedulerCron() {
        return broadcastSchedulerCron;
    }

    public void setBroadcastSchedulerCron(String broadcastSchedulerCron) {
        this.broadcastSchedulerCron = broadcastSchedulerCron;
    }

    public String getWebhookReconciliationCron() {
        return webhookReconciliationCron;
    }

    public void setWebhookReconciliationCron(String webhookReconciliationCron) {
        this.webhookReconciliationCron = webhookReconciliationCron;
    }
}
