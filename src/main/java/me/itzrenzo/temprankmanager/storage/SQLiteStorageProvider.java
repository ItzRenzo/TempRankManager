package me.itzrenzo.temprankmanager.storage;

import me.itzrenzo.temprankmanager.TempRankData;
import me.itzrenzo.temprankmanager.TempRankManager;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class SQLiteStorageProvider implements StorageProvider {
    
    private final TempRankManager plugin;
    private final String databaseFile;
    private Connection connection;
    
    public SQLiteStorageProvider(TempRankManager plugin, String databaseFile) {
        this.plugin = plugin;
        this.databaseFile = databaseFile;
    }
    
    @Override
    public void initialize() {
        try {
            File dbFile = new File(plugin.getDataFolder(), databaseFile);
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            
            connection = DriverManager.getConnection(url);
            createTables();
            
            plugin.getLogger().info("Connected to SQLite database: " + databaseFile);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize SQLite database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createTables() throws SQLException {
        String createTable = """
            CREATE TABLE IF NOT EXISTS temp_ranks (
                player_uuid TEXT PRIMARY KEY,
                rank_name TEXT NOT NULL,
                expiration_timestamp INTEGER NOT NULL,
                is_paused INTEGER NOT NULL DEFAULT 0,
                time_left_millis INTEGER NOT NULL DEFAULT 0
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
        }
    }
    
    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                plugin.getLogger().info("SQLite database connection closed.");
            } catch (SQLException e) {
                plugin.getLogger().warning("Error closing SQLite connection: " + e.getMessage());
            }
        }
    }
    
    @Override
    public Collection<TempRankData> loadAllData() {
        Collection<TempRankData> data = new ArrayList<>();
        
        String query = "SELECT * FROM temp_ranks";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                String rankName = rs.getString("rank_name");
                long expirationTimestamp = rs.getLong("expiration_timestamp");
                boolean isPaused = rs.getInt("is_paused") == 1;
                long timeLeftMillis = rs.getLong("time_left_millis");
                
                TempRankData tempRank = new TempRankData(playerUUID, rankName, expirationTimestamp, isPaused, timeLeftMillis);
                data.add(tempRank);
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load data from SQLite: " + e.getMessage());
            e.printStackTrace();
        }
        
        return data;
    }
    
    @Override
    public void saveTempRank(TempRankData data) {
        String upsert = """
            INSERT OR REPLACE INTO temp_ranks 
            (player_uuid, rank_name, expiration_timestamp, is_paused, time_left_millis) 
            VALUES (?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement stmt = connection.prepareStatement(upsert)) {
            stmt.setString(1, data.getPlayerUUID().toString());
            stmt.setString(2, data.getRankName());
            stmt.setLong(3, data.getExpirationTimestamp());
            stmt.setInt(4, data.isPaused() ? 1 : 0);
            stmt.setLong(5, data.getTimeLeftMillis());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save temp rank to SQLite: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void removeTempRank(UUID playerUUID) {
        String delete = "DELETE FROM temp_ranks WHERE player_uuid = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(delete)) {
            stmt.setString(1, playerUUID.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove temp rank from SQLite: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public TempRankData getTempRank(UUID playerUUID) {
        String query = "SELECT * FROM temp_ranks WHERE player_uuid = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, playerUUID.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String rankName = rs.getString("rank_name");
                long expirationTimestamp = rs.getLong("expiration_timestamp");
                boolean isPaused = rs.getInt("is_paused") == 1;
                long timeLeftMillis = rs.getLong("time_left_millis");
                
                return new TempRankData(playerUUID, rankName, expirationTimestamp, isPaused, timeLeftMillis);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get temp rank from SQLite: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    @Override
    public boolean hasTempRank(UUID playerUUID) {
        return getTempRank(playerUUID) != null;
    }
    
    @Override
    public void removeExpiredRanks() {
        String delete = "DELETE FROM temp_ranks WHERE is_paused = 0 AND expiration_timestamp <= ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(delete)) {
            stmt.setLong(1, System.currentTimeMillis());
            int removed = stmt.executeUpdate();
            
            if (removed > 0) {
                plugin.getLogger().info("Removed " + removed + " expired rank(s) from SQLite database.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to remove expired ranks from SQLite: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public String getProviderName() {
        return "SQLite";
    }
}