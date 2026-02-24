package dev.bekololek.dungeons.managers;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.models.*;
import dev.bekololek.dungeons.utils.MessageUtil;
import dev.bekololek.dungeons.utils.SchematicLoader;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InstanceManager {

    private final Main plugin;
    private final SchematicLoader schematicLoader;

    private final Map<UUID, DungeonInstance> activeInstances;
    private final Map<UUID, UUID> playerInstanceMap;
    private final Map<UUID, UUID> partyInstanceMap;

    public InstanceManager(Main plugin) {
        this.plugin = plugin;
        this.schematicLoader = new SchematicLoader(plugin);
        this.activeInstances = new ConcurrentHashMap<>();
        this.playerInstanceMap = new ConcurrentHashMap<>();
        this.partyInstanceMap = new ConcurrentHashMap<>();

        startInstanceMonitor();
    }

    private void killAllMobs(DungeonInstance instance) {
        Location spawnLocation = instance.getSpawnLocation();
        int slotSize = plugin.getGridManager().getSlotSize();

        org.bukkit.World world = spawnLocation.getWorld();
        if (world == null) return;

        double minX = spawnLocation.getX();
        double minY = spawnLocation.getY();
        double minZ = spawnLocation.getZ();
        double maxX = minX + slotSize;
        double maxY = minY + 100;
        double maxZ = minZ + slotSize;

        int killedCount = 0;
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            Location loc = entity.getLocation();

            if (loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ) {

                if (entity instanceof org.bukkit.entity.Monster ||
                    entity instanceof org.bukkit.entity.Animals ||
                    entity instanceof org.bukkit.entity.Flying ||
                    entity instanceof org.bukkit.entity.Slime) {
                    entity.remove();
                    killedCount++;
                }
            }
        }

        if (killedCount > 0) {
            plugin.getLogger().info("Killed " + killedCount + " mobs in instance " + instance.getInstanceId());
        }
    }

    private void teleportToReturnPoint(Player player) {
        boolean useMultiverse = plugin.getConfig().getBoolean("instance.return-point.use-multiverse", false);
        String world = plugin.getConfig().getString("instance.return-point.world", "world");
        double x = plugin.getConfig().getDouble("instance.return-point.x", 0.5);
        double y = plugin.getConfig().getDouble("instance.return-point.y", 64.0);
        double z = plugin.getConfig().getDouble("instance.return-point.z", 0.5);

        if (useMultiverse) {
            String command = String.format("mv tp %s %s %s %s %s",
                    player.getName(), world, x, y, z);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            plugin.getLogger().fine("Teleporting " + player.getName() + " to return point via Multiverse: " + world + " " + x + " " + y + " " + z);
        } else {
            org.bukkit.World bukkitWorld = Bukkit.getWorld(world);
            if (bukkitWorld != null) {
                Location returnPoint = new Location(bukkitWorld, x, y, z);
                player.teleport(returnPoint);
                plugin.getLogger().fine("Teleporting " + player.getName() + " to return point via Bukkit: " + world + " " + x + " " + y + " " + z);
            } else {
                plugin.getLogger().warning("Return point world '" + world + "' not found, teleporting to default spawn");
                Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation();
                player.teleport(spawn);
            }
        }
    }

    public DungeonInstance createInstance(Dungeon dungeon, Party party) {
        if (partyInstanceMap.containsKey(party.getPartyId())) {
            return null;
        }

        GridSlot slot = plugin.getGridManager().allocateSlot(party.getPartyId());
        if (slot == null) {
            plugin.getLogger().warning("No available grid slots for party " + party.getPartyId());
            return null;
        }

        Location spawnLocation = plugin.getGridManager().getSlotCenterLocation(slot);
        if (spawnLocation == null) {
            plugin.getGridManager().releaseSlot(slot);
            return null;
        }

        UUID instanceId = UUID.randomUUID();
        DungeonInstance instance = new DungeonInstance(instanceId, dungeon, party, slot, spawnLocation);

        List<Quest> quests = plugin.getQuestManager().getQuests(dungeon.getQuestIds());
        instance.initializeQuests(quests);

        activeInstances.put(instanceId, instance);
        partyInstanceMap.put(party.getPartyId(), instanceId);

        for (UUID memberId : party.getMembers()) {
            playerInstanceMap.put(memberId, instanceId);
        }

        plugin.getLogger().info("Created dungeon instance: " + instance);

        Bukkit.getScheduler().runTaskLater(plugin, () -> startInstance(instance), 10L);

        return instance;
    }

    private void startInstance(DungeonInstance instance) {
        Dungeon dungeon = instance.getDungeon();

        plugin.getLogger().info("Pasting schematic for instance " + instance.getInstanceId());

        boolean success = schematicLoader.pasteSchematic(dungeon.getSchematicFile(), instance.getSpawnLocation());

        if (!success) {
            plugin.getLogger().warning("Schematic paste failed for dungeon " + dungeon.getId() + ", using fallback");
        }

        if (plugin.getWorldGuardIntegration() != null && plugin.getWorldGuardIntegration().isEnabled()) {
            Location spawnLoc = instance.getSpawnLocation();
            int slotSize = plugin.getGridManager().getSlotSize();

            Location corner1 = spawnLoc.clone();
            Location corner2 = spawnLoc.clone().add(slotSize, 100, slotSize);

            plugin.getWorldGuardIntegration().createRegion(instance.getInstanceId(), corner1, corner2);
        }

        instance.setState(DungeonInstance.InstanceState.ACTIVE);

        Location spawnLoc = instance.getSpawnLocation().clone().add(
                dungeon.getSpawnOffsetX(),
                dungeon.getSpawnOffsetY(),
                dungeon.getSpawnOffsetZ()
        );

        for (Player player : instance.getOnlinePlayers()) {
            player.teleport(spawnLoc);
            MessageUtil.sendMessage(player, "dungeon.entered",
                    MessageUtil.replacement("dungeon", dungeon.getDisplayName()));
        }

        plugin.getLogger().info("Instance " + instance.getInstanceId() + " is now active");
    }

    public void completeInstance(DungeonInstance instance) {
        if (instance.isFinished()) return;

        instance.setCompleted(true);

        plugin.getLogger().info("Instance " + instance.getInstanceId() + " completed");

        for (Player player : instance.getOnlinePlayers()) {
            MessageUtil.sendMessage(player, "dungeon.completed");
        }

        distributeRewards(instance);
        setCooldowns(instance);
        updateStatistics(instance);

        boolean postCompletionEnabled = plugin.getConfig().getBoolean("instance.post-completion.enabled", false);
        int postCompletionDuration = plugin.getConfig().getInt("instance.post-completion.duration", 30);
        boolean killMobs = plugin.getConfig().getBoolean("instance.post-completion.kill-mobs", true);

        if (!postCompletionEnabled) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupInstance(instance), 100L);
            return;
        }

        instance.setState(DungeonInstance.InstanceState.POST_COMPLETION);

        if (killMobs) {
            killAllMobs(instance);
        }

        if (postCompletionDuration == -1) {
            for (Player player : instance.getOnlinePlayers()) {
                MessageUtil.sendRaw(player, "&aYou can explore the dungeon! Party leader can use &e/dungeon leave &ato exit.");
            }
            plugin.getLogger().info("Instance " + instance.getInstanceId() + " in post-completion state (unlimited)");
        } else if (postCompletionDuration > 0) {
            for (Player player : instance.getOnlinePlayers()) {
                MessageUtil.sendRaw(player, "&aYou have &e" + postCompletionDuration + " seconds &ato explore before cleanup!");
            }
            plugin.getLogger().info("Instance " + instance.getInstanceId() + " in post-completion state (" + postCompletionDuration + "s)");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (instance.getState() == DungeonInstance.InstanceState.POST_COMPLETION) {
                    cleanupInstance(instance);
                }
            }, postCompletionDuration * 20L);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupInstance(instance), 100L);
        }
    }

    public void failInstance(DungeonInstance instance, String reason) {
        if (instance.isFinished()) return;

        instance.setFailed(true);

        plugin.getLogger().info("Instance " + instance.getInstanceId() + " failed: " + reason);

        for (Player player : instance.getOnlinePlayers()) {
            MessageUtil.sendRaw(player, "&c" + reason);
            MessageUtil.sendMessage(player, "dungeon.failed");
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupInstance(instance), 100L);
    }

    private void cleanupInstance(DungeonInstance instance) {
        instance.setState(DungeonInstance.InstanceState.CLEANING_UP);

        plugin.getLogger().info("Cleaning up instance " + instance.getInstanceId());

        for (Player player : instance.getOnlinePlayers()) {
            teleportToReturnPoint(player);
            MessageUtil.sendMessage(player, "dungeon.left");
        }

        if (plugin.getWorldGuardIntegration() != null && plugin.getWorldGuardIntegration().isEnabled()) {
            plugin.getWorldGuardIntegration().deleteRegion(
                    instance.getInstanceId(),
                    instance.getSpawnLocation().getWorld()
            );
        }

        for (UUID memberId : instance.getParty().getMembers()) {
            playerInstanceMap.remove(memberId);
        }

        partyInstanceMap.remove(instance.getParty().getPartyId());
        activeInstances.remove(instance.getInstanceId());

        GridSlot slot = instance.getGridSlot();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            schematicLoader.clearArea(
                    instance.getSpawnLocation(),
                    plugin.getGridManager().getSlotSize(),
                    100,
                    plugin.getGridManager().getSlotSize()
            );

            plugin.getGridManager().releaseSlot(slot);

            plugin.getLogger().info("Instance " + instance.getInstanceId() + " cleaned up");
        });
    }

    private void distributeRewards(DungeonInstance instance) {
        List<String> rewardTables = instance.getDungeon().getRewardTables();

        for (Player player : instance.getOnlinePlayers()) {
            plugin.getRewardManager().giveRewards(player, rewardTables);

            for (QuestProgress progress : instance.getAllQuestProgress()) {
                if (!progress.getQuest().isRequired() && progress.isCompleted()) {
                    String bonusReward = progress.getQuest().getBonusReward();
                    if (bonusReward != null) {
                        plugin.getRewardManager().giveRewards(player, List.of(bonusReward));
                    }
                }
            }
        }
    }

    private void setCooldowns(DungeonInstance instance) {
        long cooldown = instance.getDungeon().getCooldown();
        String dungeonId = instance.getDungeon().getId();

        for (UUID playerId : instance.getParty().getMembers()) {
            PlayerData playerData = plugin.getDatabaseManager().loadPlayerData(playerId);
            playerData.setDungeonCooldown(dungeonId, cooldown);
            playerData.updateLastSeen();
            plugin.getDatabaseManager().savePlayerData(playerData);
        }
    }

    private void updateStatistics(DungeonInstance instance) {
        String dungeonId = instance.getDungeon().getId();

        long playtime = instance.getCompletionDuration() > 0
                ? instance.getCompletionDuration() * 1000
                : instance.getElapsedTime() * 1000;

        long completionDuration = instance.getCompletionDuration();

        for (UUID playerId : instance.getParty().getMembers()) {
            PlayerData playerData = plugin.getDatabaseManager().loadPlayerData(playerId);
            playerData.incrementCompletion(dungeonId);
            playerData.addPlaytime(playtime);
            playerData.updateLastSeen();
            plugin.getDatabaseManager().savePlayerData(playerData);

            if (completionDuration > 0) {
                plugin.getDatabaseManager().saveCompletionRecord(playerId, dungeonId, completionDuration);
            }

            // Record stats for StatsManager
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                plugin.getStatsManager().recordDungeonComplete(player, dungeonId, completionDuration);
            }
        }
    }

    public void handlePlayerLeave(Player player) {
        UUID playerId = player.getUniqueId();
        DungeonInstance instance = getPlayerInstance(playerId);

        if (instance == null) return;

        Party party = instance.getParty();

        if (party.isLeader(playerId) && instance.getState() == DungeonInstance.InstanceState.POST_COMPLETION) {
            plugin.getLogger().info("Party leader left during post-completion, cleaning up instance " + instance.getInstanceId());
            cleanupInstance(instance);
            return;
        }

        if (party.isLeader(playerId)) {
            plugin.getLogger().info("Party leader left dungeon, failing instance " + instance.getInstanceId());
            failInstance(instance, "Party leader left the dungeon");
            return;
        }

        playerInstanceMap.remove(playerId);
        teleportToReturnPoint(player);
        MessageUtil.sendMessage(player, "dungeon.left");

        for (Player p : instance.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(playerId)) {
                MessageUtil.sendRaw(p, "&e" + player.getName() + " has left the dungeon");
            }
        }

        boolean anyoneLeft = false;
        for (UUID memberId : party.getMembers()) {
            if (!memberId.equals(playerId) && playerInstanceMap.containsKey(memberId)) {
                anyoneLeft = true;
                break;
            }
        }

        if (!anyoneLeft) {
            if (instance.getState() == DungeonInstance.InstanceState.POST_COMPLETION) {
                cleanupInstance(instance);
            } else {
                failInstance(instance, "All players left the dungeon");
            }
        }
    }

    public void handlePlayerDeath(Player player) {
        plugin.getStatsManager().recordDeath(player);
    }

    public DungeonInstance getInstance(UUID instanceId) { return activeInstances.get(instanceId); }

    public DungeonInstance getPlayerInstance(UUID playerId) {
        UUID instanceId = playerInstanceMap.get(playerId);
        return instanceId != null ? activeInstances.get(instanceId) : null;
    }

    public DungeonInstance getPartyInstance(UUID partyId) {
        UUID instanceId = partyInstanceMap.get(partyId);
        return instanceId != null ? activeInstances.get(instanceId) : null;
    }

    public boolean isPlayerInDungeon(UUID playerId) { return playerInstanceMap.containsKey(playerId); }

    public Collection<DungeonInstance> getActiveInstances() { return new ArrayList<>(activeInstances.values()); }

    private void startInstanceMonitor() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (DungeonInstance instance : activeInstances.values()) {
                if (instance.isActive() && instance.isExpired()) {
                    failInstance(instance, "Time limit exceeded");
                    continue;
                }

                if (instance.isActive() && instance.areRequiredQuestsComplete()) {
                    completeInstance(instance);
                }
            }
        }, 20L, 20L);
    }

    public void cleanupAllInstances() {
        plugin.getLogger().info("Cleaning up " + activeInstances.size() + " active instances");

        for (DungeonInstance instance : new ArrayList<>(activeInstances.values())) {
            cleanupInstance(instance);
        }
    }

    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("active_instances", activeInstances.size());
        stats.put("players_in_dungeons", playerInstanceMap.size());
        return stats;
    }

    public SchematicLoader getSchematicLoader() { return schematicLoader; }
}
