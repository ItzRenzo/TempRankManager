package me.itzrenzo.temprankmanager;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SchedulerHandler {
    private final TempRankManager plugin;
    private final DataManager dataManager;
    private final Permission permission;
    private final Map<UUID, BukkitTask> scheduledTasks = new ConcurrentHashMap<>();

    public SchedulerHandler(TempRankManager plugin, DataManager dataManager, Permission permission) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.permission = permission;
    }

    public void scheduleExistingRanks() {
        dataManager.removeExpiredRanks();
        
        for (TempRankData data : dataManager.getAllTempRanks()) {
            if (data.isExpired()) {
                expireRank(data.getPlayerUUID(), data.getRankName());
            } else if (!data.isPaused()) {
                scheduleRankExpiration(data);
            }
        }
    }

    public void scheduleRankExpiration(TempRankData data) {
        cancelTask(data.getPlayerUUID());
        
        if (data.isPaused()) {
            return;
        }

        long delay = Math.max(1, (data.getExpirationTimestamp() - System.currentTimeMillis()) / 50); // Convert to ticks
        
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                expireRank(data.getPlayerUUID(), data.getRankName());
                scheduledTasks.remove(data.getPlayerUUID());
            }
        }.runTaskLater(plugin, delay);
        
        scheduledTasks.put(data.getPlayerUUID(), task);
    }

    public void expireRank(UUID playerUUID, String rankName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        String playerName = player.getName() != null ? player.getName() : "Unknown";
        
        // Remove from permission system
        permission.playerRemoveGroup(null, player, rankName);
        String defaultGroup = plugin.getDefaultGroup();
        permission.playerAddGroup(null, player, defaultGroup);
        
        // Remove from data
        dataManager.removeTempRank(playerUUID);
        
        // Cancel scheduled task
        cancelTask(playerUUID);
        
        plugin.getLogger().info("Rank " + rankName + " expired for " + playerName + ". Reverted to " + defaultGroup + ".");
    }

    public void pauseAllTimers() {
        long currentTime = System.currentTimeMillis();
        
        for (TempRankData data : dataManager.getAllTempRanks()) {
            if (!data.isPaused()) {
                long remainingTime = data.getExpirationTimestamp() - currentTime;
                data.setTimeLeftMillis(Math.max(0, remainingTime));
                data.setPaused(true);
                dataManager.updateTempRank(data.getPlayerUUID(), data);
                cancelTask(data.getPlayerUUID());
            }
        }
    }

    public void resumeAllTimers() {
        long currentTime = System.currentTimeMillis();
        
        for (TempRankData data : dataManager.getAllTempRanks()) {
            if (data.isPaused()) {
                data.setExpirationTimestamp(currentTime + data.getTimeLeftMillis());
                data.setPaused(false);
                data.setTimeLeftMillis(0);
                dataManager.updateTempRank(data.getPlayerUUID(), data);
                scheduleRankExpiration(data);
            }
        }
    }

    public void cancelTask(UUID playerUUID) {
        BukkitTask task = scheduledTasks.remove(playerUUID);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public void cancelAllTasks() {
        for (BukkitTask task : scheduledTasks.values()) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        scheduledTasks.clear();
    }

    public RankAssignmentResult giveRank(UUID playerUUID, String playerName, String rankName, long durationMillis) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        boolean shouldAccumulate = plugin.getConfig().getBoolean("settings.accumulate-time", true);
        
        // Check if player already has a temporary rank
        if (dataManager.hasTempRank(playerUUID)) {
            TempRankData existingData = dataManager.getTempRank(playerUUID);
            
            // If it's the same rank and accumulation is enabled, accumulate the time
            if (existingData.getRankName().equalsIgnoreCase(rankName) && shouldAccumulate) {
                long currentRemainingTime = existingData.getRemainingTime();
                long newTotalTime = currentRemainingTime + durationMillis;
                long newExpirationTime = System.currentTimeMillis() + newTotalTime;
                
                // Update the expiration time
                existingData.setExpirationTimestamp(newExpirationTime);
                if (existingData.isPaused()) {
                    // If paused, update the stored time left
                    existingData.setTimeLeftMillis(newTotalTime);
                }
                
                dataManager.updateTempRank(playerUUID, existingData);
                
                // Cancel old task and schedule new one
                cancelTask(playerUUID);
                if (!existingData.isPaused()) {
                    scheduleRankExpiration(existingData);
                }
                
                // Log message for time accumulation
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String expirationDate = sdf.format(new Date(newExpirationTime));
                String addedTime = TimeUtil.formatTime(durationMillis);
                String totalTime = TimeUtil.formatTime(newTotalTime);
                plugin.getLogger().info("Added " + addedTime + " to player " + playerName + "'s " + rankName + " rank. Total time remaining: " + totalTime + " (expires " + expirationDate + ").");
                
                return new RankAssignmentResult(true, durationMillis, newTotalTime);
            } else {
                // Different rank or accumulation disabled, remove the old one first
                permission.playerRemoveGroup(null, player, existingData.getRankName());
                cancelTask(playerUUID);
                
                if (existingData.getRankName().equalsIgnoreCase(rankName)) {
                    // Same rank but accumulation disabled - log replacement
                    plugin.getLogger().info("Replacing " + playerName + "'s " + rankName + " rank (accumulation disabled).");
                }
            }
        }
        
        // New rank or different rank - set normally
        long expirationTime = System.currentTimeMillis() + durationMillis;
        
        // Add new rank
        permission.playerAddGroup(null, player, rankName);
        dataManager.addTempRank(playerUUID, rankName, expirationTime);
        
        // Schedule expiration
        TempRankData data = dataManager.getTempRank(playerUUID);
        scheduleRankExpiration(data);
        
        // Log message
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String expirationDate = sdf.format(new Date(expirationTime));
        String formattedDuration = TimeUtil.formatTime(durationMillis);
        plugin.getLogger().info("Gave player " + playerName + " rank " + rankName + " for " + formattedDuration + " (expires " + expirationDate + ").");
        
        return new RankAssignmentResult(false, durationMillis, durationMillis);
    }

    public void removeRank(UUID playerUUID, String playerName) {
        if (!dataManager.hasTempRank(playerUUID)) {
            return;
        }
        
        TempRankData data = dataManager.getTempRank(playerUUID);
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        
        // Remove from permission system
        permission.playerRemoveGroup(null, player, data.getRankName());
        String defaultGroup = plugin.getDefaultGroup();
        permission.playerAddGroup(null, player, defaultGroup);
        
        // Remove from data and cancel task
        dataManager.removeTempRank(playerUUID);
        cancelTask(playerUUID);
        
        plugin.getLogger().info("Manually removed temporary rank " + data.getRankName() + " from " + playerName + ". Reverted to " + defaultGroup + ".");
    }

    public static class RankAssignmentResult {
        private final boolean wasAccumulated;
        private final long addedTime;
        private final long totalTime;

        public RankAssignmentResult(boolean wasAccumulated, long addedTime, long totalTime) {
            this.wasAccumulated = wasAccumulated;
            this.addedTime = addedTime;
            this.totalTime = totalTime;
        }

        public boolean wasAccumulated() {
            return wasAccumulated;
        }

        public long getAddedTime() {
            return addedTime;
        }

        public long getTotalTime() {
            return totalTime;
        }
    }
}