package dev.bekololek.dungeons.integrations;

import dev.bekololek.dungeons.Main;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.api.skills.Skill;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.logging.Level;

public class MythicMobsIntegration {

    private final Main plugin;
    private boolean enabled;

    public MythicMobsIntegration(Main plugin) {
        this.plugin=plugin;
        this.enabled = false;
    }

    public void initialize() {
        try {
            if (plugin.getServer().getPluginManager().getPlugin("MythicMobs") == null) {
                plugin.getLogger().warning("MythicMobs not found - MythicMob functionality will be disabled");
                enabled = false;
                return;
            }
            plugin.getLogger().info("MythicMobs integration enabled");
            enabled = true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize MythicMobs integration", e);
            enabled = false;
        }
    }

    public boolean isEnabled() { return enabled; }

    public void spawnMythicMob(String mobName, int level, Location spawnLocation) {
        getMythicMob(mobName).spawn(BukkitAdapter.adapt(spawnLocation),level);
    }

    public Entity spawnAndGetMythicMob(String mobName, int level, Location spawnLocation) {
        ActiveMob mob = getMythicMob(mobName).spawn(BukkitAdapter.adapt(spawnLocation),level);
        return mob.getEntity().getBukkitEntity();
    }

    public void runMythicSkill(String skillName, Location originLocation, Location targetLocation) {
        // TODO: Set up method
    }

    public boolean mythicMobExists(String mobName) {
        return getMythicMob(mobName) != null;
    }

    public MythicMob getMythicMob(String mobName) {
        return MythicBukkit.inst().getMobManager().getMythicMob(mobName).orElse(null);
    }

    public Skill getMythicSkill(String skillName) {
        return MythicBukkit.inst().getSkillManager().getSkill(skillName).orElse(null);
    }

}
