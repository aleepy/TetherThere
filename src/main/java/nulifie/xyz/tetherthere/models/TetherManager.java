package nulifie.xyz.tetherthere.models;

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

import nulifie.xyz.tetherthere.Main;
import nulifie.xyz.tetherthere.tasks.*;
import nulifie.xyz.tetherthere.util.*;

import java.util.HashMap;
import java.util.UUID;
import java.util.Map;

public class TetherManager {

    private final JavaPlugin plugin;
    private final HashMap<UUID, UUID> tetheredPlayers = new HashMap<>();
    private final HashMap<UUID, Long> lastBindAttemptTime = new HashMap<>();
    private final HashMap<UUID, Long> lastUnbindAttemptTime = new HashMap<>();
    private final HashMap<UUID, BukkitRunnable> bindingTasks = new HashMap<>();
    private final HashMap<UUID, BukkitRunnable> tetherTimers = new HashMap<>();
    private final HashMap<UUID, BukkitRunnable> tetherPullTasks = new HashMap<>();

    private int tetherChance;
    private long tetherCooldownMillis;
    private double maxTetherDistance;
    private int tetherDurationTicks;
    private double tetherPullDistance;
    private double tetherPullSpeed;
    private int unbindChance;
    private int tetherEffectDurationTicks;
    private double bindingMaxDistance;
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

        UnbindingTask unbindingTask = new UnbindingTask(plugin, player, target, tetherer, 
            plugin.getConfig().getInt("unbind_duration_seconds") * 20, maxTetherDistance, this);

        bindingTasks.put(player.getUniqueId(), unbindingTask);
        unbindingTask.runTaskTimer(plugin, 0, 1);
    }

    public void startBindingProcess(Player player, Player target) {
        if (isPlayerTethered(player)) {
            player.sendMessage(ChatColor.RED + "Ви вже зв'язані з кимось.");
            return;
        }

        if (isPlayerTethered(target)) {
            player.sendMessage(ChatColor.RED + "Цей гравець вже зв'язаний з кимось.");
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastAttempt = getLastBindAttemptTime(player.getUniqueId());
        if (currentTime - lastAttempt < getTetherCooldownMillis()) {
            player.sendMessage(ChatColor.RED + "Почекайте трохи, перш ніж спробувати знову.");
            return;
        }

        setLastBindAttemptTime(player.getUniqueId(), currentTime);

        player.sendMessage(ChatColor.YELLOW + "Зв'язування... Тримайтеся поруч протягом " + (tetherDurationTicks / 20) + " секунд.");
        showBindingTitle(player, target);

        BindingTask bindingTask = new BindingTask(plugin, player, target, tetherDurationTicks, maxTetherDistance, this);
        bindingTasks.put(player.getUniqueId(), bindingTask);
        bindingTask.runTaskTimer(plugin, 0, 1);
    }

    public void clearActionBars(Player... players) {
        for (Player p : players) {
            if (p != null && p.isOnline()) {
                p.sendActionBar("");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (p.isOnline()) {
                            p.sendActionBar("");
                        }
                    }
                }.runTaskLater(plugin, 1L);
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

        ConfigurationSection debuffsSection = plugin.getConfig().getConfigurationSection("debuffs");
        debuffEffects.clear();
        debuffEffects.putAll(ConfigUtil.loadDebuffEffects(debuffsSection));
    }

    public long getLastBindAttemptTime(UUID playerId) {
        return lastBindAttemptTime.getOrDefault(playerId, 0L);
    }

    public void setLastBindAttemptTime(UUID playerId, long time) {
        lastBindAttemptTime.put(playerId, time);
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
        lastBindAttemptTime.clear();
        lastUnbindAttemptTime.clear();
        bindingTasks.forEach((uuid, task) -> task.cancel());
        bindingTasks.clear();
        tetherTimers.forEach((uuid, task) -> task.cancel());
        tetherTimers.clear();
        tetherPullTasks.forEach((uuid, task) -> task.cancel());
        tetherPullTasks.clear();
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
        EffectUtil.applyDebuffs(target, debuffEffects);
        startTetherTimer(tetherer, target);
        startTetherPullTask(tetherer, target);
        showTetherTitle(tetherer, target);
        logTetherAction(tetherer, target);
        createLeashEffect(tetherer, target);
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

    public void unbindPlayers(Player target, Player tetherer) {
        // Спочатку зупиняємо всі таски
        stopTetherTimer(target);
        stopTetherPullTask(target);
        
        // Потім видаляємо з мапи прив'язаних гравців
        tetheredPlayers.remove(target.getUniqueId());
        
        // Видаляємо ефекти
        EffectUtil.removeDebuffs(target, debuffEffects);
        
        // Показуємо повідомлення та оновлюємо статистику
        showUnbindTitle(tetherer, target);
        logUnbindAction(tetherer, target);
        
        // Відновлюємо колізію
        target.setCollidable(true);
        
        // Очищаємо action bar
        clearActionBars(target, tetherer);
        
        // Додатково перевіряємо та очищаємо всі таски
        BukkitRunnable timerTask = tetherTimers.remove(target.getUniqueId());
        if (timerTask != null) {
            timerTask.cancel();
        }
        
        BukkitRunnable pullTask = tetherPullTasks.remove(target.getUniqueId());
        if (pullTask != null) {
            pullTask.cancel();
        }
    }

    private void stopTetherTimer(Player target) {
        BukkitRunnable timer = tetherTimers.remove(target.getUniqueId());
        if (timer != null) {
            timer.cancel();
        }
        // Додатково очищаємо action bar
        if (target.isOnline()) {
            clearActionBars(target);
        }
    }

    private void stopTetherPullTask(Player target) {
        BukkitRunnable pullTask = tetherPullTasks.remove(target.getUniqueId());
        if (pullTask != null) {
            pullTask.cancel();
        }
    }

    private void startTetherTimer(Player tetherer, Player target) {
        TetherTimerTask timerTask = new TetherTimerTask(plugin, tetherer, target, tetherEffectDurationTicks, this);
        tetherTimers.put(target.getUniqueId(), timerTask);
        timerTask.runTaskTimer(plugin, 0, 1);
    }

    private void startTetherPullTask(Player tetherer, Player target) {
        TetherPullTask pullTask = new TetherPullTask(plugin, tetherer, target, 
            tetherPullDistance, tetherPullSpeed, bindingMaxDistance);
        tetherPullTasks.put(target.getUniqueId(), pullTask);
        pullTask.runTaskTimer(plugin, 0, 1);
    }

    private void showTetherTitle(Player tetherer, Player target) {
        target.sendTitle(ChatColor.RED + "Вас Зв'язали", "", 10, 40, 10);
    }

    private void showUnbindTitle(Player tetherer, Player target) {
        tetherer.sendTitle(ChatColor.YELLOW + "Мотузка розірвана.", "", 10, 40, 10);
    }

    private void logTetherAction(Player tetherer, Player target) {
        if (Main.getActionLogAPI() != null) {
            updatePlayerDanger(tetherer, 50);
            updatePlayerDanger(target, 50);
        }
    }

    private void logUnbindAction(Player tetherer, Player target) {
        if (Main.getActionLogAPI() != null) {
            updatePlayerDanger(tetherer, -50);
            updatePlayerDanger(target, -50);
        }
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

    public Map<PotionEffectType, PotionEffect> getDebuffEffects() {
        return debuffEffects;
    }
}