package me.herex.watchdogreport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.*;
import java.util.*;

public class ReportManager {

    private final WatchdogReport plugin;
    private final DatabaseManager dbManager;
    private final Map<Player, String> reportSelections = new HashMap<>();
    private final Map<String, Long> reportCooldowns = new HashMap<>();
    private final Set<String> validReasons;

    public ReportManager(WatchdogReport plugin) {
        this.plugin = plugin;
        this.dbManager = plugin.getDatabaseManager();
        this.validReasons = new HashSet<>(Arrays.asList(
                "Chat Abuse", "Cheating (Hacks)", "Bad Name", "Bad Skin", "Other",
                "Bug Abuse", "Bad Pet name", "Boosting / Exploits"
        ));
    }

    // Open Hypixel-style report GUI
    public void openReportGUI(Player player, String reportedName) {
        reportSelections.put(player, reportedName);

        Inventory menu = Bukkit.createInventory(null, 54, "Report Menu");

        // Player head (top center) - Hypixel style using VersionAdapter
        ItemStack head = VersionAdapter.createSkull();
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwner(reportedName);
        skullMeta.setDisplayName(ChatColor.GRAY + "/reporting " + ChatColor.AQUA + reportedName);
        skullMeta.setLore(Arrays.asList(ChatColor.GRAY + "Select a reason below"));
        head.setItemMeta(skullMeta);
        menu.setItem(4, head);

        // Report reason items with version compatibility
        menu.setItem(20, createMenuItem(VersionAdapter.getBookAndQuillMaterial(), ChatColor.GREEN + "Chat Abuse", (short) 0));
        menu.setItem(21, createMenuItem(Material.DIAMOND_SWORD, ChatColor.GREEN + "Cheating (Hacks)", (short) 0));
        menu.setItem(22, createMenuItem(Material.PAPER, ChatColor.GREEN + "Bad Name", (short) 0));
        menu.setItem(23, createMenuItem(Material.BANNER, ChatColor.GREEN + "Bad Skin", VersionAdapter.isLegacy() ? (short) 10 : (short) 0));
        menu.setItem(24, createMenuItem(Material.COMPASS, ChatColor.GREEN + "Other", (short) 0));

        // Second row
        menu.setItem(29, createMenuItem(Material.LEATHER, ChatColor.GREEN + "Bug Abuse", (short) 0));
        menu.setItem(30, createMenuItem(Material.MONSTER_EGG, ChatColor.GREEN + "Bad Pet name", VersionAdapter.isLegacy() ? (short) 98 : (short) 0));
        menu.setItem(31, createMenuItem(Material.TNT, ChatColor.GREEN + "Boosting / Exploits", (short) 0));

        // Info book and close button
        menu.setItem(48, createInfoBook());
        menu.setItem(49, createCloseItem());

        player.openInventory(menu);
    }

