package dev.bekololek.dungeons.utils;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import dev.bekololek.dungeons.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;

public class SchematicLoader {

    private final Main plugin;
    private final File schematicsFolder;

    public SchematicLoader(Main plugin) {
        this.plugin = plugin;
        this.schematicsFolder = new File(plugin.getDataFolder(), "schematics");

        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();
            plugin.getLogger().info("Created schematics folder at: " + schematicsFolder.getAbsolutePath());
        }
    }

    public boolean pasteSchematic(String schematicName, Location location) {
        File schematicFile = new File(schematicsFolder, schematicName);

        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Schematic file not found: " + schematicFile.getAbsolutePath());
            return false;
        }

        try {
            if (tryWorldEditPaste(schematicFile, location)) {
                plugin.getLogger().info("Successfully pasted schematic '" + schematicName + "' using WorldEdit at " +
                        location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
                return true;
            }

            plugin.getLogger().warning("WorldEdit paste failed or unavailable, using fallback loader");
            return pasteFallback(schematicFile, location);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to paste schematic: " + schematicName, e);
            return false;
        }
    }

    private boolean tryWorldEditPaste(File schematicFile, Location location) {
        try {
            Class.forName("com.sk89q.worldedit.WorldEdit");
            Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");

            return WorldEditPaster.paste(plugin, schematicFile, location);

        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("WorldEdit not found, using fallback loader");
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("WorldEdit paste failed: " + e.getMessage());
            return false;
        }
    }

    private boolean pasteFallback(File schematicFile, Location location) {
        plugin.getLogger().warning("Using fallback schematic loader - creating basic platform");

        int size = 50;
        Location start = location.clone();

        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                Block block = start.clone().add(x, 0, z).getBlock();
                block.setType(Material.STONE);

                if ((x + z) % 10 == 0) {
                    block.setType(Material.COBBLESTONE);
                }
            }
        }

        plugin.getLogger().warning("Created basic " + size + "x" + size + " platform as fallback");
        return true;
    }

    public void clearArea(Location location, int sizeX, int sizeY, int sizeZ) {
        try {
            if (tryWorldEditClear(location, sizeX, sizeY, sizeZ)) {
                return;
            }

            clearAreaManually(location, sizeX, sizeY, sizeZ);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to clear area", e);
        }
    }

    private boolean tryWorldEditClear(Location location, int sizeX, int sizeY, int sizeZ) {
        try {
            Class.forName("com.sk89q.worldedit.WorldEdit");
            return WorldEditPaster.clear(plugin, location, sizeX, sizeY, sizeZ);
        } catch (Exception e) {
            return false;
        }
    }

    private void clearAreaManually(Location location, int sizeX, int sizeY, int sizeZ) {
        plugin.getLogger().fine("Clearing area manually at " + location.getBlockX() + ", " +
                location.getBlockY() + ", " + location.getBlockZ());

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    Block block = location.clone().add(x, y, z).getBlock();
                    block.setType(Material.AIR);
                }
            }
        }
    }

    public boolean schematicExists(String schematicName) {
        File schematicFile = new File(schematicsFolder, schematicName);
        return schematicFile.exists();
    }

    public File getSchematicFile(String schematicName) {
        return new File(schematicsFolder, schematicName);
    }

    public File getSchematicsFolder() {
        return schematicsFolder;
    }

    public int[] getSchematicDimensions(String schematicName) {
        File schematicFile = new File(schematicsFolder, schematicName);

        if (!schematicFile.exists()) {
            return new int[]{0, 0, 0};
        }

        try {
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            if (format == null) {
                return new int[]{0, 0, 0};
            }

            Clipboard clipboard;
            try (FileInputStream fis = new FileInputStream(schematicFile);
                 ClipboardReader reader = format.getReader(fis)) {
                clipboard = reader.read();
            }

            BlockVector3 dimensions = clipboard.getDimensions();
            return new int[]{dimensions.x(), dimensions.y(), dimensions.z()};

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get schematic dimensions: " + schematicName, e);
            return new int[]{0, 0, 0};
        }
    }
}
