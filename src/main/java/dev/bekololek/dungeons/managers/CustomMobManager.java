package dev.bekololek.dungeons.managers;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.models.CustomMob;
import dev.bekololek.dungeons.models.Dungeon;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class CustomMobManager {

    private final Main plugin;
    private final Map<String, Map<String, CustomMob>> dungeonMobs;

    public CustomMobManager(Main plugin) {
        this.plugin = plugin;
        this.dungeonMobs = new HashMap<>();
    }

    public void loadDungeonMobs(Dungeon dungeon, FileConfiguration config) {
        String dungeonId = dungeon.getId();
        Map<String, CustomMob> mobs = new HashMap<>();

        ConfigurationSection mobsSection = config.getConfigurationSection("custom-mobs");
        if (mobsSection == null) {
            dungeonMobs.put(dungeonId, mobs);
            return;
        }

        for (String mobId : mobsSection.getKeys(false)) {
            try {
                CustomMob mob = loadCustomMob(mobId, mobsSection.getConfigurationSection(mobId));
                if (mob != null) {
                    mobs.put(mobId, mob);
                    plugin.getLogger().info("Loaded custom mob '" + mobId + "' for dungeon " + dungeonId);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load custom mob '" + mobId + "' for dungeon " + dungeonId, e);
            }
        }

        dungeonMobs.put(dungeonId, mobs);
        plugin.getLogger().info("Loaded " + mobs.size() + " custom mobs for dungeon " + dungeonId);
    }

    private CustomMob loadCustomMob(String mobId, ConfigurationSection section) {
        if (section == null) return null;

        String typeStr = section.getString("type", "ZOMBIE");
        EntityType type;
        try {
            type = EntityType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid entity type for custom mob " + mobId + ": " + typeStr);
            return null;
        }

        CustomMob mob = new CustomMob(mobId, type);

        mob.setCustomName(section.getString("name"));
        mob.setShowName(section.getBoolean("show-name", true));
        mob.setHealth(section.getDouble("health", -1));
        mob.setDamage(section.getDouble("damage", -1));
        mob.setSpeed(section.getDouble("speed", -1));

        mob.setHelmet(loadItemStack(section.getConfigurationSection("equipment.helmet")));
        mob.setHelmetDropChance((float) section.getDouble("equipment.helmet.drop-chance", 0.0));

        mob.setChestplate(loadItemStack(section.getConfigurationSection("equipment.chestplate")));
        mob.setChestplateDropChance((float) section.getDouble("equipment.chestplate.drop-chance", 0.0));

        mob.setLeggings(loadItemStack(section.getConfigurationSection("equipment.leggings")));
        mob.setLeggingsDropChance((float) section.getDouble("equipment.leggings.drop-chance", 0.0));

        mob.setBoots(loadItemStack(section.getConfigurationSection("equipment.boots")));
        mob.setBootsDropChance((float) section.getDouble("equipment.boots.drop-chance", 0.0));

        mob.setMainHand(loadItemStack(section.getConfigurationSection("equipment.main-hand")));
        mob.setMainHandDropChance((float) section.getDouble("equipment.main-hand.drop-chance", 0.0));

        mob.setOffHand(loadItemStack(section.getConfigurationSection("equipment.off-hand")));
        mob.setOffHandDropChance((float) section.getDouble("equipment.off-hand.drop-chance", 0.0));

        ConfigurationSection effectsSection = section.getConfigurationSection("potion-effects");
        if (effectsSection != null) {
            for (String effectName : effectsSection.getKeys(false)) {
                try {
                    NamespacedKey key = NamespacedKey.minecraft(effectName.toLowerCase());
                    PotionEffectType effectType = Registry.EFFECT.get(key);

                    if (effectType != null) {
                        int duration = effectsSection.getInt(effectName + ".duration", 600);
                        int amplifier = effectsSection.getInt(effectName + ".amplifier", 0);
                        mob.addPotionEffect(new PotionEffect(effectType, duration * 20, amplifier));
                    } else {
                        plugin.getLogger().warning("Invalid potion effect: " + effectName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid potion effect: " + effectName);
                }
            }
        }

        return mob;
    }

    private ItemStack loadItemStack(ConfigurationSection section) {
        if (section == null) return null;

        String materialStr = section.getString("material");
        if (materialStr == null) return null;

        try {
            Material material = Material.valueOf(materialStr.toUpperCase());
            ItemStack item = new ItemStack(material);

            ConfigurationSection enchantsSection = section.getConfigurationSection("enchantments");
            if (enchantsSection != null) {
                for (String enchantName : enchantsSection.getKeys(false)) {
                    try {
                        NamespacedKey key = NamespacedKey.minecraft(enchantName.toLowerCase());
                        Enchantment enchant = Registry.ENCHANTMENT.get(key);

                        if (enchant != null) {
                            int level = enchantsSection.getInt(enchantName);
                            item.addUnsafeEnchantment(enchant, level);
                        } else {
                            plugin.getLogger().warning("Invalid enchantment: " + enchantName);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid enchantment: " + enchantName);
                    }
                }
            }

            return item;
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material: " + materialStr);
            return null;
        }
    }

    public CustomMob getCustomMob(String dungeonId, String mobId) {
        Map<String, CustomMob> mobs = dungeonMobs.get(dungeonId);
        if (mobs == null) return null;
        return mobs.get(mobId);
    }

    public Map<String, CustomMob> getDungeonMobs(String dungeonId) {
        return dungeonMobs.getOrDefault(dungeonId, new HashMap<>());
    }

    public void setCustomMob(String dungeonId, CustomMob mob) {
        dungeonMobs.computeIfAbsent(dungeonId, k -> new HashMap<>()).put(mob.getId(), mob);
    }

    public void removeCustomMob(String dungeonId, String mobId) {
        Map<String, CustomMob> mobs = dungeonMobs.get(dungeonId);
        if (mobs != null) {
            mobs.remove(mobId);
        }
    }

    public void saveDungeonMobs(String dungeonId) {
        Map<String, CustomMob> mobs = dungeonMobs.get(dungeonId);
        if (mobs == null || mobs.isEmpty()) return;

        File dungeonsFolder = new File(plugin.getDataFolder(), "dungeons");
        File dungeonFile = new File(dungeonsFolder, dungeonId + ".yml");

        if (!dungeonFile.exists()) {
            plugin.getLogger().warning("Cannot save custom mobs - dungeon file not found: " + dungeonId);
            return;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(dungeonFile);
            config.set("custom-mobs", null);

            for (CustomMob mob : mobs.values()) {
                String path = "custom-mobs." + mob.getId();

                config.set(path + ".type", mob.getEntityType().name());
                if (mob.getCustomName() != null) {
                    config.set(path + ".name", mob.getCustomName());
                }
                config.set(path + ".show-name", mob.isShowName());
                if (mob.getHealth() > 0) {
                    config.set(path + ".health", mob.getHealth());
                }
                if (mob.getDamage() > 0) {
                    config.set(path + ".damage", mob.getDamage());
                }
                if (mob.getSpeed() > 0) {
                    config.set(path + ".speed", mob.getSpeed());
                }

                saveItemStack(config, path + ".equipment.helmet", mob.getHelmet(), mob.getHelmetDropChance());
                saveItemStack(config, path + ".equipment.chestplate", mob.getChestplate(), mob.getChestplateDropChance());
                saveItemStack(config, path + ".equipment.leggings", mob.getLeggings(), mob.getLeggingsDropChance());
                saveItemStack(config, path + ".equipment.boots", mob.getBoots(), mob.getBootsDropChance());
                saveItemStack(config, path + ".equipment.main-hand", mob.getMainHand(), mob.getMainHandDropChance());
                saveItemStack(config, path + ".equipment.off-hand", mob.getOffHand(), mob.getOffHandDropChance());

                for (PotionEffect effect : mob.getPotionEffects()) {
                    String effectPath = path + ".potion-effects." + effect.getType().getKey().getKey();
                    config.set(effectPath + ".duration", effect.getDuration() / 20);
                    config.set(effectPath + ".amplifier", effect.getAmplifier());
                }
            }

            config.save(dungeonFile);
            plugin.getLogger().info("Saved " + mobs.size() + " custom mobs for dungeon " + dungeonId);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save custom mobs for dungeon " + dungeonId, e);
        }
    }

    private void saveItemStack(FileConfiguration config, String path, ItemStack item, float dropChance) {
        if (item == null) return;

        config.set(path + ".material", item.getType().name());
        config.set(path + ".drop-chance", dropChance);

        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                config.set(path + ".enchantments." + entry.getKey().getKey().getKey(), entry.getValue());
            }
        }
    }

    public void clear() {
        dungeonMobs.clear();
    }
}
