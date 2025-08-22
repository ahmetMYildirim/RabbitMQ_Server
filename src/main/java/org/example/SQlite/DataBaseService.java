package org.example.SQlite;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


public class DataBaseService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger logger = LogManager.getLogger(DataBaseService.class);


    // Giriş ve çıkış yapan kullanıcıların bilgilerini kaydetme metotları
    public void recordUserLogin(String username, String passwordHash) {
        String sql = "INSERT INTO user_logs (username, password_hash, connected_at) VALUES (?, ?, ?)";

        try(Connection conn = sqconnection.connection();
            PreparedStatement preparedStatement = conn.prepareStatement(sql)){

            preparedStatement.setString(1, username);
            preparedStatement.setString(2, passwordHash);
            preparedStatement.setString(3, LocalDateTime.now().format(FORMATTER));
            preparedStatement.executeUpdate();
        }catch (Exception e) {
            System.out.println("Giriş kaydı oluşturulmadı: " + e.getMessage());
        }
    }

    public void recordUserLogOut(String username){
        String findSql = "SELECT id, connected_at FROM user_logs WHERE username = ? AND disconnected_at IS NULL ORDER BY id DESC LIMIT 1";
        String updateSql = "UPDATE user_logs SET disconnected_at = ?, session_duration = ? WHERE id = ?";

        try(Connection conn = sqconnection.connection();){
            int activeSessionId = -1;
            String connectedAt = null;

            try(PreparedStatement preparedStatement = conn.prepareStatement(findSql)){
                preparedStatement.setString(1, username);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    activeSessionId = resultSet.getInt("id");
                    connectedAt = resultSet.getString("connected_at");
                }
            }

            if(activeSessionId != -1 && connectedAt != null){
                LocalDateTime loginTime = LocalDateTime.parse(connectedAt, FORMATTER);
                LocalDateTime logoutTime = LocalDateTime.now();
                long sessionDurationSeconds = java.time.Duration.between(loginTime, logoutTime).getSeconds();

                try(PreparedStatement preparedStatement = conn.prepareStatement(updateSql)){
                    preparedStatement.setString(1, logoutTime.format(FORMATTER));
                    preparedStatement.setLong(2, sessionDurationSeconds);
                    preparedStatement.setInt(3, activeSessionId);

                    int updateRows = preparedStatement.executeUpdate();
                    if(updateRows > 0){
                        System.out.println("Çıkış kaydı güncellendi: " + username + " - Süre: " + formatDuration(sessionDurationSeconds));
                    }else{
                        System.out.println("Güncelleme gerçekleştirilemedi");
                    }
                }
            }

        }catch (SQLException e){
            System.err.println("Çıkış kaydı oluşturulamadı: " + e.getMessage());
        }
    }

    // Kullanıcının son giriş ve çıkış bilgilerini getiren metotlar
    public String getLastLogin(String username){
        String sql = "SELECT connected_at FROM user_logs WHERE username = ? ORDER BY connected_at DESC LIMIT 1";

        try(Connection conn = sqconnection.connection();
        PreparedStatement preparedStatement = conn.prepareStatement(sql)){

            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            if(resultSet.next()){
                String connected_at = resultSet.getString("connected_at");
                if(connected_at != null){
                    LocalDateTime connected_at_date = LocalDateTime.parse(connected_at, FORMATTER);
                    return connected_at_date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                }
            }
        }catch (SQLException e){
            System.out.println("Hata: " + e.getMessage());
        }
        return "Giriş yapılmadı";
    }

    public String getLastLogOut(String username) {
        String sql = "SELECT disconnected_at FROM user_logs WHERE username = ? AND disconnected_at IS NOT NULL ORDER BY disconnected_at DESC LIMIT 1";

        try(Connection conn = sqconnection.connection();
            PreparedStatement preparedStatement = conn.prepareStatement(sql)){

            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            if(resultSet.next()){
                String disconnected_at = resultSet.getString("disconnected_at");
                if(disconnected_at != null){
                    LocalDateTime disconnected_at_date = LocalDateTime.parse(disconnected_at, FORMATTER);
                    return disconnected_at_date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                }
            }
        }catch (SQLException e){
            System.out.println("Son çıkış tarihi alınamadı: " + e.getMessage());
        }
        return "Hiç çıkış yapmadı";
    }

    // Tüm kullanıcıların bilgilerinin tamamını getiren metot
    public Map<String, UserSessionInfo> getAllUserSessions() {
        Map<String, UserSessionInfo> userSessions = new HashMap<>();
        String sql = """
                SELECT username,
               MAX(connected_at) as last_login,
               (SELECT disconnected_at FROM user_logs ul2\s
                WHERE ul2.username = ul1.username\s
                AND ul2.disconnected_at IS NOT NULL\s
                ORDER BY ul2.disconnected_at DESC LIMIT 1) as last_logout,
               (SELECT connected_at FROM user_logs ul3
                WHERE ul3.username = ul1.username
                AND ul3.disconnected_at IS NULL
                ORDER BY ul3.id DESC LIMIT 1) as active_session_start,
               (SELECT session_duration FROM user_logs ul4
                WHERE ul4.username = ul1.username
                AND ul4.disconnected_at IS NOT NULL
                ORDER BY ul4.id DESC LIMIT 1) as last_session_duration
        FROM user_logs ul1
        GROUP BY username
            """;

        try(Connection conn = sqconnection.connection();
            PreparedStatement preparedStatement = conn.prepareStatement(sql)){

            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                String username = resultSet.getString("username");
                String lastLogin = resultSet.getString("last_login");
                String lastLogout = resultSet.getString("last_logout");
                String activeSessionStart = resultSet.getString("active_session_start");
                Long lastSessionDuration = resultSet.getLong("last_session_duration");

                UserSessionInfo userSessionInfo = new UserSessionInfo();
                userSessionInfo.username = username;

                if(lastLogin != null){
                    LocalDateTime login_date = LocalDateTime.parse(lastLogin, FORMATTER);
                    userSessionInfo.lastLoginFormatted = login_date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                }else{
                    userSessionInfo.lastLoginFormatted = "Giriş Yapmadı";
                }

                if(lastLogout != null){
                    LocalDateTime logout_date = LocalDateTime.parse(lastLogout, FORMATTER);
                    userSessionInfo.lastLogoutFormatted = logout_date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                }else{
                    userSessionInfo.lastLogoutFormatted = "Çıkış yapmadı";
                }

                if(activeSessionStart != null){
                    userSessionInfo.isActiveSession = true;
                    userSessionInfo.activeSessionStartTime = LocalDateTime.parse(activeSessionStart, FORMATTER);
                    userSessionInfo.sessionDurationFormatted = calculateActiveSessionDuration(username);
                }else{
                    userSessionInfo.isActiveSession = false;
                    if(lastSessionDuration != null && lastSessionDuration > 0){
                        userSessionInfo.sessionDurationFormatted = formatDuration(lastSessionDuration);
                    } else {
                        userSessionInfo.sessionDurationFormatted = "0 dk";
                    }
                }

                userSessions.put(username, userSessionInfo);
            }

        }catch (SQLException e){
            System.err.println("Kullanıcı oturum bilgileri alınamadı: " + e.getMessage());
        }
        return userSessions;
    }

    // Kullanıcı aktifliğini kontrol eden metot
    public boolean hasActiveSession(String username){
        String sql = "SELECT COUNT(*) FROM `user_logs` WHERE `username` = ? AND `disconnected_at` IS NULL";
        try(Connection conn = sqconnection.connection();
        PreparedStatement preparedStatement = conn.prepareStatement(sql)){

            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                return resultSet.getInt(1) > 0;
            }

        }catch (SQLException e){
            System.err.println("Hata: " + e.getMessage());
        }
        return false;
    }

    // Kullanıcının son yaptığı 10 oturum bilgilerini getiren metot
    public void printUserSessionsHistory(String username){
        String sql = "SELECT disconnected_at, connected_at FROM `user_logs` WHERE `username` = ? ORDER BY disconnected_at DESC LIMIT 10";

        try(Connection conn = sqconnection.connection();
        PreparedStatement preparedStatement = conn.prepareStatement(sql)){

            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            while(resultSet.next()){
                Map<String, Object> logEntry = new HashMap<>();
                logEntry.put("username", resultSet.getString("username"));
                logEntry.put("connected_at", resultSet.getString("connected_at"));
                logEntry.put("disconnected_at", resultSet.getString("disconnected_at"));
            }

        }catch (SQLException e){
            System.out.println("Hata: " + e.getMessage());
        }
    }

    // Aktif oturum süresini hesaplayan metot
    public String calculateActiveSessionDuration(String username){
        String sql = "SELECT connected_at FROM user_logs WHERE username = ? AND disconnected_at IS NULL ORDER BY id DESC LIMIT 1";

        try(Connection conn = sqconnection.connection();
        PreparedStatement preparedStatement = conn.prepareStatement(sql)){

            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();
            while(resultSet.next()){
                String connected_at = resultSet.getString("connected_at");
                if(connected_at != null){
                    LocalDateTime connected_at_date = LocalDateTime.parse(connected_at, FORMATTER);
                    LocalDateTime now = LocalDateTime.now();
                    long duration = now.getSecond() - connected_at_date.getSecond();
                    return formatDuration(duration);
                }
            }

        }catch (SQLException e){
            System.err.println("Süre hesaplanamadı: " + e.getMessage());
        }
        return "0 dk";
    }

    // Databasede kullanıcı oturum sürelerini güncelleyen metot
    public void updateSessionDurationInDatabase() {
        String sql = """
            UPDATE user_logs 
            SET session_duration = CASE 
                WHEN disconnected_at IS NOT NULL AND connected_at IS NOT NULL 
                THEN (strftime('%s', disconnected_at) - strftime('%s', connected_at))
                ELSE session_duration 
            END 
            WHERE session_duration IS NULL OR session_duration = 0
            """;

        try(Connection conn = sqconnection.connection();
            PreparedStatement preparedStatement = conn.prepareStatement(sql)){
            
            int updatedRows = preparedStatement.executeUpdate();
            System.out.println("Oturum süreleri güncellendi: " + updatedRows + " kayıt");
            
        }catch (SQLException e){
            System.err.println("Oturum süreleri güncellenemedi: " + e.getMessage());
        }
    }

    // Kullanıcı oturum bilgileri (Sub-Model)
    public static class UserSessionInfo {
        public String username;
        public String lastLoginFormatted;
        public String lastLogoutFormatted;
        public String sessionDurationFormatted;
        public boolean isActiveSession;
        public LocalDateTime activeSessionStartTime;
    }

    //Oturum süresi hesaplama metodu
    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%d sa %d dk", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%d dk %d sn", minutes, seconds);
        } else {
            return String.format("%d sn", seconds);
        }
    }
}
