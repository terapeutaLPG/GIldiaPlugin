package pl.gildia;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import pl.gildia.commands.GildiaCommand;
import pl.gildia.listeners.ChatListener;
import pl.gildia.listeners.PlayerDisplayListener;
import pl.gildia.listeners.TagDebugListener;
import pl.gildia.managers.GildiaManager;

public class GildiaPlugin extends JavaPlugin implements Listener {

    private static GildiaPlugin instance;
    private GildiaManager gildiaManager;
    private PlayerDisplayListener playerDisplayListener;
    private File gildieFile;
    private FileConfiguration gildieConfig;

    @Override
    public void onEnable() {
        instance = this;

        // Utworzenie folderów i plików
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        createGildieFile();

        // Inicjalizacja managerów
        gildiaManager = new GildiaManager(this);

        // Rejestracja komend
        GildiaCommand gildiaCommand = new GildiaCommand(this);
        getCommand("gildia").setExecutor(gildiaCommand);
        getCommand("gildia").setTabCompleter(gildiaCommand);

        // Rejestracja listenerów
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        playerDisplayListener = new PlayerDisplayListener(this);
        getServer().getPluginManager().registerEvents(playerDisplayListener, this);
        getServer().getPluginManager().registerEvents(new TagDebugListener(this), this);
        getServer().getPluginManager().registerEvents(new pl.gildia.listeners.KillListener(this), this);

        getLogger().info("Plugin GildiaPlugin został włączony!");
    }

    @Override
    public void onDisable() {
        saveGildieConfig();
        getLogger().info("Plugin GildiaPlugin został wyłączony!");
    }

    private void createGildieFile() {
        gildieFile = new File(getDataFolder(), "gildie.yml");
        if (!gildieFile.exists()) {
            try {
                gildieFile.createNewFile();
                gildieConfig = YamlConfiguration.loadConfiguration(gildieFile);
                gildieConfig.set("gildie", "");
                saveGildieConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        gildieConfig = YamlConfiguration.loadConfiguration(gildieFile);
    }

    public void saveGildieConfig() {
        try {
            gildieConfig.save(gildieFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadGildieConfig() {
        gildieConfig = YamlConfiguration.loadConfiguration(gildieFile);
    }

    public FileConfiguration getGildieConfig() {
        return gildieConfig;
    }

    public GildiaManager getGildiaManager() {
        return gildiaManager;
    }

    public PlayerDisplayListener getPlayerDisplayListener() {
        return playerDisplayListener;
    }

    public static GildiaPlugin getInstance() {
        return instance;
    }
}
