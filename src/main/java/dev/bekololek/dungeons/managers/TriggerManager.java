package dev.bekololek.dungeons.managers;

import dev.bekololek.dungeons.Main;
import dev.bekololek.dungeons.models.CustomMob;
import dev.bekololek.dungeons.models.DungeonInstance;
import dev.bekololek.dungeons.models.QuestProgress;
import dev.bekololek.dungeons.models.Trigger;
import dev.bekololek.dungeons.utils.ClickType;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.logging.Level;

public class TriggerManager {

    private final Main plugin;

    public TriggerManager(Main plugin) {
        this.plugin = plugin;
    }

    public void checkLocationTriggers(DungeonInstance instance, Player player, Location playerLocation) {
        if (!instance.isActive()) return;

        for (Trigger trigger : instance.getDungeon().getTriggers()) {
            if (trigger.getType() != Trigger.TriggerType.LOCATION) continue;
            if (!instance.canTriggerFire(trigger)) continue;

            if (trigger.getCondition().isLocationInRange(playerLocation, instance.getSpawnLocation())) {
                executeTrigger(instance, trigger, player);
            }
        }
    }

    public void checkBlockInteractTriggers(DungeonInstance instance, Player player, Location interactLocation, ClickType clickType) {
        if (!instance.isActive()) return;

        for (Trigger trigger : instance.getDungeon().getTriggers()) {
            if (trigger.getType() != Trigger.TriggerType.BLOCK_INTERACT) continue;
            if (!instance.canTriggerFire(trigger)) continue;
            if(clickType != trigger.getCondition().getClickType()) continue;

            if (trigger.getCondition().isExactLocation(interactLocation, instance.getSpawnLocation())) {
                executeTrigger(instance, trigger, player);
            }
        }
    }

    public void checkTimerTriggers(DungeonInstance instance) {
        if (!instance.isActive()) return;

        long elapsedTime = instance.getElapsedTime();

        for (Trigger trigger : instance.getDungeon().getTriggers()) {
            if (trigger.getType() != Trigger.TriggerType.TIMER) continue;
            if (!instance.canTriggerFire(trigger)) continue;

            int requiredTime = trigger.getCondition().getTime();
            if (elapsedTime >= requiredTime) {
                executeTrigger(instance, trigger, null);
            }
        }
    }

    public void checkMobKillTriggers(DungeonInstance instance) {
        if (!instance.isActive()) return;

        int totalKills = instance.getTotalMobKills();

        for (Trigger trigger : instance.getDungeon().getTriggers()) {
            if (trigger.getType() != Trigger.TriggerType.MOB_KILL) continue;
            if (!instance.canTriggerFire(trigger)) continue;

            int requiredKills = trigger.getCondition().getMobCount();
            if (totalKills >= requiredKills) {
                executeTrigger(instance, trigger, null);
            }
        }
    }

    public void checkQuestCompleteTriggers(DungeonInstance instance, String questId) {
        if (!instance.isActive()) return;

        for (Trigger trigger : instance.getDungeon().getTriggers()) {
            if (trigger.getType() != Trigger.TriggerType.QUEST_COMPLETE) continue;
            if (!instance.canTriggerFire(trigger)) continue;

            String requiredQuestId = trigger.getCondition().getQuestId();
            if (requiredQuestId != null && requiredQuestId.equals(questId)) {
                QuestProgress progress = instance.getQuestProgress(questId);
                if (progress != null && progress.isCompleted()) {
                    executeTrigger(instance, trigger, null);
                }
            }
        }
    }

    private void executeTrigger(DungeonInstance instance, Trigger trigger, Player triggeringPlayer) {
        plugin.getLogger().info("Executing trigger '" + trigger.getId() + "' for instance " + instance.getInstanceId());

        instance.markTriggerFired(trigger);

        for (Trigger.TriggerAction action : trigger.getActions()) {
            try {
                executeAction(instance, action, triggeringPlayer);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error executing trigger action: " + action.getType(), e);
            }
        }
    }

    private void executeAction(DungeonInstance instance, Trigger.TriggerAction action, Player triggeringPlayer) {
        World world = instance.getSpawnLocation().getWorld();
        if (world == null) return;

        switch (action.getType()) {
            case SPAWN_MOB:
                spawnMob(instance, action, world);
                break;
            case SPAWN_MYTHIC_MOB:
                if(plugin.getMythicMobsIntegration().isEnabled()) {
                    spawnMythicMob(instance, action, world);
                } else {
                    System.out.println("Mythic Mobs integration has not been enabled.");
                }
                break;
            case DROP_ITEM:
                dropItem(instance, action, world);
                break;
            case DAMAGE_PLAYER:
                damagePlayer(instance, action, triggeringPlayer);
                break;
            case MESSAGE:
                sendMessage(instance, action);
                break;
            case COMMAND:
                executeCommand(action);
                break;
            case POTION_EFFECT:
                applyPotionEffect(instance, action, triggeringPlayer);
                break;
        }
    }

    private void spawnMythicMob(DungeonInstance instance, String customMobId, int count, int level, Location baseLocation) {
        String dungeonId = instance.getDungeon().getId();

        if (!plugin.getMythicMobsIntegration().mythicMobExists(customMobId)) {
            plugin.getLogger().warning("Mythic mob not found: " + customMobId + " for dungeon " + dungeonId);
            return;
        }

        for (int i = 0; i < count; i++) {
            Location mobLocation = baseLocation.clone().add(
                    (Math.random() - 0.5) * 2,
                    0,
                    (Math.random() - 0.5) * 2
            );

            Entity mythicMob = plugin.getMythicMobsIntegration().spawnAndGetMythicMob(customMobId,level,mobLocation);
            instance.addSpawnedMob(mythicMob.getUniqueId());
        }

        plugin.getLogger().info("Spawned " + count + " mythic mob '" + customMobId + "' at " + baseLocation);
    }

