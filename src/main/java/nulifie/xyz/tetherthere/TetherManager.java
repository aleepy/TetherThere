package nulifie.xyz.tetherthere;

import hetmanplugins.actionLog.models.DangerLevel;
import static hetmanplugins.actionLog.util.ActionLogManager.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
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
    private final HashMap<UUID, BukkitRunnable> tetherTimers = new HashMap<>();

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

    private String createProgressBar(int current, int max, int bars, ChatColor filledColor, ChatColor emptyColor) {
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

    public void startUnbindingProcess(Player player, Player target) {
        Player tetherer = getTetherer(target);
        if (tetherer == null) {
            player.sendMessage(ChatColor.RED + "Цей гравець не прив'язаний.");
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastUnbindAttempt = getLastUnbindAttemptTime(player.getUniqueId());
        if (currentTime - lastUnbindAttempt < getUnbindFailCooldownMillis()) {
            player.sendMessage(ChatColor.RED + "Почекайте трохи, перш ніж спробувати знову.");
            return;
        }

        BukkitRunnable unbindingTask = new BukkitRunnable() {
            int ticks = 0;
            final int unbindDurationTicks = plugin.getConfig().getInt("unbind_duration_seconds") * 20;

            @Override
            public void run() {
                if (player.isOnline() && target.isOnline() && player.getLocation().distance(target.getLocation()) <= maxTetherDistance) {
                    ticks++;

                    String progress = createProgressBar(
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
                        unbindPlayers(target, tetherer);
                        player.sendMessage(ChatColor.GREEN + "Ви успішно розв'язали " + target.getName() + ".");
                        target.sendMessage(ChatColor.GREEN + "Вас розв'язав " + player.getName() + ".");
                        bindingTasks.remove(player.getUniqueId());
                        setLastUnbindAttemptTime(player.getUniqueId(), System.currentTimeMillis());
                        clearActionBars(player, target);
                        this.cancel();
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Не вдалось розв'язати. Ви занадто далеко.");
                    bindingTasks.remove(player.getUniqueId());
                    setLastUnbindAttemptTime(player.getUniqueId(), System.currentTimeMillis());
                    clearActionBars(player, target);
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
        showBindingTitle(player, target);

        BukkitRunnable bindingTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (player.isOnline() && target.isOnline() && player.getLocation().distance(target.getLocation()) <= maxTetherDistance) {
                    ticks++;

                    String progress = createProgressBar(
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
                        tetherPlayers(player, target);
                        bindingTasks.remove(player.getUniqueId());
                        clearActionBars(player, target);
                        this.cancel();
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Не вдалось зв'язати.");
                    bindingTasks.remove(player.getUniqueId());
                    clearActionBars(player, target);
                    this.cancel();
                }
            }
        };

        bindingTasks.put(player.getUniqueId(), bindingTask);
        bindingTask.runTaskTimer(plugin, 0, 1);
    }

    private void clearActionBars(Player... players) {
        for (Player p : players) {
            if (p != null && p.isOnline()) {
                p.sendActionBar("");
            }
        }
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
        unbindFailCooldownMillis = plugin.getConfig().getInt("unbind_fail_cooldown_minutes") * 60 * 1000;
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
        tetherTimers.forEach((uuid, task) -> task.cancel());
        tetherTimers.clear();
        lastUnbindAttemptTime.clear();
        clearActionBars(Bukkit.getOnlinePlayers().toArray(new Player[0]));
    }

    public boolean isPlayerTethered(Player player) {
        return tetheredPlayers.containsKey(player.getUniqueId());
    }

    public void showBindingTitle(Player player, Player target) {
        target.sendTitle(ChatColor.RED + "Зв'язування...", ChatColor.LIGHT_PURPLE + "Вас зв'язує " + player.getName(), 10, 70, 20);
    }

    public void tetherPlayers(Player tetherer, Player target) {
        tetheredPlayers.put(target.getUniqueId(), tetherer.getUniqueId());
        tetherer.sendMessage(ChatColor.GREEN + "Ви успішно зв'язали " + target.getName() + ".");
        target.sendMessage(ChatColor.RED + "Ви були зв'язані " + tetherer.getName() + ".");
        target.sendTitle(ChatColor.RED + "Вас Зв'язали", "", 10, 40, 10);

        // Оновлення рівня небезпеки
        updateDangerLevels(tetherer, target);

        applyDebuffs(target);
        createLeashEffect(tetherer, target);

        BukkitRunnable timerTask = new BukkitRunnable() {
            int remainingTicks = tetherEffectDurationTicks;

            @Override
            public void run() {
                if (!tetheredPlayers.containsKey(target.getUniqueId())) {
                    this.cancel();
                    return;
                }

                remainingTicks--;

                String progress = createProgressBar(
                        remainingTicks,
                        tetherEffectDurationTicks,
                        20,
                        ChatColor.LIGHT_PURPLE,
                        ChatColor.DARK_GRAY
                );

                String actionBar = ChatColor.LIGHT_PURPLE + "Зв'язаний: " + progress + " " + ChatColor.GRAY + (remainingTicks/20) + "с";
                tetherer.sendActionBar(actionBar);
                target.sendActionBar(actionBar);

                if (remainingTicks <= 0) {
                    this.cancel();
                }
            }
        };

        timerTask.runTaskTimer(plugin, 0, 1);
        tetherTimers.put(target.getUniqueId(), timerTask);

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
                    Player tethererOnline = Bukkit.getPlayer(tetheredPlayers.get(target.getUniqueId()));
                    if (tethererOnline != null && tethererOnline.isOnline()) {
                        double distance = target.getLocation().distance(tethererOnline.getLocation());
                        if (distance > bindingMaxDistance && distance <= tetherPullDistance) {
                            Vector direction = tethererOnline.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
                            target.setVelocity(direction.multiply(tetherPullSpeed));
                            target.playSound(target.getLocation(), Sound.ENTITY_LEASH_KNOT_PLACE, 1.0f, 1.0f);
                        } else if (distance > tetherPullDistance) {
                            target.teleport(tethererOnline.getLocation());
                            target.sendMessage(ChatColor.YELLOW + "Вас витягли назад до " + tethererOnline.getName() + " мотузкою!");
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
        Player[] players = {player1, player2};

        for (Player p : players) {
            if (tetheredPlayers.containsKey(p.getUniqueId())) {
                BukkitRunnable timer = tetherTimers.remove(p.getUniqueId());
                if (timer != null) timer.cancel();
                tetheredPlayers.remove(p.getUniqueId());
                removeDebuffs(p);
                p.sendActionBar("");


                if (Main.getActionLogAPI() != null) {
                    updatePlayerDanger(p, -50);
                }
            }
        }

        clearActionBars(player1, player2);
        lastUnbindAttemptTime.put(player1.getUniqueId(), System.currentTimeMillis());
    }

    private void updateDangerLevels(Player tetherer, Player target) {
        if (Main.getActionLogAPI() != null) {
            int dangerIncrease = plugin.getConfig().getInt("danger_level_increase", 10);
            updatePlayerDanger(tetherer, dangerIncrease);
            updatePlayerDanger(target, dangerIncrease);

            if (plugin.getConfig().getBoolean("show_danger_messages", true)) {
            }
        }
    }

    private void updatePlayerDanger(Player player, int amount) {
        try {
            DangerLevel dangerLevel = getDangerLevel(player.getName());
            if (dangerLevel == null) {
                dangerLevel = new DangerLevel(player);
                setDangerLevel(player.getName(), dangerLevel);
            }
            dangerLevel.updateDanger(amount);

            if (dangerLevel.getDanger() > 100) {
                dangerLevel.setDanger(100);
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Помилка оновлення небезпеки для " + player.getName() + ": " + e.getMessage());
        }
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