    private ItemStack createMenuItem(Material material, String name, short durability) {
        ItemStack item;
        if (VersionAdapter.isLegacy()) {
            item = new ItemStack(material, 1, durability);
        } else {
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList("" + ChatColor.RED + "Abuse may result in punishment!"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoBook() {
        ItemStack book = new ItemStack(Material.BOOK, 1);
        ItemMeta meta = book.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Use this menu to report a player for breaking our rules");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Hover over the specific report type",
                ChatColor.GRAY + "for detailed information and then",
                ChatColor.GRAY + "select the closest option for the",
                ChatColor.GRAY + "rules being broken.",
                ChatColor.GRAY + "", ChatColor.GRAY + "Visit " + ChatColor.AQUA + "https://farepixel.fun/rules" + ChatColor.GRAY + " for",
                ChatColor.GRAY + "more information."
        ));
        book.setItemMeta(meta);
        return book;
    }

    private ItemStack createCloseItem() {
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta meta = close.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Close");
        close.setItemMeta(meta);
        return close;
    }

    // Open confirmation menu
    public void openConfirmationMenu(Player player, String reportedName, String reason) {
        reportSelections.put(player, reportedName + ":" + reason);

        Inventory confirmMenu = Bukkit.createInventory(null, 9 * 3, "Confirm Report");

        // Green clay/terracotta using VersionAdapter
        ItemStack confirm = VersionAdapter.createColoredClay((short) 13);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Submit Report");
        confirm.setItemMeta(confirmMeta);
        confirmMenu.setItem(11, confirm);

        // Red clay/terracotta using VersionAdapter
        ItemStack cancel = VersionAdapter.createColoredClay((short) 14);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel Report");
        cancel.setItemMeta(cancelMeta);
        confirmMenu.setItem(15, cancel);

        // Player head center
        ItemStack head = VersionAdapter.createSkull();
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwner(reportedName);
        headMeta.setDisplayName(ChatColor.GRAY + "/Reported " + ChatColor.AQUA + reportedName);
        headMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "",
                ChatColor.YELLOW + "Report " + reportedName + " for",
                ChatColor.GREEN + reason
        ));
        head.setItemMeta(headMeta);
        confirmMenu.setItem(13, head);

