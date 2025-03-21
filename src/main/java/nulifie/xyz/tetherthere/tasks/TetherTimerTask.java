package nulifie.xyz.tetherthere.tasks;

import nulifie.xyz.tetherthere.util.ProgressBarUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;
import nulifie.xyz.tetherthere.models.TetherManager;

import java.util.UUID;

public class TetherTimerTask extends BukkitRunnable {
    private final Player tetherer;
    private final Player target;
    private final JavaPlugin plugin;
    private final int tetherEffectDurationTicks;
    private final UUID targetId;
    private final TetherManager tetherManager;
    private int remainingTicks;

    public TetherTimerTask(JavaPlugin plugin, Player tetherer, Player target, int tetherEffectDurationTicks, TetherManager tetherManager) {
        this.plugin = plugin;
        this.tetherer = tetherer;
        this.target = target;
        this.tetherEffectDurationTicks = tetherEffectDurationTicks;
        this.targetId = target.getUniqueId();
        this.tetherManager = tetherManager;
        this.remainingTicks = tetherEffectDurationTicks;
    }

    @Override
    public void run() {
        if (!target.isOnline() || !tetherer.isOnline()) {
            tetherManager.clearActionBars(target, tetherer);
            this.cancel();
            return;
        }

        remainingTicks--;

        String progress = ProgressBarUtil.createProgressBar(
                remainingTicks,
                tetherEffectDurationTicks,
                20,
                ChatColor.RED,
                ChatColor.DARK_GRAY
        );

        String actionBar = ChatColor.RED + "Зв'язаний: " + progress + " " + ChatColor.GRAY + (remainingTicks/20) + "с";
        target.sendActionBar(actionBar);
        tetherer.sendActionBar(actionBar);

        if (remainingTicks <= 0) {
            tetherManager.unbindPlayers(target, tetherer);
            tetherManager.clearActionBars(target, tetherer);
            this.cancel();
        }
    }

    public UUID getTargetId() {
        return targetId;
    }
} 