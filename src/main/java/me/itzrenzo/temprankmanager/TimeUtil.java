package me.itzrenzo.temprankmanager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {
    
    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([smhd]|mo)$", Pattern.CASE_INSENSITIVE);
    
    /**
     * Parse a time string like "10s", "5m", "2h", "7d", "3mo" into milliseconds
     * @param timeString The time string to parse
     * @return Milliseconds, or -1 if invalid format
     */
    public static long parseTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return -1;
        }
        
        Matcher matcher = TIME_PATTERN.matcher(timeString.toLowerCase());
        if (!matcher.matches()) {
            return -1;
        }
        
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        
        return switch (unit) {
            case "s" -> amount * 1000L; // seconds to milliseconds
            case "m" -> amount * 60L * 1000L; // minutes to milliseconds
            case "h" -> amount * 60L * 60L * 1000L; // hours to milliseconds
            case "d" -> amount * 24L * 60L * 60L * 1000L; // days to milliseconds
            case "mo" -> amount * 30L * 24L * 60L * 60L * 1000L; // months (30 days) to milliseconds
            default -> -1;
        };
    }
    
    /**
     * Check if a time string is valid
     * @param timeString The time string to validate
     * @return true if valid format
     */
    public static boolean isValidTime(String timeString) {
        return parseTime(timeString) > 0;
    }
    
    /**
     * Format milliseconds into a human-readable time string
     * @param milliseconds The time in milliseconds
     * @return Formatted time string
     */
    public static String formatTime(long milliseconds) {
        if (milliseconds <= 0) {
            return "Expired";
        }

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long months = days / 30;

        if (months > 0) {
            days = days % 30;
            if (days > 0) {
                return months + "mo " + days + "d";
            }
            return months + "mo";
        } else if (days > 0) {
            hours = hours % 24;
            if (hours > 0) {
                return days + "d " + hours + "h";
            }
            return days + "d";
        } else if (hours > 0) {
            minutes = minutes % 60;
            if (minutes > 0) {
                return hours + "h " + minutes + "m";
            }
            return hours + "h";
        } else if (minutes > 0) {
            seconds = seconds % 60;
            if (seconds > 0) {
                return minutes + "m " + seconds + "s";
            }
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }
    
    /**
     * Get example time formats for help/tab completion
     * @return Array of example time formats
     */
    public static String[] getExampleTimes() {
        return new String[]{"30s", "5m", "1h", "7d", "1mo", "30d", "12h", "90m"};
    }
}