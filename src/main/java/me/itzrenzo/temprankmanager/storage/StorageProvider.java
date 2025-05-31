package me.itzrenzo.temprankmanager.storage;

import me.itzrenzo.temprankmanager.TempRankData;

import java.util.Collection;
import java.util.UUID;

public interface StorageProvider {
    
    /**
     * Initialize the storage provider
     */
    void initialize();
    
    /**
     * Close the storage provider and cleanup resources
     */
    void close();
    
    /**
     * Load all temporary rank data
     */
    Collection<TempRankData> loadAllData();
    
    /**
     * Save a temporary rank record
     */
    void saveTempRank(TempRankData data);
    
    /**
     * Remove a temporary rank record
     */
    void removeTempRank(UUID playerUUID);
    
    /**
     * Get a specific temporary rank record
     */
    TempRankData getTempRank(UUID playerUUID);
    
    /**
     * Check if a player has a temporary rank
     */
    boolean hasTempRank(UUID playerUUID);
    
    /**
     * Remove all expired ranks
     */
    void removeExpiredRanks();
    
    /**
     * Get the name of this storage provider
     */
    String getProviderName();
}