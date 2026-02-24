package dev.bekololek.dungeons.managers;

import dev.bekololek.dungeons.Main;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class StatsManager {

    private final Main plugin;
    private final File statsFile;
    private final Map<UUID, PlayerStats> players;

    private static final List<StatDef> PLAYER_SCHEMA = List.of(
            new StatDef("dungeons_completed", "Dungeons Completed", "int", null, true),
            new StatDef("total_playtime", "Total Playtime", "long", "seconds", true),
            new StatDef("deaths", "Deaths", "int", null, true),
            new StatDef("quests_completed", "Quests Completed", "int", null, true),
            new StatDef("mobs_killed", "Mobs Killed", "int", null, true),
            new StatDef("fastest_completion", "Fastest Completion", "long", "seconds", false),
            new StatDef("favorite_dungeon", "Favorite Dungeon", "string", null, false)
    );

    private static final List<StatDef> GLOBAL_SCHEMA = List.of(
            new StatDef("total_completions", "Total Completions", "int", null, false),
            new StatDef("total_playtime", "Total Playtime", "long", "seconds", false),
            new StatDef("active_instances", "Active Instances", "int", null, false),
            new StatDef("most_popular_dungeon", "Most Popular Dungeon", "string", null, false),
            new StatDef("most_experienced_player", "Most Experienced Player", "string", null, false),
            new StatDef("total_deaths", "Total Deaths", "int", null, false)
    );

    record StatDef(String key, String label, String type, String unit, boolean leaderboard) {}

    public StatsManager(Main plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        this.players = new HashMap<>();
    }

    public void load() {
        if (!statsFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(statsFile);
        var playersSection = yaml.getConfigurationSection("players");
        if (playersSection == null) return;

        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                var section = playersSection.getConfigurationSection(uuidStr);
                if (section == null) continue;

                PlayerStats stats = new PlayerStats();
                stats.name = section.getString("name", "Unknown");
                stats.dungeonsCompleted = section.getInt("dungeons_completed", 0);
                stats.totalPlaytime = section.getLong("total_playtime", 0);
                stats.deaths = section.getInt("deaths", 0);
                stats.questsCompleted = section.getInt("quests_completed", 0);
                stats.mobsKilled = section.getInt("mobs_killed", 0);
                stats.fastestCompletion = section.getLong("fastest_completion", 0);

                var dungeonCounts = section.getConfigurationSection("dungeon_counts");
                if (dungeonCounts != null) {
                    for (String key : dungeonCounts.getKeys(false)) {
                        stats.dungeonCounts.put(key, dungeonCounts.getInt(key, 0));
                    }
                }

                players.put(uuid, stats);
            } catch (IllegalArgumentException ignored) {}
        }

        plugin.getLogger().info("Loaded stats for " + players.size() + " players.");
    }

    public void save() {
        new BukkitRunnable() {
            @Override
            public void run() {
                writeFile();
            }
        }.runTaskAsynchronously(plugin);
    }

    public void saveSync() {
        writeFile();
    }

    private synchronized void writeFile() {
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("plugin", "Dungeons.v1 - BL");
        yaml.set("version", 1);

        for (StatDef def : PLAYER_SCHEMA) {
            String base = "schema.player." + def.key;
            yaml.set(base + ".label", def.label);
            yaml.set(base + ".type", def.type);
            if (def.unit != null) yaml.set(base + ".unit", def.unit);
            yaml.set(base + ".leaderboard", def.leaderboard);
        }

        for (StatDef def : GLOBAL_SCHEMA) {
            String base = "schema.global." + def.key;
            yaml.set(base + ".label", def.label);
            yaml.set(base + ".type", def.type);
            if (def.unit != null) yaml.set(base + ".unit", def.unit);
            yaml.set(base + ".leaderboard", def.leaderboard);
        }

        // Global stats
        int totalCompletions = players.values().stream().mapToInt(p -> p.dungeonsCompleted).sum();
        long totalPlaytime = players.values().stream().mapToLong(p -> p.totalPlaytime).sum();
        int totalDeaths = players.values().stream().mapToInt(p -> p.deaths).sum();

        yaml.set("global.total_completions", totalCompletions);
        yaml.set("global.total_playtime", totalPlaytime);
        yaml.set("global.active_instances", plugin.getInstanceManager() != null ? plugin.getInstanceManager().getActiveInstances().size() : 0);
        yaml.set("global.total_deaths", totalDeaths);

        // Most popular dungeon
        Map<String, Integer> allDungeonCounts = new HashMap<>();
        for (PlayerStats stats : players.values()) {
            for (var entry : stats.dungeonCounts.entrySet()) {
                allDungeonCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        String mostPopular = allDungeonCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");
        yaml.set("global.most_popular_dungeon", mostPopular);

        // Most experienced player
        String mostExperienced = players.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().dungeonsCompleted))
                .map(e -> e.getValue().name)
                .orElse("None");
        yaml.set("global.most_experienced_player", mostExperienced);

        // Players
        for (var entry : players.entrySet()) {
            String base = "players." + entry.getKey().toString();
            PlayerStats s = entry.getValue();
            yaml.set(base + ".name", s.name);
            yaml.set(base + ".dungeons_completed", s.dungeonsCompleted);
            yaml.set(base + ".total_playtime", s.totalPlaytime);
            yaml.set(base + ".deaths", s.deaths);
            yaml.set(base + ".quests_completed", s.questsCompleted);
            yaml.set(base + ".mobs_killed", s.mobsKilled);
            yaml.set(base + ".fastest_completion", s.fastestCompletion);
            yaml.set(base + ".favorite_dungeon", s.getFavoriteDungeon());
            for (var dc : s.dungeonCounts.entrySet()) {
                yaml.set(base + ".dungeon_counts." + dc.getKey(), dc.getValue());
            }
        }

        try {
            yaml.save(statsFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save stats.yml: " + e.getMessage());
        }
    }

    public void startAutoSave() {
        new BukkitRunnable() {
            @Override
            public void run() {
                save();
            }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L);
    }

    private PlayerStats getOrCreate(UUID uuid) {
        return players.computeIfAbsent(uuid, k -> new PlayerStats());
    }

    public void recordDungeonComplete(Player player, String dungeonId, long completionDurationSeconds) {
        PlayerStats stats = getOrCreate(player.getUniqueId());
        stats.name = player.getName();
        stats.dungeonsCompleted++;
        stats.totalPlaytime += completionDurationSeconds;
        stats.dungeonCounts.merge(dungeonId, 1, Integer::sum);
        if (completionDurationSeconds > 0 && (stats.fastestCompletion == 0 || completionDurationSeconds < stats.fastestCompletion)) {
            stats.fastestCompletion = completionDurationSeconds;
        }
    }

    public void recordDeath(Player player) {
        PlayerStats stats = getOrCreate(player.getUniqueId());
        stats.name = player.getName();
        stats.deaths++;
    }

    public void recordMobKill(Player player) {
        PlayerStats stats = getOrCreate(player.getUniqueId());
        stats.name = player.getName();
        stats.mobsKilled++;
    }

    public void recordQuestComplete(Player player) {
        PlayerStats stats = getOrCreate(player.getUniqueId());
        stats.name = player.getName();
        stats.questsCompleted++;
    }

    public void updateName(Player player) {
        PlayerStats stats = players.get(player.getUniqueId());
        if (stats != null) stats.name = player.getName();
    }

    public Object getPlayerStat(UUID uuid, String statName) {
        PlayerStats stats = players.get(uuid);
        if (stats == null) return statName.equals("favorite_dungeon") ? "None" : 0;

        return switch (statName) {
            case "dungeons_completed" -> stats.dungeonsCompleted;
            case "total_playtime" -> stats.totalPlaytime;
            case "deaths" -> stats.deaths;
            case "quests_completed" -> stats.questsCompleted;
            case "mobs_killed" -> stats.mobsKilled;
            case "fastest_completion" -> stats.fastestCompletion;
            case "favorite_dungeon" -> stats.getFavoriteDungeon();
            default -> 0;
        };
    }

    public Object getGlobalStat(String statName) {
        return switch (statName) {
            case "total_completions", "dungeons_completed" -> players.values().stream().mapToInt(p -> p.dungeonsCompleted).sum();
            case "total_playtime" -> players.values().stream().mapToLong(p -> p.totalPlaytime).sum();
            case "active_instances" -> plugin.getInstanceManager() != null ? plugin.getInstanceManager().getActiveInstances().size() : 0;
            case "deaths", "total_deaths" -> players.values().stream().mapToInt(p -> p.deaths).sum();
            case "quests_completed" -> players.values().stream().mapToInt(p -> p.questsCompleted).sum();
            case "mobs_killed" -> players.values().stream().mapToInt(p -> p.mobsKilled).sum();
            case "most_popular_dungeon" -> {
                Map<String, Integer> counts = new HashMap<>();
                for (PlayerStats s : players.values()) {
                    for (var e : s.dungeonCounts.entrySet()) counts.merge(e.getKey(), e.getValue(), Integer::sum);
                }
                yield counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("None");
            }
            case "most_experienced_player" -> players.entrySet().stream()
                    .max(Comparator.comparingInt(e -> e.getValue().dungeonsCompleted))
                    .map(e -> e.getValue().name).orElse("None");
            default -> 0;
        };
    }

    public List<Map.Entry<String, Number>> getTopPlayers(String statName, int limit) {
        List<Map.Entry<String, Number>> entries = new ArrayList<>();

        for (var entry : players.entrySet()) {
            PlayerStats stats = entry.getValue();
            Number value = switch (statName) {
                case "dungeons_completed" -> stats.dungeonsCompleted;
                case "total_playtime" -> stats.totalPlaytime;
                case "deaths" -> stats.deaths;
                case "quests_completed" -> stats.questsCompleted;
                case "mobs_killed" -> stats.mobsKilled;
                case "fastest_completion" -> stats.fastestCompletion;
                default -> 0;
            };
            if (value.longValue() > 0) {
                entries.add(Map.entry(stats.name, value));
            }
        }

        if ("fastest_completion".equals(statName)) {
            entries.sort(Comparator.comparingLong(e -> e.getValue().longValue()));
        } else {
            entries.sort((a, b) -> Long.compare(b.getValue().longValue(), a.getValue().longValue()));
        }

        return entries.stream().limit(limit).collect(Collectors.toList());
    }

    public static List<String> leaderboardStats() {
        return PLAYER_SCHEMA.stream().filter(s -> s.leaderboard).map(s -> s.key).toList();
    }

    public static String statLabel(String key) {
        for (StatDef def : PLAYER_SCHEMA) {
            if (def.key.equals(key)) return def.label;
        }
        for (StatDef def : GLOBAL_SCHEMA) {
            if (def.key.equals(key)) return def.label;
        }
        return key;
    }

    private static class PlayerStats {
        String name = "Unknown";
        int dungeonsCompleted;
        long totalPlaytime;
        int deaths;
        int questsCompleted;
        int mobsKilled;
        long fastestCompletion;
        Map<String, Integer> dungeonCounts = new HashMap<>();

        String getFavoriteDungeon() {
            return dungeonCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("None");
        }
    }
}
