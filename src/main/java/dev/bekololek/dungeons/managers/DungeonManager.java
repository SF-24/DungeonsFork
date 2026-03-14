package dev.bekololek.dungeons.managers;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.models.Dungeon;
import dev.bekololek.dungeons.models.Trigger;
import dev.bekololek.dungeons.utils.ClickType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class DungeonManager {

    private final Main plugin;
    private final Map<String, Dungeon> dungeons;

    public DungeonManager(Main plugin) {
        this.plugin = plugin;
        this.dungeons = new HashMap<>();
    }

    public void loadDungeons() {
        dungeons.clear();

        File dungeonsFolder = new File(plugin.getDataFolder(), "dungeons");
        if (!dungeonsFolder.exists()) {
            dungeonsFolder.mkdirs();
            plugin.saveResource("dungeons/goblin_cave.yml", false);
            plugin.saveResource("dungeons/undead_crypt.yml", false);
            plugin.saveResource("dungeons/ice_temple.yml", false);
            plugin.getLogger().info("Created dungeons folder with default dungeons");
        }

        File[] dungeonFiles = dungeonsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (dungeonFiles == null || dungeonFiles.length == 0) {
            plugin.getLogger().warning("No dungeon files found in dungeons/ folder!");
            return;
        }

        for (File dungeonFile : dungeonFiles) {
            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(dungeonFile);
                Dungeon dungeon = loadDungeon(config);
                if (dungeon != null) {
                    dungeons.put(dungeon.getId(), dungeon);
                    plugin.getLogger().info("Loaded dungeon: " + dungeon.getId() + " from " + dungeonFile.getName());

                    plugin.getCustomMobManager().loadDungeonMobs(dungeon, config);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load dungeon from " + dungeonFile.getName(), e);
            }
        }

        plugin.getLogger().info("Loaded " + dungeons.size() + " dungeons");
    }

    private Dungeon loadDungeon(FileConfiguration config) {
        String id = config.getString("id");
        if (id == null) {
            plugin.getLogger().warning("Dungeon file missing 'id' field!");
            return null;
        }

        String displayName = config.getString("display-name", id);
        List<String> description = config.getStringList("description");
        String schematicFile = config.getString("schematic-file", id + ".schem");

        int minPartySize = config.getInt("party-size.min", 1);
        int maxPartySize = config.getInt("party-size.max", 5);

        String difficulty = config.getString("difficulty", "MEDIUM");
        int timeLimit = config.getInt("time-limit", 1200);
        List<String> questIds = config.getStringList("quests");

        double spawnOffsetX = config.getDouble("spawn-offset.x", 0.5);
        double spawnOffsetY = config.getDouble("spawn-offset.y", 1.0);
        double spawnOffsetZ = config.getDouble("spawn-offset.z", 0.5);

        Material iconMaterial = Material.STONE;
        try {
            String materialName = config.getString("icon.material", "STONE");
            iconMaterial = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid icon material for dungeon " + id + ", using STONE");
        }
        int iconCustomModelData = config.getInt("icon.custom-model-data", 0);

        List<String> rewardTables = config.getStringList("rewards");
        long cooldown = config.getLong("cooldown", 3600);
        double entryCost = config.getDouble("entry-cost", 0);
        String permission = config.getString("permission", "dungeons.enter." + id);
        boolean enabled = config.getBoolean("enabled", true);

        List<Trigger> triggers = loadTriggers(config, id);

        return new Dungeon(
                id, displayName, description, schematicFile,
                minPartySize, maxPartySize, difficulty, timeLimit,
                questIds, iconMaterial, iconCustomModelData,
                rewardTables, cooldown, entryCost, permission, enabled,
                spawnOffsetX, spawnOffsetY, spawnOffsetZ, triggers
        );
    }

    private List<Trigger> loadTriggers(FileConfiguration config, String dungeonId) {
        List<Trigger> triggers = new ArrayList<>();

        ConfigurationSection triggersSection = config.getConfigurationSection("triggers");
        if (triggersSection == null) return triggers;

        for (String triggerId : triggersSection.getKeys(false)) {
            try {
                ConfigurationSection triggerSection = triggersSection.getConfigurationSection(triggerId);
                if (triggerSection == null) continue;

                String typeStr = triggerSection.getString("type", "LOCATION").toUpperCase();
                Trigger.TriggerType type = Trigger.TriggerType.valueOf(typeStr);

                Trigger.Builder builder = new Trigger.Builder(triggerId);

                String description = triggerSection.getString("description", "");
                builder.description(description);

                ConfigurationSection conditionSection = triggerSection.getConfigurationSection("condition");
                if (conditionSection != null) {
                    switch (type) {
                        case LOCATION:
                            double x = conditionSection.getDouble("x");
                            double y = conditionSection.getDouble("y");
                            double z = conditionSection.getDouble("z");
                            double radius = conditionSection.getDouble("radius", 3.0);
                            builder.location(x, y, z, radius);
                            break;
                        case BLOCK_INTERACT:
                            double ix = conditionSection.getDouble("x");
                            double iy = conditionSection.getDouble("y");
                            double iz = conditionSection.getDouble("z");
                            try {
                                if(conditionSection.getString("click-type")==null) {
                                    break;
                                }
                                ClickType clickType = ClickType.valueOf(conditionSection.getString("click-type").toUpperCase(Locale.ROOT));
                                builder.interact(ix,iy,iz,clickType);
                            }  catch(Exception ignored) {
                                break;
                            }
                            break;
                        case TIMER:
                            int time = conditionSection.getInt("time");
                            builder.timer(time);
                            break;
                        case MOB_KILL:
                            int count = conditionSection.getInt("count");
                            builder.mobKill(count);
                            break;
                        case QUEST_COMPLETE:
                            String questId = conditionSection.getString("quest-id");
                            builder.questComplete(questId);
                            break;
                        case BOSS_KILL:
                            String bossId = conditionSection.getString("boss-id");
                            builder.bossKill(bossId);
                            break;
                    }
                }

                List<?> actionsList = triggerSection.getList("actions");
                if (actionsList != null) {
                    for (Object actionObj : actionsList) {
                        if (actionObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> actionMap = (Map<String, Object>) actionObj;
                            Trigger.TriggerAction action = parseAction(actionMap);
                            if (action != null) {
                                builder.addAction(action);
                            }
                        }
                    }
                }

                builder.repeatable(triggerSection.getBoolean("repeatable", false));
                builder.cooldown(triggerSection.getInt("cooldown", 0));

                triggers.add(builder.build());
                plugin.getLogger().info("Loaded trigger '" + triggerId + "' for dungeon " + dungeonId);

            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load trigger '" + triggerId +
                        "' for dungeon " + dungeonId, e);
            }
        }

        return triggers;
    }

    private Trigger.TriggerAction parseAction(Map<String, Object> actionMap) {
        String typeStr = (String) actionMap.get("type");
        if (typeStr == null) return null;

        try {
            Trigger.TriggerAction.ActionType type =
                    Trigger.TriggerAction.ActionType.valueOf(typeStr.toUpperCase());

            switch (type) {
                case SPAWN_MOB:
                    int mobCount = ((Number) actionMap.getOrDefault("count", 1)).intValue();
                    double mobX = ((Number) actionMap.getOrDefault("x", 0.0)).doubleValue();
                    double mobY = ((Number) actionMap.getOrDefault("y", 0.0)).doubleValue();
                    double mobZ = ((Number) actionMap.getOrDefault("z", 0.0)).doubleValue();

                    String customMobId = (String) actionMap.get("custom-mob-id");
                    if (customMobId != null) {
                        return Trigger.TriggerAction.spawnCustomMob(customMobId, mobCount, mobX, mobY, mobZ);
                    }

                    String mobTypeStr = (String) actionMap.get("mob-type");
                    org.bukkit.entity.EntityType mobType = org.bukkit.entity.EntityType.valueOf(mobTypeStr.toUpperCase());
                    return Trigger.TriggerAction.spawnMob(mobType, mobCount, mobX, mobY, mobZ);

                case SPAWN_MYTHIC_MOB:
                    int mobCount_ = ((Number) actionMap.getOrDefault("count", 1)).intValue();
                    double mobX_ = ((Number) actionMap.getOrDefault("x", 0.0)).doubleValue();
                    double mobY_ = ((Number) actionMap.getOrDefault("y", 0.0)).doubleValue();
                    double mobZ_ = ((Number) actionMap.getOrDefault("z", 0.0)).doubleValue();

                    String customMobId_ = (String) actionMap.get("custom-mob-id");
                    if (customMobId_ != null) {
                        return Trigger.TriggerAction.spawnMythicMob(customMobId_, mobCount_, mobX_, mobY_, mobZ_);
                    }
                case DROP_ITEM:
                    String material = (String) actionMap.get("material");
                    int itemAmount = ((Number) actionMap.getOrDefault("amount", 1)).intValue();
                    double itemX = ((Number) actionMap.getOrDefault("x", 0.0)).doubleValue();
                    double itemY = ((Number) actionMap.getOrDefault("y", 0.0)).doubleValue();
                    double itemZ = ((Number) actionMap.getOrDefault("z", 0.0)).doubleValue();
                    return Trigger.TriggerAction.dropItem(Material.valueOf(material), itemAmount, itemX, itemY, itemZ);

                case DAMAGE_PLAYER:
                    int damage = ((Number) actionMap.getOrDefault("damage", 1)).intValue();
                    return Trigger.TriggerAction.damagePlayer(damage);

                case MESSAGE:
                    String message = (String) actionMap.get("message");
                    return Trigger.TriggerAction.message(message);

                case COMMAND:
                    String command = (String) actionMap.get("command");
                    return Trigger.TriggerAction.command(command);

                case POTION_EFFECT:
                    String effectName = (String) actionMap.get("effect");
                    NamespacedKey effectKey = NamespacedKey.minecraft(effectName.toLowerCase());
                    PotionEffectType potionType = Registry.EFFECT.get(effectKey);
                    int duration = ((Number) actionMap.getOrDefault("duration", 30)).intValue();
                    int amplifier = ((Number) actionMap.getOrDefault("amplifier", 0)).intValue();
                    return Trigger.TriggerAction.potionEffect(potionType, duration, amplifier);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to parse action: " + typeStr, e);
        }

        return null;
    }

    public Dungeon getDungeon(String dungeonId) { return dungeons.get(dungeonId); }

    public Collection<Dungeon> getAllDungeons() { return new ArrayList<>(dungeons.values()); }

    public Collection<Dungeon> getEnabledDungeons() {
        List<Dungeon> enabled = new ArrayList<>();
        for (Dungeon dungeon : dungeons.values()) {
            if (dungeon.isEnabled()) enabled.add(dungeon);
        }
        return enabled;
    }

    public boolean dungeonExists(String dungeonId) { return dungeons.containsKey(dungeonId); }
    public Set<String> getDungeonIds() { return new HashSet<>(dungeons.keySet()); }

    public boolean saveDungeon(Dungeon dungeon) {
        try {
            File dungeonsFolder = new File(plugin.getDataFolder(), "dungeons");
            if (!dungeonsFolder.exists()) dungeonsFolder.mkdirs();

            File dungeonFile = new File(dungeonsFolder, dungeon.getId() + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(dungeonFile);

            config.set("triggers", null);

            for (Trigger trigger : dungeon.getTriggers()) {
                String basePath = "triggers." + trigger.getId();
                config.set(basePath + ".type", trigger.getType().name());
                config.set(basePath + ".repeatable", trigger.isRepeatable());
                config.set(basePath + ".cooldown", trigger.getCooldown());

                Trigger.TriggerCondition condition = trigger.getCondition();
                if (condition != null) {
                    switch (trigger.getType()) {
                        case LOCATION:
                            config.set(basePath + ".condition.x", condition.getX());
                            config.set(basePath + ".condition.y", condition.getY());
                            config.set(basePath + ".condition.z", condition.getZ());
                            config.set(basePath + ".condition.radius", condition.getRadius());
                            break;
                        case BLOCK_INTERACT:
                            config.set(basePath + ".condition.x", condition.getX());
                            config.set(basePath + ".condition.y", condition.getY());
                            config.set(basePath + ".condition.z", condition.getZ());
                            config.set(basePath + ".condition.click-type", condition.getClickType().name().toLowerCase(Locale.ROOT));
                            break;
                        case TIMER:
                            config.set(basePath + ".condition.time", condition.getTime());
                            break;
                        case MOB_KILL:
                            config.set(basePath + ".condition.mob-count", condition.getMobCount());
                            if (condition.getMobType() != null) {
                                config.set(basePath + ".condition.mob-type", condition.getMobType().name());
                            }
                            break;
                        case QUEST_COMPLETE:
                            config.set(basePath + ".condition.quest-id", condition.getQuestId());
                            break;
                    }
                }

                List<Map<String, Object>> actionsList = new ArrayList<>();
                for (Trigger.TriggerAction action : trigger.getActions()) {
                    Map<String, Object> actionMap = serializeAction(action);
                    if (actionMap != null) actionsList.add(actionMap);
                }
                config.set(basePath + ".actions", actionsList);
            }

            config.save(dungeonFile);
            plugin.getLogger().info("Saved dungeon configuration: " + dungeon.getId());

            dungeons.put(dungeon.getId(), dungeon);

            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save dungeon: " + dungeon.getId(), e);
            return false;
        }
    }

    public boolean saveDungeon(dev.bekololek.dungeons.commands.DungeonEditorCommand.DungeonBuilder builder) {
        try {
            File dungeonsFolder = new File(plugin.getDataFolder(), "dungeons");
            if (!dungeonsFolder.exists()) dungeonsFolder.mkdirs();

            File dungeonFile = new File(dungeonsFolder, builder.id + ".yml");
            FileConfiguration config = new YamlConfiguration();

            config.set("id", builder.id);
            config.set("display-name", builder.displayName);
            config.set("schematic-file", builder.schematicFile);
            config.set("party-size.min", builder.minPartySize);
            config.set("party-size.max", builder.maxPartySize);
            config.set("time-limit", builder.timeLimit);
            config.set("spawn-offset.x", builder.spawnOffsetX);
            config.set("spawn-offset.y", builder.spawnOffsetY);
            config.set("spawn-offset.z", builder.spawnOffsetZ);
            config.set("quests", builder.quests);
            config.set("rewards", builder.rewards);
            config.set("cooldown", builder.cooldown);
            config.set("difficulty", builder.difficulty);
            config.set("entry-cost", builder.entryCost);
            config.set("enabled", builder.enabled);

            if (config.getStringList("description").isEmpty()) {
                List<String> defaultDescription = new ArrayList<>();
                defaultDescription.add("&7A custom dungeon.");
                config.set("description", defaultDescription);
            }

            if (!builder.triggers.isEmpty()) {
                config.set("triggers", null);

                for (Trigger trigger : builder.triggers.values()) {
                    String basePath = "triggers." + trigger.getId();
                    config.set(basePath + ".type", trigger.getType().name());
                    config.set(basePath + ".description", trigger.getDescription());
                    config.set(basePath + ".repeatable", trigger.isRepeatable());
                    config.set(basePath + ".cooldown", trigger.getCooldown());

                    Trigger.TriggerCondition condition = trigger.getCondition();
                    if (condition != null) {
                        switch (trigger.getType()) {
                            case LOCATION:
                                config.set(basePath + ".condition.x", condition.getX());
                                config.set(basePath + ".condition.y", condition.getY());
                                config.set(basePath + ".condition.z", condition.getZ());
                                config.set(basePath + ".condition.radius", condition.getRadius());
                                break;
                            case BLOCK_INTERACT:
                                config.set(basePath + ".condition.x", condition.getX());
                                config.set(basePath + ".condition.y", condition.getY());
                                config.set(basePath + ".condition.z", condition.getZ());
                                config.set(basePath + ".condition.click-type", condition.getClickType().name().toLowerCase(Locale.ROOT));
                                break;
                            case TIMER:
                                config.set(basePath + ".condition.time", condition.getTime());
                                break;
                            case MOB_KILL:
                                config.set(basePath + ".condition.mob-count", condition.getMobCount());
                                if (condition.getMobType() != null) {
                                    config.set(basePath + ".condition.mob-type", condition.getMobType().name());
                                }
                                break;
                            case QUEST_COMPLETE:
                                config.set(basePath + ".condition.quest-id", condition.getQuestId());
                                break;
                            case BOSS_KILL:
                                config.set(basePath + ".condition.boss-id", condition.getBossId());
                                break;
                        }
                    }

                    List<Map<String, Object>> actionsList = new ArrayList<>();
                    for (Trigger.TriggerAction action : trigger.getActions()) {
                        Map<String, Object> actionMap = serializeAction(action);
                        if (actionMap != null) actionsList.add(actionMap);
                    }
                    config.set(basePath + ".actions", actionsList);
                }
            }

            config.save(dungeonFile);
            plugin.getLogger().info("Saved dungeon configuration: " + builder.id + " to " + dungeonFile.getName());
            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save dungeon: " + builder.id, e);
            return false;
        }
    }

    private Map<String, Object> serializeAction(Trigger.TriggerAction action) {
        Map<String, Object> actionMap = new HashMap<>();
        actionMap.put("type", action.getType().name());

        switch (action.getType()) {
            case MESSAGE:
                actionMap.put("message", action.getMessage());
                break;
            case SPAWN_MOB:
                if (action.getCustomMobId() != null) {
                    actionMap.put("custom-mob-id", action.getCustomMobId());
                } else if (action.getMobType() != null) {
                    actionMap.put("mob-type", action.getMobType().name());
                }
                actionMap.put("count", action.getMobCount());
                actionMap.put("x", action.getSpawnX());
                actionMap.put("y", action.getSpawnY());
                actionMap.put("z", action.getSpawnZ());
                break;
            case COMMAND:
                actionMap.put("command", action.getCommand());
                break;
            case TELEPORT:
                actionMap.put("x", action.getTeleportX());
                actionMap.put("y", action.getTeleportY());
                actionMap.put("z", action.getTeleportZ());
                break;
            case DAMAGE_PLAYER:
                actionMap.put("damage", action.getDamage());
                break;
            case DROP_ITEM:
                if (action.getDropMaterial() != null) {
                    actionMap.put("material", action.getDropMaterial().name());
                }
                actionMap.put("amount", action.getDropAmount());
                actionMap.put("x", action.getDropX());
                actionMap.put("y", action.getDropY());
                actionMap.put("z", action.getDropZ());
                break;
            case POTION_EFFECT:
                if (action.getPotionType() != null) {
                    actionMap.put("effect", action.getPotionType().getKey().getKey());
                }
                actionMap.put("duration", action.getPotionDuration());
                actionMap.put("amplifier", action.getPotionAmplifier());
                break;
        }

        return actionMap;
    }
}
