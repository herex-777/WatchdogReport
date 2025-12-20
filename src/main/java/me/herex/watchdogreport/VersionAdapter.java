package me.herex.watchdogreport;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class VersionAdapter {

    private static String serverVersion;

    static {
        serverVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    }

    public static boolean isLegacy() {
        return serverVersion.contains("v1_8") ||
                serverVersion.contains("v1_9") ||
                serverVersion.contains("v1_10") ||
                serverVersion.contains("v1_11") ||
                serverVersion.contains("v1_12");
    }

    public static Material getSkullMaterial() {
        try {
            if (isLegacy()) {
                return Material.valueOf("SKULL_ITEM");
            } else {
                return Material.valueOf("PLAYER_HEAD");
            }
        } catch (IllegalArgumentException e) {
            return Material.valueOf("PLAYER_HEAD");
        }
    }

    public static Material getBookAndQuillMaterial() {
        try {
            if (isLegacy()) {
                return Material.valueOf("BOOK_AND_QUILL");
            } else {
                return Material.valueOf("WRITABLE_BOOK");
            }
        } catch (IllegalArgumentException e) {
            return Material.valueOf("WRITABLE_BOOK");
        }
    }

    public static Material getStainedClayMaterial() {
        try {
            if (isLegacy()) {
                return Material.valueOf("STAINED_CLAY");
            } else {
                return Material.valueOf("TERRACOTTA");
            }
        } catch (IllegalArgumentException e) {
            return Material.valueOf("TERRACOTTA");
        }
    }

    public static ItemStack createSkull() {
        if (isLegacy()) {
            return new ItemStack(getSkullMaterial(), 1, (short) 3);
        } else {
            return new ItemStack(getSkullMaterial(), 1);
        }
    }

    public static ItemStack createColoredClay(short color) {
        if (isLegacy()) {
            return new ItemStack(getStainedClayMaterial(), 1, color);
        } else {
            Material material = getConcreteColor(color);
            return new ItemStack(material, 1);
        }
    }

    private static Material getConcreteColor(short color) {
        switch (color) {
            case 13: return Material.valueOf("GREEN_CONCRETE");
            case 14: return Material.valueOf("RED_CONCRETE");
            default: return Material.valueOf("WHITE_CONCRETE");
        }
    }
}