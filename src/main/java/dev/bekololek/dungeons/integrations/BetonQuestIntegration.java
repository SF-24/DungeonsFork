package dev.bekololek.dungeons.integrations;

import dev.bekololek.dungeons.Main;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.id.action.ActionIdentifierFactory;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class BetonQuestIntegration {

    private final Main plugin;
    private boolean enabled;

    public BetonQuestIntegration(Main plugin) {
        this.plugin=plugin;
        this.enabled = false;
    }

    public void initialize() {
        try {
            if (plugin.getServer().getPluginManager().getPlugin("BetonQuest") == null) {
                plugin.getLogger().warning("BetonQuest not found - BetonQuest functionality will be disabled");
                enabled = false;
                return;
            }
            plugin.getLogger().info("BetonQuest integration enabled");
            enabled = true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize BetonQuest integration", e);
            enabled = false;
        }
    }

    public boolean isEnabled() { return enabled; }

    public static void runBetonQuestEvent(Player targetPlayer, QuestPackage questPackage, String event) {
        try {
            final OnlineProfile playerProfile = BetonQuest.getInstance().getProfileProvider().getProfile(targetPlayer);
            BetonQuest.getInstance().getBetonQuestApi().actions().manager().run(playerProfile, new ActionIdentifierFactory(BetonQuest.getInstance().getQuestPackageManager()).parseIdentifier(questPackage,event));
        } catch (QuestException e) {
            System.out.println("Error running beton event: " + questPackage + ">" + event);
        }
    }


}
