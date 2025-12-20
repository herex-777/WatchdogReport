package me.herex.watchdogreport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Map;

public class CommandManager {

    private final WatchdogReport plugin;
    private final ReportManager reportManager;
    private final PunishmentManager punishmentManager;

    public CommandManager(WatchdogReport plugin) {
        this.plugin = plugin;
        this.reportManager = plugin.getReportManager();
        this.punishmentManager = plugin.getPunishmentManager();
    }

    public boolean handleCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "report": return handleReport(sender, args);
            case "watchdogreport-accept": return handleAcceptReport(sender, args);
            case "watchdoglist": return handleListReports(sender, args);
            case "ban": return handleBan(sender, args);
            case "tempban": return handleTempBan(sender, args);
            case "mute": return handleMute(sender, args);
            case "tempmute": return handleTempMute(sender, args);
            case "warn": return handleWarn(sender, args);
            case "kick": return handleKick(sender, args);
            case "unban": return handleUnban(sender, args);
            case "unmute": return handleUnmute(sender, args);
            case "history": return handleHistory(sender, args);
            default: return false;
        }
    }

    private boolean handleReport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.sendMessage(sender, "&cThis command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            plugin.sendMessage(player, "&cUse: /report <username>");
            return true;
        }

        String reportedName = args[0];

        if (reportedName.equalsIgnoreCase(player.getName())) {
            plugin.sendMessage(player, "&f[WATCHDOG] &cYou can't report yourself!");
            return true;
        }

        reportManager.openReportGUI(player, reportedName);
        return true;
    }

    private boolean handleAcceptReport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("farepixel.mod")) {
            plugin.sendMessage(sender, "&cYou're not the rank of this command! You need to be admin or higher!");
            return true;
        }

        if (args.length < 2) {
            plugin.sendMessage(sender, "&cUse: /watchdogreport-accept <reporter> <reported>");
            return true;
        }

        String reporter = args[0];
        String reported = args[1];

        if (reportManager.acceptReport(reporter, reported, sender.getName())) {
            plugin.sendMessage(sender, "&f[WATCHDOG] &aAccepted " + reporter + "'s report against " + reported + ".");
        } else {
            plugin.sendMessage(sender, "&cNo pending report found for " + reporter + " against " + reported);
        }

        return true;
    }

    private boolean handleListReports(CommandSender sender, String[] args) {
        if (!sender.hasPermission("farepixel.admin")) {
            plugin.sendMessage(sender, "&cYou don't have permission to view reports!");
            return true;
        }

        List<Map<String, Object>> reports = reportManager.getPendingReports(20);

        if (reports.isEmpty()) {
            plugin.sendMessage(sender, "&f[WATCHDOG] No pending reports.");
            return true;
        }

        plugin.sendMessage(sender, "&f[WATCHDOG] Pending reports (latest first):");
        for (Map<String, Object> report : reports) {
            String line = "&eID: " + report.get("id") + " &7- &e" + report.get("reporter") +
                    " &areported &e" + report.get("reported") + " &afor &e[" + report.get("reason") +
                    "] &aat &e" + report.get("timestamp");
            plugin.sendMessage(sender, line);
        }
        return true;
    }

    private boolean handleBan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("watchdog.ban")) {
            plugin.sendMessage(sender, "&cYou don't have permission for that!");
            return true;
        }

        if (args.length < 2) {
            plugin.sendMessage(sender, "&cUsage: /ban <player> <reason>");
            plugin.sendMessage(sender, "&7Use &f/ban -s <player> <reason> &7for silent ban");
            return true;
        }

        boolean silent = args[0].equalsIgnoreCase("-s");
        int startIndex = silent ? 1 : 0;

        if (args.length < startIndex + 2) {
            plugin.sendMessage(sender, "&cUsage: /ban <player> <reason>");
            return true;
        }

        String playerName = args[startIndex];
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = startIndex + 1; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        if (reason.isEmpty()) {
            reason = "No reason provided";
        }

        if (punishmentManager.isPlayerBanned(playerName)) {
            plugin.sendMessage(sender, "&c" + playerName + " is already banned!");
            return true;
        }

        if (punishmentManager.addPunishment(playerName, null, null,
                PunishmentManager.PunishmentType.BAN, reason,
                sender.getName(), 0, silent)) {
            plugin.sendMessage(sender, "&c" + playerName + " was successfully banned!");

            if (!silent) {
                String message = "&c&l&n" + playerName + " &cgot banned by &l" + sender.getName() + " &cFor " + reason + " permanently";
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("watchdog.staff")) {
                        plugin.sendMessage(p, message);
                    }
                }
            }

            Player target = Bukkit.getPlayer(playerName);
            if (target != null) {
                target.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                        "&cYou are permanently banned from this server!\n" +
                                "&7\n" +
                                "&7Reason: &f" + reason + "\n" +
                                "&7Find out more: &b&n" + plugin.getConfigManager().getAppealLink() + "\n" +
                                "&7\n" +
                                "&7Ban ID: &f#0001\n" +
                                "&7Sharing your Ban ID may affect the processing of your appeal"
                ));
            }
        } else {
            plugin.sendMessage(sender, "&cFailed to ban " + playerName);
        }

        return true;
    }

    private boolean handleTempBan(CommandSender sender, String[] args) {
        if (!sender.hasPermission("watchdog.tempban")) {
            plugin.sendMessage(sender, "&cYou don't have permission for that!");
            return true;
        }

        if (args.length < 3) {
            plugin.sendMessage(sender, "&cUsage: /tempban <player> <duration> <reason>");
            plugin.sendMessage(sender, "&7Use &f/tempban -s <player> <duration> <reason> &7for silent tempban");
            plugin.sendMessage(sender, "&7Duration format: 30s, 10m, 2h, 1d");
            return true;
        }

        boolean silent = args[0].equalsIgnoreCase("-s");
        int startIndex = silent ? 1 : 0;

        if (args.length < startIndex + 3) {
            plugin.sendMessage(sender, "&cUsage: /tempban <player> <duration> <reason>");
            return true;
        }

        String playerName = args[startIndex];
        String durationStr = args[startIndex + 1];
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = startIndex + 2; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        if (reason.isEmpty()) {
            reason = "No reason provided";
        }

        long duration = punishmentManager.parseDuration(durationStr);
        if (duration <= 0) {
            plugin.sendMessage(sender, "&cInvalid duration format! Use: 30s, 10m, 2h, 1d");
            return true;
        }

        if (punishmentManager.addPunishment(playerName, null, null,
                PunishmentManager.PunishmentType.TEMPBAN, reason,
                sender.getName(), duration, silent)) {
            String formattedDuration = punishmentManager.formatDuration(duration);
            plugin.sendMessage(sender, "&c" + playerName + " was successfully temp-banned for " + formattedDuration + "!");

            if (!silent) {
                String message = "&c&l&n" + playerName + " &cgot banned by &l" + sender.getName() +
                        " &cFor " + reason + " For &f" + formattedDuration;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("watchdog.staff")) {
                        plugin.sendMessage(p, message);
                    }
                }
            }

            Player target = Bukkit.getPlayer(playerName);
            if (target != null) {
                target.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                        "&c You are temporarily banned for &f" + formattedDuration + " &cfrom this server!\n" +
                                "&7\n" +
                                "&7Reason: &f" + reason + "\n" +
                                "&7Find out more: &b&n" + plugin.getConfigManager().getAppealLink() + "\n" +
                                "&7\n" +
                                "&7Ban ID: &f#0001\n" +
                                "&7Sharing your Ban ID may affect the processing of your appeal"
                ));
            }
        } else {
            plugin.sendMessage(sender, "&cFailed to temp-ban " + playerName);
        }

        return true;
    }

    private boolean handleMute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("watchdog.mute")) {
            plugin.sendMessage(sender, "&cYou don't have permission for that!");
            return true;
        }

        if (args.length < 2) {
            plugin.sendMessage(sender, "&cUsage: /mute <player> <reason>");
            plugin.sendMessage(sender, "&7Use &f/mute -s <player> <reason> &7for silent mute");
            return true;
        }

        boolean silent = args[0].equalsIgnoreCase("-s");
        int startIndex = silent ? 1 : 0;

        if (args.length < startIndex + 2) {
            plugin.sendMessage(sender, "&cUsage: /mute <player> <reason>");
            return true;
        }

        String playerName = args[startIndex];
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = startIndex + 1; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        if (reason.isEmpty()) {
            reason = "No reason provided";
        }

        if (punishmentManager.isPlayerMuted(playerName)) {
            plugin.sendMessage(sender, "&c" + playerName + " is already muted!");
            return true;
        }

        if (punishmentManager.addPunishment(playerName, null, null,
                PunishmentManager.PunishmentType.MUTE, reason,
                sender.getName(), 0, silent)) {
            plugin.sendMessage(sender, "&c" + playerName + " was successfully muted!");

            if (!silent) {
                String message = "&c&l&n" + playerName + " &cgot muted permanently by &l" + sender.getName() + " &cFor " + reason;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("watchdog.staff")) {
                        plugin.sendMessage(p, message);
                    }
                }
            }
        } else {
            plugin.sendMessage(sender, "&cFailed to mute " + playerName);
        }

        return true;
    }

    private boolean handleTempMute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("watchdog.tempmute")) {
            plugin.sendMessage(sender, "&cYou don't have permission for that!");
            return true;
        }

        if (args.length < 3) {
            plugin.sendMessage(sender, "&cUsage: /tempmute <player> <duration> <reason>");
            plugin.sendMessage(sender, "&7Use &f/tempmute -s <player> <duration> <reason> &7for silent tempmute");
            plugin.sendMessage(sender, "&7Duration format: 30s, 10m, 2h, 1d");
            return true;
        }

        boolean silent = args[0].equalsIgnoreCase("-s");
        int startIndex = silent ? 1 : 0;

        if (args.length < startIndex + 3) {
            plugin.sendMessage(sender, "&cUsage: /tempmute <player> <duration> <reason>");
            return true;
        }

        String playerName = args[startIndex];
        String durationStr = args[startIndex + 1];
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = startIndex + 2; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        if (reason.isEmpty()) {
            reason = "No reason provided";
        }

        long duration = punishmentManager.parseDuration(durationStr);
        if (duration <= 0) {
            plugin.sendMessage(sender, "&cInvalid duration format! Use: 30s, 10m, 2h, 1d");
            return true;
        }

        if (punishmentManager.addPunishment(playerName, null, null,
                PunishmentManager.PunishmentType.TEMPMUTE, reason,
                sender.getName(), duration, silent)) {
            String formattedDuration = punishmentManager.formatDuration(duration);
            plugin.sendMessage(sender, "&c" + playerName + " was successfully temp-muted for " + formattedDuration + "!");

            if (!silent) {
                String message = "&c&l&n" + playerName + " &cgot muted by &l" + sender.getName() +
                        " &cFor " + reason + " For &f" + formattedDuration;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("watchdog.staff")) {
                        plugin.sendMessage(p, message);
                    }
                }
            }
        } else {
            plugin.sendMessage(sender, "&cFailed to temp-mute " + playerName);
        }

        return true;
    }

    private boolean handleWarn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("watchdog.warn")) {
            plugin.sendMessage(sender, "&cYou don't have permission for that!");
            return true;
        }

        if (args.length < 2) {
            plugin.sendMessage(sender, "&cUsage: /warn <player> <reason>");
            plugin.sendMessage(sender, "&7Use &f/warn -s <player> <reason> &7for silent warn");
            return true;
        }

        boolean silent = args[0].equalsIgnoreCase("-s");
        int startIndex = silent ? 1 : 0;

        if (args.length < startIndex + 2) {
            plugin.sendMessage(sender, "&cUsage: /warn <player> <reason>");
            return true;
        }

        String playerName = args[startIndex];
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = startIndex + 1; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        if (reason.isEmpty()) {
            reason = "No reason provided";
        }

        if (punishmentManager.addPunishment(playerName, null, null,
                PunishmentManager.PunishmentType.WARN, reason,
                sender.getName(), 0, silent)) {
            plugin.sendMessage(sender, "&c" + playerName + " was successfully warned!");

            if (!silent) {
                String message = "&c&l&n" + playerName + " &cgot warned by &l" + sender.getName() + " &cFor the reason " + reason;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("watchdog.staff")) {
                        plugin.sendMessage(p, message);
                    }
                }
            }

            Player target = Bukkit.getPlayer(playerName);
            if (target != null) {
                plugin.sendMessage(target,
                        "&cYou received a warning from this server!\n" +
                                "&7\n" +
                                "&7Reason: &f" + reason + "\n" +
                                "&7Find out more: &b&n" + plugin.getConfigManager().getAppealLink()
                );
            }
        } else {
            plugin.sendMessage(sender, "&cFailed to warn " + playerName);
        }

        return true;
    }

    private boolean handleKick(CommandSender sender, String[] args) {
        if (!sender.hasPermission("watchdog.kick")) {
            plugin.sendMessage(sender, "&cYou don't have permission for that!");
            return true;
        }

        if (args.length < 2) {
            plugin.sendMessage(sender, "&cUsage: /kick <player> <reason>");
            plugin.sendMessage(sender, "&7Use &f/kick -s <player> <reason> &7for silent kick");
            return true;
        }

        boolean silent = args[0].equalsIgnoreCase("-s");
        int startIndex = silent ? 1 : 0;

        if (args.length < startIndex + 2) {
            plugin.sendMessage(sender, "&cUsage: /kick <player> <reason>");
            return true;
        }

        String playerName = args[startIndex];
        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = startIndex + 1; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        if (reason.isEmpty()) {
            reason = "No reason provided";
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            plugin.sendMessage(sender, "&c" + playerName + " is not online!");
            return true;
        }

        if (punishmentManager.addPunishment(playerName, null, null,
                PunishmentManager.PunishmentType.KICK, reason,
                sender.getName(), 0, silent)) {
            plugin.sendMessage(sender, "&c" + playerName + " was successfully kicked!");

            if (!silent) {
                String message = "&c&l&n" + playerName + " &cgot kicked by &l" + sender.getName();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("watchdog.staff")) {
                        plugin.sendMessage(p, message);
                    }
                }
            }

            target.kickPlayer(ChatColor.translateAlternateColorCodes('&',
                    "&cYou got kicked from this server!\n" +
                            "&7\n" +
                            "&7Reason: &l" + reason + "\n" +
                            "&7"
            ));
        } else {
            plugin.sendMessage(sender, "&cFailed to kick " + playerName);
        }

        return true;
    }

    private boolean handleUnban(CommandSender sender, String[] args) {
        if (!sender.hasPermission("watchdog.unban")) {
            plugin.sendMessage(sender, "&cYou don't have permission for that!");
            return true;
        }

        if (args.length < 1) {
            plugin.sendMessage(sender, "&cUsage: /unban <player>");
            return true;
        }

        String target = args[0];

        if (punishmentManager.removePunishment(target, "BAN") ||
                punishmentManager.removePunishment(target, "TEMPBAN")) {
            plugin.sendMessage(sender, "&a" + target + " was successfully unbanned!");

            String message = "&e" + sender.getName() + " &7unbanned &c" + target;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("watchdog.staff")) {
                    plugin.sendMessage(p, message);
                }
            }
        } else {
            plugin.sendMessage(sender, "&c" + target + " is not banned!");
        }

        return true;
    }

    private boolean handleUnmute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("watchdog.unmute")) {
            plugin.sendMessage(sender, "&cYou don't have permission for that!");
            return true;
        }

        if (args.length < 1) {
            plugin.sendMessage(sender, "&cUsage: /unmute <player>");
            return true;
        }

        String target = args[0];

        if (punishmentManager.removePunishment(target, "MUTE") ||
                punishmentManager.removePunishment(target, "TEMPMUTE")) {
            plugin.sendMessage(sender, "&a" + target + " was successfully unmuted!");

            String message = "&e" + sender.getName() + " &7unmuted &c" + target;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("watchdog.staff")) {
                    plugin.sendMessage(p, message);
                }
            }
        } else {
            plugin.sendMessage(sender, "&c" + target + " is not muted!");
        }

        return true;
    }

    private boolean handleHistory(CommandSender sender, String[] args) {
        if (!sender.hasPermission("watchdog.history")) {
            plugin.sendMessage(sender, "&cYou don't have permission for that!");
            return true;
        }

        if (args.length < 1) {
            plugin.sendMessage(sender, "&cUsage: /history <player>");
            return true;
        }

        String target = args[0];
        plugin.sendMessage(sender, "&7History feature will be implemented in next update");
        return true;
    }
}