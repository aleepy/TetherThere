package nulifie.xyz.tetherthere;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class TetherManager {

    private final JavaPlugin plugin;
    private final HashMap<UUID, UUID> tetheredPlayers = new HashMap<>();
    private final HashMap<UUID, Long> lastAttemptTime = new HashMap<>();
    private final HashMap<UUID, BukkitRunnable> bindingTasks = new HashMap<>();
    private final HashMap<UUID, BossBar> bindingBars = new HashMap<>();

    private int tetherChance;
    private long tetherCooldownMillis;
    private double maxTetherDistance;
    private int tetherDurationTicks;
    private double tetherPullDistance;
    private double tetherPullSpeed;
    private int unbindChance;
    private int tetherEffectDurationTicks;
    private double bindingMaxDistance;
    private final HashMap<UUID, Long> lastUnbindAttemptTime = new HashMap<>();
    private int unbindFailCooldownMillis;

    private final HashMap<PotionEffectType, PotionEffect> debuffEffects = new HashMap<>();

    public TetherManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfigValues();
    }

    public void startUnbindingProcess(Player player, Player target) {
        if (isBindingInProgress(player)) {
            player.sendMessage(ChatColor.RED + "Ви вже розв'язуєте когось. Зачекайте завершення.");
            return;
        }
        long currentTime = System.currentTimeMillis();
        long lastUnbindAttempt = getLastUnbindAttemptTime(player.getUniqueId());
        if (currentTime - lastUnbindAttempt < getUnbindFailCooldownMillis()) {
            player.sendMessage(ChatColor.RED + "Почекайте трохи, перш ніж спробувати знову.");
            return;
        }
        BossBar bossBar = Bukkit.createBossBar("Розв'язування...", BarColor.BLUE, BarStyle.SOLID);
        bossBar.setProgress(0);
        bossBar.addPlayer(player);
        bossBar.addPlayer(target);
        bindingBars.put(player.getUniqueId(), bossBar);
        BukkitRunnable unbindingTask = new BukkitRunnable() {
            int ticks = 0;
            final int unbindDurationTicks = plugin.getConfig().getInt("unbind_duration_seconds") * 20;
            @Override
            public void run() {
                if (player.isOnline() && target.isOnline() && player.getLocation().distance(target.getLocation()) <= maxTetherDistance) {
                    ticks++;
                    bossBar.setProgress(ticks / (double) unbindDurationTicks);
                    if (ticks >= unbindDurationTicks) {
                        unbindPlayers(player, target);
                        player.sendMessage(ChatColor.GREEN + "Ви успішно розв'язали " + target.getName() + ".");
                        target.sendMessage(ChatColor.GREEN + "Вас розв'язав " + player.getName() + ".");
                        bindingTasks.remove(player.getUniqueId());
                        bindingBars.remove(player.getUniqueId()).removeAll();
                        setLastUnbindAttemptTime(player.getUniqueId(), System.currentTimeMillis());
                        this.cancel();
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Не вдалось розв'язати. Ви занадто далеко.");
                    bindingTasks.remove(player.getUniqueId());
                    bindingBars.remove(player.getUniqueId()).removeAll();
                    // Устанавливаем кулдаун при неудачной попытке
                    setLastUnbindAttemptTime(player.getUniqueId(), System.currentTimeMillis());
                    this.cancel();
                }
            }
        };
        bindingTasks.put(player.getUniqueId(), unbindingTask);
        unbindingTask.runTaskTimer(plugin, 0, 1);
    }
    public void startBindingProcess(Player player, Player target) {
        lastAttemptTime.put(player.getUniqueId(), System.currentTimeMillis());

        player.sendMessage(ChatColor.YELLOW + "Зв'язування... Тримайтеся поруч протягом " + (tetherDurationTicks / 20) + " секунд.");
        BossBar bossBar = Bukkit.createBossBar("Зв'язування...", BarColor.RED, BarStyle.SOLID);
        bossBar.setProgress(0);
        bossBar.addPlayer(player);
        bossBar.addPlayer(target);
        bindingBars.put(player.getUniqueId(), bossBar);

        showBindingTitle(player, target);

        BukkitRunnable bindingTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (player.isOnline() && target.isOnline() && player.getLocation().distance(target.getLocation()) <= maxTetherDistance) {
                    ticks++;
                    bossBar.setProgress(ticks / (double) tetherDurationTicks);
                    if (ticks >= tetherDurationTicks) {
                        tetherPlayers(player, target);
                        bindingTasks.remove(player.getUniqueId());
                        bindingBars.remove(player.getUniqueId()).removeAll();
                        this.cancel();
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Не вдалось зв'язати.");
                    bindingTasks.remove(player.getUniqueId());
                    bindingBars.remove(player.getUniqueId()).removeAll();
                    this.cancel();
                }
            }
        };
        bindingTasks.put(player.getUniqueId(), bindingTask);
        bindingTask.runTaskTimer(plugin, 0, 1);
    }

    public void loadConfigValues() {
        tetherChance = plugin.getConfig().getInt("tether_chance");
        tetherCooldownMillis = plugin.getConfig().getInt("tether_cooldown_minutes") * 60 * 1000L;
        maxTetherDistance = plugin.getConfig().getDouble("tether_max_distance");
        tetherDurationTicks = plugin.getConfig().getInt("tether_duration_seconds") * 20;
        tetherEffectDurationTicks = plugin.getConfig().getInt("tether_effect_duration_seconds") * 20;
        tetherPullDistance = plugin.getConfig().getDouble("tether_pull_distance");
        tetherPullSpeed = plugin.getConfig().getDouble("tether_pull_speed");
        unbindChance = plugin.getConfig().getInt("unbind_chance");
        unbindFailCooldownMillis = plugin.getConfig().getInt("unbind_fail_cooldown_minutes") * 60 * 1000; // Минуты в миллисекунды
        bindingMaxDistance = plugin.getConfig().getDouble("binding_max_distance");

        debuffEffects.clear();
        ConfigurationSection debuffsSection = plugin.getConfig().getConfigurationSection("debuffs");
        if (debuffsSection != null) {
            for (String key : debuffsSection.getKeys(false)) {
                ConfigurationSection effectSection = debuffsSection.getConfigurationSection(key);
                if (effectSection != null) {
                    PotionEffectType effectType = PotionEffectType.getByName(key.toUpperCase());
                    if (effectType != null) {
                        int duration = effectSection.getInt("duration_seconds") * 20;
                        int level = effectSection.getInt("level");
                        debuffEffects.put(effectType, new PotionEffect(effectType, duration, level));
                    } else {
                        plugin.getLogger().warning("Invalid potion effect type in config: " + key);
                    }
                }
            }
        }
    }

    public long getLastUnbindAttemptTime(UUID playerId) {
        return lastUnbindAttemptTime.getOrDefault(playerId, 0L);
    }

    public void setLastUnbindAttemptTime(UUID playerId, long time) {
        lastUnbindAttemptTime.put(playerId, time);
    }

    public int getUnbindFailCooldownMillis() {
        return unbindFailCooldownMillis;
    }

    public boolean isBindingInProgress(Player player) {
        return bindingTasks.containsKey(player.getUniqueId());
    }

    public long getLastAttemptTime(UUID playerId) {
        return lastAttemptTime.getOrDefault(playerId, 0L);
    }

    public int getTetherChance() {
        return tetherChance;
    }

    public int getUnbindChance() {
        return unbindChance;
    }

    public double getMaxTetherDistance() {
        return maxTetherDistance;
    }

    public long getTetherCooldownMillis() {
        return tetherCooldownMillis;
    }

    public Player getTetherer(Player player) {
        UUID tethererId = tetheredPlayers.get(player.getUniqueId());
        return tethererId != null ? Bukkit.getPlayer(tethererId) : null;
    }

    public void clearAll() {
        lastAttemptTime.clear();
        bindingTasks.forEach((uuid, task) -> task.cancel());
        bindingTasks.clear();
        bindingBars.forEach((uuid, bar) -> bar.removeAll());
        bindingBars.clear();
        lastUnbindAttemptTime.clear();
    }

    public boolean isPlayerTethered(Player player) {
        return tetheredPlayers.containsKey(player.getUniqueId());
    }
    public void showBindingTitle(Player player, Player target) {
        target.sendTitle(ChatColor.RED + "Зв'язування...", ChatColor.LIGHT_PURPLE + "Вас Зв'язує " + player.getName(), 10, 70, 20);
    }
    public void tetherPlayers(Player tetherer, Player target) {
        tetheredPlayers.put(target.getUniqueId(), tetherer.getUniqueId());
        tetherer.sendMessage(ChatColor.GREEN + "Ви успішно зв'язали " + target.getName() + ".");
        target.sendMessage(ChatColor.RED + "Ви були зв'язані " + tetherer.getName() + ".");


        target.sendTitle(ChatColor.RED + "Вас Зв'язали", "", 10, 40, 10);

        applyDebuffs(target);
        createLeashEffect(tetherer, target);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (tetheredPlayers.containsKey(target.getUniqueId())) {
                    unbindPlayers(tetherer, target);
                    target.sendMessage(ChatColor.YELLOW + "Мотузка розірвана.");
                    tetherer.sendMessage(ChatColor.YELLOW + "Мотузка розірвана.");
                }
            }
        }.runTaskLater(plugin, tetherEffectDurationTicks);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (tetheredPlayers.containsKey(target.getUniqueId())) {
                    Player tetherer = Bukkit.getPlayer(tetheredPlayers.get(target.getUniqueId()));
                    if (tetherer != null && tetherer.isOnline()) {
                        double distance = target.getLocation().distance(tetherer.getLocation());
                        if (distance > bindingMaxDistance && distance <= tetherPullDistance) {
                            Vector direction = tetherer.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
                            target.setVelocity(direction.multiply(tetherPullSpeed));
                            target.playSound(target.getLocation(), Sound.ENTITY_LEASH_KNOT_PLACE, 1.0f, 1.0f);
                        } else if (distance > tetherPullDistance) {
                            target.teleport(tetherer.getLocation());
                            target.sendMessage(ChatColor.YELLOW + "Вас витягли назад до " + tetherer.getName() + " мотузкою!");
                        }
                    } else {
                        this.cancel();
                    }
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 10);
    }

    public void unbindPlayers(Player player1, Player player2) {
        if (tetheredPlayers.containsKey(player1.getUniqueId()) && tetheredPlayers.get(player1.getUniqueId()).equals(player2.getUniqueId())) {
            tetheredPlayers.remove(player1.getUniqueId());
            removeDebuffs(player1);
            removeDebuffs(player2);
        } else if (tetheredPlayers.containsKey(player2.getUniqueId()) && tetheredPlayers.get(player2.getUniqueId()).equals(player1.getUniqueId())) {
            tetheredPlayers.remove(player2.getUniqueId());
            removeDebuffs(player1);
            removeDebuffs(player2);
        }

        lastUnbindAttemptTime.put(player1.getUniqueId(), System.currentTimeMillis());
    }

    private void applyDebuffs(Player player) {
        for (PotionEffect effect : debuffEffects.values()) {
            player.addPotionEffect(effect);
        }
        player.setCollidable(false);
    }

    public void removeDebuffs(Player player) {
        for (PotionEffectType effectType : debuffEffects.keySet()) {
            player.removePotionEffect(effectType);
        }
        player.setCollidable(true);
    }

    private void createLeashEffect(Player tetherer, Player target) {
        Bat tetherBat = tetherer.getWorld().spawn(tetherer.getLocation(), Bat.class, bat -> {
            bat.setInvisible(true);
            bat.setInvulnerable(true);
            bat.setSilent(true);
            bat.setPersistent(true);
            bat.setAI(false);
        });

        tetherBat.setLeashHolder(tetherer);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (tetheredPlayers.containsKey(target.getUniqueId()) && tetherBat.isValid()) {
                    if (target.isOnline() && tetherer.isOnline()) {
                        tetherBat.teleport(target.getLocation());
                    }
                } else {
                    if (tetherBat.isValid()) {
                        tetherBat.setLeashHolder(null);
                        tetherBat.remove();
                    }
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 5);
    }
}