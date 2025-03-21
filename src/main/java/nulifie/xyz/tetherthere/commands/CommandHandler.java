package nulifie.xyz.tetherthere.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import nulifie.xyz.tetherthere.Main;
import nulifie.xyz.tetherthere.models.TetherManager;

import java.util.ArrayList;
import java.util.List;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final TetherManager tetherManager;

    public CommandHandler(Main plugin, TetherManager tetherManager) {
        this.plugin = plugin;
        this.tetherManager = tetherManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("tether")) {
            if (args.length >= 1) {
                String subCommand = args[0].toLowerCase();

                switch (subCommand) {
                    case "bind":
                        if (args.length == 3) {
                            Player player1 = Bukkit.getPlayer(args[1]);
                            Player player2 = Bukkit.getPlayer(args[2]);
                            if (player1 == null || player2 == null) {
                                sender.sendMessage(ChatColor.RED + "Один або обидва гравці не в мережі.");
                                return true;
                            }
                            tetherManager.tetherPlayers(player1, player2);
                            return true;
                        }
                        sender.sendMessage(ChatColor.RED + "Неправильне використання команди.");
                        sender.sendMessage(ChatColor.GRAY + "Приклад: /tether bind <player1> <player2>");
                        return true;

                    case "unbind":
                        if (args.length == 3) {
                            Player player1 = Bukkit.getPlayer(args[1]);
                            Player player2 = Bukkit.getPlayer(args[2]);
                            if (player1 == null || player2 == null) {
                                sender.sendMessage(ChatColor.RED + "Один або обидва гравці не в мережі.");
                                return true;
                            }
                            tetherManager.unbindPlayers(player1, player2);
                            sender.sendMessage(ChatColor.GREEN + "Гравці " + player1.getName() + " та " + player2.getName() + " розв'язані.");
                            return true;
                        }
                        sender.sendMessage(ChatColor.RED + "Неправильне використання команди.");
                        sender.sendMessage(ChatColor.GRAY + "Приклад: /tether unbind <player1> <player2>");
                        return true;

                    case "resetbindcooldown":
                        tetherManager.clearAll();
                        sender.sendMessage(ChatColor.GREEN + "Кулдаун на зв'язування було скинуто для всіх гравців.");
                        return true;

                    case "reload":
                        plugin.reloadConfig();
                        tetherManager.loadConfigValues();
                        sender.sendMessage(ChatColor.GREEN + "Конфігурація була перезавантажена.");
                        return true;

                    default:
                        sender.sendMessage(ChatColor.RED + "Невідома команда.");
                        sendSubCommandUsage(sender);
                        return true;
                }
            }
            sender.sendMessage(ChatColor.RED + "Неправильне використання команди.");
            sendSubCommandUsage(sender);
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("tether")) {
            if (args.length == 1) {
                completions.add("bind");
                completions.add("unbind");
                completions.add("reload");
                completions.add("help");
                completions.add("resetbindcooldown");
            } else if (args.length == 2 || args.length == 3) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }

    private void sendSubCommandUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "====== TetherThere Help ======");
        sender.sendMessage(ChatColor.YELLOW + "/tether bind <player1> <player2>");
        sender.sendMessage(ChatColor.WHITE + "  - " + ChatColor.GRAY + "Зв'язує двох гравців мотузкою.");
        sender.sendMessage(ChatColor.GRAY + "    Приклад: /tether bind Steve Alex");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "/tether unbind <player1> <player2>");
        sender.sendMessage(ChatColor.WHITE + "  - " + ChatColor.GRAY + "Розв'язує мотузку між двома гравцями.");
        sender.sendMessage(ChatColor.GRAY + "    Приклад: /tether unbind Steve Alex");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "/tether resetbindcooldown");
        sender.sendMessage(ChatColor.WHITE + "  - " + ChatColor.GRAY + "Скидає кулдаун для всіх гравців, дозволяючи їм спробувати зв'язування знову.");
        sender.sendMessage(ChatColor.GRAY + "    Приклад: /tether resetbindcooldown");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "/tether reload");
        sender.sendMessage(ChatColor.WHITE + "  - " + ChatColor.GRAY + "Перезавантажує конфігурацію плагіну.");
        sender.sendMessage(ChatColor.GRAY + "    Приклад: /tether reload");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "==============================");
    }
}