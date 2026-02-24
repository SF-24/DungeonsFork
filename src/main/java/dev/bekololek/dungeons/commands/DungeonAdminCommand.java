package dev.bekololek.dungeons.commands;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.models.DungeonInstance;
import dev.bekololek.dungeons.utils.ColorUtil;
import dev.bekololek.dungeons.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class DungeonAdminCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public DungeonAdminCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload":
                handleReload(sender);
                break;
            case "reset":
                handleReset(sender, args, label);
                break;
            case "list":
                handleList(sender);
                break;
            case "teleport":
            case "tp":
                handleTeleport(sender, args, label);
                break;
            case "stats":
                handleStats(sender);
                break;
            case "cleanup":
                handleCleanup(sender);
                break;
            default:
                sendUsage(sender, label);
                break;
        }

        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Component.text("--- Dungeon Admin ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("/" + label + " reload", NamedTextColor.YELLOW)
                .append(Component.text(" - Reload all configs", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " reset <player|dungeon>", NamedTextColor.YELLOW)
                .append(Component.text(" - Reset cooldowns/data", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " list", NamedTextColor.YELLOW)
                .append(Component.text(" - List active instances", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " teleport <instance_id>", NamedTextColor.YELLOW)
                .append(Component.text(" - Teleport to instance", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " stats", NamedTextColor.YELLOW)
                .append(Component.text(" - Plugin statistics", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " cleanup", NamedTextColor.YELLOW)
                .append(Component.text(" - Force cleanup all instances", NamedTextColor.GRAY)));
    }

    // ==================== reload ====================

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("dungeons.admin.reload")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }

        plugin.reload();
        MessageUtil.sendMessage(sender, "general.reload-success");
    }

    // ==================== reset ====================

    private void handleReset(CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission("dungeons.admin.reset")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }

        if (args.length < 2) {
            MessageUtil.sendRaw(sender, "&cUsage: /" + label + " reset <player_name|dungeon_id>");
            return;
        }

        String target = args[1];

        // Check if it's a player name
        Player targetPlayer = Bukkit.getPlayerExact(target);
        if (targetPlayer != null) {
            // Reset player cooldowns
            dev.bekololek.dungeons.models.PlayerData data = plugin.getDatabaseManager().loadPlayerData(targetPlayer.getUniqueId());
            data.clearAllCooldowns();
            plugin.getDatabaseManager().savePlayerData(data);
            MessageUtil.sendRaw(sender, "&aReset all cooldowns for player: &e" + targetPlayer.getName());
            return;
        }

        // Check if it's a dungeon ID
        if (plugin.getDungeonManager().dungeonExists(target.toLowerCase())) {
            String dungeonId = target.toLowerCase();
            // Reset all player cooldowns for this dungeon
            int resetCount = 0;
            for (Player online : Bukkit.getOnlinePlayers()) {
                dev.bekololek.dungeons.models.PlayerData data = plugin.getDatabaseManager().loadPlayerData(online.getUniqueId());
                if (data.hasCooldown(dungeonId)) {
                    data.clearCooldown(dungeonId);
                    plugin.getDatabaseManager().savePlayerData(data);
                    resetCount++;
                }
            }
            MessageUtil.sendRaw(sender, "&aReset cooldowns for dungeon '&e" + dungeonId + "&a' (&e" + resetCount + " &aplayers).");
            return;
        }

        // Try offline player by name
        @SuppressWarnings("deprecation")
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(target);
        if (offlinePlayer.hasPlayedBefore()) {
            dev.bekololek.dungeons.models.PlayerData data = plugin.getDatabaseManager().loadPlayerData(offlinePlayer.getUniqueId());
            data.clearAllCooldowns();
            plugin.getDatabaseManager().savePlayerData(data);
            MessageUtil.sendRaw(sender, "&aReset all cooldowns for offline player: &e" + target);
            return;
        }

        MessageUtil.sendRaw(sender, "&cNo player or dungeon found with name: &e" + target);
    }

    // ==================== list ====================

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("dungeons.admin")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }

        Collection<DungeonInstance> instances = plugin.getInstanceManager().getActiveInstances();

        if (instances.isEmpty()) {
            MessageUtil.sendRaw(sender, "&eNo active dungeon instances.");
            return;
        }

        sender.sendMessage(Component.text("--- Active Instances (" + instances.size() + ") ---",
                NamedTextColor.GOLD, TextDecoration.BOLD));

        for (DungeonInstance instance : instances) {
            String dungeonName = instance.getDungeon().getDisplayName();
            String state = instance.getState().name();
            int playerCount = instance.getOnlinePlayers().size();
            String slot = instance.getGridSlot().getCoordinateString();

            Component line = Component.text(" " + instance.getInstanceId().toString().substring(0, 8), NamedTextColor.YELLOW)
                    .append(Component.text(" ")).append(ColorUtil.toComponent(dungeonName))
                    .append(Component.text(" [" + state + "]", stateColor(instance.getState())))
                    .append(Component.text(" " + playerCount + " players", NamedTextColor.GRAY))
                    .append(Component.text(" " + slot, NamedTextColor.DARK_GRAY));

            if (instance.hasTimeLimit()) {
                long remaining = instance.getRemainingTime();
                line = line.append(Component.text(" " + MessageUtil.formatTime(remaining), NamedTextColor.AQUA));
            }

            sender.sendMessage(line);
        }
    }

    private NamedTextColor stateColor(DungeonInstance.InstanceState state) {
        switch (state) {
            case STARTING: return NamedTextColor.YELLOW;
            case ACTIVE: return NamedTextColor.GREEN;
            case COMPLETED: return NamedTextColor.GOLD;
            case POST_COMPLETION: return NamedTextColor.AQUA;
            case FAILED: return NamedTextColor.RED;
            case CLEANING_UP: return NamedTextColor.GRAY;
            default: return NamedTextColor.WHITE;
        }
    }

    // ==================== teleport ====================

    private void handleTeleport(CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission("dungeons.admin.teleport")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }

        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "general.players-only");
            return;
        }

        if (args.length < 2) {
            MessageUtil.sendRaw(sender, "&cUsage: /" + label + " teleport <instance_id>");
            return;
        }

        String idInput = args[1].toLowerCase();

        // Find instance by partial UUID match
        DungeonInstance targetInstance = null;
        for (DungeonInstance instance : plugin.getInstanceManager().getActiveInstances()) {
            if (instance.getInstanceId().toString().toLowerCase().startsWith(idInput)) {
                targetInstance = instance;
                break;
            }
        }

        if (targetInstance == null) {
            // Try full UUID
            try {
                UUID instanceId = UUID.fromString(args[1]);
                targetInstance = plugin.getInstanceManager().getInstance(instanceId);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (targetInstance == null) {
            MessageUtil.sendRaw(sender, "&cInstance not found: &e" + idInput);
            return;
        }

        org.bukkit.Location spawnLoc = targetInstance.getSpawnLocation().clone().add(
                targetInstance.getDungeon().getSpawnOffsetX(),
                targetInstance.getDungeon().getSpawnOffsetY(),
                targetInstance.getDungeon().getSpawnOffsetZ()
        );

        player.teleport(spawnLoc);
        MessageUtil.sendRaw(sender, "&aTeleported to instance &e" + targetInstance.getInstanceId().toString().substring(0, 8)
                + " &a(" + targetInstance.getDungeon().getDisplayName() + ")");
    }

    // ==================== stats ====================

    private void handleStats(CommandSender sender) {
        if (!sender.hasPermission("dungeons.admin")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }

        sender.sendMessage(Component.text("--- Dungeons Plugin Statistics ---", NamedTextColor.GOLD, TextDecoration.BOLD));

        // Instance stats
        Map<String, Integer> instanceStats = plugin.getInstanceManager().getStatistics();
        sender.sendMessage(Component.text("Active Instances: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(instanceStats.getOrDefault("active_instances", 0)), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("Players in Dungeons: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(instanceStats.getOrDefault("players_in_dungeons", 0)), NamedTextColor.YELLOW)));

        // Grid stats
        Map<String, Integer> gridStats = plugin.getGridManager().getStatistics();
        sender.sendMessage(Component.text("Grid Mode: ", NamedTextColor.GRAY)
                .append(Component.text(plugin.getGridManager().getGridMode().toUpperCase(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Grid Slots: ", NamedTextColor.GRAY)
                .append(Component.text(gridStats.getOrDefault("occupied_slots", 0) + "/"
                        + gridStats.getOrDefault("total_slots", 0) + " used", NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("Slot Size: ", NamedTextColor.GRAY)
                .append(Component.text(plugin.getGridManager().getSlotSize() + " blocks", NamedTextColor.WHITE)));

        // Party stats
        Map<String, Integer> partyStats = plugin.getPartyManager().getStatistics();
        sender.sendMessage(Component.text("Active Parties: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(partyStats.getOrDefault("total_parties", 0)), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("Players in Parties: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(partyStats.getOrDefault("total_players", 0)), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("Pending Invitations: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(partyStats.getOrDefault("pending_invitations", 0)), NamedTextColor.YELLOW)));

        // Dungeon count
        sender.sendMessage(Component.text("Total Dungeons: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(plugin.getDungeonManager().getAllDungeons().size()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Enabled Dungeons: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(plugin.getDungeonManager().getEnabledDungeons().size()), NamedTextColor.GREEN)));

        // Quest and reward count
        sender.sendMessage(Component.text("Total Quests: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(plugin.getQuestManager().getAllQuests().size()), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Total Rewards: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(plugin.getRewardManager().getAllRewards().size()), NamedTextColor.WHITE)));

        // Integration status
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Integrations:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text(" Vault: ", NamedTextColor.GRAY)
                .append(plugin.getVaultEconomy() != null && plugin.getVaultEconomy().isEnabled()
                        ? Component.text("Enabled", NamedTextColor.GREEN)
                        : Component.text("Disabled", NamedTextColor.RED)));
        sender.sendMessage(Component.text(" WorldEdit: ", NamedTextColor.GRAY)
                .append(plugin.getWorldEditIntegration() != null && plugin.getWorldEditIntegration().isEnabled()
                        ? Component.text("Enabled", NamedTextColor.GREEN)
                        : Component.text("Disabled", NamedTextColor.RED)));
        sender.sendMessage(Component.text(" WorldGuard: ", NamedTextColor.GRAY)
                .append(plugin.getWorldGuardIntegration() != null && plugin.getWorldGuardIntegration().isEnabled()
                        ? Component.text("Enabled", NamedTextColor.GREEN)
                        : Component.text("Disabled", NamedTextColor.RED)));
    }

    // ==================== cleanup ====================

    private void handleCleanup(CommandSender sender) {
        if (!sender.hasPermission("dungeons.admin")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }

        int count = plugin.getInstanceManager().getActiveInstances().size();

        if (count == 0) {
            MessageUtil.sendRaw(sender, "&eNo active instances to clean up.");
            return;
        }

        plugin.getInstanceManager().cleanupAllInstances();
        MessageUtil.sendRaw(sender, "&aForce-cleaned &e" + count + " &aactive instance(s).");
    }

    // ==================== Tab Completion ====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "reset", "list", "teleport", "stats", "cleanup"));
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            switch (sub) {
                case "reset":
                    // Suggest online players and dungeon IDs
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        completions.add(online.getName());
                    }
                    completions.addAll(plugin.getDungeonManager().getDungeonIds());
                    break;
                case "teleport":
                case "tp":
                    // Suggest instance IDs (shortened)
                    for (DungeonInstance instance : plugin.getInstanceManager().getActiveInstances()) {
                        completions.add(instance.getInstanceId().toString().substring(0, 8));
                    }
                    break;
            }

            return filterCompletions(completions, args[1]);
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        String lower = input.toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .sorted()
                .collect(Collectors.toList());
    }
}
