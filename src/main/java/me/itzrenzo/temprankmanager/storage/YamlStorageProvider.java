package me.itzrenzo.temprankmanager.storage;

import me.itzrenzo.temprankmanager.TempRankData;
import me.itzrenzo.temprankmanager.TempRankManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class YamlStorageProvider implements StorageProvider {
    
    private final TempRankManager plugin;
    private final String dataFileName;
    private final Map<UUID, TempRankData> tempRanks = new ConcurrentHashMap<>();
    private File dataFile;
    
    public YamlStorageProvider(TempRankManager plugin, String dataFileName) {
        this.plugin = plugin;
        this.dataFileName = dataFileName;
    }
    
    @Override
    public void initialize() {
        dataFile = new File(plugin.getDataFolder(), dataFileName);
        
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
                plugin.getLogger().info("Created new YAML data file: " + dataFileName);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create YAML data file: " + e.getMessage());
            }
        }
        
        loadData();
        plugin.getLogger().info("Using YAML storage provider: " + dataFileName);
    }
    
    @Override
    public void close() {
        saveData();
        plugin.getLogger().info("YAML storage provider closed.");
    }
    
    private void loadData() {
        tempRanks.clear();
        FileConfiguration dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        for (String uuidString : dataConfig.getKeys(false)) {
            try {
                UUID playerUUID = UUID.fromString(uuidString);
                String rankName = dataConfig.getString(uuidString + ".rankName");
                long expirationTimestamp = dataConfig.getLong(uuidString + ".expirationTimestamp");
                boolean isPaused = dataConfig.getBoolean(uuidString + ".isPaused", false);
                long timeLeftMillis = dataConfig.getLong(uuidString + ".timeLeftMillis", 0);
                
                if (rankName != null) {
                    TempRankData data = new TempRankData(playerUUID, rankName, expirationTimestamp, isPaused, timeLeftMillis);
                    tempRanks.put(playerUUID, data);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in YAML data file: " + uuidString);
            }
        }
        
        plugin.getLogger().info("Loaded " + tempRanks.size() + " temporary rank records from YAML.");
    }
    
    private void saveData() {
        FileConfiguration dataConfig = new YamlConfiguration();
        
        for (Map.Entry<UUID, TempRankData> entry : tempRanks.entrySet()) {
            String uuidString = entry.getKey().toString();
            TempRankData data = entry.getValue();
            
            dataConfig.set(uuidString + ".rankName", data.getRankName());
            dataConfig.set(uuidString + ".expirationTimestamp", data.getExpirationTimestamp());
            dataConfig.set(uuidString + ".isPaused", data.isPaused());
            dataConfig.set(uuidString + ".timeLeftMillis", data.getTimeLeftMillis());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save YAML data file: " + e.getMessage());
        }
    }
    
    @Override
    public Collection<TempRankData> loadAllData() {
        return new ArrayList<>(tempRanks.values());
    }
    
    @Override
    public void saveTempRank(TempRankData data) {
        tempRanks.put(data.getPlayerUUID(), data);
        saveData();
    }
    
    @Override
    public void removeTempRank(UUID playerUUID) {
        tempRanks.remove(playerUUID);
        saveData();
    }
    
    @Override
    public TempRankData getTempRank(UUID playerUUID) {
        return tempRanks.get(playerUUID);
    }
    
    @Override
    public boolean hasTempRank(UUID playerUUID) {
        return tempRanks.containsKey(playerUUID);
    }
    
    @Override
    public void removeExpiredRanks() {
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, TempRankData> entry : tempRanks.entrySet()) {
            TempRankData data = entry.getValue();
            if (!data.isPaused() && System.currentTimeMillis() >= data.getExpirationTimestamp()) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (UUID uuid : toRemove) {
            tempRanks.remove(uuid);
        }
        
        if (!toRemove.isEmpty()) {
            saveData();
            plugin.getLogger().info("Removed " + toRemove.size() + " expired rank(s) from YAML storage.");
        }
    }
    
    @Override
    public String getProviderName() {
        return "YAML";
    }
}