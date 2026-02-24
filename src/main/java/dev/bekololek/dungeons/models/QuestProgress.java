package dev.bekololek.dungeons.models;

import java.util.HashMap;
import java.util.Map;

public class QuestProgress {

    private final Quest quest;
    private final Map<Integer, Integer> objectiveProgress;
    private boolean completed;

    public QuestProgress(Quest quest) {
        this.quest = quest;
        this.objectiveProgress = new HashMap<>();
        this.completed = false;

        for (int i = 0; i < quest.getObjectives().size(); i++) {
            objectiveProgress.put(i, 0);
        }
    }

    public Quest getQuest() { return quest; }

    public int getProgress(int objectiveIndex) {
        return objectiveProgress.getOrDefault(objectiveIndex, 0);
    }

    public void setProgress(int objectiveIndex, int progress) {
        objectiveProgress.put(objectiveIndex, progress);
        checkCompletion();
    }

    public void incrementProgress(int objectiveIndex, int amount) {
        int current = getProgress(objectiveIndex);
        setProgress(objectiveIndex, current + amount);
    }

    public boolean isObjectiveComplete(int objectiveIndex) {
        if (objectiveIndex >= quest.getObjectives().size()) return false;

        QuestObjective objective = quest.getObjectives().get(objectiveIndex);
        int current = getProgress(objectiveIndex);
        int required = objective.getAmount();

        return current >= required;
    }

    public boolean isCompleted() { return completed; }

    private void checkCompletion() {
        for (int i = 0; i < quest.getObjectives().size(); i++) {
            if (!isObjectiveComplete(i)) {
                completed = false;
                return;
            }
        }
        completed = true;
    }

    public float getCompletionPercentage() {
        if (quest.getObjectives().isEmpty()) return 100.0f;

        int totalRequired = 0;
        int totalProgress = 0;

        for (int i = 0; i < quest.getObjectives().size(); i++) {
            QuestObjective objective = quest.getObjectives().get(i);
            totalRequired += objective.getAmount();
            totalProgress += Math.min(getProgress(i), objective.getAmount());
        }

        if (totalRequired == 0) return 100.0f;
        return (totalProgress * 100.0f) / totalRequired;
    }

    @Override
    public String toString() {
        return "QuestProgress{quest=" + quest.getId() + ", completed=" + completed
                + ", progress=" + String.format("%.1f%%", getCompletionPercentage()) + "}";
    }
}
