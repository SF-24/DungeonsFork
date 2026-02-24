package dev.bekololek.dungeons.models;

import java.util.List;

public class Reward {

    private final String id;
    private final double chance;
    private final List<RewardItem> items;
    private final String moneyRange;
    private final int experience;
    private final List<String> commands;

    public Reward(String id, double chance, List<RewardItem> items, String moneyRange, int experience, List<String> commands) {
        this.id = id;
        this.chance = chance;
        this.items = items;
        this.moneyRange = moneyRange;
        this.experience = experience;
        this.commands = commands;
    }

    public String getId() { return id; }
    public double getChance() { return chance; }
    public List<RewardItem> getItems() { return items; }
    public String getMoneyRange() { return moneyRange; }
    public int getExperience() { return experience; }
    public List<String> getCommands() { return commands; }

    public double getRandomMoney() {
        if (moneyRange == null || moneyRange.isEmpty()) return 0;

        try {
            if (moneyRange.contains("-")) {
                String[] parts = moneyRange.split("-");
                double min = Double.parseDouble(parts[0]);
                double max = Double.parseDouble(parts[1]);
                return min + (Math.random() * (max - min));
            } else {
                return Double.parseDouble(moneyRange);
            }
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "Reward{id='" + id + "', chance=" + chance + ", items=" + items.size() + "}";
    }
}
