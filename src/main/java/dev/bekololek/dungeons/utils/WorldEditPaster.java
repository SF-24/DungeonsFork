package dev.bekololek.dungeons.utils;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import dev.bekololek.dungeons.Main;
import org.bukkit.Location;

import java.io.File;
import java.io.FileInputStream;

/**
 * Separate class for WorldEdit API calls.
 * This prevents ClassNotFoundException when WorldEdit is not available.
 */
public class WorldEditPaster {

    public static boolean paste(Main plugin, File schematicFile, Location location) {
        try {
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            if (format == null) {
                plugin.getLogger().warning("Unknown schematic format for: " + schematicFile.getName());
                return false;
            }

            Clipboard clipboard;
            try (FileInputStream fis = new FileInputStream(schematicFile);
                 ClipboardReader reader = format.getReader(fis)) {
                clipboard = reader.read();
            }

            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(location.getWorld());
            BlockVector3 position = BlockVector3.at(location.getX(), location.getY(), location.getZ());

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                editSession.setReorderMode(EditSession.ReorderMode.FAST);

                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(position)
                        .ignoreAirBlocks(false)
                        .build();

                Operations.complete(operation);
            }

            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("WorldEdit paste error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return false;
        }
    }

    public static boolean clear(Main plugin, Location location, int sizeX, int sizeY, int sizeZ) {
        try {
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(location.getWorld());

            BlockVector3 min = BlockVector3.at(
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()
            );

            BlockVector3 max = BlockVector3.at(
                    location.getBlockX() + sizeX,
                    location.getBlockY() + sizeY,
                    location.getBlockZ() + sizeZ
            );

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                editSession.setReorderMode(EditSession.ReorderMode.FAST);
                editSession.setBlocks(new CuboidRegion(world, min, max), BlockTypes.AIR.getDefaultState());
            }

            return true;

        } catch (Exception e) {
            plugin.getLogger().fine("WorldEdit clear failed: " + e.getMessage());
            return false;
        }
    }
}
