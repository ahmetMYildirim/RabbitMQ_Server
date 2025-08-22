package org.example.SQlite;

import java.sql.Connection;
import java.sql.Statement;

public class createTable {
    public static void main(String[] args) {
        try (Connection conn = sqconnection.connection();
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS user_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT NOT NULL," +
                    "password_hash TEXT," +
                    "connected_at TEXT NOT NULL," +
                    "disconnected_at TEXT," +
                    "session_duration INTEGER," +
                    "created_date TEXT DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(sql);

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_username ON user_logs(username)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_connected_at ON user_logs(connected_at)");

            System.out.println("Tablo oluşturuldu/güncellendi");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}