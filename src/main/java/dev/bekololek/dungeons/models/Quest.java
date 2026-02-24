package dev.bekololek.dungeons.models;

import java.util.List;

public class Quest {

    private final String id;
    private final String name;
    private final String description;
    private final QuestType type;
    private final List<QuestObjective> objectives;
    private final boolean required;
    private final boolean showProgress;
    private final String bonusReward;

    public Quest(String id, String name, String description, QuestType type,
                 List<QuestObjective> objectives, boolean required, boolean showProgress,
                 String bonusReward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.objectives = objectives;
        this.required = required;
        this.showProgress = showProgress;
        this.bonusReward = bonusReward;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public QuestType getType() { return type; }
    public List<QuestObjective> getObjectives() { return objectives; }
    public boolean isRequired() { return required; }
    public boolean isShowProgress() { return showProgress; }
    public String getBonusReward() { return bonusReward; }

    public enum QuestType {
        KILL_MOBS,
        KILL_BOSS,
        COLLECT_ITEMS,
        REACH_LOCATION,
        SURVIVE_TIME,
        INTERACT_BLOCKS
    }

    @Override
    public String toString() {
        return "Quest{id='" + id + "', name='" + name + "', type=" + type + ", required=" + required + "}";
    }
}
