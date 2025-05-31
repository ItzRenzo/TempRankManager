package me.itzrenzo.temprankmanager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class TempRankPlaceholders extends PlaceholderExpansion {
    
    private final TempRankManager plugin;
    
    public TempRankPlaceholders(TempRankManager plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "temprank";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "ItzRenzo";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        TempRankData tempRankData = plugin.getDataManager().getTempRank(player.getUniqueId());
        
        switch (params.toLowerCase()) {
            case "time_raw":
                // Returns remaining time in milliseconds, or 0 if no temp rank
                if (tempRankData == null) {
                    return "0";
                }
                return String.valueOf(tempRankData.getRemainingTime());
                
            case "time_formatted":
                // Returns formatted time like "2h 30m" or "No temp rank" if none
                if (tempRankData == null) {
                    return "No temp rank";
                }
                long remainingTime = tempRankData.getRemainingTime();
                if (remainingTime <= 0) {
                    return "Expired";
                }
                return TimeUtil.formatTime(remainingTime);
                
            case "rank":
                // Returns the rank name or "none" if no temp rank
                if (tempRankData == null) {
                    return "none";
                }
                return tempRankData.getRankName();
                
            case "expires_at":
                // Returns expiration timestamp or "0" if no temp rank
                if (tempRankData == null) {
                    return "0";
                }
                return String.valueOf(tempRankData.getExpirationTimestamp());
                
            case "is_paused":
                // Returns "true" if paused, "false" if not paused or no temp rank
                if (tempRankData == null) {
                    return "false";
                }
                return String.valueOf(tempRankData.isPaused());
                
            case "has_temprank":
                // Returns "true" if player has a temp rank, "false" otherwise
                return String.valueOf(tempRankData != null);
                
            default:
                return null; // Placeholder is unknown by the Expansion
        }
    }
}