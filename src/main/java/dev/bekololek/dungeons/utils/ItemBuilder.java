package dev.bekololek.dungeons.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.itemStack = new ItemStack(material, amount);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder setName(String name) {
        if (itemMeta != null && name != null) {
            itemMeta.setDisplayName(ColorUtil.translate(name));
        }
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        if (itemMeta != null && lore != null) {
            List<String> translatedLore = new ArrayList<>();
            for (String line : lore) {
                translatedLore.add(ColorUtil.translate(line));
            }
            itemMeta.setLore(translatedLore);
        }
        return this;
    }

    public ItemBuilder addLoreLine(String line) {
        if (itemMeta != null && line != null) {
            List<String> lore = itemMeta.hasLore() ? itemMeta.getLore() : new ArrayList<>();
            if (lore != null) {
                lore.add(ColorUtil.translate(line));
                itemMeta.setLore(lore);
            }
        }
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        if (itemMeta != null) {
            itemMeta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    public ItemBuilder addEnchantments(Map<Enchantment, Integer> enchantments) {
        if (enchantments != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                addEnchantment(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    public ItemBuilder addStoredEnchantment(Enchantment enchantment, int level) {
        if (itemMeta instanceof EnchantmentStorageMeta) {
            ((EnchantmentStorageMeta) itemMeta).addStoredEnchant(enchantment, level, true);
        }
        return this;
    }

    public ItemBuilder addStoredEnchantments(Map<Enchantment, Integer> enchantments) {
        if (enchantments != null) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                addStoredEnchantment(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        if (itemMeta != null) {
            itemMeta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        if (itemMeta != null) {
            itemMeta.setUnbreakable(unbreakable);
        }
        return this;
    }

    public ItemBuilder setCustomModelData(int data) {
        if (itemMeta != null) {
            itemMeta.setCustomModelData(data);
        }
        return this;
    }

    public ItemBuilder setSkullTexture(String texture) {
        if (itemMeta instanceof SkullMeta && texture != null && !texture.isEmpty()) {
            SkullMeta skullMeta = (SkullMeta) itemMeta;

            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.setProperty(new ProfileProperty("textures", texture));
            skullMeta.setPlayerProfile(profile);
        }
        return this;
    }

    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    public static ItemStack createItem(Material material, String name) {
        return new ItemBuilder(material).setName(name).build();
    }

    public static ItemStack createItem(Material material, String name, List<String> lore) {
        return new ItemBuilder(material).setName(name).setLore(lore).build();
    }
}
