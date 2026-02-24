package dev.bekololek.dungeons.integrations;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import dev.bekololek.dungeons.Main;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;

public class WorldEditIntegration {

    private final Main plugin;
    private final WorldEdit worldEdit;
    private boolean enabled;

    public WorldEditIntegration(Main plugin) {
        this.plugin = plugin;
        this.worldEdit = WorldEdit.getInstance();
        this.enabled = false;
    }

    public void initialize() {
        try {
            if (plugin.getServer().getPluginManager().getPlugin("WorldEdit") == null) {
                plugin.getLogger().warning("WorldEdit not found - schematic functionality will be disabled");
                enabled = false;
                return;
            }
            plugin.getLogger().info("WorldEdit integration enabled");
            enabled = true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize WorldEdit integration", e);
            enabled = false;
        }
    }

    public boolean isEnabled() { return enabled; }

    public Clipboard loadSchematic(String schematicName) throws IOException {
        File schematicsFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicsFolder.exists()) schematicsFolder.mkdirs();

        File schematicFile = new File(schematicsFolder, schematicName);
        if (!schematicFile.exists()) throw new IOException("Schematic file not found: " + schematicName);

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null) throw new IOException("Unknown schematic format: " + schematicName);

        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            return reader.read();
        }
    }

    public boolean pasteSchematic(Clipboard clipboard, Location location) {
        if (!enabled) {
            plugin.getLogger().warning("Cannot paste schematic - WorldEdit not enabled");
            return false;
        }

        try {
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(location.getWorld());
            BlockVector3 pasteLocation = BlockVector3.at(location.getX(), location.getY(), location.getZ());

            try (EditSession editSession = worldEdit.newEditSession(weWorld)) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(pasteLocation)
                        .ignoreAirBlocks(false)
                        .build();
                Operations.complete(operation);
            }

            plugin.getLogger().info("Pasted schematic at: " + location.getBlockX() + ", "
                    + location.getBlockY() + ", " + location.getBlockZ());
            return true;
        } catch (WorldEditException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to paste schematic", e);
            return false;
        }
    }

    public boolean saveSchematic(String schematicName, Location pos1, Location pos2) {
        if (!enabled) {
            plugin.getLogger().warning("Cannot save schematic - WorldEdit not enabled");
            return false;
        }

        if (pos1.getWorld() != pos2.getWorld()) {
            plugin.getLogger().warning("Positions must be in the same world");
            return false;
        }

        try {
            File schematicsFolder = new File(plugin.getDataFolder(), "schematics");
            if (!schematicsFolder.exists()) schematicsFolder.mkdirs();

            File schematicFile = new File(schematicsFolder, schematicName);
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(pos1.getWorld());
            BlockVector3 wePos1 = BlockVector3.at(pos1.getX(), pos1.getY(), pos1.getZ());
            BlockVector3 wePos2 = BlockVector3.at(pos2.getX(), pos2.getY(), pos2.getZ());

            CuboidRegion region = new CuboidRegion(weWorld, wePos1, wePos2);

            Clipboard clipboard;
            try (EditSession editSession = worldEdit.newEditSession(weWorld)) {
                clipboard = new com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard(region);
                clipboard.setOrigin(wePos1);

                com.sk89q.worldedit.function.operation.ForwardExtentCopy copy =
                        new com.sk89q.worldedit.function.operation.ForwardExtentCopy(
                                editSession, region, clipboard, region.getMinimumPoint());
                copy.setCopyingEntities(true);
                copy.setCopyingBiomes(false);
                Operations.complete(copy);
            }

            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            if (format == null) format = ClipboardFormats.findByAlias("sponge");

            try (ClipboardWriter writer = format.getWriter(new FileOutputStream(schematicFile))) {
                writer.write(clipboard);
            }

            plugin.getLogger().info("Saved schematic: " + schematicName);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save schematic: " + schematicName, e);
            return false;
        }
    }

    public Region getSelection(Player player) {
        try {
            com.sk89q.worldedit.bukkit.BukkitPlayer wePlayer = BukkitAdapter.adapt(player);
            com.sk89q.worldedit.LocalSession session = worldEdit.getSessionManager().get(wePlayer);
            return session.getSelection(BukkitAdapter.adapt(player.getWorld()));
        } catch (Exception e) {
            return null;
        }
    }

    public void setSelection(Player player, Location pos1, Location pos2) {
        try {
            com.sk89q.worldedit.bukkit.BukkitPlayer wePlayer = BukkitAdapter.adapt(player);
            com.sk89q.worldedit.LocalSession session = worldEdit.getSessionManager().get(wePlayer);

            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(pos1.getWorld());
            BlockVector3 wePos1 = BlockVector3.at(pos1.getX(), pos1.getY(), pos1.getZ());
            BlockVector3 wePos2 = BlockVector3.at(pos2.getX(), pos2.getY(), pos2.getZ());

            session.setRegionSelector(weWorld,
                    new com.sk89q.worldedit.regions.selector.CuboidRegionSelector(weWorld, wePos1, wePos2));
            session.getRegionSelector(weWorld).learnChanges();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to set WorldEdit selection", e);
        }
    }

    public String getRegionDimensions(Location pos1, Location pos2) {
        int width = Math.abs(pos2.getBlockX() - pos1.getBlockX()) + 1;
        int height = Math.abs(pos2.getBlockY() - pos1.getBlockY()) + 1;
        int length = Math.abs(pos2.getBlockZ() - pos1.getBlockZ()) + 1;
        int volume = width * height * length;
        return width + "x" + height + "x" + length + " (" + volume + " blocks)";
    }
}
