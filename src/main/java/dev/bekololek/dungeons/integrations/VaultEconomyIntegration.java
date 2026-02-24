package dev.bekololek.dungeons.integrations;

import dev.bekololek.dungeons.Main;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomyIntegration {

    private final Main plugin;
    private Economy economy;
    private boolean enabled;

    public VaultEconomyIntegration(Main plugin) {
        this.plugin = plugin;
        this.enabled = false;
    }

    public boolean initialize() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("Vault not found. Economy features disabled.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Vault found but no economy plugin detected. Economy features disabled.");
            return false;
        }

        economy = rsp.getProvider();
        enabled = true;
        plugin.getLogger().info("Vault economy integration enabled.");
        return true;
    }

    public boolean isEnabled() { return enabled; }

    public boolean has(Player player, double amount) {
        if (!enabled || economy == null) return true;
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!enabled || economy == null) return true;
        if (!economy.has(player, amount)) return false;
        economy.withdrawPlayer(player, amount);
        return true;
    }

    public void deposit(Player player, double amount) {
        if (!enabled || economy == null) return;
        economy.depositPlayer(player, amount);
    }

    public double getBalance(Player player) {
        if (!enabled || economy == null) return 0.0;
        return economy.getBalance(player);
    }

    public String format(double amount) {
        if (!enabled || economy == null) return String.format("$%.2f", amount);
        return economy.format(amount);
    }
}
