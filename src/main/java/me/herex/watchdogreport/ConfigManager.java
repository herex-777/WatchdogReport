package me.herex.watchdogreport;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;

public class ConfigManager {

    private final WatchdogReport plugin;
    private FileConfiguration config;

    public ConfigManager(WatchdogReport plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        config = plugin.getConfig();
    }

    // Database
    public String getDatabaseType() { return config.getString("database.type", "sqlite"); }
    public String getMySQLHost() { return config.getString("database.mysql.host", "localhost"); }
    public int getMySQLPort() { return config.getInt("database.mysql.port", 3306); }
    public String getMySQLDatabase() { return config.getString("database.mysql.database", "watchdog_db"); }
    public String getMySQLUsername() { return config.getString("database.mysql.username", "root"); }
    public String getMySQLPassword() { return config.getString("database.mysql.password", ""); }
    public boolean getMySQLUseSSL() { return config.getBoolean("database.mysql.useSSL", false); }
    public boolean getMySQLAllowPublicKey() { return config.getBoolean("database.mysql.allowPublicKeyRetrieval", true); }
    public String getMySQLTimezone() { return config.getString("database.mysql.serverTimezone", "UTC"); }
    public String getSQLiteFilename() { return config.getString("database.sqlite.filename", "watchdog.db"); }

    // Server
    public String getServerName() { return config.getString("server.name", "YourServer"); }
    public List<String> getEnabledWorlds() { return config.getStringList("server.enabled-worlds"); }
    public String getAppealLink() { return config.getString("server.appeal-link", "https://www.farepixel.fun/appeal"); }
    public String getReportLink() { return config.getString("server.report-link", "https://farepixel.net/report"); }

    // Announcements
    public boolean getAnnouncementsEnabled() { return config.getBoolean("announcements.enabled", true); }
    public long getAnnouncementInterval() { return config.getLong("announcements.interval-ticks", 36000L); }
    public List<String> getAnnouncementMessages() { return config.getStringList("announcements.messages"); }

    // Reports
    public int getCooldownSeconds() { return config.getInt("reports.cooldown-seconds", 60); }
    public boolean getNotifyStaff() { return config.getBoolean("reports.notify-staff", true); }
    public String getStaffPermission() { return config.getString("reports.staff-permission", "watchdog.staff"); }
    public boolean getRequireWebsiteReport() { return config.getBoolean("reports.require-website-report", true); }
    public List<String> getValidReasons() { return config.getStringList("reports.valid-reasons"); }

    // World check
    public boolean isWorldEnabled(String worldName) {
        List<String> worlds = getEnabledWorlds();
        return worlds.isEmpty() || worlds.contains(worldName);
    }
}