package dev.bekololek.dungeons.listeners;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.models.DungeonInstance;
import dev.bekololek.dungeons.models.Quest;
import dev.bekololek.dungeons.models.QuestObjective;
import dev.bekololek.dungeons.models.QuestProgress;
import dev.bekololek.dungeons.utils.ClickType;
import dev.bekololek.dungeons.utils.MessageUtil;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class DungeonListener implements Listener {

    private final Main plugin;

    public DungeonListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getStatsManager().updateName(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (plugin.getInstanceManager().isPlayerInDungeon(playerId)) {
            plugin.getInstanceManager().handlePlayerLeave(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        UUID killerId = killer.getUniqueId();
        DungeonInstance instance = plugin.getInstanceManager().getPlayerInstance(killerId);
        if (instance == null || !instance.isActive()) return;

        // Track mob kill
        instance.incrementMobKills();
        plugin.getStatsManager().recordMobKill(killer);

        // Check if it was a spawned dungeon mob
        UUID entityId = entity.getUniqueId();
        if (instance.getSpawnedMobs().contains(entityId)) {
            instance.removeSpawnedMob(entityId);
        }

        // Update quest progress for KILL_MOBS quests
        for (QuestProgress progress : instance.getAllQuestProgress()) {
            if (progress.isCompleted()) continue;
            Quest quest = progress.getQuest();
            if (quest.getType() != Quest.QuestType.KILL_MOBS && quest.getType() != Quest.QuestType.KILL_BOSS) continue;

            for (int i = 0; i < quest.getObjectives().size(); i++) {
                QuestObjective obj = quest.getObjectives().get(i);
                if (progress.isObjectiveComplete(i)) continue;

                if (quest.getType() == Quest.QuestType.KILL_MOBS) {
                    if (obj.getMobType() == null || obj.getMobType() == entity.getType()) {
                        progress.incrementProgress(i, 1);
                    }
                }
            }

            if (progress.isCompleted()) {
                plugin.getStatsManager().recordQuestComplete(killer);
                for (Player p : instance.getOnlinePlayers()) {
                    MessageUtil.sendMessage(p, "quest.completed",
                            MessageUtil.replacement("quest", quest.getName()));
                }
                plugin.getTriggerManager().checkQuestCompleteTriggers(instance, quest.getId());
            }
        }

        // Check mob kill triggers
        plugin.getTriggerManager().checkMobKillTriggers(instance);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        DungeonInstance instance = plugin.getInstanceManager().getPlayerInstance(playerId);
        if (instance == null || !instance.isActive()) return;

        instance.recordDeath(playerId);
        plugin.getInstanceManager().handlePlayerDeath(player);

        boolean keepInventory = plugin.getConfig().getBoolean("instance.keep-inventory", false);
        if (keepInventory) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }

        int maxDeaths = plugin.getConfig().getInt("instance.max-deaths", 0);
        if (maxDeaths > 0 && instance.getDeathCount(playerId) >= maxDeaths) {
            MessageUtil.sendRaw(player, "&cYou have been removed from the dungeon for dying too many times!");
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getInstanceManager().handlePlayerLeave(player);
            }, 5L);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        DungeonInstance instance = plugin.getInstanceManager().getPlayerInstance(playerId);
        if (instance == null) return;

        boolean respawnInDungeon = plugin.getConfig().getBoolean("instance.respawn-in-dungeon", true);
        if (respawnInDungeon && instance.isActive()) {
            event.setRespawnLocation(instance.getSpawnLocation().clone().add(
                    instance.getDungeon().getSpawnOffsetX(),
                    instance.getDungeon().getSpawnOffsetY(),
                    instance.getDungeon().getSpawnOffsetZ()
            ));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        DungeonInstance instance = plugin.getInstanceManager().getPlayerInstance(playerId);
        if (instance == null || !instance.isActive()) return;

        // Check location triggers
        plugin.getTriggerManager().checkLocationTriggers(instance, player, event.getTo());

        // Check REACH_LOCATION quests
        for (QuestProgress progress : instance.getAllQuestProgress()) {
            if (progress.isCompleted()) continue;
            Quest quest = progress.getQuest();
            if (quest.getType() != Quest.QuestType.REACH_LOCATION) continue;

            for (int i = 0; i < quest.getObjectives().size(); i++) {
                QuestObjective obj = quest.getObjectives().get(i);
                if (progress.isObjectiveComplete(i)) continue;

                double dx = event.getTo().getX() - (instance.getSpawnLocation().getX() + obj.getX());
                double dy = event.getTo().getY() - (instance.getSpawnLocation().getY() + obj.getY());
                double dz = event.getTo().getZ() - (instance.getSpawnLocation().getZ() + obj.getZ());
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                int radius = obj.getRadius() > 0 ? obj.getRadius() : 3;
                if (distance <= radius) {
                    progress.setProgress(i, obj.getAmount());
                }
            }

            if (progress.isCompleted()) {
                plugin.getStatsManager().recordQuestComplete(player);
                for (Player p : instance.getOnlinePlayers()) {
                    MessageUtil.sendMessage(p, "quest.completed",
                            MessageUtil.replacement("quest", quest.getName()));
                }
                plugin.getTriggerManager().checkQuestCompleteTriggers(instance, quest.getId());
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) return;

        UUID playerId = player.getUniqueId();
        DungeonInstance instance = plugin.getInstanceManager().getPlayerInstance(playerId);
        if (instance == null || !instance.isActive()) return;

        // Check BLOCK_INTERACT trigger, parsing the click type.
        if(event.getAction().isRightClick()) {
            plugin.getTriggerManager().checkBlockInteractTriggers(instance, player, event.getClickedBlock().getLocation(),ClickType.RIGHT_CLICK);
        } else if(event.getAction().isLeftClick()) {
            plugin.getTriggerManager().checkBlockInteractTriggers(instance, player, event.getClickedBlock().getLocation(),ClickType.LEFT_CLICK);
        }

        // Check INTERACT_BLOCKS quests
        for (QuestProgress progress : instance.getAllQuestProgress()) {
            if (progress.isCompleted()) continue;
            Quest quest = progress.getQuest();
            if (quest.getType() != Quest.QuestType.INTERACT_BLOCKS) continue;

            for (int i = 0; i < quest.getObjectives().size(); i++) {
                QuestObjective obj = quest.getObjectives().get(i);
                if (progress.isObjectiveComplete(i)) continue;

                if (obj.getMaterial() != null && obj.getMaterial() == event.getClickedBlock().getType()) {
                    progress.incrementProgress(i, 1);
                }
            }

            if (progress.isCompleted()) {
                plugin.getStatsManager().recordQuestComplete(player);
                for (Player p : instance.getOnlinePlayers()) {
                    MessageUtil.sendMessage(p, "quest.completed",
                            MessageUtil.replacement("quest", quest.getName()));
                }
                plugin.getTriggerManager().checkQuestCompleteTriggers(instance, quest.getId());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null) return;

        // Check friendly fire
        DungeonInstance victimInstance = plugin.getInstanceManager().getPlayerInstance(victim.getUniqueId());
        DungeonInstance attackerInstance = plugin.getInstanceManager().getPlayerInstance(attacker.getUniqueId());

        if (victimInstance != null && attackerInstance != null
                && victimInstance.getInstanceId().equals(attackerInstance.getInstanceId())) {
            boolean friendlyFire = plugin.getConfig().getBoolean("party.friendly-fire", false);
            if (!friendlyFire) {
                event.setCancelled(true);
            }
        }
    }
}
