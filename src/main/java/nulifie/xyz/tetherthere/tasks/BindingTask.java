package nulifie.xyz.tetherthere.tasks;

import nulifie.xyz.tetherthere.util.ProgressBarUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;
import nulifie.xyz.tetherthere.models.TetherManager;

public class BindingTask extends BukkitRunnable {
    private final Player player;
    private final Player target;
    private final JavaPlugin plugin;
    private final int tetherDurationTicks;
    private final double maxTetherDistance;
    private final TetherManager tetherManager;
    private int ticks;

    public BindingTask(JavaPlugin plugin, Player player, Player target, int tetherDurationTicks, double maxTetherDistance, TetherManager tetherManager) {
        this.plugin = plugin;
        this.player = player;
        this.target = target;
        this.tetherDurationTicks = tetherDurationTicks;
        this.maxTetherDistance = maxTetherDistance;
        this.tetherManager = tetherManager;
        this.ticks = 0;
    }

    @Override
    public void run() {
        if (player.isOnline() && target.isOnline() && player.getLocation().distance(target.getLocation()) <= maxTetherDistance) {
            ticks++;

            String progress = ProgressBarUtil.createProgressBar(
                    ticks,
                    tetherDurationTicks,
                    20,
                    ChatColor.RED,
                    ChatColor.DARK_GRAY
            );

            String actionBar = ChatColor.GOLD + "Зв'язування: " + progress + " " + ChatColor.GRAY + (tetherDurationTicks/20 - ticks/20) + "с";
            player.sendActionBar(actionBar);
            target.sendActionBar(actionBar);

            if (ticks >= tetherDurationTicks) {
                tetherManager.tetherPlayers(player, target);
                player.sendMessage(ChatColor.GREEN + "Ви успішно зв'язали " + target.getName() + ".");
                target.sendMessage(ChatColor.RED + "Ви були зв'язані " + player.getName() + ".");
                tetherManager.clearActionBars(player, target);
                this.cancel();
            }
        } else {
            player.sendMessage(ChatColor.RED + "Не вдалось зв'язати.");
            tetherManager.clearActionBars(player, target);
            this.cancel();
        }
    }

    public boolean isComplete() {
        return ticks >= tetherDurationTicks;
    }
} 