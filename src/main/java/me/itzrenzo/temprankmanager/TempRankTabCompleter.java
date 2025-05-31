package me.itzrenzo.temprankmanager;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TempRankTabCompleter implements TabCompleter {
    
    private final DataManager dataManager;
    private final Permission permission;
    
    public TempRankTabCompleter(DataManager dataManager, Permission permission) {
        this.dataManager = dataManager;
        this.permission = permission;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - subcommands
            List<String> subcommands = Arrays.asList("give", "remove", "list");
            return subcommands.stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            // Second argument - player names for give/remove commands
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove")) {
                return getPlayerNames(args[1]);
            }
        }
        
        if (args.length == 3) {
            // Third argument - rank names for give command
            if (args[0].equalsIgnoreCase("give")) {
                return getRankNames(args[2]);
            }
        }
        
        if (args.length == 4) {
            // Fourth argument - time format suggestions for give command
            if (args[0].equalsIgnoreCase("give")) {
                String[] timeExamples = TimeUtil.getExampleTimes();
                return Arrays.stream(timeExamples)
                        .filter(time -> time.startsWith(args[3].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
    
    private List<String> getPlayerNames(String partial) {
        List<String> playerNames = new ArrayList<>();
        
        // Add online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(partial.toLowerCase())) {
                playerNames.add(player.getName());
            }
        }
        
        // Add players with active temp ranks
        for (TempRankData data : dataManager.getAllTempRanks()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(data.getPlayerUUID());
            if (player.getName() != null && 
                player.getName().toLowerCase().startsWith(partial.toLowerCase()) &&
                !playerNames.contains(player.getName())) {
                playerNames.add(player.getName());
            }
        }
        
        return playerNames;
    }
    
    private List<String> getRankNames(String partial) {
        List<String> rankNames = new ArrayList<>();
        
        // Get available groups from permission system
        String[] groups = permission.getGroups();
        if (groups != null) {
            for (String group : groups) {
                if (group.toLowerCase().startsWith(partial.toLowerCase())) {
                    rankNames.add(group);
                }
            }
        }
        
        // Add common rank names if no groups found or as fallback
        if (rankNames.isEmpty()) {
            List<String> commonRanks = Arrays.asList(
                "vip", "premium", "moderator", "admin", "helper", 
                "builder", "donor", "supporter", "member", "trusted"
            );
            
            rankNames.addAll(commonRanks.stream()
                    .filter(rank -> rank.toLowerCase().startsWith(partial.toLowerCase()))
                    .collect(Collectors.toList()));
        }
        
        return rankNames;
    }
}