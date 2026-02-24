package dev.bekololek.dungeons.utils;

import dev.bekololek.dungeons.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageUtil {

    private static Main plugin;
    private static FileConfiguration messages;
    private static String prefix;

    public static void initialize(Main pluginInstance) {
        plugin = pluginInstance;
        loadMessages();
    }

    public static void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = messages.getString("prefix", "&8[&6Dungeons&8]&r ");
    }

    public static String getMessage(String path) {
        return messages.getString(path, "Message not found: " + path);
    }

    public static String getMessage(String path, Map<String, String> replacements) {
        String message = getMessage(path);

        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        return message;
    }

    public static void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, null, true);
    }

    public static void sendMessage(CommandSender sender, String path, Map<String, String> replacements) {
        sendMessage(sender, path, replacements, true);
    }

    public static void sendMessage(CommandSender sender, String path, Map<String, String> replacements, boolean usePrefix) {
        String message = getMessage(path, replacements);

        if (usePrefix) {
            message = prefix + message;
        }

        sender.sendMessage(ColorUtil.toComponent(message));
    }

    public static void sendRaw(CommandSender sender, String message) {
        sendRaw(sender, message, true);
    }

    public static void sendRaw(CommandSender sender, String message, boolean usePrefix) {
        if (usePrefix) {
            message = prefix + message;
        }
        sender.sendMessage(ColorUtil.toComponent(message));
    }

    public static void sendActionBar(Player player, String message) {
        player.sendActionBar(ColorUtil.toComponent(message));
    }

    public static void broadcast(String path) {
        broadcast(path, null);
    }

    public static void broadcast(String path, Map<String, String> replacements) {
        String message = getMessage(path, replacements);
        message = prefix + message;

        Component component = ColorUtil.toComponent(message);
        plugin.getServer().broadcast(component);
    }

    public static Map<String, String> replacements() {
        return new HashMap<>();
    }

    public static Map<String, String> replacement(String key, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    public static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long secs = seconds % 60;
            return minutes + "m " + secs + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }

    public static String formatMoney(double amount) {
        return String.format("$%.2f", amount);
    }

    public static String getPrefix() {
        return prefix;
    }
}
