package nulifie.xyz.tetherthere;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private TetherManager tetherManager;
    private CommandHandler commandHandler;
    private EventListener eventListener;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        tetherManager = new TetherManager(this);
        commandHandler = new CommandHandler(this, tetherManager);
        eventListener = new EventListener(tetherManager);


        getServer().getPluginManager().registerEvents(eventListener, this);
        getCommand("tether").setExecutor(commandHandler);
        getCommand("tether").setTabCompleter(commandHandler);
    }

    @Override
    public void onDisable() {
        tetherManager.clearAll();
    }
}