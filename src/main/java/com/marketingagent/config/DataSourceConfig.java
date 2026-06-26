package com.marketingagent.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl != null && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            System.out.println("[DB CONFIG] Detected PostgreSQL DATABASE_URL. Translating to JDBC...");
            
            // Format: postgres://username:password@host:port/database
            String cleanUrl = databaseUrl.replace("postgres://", "").replace("postgresql://", "");
            
            String username = null;
            String password = null;
            String jdbcUrl = null;
            
            if (cleanUrl.contains("@")) {
                String[] parts = cleanUrl.split("@");
                String credentials = parts[0];
                jdbcUrl = "jdbc:postgresql://" + parts[1];
                
                if (credentials.contains(":")) {
                    String[] credParts = credentials.split(":");
                    username = credParts[0];
                    password = credParts[1];
                } else {
                    username = credentials;
                }
            } else {
                jdbcUrl = "jdbc:postgresql://" + cleanUrl;
            }
            
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(jdbcUrl);
            if (username != null) {
                dataSource.setUsername(username);
            }
            if (password != null) {
                dataSource.setPassword(password);
            }
            dataSource.setDriverClassName("org.postgresql.Driver");
            
            // Render specific pools optimizations
            dataSource.setMaximumPoolSize(5);
            dataSource.setMinimumIdle(1);
            dataSource.setIdleTimeout(300000);
            dataSource.setConnectionTimeout(30000);
            
            return dataSource;
        }
        
        System.out.println("[DB CONFIG] Using local configuration from application.yml...");
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }
}
