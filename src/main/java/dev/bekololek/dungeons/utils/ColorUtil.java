package dev.bekololek.dungeons.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

public class ColorUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    public static String translate(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        if (text.contains("<") && text.contains(">")) {
            try {
                Component component = MINI_MESSAGE.deserialize(text);
                return LEGACY_SERIALIZER.serialize(component);
            } catch (Exception ignored) {
            }
        }

        return text.replace('&', '\u00A7');
    }

    public static List<String> translate(List<String> texts) {
        if (texts == null) {
            return new ArrayList<>();
        }

        List<String> translated = new ArrayList<>();
        for (String text : texts) {
            translated.add(translate(text));
        }
        return translated;
    }

    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        if (text.contains("<") && text.contains(">")) {
            try {
                return MINI_MESSAGE.deserialize(text);
            } catch (Exception ignored) {
            }
        }

        return LEGACY_SERIALIZER.deserialize(text);
    }

    public static String stripColor(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replaceAll("&[0-9a-fk-or]", "").replaceAll("\u00A7[0-9a-fk-or]", "");
    }

    public static void sendColored(org.bukkit.command.CommandSender sender, String message) {
        if (sender != null && message != null) {
            sender.sendMessage(toComponent(message));
        }
    }
}
