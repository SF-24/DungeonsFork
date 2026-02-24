package dev.bekololek.dungeons.managers;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.models.GridSlot;
import dev.bekololek.dungeons.utils.EmptyChunkGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class GridManager {

    private final Main plugin;
    private final Map<GridSlot, UUID> occupiedSlots;
    private final Set<GridSlot> availableSlots;

    private World dungeonWorld;
    private String gridMode;
    private int slotSize;
    private int padding;
    private int baseY;
    private int maxSlots;
    private int startX;
    private int startZ;

    public GridManager(Main plugin) {
        this.plugin = plugin;
        this.occupiedSlots = new ConcurrentHashMap<>();
        this.availableSlots = ConcurrentHashMap.newKeySet();
        loadConfiguration();
    }

    private void loadConfiguration() {
        this.gridMode = plugin.getConfig().getString("grid.mode", "auto").toLowerCase();
        this.slotSize = plugin.getConfig().getInt("grid.slot-size", 256);
        this.padding = plugin.getConfig().getInt("grid.padding", 100);
        this.baseY = plugin.getConfig().getInt("grid.base-y-level", 100);
        this.maxSlots = plugin.getConfig().getInt("grid.max-slots", 100);

        if ("manual".equals(gridMode)) {
            this.startX = plugin.getConfig().getInt("grid.manual.start-x", 10000);
            this.startZ = plugin.getConfig().getInt("grid.manual.start-z", 10000);
        } else {
            this.startX = 0;
            this.startZ = 0;
        }

        plugin.getLogger().info("Grid configuration loaded (" + gridMode.toUpperCase() + " mode): "
                + "slotSize=" + slotSize + ", padding=" + padding + ", baseY=" + baseY + ", maxSlots=" + maxSlots);
    }

    public void initializeWorld() {
        if ("manual".equals(gridMode)) {
            String worldName = plugin.getConfig().getString("grid.manual.world-name", "world");
            dungeonWorld = Bukkit.getWorld(worldName);

            if (dungeonWorld == null) {
                plugin.getLogger().severe("Manual mode: World '" + worldName + "' not found!");
                return;
            }
            plugin.getLogger().info("Manual mode: Using existing world '" + worldName + "'");
        } else {
            String worldName = plugin.getConfig().getString("grid.auto.world-name", "dungeon_world");
            dungeonWorld = Bukkit.getWorld(worldName);

            if (dungeonWorld == null) {
                plugin.getLogger().info("Auto mode: Creating dungeon world: " + worldName);

                WorldCreator creator = new WorldCreator(worldName);
                creator.type(WorldType.FLAT);
                creator.generateStructures(false);
                creator.generator(new EmptyChunkGenerator());

                dungeonWorld = creator.createWorld();

                if (dungeonWorld != null) {
                    dungeonWorld.setAutoSave(false);
                    dungeonWorld.setKeepSpawnInMemory(false);
                    plugin.getLogger().info("Auto mode: Dungeon world created successfully");
                } else {
                    plugin.getLogger().severe("Auto mode: Failed to create dungeon world!");
                    return;
                }
            } else {
                plugin.getLogger().info("Auto mode: Dungeon world already exists: " + worldName);
            }
        }

        generateAvailableSlots();
    }

    private void generateAvailableSlots() {
        int gridSize = (int) Math.ceil(Math.sqrt(maxSlots));
        for (int x = 0; x < gridSize; x++) {
            for (int z = 0; z < gridSize; z++) {
                availableSlots.add(new GridSlot(x, z));
                if (availableSlots.size() >= maxSlots) {
                    plugin.getLogger().info("Pre-generated " + maxSlots + " grid slots");
                    return;
                }
            }
        }
        plugin.getLogger().info("Pre-generated " + availableSlots.size() + " grid slots");
    }

    public World getDungeonWorld() { return dungeonWorld; }

    public GridSlot allocateSlot(UUID partyId) {
        if (availableSlots.isEmpty()) {
            plugin.getLogger().warning("No available grid slots!");
            return null;
        }

        GridSlot slot = availableSlots.iterator().next();
        availableSlots.remove(slot);
        slot.occupy(partyId);
        occupiedSlots.put(slot, partyId);

        plugin.getLogger().info("Allocated grid slot " + slot.getCoordinateString() + " to party " + partyId);
        return slot;
    }

    public void releaseSlot(GridSlot slot) {
        if (slot == null) return;
        occupiedSlots.remove(slot);
        slot.release();
        availableSlots.add(slot);
        plugin.getLogger().info("Released grid slot " + slot.getCoordinateString());
    }

    public void releaseSlotByParty(UUID partyId) {
        GridSlot slot = getSlotByParty(partyId);
        if (slot != null) releaseSlot(slot);
    }

    public GridSlot getSlotByParty(UUID partyId) {
        for (Map.Entry<GridSlot, UUID> entry : occupiedSlots.entrySet()) {
            if (entry.getValue().equals(partyId)) return entry.getKey();
        }
        return null;
    }

    public Location getSlotLocation(GridSlot slot) {
        if (dungeonWorld == null) return null;
        return slot.getWorldLocation(dungeonWorld, slotSize, padding, baseY, startX, startZ);
    }

    public Location getSlotCenterLocation(GridSlot slot) {
        if (dungeonWorld == null) return null;
        return slot.getCenterLocation(dungeonWorld, slotSize, padding, baseY, startX, startZ);
    }

    public GridSlot getSlotFromLocation(Location location) {
        if (location.getWorld() != dungeonWorld) return null;
        return GridSlot.fromLocation(location, slotSize, padding, startX, startZ);
    }

    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total_slots", maxSlots);
        stats.put("occupied_slots", occupiedSlots.size());
        stats.put("available_slots", availableSlots.size());
        return stats;
    }

    public Map<GridSlot, UUID> getOccupiedSlots() { return new HashMap<>(occupiedSlots); }

    public void releaseAllSlots() {
        plugin.getLogger().warning("Force releasing all grid slots!");
        for (GridSlot slot : new HashSet<>(occupiedSlots.keySet())) {
            releaseSlot(slot);
        }
        occupiedSlots.clear();
    }

    public void cleanupExpiredSlots(long maxAgeMillis) {
        long now = System.currentTimeMillis();
        List<GridSlot> toRelease = new ArrayList<>();
        for (GridSlot slot : occupiedSlots.keySet()) {
            if (slot.getOccupiedSince() > 0 && (now - slot.getOccupiedSince()) > maxAgeMillis) {
                toRelease.add(slot);
            }
        }
        if (!toRelease.isEmpty()) {
            plugin.getLogger().info("Cleaning up " + toRelease.size() + " expired grid slots");
            for (GridSlot slot : toRelease) releaseSlot(slot);
        }
    }

    public int getSlotSize() { return slotSize; }
    public int getPadding() { return padding; }
    public int getBaseY() { return baseY; }
    public int getMaxSlots() { return maxSlots; }
    public String getGridMode() { return gridMode; }
    public int getStartX() { return startX; }
    public int getStartZ() { return startZ; }
}
