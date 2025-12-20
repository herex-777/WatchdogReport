package me.herex.watchdogreport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class PunishmentManager {

    public enum PunishmentType {
        BAN, TEMPBAN, MUTE, TEMPMUTE, WARN, KICK
    }

    private final WatchdogReport plugin;
    private final DatabaseManager dbManager;

    public PunishmentManager(WatchdogReport plugin) {
        this.plugin = plugin;
        this.dbManager = plugin.getDatabaseManager();
    }

    // Get ban kick message for login events
    public String getBanKickMessage(String playerName) {
        Connection conn = dbManager.getConnection();
        if (conn == null) {
            return getDefaultBanMessage("Database error");
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String query = "SELECT * FROM punishments WHERE player_name = ? " +
                    "AND punishment_type IN ('BAN', 'TEMPBAN') " +
                    "AND active = 1 ORDER BY start_time DESC LIMIT 1";

            stmt = conn.prepareStatement(query);
            stmt.setString(1, playerName);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String type = rs.getString("punishment_type");
                String reason = rs.getString("reason");
                long endTime = rs.getLong("end_time");

                if (type.equals("BAN")) {
                    return getDefaultBanMessage(reason);
                } else if (type.equals("TEMPBAN")) {
                    long currentTime = System.currentTimeMillis();
                    if (endTime > currentTime) {
                        long remaining = endTime - currentTime;
                        String duration = formatDuration(remaining);
                        return getTempBanMessage(reason, duration);
                    } else {
                        // Temp ban expired - remove it
                        removeExpiredTempBan(playerName);
                        return null;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get ban info", e);
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
        }
        return null;
    }

    // Remove expired temporary bans
    private void removeExpiredTempBan(String playerName) {
        Connection conn = dbManager.getConnection();
        if (conn == null) return;

        PreparedStatement stmt = null;
        try {
            String query = "UPDATE punishments SET active = 0 WHERE player_name = ? " +
                    "AND punishment_type = 'TEMPBAN' AND active = 1 AND end_time <= ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, playerName);
            stmt.setLong(2, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove expired temp ban: " + e.getMessage());
        } finally {
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
            }
        }
    }

    // Temporary ban message
    private String getTempBanMessage(String reason, String duration) {
        return ChatColor.translateAlternateColorCodes('&',
                "&cYou are temporarily banned for &f" + duration + " &cfrom this server!\n" +
                        "&7\n" +
                        "&7Reason: &f" + reason + "\n" +
                        "&7Find out more: &b&n" + plugin.getConfigManager().getAppealLink() + "\n" +
                        "&7\n" +
                        "&7Ban ID: &f#0001\n" +
                        "&7Sharing your Ban ID may affect the processing of your appeal"
        );
    }

    // Permanent ban message
    private String getDefaultBanMessage(String reason) {
        return ChatColor.translateAlternateColorCodes('&',
                "&cYou are permanently banned from this server!\n" +
                        "&7\n" +
                        "&7Reason: &f" + reason + "\n" +
                        "&7Find out more: &b&n" + plugin.getConfigManager().getAppealLink() + "\n" +
                        "&7\n" +
                        "&7Ban ID: &f#0001\n" +
                        "&7Sharing your Ban ID may affect the processing of your appeal"
        );
    }

    public boolean addPunishment(String playerName, String playerUUID, String playerIP,
                                 PunishmentType type, String reason, String operator,
                                 long duration, boolean silent) {

        plugin.getLogger().info("Adding punishment: " + type + " for " + playerName);

        Connection conn = dbManager.getConnection();
        if (conn == null) {
            plugin.getLogger().severe("Database connection is NULL!");
            return false;
        }

        PreparedStatement stmt = null;
        try {
            String sql;
            if (dbManager.getDatabaseType().equals("mysql")) {
                sql = "INSERT INTO punishments (player_name, player_uuid, player_ip, punishment_type, " +
                        "reason, operator, duration, start_time, end_time, silent, active) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)";
            } else {
                // SQLite uses 1 for TRUE
                sql = "INSERT INTO punishments (player_name, player_uuid, player_ip, punishment_type, " +
                        "reason, operator, duration, start_time, end_time, silent, active) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";
            }

            stmt = conn.prepareStatement(sql);

            long startTime = System.currentTimeMillis();
            long endTime = (duration > 0) ? startTime + duration : 0;

            stmt.setString(1, playerName);
            stmt.setString(2, playerUUID);
            stmt.setString(3, playerIP);
            stmt.setString(4, type.name());
            stmt.setString(5, reason);
            stmt.setString(6, operator);

            if (duration > 0) {
                stmt.setLong(7, duration);
            } else {
                stmt.setNull(7, Types.BIGINT);
            }

            stmt.setLong(8, startTime);

            if (duration > 0) {
                stmt.setLong(9, endTime);
            } else {
                stmt.setNull(9, Types.BIGINT);
            }

            // Convert boolean to int for SQLite (1=true, 0=false)
            if (dbManager.getDatabaseType().equals("mysql")) {
                stmt.setBoolean(10, silent);
            } else {
                stmt.setInt(10, silent ? 1 : 0);
            }

            int rows = stmt.executeUpdate();

            if (rows > 0) {
                plugin.getLogger().info("Punishment added to database: " + type + " for " + playerName);

                // Apply punishment immediately
                applyPunishment(playerName, type, reason, duration, operator, silent);
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("SQL ERROR in addPunishment: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
            }
        }
        return false;
    }

    private void applyPunishment(String playerName, PunishmentType type, String reason,
                                 long duration, String operator, boolean silent) {
        Player player = Bukkit.getPlayer(playerName);

        switch (type) {
            case BAN:
                if (player != null && player.isOnline()) {
                    String kickMessage = getDefaultBanMessage(reason);
                    player.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMessage));
                }
                break;

            case TEMPBAN:
                if (player != null && player.isOnline()) {
                    String formattedDuration = formatDuration(duration);
                    String kickMessage = getTempBanMessage(reason, formattedDuration);
                    player.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMessage));
                }
                break;

            case KICK:
                if (player != null && player.isOnline()) {
                    player.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                            "&cYou got kicked from this server!\n" +
                                    "&7\n" +
                                    "&7Reason: &l" + reason + "\n" +
                                    "&7"
                    ));
                }
                break;

            case MUTE:
            case TEMPMUTE:
            case WARN:
                // These are handled by event listeners
                break;
        }
    }

    public boolean isPlayerBanned(String playerName) {
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String query = "SELECT * FROM punishments WHERE player_name = ? " +
                    "AND punishment_type IN ('BAN', 'TEMPBAN') " +
                    "AND active = 1";

            stmt = conn.prepareStatement(query);
            stmt.setString(1, playerName);
            rs = stmt.executeQuery();

            while (rs.next()) {
                String type = rs.getString("punishment_type");

                if (type.equals("BAN")) {
                    return true;
                } else if (type.equals("TEMPBAN")) {
                    long endTime = rs.getLong("end_time");
                    if (endTime == 0 || endTime > System.currentTimeMillis()) {
                        return true;
                    } else {
                        // Temp ban expired - remove it
                        removeExpiredTempBan(playerName);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check ban status", e);
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
        }
        return false;
    }

    public boolean isPlayerMuted(String playerName) {
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String query = "SELECT * FROM punishments WHERE player_name = ? " +
                    "AND punishment_type IN ('MUTE', 'TEMPMUTE') " +
                    "AND active = 1";

            stmt = conn.prepareStatement(query);
            stmt.setString(1, playerName);
            rs = stmt.executeQuery();

            while (rs.next()) {
                String type = rs.getString("punishment_type");

                if (type.equals("MUTE")) {
                    return true;
                } else if (type.equals("TEMPMUTE")) {
                    long endTime = rs.getLong("end_time");
                    if (endTime == 0 || endTime > System.currentTimeMillis()) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check mute status", e);
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
        }
        return false;
    }

    public boolean removePunishment(String playerName, String punishmentType) {
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        PreparedStatement stmt = null;
        try {
            String query = "UPDATE punishments SET active = 0 WHERE player_name = ? " +
                    "AND punishment_type = ? AND active = 1";

            stmt = conn.prepareStatement(query);
            stmt.setString(1, playerName);
            stmt.setString(2, punishmentType);

            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to remove punishment", e);
            return false;
        } finally {
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
            }
        }
    }

    public String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day(s) " + (hours % 24) + " hour(s)";
        } else if (hours > 0) {
            return hours + " hour(s) " + (minutes % 60) + " minute(s)";
        } else if (minutes > 0) {
            return minutes + " minute(s) " + (seconds % 60) + " second(s)";
        } else {
            return seconds + " second(s)";
        }
    }

    public long parseDuration(String durationStr) {
        try {
            if (durationStr.startsWith("#")) {
                durationStr = durationStr.substring(1);
            }

            long total = 0;

            if (durationStr.endsWith("s")) {
                long seconds = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
                total = seconds * 1000;
            }
            else if (durationStr.endsWith("m")) {
                long minutes = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
                total = minutes * 60 * 1000;
            }
            else if (durationStr.endsWith("h")) {
                long hours = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
                total = hours * 60 * 60 * 1000;
            }
            else if (durationStr.endsWith("d")) {
                long days = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
                total = days * 24 * 60 * 60 * 1000;
            }
            else {
                // Try to parse as just minutes
                try {
                    long minutes = Long.parseLong(durationStr);
                    total = minutes * 60 * 1000;
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid duration format: " + durationStr);
                    return -1;
                }
            }

            return total;
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid duration format: " + durationStr);
            return -1;
        }
    }
}