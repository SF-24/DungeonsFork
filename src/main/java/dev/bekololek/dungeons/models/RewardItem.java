package dev.bekololek.dungeons.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

public class RewardItem {

    private final Material material;
    private final String amountRange;
    private final double chance;
    private final String name;
    private final List<String> lore;
    private final Map<Enchantment, Integer> enchantments;
    private final Map<Enchantment, Integer> storedEnchantments;
    private final String skullTexture;

    public RewardItem(Material material, String amountRange, double chance, String name,
                      List<String> lore, Map<Enchantment, Integer> enchantments,
                      Map<Enchantment, Integer> storedEnchantments, String skullTexture) {
        this.material = material;
        this.amountRange = amountRange;
        this.chance = chance;
        this.name = name;
        this.lore = lore;
        this.enchantments = enchantments;
        this.storedEnchantments = storedEnchantments;
        this.skullTexture = skullTexture;
    }

    public Material getMaterial() { return material; }
    public String getAmountRange() { return amountRange; }
    public double getChance() { return chance; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }
    public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
    public Map<Enchantment, Integer> getStoredEnchantments() { return storedEnchantments; }
    public String getSkullTexture() { return skullTexture; }

    public int getRandomAmount() {
        if (amountRange == null || amountRange.isEmpty()) return 1;

        try {
            if (amountRange.contains("-")) {
                String[] parts = amountRange.split("-");
                int min = Integer.parseInt(parts[0]);
                int max = Integer.parseInt(parts[1]);
                return min + (int) (Math.random() * (max - min + 1));
            } else {
                return Integer.parseInt(amountRange);
            }
        } catch (Exception e) {
            return 1;
        }
    }

    @Override
    public String toString() {
        return "RewardItem{material=" + material + ", amount=" + amountRange + ", chance=" + chance + "}";
    }
}
