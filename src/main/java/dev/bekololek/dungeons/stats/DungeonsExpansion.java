package dev.bekololek.dungeons.stats;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.managers.StatsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class DungeonsExpansion extends PlaceholderExpansion {

    private final Main plugin;

    public DungeonsExpansion(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "dungeons"; }

    @Override
    public @NotNull String getAuthor() { return "Lolek"; }

    @Override
    public @NotNull String getVersion() { return "1"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        StatsManager stats = plugin.getStatsManager();
        if (stats == null) return null;

        // %dungeons_stat_<name>%
        if (params.startsWith("stat_")) {
            if (player == null) return "0";
            String statName = params.substring(5);
            Object value = stats.getPlayerStat(player.getUniqueId(), statName);
            return String.valueOf(value);
        }

        // %dungeons_global_<name>%
        if (params.startsWith("global_")) {
            String statName = params.substring(7);
            Object value = stats.getGlobalStat(statName);
            return String.valueOf(value);
        }

        // %dungeons_top_<name>_<pos>%
        if (params.startsWith("top_")) {
            String rest = params.substring(4);
            int lastUnderscore = rest.lastIndexOf('_');
            if (lastUnderscore == -1) return null;

            String statName = rest.substring(0, lastUnderscore);
            try {
                int pos = Integer.parseInt(rest.substring(lastUnderscore + 1));
                List<Map.Entry<String, Number>> top = stats.getTopPlayers(statName, pos);
                if (pos > 0 && pos <= top.size()) {
                    return top.get(pos - 1).getKey();
                }
                return "-";
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // %dungeons_topvalue_<name>_<pos>%
        if (params.startsWith("topvalue_")) {
            String rest = params.substring(9);
            int lastUnderscore = rest.lastIndexOf('_');
            if (lastUnderscore == -1) return null;

            String statName = rest.substring(0, lastUnderscore);
            try {
                int pos = Integer.parseInt(rest.substring(lastUnderscore + 1));
                List<Map.Entry<String, Number>> top = stats.getTopPlayers(statName, pos);
                if (pos > 0 && pos <= top.size()) {
                    return String.valueOf(top.get(pos - 1).getValue());
                }
                return "0";
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }
}
