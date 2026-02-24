package dev.bekololek.dungeons.commands;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.models.*;
import dev.bekololek.dungeons.utils.ColorUtil;
import dev.bekololek.dungeons.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class DungeonEditorCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final Map<UUID, DungeonBuilder> sessions;
    private final Map<UUID, Location> pos1Map;
    private final Map<UUID, Location> pos2Map;
    private final Map<UUID, Clipboard> clipboardMap;

    public DungeonEditorCommand(Main plugin) {
        this.plugin = plugin;
        this.sessions = new HashMap<>();
        this.pos1Map = new HashMap<>();
        this.pos2Map = new HashMap<>();
        this.clipboardMap = new HashMap<>();
    }

    public static class DungeonBuilder {
        public String id, displayName, schematicFile, difficulty;
        public int minPartySize, maxPartySize, timeLimit;
        public double spawnOffsetX, spawnOffsetY, spawnOffsetZ;
        public List<String> quests, rewards;
        public long cooldown;
        public double entryCost;
        public boolean enabled;
        public Map<String, Trigger> triggers;

        public DungeonBuilder(String id) {
            this.id = id;
            this.displayName = id;
            this.schematicFile = id + ".schem";
            this.difficulty = "MEDIUM";
            this.minPartySize = 1;
            this.maxPartySize = 5;
            this.timeLimit = 1200;
            this.spawnOffsetX = 0.5;
            this.spawnOffsetY = 1.0;
            this.spawnOffsetZ = 0.5;
            this.quests = new ArrayList<>();
            this.rewards = new ArrayList<>();
            this.cooldown = 3600;
            this.entryCost = 0;
            this.enabled = false;
            this.triggers = new HashMap<>();
        }

        public DungeonBuilder(Dungeon dungeon) {
            this.id = dungeon.getId();
            this.displayName = dungeon.getDisplayName();
            this.schematicFile = dungeon.getSchematicFile();
            this.difficulty = dungeon.getDifficulty();
            this.minPartySize = dungeon.getMinPartySize();
            this.maxPartySize = dungeon.getMaxPartySize();
            this.timeLimit = dungeon.getTimeLimit();
            this.spawnOffsetX = dungeon.getSpawnOffsetX();
            this.spawnOffsetY = dungeon.getSpawnOffsetY();
            this.spawnOffsetZ = dungeon.getSpawnOffsetZ();
            this.quests = new ArrayList<>(dungeon.getQuestIds());
            this.rewards = new ArrayList<>(dungeon.getRewardTables());
            this.cooldown = dungeon.getCooldown();
            this.entryCost = dungeon.getEntryCost();
            this.enabled = dungeon.isEnabled();
            this.triggers = dungeon.getTriggersMap();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendMessage(sender, "general.players-only");
            return true;
        }

        if (!player.hasPermission("dungeons.editor")) {
            MessageUtil.sendMessage(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create":
                handleCreate(player, args, label);
                break;
            case "edit":
                handleEdit(player, args, label);
                break;
            case "save":
                handleSave(player);
                break;
            case "cancel":
                handleCancel(player);
                break;
            case "info":
                handleInfo(player, args, label);
                break;
            case "list":
                handleList(player);
                break;
            case "set":
                handleSet(player, args, label);
                break;
            case "add":
                handleAdd(player, args, label);
                break;
            case "remove":
                handleRemove(player, args, label);
                break;
            case "quest":
                handleQuest(player, args, label);
                break;
            case "reward":
                handleReward(player, args, label);
                break;
            case "schematic":
                handleSchematic(player, args, label);
                break;
            case "trigger":
                handleTrigger(player, args, label);
                break;
            case "mobs":
                handleMobs(player, args, label);
                break;
            default:
                sendUsage(player, label);
                break;
        }

        return true;
    }

    private void sendUsage(Player player, String label) {
        player.sendMessage(Component.text("--- Dungeon Editor ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("/" + label + " create <id>", NamedTextColor.YELLOW)
                .append(Component.text(" - Create new dungeon", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + label + " edit <id>", NamedTextColor.YELLOW)
                .append(Component.text(" - Edit existing dungeon", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + label + " save", NamedTextColor.YELLOW)
                .append(Component.text(" - Save current session", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + label + " cancel", NamedTextColor.YELLOW)
                .append(Component.text(" - Cancel session", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + label + " info [id]", NamedTextColor.YELLOW)
                .append(Component.text(" - View dungeon info", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + label + " list", NamedTextColor.YELLOW)
                .append(Component.text(" - List all dungeons", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + label + " set <property> <value>", NamedTextColor.YELLOW)
                .append(Component.text(" - Set a property", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + label + " add <quest|reward|trigger>", NamedTextColor.YELLOW)
                .append(Component.text(" - Add to lists", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + label + " remove <quest|reward|trigger> <id>", NamedTextColor.YELLOW)
                .append(Component.text(" - Remove from lists", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + label + " quest <create|set|list|delete>", NamedTextColor.YELLOW)
                .append(Component.text(" - Manage quests", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + label + " reward <add|set|view|list>", NamedTextColor.YELLOW)
                .append(Component.text(" - Manage rewards", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + label + " schematic <paste|confirm|pos1|pos2|save>", NamedTextColor.YELLOW)
                .append(Component.text(" - Schematic tools", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + label + " trigger <set|edit|list|add>", NamedTextColor.YELLOW)
                .append(Component.text(" - Manage triggers", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/" + label + " mobs <create|edit|remove|list>", NamedTextColor.YELLOW)
                .append(Component.text(" - Manage custom mobs", NamedTextColor.GRAY)));
    }

    private DungeonBuilder getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    private boolean requireSession(Player player) {
        if (!sessions.containsKey(player.getUniqueId())) {
            MessageUtil.sendRaw(player, "&cNo active editor session. Use /dng create <id> or /dng edit <id>.");
            return false;
        }
        return true;
    }

    // ==================== create ====================

    private void handleCreate(Player player, String[] args, String label) {
        if (args.length < 2) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " create <id>");
            return;
        }

        String id = args[1].toLowerCase();

        if (plugin.getDungeonManager().dungeonExists(id)) {
            MessageUtil.sendRaw(player, "&cDungeon '" + id + "' already exists. Use /dng edit " + id + " instead.");
            return;
        }

        if (sessions.containsKey(player.getUniqueId())) {
            MessageUtil.sendRaw(player, "&cYou already have an active session. Use /dng save or /dng cancel first.");
            return;
        }

        DungeonBuilder builder = new DungeonBuilder(id);
        sessions.put(player.getUniqueId(), builder);

        MessageUtil.sendRaw(player, "&aCreated new dungeon editor session for '&e" + id + "&a'.");
        MessageUtil.sendRaw(player, "&7Use /dng set <property> <value> to configure, /dng save to save.");
    }

    // ==================== edit ====================

    private void handleEdit(Player player, String[] args, String label) {
        if (args.length < 2) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " edit <id>");
            return;
        }

        String id = args[1].toLowerCase();

        if (!plugin.getDungeonManager().dungeonExists(id)) {
            MessageUtil.sendRaw(player, "&cDungeon '" + id + "' does not exist.");
            return;
        }

        if (sessions.containsKey(player.getUniqueId())) {
            MessageUtil.sendRaw(player, "&cYou already have an active session. Use /dng save or /dng cancel first.");
            return;
        }

        Dungeon dungeon = plugin.getDungeonManager().getDungeon(id);
        DungeonBuilder builder = new DungeonBuilder(dungeon);
        sessions.put(player.getUniqueId(), builder);

        MessageUtil.sendRaw(player, "&aLoaded dungeon '&e" + id + "&a' into editor session.");
        MessageUtil.sendRaw(player, "&7Use /dng set <property> <value> to modify, /dng save to save.");
    }

    // ==================== save ====================

    private void handleSave(Player player) {
        if (!requireSession(player)) return;

        DungeonBuilder builder = getSession(player);

        boolean success = plugin.getDungeonManager().saveDungeon(builder);
        if (!success) {
            MessageUtil.sendRaw(player, "&cFailed to save dungeon. Check console for errors.");
            return;
        }

        // Reload to pick up changes
        plugin.getDungeonManager().loadDungeons();

        sessions.remove(player.getUniqueId());
        pos1Map.remove(player.getUniqueId());
        pos2Map.remove(player.getUniqueId());
        clipboardMap.remove(player.getUniqueId());

        MessageUtil.sendRaw(player, "&aDungeon '&e" + builder.id + "&a' saved and reloaded successfully.");
    }

    // ==================== cancel ====================

    private void handleCancel(Player player) {
        if (!requireSession(player)) return;

        DungeonBuilder builder = getSession(player);
        sessions.remove(player.getUniqueId());
        pos1Map.remove(player.getUniqueId());
        pos2Map.remove(player.getUniqueId());
        clipboardMap.remove(player.getUniqueId());

        MessageUtil.sendRaw(player, "&eEditor session for '&6" + builder.id + "&e' cancelled. Changes discarded.");
    }

    // ==================== info ====================

    private void handleInfo(Player player, String[] args, String label) {
        DungeonBuilder builder;

        if (args.length >= 2) {
            String id = args[1].toLowerCase();
            Dungeon dungeon = plugin.getDungeonManager().getDungeon(id);
            if (dungeon == null) {
                MessageUtil.sendRaw(player, "&cDungeon '" + id + "' not found.");
                return;
            }
            builder = new DungeonBuilder(dungeon);
        } else {
            if (!requireSession(player)) return;
            builder = getSession(player);
        }

        player.sendMessage(Component.text("--- Dungeon: " + builder.id + " ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("Display Name: ", NamedTextColor.GRAY).append(Component.text(builder.displayName, NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Schematic: ", NamedTextColor.GRAY).append(Component.text(builder.schematicFile, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Difficulty: ", NamedTextColor.GRAY).append(Component.text(builder.difficulty, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Party Size: ", NamedTextColor.GRAY).append(Component.text(builder.minPartySize + " - " + builder.maxPartySize, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Time Limit: ", NamedTextColor.GRAY).append(Component.text(MessageUtil.formatTime(builder.timeLimit), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Spawn Offset: ", NamedTextColor.GRAY)
                .append(Component.text(builder.spawnOffsetX + ", " + builder.spawnOffsetY + ", " + builder.spawnOffsetZ, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Cooldown: ", NamedTextColor.GRAY).append(Component.text(MessageUtil.formatTime(builder.cooldown), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Entry Cost: ", NamedTextColor.GRAY).append(Component.text(MessageUtil.formatMoney(builder.entryCost), NamedTextColor.GOLD)));
        player.sendMessage(Component.text("Enabled: ", NamedTextColor.GRAY).append(builder.enabled
                ? Component.text("Yes", NamedTextColor.GREEN) : Component.text("No", NamedTextColor.RED)));
        player.sendMessage(Component.text("Quests: ", NamedTextColor.GRAY).append(Component.text(builder.quests.toString(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Rewards: ", NamedTextColor.GRAY).append(Component.text(builder.rewards.toString(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Triggers: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(builder.triggers.size()), NamedTextColor.WHITE)));
    }

    // ==================== list ====================

    private void handleList(Player player) {
        Set<String> ids = plugin.getDungeonManager().getDungeonIds();

        if (ids.isEmpty()) {
            MessageUtil.sendRaw(player, "&eNo dungeons configured.");
            return;
        }

        player.sendMessage(Component.text("--- All Dungeons ---", NamedTextColor.GOLD, TextDecoration.BOLD));

        for (String id : ids) {
            Dungeon dungeon = plugin.getDungeonManager().getDungeon(id);
            Component line = Component.text(" " + id, NamedTextColor.YELLOW)
                    .append(Component.text(" - ")).append(ColorUtil.toComponent(dungeon.getDisplayName()))
                    .append(dungeon.isEnabled()
                            ? Component.text(" [Enabled]", NamedTextColor.GREEN)
                            : Component.text(" [Disabled]", NamedTextColor.RED));
            player.sendMessage(line);
        }
    }

    // ==================== set ====================

    private void handleSet(Player player, String[] args, String label) {
        if (!requireSession(player)) return;

        if (args.length < 3) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " set <property> <value>");
            MessageUtil.sendRaw(player, "&7Properties: displayname, schematic, difficulty, minparty, maxparty, timelimit, cooldown, entrycost, enabled, spawnoffsetx, spawnoffsety, spawnoffsetz");
            return;
        }

        DungeonBuilder builder = getSession(player);
        String property = args[1].toLowerCase();
        String value = joinArgs(args, 2);

        try {
            switch (property) {
                case "displayname":
                case "name":
                    builder.displayName = value;
                    MessageUtil.sendRaw(player, "&aSet display name to: &e" + value);
                    break;
                case "schematic":
                    builder.schematicFile = value;
                    MessageUtil.sendRaw(player, "&aSet schematic file to: &e" + value);
                    break;
                case "difficulty":
                    builder.difficulty = value.toUpperCase();
                    MessageUtil.sendRaw(player, "&aSet difficulty to: &e" + value.toUpperCase());
                    break;
                case "minparty":
                    builder.minPartySize = Integer.parseInt(value);
                    MessageUtil.sendRaw(player, "&aSet min party size to: &e" + builder.minPartySize);
                    break;
                case "maxparty":
                    builder.maxPartySize = Integer.parseInt(value);
                    MessageUtil.sendRaw(player, "&aSet max party size to: &e" + builder.maxPartySize);
                    break;
                case "timelimit":
                    builder.timeLimit = Integer.parseInt(value);
                    MessageUtil.sendRaw(player, "&aSet time limit to: &e" + MessageUtil.formatTime(builder.timeLimit));
                    break;
                case "cooldown":
                    builder.cooldown = Long.parseLong(value);
                    MessageUtil.sendRaw(player, "&aSet cooldown to: &e" + MessageUtil.formatTime(builder.cooldown));
                    break;
                case "entrycost":
                    builder.entryCost = Double.parseDouble(value);
                    MessageUtil.sendRaw(player, "&aSet entry cost to: &e" + MessageUtil.formatMoney(builder.entryCost));
                    break;
                case "enabled":
                    builder.enabled = Boolean.parseBoolean(value);
                    MessageUtil.sendRaw(player, "&aSet enabled to: &e" + builder.enabled);
                    break;
                case "spawnoffsetx":
                    builder.spawnOffsetX = Double.parseDouble(value);
                    MessageUtil.sendRaw(player, "&aSet spawn offset X to: &e" + builder.spawnOffsetX);
                    break;
                case "spawnoffsety":
                    builder.spawnOffsetY = Double.parseDouble(value);
                    MessageUtil.sendRaw(player, "&aSet spawn offset Y to: &e" + builder.spawnOffsetY);
                    break;
                case "spawnoffsetz":
                    builder.spawnOffsetZ = Double.parseDouble(value);
                    MessageUtil.sendRaw(player, "&aSet spawn offset Z to: &e" + builder.spawnOffsetZ);
                    break;
                default:
                    MessageUtil.sendRaw(player, "&cUnknown property: &e" + property);
                    MessageUtil.sendRaw(player, "&7Properties: displayname, schematic, difficulty, minparty, maxparty, timelimit, cooldown, entrycost, enabled, spawnoffsetx, spawnoffsety, spawnoffsetz");
                    break;
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendRaw(player, "&cInvalid number: &e" + value);
        }
    }

    // ==================== add ====================

    private void handleAdd(Player player, String[] args, String label) {
        if (!requireSession(player)) return;

        if (args.length < 3) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " add <quest|reward|trigger> <id>");
            return;
        }

        DungeonBuilder builder = getSession(player);
        String type = args[1].toLowerCase();
        String id = args[2].toLowerCase();

        switch (type) {
            case "quest":
                if (builder.quests.contains(id)) {
                    MessageUtil.sendRaw(player, "&cQuest '" + id + "' is already added.");
                    return;
                }
                builder.quests.add(id);
                MessageUtil.sendRaw(player, "&aAdded quest: &e" + id);
                break;
            case "reward":
                if (builder.rewards.contains(id)) {
                    MessageUtil.sendRaw(player, "&cReward '" + id + "' is already added.");
                    return;
                }
                builder.rewards.add(id);
                MessageUtil.sendRaw(player, "&aAdded reward table: &e" + id);
                break;
            case "trigger":
                if (args.length < 4) {
                    MessageUtil.sendRaw(player, "&cUsage: /" + label + " add trigger <id> <type>");
                    MessageUtil.sendRaw(player, "&7Types: LOCATION, TIMER, MOB_KILL, QUEST_COMPLETE, PLAYER_DEATH, BOSS_KILL");
                    return;
                }
                String triggerTypeStr = args[3].toUpperCase();
                try {
                    Trigger.TriggerType triggerType = Trigger.TriggerType.valueOf(triggerTypeStr);
                    Trigger trigger = new Trigger(id, triggerType);
                    builder.triggers.put(id, trigger);
                    MessageUtil.sendRaw(player, "&aAdded trigger '&e" + id + "&a' of type &e" + triggerTypeStr);
                } catch (IllegalArgumentException e) {
                    MessageUtil.sendRaw(player, "&cInvalid trigger type: &e" + triggerTypeStr);
                }
                break;
            default:
                MessageUtil.sendRaw(player, "&cUsage: /" + label + " add <quest|reward|trigger> <id>");
                break;
        }
    }

    // ==================== remove ====================

    private void handleRemove(Player player, String[] args, String label) {
        if (!requireSession(player)) return;

        if (args.length < 3) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " remove <quest|reward|trigger> <id>");
            return;
        }

        DungeonBuilder builder = getSession(player);
        String type = args[1].toLowerCase();
        String id = args[2].toLowerCase();

        switch (type) {
            case "quest":
                if (builder.quests.remove(id)) {
                    MessageUtil.sendRaw(player, "&aRemoved quest: &e" + id);
                } else {
                    MessageUtil.sendRaw(player, "&cQuest '" + id + "' not found.");
                }
                break;
            case "reward":
                if (builder.rewards.remove(id)) {
                    MessageUtil.sendRaw(player, "&aRemoved reward table: &e" + id);
                } else {
                    MessageUtil.sendRaw(player, "&cReward '" + id + "' not found.");
                }
                break;
            case "trigger":
                if (builder.triggers.remove(id) != null) {
                    MessageUtil.sendRaw(player, "&aRemoved trigger: &e" + id);
                } else {
                    MessageUtil.sendRaw(player, "&cTrigger '" + id + "' not found.");
                }
                break;
            default:
                MessageUtil.sendRaw(player, "&cUsage: /" + label + " remove <quest|reward|trigger> <id>");
                break;
        }
    }

    // ==================== quest ====================

    private void handleQuest(Player player, String[] args, String label) {
        if (args.length < 2) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " quest <create|set|list|delete> ...");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "create":
                handleQuestCreate(player, args, label);
                break;
            case "set":
                handleQuestSet(player, args, label);
                break;
            case "list":
                handleQuestList(player);
                break;
            case "delete":
                handleQuestDelete(player, args, label);
                break;
            default:
                MessageUtil.sendRaw(player, "&cUsage: /" + label + " quest <create|set|list|delete>");
                break;
        }
    }

    private void handleQuestCreate(Player player, String[] args, String label) {
        if (args.length < 4) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " quest create <id> <type>");
            MessageUtil.sendRaw(player, "&7Types: KILL_MOBS, KILL_BOSS, COLLECT_ITEMS, REACH_LOCATION, SURVIVE_TIME, INTERACT_BLOCKS");
            return;
        }

        String questId = args[2].toLowerCase();
        String typeStr = args[3].toUpperCase();

        if (plugin.getQuestManager().questExists(questId)) {
            MessageUtil.sendRaw(player, "&cQuest '" + questId + "' already exists.");
            return;
        }

        try {
            Quest.QuestType questType = Quest.QuestType.valueOf(typeStr);
            Quest quest = new Quest(questId, questId, "", questType, new ArrayList<>(), true, true, null);
            plugin.getQuestManager().addQuest(quest);
            plugin.getQuestManager().saveQuests();
            MessageUtil.sendRaw(player, "&aCreated quest '&e" + questId + "&a' of type &e" + typeStr);
        } catch (IllegalArgumentException e) {
            MessageUtil.sendRaw(player, "&cInvalid quest type: &e" + typeStr);
        }
    }

    private void handleQuestSet(Player player, String[] args, String label) {
        if (args.length < 5) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " quest set <quest_id> <property> <value>");
            MessageUtil.sendRaw(player, "&7Properties: name, description, required, show-progress, bonus-reward");
            return;
        }

        String questId = args[2].toLowerCase();
        String property = args[3].toLowerCase();
        String value = joinArgs(args, 4);

        Quest existing = plugin.getQuestManager().getQuest(questId);
        if (existing == null) {
            MessageUtil.sendRaw(player, "&cQuest '" + questId + "' not found.");
            return;
        }

        // Rebuild quest with updated property
        String name = existing.getName();
        String description = existing.getDescription();
        boolean required = existing.isRequired();
        boolean showProgress = existing.isShowProgress();
        String bonusReward = existing.getBonusReward();

        switch (property) {
            case "name":
                name = value;
                break;
            case "description":
                description = value;
                break;
            case "required":
                required = Boolean.parseBoolean(value);
                break;
            case "show-progress":
                showProgress = Boolean.parseBoolean(value);
                break;
            case "bonus-reward":
                bonusReward = value;
                break;
            default:
                MessageUtil.sendRaw(player, "&cUnknown property: &e" + property);
                return;
        }

        Quest updated = new Quest(questId, name, description, existing.getType(), existing.getObjectives(), required, showProgress, bonusReward);
        plugin.getQuestManager().addQuest(updated);
        plugin.getQuestManager().saveQuests();
        MessageUtil.sendRaw(player, "&aUpdated quest '&e" + questId + "&a' property '&e" + property + "&a' to: &e" + value);
    }

    private void handleQuestList(Player player) {
        Collection<Quest> quests = plugin.getQuestManager().getAllQuests();

        if (quests.isEmpty()) {
            MessageUtil.sendRaw(player, "&eNo quests configured.");
            return;
        }

        player.sendMessage(Component.text("--- All Quests ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (Quest quest : quests) {
            Component line = Component.text(" " + quest.getId(), NamedTextColor.YELLOW)
                    .append(Component.text(" [" + quest.getType().name() + "]", NamedTextColor.GRAY))
                    .append(Component.text(" - " + quest.getName(), NamedTextColor.WHITE))
                    .append(quest.isRequired()
                            ? Component.text(" (Required)", NamedTextColor.RED)
                            : Component.text(" (Optional)", NamedTextColor.GREEN));
            player.sendMessage(line);
        }
    }

    private void handleQuestDelete(Player player, String[] args, String label) {
        if (args.length < 3) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " quest delete <id>");
            return;
        }

        String questId = args[2].toLowerCase();
        if (!plugin.getQuestManager().questExists(questId)) {
            MessageUtil.sendRaw(player, "&cQuest '" + questId + "' not found.");
            return;
        }

        plugin.getQuestManager().removeQuest(questId);
        plugin.getQuestManager().saveQuests();
        MessageUtil.sendRaw(player, "&aDeleted quest: &e" + questId);
    }

    // ==================== reward ====================

    private void handleReward(Player player, String[] args, String label) {
        if (args.length < 2) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " reward <add|set|view|list> ...");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "add":
                handleRewardAdd(player, args, label);
                break;
            case "set":
                handleRewardSet(player, args, label);
                break;
            case "view":
                handleRewardView(player, args, label);
                break;
            case "list":
                handleRewardList(player);
                break;
            default:
                MessageUtil.sendRaw(player, "&cUsage: /" + label + " reward <add|set|view|list>");
                break;
        }
    }

    private void handleRewardAdd(Player player, String[] args, String label) {
        // /dng reward add <reward_id> <material> [amount] [chance]
        if (args.length < 4) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " reward add <reward_id> <material> [amount] [chance]");
            return;
        }

        String rewardId = args[2].toLowerCase();
        String materialStr = args[3].toUpperCase();
        String amountRange = args.length >= 5 ? args[4] : "1";
        double chance = args.length >= 6 ? Double.parseDouble(args[5]) : 100.0;

        try {
            Material material = Material.valueOf(materialStr);
            plugin.getRewardManager().addItemToReward(rewardId, material, amountRange, chance);
            plugin.getRewardManager().saveRewards();
            MessageUtil.sendRaw(player, "&aAdded &e" + materialStr + " &ato reward &e" + rewardId);
        } catch (IllegalArgumentException e) {
            MessageUtil.sendRaw(player, "&cInvalid material: &e" + materialStr);
        }
    }

    private void handleRewardSet(Player player, String[] args, String label) {
        // /dng reward set <reward_id> <money|experience> <value>
        if (args.length < 5) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " reward set <reward_id> <money|experience> <value>");
            return;
        }

        String rewardId = args[2].toLowerCase();
        String property = args[3].toLowerCase();
        String value = args[4];

        switch (property) {
            case "money":
                plugin.getRewardManager().setRewardMoney(rewardId, value);
                plugin.getRewardManager().saveRewards();
                MessageUtil.sendRaw(player, "&aSet money range for &e" + rewardId + "&a to &e" + value);
                break;
            case "experience":
            case "xp":
                try {
                    int xp = Integer.parseInt(value);
                    plugin.getRewardManager().setRewardExperience(rewardId, xp);
                    plugin.getRewardManager().saveRewards();
                    MessageUtil.sendRaw(player, "&aSet experience for &e" + rewardId + "&a to &e" + xp);
                } catch (NumberFormatException e) {
                    MessageUtil.sendRaw(player, "&cInvalid number: &e" + value);
                }
                break;
            default:
                MessageUtil.sendRaw(player, "&cUnknown property: &e" + property + "&c. Use: money, experience");
                break;
        }
    }

    private void handleRewardView(Player player, String[] args, String label) {
        if (args.length < 3) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " reward view <reward_id>");
            return;
        }

        String rewardId = args[2].toLowerCase();
        Reward reward = plugin.getRewardManager().getReward(rewardId);

        if (reward == null) {
            MessageUtil.sendRaw(player, "&cReward '" + rewardId + "' not found.");
            return;
        }

        player.sendMessage(Component.text("--- Reward: " + rewardId + " ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("Chance: ", NamedTextColor.GRAY).append(Component.text(reward.getChance() + "%", NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Money: ", NamedTextColor.GRAY).append(Component.text(reward.getMoneyRange(), NamedTextColor.GOLD)));
        player.sendMessage(Component.text("Experience: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(reward.getExperience()), NamedTextColor.GREEN)));

        if (!reward.getItems().isEmpty()) {
            player.sendMessage(Component.text("Items:", NamedTextColor.GRAY));
            int index = 0;
            for (RewardItem item : reward.getItems()) {
                player.sendMessage(Component.text("  [" + index + "] ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(item.getMaterial().name(), NamedTextColor.YELLOW))
                        .append(Component.text(" x" + item.getAmountRange(), NamedTextColor.WHITE))
                        .append(Component.text(" (" + item.getChance() + "%)", NamedTextColor.GRAY)));
                index++;
            }
        }

        if (reward.getCommands() != null && !reward.getCommands().isEmpty()) {
            player.sendMessage(Component.text("Commands: " + reward.getCommands().size(), NamedTextColor.GRAY));
        }
    }

    private void handleRewardList(Player player) {
        Collection<Reward> rewards = plugin.getRewardManager().getAllRewards();

        if (rewards.isEmpty()) {
            MessageUtil.sendRaw(player, "&eNo rewards configured.");
            return;
        }

        player.sendMessage(Component.text("--- All Rewards ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (Reward reward : rewards) {
            player.sendMessage(Component.text(" " + reward.getId(), NamedTextColor.YELLOW)
                    .append(Component.text(" - " + reward.getItems().size() + " items", NamedTextColor.GRAY))
                    .append(Component.text(", " + reward.getChance() + "% chance", NamedTextColor.DARK_GRAY)));
        }
    }

    // ==================== schematic ====================

    private void handleSchematic(Player player, String[] args, String label) {
        if (args.length < 2) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " schematic <paste|confirm|pos1|pos2|save>");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "paste":
                handleSchematicPaste(player, args, label);
                break;
            case "confirm":
                handleSchematicConfirm(player);
                break;
            case "pos1":
                handleSchematicPos1(player);
                break;
            case "pos2":
                handleSchematicPos2(player);
                break;
            case "save":
                handleSchematicSave(player, args, label);
                break;
            default:
                MessageUtil.sendRaw(player, "&cUsage: /" + label + " schematic <paste|confirm|pos1|pos2|save>");
                break;
        }
    }

    private void handleSchematicPaste(Player player, String[] args, String label) {
        if (!plugin.getWorldEditIntegration().isEnabled()) {
            MessageUtil.sendRaw(player, "&cWorldEdit is not available.");
            return;
        }

        if (args.length < 3) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " schematic paste <filename>");
            return;
        }

        String filename = args[2];
        try {
            Clipboard clipboard = plugin.getWorldEditIntegration().loadSchematic(filename);
            clipboardMap.put(player.getUniqueId(), clipboard);
            MessageUtil.sendRaw(player, "&aLoaded schematic '&e" + filename + "&a'. Use /dng schematic confirm to paste at your location.");
        } catch (Exception e) {
            MessageUtil.sendRaw(player, "&cFailed to load schematic: &e" + e.getMessage());
        }
    }

    private void handleSchematicConfirm(Player player) {
        if (!plugin.getWorldEditIntegration().isEnabled()) {
            MessageUtil.sendRaw(player, "&cWorldEdit is not available.");
            return;
        }

        Clipboard clipboard = clipboardMap.get(player.getUniqueId());
        if (clipboard == null) {
            MessageUtil.sendRaw(player, "&cNo schematic loaded. Use /dng schematic paste <filename> first.");
            return;
        }

        boolean success = plugin.getWorldEditIntegration().pasteSchematic(clipboard, player.getLocation());
        if (success) {
            MessageUtil.sendRaw(player, "&aSchematic pasted at your location.");
        } else {
            MessageUtil.sendRaw(player, "&cFailed to paste schematic.");
        }
    }

    private void handleSchematicPos1(Player player) {
        Location loc = player.getLocation();
        pos1Map.put(player.getUniqueId(), loc);
        MessageUtil.sendRaw(player, "&aPosition 1 set to: &e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

        if (pos2Map.containsKey(player.getUniqueId())) {
            String dims = plugin.getWorldEditIntegration().getRegionDimensions(loc, pos2Map.get(player.getUniqueId()));
            MessageUtil.sendRaw(player, "&7Selection: " + dims);
        }
    }

    private void handleSchematicPos2(Player player) {
        Location loc = player.getLocation();
        pos2Map.put(player.getUniqueId(), loc);
        MessageUtil.sendRaw(player, "&aPosition 2 set to: &e" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());

        if (pos1Map.containsKey(player.getUniqueId())) {
            String dims = plugin.getWorldEditIntegration().getRegionDimensions(pos1Map.get(player.getUniqueId()), loc);
            MessageUtil.sendRaw(player, "&7Selection: " + dims);
        }
    }

    private void handleSchematicSave(Player player, String[] args, String label) {
        if (!plugin.getWorldEditIntegration().isEnabled()) {
            MessageUtil.sendRaw(player, "&cWorldEdit is not available.");
            return;
        }

        Location p1 = pos1Map.get(player.getUniqueId());
        Location p2 = pos2Map.get(player.getUniqueId());

        if (p1 == null || p2 == null) {
            MessageUtil.sendRaw(player, "&cSet both pos1 and pos2 first.");
            return;
        }

        String filename;
        if (args.length >= 3) {
            filename = args[2];
        } else if (requireSession(player)) {
            filename = getSession(player).schematicFile;
        } else {
            return;
        }

        if (!filename.endsWith(".schem")) {
            filename = filename + ".schem";
        }

        boolean success = plugin.getWorldEditIntegration().saveSchematic(filename, p1, p2);
        if (success) {
            MessageUtil.sendRaw(player, "&aSchematic saved as '&e" + filename + "&a'.");
            if (sessions.containsKey(player.getUniqueId())) {
                getSession(player).schematicFile = filename;
                MessageUtil.sendRaw(player, "&7Updated session schematic file to: &e" + filename);
            }
        } else {
            MessageUtil.sendRaw(player, "&cFailed to save schematic. Check console.");
        }
    }

    // ==================== trigger ====================

    private void handleTrigger(Player player, String[] args, String label) {
        if (!requireSession(player)) return;

        if (args.length < 2) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " trigger <set|edit|list|add>");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "set":
                handleTriggerSet(player, args, label);
                break;
            case "edit":
                handleTriggerEdit(player, args, label);
                break;
            case "list":
                handleTriggerList(player);
                break;
            case "add":
                handleTriggerAdd(player, args, label);
                break;
            default:
                MessageUtil.sendRaw(player, "&cUsage: /" + label + " trigger <set|edit|list|add>");
                break;
        }
    }

    private void handleTriggerSet(Player player, String[] args, String label) {
        // /dng trigger set <trigger_id> <property> <value>
        if (args.length < 5) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " trigger set <trigger_id> <property> <value>");
            MessageUtil.sendRaw(player, "&7Properties: type, description, repeatable, cooldown, x, y, z, radius, time, questid, bossid, killcount");
            return;
        }

        DungeonBuilder builder = getSession(player);
        String triggerId = args[2].toLowerCase();
        String property = args[3].toLowerCase();
        String value = joinArgs(args, 4);

        Trigger trigger = builder.triggers.get(triggerId);
        if (trigger == null) {
            MessageUtil.sendRaw(player, "&cTrigger '" + triggerId + "' not found. Add it first.");
            return;
        }

        try {
            switch (property) {
                case "type":
                    trigger.setType(Trigger.TriggerType.valueOf(value.toUpperCase()));
                    MessageUtil.sendRaw(player, "&aSet trigger type to: &e" + value.toUpperCase());
                    break;
                case "description":
                    trigger.setDescription(value);
                    MessageUtil.sendRaw(player, "&aSet trigger description to: &e" + value);
                    break;
                case "repeatable":
                    trigger.setRepeatable(Boolean.parseBoolean(value));
                    MessageUtil.sendRaw(player, "&aSet trigger repeatable to: &e" + value);
                    break;
                case "cooldown":
                    trigger.setCooldown(Integer.parseInt(value));
                    MessageUtil.sendRaw(player, "&aSet trigger cooldown to: &e" + value + "s");
                    break;
                case "x":
                    trigger.setTriggerX(Double.parseDouble(value));
                    MessageUtil.sendRaw(player, "&aSet trigger X to: &e" + value);
                    break;
                case "y":
                    trigger.setTriggerY(Double.parseDouble(value));
                    MessageUtil.sendRaw(player, "&aSet trigger Y to: &e" + value);
                    break;
                case "z":
                    trigger.setTriggerZ(Double.parseDouble(value));
                    MessageUtil.sendRaw(player, "&aSet trigger Z to: &e" + value);
                    break;
                case "radius":
                    trigger.setTriggerRadius(Double.parseDouble(value));
                    MessageUtil.sendRaw(player, "&aSet trigger radius to: &e" + value);
                    break;
                case "time":
                    trigger.setTriggerTime(Integer.parseInt(value));
                    MessageUtil.sendRaw(player, "&aSet trigger time to: &e" + value + "s");
                    break;
                case "questid":
                    trigger.setQuestId(value);
                    MessageUtil.sendRaw(player, "&aSet trigger quest ID to: &e" + value);
                    break;
                case "bossid":
                    trigger.setBossId(value);
                    MessageUtil.sendRaw(player, "&aSet trigger boss ID to: &e" + value);
                    break;
                case "killcount":
                    trigger.setKillCount(Integer.parseInt(value));
                    MessageUtil.sendRaw(player, "&aSet trigger kill count to: &e" + value);
                    break;
                default:
                    MessageUtil.sendRaw(player, "&cUnknown trigger property: &e" + property);
                    break;
            }
        } catch (IllegalArgumentException e) {
            MessageUtil.sendRaw(player, "&cInvalid value: &e" + value);
        }
    }

    private void handleTriggerEdit(Player player, String[] args, String label) {
        // /dng trigger edit <trigger_id> - shows trigger details
        if (args.length < 3) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " trigger edit <trigger_id>");
            return;
        }

        DungeonBuilder builder = getSession(player);
        String triggerId = args[2].toLowerCase();

        Trigger trigger = builder.triggers.get(triggerId);
        if (trigger == null) {
            MessageUtil.sendRaw(player, "&cTrigger '" + triggerId + "' not found.");
            return;
        }

        player.sendMessage(Component.text("--- Trigger: " + triggerId + " ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("Type: ", NamedTextColor.GRAY).append(Component.text(trigger.getType().name(), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Description: ", NamedTextColor.GRAY).append(Component.text(trigger.getDescription(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Repeatable: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(trigger.isRepeatable()), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Cooldown: ", NamedTextColor.GRAY).append(Component.text(trigger.getCooldown() + "s", NamedTextColor.WHITE)));

        Trigger.TriggerCondition condition = trigger.getCondition();
        if (condition != null) {
            player.sendMessage(Component.text("Condition:", NamedTextColor.GRAY));
            switch (trigger.getType()) {
                case LOCATION:
                    player.sendMessage(Component.text("  X: " + condition.getX() + " Y: " + condition.getY() + " Z: " + condition.getZ(), NamedTextColor.WHITE));
                    player.sendMessage(Component.text("  Radius: " + condition.getRadius(), NamedTextColor.WHITE));
                    break;
                case TIMER:
                    player.sendMessage(Component.text("  Time: " + condition.getTime() + "s", NamedTextColor.WHITE));
                    break;
                case MOB_KILL:
                    player.sendMessage(Component.text("  Kill Count: " + condition.getMobCount(), NamedTextColor.WHITE));
                    break;
                case QUEST_COMPLETE:
                    player.sendMessage(Component.text("  Quest ID: " + condition.getQuestId(), NamedTextColor.WHITE));
                    break;
                case BOSS_KILL:
                    player.sendMessage(Component.text("  Boss ID: " + condition.getBossId(), NamedTextColor.WHITE));
                    break;
            }
        }

        player.sendMessage(Component.text("Actions: " + trigger.getActions().size(), NamedTextColor.GRAY));
        int i = 0;
        for (Trigger.TriggerAction triggerAction : trigger.getActions()) {
            player.sendMessage(Component.text("  [" + i + "] ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(triggerAction.getType().name(), NamedTextColor.YELLOW)));
            i++;
        }
    }

    private void handleTriggerList(Player player) {
        DungeonBuilder builder = getSession(player);

        if (builder.triggers.isEmpty()) {
            MessageUtil.sendRaw(player, "&eNo triggers defined.");
            return;
        }

        player.sendMessage(Component.text("--- Triggers ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (Map.Entry<String, Trigger> entry : builder.triggers.entrySet()) {
            Trigger trigger = entry.getValue();
            player.sendMessage(Component.text(" " + entry.getKey(), NamedTextColor.YELLOW)
                    .append(Component.text(" [" + trigger.getType().name() + "]", NamedTextColor.GRAY))
                    .append(Component.text(" " + trigger.getActions().size() + " actions", NamedTextColor.DARK_GRAY))
                    .append(trigger.isRepeatable()
                            ? Component.text(" (Repeatable)", NamedTextColor.GREEN)
                            : Component.text(" (Once)", NamedTextColor.RED)));
        }
    }

    private void handleTriggerAdd(Player player, String[] args, String label) {
        // /dng trigger add <trigger_id> <action_type> [params...]
        if (args.length < 4) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " trigger add <trigger_id> <action_type> [params...]");
            MessageUtil.sendRaw(player, "&7Action types: SPAWN_MOB, DROP_ITEM, DAMAGE_PLAYER, MESSAGE, COMMAND, TELEPORT, POTION_EFFECT");
            return;
        }

        DungeonBuilder builder = getSession(player);
        String triggerId = args[2].toLowerCase();
        String actionTypeStr = args[3].toUpperCase();

        Trigger trigger = builder.triggers.get(triggerId);
        if (trigger == null) {
            MessageUtil.sendRaw(player, "&cTrigger '" + triggerId + "' not found.");
            return;
        }

        try {
            Trigger.TriggerAction.ActionType actionType = Trigger.TriggerAction.ActionType.valueOf(actionTypeStr);
            Trigger.TriggerAction action;

            switch (actionType) {
                case SPAWN_MOB:
                    // /dng trigger add <id> SPAWN_MOB <mob_type> <count> <x> <y> <z>
                    if (args.length < 9) {
                        MessageUtil.sendRaw(player, "&cUsage: /" + label + " trigger add <id> SPAWN_MOB <mob_type> <count> <x> <y> <z>");
                        return;
                    }
                    EntityType mobType = EntityType.valueOf(args[4].toUpperCase());
                    int count = Integer.parseInt(args[5]);
                    double sx = Double.parseDouble(args[6]);
                    double sy = Double.parseDouble(args[7]);
                    double sz = Double.parseDouble(args[8]);
                    action = Trigger.TriggerAction.spawnMob(mobType, count, sx, sy, sz);
                    break;
                case DROP_ITEM:
                    // /dng trigger add <id> DROP_ITEM <material> <amount> <x> <y> <z>
                    if (args.length < 9) {
                        MessageUtil.sendRaw(player, "&cUsage: /" + label + " trigger add <id> DROP_ITEM <material> <amount> <x> <y> <z>");
                        return;
                    }
                    Material mat = Material.valueOf(args[4].toUpperCase());
                    int amt = Integer.parseInt(args[5]);
                    double dx = Double.parseDouble(args[6]);
                    double dy = Double.parseDouble(args[7]);
                    double dz = Double.parseDouble(args[8]);
                    action = Trigger.TriggerAction.dropItem(mat, amt, dx, dy, dz);
                    break;
                case DAMAGE_PLAYER:
                    // /dng trigger add <id> DAMAGE_PLAYER <damage>
                    if (args.length < 5) {
                        MessageUtil.sendRaw(player, "&cUsage: /" + label + " trigger add <id> DAMAGE_PLAYER <damage>");
                        return;
                    }
                    double damage = Double.parseDouble(args[4]);
                    action = Trigger.TriggerAction.damagePlayer(damage);
                    break;
                case MESSAGE:
                    // /dng trigger add <id> MESSAGE <message...>
                    if (args.length < 5) {
                        MessageUtil.sendRaw(player, "&cUsage: /" + label + " trigger add <id> MESSAGE <message>");
                        return;
                    }
                    String message = joinArgs(args, 4);
                    action = Trigger.TriggerAction.message(message);
                    break;
                case COMMAND:
                    // /dng trigger add <id> COMMAND <command...>
                    if (args.length < 5) {
                        MessageUtil.sendRaw(player, "&cUsage: /" + label + " trigger add <id> COMMAND <command>");
                        return;
                    }
                    String cmd = joinArgs(args, 4);
                    action = Trigger.TriggerAction.command(cmd);
                    break;
                case TELEPORT:
                    // /dng trigger add <id> TELEPORT <x> <y> <z>
                    if (args.length < 7) {
                        MessageUtil.sendRaw(player, "&cUsage: /" + label + " trigger add <id> TELEPORT <x> <y> <z>");
                        return;
                    }
                    double tx = Double.parseDouble(args[4]);
                    double ty = Double.parseDouble(args[5]);
                    double tz = Double.parseDouble(args[6]);
                    action = Trigger.TriggerAction.teleport(tx, ty, tz);
                    break;
                case POTION_EFFECT:
                    // /dng trigger add <id> POTION_EFFECT <effect> <duration> <amplifier>
                    if (args.length < 7) {
                        MessageUtil.sendRaw(player, "&cUsage: /" + label + " trigger add <id> POTION_EFFECT <effect> <duration> <amplifier>");
                        return;
                    }
                    org.bukkit.NamespacedKey effectKey = org.bukkit.NamespacedKey.minecraft(args[4].toLowerCase());
                    org.bukkit.potion.PotionEffectType potionType = org.bukkit.Registry.EFFECT.get(effectKey);
                    if (potionType == null) {
                        MessageUtil.sendRaw(player, "&cInvalid potion effect: &e" + args[4]);
                        return;
                    }
                    int duration = Integer.parseInt(args[5]);
                    int amplifier = Integer.parseInt(args[6]);
                    action = Trigger.TriggerAction.potionEffect(potionType, duration, amplifier);
                    break;
                default:
                    MessageUtil.sendRaw(player, "&cUnhandled action type.");
                    return;
            }

            trigger.addAction(action);
            MessageUtil.sendRaw(player, "&aAdded &e" + actionTypeStr + " &aaction to trigger '&e" + triggerId + "&a'.");
        } catch (IllegalArgumentException e) {
            MessageUtil.sendRaw(player, "&cInvalid value: &e" + e.getMessage());
        }
    }

    // ==================== mobs ====================

    private void handleMobs(Player player, String[] args, String label) {
        if (!requireSession(player)) return;

        if (args.length < 2) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " mobs <create|edit|remove|list>");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "create":
                handleMobCreate(player, args, label);
                break;
            case "edit":
                handleMobEdit(player, args, label);
                break;
            case "remove":
                handleMobRemove(player, args, label);
                break;
            case "list":
                handleMobList(player);
                break;
            default:
                MessageUtil.sendRaw(player, "&cUsage: /" + label + " mobs <create|edit|remove|list>");
                break;
        }
    }

    private void handleMobCreate(Player player, String[] args, String label) {
        // /dng mobs create <mob_id> <entity_type>
        if (args.length < 4) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " mobs create <mob_id> <entity_type>");
            return;
        }

        DungeonBuilder builder = getSession(player);
        String mobId = args[2].toLowerCase();
        String typeStr = args[3].toUpperCase();

        try {
            EntityType entityType = EntityType.valueOf(typeStr);
            CustomMob mob = new CustomMob(mobId, entityType);
            plugin.getCustomMobManager().setCustomMob(builder.id, mob);
            MessageUtil.sendRaw(player, "&aCreated custom mob '&e" + mobId + "&a' of type &e" + typeStr);
        } catch (IllegalArgumentException e) {
            MessageUtil.sendRaw(player, "&cInvalid entity type: &e" + typeStr);
        }
    }

    private void handleMobEdit(Player player, String[] args, String label) {
        // /dng mobs edit <mob_id> <property> <value>
        if (args.length < 5) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " mobs edit <mob_id> <property> <value>");
            MessageUtil.sendRaw(player, "&7Properties: name, health, damage, speed, showname");
            return;
        }

        DungeonBuilder builder = getSession(player);
        String mobId = args[2].toLowerCase();
        String property = args[3].toLowerCase();
        String value = joinArgs(args, 4);

        CustomMob mob = plugin.getCustomMobManager().getCustomMob(builder.id, mobId);
        if (mob == null) {
            MessageUtil.sendRaw(player, "&cCustom mob '" + mobId + "' not found for this dungeon.");
            return;
        }

        try {
            switch (property) {
                case "name":
                    mob.setCustomName(value);
                    MessageUtil.sendRaw(player, "&aSet mob name to: &e" + value);
                    break;
                case "health":
                    mob.setHealth(Double.parseDouble(value));
                    MessageUtil.sendRaw(player, "&aSet mob health to: &e" + value);
                    break;
                case "damage":
                    mob.setDamage(Double.parseDouble(value));
                    MessageUtil.sendRaw(player, "&aSet mob damage to: &e" + value);
                    break;
                case "speed":
                    mob.setSpeed(Double.parseDouble(value));
                    MessageUtil.sendRaw(player, "&aSet mob speed to: &e" + value);
                    break;
                case "showname":
                    mob.setShowName(Boolean.parseBoolean(value));
                    MessageUtil.sendRaw(player, "&aSet mob show name to: &e" + value);
                    break;
                default:
                    MessageUtil.sendRaw(player, "&cUnknown mob property: &e" + property);
                    break;
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendRaw(player, "&cInvalid number: &e" + value);
        }
    }

    private void handleMobRemove(Player player, String[] args, String label) {
        if (args.length < 3) {
            MessageUtil.sendRaw(player, "&cUsage: /" + label + " mobs remove <mob_id>");
            return;
        }

        DungeonBuilder builder = getSession(player);
        String mobId = args[2].toLowerCase();

        CustomMob mob = plugin.getCustomMobManager().getCustomMob(builder.id, mobId);
        if (mob == null) {
            MessageUtil.sendRaw(player, "&cCustom mob '" + mobId + "' not found for this dungeon.");
            return;
        }

        plugin.getCustomMobManager().removeCustomMob(builder.id, mobId);
        MessageUtil.sendRaw(player, "&aRemoved custom mob: &e" + mobId);
    }

    private void handleMobList(Player player) {
        DungeonBuilder builder = getSession(player);
        Map<String, CustomMob> mobs = plugin.getCustomMobManager().getDungeonMobs(builder.id);

        if (mobs.isEmpty()) {
            MessageUtil.sendRaw(player, "&eNo custom mobs defined for this dungeon.");
            return;
        }

        player.sendMessage(Component.text("--- Custom Mobs: " + builder.id + " ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        for (Map.Entry<String, CustomMob> entry : mobs.entrySet()) {
            CustomMob mob = entry.getValue();
            Component line = Component.text(" " + entry.getKey(), NamedTextColor.YELLOW)
                    .append(Component.text(" [" + mob.getEntityType().name() + "]", NamedTextColor.GRAY));

            if (mob.getCustomName() != null) {
                line = line.append(Component.text(" \"" + mob.getCustomName() + "\"", NamedTextColor.WHITE));
            }
            if (mob.getHealth() > 0) {
                line = line.append(Component.text(" HP:" + mob.getHealth(), NamedTextColor.RED));
            }
            if (mob.getDamage() > 0) {
                line = line.append(Component.text(" DMG:" + mob.getDamage(), NamedTextColor.DARK_RED));
            }

            player.sendMessage(line);
        }
    }

    // ==================== Utility ====================

    private String joinArgs(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }

    // ==================== Tab Completion ====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "edit", "save", "cancel", "info", "list",
                    "set", "add", "remove", "quest", "reward", "schematic", "trigger", "mobs"));
            return filterCompletions(completions, args[0]);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            switch (sub) {
                case "edit":
                case "info":
                    completions.addAll(plugin.getDungeonManager().getDungeonIds());
                    break;
                case "set":
                    completions.addAll(Arrays.asList("displayname", "schematic", "difficulty", "minparty",
                            "maxparty", "timelimit", "cooldown", "entrycost", "enabled",
                            "spawnoffsetx", "spawnoffsety", "spawnoffsetz"));
                    break;
                case "add":
                case "remove":
                    completions.addAll(Arrays.asList("quest", "reward", "trigger"));
                    break;
                case "quest":
                    completions.addAll(Arrays.asList("create", "set", "list", "delete"));
                    break;
                case "reward":
                    completions.addAll(Arrays.asList("add", "set", "view", "list"));
                    break;
                case "schematic":
                    completions.addAll(Arrays.asList("paste", "confirm", "pos1", "pos2", "save"));
                    break;
                case "trigger":
                    completions.addAll(Arrays.asList("set", "edit", "list", "add"));
                    break;
                case "mobs":
                    completions.addAll(Arrays.asList("create", "edit", "remove", "list"));
                    break;
            }
            return filterCompletions(completions, args[1]);
        }

        if (args.length == 3) {
            switch (sub) {
                case "set":
                    String prop = args[1].toLowerCase();
                    if (prop.equals("difficulty")) {
                        completions.addAll(Arrays.asList("EASY", "MEDIUM", "HARD", "EXTREME"));
                    } else if (prop.equals("enabled")) {
                        completions.addAll(Arrays.asList("true", "false"));
                    }
                    break;
                case "add":
                    String addType = args[1].toLowerCase();
                    if (addType.equals("quest")) {
                        completions.addAll(plugin.getQuestManager().getQuestIds());
                    } else if (addType.equals("reward")) {
                        completions.addAll(plugin.getRewardManager().getRewardIds());
                    }
                    break;
                case "remove":
                    String removeType = args[1].toLowerCase();
                    DungeonBuilder session = getSession((Player) sender);
                    if (session != null) {
                        if (removeType.equals("quest")) {
                            completions.addAll(session.quests);
                        } else if (removeType.equals("reward")) {
                            completions.addAll(session.rewards);
                        } else if (removeType.equals("trigger")) {
                            completions.addAll(session.triggers.keySet());
                        }
                    }
                    break;
                case "quest":
                    String questAction = args[1].toLowerCase();
                    if (questAction.equals("set") || questAction.equals("delete")) {
                        completions.addAll(plugin.getQuestManager().getQuestIds());
                    } else if (questAction.equals("create")) {
                        completions.add("<id>");
                    }
                    break;
                case "reward":
                    String rewardAction = args[1].toLowerCase();
                    if (rewardAction.equals("add") || rewardAction.equals("set") || rewardAction.equals("view")) {
                        completions.addAll(plugin.getRewardManager().getRewardIds());
                    }
                    break;
                case "trigger":
                    String triggerAction = args[1].toLowerCase();
                    DungeonBuilder trigSession = getSession((Player) sender);
                    if (trigSession != null && (triggerAction.equals("set") || triggerAction.equals("edit") || triggerAction.equals("add"))) {
                        completions.addAll(trigSession.triggers.keySet());
                    }
                    break;
                case "mobs":
                    String mobAction = args[1].toLowerCase();
                    if (mobAction.equals("edit") || mobAction.equals("remove")) {
                        DungeonBuilder mobSession = getSession((Player) sender);
                        if (mobSession != null) {
                            completions.addAll(plugin.getCustomMobManager().getDungeonMobs(mobSession.id).keySet());
                        }
                    }
                    break;
            }
            return filterCompletions(completions, args[2]);
        }

        if (args.length == 4) {
            switch (sub) {
                case "add":
                    if (args[1].equalsIgnoreCase("trigger")) {
                        for (Trigger.TriggerType t : Trigger.TriggerType.values()) {
                            completions.add(t.name());
                        }
                    }
                    break;
                case "quest":
                    if (args[1].equalsIgnoreCase("create")) {
                        for (Quest.QuestType t : Quest.QuestType.values()) {
                            completions.add(t.name());
                        }
                    } else if (args[1].equalsIgnoreCase("set")) {
                        completions.addAll(Arrays.asList("name", "description", "required", "show-progress", "bonus-reward"));
                    }
                    break;
                case "reward":
                    if (args[1].equalsIgnoreCase("add")) {
                        for (Material m : Material.values()) {
                            if (m.isItem()) completions.add(m.name());
                        }
                    } else if (args[1].equalsIgnoreCase("set")) {
                        completions.addAll(Arrays.asList("money", "experience"));
                    }
                    break;
                case "trigger":
                    if (args[1].equalsIgnoreCase("set")) {
                        completions.addAll(Arrays.asList("type", "description", "repeatable", "cooldown",
                                "x", "y", "z", "radius", "time", "questid", "bossid", "killcount"));
                    } else if (args[1].equalsIgnoreCase("add")) {
                        for (Trigger.TriggerAction.ActionType t : Trigger.TriggerAction.ActionType.values()) {
                            completions.add(t.name());
                        }
                    }
                    break;
                case "mobs":
                    if (args[1].equalsIgnoreCase("create")) {
                        for (EntityType t : EntityType.values()) {
                            if (t.isAlive()) completions.add(t.name());
                        }
                    } else if (args[1].equalsIgnoreCase("edit")) {
                        completions.addAll(Arrays.asList("name", "health", "damage", "speed", "showname"));
                    }
                    break;
            }
            return filterCompletions(completions, args[3]);
        }

        if (args.length == 5) {
            if (sub.equals("trigger") && args[1].equalsIgnoreCase("add")) {
                String actionType = args[3].toUpperCase();
                if (actionType.equals("SPAWN_MOB")) {
                    for (EntityType t : EntityType.values()) {
                        if (t.isAlive()) completions.add(t.name());
                    }
                } else if (actionType.equals("DROP_ITEM")) {
                    for (Material m : Material.values()) {
                        if (m.isItem()) completions.add(m.name());
                    }
                }
            } else if (sub.equals("trigger") && args[1].equalsIgnoreCase("set")) {
                String triggerProp = args[3].toLowerCase();
                if (triggerProp.equals("type")) {
                    for (Trigger.TriggerType t : Trigger.TriggerType.values()) {
                        completions.add(t.name());
                    }
                } else if (triggerProp.equals("repeatable")) {
                    completions.addAll(Arrays.asList("true", "false"));
                }
            }
            return filterCompletions(completions, args[4]);
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
