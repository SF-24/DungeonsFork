package dev.bekololek.dungeons.managers;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.models.Reward;
import dev.bekololek.dungeons.models.RewardItem;
import dev.bekololek.dungeons.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class RewardManager {

    private final Main plugin;
    private final Map<String, Reward> rewards;

    public RewardManager(Main plugin) {
        this.plugin = plugin;
        this.rewards = new HashMap<>();
    }

    public void loadRewards() {
        rewards.clear();

        File rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) {
            plugin.saveResource("rewards.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(rewardsFile);
        ConfigurationSection rewardsSection = config.getConfigurationSection("rewards");

        if (rewardsSection == null) {
            plugin.getLogger().warning("No rewards configured in rewards.yml!");
            return;
        }

        for (String rewardId : rewardsSection.getKeys(false)) {
            try {
                Reward reward = loadReward(rewardId, rewardsSection.getConfigurationSection(rewardId));
                if (reward != null) {
                    rewards.put(rewardId, reward);
                    plugin.getLogger().info("Loaded reward: " + rewardId);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load reward: " + rewardId, e);
            }
        }

        plugin.getLogger().info("Loaded " + rewards.size() + " rewards");
    }

    private Reward loadReward(String id, ConfigurationSection section) {
        if (section == null) return null;

        double chance = section.getDouble("chance", 100.0);
        String moneyRange = section.getString("money", "0");
        int experience = section.getInt("experience", 0);
        List<String> commands = section.getStringList("commands");

        List<RewardItem> items = new ArrayList<>();
        if (section.contains("items")) {
            List<Map<?, ?>> itemsList = section.getMapList("items");
            for (Map<?, ?> itemData : itemsList) {
                RewardItem item = parseRewardItem(itemData);
                if (item != null) {
                    items.add(item);
                }
            }
        }

        return new Reward(id, chance, items, moneyRange, experience, commands);
    }

    @SuppressWarnings("unchecked")
    private RewardItem parseRewardItem(Map<?, ?> data) {
        String materialString = (String) data.get("material");
        if (materialString == null) return null;

        Material material;
        try {
            material = Material.valueOf(materialString.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material: " + materialString);
            return null;
        }

        Object amountObj = data.get("amount");
        String amountRange = amountObj != null ? amountObj.toString() : "1";

        double chance = 100.0;
        if (data.containsKey("chance")) {
            Object chanceObj = data.get("chance");
            if (chanceObj instanceof Number) {
                chance = ((Number) chanceObj).doubleValue();
            }
        }

        String name = (String) data.get("name");
        List<String> lore = data.containsKey("lore") ? (List<String>) data.get("lore") : null;

        Map<Enchantment, Integer> enchantments = new HashMap<>();
        if (data.containsKey("enchantments")) {
            Map<String, Object> enchantData = (Map<String, Object>) data.get("enchantments");
            for (Map.Entry<String, Object> entry : enchantData.entrySet()) {
                NamespacedKey key = NamespacedKey.minecraft(entry.getKey().toLowerCase());
                Enchantment ench = Registry.ENCHANTMENT.get(key);
                if (ench != null && entry.getValue() instanceof Number) {
                    enchantments.put(ench, ((Number) entry.getValue()).intValue());
                }
            }
        }

        Map<Enchantment, Integer> storedEnchantments = new HashMap<>();
        if (data.containsKey("stored-enchantments")) {
            Map<String, Object> enchantData = (Map<String, Object>) data.get("stored-enchantments");
            for (Map.Entry<String, Object> entry : enchantData.entrySet()) {
                NamespacedKey key = NamespacedKey.minecraft(entry.getKey().toLowerCase());
                Enchantment ench = Registry.ENCHANTMENT.get(key);
                if (ench != null && entry.getValue() instanceof Number) {
                    storedEnchantments.put(ench, ((Number) entry.getValue()).intValue());
                }
            }
        }

        String skullTexture = (String) data.get("skull-texture");

        return new RewardItem(material, amountRange, chance, name, lore, enchantments, storedEnchantments, skullTexture);
    }

    public void giveRewards(Player player, List<String> rewardIds) {
        for (String rewardId : rewardIds) {
            Reward reward = rewards.get(rewardId);
            if (reward == null) {
                plugin.getLogger().warning("Reward not found: " + rewardId);
                continue;
            }

            if (Math.random() * 100 > reward.getChance()) {
                continue;
            }

            giveReward(player, reward);
        }
    }

    public void giveReward(Player player, Reward reward) {
        List<ItemStack> rewardedItems = new ArrayList<>();
        for (RewardItem rewardItem : reward.getItems()) {
            if (Math.random() * 100 > rewardItem.getChance()) {
                continue;
            }

            ItemStack item = createItemStack(rewardItem);
            if (item != null) {
                rewardedItems.add(item);
            }
        }

        for (ItemStack item : rewardedItems) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }

        if (reward.getMoneyRange() != null && !reward.getMoneyRange().isEmpty()) {
            double money = reward.getRandomMoney();
            if (money > 0 && plugin.getVaultEconomy() != null && plugin.getVaultEconomy().isEnabled()) {
                plugin.getVaultEconomy().deposit(player, money);
                plugin.getLogger().fine("Gave " + money + " money to " + player.getName());
            }
        }

        if (reward.getExperience() > 0) {
            player.giveExp(reward.getExperience());
        }

        if (reward.getCommands() != null) {
            for (String command : reward.getCommands()) {
                String processedCommand = command.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
            }
        }
    }

    private ItemStack createItemStack(RewardItem rewardItem) {
        int amount = rewardItem.getRandomAmount();

        ItemBuilder builder = new ItemBuilder(rewardItem.getMaterial(), amount);

        if (rewardItem.getName() != null) {
            builder.setName(rewardItem.getName());
        }

        if (rewardItem.getLore() != null && !rewardItem.getLore().isEmpty()) {
            builder.setLore(rewardItem.getLore());
        }

        if (rewardItem.getEnchantments() != null && !rewardItem.getEnchantments().isEmpty()) {
            builder.addEnchantments(rewardItem.getEnchantments());
        }

        if (rewardItem.getStoredEnchantments() != null && !rewardItem.getStoredEnchantments().isEmpty()) {
            builder.addStoredEnchantments(rewardItem.getStoredEnchantments());
        }

        if (rewardItem.getSkullTexture() != null && !rewardItem.getSkullTexture().isEmpty()) {
            builder.setSkullTexture(rewardItem.getSkullTexture());
        }

        return builder.build();
    }

    public Reward getReward(String rewardId) { return rewards.get(rewardId); }
    public Collection<Reward> getAllRewards() { return new ArrayList<>(rewards.values()); }
    public boolean rewardExists(String rewardId) { return rewards.containsKey(rewardId); }
    public Set<String> getRewardIds() { return new HashSet<>(rewards.keySet()); }
    public void addReward(Reward reward) { rewards.put(reward.getId(), reward); }
    public void removeReward(String rewardId) { rewards.remove(rewardId); }

    public void saveRewards() {
        try {
            File rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(rewardsFile);
            config.set("rewards", null);

            for (Reward reward : rewards.values()) {
                saveReward(config, reward);
            }

            config.save(rewardsFile);
            plugin.getLogger().info("Saved " + rewards.size() + " rewards to rewards.yml");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save rewards!", e);
        }
    }

    private void saveReward(FileConfiguration config, Reward reward) {
        String path = "rewards." + reward.getId();

        config.set(path + ".chance", reward.getChance());

        if (!reward.getItems().isEmpty()) {
            List<Map<String, Object>> itemsList = new ArrayList<>();
            for (RewardItem item : reward.getItems()) {
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("material", item.getMaterial().name());
                itemData.put("amount", item.getAmountRange());
                itemData.put("chance", item.getChance());

                if (item.getName() != null) itemData.put("name", item.getName());
                if (item.getLore() != null && !item.getLore().isEmpty()) itemData.put("lore", item.getLore());
                if (item.getEnchantments() != null && !item.getEnchantments().isEmpty()) {
                    Map<String, Integer> enchMap = new HashMap<>();
                    for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                        enchMap.put(entry.getKey().getKey().getKey(), entry.getValue());
                    }
                    itemData.put("enchantments", enchMap);
                }
                if (item.getStoredEnchantments() != null && !item.getStoredEnchantments().isEmpty()) {
                    Map<String, Integer> enchMap = new HashMap<>();
                    for (Map.Entry<Enchantment, Integer> entry : item.getStoredEnchantments().entrySet()) {
                        enchMap.put(entry.getKey().getKey().getKey(), entry.getValue());
                    }
                    itemData.put("stored-enchantments", enchMap);
                }
                if (item.getSkullTexture() != null) itemData.put("skull-texture", item.getSkullTexture());

                itemsList.add(itemData);
            }
            config.set(path + ".items", itemsList);
        }

        if (reward.getMoneyRange() != null && !reward.getMoneyRange().isEmpty() && !reward.getMoneyRange().equals("0")) {
            config.set(path + ".money", reward.getMoneyRange());
        }

        if (reward.getExperience() > 0) {
            config.set(path + ".experience", reward.getExperience());
        }

        if (reward.getCommands() != null && !reward.getCommands().isEmpty()) {
            config.set(path + ".commands", reward.getCommands());
        }
    }

    public Reward getOrCreateReward(String rewardId) {
        if (rewards.containsKey(rewardId)) return rewards.get(rewardId);
        Reward newReward = new Reward(rewardId, 100.0, new ArrayList<>(), "0", 0, new ArrayList<>());
        rewards.put(rewardId, newReward);
        return newReward;
    }

    public void addItemToReward(String rewardId, Material material, String amountRange, double chance) {
        Reward reward = getOrCreateReward(rewardId);
        RewardItem item = new RewardItem(material, amountRange, chance, null, null, new HashMap<>(), new HashMap<>(), null);
        reward.getItems().add(item);
    }

    public void removeItemFromReward(String rewardId, int index) {
        Reward reward = rewards.get(rewardId);
        if (reward != null && index >= 0 && index < reward.getItems().size()) {
            reward.getItems().remove(index);
        }
    }

    public void setRewardMoney(String rewardId, String moneyRange) {
        Reward reward = getOrCreateReward(rewardId);
        Reward updatedReward = new Reward(
            reward.getId(), reward.getChance(), reward.getItems(),
            moneyRange, reward.getExperience(), reward.getCommands()
        );
        rewards.put(rewardId, updatedReward);
    }

    public void setRewardExperience(String rewardId, int experience) {
        Reward reward = getOrCreateReward(rewardId);
        Reward updatedReward = new Reward(
            reward.getId(), reward.getChance(), reward.getItems(),
            reward.getMoneyRange(), experience, reward.getCommands()
        );
        rewards.put(rewardId, updatedReward);
    }
}
