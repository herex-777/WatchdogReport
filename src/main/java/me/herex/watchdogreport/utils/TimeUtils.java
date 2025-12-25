package me.herex.watchdogreport.utils;

public class TimeUtils {

    public static String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long months = days / 30;

        if (months > 0) {
            return String.format("%d month(s) %d day(s)", months, days % 30);
        } else if (days > 0) {
            return String.format("%d day(s) %d hour(s)", days, hours % 24);
        } else if (hours > 0) {
            return String.format("%d hour(s) %d minute(s)", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d minute(s) %d second(s)", minutes, seconds % 60);
        } else {
            return String.format("%d second(s)", seconds);
        }
    }

    public static long parseDuration(String durationStr) {
        try {
            if (durationStr.startsWith("#")) {
                return parseTimeLayout(durationStr.substring(1));
            }

            long total = 0;
            String[] parts = durationStr.split("(?<=[smhd])");
            for (String part : parts) {
                if (part.length() < 2) continue;

                char unit = part.charAt(part.length() - 1);
                long value = Long.parseLong(part.substring(0, part.length() - 1));

                switch (unit) {
                    case 's': total += value * 1000; break;
                    case 'm': total += value * 60 * 1000; break;
                    case 'h': total += value * 60 * 60 * 1000; break;
                    case 'd': total += value * 24 * 60 * 60 * 1000; break;
                    case 'o': // months
                        total += value * 30L * 24 * 60 * 60 * 1000;
                        break;
                }
            }
            return total;
        } catch (Exception e) {
            return -1;
        }
    }

    private static long parseTimeLayout(String layout) {
        long total = 0;
        if (layout.contains("d")) {
            String[] daysPart = layout.split("d");
            if (daysPart.length > 0) {
                total += Long.parseLong(daysPart[0]) * 24 * 60 * 60 * 1000;
            }
        }
        return total;
    }
}