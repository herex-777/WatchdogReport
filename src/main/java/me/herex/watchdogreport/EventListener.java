package me.herex.watchdogreport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;

public class EventListener implements Listener {

    private final WatchdogReport plugin;
    private final PunishmentManager punishmentManager;
    private final ReportManager reportManager;

    public EventListener(WatchdogReport plugin) {
        this.plugin = plugin;
        this.punishmentManager = plugin.getPunishmentManager();
        this.reportManager = plugin.getReportManager();
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        // Check if player is banned
        if (punishmentManager.isPlayerBanned(player.getName())) {
            event.setResult(PlayerLoginEvent.Result.KICK_BANNED);

            // Get the proper ban message from PunishmentManager
            String kickMessage = punishmentManager.getBanKickMessage(player.getName());
            if (kickMessage != null) {
                event.setKickMessage(kickMessage);
            } else {
                // Fallback message
                event.setKickMessage(ChatColor.translateAlternateColorCodes('&',
                        "&cYou are banned from this server!\n" +
                                "&7\n" +
                                "&7Find out more: &b&n" + plugin.getConfigManager().getAppealLink()
                ));
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (punishmentManager.isPlayerMuted(player.getName())) {
            event.setCancelled(true);
            plugin.sendMessage(player, "&cYou are muted!");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        event.setCancelled(true);

        if (title.equals("Report Menu")) {
            handleReportMenuClick(player, event);
        } else if (title.equals("Confirm Report")) {
            handleConfirmMenuClick(player, event);
        }
    }

    private void handleReportMenuClick(Player player, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        int slot = event.getSlot();

        // Close button
        if (slot == 49 && item.getType() == Material.BARRIER) {
            player.closeInventory();
            reportManager.removePlayerSelection(player);
            return;
        }

        // Reason selection slots
        String reason = null;
        switch (slot) {
            case 20: reason = "Chat Abuse"; break;
            case 21: reason = "Cheating (Hacks)"; break;
            case 22: reason = "Bad Name"; break;
            case 23: reason = "Bad Skin"; break;
            case 24: reason = "Other"; break;
            case 29: reason = "Bug Abuse"; break;
            case 30: reason = "Bad Pet name"; break;
            case 31: reason = "Boosting / Exploits"; break;
        }

        if (reason != null) {
            String reportedName = reportManager.getPlayerSelection(player);
            if (reportedName != null) {
                reportManager.openConfirmationMenu(player, reportedName, reason);
            }
        }
    }

    private void handleConfirmMenuClick(Player player, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (item.getType() == Material.STAINED_CLAY) {
            short data = item.getDurability();

            String selection = reportManager.getPlayerSelection(player);
            if (selection == null || !selection.contains(":")) {
                player.closeInventory();
                reportManager.removePlayerSelection(player);
                return;
            }

            String[] parts = selection.split(":");
            String reportedName = parts[0];
            String reason = parts[1];

            if (data == 13) { // Green - Submit
                if (reportManager.submitReport(player, reportedName, reason)) {
                    player.closeInventory();
                } else {
                    plugin.sendMessage(player, "&cFailed to submit report.");
                }
            } else if (data == 14) { // Red - Cancel
                plugin.sendMessage(player, "&cReport cancelled.");
                player.closeInventory();
                reportManager.removePlayerSelection(player);
            }
        }
    }
}