package me.herex.watchdogreport.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;

public class ConfirmationGUI {

    public ConfirmationGUI() {
        // Empty constructor
    }

    public Inventory createConfirmationMenu(String reportedName, String reason) {
        Inventory confirmMenu = Bukkit.createInventory(null, 9 * 3, "Confirm Report");

        ItemStack confirm = new ItemStack(Material.STAINED_CLAY, 1, (short) 13);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Submit Report");
        confirm.setItemMeta(confirmMeta);
        confirmMenu.setItem(11, confirm);

        ItemStack cancel = new ItemStack(Material.STAINED_CLAY, 1, (short) 14);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "Cancel Report");
        cancel.setItemMeta(cancelMeta);
        confirmMenu.setItem(15, cancel);

        // Player head center
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
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

        return confirmMenu;
    }
}