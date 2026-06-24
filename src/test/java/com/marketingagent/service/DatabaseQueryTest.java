package com.marketingagent.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class DatabaseQueryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void queryDb() {
        System.out.println("--- LOGGING MAGAZINES ---");
        try {
            List<Map<String, Object>> magazines = jdbcTemplate.queryForList("SELECT id, title, processing_status, error_message, file_path, tenant_id FROM magazines");
            for (Map<String, Object> m : magazines) {
                System.out.println("MAGAZINE: id=" + m.get("id") + ", title=" + m.get("title") + ", status=" + m.get("processing_status") + ", error=" + m.get("error_message") + ", path=" + m.get("file_path") + ", tenant=" + m.get("tenant_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("--- LOGGING AUDIT LOGS ---");
        try {
            List<Map<String, Object>> logs = jdbcTemplate.queryForList("SELECT id, action_type, details, occurred_at, tenant_id FROM audit_log ORDER BY occurred_at DESC LIMIT 10");
            for (Map<String, Object> l : logs) {
                System.out.println("LOG: id=" + l.get("id") + ", type=" + l.get("action_type") + ", details=" + l.get("details") + ", occurred=" + l.get("occurred_at") + ", tenant=" + l.get("tenant_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
