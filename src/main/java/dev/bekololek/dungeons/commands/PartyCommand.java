package dev.bekololek.dungeons.commands;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.models.DungeonInstance;
import dev.bekololek.dungeons.models.Party;
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

public class PartyCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public PartyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "general.players-only");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create":
                handleCreate(player);
                break;
            case "invite":
                handleInvite(player, args, label);
                break;
            case "accept":
                handleAccept(player);
                break;
            case "decline":
                handleDecline(player);
                break;
            case "kick":
                handleKick(player, args, label);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "disband":
                handleDisband(player);
                break;
            case "list":
                handleList(player);
                break;
            default:
                sendUsage(sender, label);
                break;
        }

        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Component.text("--- Party Commands ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        sender.sendMessage(Component.text("/" + label + " create", NamedTextColor.YELLOW)
                .append(Component.text(" - Create a new party", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " invite <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - Invite a player", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " accept", NamedTextColor.YELLOW)
                .append(Component.text(" - Accept an invitation", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " decline", NamedTextColor.YELLOW)
                .append(Component.text(" - Decline an invitation", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " kick <player>", NamedTextColor.YELLOW)
                .append(Component.text(" - Kick a member", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " leave", NamedTextColor.YELLOW)
                .append(Component.text(" - Leave your party", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " disband", NamedTextColor.YELLOW)
                .append(Component.text(" - Disband your party", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/" + label + " list", NamedTextColor.YELLOW)
                .append(Component.text(" - Show party members", NamedTextColor.GRAY)));
    }

    // ==================== create ====================

    private void handleCreate(Player player) {
        if (plugin.getPartyManager().isInParty(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "party.already-in-party");
            return;
        }

        Party party = plugin.getPartyManager().createParty(player);
        if (party == null) {
            MessageUtil.sendRaw(player, "&cFailed to create party.");
            return;
        }

        MessageUtil.sendMessage(player, "party.created");
    }

    // ==================== invite ====================

    private void handleInvite(Player player, String[] args, String label) {
        if (args.length < 2) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " invite <player>");
            return;
        }

        if (!plugin.getPartyManager().isInParty(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "party.not-in-party");
            return;
        }

        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            MessageUtil.sendMessage(player, "party.not-in-party");
            return;
        }

        if (!party.isLeader(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "party.not-leader");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            MessageUtil.sendMessage(player, "general.player-not-found",
                    MessageUtil.replacement("player", args[1]));
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            MessageUtil.sendRaw(player, "&cYou cannot invite yourself.");
            return;
        }

        if (plugin.getPartyManager().isInParty(target.getUniqueId())) {
            MessageUtil.sendMessage(player, "party.target-already-in-party",
                    MessageUtil.replacement("player", target.getName()));
            return;
        }

        if (party.getSize() >= plugin.getPartyManager().getMaxPartySize()) {
            MessageUtil.sendMessage(player, "party.full");
            return;
        }

        boolean success = plugin.getPartyManager().invitePlayer(party, player, target);
        if (!success) {
            MessageUtil.sendRaw(player, "&cFailed to invite player. They may already have a pending invitation.");
            return;
        }

        MessageUtil.sendMessage(player, "party.invite-sent",
                MessageUtil.replacement("player", target.getName()));
        MessageUtil.sendMessage(target, "party.invite-received",
                MessageUtil.replacement("player", player.getName()));
    }

    // ==================== accept ====================

    private void handleAccept(Player player) {
        if (plugin.getPartyManager().isInParty(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "party.already-in-party");
            return;
        }

        UUID partyId = plugin.getPartyManager().getMostRecentInvitation(player.getUniqueId());
        if (partyId == null) {
            MessageUtil.sendMessage(player, "party.no-invitations");
            return;
        }

        Party party = plugin.getPartyManager().getParty(partyId);
        if (party == null) {
            MessageUtil.sendMessage(player, "party.invite-expired");
            return;
        }

        boolean success = plugin.getPartyManager().acceptInvitation(player, partyId);
        if (!success) {
            MessageUtil.sendMessage(player, "party.accept-failed");
            return;
        }

        MessageUtil.sendMessage(player, "party.joined",
                MessageUtil.replacement("player", player.getName()));

        // Notify other party members
        for (Player member : party.getOnlinePlayers()) {
            if (!member.getUniqueId().equals(player.getUniqueId())) {
                MessageUtil.sendMessage(member, "party.player-joined",
                        MessageUtil.replacement("player", player.getName()));
            }
        }
    }

    // ==================== decline ====================

    private void handleDecline(Player player) {
        UUID partyId = plugin.getPartyManager().getMostRecentInvitation(player.getUniqueId());
        if (partyId == null) {
            MessageUtil.sendMessage(player, "party.no-invitations");
            return;
        }

        plugin.getPartyManager().removeInvitation(player.getUniqueId(), partyId);
        MessageUtil.sendMessage(player, "party.invite-declined");

        // Notify the party leader
        Party party = plugin.getPartyManager().getParty(partyId);
        if (party != null) {
            Player leader = Bukkit.getPlayer(party.getLeaderId());
            if (leader != null) {
                MessageUtil.sendMessage(leader, "party.invite-was-declined",
                        MessageUtil.replacement("player", player.getName()));
            }
        }
    }

    // ==================== kick ====================

    private void handleKick(Player player, String[] args, String label) {
        if (args.length < 2) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " kick <player>");
            return;
        }

        if (!plugin.getPartyManager().isInParty(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "party.not-in-party");
            return;
        }

        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            MessageUtil.sendMessage(player, "party.not-in-party");
            return;
        }

        if (!party.isLeader(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "party.not-leader");
            return;
        }

        // Find the target by name among party members
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            MessageUtil.sendMessage(player, "general.player-not-found",
                    MessageUtil.replacement("player", args[1]));
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            MessageUtil.sendRaw(player, "&cYou cannot kick yourself. Use /party leave instead.");
            return;
        }

        if (!party.isMember(target.getUniqueId())) {
            MessageUtil.sendMessage(player, "party.not-a-member",
                    MessageUtil.replacement("player", target.getName()));
            return;
        }

        boolean success = plugin.getPartyManager().kickPlayer(party, target.getUniqueId());
        if (!success) {
            MessageUtil.sendRaw(player, "&cFailed to kick player.");
            return;
        }

        MessageUtil.sendMessage(player, "party.player-kicked",
                MessageUtil.replacement("player", target.getName()));
        MessageUtil.sendMessage(target, "party.you-were-kicked",
                MessageUtil.replacement("player", player.getName()));

        // Notify remaining party members
        for (Player member : party.getOnlinePlayers()) {
            if (!member.getUniqueId().equals(player.getUniqueId()) && !member.getUniqueId().equals(target.getUniqueId())) {
                MessageUtil.sendMessage(member, "party.player-kicked",
                        MessageUtil.replacement("player", target.getName()));
            }
        }
    }

    // ==================== leave ====================

    private void handleLeave(Player player) {
        if (!plugin.getPartyManager().isInParty(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "party.not-in-party");
            return;
        }

        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            MessageUtil.sendMessage(player, "party.not-in-party");
            return;
        }

        // Check if the player is in a dungeon
        if (plugin.getInstanceManager().isPlayerInDungeon(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "party.cannot-leave-in-dungeon");
            return;
        }

        // Notify other members before leaving
        for (Player member : party.getOnlinePlayers()) {
            if (!member.getUniqueId().equals(player.getUniqueId())) {
                MessageUtil.sendMessage(member, "party.player-left",
                        MessageUtil.replacement("player", player.getName()));
            }
        }

        boolean success = plugin.getPartyManager().leaveParty(player);
        if (!success) {
            MessageUtil.sendRaw(player, "&cFailed to leave party.");
            return;
        }

        MessageUtil.sendMessage(player, "party.left");
    }

    // ==================== disband ====================

    private void handleDisband(Player player) {
        if (!plugin.getPartyManager().isInParty(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "party.not-in-party");
            return;
        }

        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            MessageUtil.sendMessage(player, "party.not-in-party");
            return;
        }

        if (!party.isLeader(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "party.not-leader");
            return;
        }

        // Check if any member is in a dungeon
        for (UUID memberId : party.getMembers()) {
            if (plugin.getInstanceManager().isPlayerInDungeon(memberId)) {
                MessageUtil.sendMessage(player, "party.cannot-disband-in-dungeon");
                return;
            }
        }

        // Notify all members
        for (Player member : party.getOnlinePlayers()) {
            if (!member.getUniqueId().equals(player.getUniqueId())) {
                MessageUtil.sendMessage(member, "party.disbanded",
                        MessageUtil.replacement("player", player.getName()));
            }
        }

        boolean success = plugin.getPartyManager().disbandParty(party);
        if (!success) {
            MessageUtil.sendRaw(player, "&cFailed to disband party.");
            return;
        }

        MessageUtil.sendMessage(player, "party.you-disbanded");
    }

    // ==================== list ====================

    private void handleList(Player player) {
        if (!plugin.getPartyManager().isInParty(player.getUniqueId())) {
            MessageUtil.sendMessage(player, "party.not-in-party");
            return;
        }

        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
        if (party == null) {
            MessageUtil.sendMessage(player, "party.not-in-party");
            return;
        }

        player.sendMessage(Component.text("--- Party Members (" + party.getSize() + "/" + plugin.getPartyManager().getMaxPartySize() + ") ---",
                NamedTextColor.GOLD, TextDecoration.BOLD));

        for (UUID memberId : party.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            String name = member != null ? member.getName() : memberId.toString();
            boolean isLeader = party.isLeader(memberId);
            boolean isOnline = member != null && member.isOnline();

            Component line = Component.text(" " + name, isOnline ? NamedTextColor.GREEN : NamedTextColor.GRAY);
            if (isLeader) {
                line = line.append(Component.text(" [Leader]", NamedTextColor.GOLD));
            }
            if (!isOnline) {
                line = line.append(Component.text(" [Offline]", NamedTextColor.RED));
            }

            player.sendMessage(line);
        }

        // Show if party is in a dungeon
        if (plugin.getInstanceManager().isPlayerInDungeon(player.getUniqueId())) {
            DungeonInstance instance = plugin.getInstanceManager().getPlayerInstance(player.getUniqueId());
            if (instance != null) {
                player.sendMessage(Component.text("Dungeon: ", NamedTextColor.GRAY)
                        .append(ColorUtil.toComponent(instance.getDungeon().getDisplayName())));
                if (instance.hasTimeLimit()) {
                    player.sendMessage(Component.text("Time Remaining: ", NamedTextColor.GRAY)
                            .append(Component.text(MessageUtil.formatTime(instance.getRemainingTime()), NamedTextColor.AQUA)));
                }
            }
        }
    }

    // ==================== Tab Completion ====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "invite", "accept", "decline", "kick", "leave", "disband", "list"));
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            switch (sub) {
                case "invite":
                    // Suggest online players not in a party
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!plugin.getPartyManager().isInParty(online.getUniqueId())
                                && !online.getUniqueId().equals(((Player) sender).getUniqueId())) {
                            completions.add(online.getName());
                        }
                    }
                    break;
                case "kick":
                    // Suggest party members (excluding the sender)
                    if (sender instanceof Player player && plugin.getPartyManager().isInParty(player.getUniqueId())) {
                        Party party = plugin.getPartyManager().getPlayerParty(player.getUniqueId());
                        if (party != null) {
                            for (UUID memberId : party.getMembers()) {
                                if (!memberId.equals(player.getUniqueId())) {
                                    Player member = Bukkit.getPlayer(memberId);
                                    if (member != null) {
                                        completions.add(member.getName());
                                    }
                                }
                            }
                        }
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
