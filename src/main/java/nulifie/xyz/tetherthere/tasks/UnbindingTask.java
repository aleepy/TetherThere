package nulifie.xyz.tetherthere.tasks;

import nulifie.xyz.tetherthere.util.ProgressBarUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;
import nulifie.xyz.tetherthere.models.TetherManager;

public class UnbindingTask extends BukkitRunnable {
    private final Player player;
    private final Player target;
    private final Player tetherer;
    private final JavaPlugin plugin;
    private final int unbindDurationTicks;
    private final double maxTetherDistance;
    private final TetherManager tetherManager;
    private int ticks;

    public UnbindingTask(JavaPlugin plugin, Player player, Player target, Player tetherer, int unbindDurationTicks, double maxTetherDistance, TetherManager tetherManager) {
        this.plugin = plugin;
        this.player = player;
        this.target = target;
        this.tetherer = tetherer;
        this.unbindDurationTicks = unbindDurationTicks;
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
                    unbindDurationTicks,
                    20,
                    ChatColor.BLUE,
                    ChatColor.DARK_GRAY
            );

            String actionBar = ChatColor.AQUA + "Розв'язування: " + progress + " " + ChatColor.GRAY + (unbindDurationTicks/20 - ticks/20) + "с";
            player.sendActionBar(actionBar);
            target.sendActionBar(actionBar);

            if (ticks >= unbindDurationTicks) {
                tetherManager.unbindPlayers(target, tetherer);
                player.sendMessage(ChatColor.GREEN + "Ви успішно розв'язали " + target.getName() + ".");
                target.sendMessage(ChatColor.GREEN + "Вас розв'язав " + player.getName() + ".");
                tetherManager.clearActionBars(player, target);
                tetherManager.setLastUnbindAttemptTime(player.getUniqueId(), System.currentTimeMillis());
                this.cancel();
            }
        } else {
            player.sendMessage(ChatColor.RED + "Не вдалось розв'язати. Ви занадто далеко.");
            tetherManager.clearActionBars(player, target);
            tetherManager.setLastUnbindAttemptTime(player.getUniqueId(), System.currentTimeMillis());
            this.cancel();
        }
    }

    public boolean isComplete() {
        return ticks >= unbindDurationTicks;
    }
} 