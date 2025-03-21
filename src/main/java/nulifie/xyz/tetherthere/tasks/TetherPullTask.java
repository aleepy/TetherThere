package nulifie.xyz.tetherthere.tasks;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.UUID;

public class TetherPullTask extends BukkitRunnable {
    private final Player tetherer;
    private final Player target;
    private final JavaPlugin plugin;
    private final double tetherPullDistance;
    private final double tetherPullSpeed;
    private final double bindingMaxDistance;
    private final UUID targetId;
    private int soundTicks = 0;

    public TetherPullTask(JavaPlugin plugin, Player tetherer, Player target, double tetherPullDistance, double tetherPullSpeed, double bindingMaxDistance) {
        this.plugin = plugin;
        this.tetherer = tetherer;
        this.target = target;
        this.tetherPullDistance = tetherPullDistance;
        this.tetherPullSpeed = tetherPullSpeed;
        this.bindingMaxDistance = bindingMaxDistance;
        this.targetId = target.getUniqueId();
    }

    @Override
    public void run() {
        if (!target.isOnline() || !tetherer.isOnline()) {
            this.cancel();
            return;
        }

        double distance = target.getLocation().distance(tetherer.getLocation());
        if (distance > bindingMaxDistance && distance <= tetherPullDistance) {
            Vector direction = tetherer.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
            target.setVelocity(direction.multiply(tetherPullSpeed));
            
            // Відтворюємо звук кожні 20 тіків (1 секунда)
            if (soundTicks >= 20) {
                target.playSound(target.getLocation(), Sound.ENTITY_LEASH_KNOT_PLACE, 1.0f, 1.0f);
                soundTicks = 0;
            }
            soundTicks++;
        } else if (distance > tetherPullDistance) {
            target.teleport(tetherer.getLocation());
            target.sendMessage(org.bukkit.ChatColor.YELLOW + "Вас витягли назад до " + tetherer.getName() + " мотузкою!");
            target.playSound(target.getLocation(), Sound.ENTITY_LEASH_KNOT_PLACE, 1.0f, 1.0f);
        }
    }

    public UUID getTargetId() {
        return targetId;
    }
} 