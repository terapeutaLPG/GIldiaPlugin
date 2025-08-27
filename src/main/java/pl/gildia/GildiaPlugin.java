package pl.gildia;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import pl.gildia.commands.GildiaCommand;
import pl.gildia.listeners.PlayerDisplayListener;
import pl.gildia.listeners.TagDebugListener;
import pl.gildia.managers.GildiaManager;
import pl.gildia.utils.DiscordWebhook;

public class GildiaPlugin extends JavaPlugin implements Listener {

    private static GildiaPlugin instance;
    private GildiaManager gildiaManager;
    private PlayerDisplayListener playerDisplayListener;
    private DiscordWebhook discordWebhook;
    private File gildieFile;
    private FileConfiguration gildieConfig;
    private FileConfiguration mainConfig;

    @Override
    public void onEnable() {
        instance = this;

        // Upewnij się że folder pluginu istnieje
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            if (dataFolder.mkdirs()) {
                getLogger().info("Utworzono folder pluginu: " + dataFolder.getAbsolutePath());
            } else {
                getLogger().severe("Nie można utworzyć folderu pluginu: " + dataFolder.getAbsolutePath());
                return;
            }
        } else {
            getLogger().info("Folder pluginu już istnieje: " + dataFolder.getAbsolutePath());
        }

        saveDefaultConfig();
        mainConfig = getConfig();
        createGildieFile();
        gildiaManager = new GildiaManager(this);
        discordWebhook = new DiscordWebhook(this);
        GildiaCommand gildiaCommand = new GildiaCommand(this);
        getCommand("gildia").setExecutor(gildiaCommand);
        getCommand("gildia").setTabCompleter(gildiaCommand);
        // ChatListener usunięty - plugin nie będzie ingerować w chat
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

        // Sprawdź status folderu i plików (dla debugowania)
        checkDataFolderStatus();

        getLogger().info("Plugin GildiaPlugin został włączony! Wymagane: PVPStats + PlaceholderAPI dla statystyk PvP.");
    }

    @Override
    public void onDisable() {
        saveGildieConfig();
        getLogger().info("Plugin GildiaPlugin został wyłączony!");
    }

    private void createGildieFile() {
        try {
            gildieFile = new File(getDataFolder(), "gildie.yml");

            if (!gildieFile.exists()) {
                if (gildieFile.createNewFile()) {
                    getLogger().info("Utworzono plik gildii: " + gildieFile.getAbsolutePath());
                } else {
                    getLogger().severe("Nie można utworzyć pliku gildii: " + gildieFile.getAbsolutePath());
                    return;
                }

                // Inicjalizuj pusty plik konfiguracyjny
                gildieConfig = YamlConfiguration.loadConfiguration(gildieFile);
                gildieConfig.createSection("gildie"); // Utwórz pustą sekcję zamiast ustawiać ""
                saveGildieConfig();
                getLogger().info("Zainicjalizowano pusty plik gildii");
            } else {
                getLogger().info("Plik gildii już istnieje: " + gildieFile.getAbsolutePath());
            }

            // Załaduj konfigurację
            gildieConfig = YamlConfiguration.loadConfiguration(gildieFile);

            // Sprawdź czy sekcja gildie istnieje
            if (!gildieConfig.contains("gildie")) {
                gildieConfig.createSection("gildie");
                saveGildieConfig();
                getLogger().info("Dodano brakującą sekcję 'gildie' do pliku konfiguracyjnego");
            }

        } catch (IOException e) {
            getLogger().severe("Błąd podczas tworzenia pliku gildii: " + e.getMessage());
        }
    }

    public void saveGildieConfig() {
        try {
            if (gildieConfig == null) {
                getLogger().warning("Konfiguracja gildii jest null - nie można zapisać!");
                return;
            }

            if (gildieFile == null) {
                getLogger().warning("Plik gildii jest null - nie można zapisać!");
                return;
            }

            gildieConfig.save(gildieFile);

        } catch (IOException e) {
            getLogger().severe("Błąd podczas zapisywania pliku gildii: " + e.getMessage());
        }
    }

    public void reloadGildieConfig() {
        gildieConfig = YamlConfiguration.loadConfiguration(gildieFile);
    }

    public FileConfiguration getGildieConfig() {
        return gildieConfig;
    }

    // Funkcja diagnostyczna do sprawdzenia stanu plików
    public void checkDataFolderStatus() {
        getLogger().info("=== Status folderu danych pluginu ===");
        getLogger().info("Folder danych: " + getDataFolder().getAbsolutePath());
        getLogger().info("Folder istnieje: " + getDataFolder().exists());
        getLogger().info("Folder można pisać: " + getDataFolder().canWrite());

        if (gildieFile != null) {
            getLogger().info("Plik gildii: " + gildieFile.getAbsolutePath());
            getLogger().info("Plik gildii istnieje: " + gildieFile.exists());
            getLogger().info("Plik gildii można pisać: " + gildieFile.canWrite());
        } else {
            getLogger().info("Plik gildii: null");
        }

        File configFile = new File(getDataFolder(), "config.yml");
        getLogger().info("Plik config.yml: " + configFile.getAbsolutePath());
        getLogger().info("Config.yml istnieje: " + configFile.exists());
        getLogger().info("=====================================");
    }

    public GildiaManager getGildiaManager() {
        return gildiaManager;
    }

    public PlayerDisplayListener getPlayerDisplayListener() {
        return playerDisplayListener;
    }

    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }

    public static GildiaPlugin getInstance() {
        return instance;
    }
}