        player.openInventory(confirmMenu);
    }

    // Submit report to database
    public boolean submitReport(Player player, String reportedName, String reason) {
        // Check cooldown
        if (isOnCooldown(player.getName())) {
            int remaining = getCooldownSeconds(player.getName());
            sendError(player, "Please wait " + remaining + " seconds before reporting again.");
            return false;
        }

        // Check database connection
        Connection conn = dbManager.getConnection();
        if (conn == null) {
            sendError(player, "Database connection is not available. Please contact an administrator.");
            return false;
        }

        PreparedStatement stmt = null;
        try {
            String query;
            if (dbManager.getDatabaseType().equals("mysql")) {
                query = "INSERT INTO reports (reporter, reported, reason, timestamp, status) VALUES (?, ?, ?, NOW(), 'pending')";
            } else {
                query = "INSERT INTO reports (reporter, reported, reason, timestamp, status) VALUES (?, ?, ?, ?, 'pending')";
            }

            stmt = conn.prepareStatement(query);
            stmt.setString(1, player.getName());
            stmt.setString(2, reportedName);
            stmt.setString(3, reason);

            if (!dbManager.getDatabaseType().equals("mysql")) {
                stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            }

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                setCooldown(player.getName());

                String message = "&b[STAFF] &c[REPORT] &e" + player.getName() + " &ahas reported &e" + reportedName + " &afor &e[" + reason + "]";

                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("watchdog.staff")) {
                        plugin.sendMessage(p, message);
                    }
                }

                plugin.sendMessage(player, "&aThanks for your " + reason + " report. We understand your concerns and it will be reviewed as soon as possible.");
                plugin.sendMessage(player, "&cWarning! Abuse of this command is punishable.");

                plugin.getLogger().info("Report submitted: " + player.getName() + " reported " + reportedName + " for " + reason);
                reportSelections.remove(player);
                return true;
            }

        } catch (SQLException e) {
            plugin.getLogger().severe("Database error when submitting report: " + e.getMessage());
            sendError(player, "Database error! Report not saved.");
        } finally {
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
            }
        }

        return false;
    }

    // Accept report
    public boolean acceptReport(String reporter, String reported, String staff) {
        Connection conn = dbManager.getConnection();
        if (conn == null) return false;

        PreparedStatement stmt = null;
        try {
            String query = "UPDATE reports SET status = 'accepted' WHERE reporter = ? AND reported = ? AND status = 'pending'";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, reporter);
            stmt.setString(2, reported);

            int rows = stmt.executeUpdate();

            // Notify reporter if online
            if (rows > 0) {
                Player reporterPlayer = Bukkit.getPlayer(reporter);
                if (reporterPlayer != null) {
                    plugin.sendMessage(reporterPlayer,
                            "&f[WATCHDOG] &aYour report against " + reported + " has been addressed! Thanks for your help!"
                    );
                }
            }

            return rows > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Database error when accepting report: " + e.getMessage());
            return false;
        } finally {
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) {}
            }
        }
    }

    // Get pending reports
    public List<Map<String, Object>> getPendingReports(int limit) {
        List<Map<String, Object>> reports = new ArrayList<>();
        Connection conn = dbManager.getConnection();
        if (conn == null) return reports;

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String query = "SELECT id, reporter, reported, reason, timestamp FROM reports " +
                    "WHERE status = 'pending' ORDER BY timestamp DESC LIMIT ?";

            stmt = conn.prepareStatement(query);
            stmt.setInt(1, limit);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, Object> report = new HashMap<>();
                report.put("id", rs.getInt("id"));
                report.put("reporter", rs.getString("reporter"));
                report.put("reported", rs.getString("reported"));
                report.put("reason", rs.getString("reason"));
                report.put("timestamp", rs.getTimestamp("timestamp"));
                reports.add(report);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database error when getting reports: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
        }

        return reports;
    }

    // Cooldown system
    private boolean isOnCooldown(String playerName) {
        if (!reportCooldowns.containsKey(playerName)) return false;
        long lastReport = reportCooldowns.get(playerName);
        long cooldownMillis = plugin.getConfigManager().getCooldownSeconds() * 1000L;
        return System.currentTimeMillis() - lastReport < cooldownMillis;
    }

    private int getCooldownSeconds(String playerName) {
        if (!reportCooldowns.containsKey(playerName)) return 0;
        long lastReport = reportCooldowns.get(playerName);
        long cooldownMillis = plugin.getConfigManager().getCooldownSeconds() * 1000L;
        long remainingMillis = cooldownMillis - (System.currentTimeMillis() - lastReport);
        return (int) Math.max(0, remainingMillis / 1000);
    }

    private void setCooldown(String playerName) {
        reportCooldowns.put(playerName, System.currentTimeMillis());
    }

    // Getters for event listener
    public String getPlayerSelection(Player player) {
        return reportSelections.get(player);
    }

    public void removePlayerSelection(Player player) {
        reportSelections.remove(player);
    }

    public boolean isValidReason(String reason) {
        return validReasons.contains(reason);
    }

    // Get reported player from selection
    public String getReportedPlayer(Player player) {
        String selection = reportSelections.get(player);
        if (selection != null && !selection.contains(":")) {
            return selection;
        } else if (selection != null && selection.contains(":")) {
            String[] parts = selection.split(":");
            if (parts.length > 0) {
                return parts[0];
            }
        }
        return null;
    }

    // Get selected reason from selection
    public String getSelectedReason(Player player) {
        String selection = reportSelections.get(player);
        if (selection != null && selection.contains(":")) {
            String[] parts = selection.split(":");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return null;
    }

    // Send error message to player
    public void sendError(Player player, String message) {
        plugin.sendMessage(player, "&c" + message);
    }

    // Send success message to player
    public void sendSuccess(Player player, String message) {
        plugin.sendMessage(player, "&a" + message);
    }

    // Get pending reports count
    public int getPendingReportsCount() {
        Connection conn = dbManager.getConnection();
        if (conn == null) return 0;

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String query = "SELECT COUNT(*) as count FROM reports WHERE status = 'pending'";
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Database error when counting pending reports: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) {}
            try { if (stmt != null) stmt.close(); } catch (SQLException e) {}
        }

        return 0;
    }

    // Cleanup player data when they logout
    public void cleanupPlayerData(Player player) {
        reportSelections.remove(player);
    }

    // Remove cooldown (admin command)
    public boolean removeCooldown(String playerName) {
        if (reportCooldowns.containsKey(playerName)) {
            reportCooldowns.remove(playerName);
            return true;
        }
        return false;
    }

    // Check if player has pending selection
    public boolean hasPendingSelection(Player player) {
        return reportSelections.containsKey(player);
    }
}