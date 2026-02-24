package dev.bekololek.dungeons.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import dev.bekololek.dungeons.Main;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldGuardIntegration {

    private final Main plugin;
    private boolean enabled;

    public WorldGuardIntegration(Main plugin) {
        this.plugin = plugin;
        this.enabled = false;
    }

    public boolean initialize() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
            plugin.getLogger().info("WorldGuard not found, region protection disabled");
            enabled = false;
            return false;
        }

        boolean useWorldGuard = plugin.getConfig().getBoolean("worldguard.enabled", true);
        if (!useWorldGuard) {
            plugin.getLogger().info("WorldGuard integration disabled in config");
            enabled = false;
            return false;
        }

        plugin.getLogger().info("WorldGuard integration enabled");
        enabled = true;
        return true;
    }

    public boolean isEnabled() { return enabled; }

    public boolean createRegion(UUID instanceId, Location corner1, Location corner2) {
        if (!enabled) return false;

        try {
            World world = corner1.getWorld();
            if (world == null) return false;

            BlockVector3 min = BlockVector3.at(
                    Math.min(corner1.getBlockX(), corner2.getBlockX()),
                    Math.min(corner1.getBlockY(), corner2.getBlockY()),
                    Math.min(corner1.getBlockZ(), corner2.getBlockZ()));

            BlockVector3 max = BlockVector3.at(
                    Math.max(corner1.getBlockX(), corner2.getBlockX()),
                    Math.max(corner1.getBlockY(), corner2.getBlockY()),
                    Math.max(corner1.getBlockZ(), corner2.getBlockZ()));

            String regionId = "dungeon_" + instanceId.toString();
            ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);
            applyFlags(region);

            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));

            if (regionManager == null) {
                plugin.getLogger().warning("Failed to get RegionManager for world: " + world.getName());
                return false;
            }

            regionManager.addRegion(region);
            plugin.getLogger().fine("Created WorldGuard region: " + regionId);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create WorldGuard region: " + e.getMessage());
            return false;
        }
    }

    public void deleteRegion(UUID instanceId, World world) {
        if (!enabled || world == null) return;

        try {
            String regionId = "dungeon_" + instanceId.toString();
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            if (regionManager == null) return;
            regionManager.removeRegion(regionId);
            plugin.getLogger().fine("Deleted WorldGuard region: " + regionId);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to delete WorldGuard region: " + e.getMessage());
        }
    }

    private void applyFlags(ProtectedCuboidRegion region) {
        Map<String, Object> flagConfig = new HashMap<>();
        flagConfig.put("block-break", plugin.getConfig().getBoolean("worldguard.flags.block-break", false));
        flagConfig.put("block-place", plugin.getConfig().getBoolean("worldguard.flags.block-place", false));
        flagConfig.put("explosion", plugin.getConfig().getBoolean("worldguard.flags.explosion", false));
        flagConfig.put("fire-spread", plugin.getConfig().getBoolean("worldguard.flags.fire-spread", false));
        flagConfig.put("lava-fire", plugin.getConfig().getBoolean("worldguard.flags.lava-fire", false));
        flagConfig.put("lighter", plugin.getConfig().getBoolean("worldguard.flags.lighter", false));
        flagConfig.put("pvp", plugin.getConfig().getBoolean("worldguard.flags.pvp", true));
        flagConfig.put("mob-damage", plugin.getConfig().getBoolean("worldguard.flags.mob-damage", true));
        flagConfig.put("mob-spawning", plugin.getConfig().getBoolean("worldguard.flags.mob-spawning", true));
        flagConfig.put("creeper-explosion", plugin.getConfig().getBoolean("worldguard.flags.creeper-explosion", false));
        flagConfig.put("ghast-fireball", plugin.getConfig().getBoolean("worldguard.flags.ghast-fireball", false));

        setFlag(region, Flags.BLOCK_BREAK, flagConfig.get("block-break"));
        setFlag(region, Flags.BLOCK_PLACE, flagConfig.get("block-place"));
        setFlag(region, Flags.TNT, flagConfig.get("explosion"));
        setFlag(region, Flags.OTHER_EXPLOSION, flagConfig.get("explosion"));
        setFlag(region, Flags.FIRE_SPREAD, flagConfig.get("fire-spread"));
        setFlag(region, Flags.LAVA_FIRE, flagConfig.get("lava-fire"));
        setFlag(region, Flags.LIGHTER, flagConfig.get("lighter"));
        setFlag(region, Flags.PVP, flagConfig.get("pvp"));
        setFlag(region, Flags.MOB_DAMAGE, flagConfig.get("mob-damage"));
        setFlag(region, Flags.MOB_SPAWNING, flagConfig.get("mob-spawning"));
        setFlag(region, Flags.CREEPER_EXPLOSION, flagConfig.get("creeper-explosion"));
        setFlag(region, Flags.GHAST_FIREBALL, flagConfig.get("ghast-fireball"));
    }

    private void setFlag(ProtectedCuboidRegion region, Flag<?> flag, Object value) {
        if (flag instanceof StateFlag && value instanceof Boolean) {
            region.setFlag((StateFlag) flag, (Boolean) value ? StateFlag.State.ALLOW : StateFlag.State.DENY);
        }
    }
}
