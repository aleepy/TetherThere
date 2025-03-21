package nulifie.xyz.tetherthere.util;

import org.bukkit.ChatColor;

public class ProgressBarUtil {
    public static String createProgressBar(int current, int max, int bars, ChatColor filledColor, ChatColor emptyColor) {
        float percent = (float) current / max;
        int progressBars = Math.round(bars * percent);

        StringBuilder sb = new StringBuilder();
        sb.append(ChatColor.GRAY).append("[");
        sb.append(filledColor);
        for (int i = 0; i < progressBars; i++) {
            sb.append("|");
        }
        sb.append(emptyColor);
        for (int i = progressBars; i < bars; i++) {
            sb.append("|");
        }
        sb.append(ChatColor.GRAY).append("]");
        return sb.toString();
    }
} 