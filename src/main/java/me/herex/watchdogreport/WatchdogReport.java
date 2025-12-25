package me.herex.watchdogreport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class WatchdogReport extends JavaPlugin {

    private static WatchdogReport instance;
    private DatabaseManager databaseManager;
    private ConfigManager configManager;
    private PunishmentManager punishmentManager;
    private ReportManager reportManager;
    private CommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("WatchdogReport starting...");

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        databaseManager = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            getLogger().severe("Failed to connect to database! Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        punishmentManager = new PunishmentManager(this);
        reportManager = new ReportManager(this);
        commandManager = new CommandManager(this);

        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getLogger().info("WatchdogReport enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("WatchdogReport disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return commandManager.handleCommand(sender, command, label, args);
    }

    public static WatchdogReport getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public ReportManager getReportManager() { return reportManager; }

    public void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}