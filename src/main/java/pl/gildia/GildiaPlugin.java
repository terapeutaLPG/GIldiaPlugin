package pl.gildia;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import pl.gildia.commands.GildiaCommand;
import pl.gildia.database.DatabaseManager;
import pl.gildia.database.MigrationManager;
import pl.gildia.database.services.GuildService;
import pl.gildia.listeners.PlayerDisplayListener;
import pl.gildia.listeners.TagDebugListener;
import pl.gildia.managers.GildiaManager;
import pl.gildia.utils.DiscordWebhook;

public class GildiaPlugin extends JavaPlugin implements Listener {

    private static GildiaPlugin instance;

    // YAML Manager (fallback)
    private GildiaManager gildiaManager;
    private File gildieFile;
    private FileConfiguration gildieConfig;

    // MySQL/Database
    private DatabaseManager databaseManager;
    private GuildService guildService;
    private MigrationManager migrationManager;

    // Inne komponenty
    private PlayerDisplayListener playerDisplayListener;
    private DiscordWebhook discordWebhook;
    private FileConfiguration mainConfig;

    private boolean useDatabase = false;

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
        }

        saveDefaultConfig();
        mainConfig = getConfig();

        // Inicjalizacja bazy danych
        databaseManager = new DatabaseManager(this);

        databaseManager.initialize().thenAccept(success -> {
            if (success) {
                useDatabase = true;
                getLogger().info("Używam MySQL do przechowywania danych gildii");

                // Inicjalizuj serwisy MySQL
                guildService = new GuildService(this, databaseManager);
                migrationManager = new MigrationManager(this, databaseManager);

                // Wykonaj migrację jeśli potrzebna
                if (!migrationManager.isMigrationDone()) {
                    migrationManager.migrate().thenRun(() -> {
                        // Załaduj cache po migracji
                        guildService.loadCache().thenRun(() -> {
                            getLogger().info("Migracja i ładowanie cache zakończone");
                        });
                    });
                } else {
                    // Załaduj cache
                    guildService.loadCache().thenRun(() -> {
                        getLogger().info("Cache gildii załadowany z MySQL");
                    });
                }

            } else {
                useDatabase = false;
                getLogger().info("Używam YAML do przechowywania danych gildii");

                // Fallback na YAML
                initializeYamlManager();
            }
        }).exceptionally(throwable -> {
            getLogger().severe("Błąd inicjalizacji bazy danych - używam YAML");
            useDatabase = false;
            initializeYamlManager();
            return null;
        });

        // Inicjalizuj pozostałe komponenty (niezależnie od DB/YAML)
        initializeComponents();
    }

    /**
     * Inicjalizuje YAML manager jako fallback
     */
    private void initializeYamlManager() {
        createGildieFile();
        gildiaManager = new GildiaManager(this);
    }

    /**
     * Inicjalizuje komponenty pluginu
     */
    private void initializeComponents() {
        discordWebhook = new DiscordWebhook(this);
        GildiaCommand gildiaCommand = new GildiaCommand(this);
        getCommand("gildia").setExecutor(gildiaCommand);
        getCommand("gildia").setTabCompleter(gildiaCommand);
        // ChatListener usunięty - plugin nie ingeruje w chat
        playerDisplayListener = new PlayerDisplayListener(this);
        getServer().getPluginManager().registerEvents(playerDisplayListener, this);
        getServer().getPluginManager().registerEvents(new TagDebugListener(this), this);
        getServer().getPluginManager().registerEvents(new pl.gildia.listeners.PvPListener(this), this);

        // Automatyczna aktualizacja statystyk co 10 sekund
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (playerDisplayListener != null) {
                playerDisplayListener.updateAllPlayersDisplayNames();
            }
        }, 200L, 200L); // 10 sekund = 200 ticków

        // Sprawdź status folderu i plików (dla debugowania)
        checkDataFolderStatus();

        String dbType = useDatabase ? "MySQL" : "YAML";
        getLogger().info("Plugin GildiaPlugin został włączony! Używa: " + dbType + " | Wymagane: PVPStats + PlaceholderAPI");
    }

    @Override
    public void onDisable() {
        if (useDatabase && databaseManager != null) {
            databaseManager.shutdown();
        } else if (gildieConfig != null) {
            saveGildieConfig();
        }
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

    public GuildService getGuildService() {
        return guildService;
    }

    public boolean isUsingDatabase() {
        return useDatabase;
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
