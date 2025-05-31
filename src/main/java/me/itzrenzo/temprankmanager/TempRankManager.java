package me.itzrenzo.temprankmanager;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class TempRankManager extends JavaPlugin implements Listener {

    private Permission permission;
    private DataManager dataManager;
    private SchedulerHandler schedulerHandler;
    private boolean isWhitelistMode = false;
    private BukkitTask cleanupTask;
    private TempRankPlaceholders placeholders;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();
        
        if (!setupPermissions()) {
            getLogger().severe("Failed to hook into Vault permissions! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        dataManager = new DataManager(this);
        schedulerHandler = new SchedulerHandler(this, dataManager, permission);
        
        // Register commands and events
        TempRankCommand commandExecutor = new TempRankCommand(this, dataManager, schedulerHandler, permission);
        getCommand("temprank").setExecutor(commandExecutor);
        getCommand("temprank").setTabCompleter(new TempRankTabCompleter(dataManager, permission));
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // Load data and schedule existing ranks
        dataManager.loadData();
        
        // Clean up expired ranks if enabled
        if (getConfig().getBoolean("settings.cleanup-on-startup", true)) {
            dataManager.removeExpiredRanks();
        }
        
        schedulerHandler.scheduleExistingRanks();
        
        // Check initial whitelist state
        isWhitelistMode = Bukkit.hasWhitelist();
        if (isWhitelistMode) {
            getLogger().info("Server is in whitelist mode. Pausing all timers.");
            schedulerHandler.pauseAllTimers();
        }
        
        // Start periodic cleanup task if configured
        startCleanupTask();
        
        // Register PlaceholderAPI expansion if available
        setupPlaceholderAPI();
        
        getLogger().info("TempRankManager has been enabled using " + dataManager.getStorageProvider().getProviderName() + " storage!");
    }

    @Override
    public void onDisable() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
        
        if (schedulerHandler != null) {
            schedulerHandler.cancelAllTasks();
        }
        
        if (dataManager != null) {
            dataManager.close();
        }
        
        getLogger().info("TempRankManager has been disabled!");
    }

    private void startCleanupTask() {
        int cleanupInterval = getConfig().getInt("settings.cleanup-interval", 60);
        
        if (cleanupInterval > 0) {
            long intervalTicks = cleanupInterval * 60 * 20L; // Convert minutes to ticks
            
            cleanupTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
                if (!isWhitelistMode) {
                    dataManager.removeExpiredRanks();
                }
            }, intervalTicks, intervalTicks);
            
            getLogger().info("Started periodic cleanup task (every " + cleanupInterval + " minutes)");
        }
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp == null) {
            return false;
        }
        permission = rsp.getProvider();
        return permission != null;
    }

    private void setupPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholders = new TempRankPlaceholders(this);
            if (placeholders.register()) {
                getLogger().info("PlaceholderAPI expansion registered successfully!");
            } else {
                getLogger().warning("Failed to register PlaceholderAPI expansion.");
            }
        } else {
            getLogger().info("PlaceholderAPI not found. Placeholders will not be available.");
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand().toLowerCase();
        if (command.equals("whitelist on") || command.equals("whitelist off")) {
            // Schedule check for next tick to ensure command has been processed
            Bukkit.getScheduler().runTask(this, () -> {
                boolean newWhitelistState = Bukkit.hasWhitelist();
                if (newWhitelistState != isWhitelistMode) {
                    isWhitelistMode = newWhitelistState;
                    if (isWhitelistMode) {
                        getLogger().info("Pausing all timers.");
                        schedulerHandler.pauseAllTimers();
                    } else {
                        getLogger().info("Resuming all timers.");
                        schedulerHandler.resumeAllTimers();
                    }
                }
            });
        }
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public SchedulerHandler getSchedulerHandler() {
        return schedulerHandler;
    }

    public Permission getPermission() {
        return permission;
    }

    public String getDefaultGroup() {
        return getConfig().getString("settings.default-group", "default");
    }
}
