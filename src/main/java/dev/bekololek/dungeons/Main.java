package dev.bekololek.dungeons;

import dev.bekololek.dungeons.commands.DungeonAdminCommand;
import dev.bekololek.dungeons.commands.DungeonCommand;
import dev.bekololek.dungeons.commands.DungeonEditorCommand;
import dev.bekololek.dungeons.commands.PartyCommand;
import dev.bekololek.dungeons.database.DatabaseManager;
import dev.bekololek.dungeons.integrations.VaultEconomyIntegration;
import dev.bekololek.dungeons.integrations.WorldEditIntegration;
import dev.bekololek.dungeons.integrations.WorldGuardIntegration;
import dev.bekololek.dungeons.listeners.DungeonListener;
import dev.bekololek.dungeons.managers.*;
import dev.bekololek.dungeons.stats.DungeonsExpansion;
import dev.bekololek.dungeons.utils.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class Main extends JavaPlugin {

    private DatabaseManager databaseManager;
    private GridManager gridManager;
    private DungeonManager dungeonManager;
    private QuestManager questManager;
    private RewardManager rewardManager;
    private PartyManager partyManager;
    private InstanceManager instanceManager;
    private TriggerManager triggerManager;
    private CustomMobManager customMobManager;
    private StatsManager statsManager;

    private VaultEconomyIntegration vaultEconomy;
    private WorldEditIntegration worldEditIntegration;
    private WorldGuardIntegration worldGuardIntegration;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        MessageUtil.initialize(this);

        // Database
        databaseManager = new DatabaseManager(this);
        try {
            databaseManager.initialize();
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Integrations
        vaultEconomy = new VaultEconomyIntegration(this);
        if (getConfig().getBoolean("economy.enabled", true)) {
            vaultEconomy.initialize();
        }

        worldEditIntegration = new WorldEditIntegration(this);
        worldEditIntegration.initialize();

        worldGuardIntegration = new WorldGuardIntegration(this);
        worldGuardIntegration.initialize();

        // Managers
        customMobManager = new CustomMobManager(this);
        gridManager = new GridManager(this);
        gridManager.initializeWorld();

        dungeonManager = new DungeonManager(this);
        questManager = new QuestManager(this);
        rewardManager = new RewardManager(this);
        triggerManager = new TriggerManager(this);

        questManager.loadQuests();
        rewardManager.loadRewards();
        dungeonManager.loadDungeons();

        partyManager = new PartyManager(this);
        instanceManager = new InstanceManager(this);

        statsManager = new StatsManager(this);
        statsManager.load();
        statsManager.startAutoSave();

        // Listeners
        getServer().getPluginManager().registerEvents(new DungeonListener(this), this);

        // Commands
        DungeonCommand dungeonCmd = new DungeonCommand(this);
        getCommand("dungeon").setExecutor(dungeonCmd);
        getCommand("dungeon").setTabCompleter(dungeonCmd);

        PartyCommand partyCmd = new PartyCommand(this);
        getCommand("dngparty").setExecutor(partyCmd);
        getCommand("dngparty").setTabCompleter(partyCmd);

        DungeonEditorCommand editorCmd = new DungeonEditorCommand(this);
        getCommand("dng").setExecutor(editorCmd);
        getCommand("dng").setTabCompleter(editorCmd);

        DungeonAdminCommand adminCmd = new DungeonAdminCommand(this);
        getCommand("dngadmin").setExecutor(adminCmd);
        getCommand("dngadmin").setTabCompleter(adminCmd);

        // PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DungeonsExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        }

        getLogger().info("Dungeons.v1 - BL enabled.");
    }

    @Override
    public void onDisable() {
        if (instanceManager != null) {
            instanceManager.cleanupAllInstances();
        }

        if (partyManager != null) {
            partyManager.saveAllParties();
        }

        if (statsManager != null) {
            statsManager.saveSync();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        getLogger().info("Dungeons.v1 - BL disabled.");
    }

    public void reload() {
        reloadConfig();
        MessageUtil.loadMessages();
        questManager.loadQuests();
        rewardManager.loadRewards();
        customMobManager.clear();
        dungeonManager.loadDungeons();
    }

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public GridManager getGridManager() { return gridManager; }
    public DungeonManager getDungeonManager() { return dungeonManager; }
    public QuestManager getQuestManager() { return questManager; }
    public RewardManager getRewardManager() { return rewardManager; }
    public PartyManager getPartyManager() { return partyManager; }
    public InstanceManager getInstanceManager() { return instanceManager; }
    public TriggerManager getTriggerManager() { return triggerManager; }
    public CustomMobManager getCustomMobManager() { return customMobManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public VaultEconomyIntegration getVaultEconomy() { return vaultEconomy; }
    public WorldEditIntegration getWorldEditIntegration() { return worldEditIntegration; }
    public WorldGuardIntegration getWorldGuardIntegration() { return worldGuardIntegration; }
}
