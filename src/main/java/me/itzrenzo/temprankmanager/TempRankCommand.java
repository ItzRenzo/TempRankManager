package me.itzrenzo.temprankmanager;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TempRankCommand implements CommandExecutor {
    private final TempRankManager plugin;
    private final DataManager dataManager;
    private final SchedulerHandler schedulerHandler;
    private final Permission permission;

    public TempRankCommand(TempRankManager plugin, DataManager dataManager, SchedulerHandler schedulerHandler, Permission permission) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.schedulerHandler = schedulerHandler;
        this.permission = permission;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("temprankmanager.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                handleGiveCommand(sender, args);
                break;
            case "remove":
                handleRemoveCommand(sender, args);
                break;
            case "list":
                handleListCommand(sender);
                break;
            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage("§cUsage: /temprank give <player> <rank> <time>");
            sender.sendMessage("§cTime examples: 30s, 5m, 2h, 7d, 1mo");
            return;
        }

        String playerName = args[1];
        String rankName = args[2];
        String timeString = args[3];

        // Parse the time string
        long durationMillis = TimeUtil.parseTime(timeString);
        if (durationMillis <= 0) {
            sender.sendMessage("§cInvalid time format! Use: 30s, 5m, 2h, 7d, 1mo");
            sender.sendMessage("§cSupported units: s (seconds), m (minutes), h (hours), d (days), mo (months)");
            return;
        }

        // Get player (online or offline)
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        OfflinePlayer player = onlinePlayer != null ? onlinePlayer : Bukkit.getOfflinePlayer(playerName);
        
        if (player.getUniqueId() == null || (!player.hasPlayedBefore() && !player.isOnline())) {
            sender.sendMessage("§cPlayer '" + playerName + "' has never played on this server.");
            return;
        }

        // Check if rank exists by trying to get groups
        String[] groups = permission.getGroups();
        boolean rankExists = false;
        if (groups != null) {
            for (String group : groups) {
                if (group.equalsIgnoreCase(rankName)) {
                    rankExists = true;
                    break;
                }
            }
        }
        
        if (!rankExists) {
            sender.sendMessage("§cRank '" + rankName + "' does not exist.");
            return;
        }

        SchedulerHandler.RankAssignmentResult result = schedulerHandler.giveRank(player.getUniqueId(), player.getName(), rankName, durationMillis);
        
        if (result.wasAccumulated()) {
            // Time was accumulated
            String addedTime = TimeUtil.formatTime(result.getAddedTime());
            String totalTime = TimeUtil.formatTime(result.getTotalTime());
            sender.sendMessage("§aAdded " + addedTime + " to " + player.getName() + "'s " + rankName + " rank. Total time: " + totalTime + ".");
        } else {
            // New rank assignment or replacement
            String formattedTime = TimeUtil.formatTime(result.getAddedTime());
            sender.sendMessage("§aGave player " + player.getName() + " rank " + rankName + " for " + formattedTime + ".");
        }
    }

    private void handleRemoveCommand(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sender.sendMessage("§cUsage: /temprank remove <player>");
            return;
        }

        String playerName = args[1];
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        OfflinePlayer player = onlinePlayer != null ? onlinePlayer : Bukkit.getOfflinePlayer(playerName);
        
        if (player.getUniqueId() == null || (!player.hasPlayedBefore() && !player.isOnline())) {
            sender.sendMessage("§cPlayer '" + playerName + "' has never played on this server.");
            return;
        }

        if (!dataManager.hasTempRank(player.getUniqueId())) {
            sender.sendMessage("§cPlayer " + player.getName() + " does not have a temporary rank.");
            return;
        }

        TempRankData data = dataManager.getTempRank(player.getUniqueId());
        schedulerHandler.removeRank(player.getUniqueId(), player.getName());
        sender.sendMessage("§aRemoved temporary rank " + data.getRankName() + " from " + player.getName() + ".");
    }

    private void handleListCommand(CommandSender sender) {
        var tempRanks = dataManager.getAllTempRanks();
        
        if (tempRanks.isEmpty()) {
            sender.sendMessage("§eNo active temporary ranks.");
            return;
        }

        sender.sendMessage("§6=== Active Temporary Ranks ===");
        for (TempRankData data : tempRanks) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(data.getPlayerUUID());
            String playerName = player.getName() != null ? player.getName() : "Unknown";
            
            String timeLeft = formatTime(data.getRemainingTime());
            String pausedStatus = data.isPaused() ? " §c[PAUSED]" : "";
            
            sender.sendMessage("§e" + playerName + " §7- §b" + data.getRankName() + " §7- §a" + timeLeft + pausedStatus);
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6TempRankManager Commands:");
        sender.sendMessage("§e/temprank give <player> <rank> <time> §7- Give a temporary rank");
        sender.sendMessage("§7  Time examples: 30s, 5m, 2h, 7d, 1mo");
        sender.sendMessage("§e/temprank remove <player> §7- Remove a temporary rank");
        sender.sendMessage("§e/temprank list §7- List all active temporary ranks");
    }

    private String formatTime(long milliseconds) {
        return TimeUtil.formatTime(milliseconds);
    }
}