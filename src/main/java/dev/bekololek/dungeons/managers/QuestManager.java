package dev.bekololek.dungeons.managers;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.models.Quest;
import dev.bekololek.dungeons.models.QuestObjective;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class QuestManager {

    private final Main plugin;
    private final Map<String, Quest> quests;

    public QuestManager(Main plugin) {
        this.plugin = plugin;
        this.quests = new HashMap<>();
    }

    public void loadQuests() {
        quests.clear();

        File questsFile = new File(plugin.getDataFolder(), "quests.yml");
        if (!questsFile.exists()) {
            plugin.saveResource("quests.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(questsFile);
        ConfigurationSection questsSection = config.getConfigurationSection("quests");

        if (questsSection == null) {
            plugin.getLogger().warning("No quests configured in quests.yml!");
            return;
        }

        for (String questId : questsSection.getKeys(false)) {
            try {
                Quest quest = loadQuest(questId, questsSection.getConfigurationSection(questId));
                if (quest != null) {
                    quests.put(questId, quest);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load quest: " + questId, e);
            }
        }

        plugin.getLogger().info("Loaded " + quests.size() + " quests");
    }

    private Quest loadQuest(String id, ConfigurationSection section) {
        if (section == null) return null;

        String name = section.getString("name", id);
        String description = section.getString("description", "");

        Quest.QuestType type;
        try {
            type = Quest.QuestType.valueOf(section.getString("type", "KILL_MOBS").toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid quest type for quest " + id + ", defaulting to KILL_MOBS");
            type = Quest.QuestType.KILL_MOBS;
        }

        boolean required = section.getBoolean("required", true);
        boolean showProgress = section.getBoolean("show-progress", true);
        String bonusReward = section.getString("bonus-reward", null);

        List<QuestObjective> objectives = new ArrayList<>();
        List<Map<?, ?>> objectivesList = section.getMapList("objectives");
        for (Map<?, ?> objectiveMap : objectivesList) {
            QuestObjective objective = parseObjective(objectiveMap, type);
            if (objective != null) objectives.add(objective);
        }

        return new Quest(id, name, description, type, objectives, required, showProgress, bonusReward);
    }

    private QuestObjective parseObjective(Map<?, ?> data, Quest.QuestType type) {
        QuestObjective.Builder builder = QuestObjective.builder();

        String displayName = (String) data.get("display-name");
        if (displayName != null) builder.displayName(displayName);

        Object amountObj = data.get("amount");
        if (amountObj instanceof Number) builder.amount(((Number) amountObj).intValue());

        switch (type) {
            case KILL_MOBS:
            case KILL_BOSS:
                String mobString = (String) data.get("mob");
                if (mobString != null) {
                    try { builder.mobType(EntityType.valueOf(mobString.toUpperCase())); }
                    catch (IllegalArgumentException e) { plugin.getLogger().warning("Invalid entity type: " + mobString); }
                }
                String bossId = (String) data.get("boss-id");
                if (bossId != null) builder.bossId(bossId);
                break;
            case COLLECT_ITEMS:
            case INTERACT_BLOCKS:
                String materialString = (String) data.get("material");
                if (materialString != null) {
                    try { builder.material(Material.valueOf(materialString.toUpperCase())); }
                    catch (IllegalArgumentException e) { plugin.getLogger().warning("Invalid material: " + materialString); }
                }
                break;
            case REACH_LOCATION:
                Object xObj = data.get("x"); Object yObj = data.get("y"); Object zObj = data.get("z");
                if (xObj instanceof Number && yObj instanceof Number && zObj instanceof Number) {
                    builder.location(((Number) xObj).intValue(), ((Number) yObj).intValue(), ((Number) zObj).intValue());
                }
                Object radiusObj = data.get("radius");
                if (radiusObj instanceof Number) builder.radius(((Number) radiusObj).intValue());
                break;
            case SURVIVE_TIME:
                Object durationObj = data.get("duration");
                if (durationObj instanceof Number) builder.duration(((Number) durationObj).intValue());
                break;
        }

        return builder.build();
    }

    public Quest getQuest(String questId) { return quests.get(questId); }

    public List<Quest> getQuests(List<String> questIds) {
        List<Quest> questList = new ArrayList<>();
        for (String questId : questIds) {
            Quest quest = quests.get(questId);
            if (quest != null) questList.add(quest);
            else plugin.getLogger().warning("Quest not found: " + questId);
        }
        return questList;
    }

    public Collection<Quest> getAllQuests() { return new ArrayList<>(quests.values()); }
    public boolean questExists(String questId) { return quests.containsKey(questId); }
    public Set<String> getQuestIds() { return new HashSet<>(quests.keySet()); }
    public void addQuest(Quest quest) { quests.put(quest.getId(), quest); }
    public void removeQuest(String questId) { quests.remove(questId); }

    public void saveQuests() {
        try {
            File questsFile = new File(plugin.getDataFolder(), "quests.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(questsFile);
            config.set("quests", null);

            for (Quest quest : quests.values()) {
                String path = "quests." + quest.getId();
                config.set(path + ".name", quest.getName());
                config.set(path + ".description", quest.getDescription());
                config.set(path + ".type", quest.getType().name());
                config.set(path + ".required", quest.isRequired());
                config.set(path + ".show-progress", quest.isShowProgress());
                if (quest.getBonusReward() != null && !quest.getBonusReward().isEmpty()) {
                    config.set(path + ".bonus-reward", quest.getBonusReward());
                }
            }

            config.save(questsFile);
            plugin.getLogger().info("Saved " + quests.size() + " quests");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save quests!", e);
        }
    }
}
