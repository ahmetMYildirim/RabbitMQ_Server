package org.example.rabbitmq_server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    @JsonProperty("name")
    private String name;

    @JsonProperty("password_hash")
    private String passwordHash;

    @JsonProperty("hashing_algorithm")
    private String hashingAlgorithm;

    private String tags;

    @JsonProperty("limits")
    private Object limits;

    @JsonProperty("last_login_date")
    private String lastLoginDate;

    @JsonProperty("last_logout_date")
    private String lastLogoutDate;

    @JsonProperty("session_Duration")
    private String sessionDuration;

    @JsonProperty("login_start_time")
    private LocalDateTime loginStartTime;

    private boolean isActive = false;

    public User() {}

    public User(String name, String tags){
        this.name = name;
        this.tags = tags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getHashingAlgorithm() {
        return hashingAlgorithm;
    }

    public void setHashingAlgorithm(String hashingAlgorithm) {
        this.hashingAlgorithm = hashingAlgorithm;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getLastLoginFormatted() {
        return lastLoginDate != null ? lastLoginDate : "Giriş yapmadı";
    }

    public void setLastLoginFormatted(String lastLoginFormatted) {
        this.lastLoginDate = lastLoginFormatted;
    }

    public String getLastLogoutFormatted() {
        return lastLogoutDate != null ? lastLogoutDate : "Çıkış yapmadı";
    }

    public void setLastLogoutFormatted(String lastLogoutFormatted) {
        this.lastLogoutDate = lastLogoutFormatted;
    }

    public String getSessionDurationFormatted() {
        if(isActive && loginStartTime != null) {
            return calculateDuration();
        }
        return sessionDuration != null ? sessionDuration : "0 dk";
    }

    public void setSessionDurationFormatted(String sessionDurationFormatted) {
        this.sessionDuration = sessionDurationFormatted;
    }

    public LocalDateTime getLoginStartTime() {
        return loginStartTime;
    }

    public void setLoginStartTime(LocalDateTime loginStartTime) {
        this.loginStartTime = loginStartTime;
    }

    public boolean isActiveSession() {
        return isActive;
    }

    public void setActiveSession(boolean activeSession) {
        this.isActive = activeSession;
    }

    public Object getLimits() {
        return limits;
    }

    public void setLimits(Object limits) {
        this.limits = limits;
    }


    private String calculateDuration() {
        if(loginStartTime == null) {
            return "0 dk";
        }

        Duration duration = Duration.between(loginStartTime, LocalDateTime.now());
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.toSeconds() % 60;

        if (hours > 0) {
            return String.format("%d sa %d dk %d sn", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d dk %d sn", minutes, seconds);
        } else {
            return String.format("%d sn", seconds);
        }
    }

    @JsonSetter("tags")
    public void setTagsFromArray(Object tagsObj) {
        if (tagsObj instanceof String) {
            this.tags = (String) tagsObj;
        } else if (tagsObj instanceof List) {
            List<?> tagsList = (List<?>) tagsObj;
            this.tags = String.join(",", tagsList.stream()
                .map(Object::toString)
                .toArray(String[]::new));
        } else if (tagsObj instanceof String[]) {
            String[] tagsArray = (String[]) tagsObj;
            this.tags = String.join(",", tagsArray);
        } else {
            this.tags = "";
        }
    }

    public boolean isAdmin(){
        return tags != null && tags.contains("administrator");
    }

    @Override
    public String toString() {
        return name + " (" + (tags != null ? tags : "no tags") + ")";
    }
}