    private void spawnMob(DungeonInstance instance, Trigger.TriggerAction action, World world) {
        int count = action.getMobCount();

        Location dungeonOrigin = instance.getSpawnLocation();
        double spawnX = dungeonOrigin.getX() + action.getSpawnX();
        double spawnY = dungeonOrigin.getY() + action.getSpawnY();
        double spawnZ = dungeonOrigin.getZ() + action.getSpawnZ();

        Location spawnLocation = new Location(world, spawnX, spawnY, spawnZ);

        String customMobId = action.getCustomMobId();
        if (customMobId != null) {
            spawnCustomMob(instance, customMobId, count, spawnLocation);
            return;
        }

        EntityType mobType = action.getMobType();
        if (mobType == null) {
            plugin.getLogger().warning("No mob type specified for spawn action");
            return;
        }

        for (int i = 0; i < count; i++) {
            Location mobLocation = spawnLocation.clone().add(
                    (Math.random() - 0.5) * 2,
                    0,
                    (Math.random() - 0.5) * 2
            );

            LivingEntity entity = (LivingEntity) world.spawnEntity(mobLocation, mobType);
            instance.addSpawnedMob(entity.getUniqueId());
        }

        plugin.getLogger().info("Spawned " + count + " " + mobType + " at " + spawnLocation);
    }

    private void spawnMythicMob(DungeonInstance instance, Trigger.TriggerAction action, World world) {
        int count = action.getMobCount();

        Location dungeonOrigin = instance.getSpawnLocation();
        double spawnX = dungeonOrigin.getX() + action.getSpawnX();
        double spawnY = dungeonOrigin.getY() + action.getSpawnY();
        double spawnZ = dungeonOrigin.getZ() + action.getSpawnZ();

        Location spawnLocation = new Location(world, spawnX, spawnY, spawnZ);

        String customMobId = action.getCustomMobId();
        if (customMobId != null) {
            spawnMythicMob(instance, customMobId, count, 0, spawnLocation);
            return;
        }
    }

    private void spawnCustomMob(DungeonInstance instance, String customMobId, int count, Location baseLocation) {
        String dungeonId = instance.getDungeon().getId();
        CustomMob customMob = plugin.getCustomMobManager().getCustomMob(dungeonId, customMobId);

        if (customMob == null) {
            plugin.getLogger().warning("Custom mob not found: " + customMobId + " for dungeon " + dungeonId);
            return;
        }

        for (int i = 0; i < count; i++) {
            Location mobLocation = baseLocation.clone().add(
                    (Math.random() - 0.5) * 2,
                    0,
                    (Math.random() - 0.5) * 2
            );

            LivingEntity entity = (LivingEntity) baseLocation.getWorld().spawnEntity(mobLocation, customMob.getEntityType());
            customMob.applyToEntity(entity);
            instance.addSpawnedMob(entity.getUniqueId());
        }

        plugin.getLogger().info("Spawned " + count + " custom mob '" + customMobId + "' at " + baseLocation);
    }


    private void dropItem(DungeonInstance instance, Trigger.TriggerAction action, World world) {
        String materialName = action.getItemMaterial();
        int amount = action.getItemAmount();

        try {
            Material material = Material.valueOf(materialName.toUpperCase());

            Location dungeonOrigin = instance.getSpawnLocation();
            double dropX = dungeonOrigin.getX() + action.getSpawnX();
            double dropY = dungeonOrigin.getY() + action.getSpawnY();
            double dropZ = dungeonOrigin.getZ() + action.getSpawnZ();

            Location dropLocation = new Location(world, dropX, dropY, dropZ);

            ItemStack itemStack = new ItemStack(material, amount);
            world.dropItemNaturally(dropLocation, itemStack);

            plugin.getLogger().info("Dropped " + amount + " " + material + " at " + dropLocation);

        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material for drop item action: " + materialName);
        }
    }

    private void damagePlayer(DungeonInstance instance, Trigger.TriggerAction action, Player triggeringPlayer) {
        int damage = (int) action.getDamage();

        if (triggeringPlayer != null) {
            triggeringPlayer.damage(damage);
        } else {
            for (Player player : instance.getOnlinePlayers()) {
                player.damage(damage);
            }
        }

        plugin.getLogger().info("Applied " + damage + " damage to players in instance " + instance.getInstanceId());
    }

    private void sendMessage(DungeonInstance instance, Trigger.TriggerAction action) {
        String message = action.getMessage();
        if (message == null) return;

        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

        for (Player player : instance.getOnlinePlayers()) {
            player.sendMessage(coloredMessage);
        }

        plugin.getLogger().info("Sent trigger message to instance " + instance.getInstanceId());
    }

    private void executeCommand(Trigger.TriggerAction action) {
        String command = action.getCommand();
        if (command == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                plugin.getLogger().info("Executed trigger command: " + command);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error executing trigger command: " + command, e);
            }
        });
    }

    private void applyPotionEffect(DungeonInstance instance, Trigger.TriggerAction action, Player triggeringPlayer) {
        if (action.getPotionType() == null) {
            plugin.getLogger().warning("No potion effect type specified");
            return;
        }

        PotionEffect effect = new PotionEffect(
                action.getPotionType(),
                action.getPotionDuration() * 20,
                action.getPotionAmplifier()
        );

        if (triggeringPlayer != null) {
            triggeringPlayer.addPotionEffect(effect);
        } else {
            for (Player player : instance.getOnlinePlayers()) {
                player.addPotionEffect(effect);
            }
        }

        plugin.getLogger().info("Applied potion effect " + action.getPotionType().getKey().getKey() +
                " to players in instance " + instance.getInstanceId());
    }
}
