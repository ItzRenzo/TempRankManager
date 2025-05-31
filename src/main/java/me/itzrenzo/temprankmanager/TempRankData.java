package me.itzrenzo.temprankmanager;

import java.util.UUID;

public class TempRankData {
    private UUID playerUUID;
    private String rankName;
    private long expirationTimestamp;
    private boolean isPaused;
    private long timeLeftMillis;

    public TempRankData(UUID playerUUID, String rankName, long expirationTimestamp) {
        this.playerUUID = playerUUID;
        this.rankName = rankName;
        this.expirationTimestamp = expirationTimestamp;
        this.isPaused = false;
        this.timeLeftMillis = 0;
    }

    public TempRankData(UUID playerUUID, String rankName, long expirationTimestamp, boolean isPaused, long timeLeftMillis) {
        this.playerUUID = playerUUID;
        this.rankName = rankName;
        this.expirationTimestamp = expirationTimestamp;
        this.isPaused = isPaused;
        this.timeLeftMillis = timeLeftMillis;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getRankName() {
        return rankName;
    }

    public long getExpirationTimestamp() {
        return expirationTimestamp;
    }

    public void setExpirationTimestamp(long expirationTimestamp) {
        this.expirationTimestamp = expirationTimestamp;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }

    public long getTimeLeftMillis() {
        return timeLeftMillis;
    }

    public void setTimeLeftMillis(long timeLeftMillis) {
        this.timeLeftMillis = timeLeftMillis;
    }

    public boolean isExpired() {
        return !isPaused && System.currentTimeMillis() >= expirationTimestamp;
    }

    public long getRemainingTime() {
        if (isPaused) {
            return timeLeftMillis;
        }
        return Math.max(0, expirationTimestamp - System.currentTimeMillis());
    }
}