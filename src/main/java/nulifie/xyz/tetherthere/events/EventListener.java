package nulifie.xyz.tetherthere.events;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import nulifie.xyz.tetherthere.models.TetherManager;
import nulifie.xyz.tetherthere.util.EffectUtil;

public class EventListener implements Listener {

    private final TetherManager tetherManager;


    public EventListener(TetherManager tetherManager) {
        this.tetherManager = tetherManager;
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getRightClicked() instanceof Player) {
            Player player = event.getPlayer();
            Player target = (Player) event.getRightClicked();

            if (player.isSneaking() && player.getInventory().getItemInMainHand().getType() == Material.LEAD) {
                event.setCancelled(true);

                if (tetherManager.isPlayerTethered(player)) {
                    player.sendMessage(ChatColor.RED + "Ви вже зв'язані.");
                    return;
                }

                if (tetherManager.isPlayerTethered(target)) {
                    player.sendMessage(ChatColor.RED + "Цей гравець вже зв'язаний!");
                    return;
                }

                if (tetherManager.hasActiveBindingProcess(player) || tetherManager.hasActiveBindingProcess(target)) {
                    player.sendMessage(ChatColor.RED + "Зачекайте, доки завершиться поточний процес зв'язування.");
                    return;
                }

                if (player.getLocation().distance(target.getLocation()) > tetherManager.getMaxTetherDistance()) {
                    player.sendMessage(ChatColor.RED + "Занадто далеко, підійдіть ближче.");
                    return;
                }

                long currentTime = System.currentTimeMillis();
                long lastAttempt = tetherManager.getLastBindAttemptTime(player.getUniqueId());
                long timeLeft = (lastAttempt + tetherManager.getTetherCooldownMillis()) - currentTime;

                // Дебаг інформація
                Bukkit.getLogger().info("[TetherDebug] Спроба зв'язування через клік від " + player.getName());
                Bukkit.getLogger().info("[TetherDebug] Останній час спроби: " + lastAttempt);
                Bukkit.getLogger().info("[TetherDebug] Поточний час: " + currentTime);
                Bukkit.getLogger().info("[TetherDebug] Час до кінця кулдауну: " + (timeLeft / 1000) + " секунд");

                if (timeLeft > 0) {
                    player.sendMessage(ChatColor.RED + "Почекайте ще " + (timeLeft / 1000) + " секунд.");
                    return;
                }

                if (Math.random() * 100 >= tetherManager.getTetherChance()) {
                    player.sendMessage(ChatColor.RED + "Спроба зв'язування не вдалася.");
                    return;
                }

                ItemStack itemInHand = player.getInventory().getItemInMainHand();
                if (itemInHand.getAmount() > 1) {
                    itemInHand.setAmount(itemInHand.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }

                tetherManager.startBindingProcess(player, target);
            }
        }
    }

    @EventHandler
    public void onPlayerInteractWithShears(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() == Material.SHEARS) {
            if (event.getRightClicked() instanceof Player) {
                Player target = (Player) event.getRightClicked();

                if (player.isSneaking()) {
                    if (tetherManager.isPlayerTethered(target)) {
                        Player tetherer = tetherManager.getTetherer(target);
                        if (tetherer != null && tetherer.isOnline()) {
                            long currentTime = System.currentTimeMillis();
                            long lastUnbindAttempt = tetherManager.getLastUnbindAttemptTime(player.getUniqueId());

                            if (currentTime - lastUnbindAttempt < tetherManager.getUnbindFailCooldownMillis()) {
                                player.sendMessage(ChatColor.RED + "Почекайте трохи, перш ніж спробувати знову.");
                                return;
                            }

                            if (Math.random() * 100 < tetherManager.getUnbindChance()) {
                                tetherManager.startUnbindingProcess(player, target);
                                event.setCancelled(true);
                            } else {
                                player.sendMessage(ChatColor.RED + "Спроба розв'язування не вдалася.");
                                tetherManager.setLastUnbindAttemptTime(player.getUniqueId(), currentTime);
                            }
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Цей гравець не прив'язаний.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Вам потрібно присісти, щоб розірвати мотузку.");
                }
            }
        }
    }





    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (tetherManager.isPlayerTethered(player)) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();
            if (itemInHand.getType().isEdible()) {
                return;
            }

            event.setCancelled(true);

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (itemInHand.getType() == Material.OAK_BOAT ||
                        itemInHand.getType() == Material.MINECART ||
                        itemInHand.getType() == Material.SADDLE) {
                    player.sendMessage(ChatColor.RED + "Ви не можете використовувати транспорт, поки ви зв'язані.");
                    event.setCancelled(true);
                }

                if (event.getClickedBlock() != null) {
                    Material clickedBlockType = event.getClickedBlock().getType();
                    if (clickedBlockType == Material.CHEST ||
                            clickedBlockType == Material.FURNACE ||
                            clickedBlockType == Material.ANVIL ||
                            clickedBlockType == Material.ENCHANTING_TABLE) {
                        player.sendMessage(ChatColor.RED + "Ви не можете взаємодіяти з цим об'єктом, поки ви зв'язані.");
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (tetherManager.isPlayerTethered(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Ви не можете ламати блоки, поки ви зв'язані.");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (tetherManager.isPlayerTethered(player)) {
            Player tetherer = tetherManager.getTetherer(player);
            if (tetherer != null && tetherer.isOnline()) {
                tetherer.sendMessage(ChatColor.GREEN + "Ваша мотузка з " + player.getName() + " була розірвана через його смерть.");
            }
            tetherManager.unbindPlayers(player, tetherer);
        }
        for (Player potentialTarget : Bukkit.getOnlinePlayers()) {
            if (tetherManager.isPlayerTethered(potentialTarget)) {
                Player currentTetherer = tetherManager.getTetherer(potentialTarget);
                if (currentTetherer != null && currentTetherer.equals(player)) {
                    tetherManager.unbindPlayers(potentialTarget, player);
                    potentialTarget.sendMessage(ChatColor.YELLOW + "Мотузка розірвалась, бо " + player.getName() + " загинув.");
                    player.sendMessage(ChatColor.YELLOW + "Мотузка з " + potentialTarget.getName() + " розірвалась через вашу смерть.");
                }
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (tetherManager.isPlayerTethered(player)) {
            EffectUtil.removeDebuffs(player, tetherManager.getDebuffEffects());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (tetherManager.isPlayerTethered(player)) {
            Player tetherer = tetherManager.getTetherer(player);
            String message = ChatColor.BOLD + "" + ChatColor.RED + "Гравець " + player.getName()
                    + " вийшов із сервера, будучи зв'язаним з "
                    + (tetherer != null ? tetherer.getName() : "невідомим гравцем") + ".";
            Bukkit.broadcastMessage(message);
            Bukkit.getLogger().info(ChatColor.stripColor(message));
            if (tetherer != null && tetherer.isOnline()) {
                tetherer.sendMessage(ChatColor.GREEN + "Мотузка з " + player.getName() + " розірвалась.");
            }
            tetherManager.unbindPlayers(player, tetherer);
        }
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (tetherManager.isPlayerTethered(target)) {
                Player currentTetherer = tetherManager.getTetherer(target);
                if (currentTetherer != null && currentTetherer.equals(player)) {
                    tetherManager.unbindPlayers(target, player);
                    target.sendMessage(ChatColor.YELLOW + "Мотузка розірвалась, бо " + player.getName() + " вийшов із сервера.");
                    Bukkit.getLogger().info("Мотузка між " + player.getName() + " і " + target.getName() + " розірвалась через вихід з гри.");
                }
            }
        }
    }
}