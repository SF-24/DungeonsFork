package dev.bekololek.dungeons.models;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

public class GridSlot {

    private final int gridX;
    private final int gridZ;
    private boolean occupied;
    private UUID occupyingPartyId;
    private long occupiedSince;

    public GridSlot(int gridX, int gridZ) {
        this.gridX = gridX;
        this.gridZ = gridZ;
        this.occupied = false;
        this.occupyingPartyId = null;
        this.occupiedSince = 0;
    }

    public int getGridX() { return gridX; }
    public int getGridZ() { return gridZ; }
    public boolean isOccupied() { return occupied; }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
        if (!occupied) {
            this.occupyingPartyId = null;
            this.occupiedSince = 0;
        }
    }

    public UUID getOccupyingPartyId() { return occupyingPartyId; }

    public void occupy(UUID partyId) {
        this.occupied = true;
        this.occupyingPartyId = partyId;
        this.occupiedSince = System.currentTimeMillis();
    }

    public void release() {
        this.occupied = false;
        this.occupyingPartyId = null;
        this.occupiedSince = 0;
    }

    public long getOccupiedSince() { return occupiedSince; }

    public Location getWorldLocation(World world, int slotSize, int padding, int baseY, int startX, int startZ) {
        int totalSize = slotSize + padding;
        int worldX = startX + (gridX * totalSize);
        int worldZ = startZ + (gridZ * totalSize);
        return new Location(world, worldX, baseY, worldZ);
    }

    public Location getCenterLocation(World world, int slotSize, int padding, int baseY, int startX, int startZ) {
        Location loc = getWorldLocation(world, slotSize, padding, baseY, startX, startZ);
        loc.add(slotSize / 2.0, 0, slotSize / 2.0);
        return loc;
    }

    public static GridSlot fromLocation(Location location, int slotSize, int padding, int startX, int startZ) {
        int totalSize = slotSize + padding;
        int gridX = Math.floorDiv(location.getBlockX() - startX, totalSize);
        int gridZ = Math.floorDiv(location.getBlockZ() - startZ, totalSize);
        return new GridSlot(gridX, gridZ);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GridSlot gridSlot = (GridSlot) o;
        return gridX == gridSlot.gridX && gridZ == gridSlot.gridZ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gridX, gridZ);
    }

    @Override
    public String toString() {
        return "GridSlot{x=" + gridX + ", z=" + gridZ + ", occupied=" + occupied + "}";
    }

    public String getCoordinateString() {
        return "[" + gridX + "," + gridZ + "]";
    }
}
