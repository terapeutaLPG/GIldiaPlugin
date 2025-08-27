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
    private FileConfiguration mainConfig;

    @Override
    public void onEnable() {
        instance = this;
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        saveDefaultConfig();
        mainConfig = getConfig();
        createGildieFile();
        gildiaManager = new GildiaManager(this);
        GildiaCommand gildiaCommand = new GildiaCommand(this);
        getCommand("gildia").setExecutor(gildiaCommand);
        getCommand("gildia").setTabCompleter(gildiaCommand);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        playerDisplayListener = new PlayerDisplayListener(this);
        getServer().getPluginManager().registerEvents(playerDisplayListener, this);
        getServer().getPluginManager().registerEvents(new TagDebugListener(this), this);
        getServer().getPluginManager().registerEvents(new pl.gildia.listeners.PvPListener(this), this);
        // KillListener usunięty - PvP Stats będzie zarządzać statystykami PvP

        // Automatyczna aktualizacja statystyk co 10 sekund
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (playerDisplayListener != null) {
                playerDisplayListener.updateAllPlayersDisplayNames();
            }
        }, 200L, 200L); // 10 sekund = 200 ticków

        getLogger().info("Plugin GildiaPlugin został włączony! Wymagane: PVPStats + PlaceholderAPI dla statystyk PvP.");
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
