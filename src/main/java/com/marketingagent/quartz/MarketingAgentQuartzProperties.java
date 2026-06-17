package com.marketingagent.quartz;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "marketing-agent.quartz")
public class MarketingAgentQuartzProperties {

    private String broadcastSchedulerCron;
    private String webhookReconciliationCron;

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
