package nulifie.xyz.tetherthere;

import hetmanplugins.actionLog.ActionLog;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static ActionLog actionLogAPI;
    private TetherManager tetherManager;
    private CommandHandler commandHandler;
    private EventListener eventListener;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (Bukkit.getPluginManager().getPlugin("ActionLog") != null) {
            actionLogAPI = (ActionLog) Bukkit.getPluginManager().getPlugin("ActionLog");
            getLogger().info("Успішно інтегровано з ActionLog!");
        }
        tetherManager = new TetherManager(this);
        commandHandler = new CommandHandler(this, tetherManager);
        eventListener = new EventListener(tetherManager);
        sendStartupMessage();


        getServer().getPluginManager().registerEvents(eventListener, this);
        getCommand("tether").setExecutor(commandHandler);
        getCommand("tether").setTabCompleter(commandHandler);
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
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "TetherThere v" + getDescription().getVersion() + " successfully launched!");
        getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "Author: " + String.join(", ", getDescription().getAuthors()));
    }

    @Override
    public void onDisable() {
        tetherManager.clearAll();
    }
}