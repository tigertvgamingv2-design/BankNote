package com.cdmstudios.bnote;

import com.cdmstudios.bnote.commands.AdminCommand;
import com.cdmstudios.bnote.commands.BalanceCommand;
import com.cdmstudios.bnote.gui.GuiManager;
import com.cdmstudios.bnote.hooks.BankNotesExpansion;
import com.cdmstudios.bnote.managers.TemplateManager;
import com.cdmstudios.bnote.commands.WithdrawCommand;
import com.cdmstudios.bnote.listeners.PlayerListener;
import com.cdmstudios.bnote.managers.PluginManager;
import com.cdmstudios.bnote.managers.RedeemedStore;
import com.cdmstudios.bnote.utils.LicenseGuard;
import com.cdmstudios.bnote.utils.Log;
import java.io.IOException;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class BankNotes extends JavaPlugin {

    private static final String LICENSE_API_URL = "https://lic.cloudrix.uk/api/verify";
    private static final String PRODUCT_SLUG = "banknotes";

    private static BankNotes instance;
    private Economy economy;
    private RedeemedStore redeemedStore;
    private TemplateManager templates;
    private GuiManager gui;

    public RedeemedStore getRedeemedStore() {
        return redeemedStore;
    }

    @Override
    public void onEnable() {
        instance = this;
        Log.banner(getDescription().getVersion());

        if (!setupEconomy()) {
            Log.error("Vault and an economy plugin are required! Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Log.success("Hooked into economy: &e" + economy.getName());
        saveDefaultConfig();
        // writes any newly added default options into an existing config.yml
        getConfig().options().copyDefaults(true);
        saveConfig();
        String licenseKey = getConfig().getString("license.key", "YOUR_LICENSE_KEY");
        if (licenseKey.isBlank() || licenseKey.startsWith("YOUR_")) {
            Log.error("Set license.key in config.yml before enabling this plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Log.info("Verifying license...");
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            String failure;
            try {
                failure = LicenseGuard.verify(LICENSE_API_URL, PRODUCT_SLUG, licenseKey);
            } catch (IOException e) {
                Log.warn("License verification request failed: " + e.getMessage());
                failure = e.getMessage();
            }
            String reason = failure;
            getServer().getScheduler().runTask(this, () -> {
                if (reason != null) {
                    Log.error("License verification failed: " + reason + "; disabling plugin.");
                    getServer().getPluginManager().disablePlugin(this);
                    return;
                }
                Log.success("License verified!");
                enableFeatures();
            });
        });
    }

    private void enableFeatures() {
        PluginManager.getInstance().initialize();
        try {
            redeemedStore = new RedeemedStore(getDataFolder());
        } catch (IOException e) {
            Log.error("Could not load redeemed notes: " + e.getMessage() + "; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Log.info("Loaded &e" + redeemedStore.size() + " &7redeemed note IDs");
        templates = new TemplateManager(getDataFolder());
        Log.info("Loaded &e" + templates.all().size() + " &7banknote template(s)");
        gui = new GuiManager(this);
        Log.info("Managers initialized");
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(gui, this);
        Log.info("Registered listeners");
        getCommand("withdraw").setExecutor(new WithdrawCommand());
        Log.info("Registered command &e/withdraw");
        getCommand("bnbalance").setExecutor(new BalanceCommand());
        Log.info("Registered command &e/bnbalance");
        hookIntegrations();
        AdminCommand admin = new AdminCommand();
        getCommand("bnadmin").setExecutor(admin);
        getCommand("bnadmin").setTabCompleter(admin);
        Log.info("Registered command &e/bnadmin");
        Log.success("&a&lBank Notes enabled and ready!");
    }

    private void hookIntegrations() {
        if (getServer().getPluginManager().getPlugin("Essentials") != null) {
            Log.success("EssentialsX detected - economy handled through Vault");
        }
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BankNotesExpansion(this).register();
            Log.success("PlaceholderAPI detected - registered &e%banknotes_*% &fplaceholders");
        }
    }

    public TemplateManager getTemplates() {
        return templates;
    }

    public GuiManager getGui() {
        return gui;
    }

    @Override
    public void onDisable() {
        Log.warn("Bank Notes has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public static BankNotes getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }
}

