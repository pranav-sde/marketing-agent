package com.marketingagent.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * One-shot utility to reset magazines stuck in EXTRACTING state.
 * Run with: mvn compile exec:java -Dexec.mainClass="com.marketingagent.util.ResetStuckMagazines"
 */
public class ResetStuckMagazines {
    public static void main(String[] args) throws Exception {
        String url = System.getenv("DATABASE_URL");
        String user = System.getenv("DATABASE_USERNAME");
        String pass = System.getenv("DATABASE_PASSWORD");

        if (url == null) {
            System.err.println("DATABASE_URL not set. Source your .env first.");
            System.exit(1);
        }

        // Convert jdbc: URL to plain postgres URL for DriverManager
        String jdbcUrl = url.startsWith("jdbc:") ? url : "jdbc:" + url;

        System.out.println("Connecting to: " + jdbcUrl.split("\\?")[0]);

        // Explicitly load the PostgreSQL driver
        Class.forName("org.postgresql.Driver");

        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, pass)) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, title, processing_status, file_path, error_message FROM magazines ORDER BY created_at DESC LIMIT 10")) {
                ResultSet rs = ps.executeQuery();
                System.out.println("Recent magazines:");
                boolean found = false;
                while (rs.next()) {
                    System.out.println("  ID: " + rs.getString("id"));
                    System.out.println("  Title: " + rs.getString("title"));
                    System.out.println("  Status: " + rs.getString("processing_status"));
                    System.out.println("  FilePath: " + rs.getString("file_path"));
                    System.out.println("  Error: " + rs.getString("error_message"));
                    System.out.println("  ---");
                    found = true;
                }
                if (!found) System.out.println("  None.");
            }

            // Reset them to UPLOADED so they can be retried
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE magazines SET processing_status='UPLOADED', error_message=NULL WHERE processing_status='EXTRACTING'")) {
                int updated = ps.executeUpdate();
                System.out.println("Reset " + updated + " magazine(s) from EXTRACTING -> UPLOADED.");
            }
        }
        System.out.println("Done.");
    }
}
