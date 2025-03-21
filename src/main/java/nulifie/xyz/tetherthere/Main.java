package nulifie.xyz.tetherthere;

import hetmanplugins.actionLog.ActionLog;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import nulifie.xyz.tetherthere.commands.CommandHandler;
import nulifie.xyz.tetherthere.events.EventListener;
import nulifie.xyz.tetherthere.models.TetherManager;

public class Main extends JavaPlugin {
    private static ActionLog actionLogAPI;
    private TetherManager tetherManager;
    private CommandHandler commandHandler;
    private EventListener eventListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // Спробуємо завантажити ActionLog, але не зупиняємо роботу плагіна, якщо не вдалося
        try {
            if (Bukkit.getPluginManager().getPlugin("ActionLog") != null) {
                actionLogAPI = (ActionLog) Bukkit.getPluginManager().getPlugin("ActionLog");
                getLogger().info("Успішно інтегровано з ActionLog!");
            } else {
                getLogger().warning("ActionLog не знайдено. Деякі функції будуть недоступні.");
            }
        } catch (Exception e) {
            getLogger().warning("Не вдалося завантажити ActionLog: " + e.getMessage());
        }

        tetherManager = new TetherManager(this);
        commandHandler = new CommandHandler(this, tetherManager);
        eventListener = new EventListener(tetherManager);
        sendStartupMessage();

        getServer().getPluginManager().registerEvents(eventListener, this);
        getCommand("tether").setExecutor(commandHandler);
        getCommand("tether").setTabCompleter(commandHandler);
    }

    @Override
    public void onDisable() {
        if (tetherManager != null) {
            tetherManager.clearAll();
        }
        if (actionLogAPI != null) {
            actionLogAPI = null;
        }
    }

    public static ActionLog getActionLogAPI() {
        return actionLogAPI;
    }

    private void sendStartupMessage() {
        String[] logo = {
                ChatColor.AQUA + "______       __   __               ",
                ChatColor.AQUA + "/_  __/___   / /_ / /_   ___   _____",
                ChatColor.AQUA + " / /  / _ \\ / __// __ \\ / _ \\ / ___/",
                ChatColor.AQUA + "/ /  /  __// /_ / / / //  __// /    ",
                ChatColor.AQUA + "/_/   \\___/ \\__//_/ /_/ \\___//_/    "
        };

        for (String line : logo) {
            getServer().getConsoleSender().sendMessage(line);
        }
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "TetherThere v" + getDescription().getVersion() + " успішно запущено!");
        getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "Автор: " + String.join(", ", getDescription().getAuthors()));
    }
}