package me.herex.watchdogreport.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ReportGUI {

    // Map to store which reason corresponds to which slot
    private final Map<Integer, String> slotToReason = new HashMap<>();

    public ReportGUI() {
        // Initialize slot mappings exactly like your original code
        initializeSlotMappings();
    }

    private void initializeSlotMappings() {
        slotToReason.put(20, "Chat Abuse");
        slotToReason.put(21, "Cheating (Hacks)");
        slotToReason.put(22, "Bad Name");
        slotToReason.put(23, "Bad Skin");
        slotToReason.put(24, "Other");
        slotToReason.put(29, "Bug Abuse");
        slotToReason.put(30, "Bad Pet name");
        slotToReason.put(31, "Boosting / Exploits");
    }

    public Inventory createReportMenu(String reportedName) {
        Inventory menu = Bukkit.createInventory(null, 54, "Report Menu");

        // Player head (top center) - EXACTLY like original
        ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwner(reportedName);
        skullMeta.setDisplayName(ChatColor.YELLOW + "/reporting " + reportedName);
        skullMeta.setLore(Arrays.asList(ChatColor.GRAY + "Select a reason below"));
        head.setItemMeta(skullMeta);
        menu.setItem(4, head);

        // Report reason items - EXACT positions like original
        menu.setItem(20, createMenuItem(Material.BOOK_AND_QUILL, ChatColor.GREEN + "Chat Abuse", (short) 0));
        menu.setItem(21, createMenuItem(Material.DIAMOND_SWORD, ChatColor.GREEN + "Cheating (Hacks)", (short) 0));
        menu.setItem(22, createMenuItem(Material.PAPER, ChatColor.GREEN + "Bad Name", (short) 0));
        // Changed banner to green banner (as requested)
        menu.setItem(23, createMenuItem(Material.BANNER, ChatColor.GREEN + "Bad Skin", (short) 10)); // 10 = Green banner
        menu.setItem(24, createMenuItem(Material.COMPASS, ChatColor.GREEN + "Other", (short) 0));

        // Second row - EXACT positions like original
        menu.setItem(29, createMenuItem(Material.LEATHER, ChatColor.GREEN + "Bug Abuse", (short) 0));
        // Changed to cow egg (98 = cow)
        menu.setItem(30, createMenuItem(Material.MONSTER_EGG, ChatColor.GREEN + "Bad Pet name", (short) 98));
        menu.setItem(31, createMenuItem(Material.TNT, ChatColor.GREEN + "Boosting / Exploits", (short) 0));

        // Book with Hypixel-like lore
        menu.setItem(48, createInfoBook());
        menu.setItem(49, createCloseItem());

        return menu;
    }

    private ItemStack createMenuItem(Material material, String name, short durability) {
        ItemStack item = new ItemStack(material, 1, durability);
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
        meta.setDisplayName(ChatColor.RED + "Close"); // FIXED: Removed "(#0166)"
        // FIXED: No lore added
        close.setItemMeta(meta);
        return close;
    }

    public String getReasonFromSlot(int slot) {
        return slotToReason.get(slot);
    }

    public boolean isReasonSlot(int slot) {
        return slotToReason.containsKey(slot);
    }
}