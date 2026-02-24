package dev.bekololek.dungeons.commands;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.managers.StatsManager;
import dev.bekololek.dungeons.models.*;
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

public class DungeonCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public DungeonCommand(Main plugin) {
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
            case "list":
                handleList(sender);
                break;
            case "info":
                handleInfo(sender, args, label);
                break;
            case "join":
                handleJoin(sender, args, label);
                break;
            case "leave":
                handleLeave(sender);
                break;
            case "stats":
                handleStats(sender, args);
                break;
            default:
                sendUsage(sender, label);
                break;
        }

        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Component.text("--- Dungeons Commands ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("/" + label + " list", NamedTextColor.YELLOW)
                .append(Component.text(" - List available dungeons", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " info <dungeon>", NamedTextColor.YELLOW)
                .append(Component.text(" - View dungeon details", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " join <dungeon>", NamedTextColor.YELLOW)
                .append(Component.text(" - Start a dungeon", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " leave", NamedTextColor.YELLOW)
                .append(Component.text(" - Leave current dungeon", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " stats [player|top [stat]]", NamedTextColor.YELLOW)
                .append(Component.text(" - View statistics", NamedTextColor.GRAY)));
    }

    // ==================== list ====================

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("dungeons.enter")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }

        Collection<Dungeon> dungeons = plugin.getDungeonManager().getEnabledDungeons();

        if (dungeons.isEmpty()) {
            MessageUtil.sendRaw(sender, "&cNo dungeons are currently available.");
            return;
        }

        sender.sendMessage(Component.text("--- Available Dungeons ---", NamedTextColor.GOLD, TextDecoration.BOLD));

        for (Dungeon dungeon : dungeons) {
            String difficulty = dungeon.getDifficulty() != null ? dungeon.getDifficulty() : "Unknown";
            String partySize = dungeon.getMinPartySize() + "-" + dungeon.getMaxPartySize();

            Component line = Component.text(" ").append(ColorUtil.toComponent(dungeon.getDisplayName()))
                    .append(Component.text(" [" + difficulty + "]", NamedTextColor.GRAY))
                    .append(Component.text(" (" + partySize + " players)", NamedTextColor.DARK_GRAY));

            if (dungeon.getTimeLimit() > 0) {
                line = line.append(Component.text(" " + MessageUtil.formatTime(dungeon.getTimeLimit()), NamedTextColor.AQUA));
            }

            sender.sendMessage(line);
        }

        sender.sendMessage(Component.text("Use /dungeon info <id> for details.", NamedTextColor.GRAY));
    }

    // ==================== info ====================

    private void handleInfo(CommandSender sender, String[] args, String label) {
        if (args.length < 2) {
            MessageUtil.sendRaw(sender, "&cUsage: /" + label + " info <dungeon_id>");
            return;
        }

        String dungeonId = args[1].toLowerCase();
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);

        if (dungeon == null) {
            MessageUtil.sendMessage(sender, "dungeon.not-found",
                    MessageUtil.replacement("dungeon", dungeonId));
            return;
        }

        sender.sendMessage(Component.text("--- ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(ColorUtil.toComponent(dungeon.getDisplayName()))
                .append(Component.text(" ---", NamedTextColor.GOLD, TextDecoration.BOLD)));

        if (dungeon.getDescription() != null && !dungeon.getDescription().isEmpty()) {
            for (String line : dungeon.getDescription()) {
                MessageUtil.sendRaw(sender, line, false);
            }
        }

        sender.sendMessage(Component.text("ID: ", NamedTextColor.GRAY)
                .append(Component.text(dungeon.getId(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Difficulty: ", NamedTextColor.GRAY)
                .append(Component.text(dungeon.getDifficulty(), NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("Party Size: ", NamedTextColor.GRAY)
                .append(Component.text(dungeon.getMinPartySize() + " - " + dungeon.getMaxPartySize(), NamedTextColor.WHITE)));

        if (dungeon.getTimeLimit() > 0) {
            sender.sendMessage(Component.text("Time Limit: ", NamedTextColor.GRAY)
                    .append(Component.text(MessageUtil.formatTime(dungeon.getTimeLimit()), NamedTextColor.AQUA)));
        } else {
            sender.sendMessage(Component.text("Time Limit: ", NamedTextColor.GRAY)
                    .append(Component.text("None", NamedTextColor.GREEN)));
        }

        if (dungeon.getCooldown() > 0) {
            sender.sendMessage(Component.text("Cooldown: ", NamedTextColor.GRAY)
                    .append(Component.text(MessageUtil.formatTime(dungeon.getCooldown()), NamedTextColor.WHITE)));
        }

        if (dungeon.getEntryCost() > 0) {
            sender.sendMessage(Component.text("Entry Cost: ", NamedTextColor.GRAY)
                    .append(Component.text(MessageUtil.formatMoney(dungeon.getEntryCost()), NamedTextColor.GOLD)));
        }

        if (dungeon.getQuestIds() != null && !dungeon.getQuestIds().isEmpty()) {
            sender.sendMessage(Component.text("Quests: ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(dungeon.getQuestIds().size()), NamedTextColor.WHITE)));
        }

        sender.sendMessage(Component.text("Status: ", NamedTextColor.GRAY)
                .append(dungeon.isEnabled()
                        ? Component.text("Enabled", NamedTextColor.GREEN)
                        : Component.text("Disabled", NamedTextColor.RED)));

        // Show cooldown status for player
        if (sender instanceof Player player) {
            PlayerData data = plugin.getDatabaseManager().loadPlayerData(player.getUniqueId());
            if (data.hasCooldown(dungeonId)) {
                long remaining = data.getRemainingCooldown(dungeonId);
                sender.sendMessage(Component.text("Your Cooldown: ", NamedTextColor.GRAY)
                        .append(Component.text(MessageUtil.formatTime(remaining) + " remaining", NamedTextColor.RED)));
            }
        }
    }

    // ==================== join ====================

    private void handleJoin(CommandSender sender, String[] args, String label) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "general.players-only");
            return;
        }

        if (args.length < 2) {
            MessageUtil.sendRaw(sender, "&cUsage: /" + label + " join <dungeon_id>");
            return;
        }

        String dungeonId = args[1].toLowerCase();
        Dungeon dungeon = plugin.getDungeonManager().getDungeon(dungeonId);

        if (dungeon == null) {
            MessageUtil.sendMessage(sender, "dungeon.not-found",
                    MessageUtil.replacement("dungeon", dungeonId));
            return;
        }

        if (!dungeon.isEnabled()) {
            MessageUtil.sendMessage(sender, "dungeon.disabled");
            return;
        }

        // Permission check
        if (!player.hasPermission("dungeons.enter") && !player.hasPermission(dungeon.getPermission())) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }

        // Already in dungeon check
        if (plugin.getInstanceManager().isPlayerInDungeon(player.getUniqueId())) {
            MessageUtil.sendMessage(sender, "dungeon.already-in-dungeon");
            return;
        }

        // Party handling - create solo party if not in one
        Party party;
        if (plugin.getPartyManager().isInParty(player.getUniqueId())) {
            party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        } else {
            // Create a solo party for the player
            party = plugin.getPartyManager().createParty(player);
            if (party == null) {
                MessageUtil.sendRaw(sender, "&cFailed to create a party. Try again.");
                return;
            }
        }

        // Leader check
        if (!party.isLeader(player.getUniqueId())) {
            MessageUtil.sendMessage(sender, "dungeon.not-party-leader");
            return;
        }

        // Check if any party member is already in a dungeon
        for (UUID memberId : party.getMembers()) {
            if (plugin.getInstanceManager().isPlayerInDungeon(memberId)) {
                MessageUtil.sendMessage(sender, "dungeon.party-member-in-dungeon");
                return;
            }
        }

        // Party size check
        if (!player.hasPermission("dungeons.bypass.party-size")) {
            int partySize = party.getSize();
            if (partySize < dungeon.getMinPartySize()) {
                MessageUtil.sendMessage(sender, "dungeon.party-too-small",
                        MessageUtil.replacement("min", String.valueOf(dungeon.getMinPartySize())));
                return;
            }
            if (partySize > dungeon.getMaxPartySize()) {
                MessageUtil.sendMessage(sender, "dungeon.party-too-large",
                        MessageUtil.replacement("max", String.valueOf(dungeon.getMaxPartySize())));
                return;
            }
        }

        // Cooldown check
        if (!player.hasPermission("dungeons.bypass.cooldown")) {
            PlayerData data = plugin.getDatabaseManager().loadPlayerData(player.getUniqueId());
            if (data.hasCooldown(dungeonId)) {
                long remaining = data.getRemainingCooldown(dungeonId);
                MessageUtil.sendMessage(sender, "dungeon.on-cooldown",
                        MessageUtil.replacement("time", MessageUtil.formatTime(remaining)));
                return;
            }
        }

        // Entry cost check
        if (dungeon.getEntryCost() > 0 && plugin.getVaultEconomy() != null && plugin.getVaultEconomy().isEnabled()) {
            if (!plugin.getVaultEconomy().has(player, dungeon.getEntryCost())) {
                MessageUtil.sendMessage(sender, "dungeon.insufficient-funds",
                        MessageUtil.replacement("cost", MessageUtil.formatMoney(dungeon.getEntryCost())));
                return;
            }

            // Withdraw entry cost
            plugin.getVaultEconomy().withdraw(player, dungeon.getEntryCost());
            MessageUtil.sendMessage(sender, "dungeon.entry-cost-charged",
                    MessageUtil.replacement("cost", MessageUtil.formatMoney(dungeon.getEntryCost())));
        }

        // Create instance
        DungeonInstance instance = plugin.getInstanceManager().createInstance(dungeon, party);
        if (instance == null) {
            // Refund entry cost if instance creation failed
            if (dungeon.getEntryCost() > 0 && plugin.getVaultEconomy() != null && plugin.getVaultEconomy().isEnabled()) {
                plugin.getVaultEconomy().deposit(player, dungeon.getEntryCost());
            }
            MessageUtil.sendMessage(sender, "dungeon.creation-failed");
            return;
        }

        // Notify all party members
        for (Player member : party.getOnlinePlayers()) {
            MessageUtil.sendMessage(member, "dungeon.joining",
                    MessageUtil.replacement("dungeon", dungeon.getDisplayName()));
        }
    }

    // ==================== leave ====================

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "general.players-only");
            return;
        }

        if (!plugin.getInstanceManager().isPlayerInDungeon(player.getUniqueId())) {
            MessageUtil.sendMessage(sender, "dungeon.not-in-dungeon");
            return;
        }

        plugin.getInstanceManager().handlePlayerLeave(player);
    }

    // ==================== stats ====================

    private void handleStats(CommandSender sender, String[] args) {
        StatsManager statsManager = plugin.getStatsManager();

        // /dungeon stats top [stat]
        if (args.length >= 2 && args[1].equalsIgnoreCase("top")) {
            if (!sender.hasPermission("dungeons.stats")) {
                MessageUtil.sendMessage(sender, "general.no-permission");
                return;
            }

            String statName = args.length >= 3 ? args[2].toLowerCase() : null;

            if (statName == null) {
                // Show available leaderboard stats
                List<String> available = statsManager.leaderboardStats();
                sender.sendMessage(Component.text("--- Leaderboard Stats ---", NamedTextColor.GOLD, TextDecoration.BOLD));
                for (String stat : available) {
                    sender.sendMessage(Component.text(" " + stat, NamedTextColor.YELLOW)
                            .append(Component.text(" - " + statsManager.statLabel(stat), NamedTextColor.GRAY)));
                }
                sender.sendMessage(Component.text("Use /dungeon stats top <stat> to view.", NamedTextColor.GRAY));
                return;
            }

            List<?> topPlayers = statsManager.getTopPlayers(statName, 10);
            if (topPlayers == null || topPlayers.isEmpty()) {
                MessageUtil.sendRaw(sender, "&cNo data available for stat: &e" + statName);
                return;
            }

            sender.sendMessage(Component.text("--- Top " + statsManager.statLabel(statName) + " ---",
                    NamedTextColor.GOLD, TextDecoration.BOLD));

            int rank = 1;
            for (Object entry : topPlayers) {
                NamedTextColor rankColor;
                if (rank == 1) rankColor = NamedTextColor.GOLD;
                else if (rank == 2) rankColor = NamedTextColor.GRAY;
                else if (rank == 3) rankColor = NamedTextColor.DARK_RED;
                else rankColor = NamedTextColor.WHITE;

                sender.sendMessage(Component.text("#" + rank + " ", rankColor)
                        .append(Component.text(entry.toString(), NamedTextColor.YELLOW)));
                rank++;
            }
            return;
        }

        // /dungeon stats <player>
        if (args.length >= 2 && !args[1].equalsIgnoreCase("top")) {
            if (!sender.hasPermission("dungeons.stats.others")) {
                MessageUtil.sendMessage(sender, "general.no-permission");
                return;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                MessageUtil.sendMessage(sender, "general.player-not-found",
                        MessageUtil.replacement("player", args[1]));
                return;
            }

            showPlayerStats(sender, target);
            return;
        }

        // /dungeon stats (own stats)
        if (!sender.hasPermission("dungeons.stats")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return;
        }

        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "general.players-only");
            return;
        }

        showPlayerStats(sender, player);
    }

    private void showPlayerStats(CommandSender sender, Player target) {
        StatsManager statsManager = plugin.getStatsManager();

        sender.sendMessage(Component.text("--- " + target.getName() + "'s Dungeon Stats ---",
                NamedTextColor.GOLD, TextDecoration.BOLD));

        List<String> statNames = statsManager.leaderboardStats();
        for (String statName : statNames) {
            Object value = statsManager.getPlayerStat(target.getUniqueId(), statName);
            String label = statsManager.statLabel(statName);
            String displayValue = value != null ? value.toString() : "0";

            sender.sendMessage(Component.text(" " + label + ": ", NamedTextColor.GRAY)
                    .append(Component.text(displayValue, NamedTextColor.YELLOW)));
        }

        // Show global highlights
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text("Global Highlights:", NamedTextColor.GOLD));

        for (String statName : statNames) {
            Object globalValue = statsManager.getGlobalStat(statName);
            String label = statsManager.statLabel(statName);
            String displayValue = globalValue != null ? globalValue.toString() : "0";

            sender.sendMessage(Component.text(" " + label + ": ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(displayValue, NamedTextColor.WHITE)));
        }
    }

    // ==================== Tab Completion ====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "info", "join", "leave", "stats"));
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            switch (sub) {
                case "info":
                case "join":
                    for (String id : plugin.getDungeonManager().getDungeonIds()) {
                        completions.add(id);
                    }
                    break;
                case "stats":
                    completions.add("top");
                    if (sender.hasPermission("dungeons.stats.others")) {
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            completions.add(online.getName());
                        }
                    }
                    break;
            }

            return filterCompletions(completions, args[1]);
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();

            if (sub.equals("stats") && args[1].equalsIgnoreCase("top")) {
                completions.addAll(plugin.getStatsManager().leaderboardStats());
                return filterCompletions(completions, args[2]);
            }
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
