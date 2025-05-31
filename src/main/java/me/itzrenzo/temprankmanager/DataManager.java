package me.itzrenzo.temprankmanager;

import me.itzrenzo.temprankmanager.storage.SQLiteStorageProvider;
import me.itzrenzo.temprankmanager.storage.StorageProvider;
import me.itzrenzo.temprankmanager.storage.YamlStorageProvider;

import java.util.*;

public class DataManager {
    private final TempRankManager plugin;
    private StorageProvider storageProvider;

    public DataManager(TempRankManager plugin) {
        this.plugin = plugin;
        initializeStorageProvider();
    }

    private void initializeStorageProvider() {
        String storageType = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase();
        
        switch (storageType) {
            case "sqlite":
                String dbFile = plugin.getConfig().getString("sqlite.database-file", "tempranks.db");
                storageProvider = new SQLiteStorageProvider(plugin, dbFile);
                break;
            case "yaml":
                String yamlFile = plugin.getConfig().getString("yaml.data-file", "data.yml");
                storageProvider = new YamlStorageProvider(plugin, yamlFile);
                break;
            default:
                plugin.getLogger().warning("Unknown storage type '" + storageType + "', defaulting to SQLite");
                storageProvider = new SQLiteStorageProvider(plugin, "tempranks.db");
                break;
        }
        
        storageProvider.initialize();
        plugin.getLogger().info("Using " + storageProvider.getProviderName() + " storage provider");
    }

    public void loadData() {
        // Data is loaded automatically during storage provider initialization
        plugin.getLogger().info("Data loaded using " + storageProvider.getProviderName() + " storage");
    }

    public void saveData() {
        // Individual saves are handled automatically by storage providers
        // This method is kept for compatibility
    }

    public void close() {
        if (storageProvider != null) {
            storageProvider.close();
        }
    }

    public void addTempRank(UUID playerUUID, String rankName, long expirationTimestamp) {
        TempRankData data = new TempRankData(playerUUID, rankName, expirationTimestamp);
        storageProvider.saveTempRank(data);
    }

    public void removeTempRank(UUID playerUUID) {
        storageProvider.removeTempRank(playerUUID);
    }

    public TempRankData getTempRank(UUID playerUUID) {
        return storageProvider.getTempRank(playerUUID);
    }

    public boolean hasTempRank(UUID playerUUID) {
        return storageProvider.hasTempRank(playerUUID);
    }

    public Collection<TempRankData> getAllTempRanks() {
        return storageProvider.loadAllData();
    }

    public void updateTempRank(UUID playerUUID, TempRankData data) {
        storageProvider.saveTempRank(data);
    }

    public void removeExpiredRanks() {
        storageProvider.removeExpiredRanks();
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }
}