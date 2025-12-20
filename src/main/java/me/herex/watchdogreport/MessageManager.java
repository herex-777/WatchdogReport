package me.herex.watchdogreport;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MessageManager {

    private final WatchdogReport plugin;
    private FileConfiguration messages;

    public MessageManager(WatchdogReport plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public String getMessage(String path, String... replacements) {
        String message = messages.getString(path, "&cMessage not found: " + path);

        // Apply replacements
        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public List<String> getMessageList(String path, String... replacements) {
        List<String> raw = messages.getStringList(path);
        List<String> formatted = new ArrayList<>();

        for (String line : raw) {
            // Apply replacements
            if (replacements.length % 2 == 0) {
                for (int i = 0; i < replacements.length; i += 2) {
                    line = line.replace("{" + replacements[i] + "}", replacements[i + 1]);
                }
            }
            formatted.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        return formatted;
    }
}