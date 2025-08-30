package pl.gildia.database;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.gildia.GildiaPlugin;
import pl.gildia.utils.UUIDUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manager migracji danych z YAML do MySQL
 */
public class MigrationManager {

    private final GildiaPlugin plugin;
    private final DatabaseManager databaseManager;

    public MigrationManager(GildiaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Wykonuje pełną migrację
     */
    public CompletableFuture<Boolean> migrate() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Wykonaj skrypty SQL
                if (!executeInitialMigration()) {
                    return false;
                }

                // 2. Migruj dane z YAML jeśli istnieją
                if (!migrateDataFromYaml()) {
                    return false;
                }

                // 3. Oznacz migrację jako zakończoną
                markMigrationDone();

                plugin.getLogger().info("Migracja do MySQL zakończona pomyślnie");
                return true;

            } catch (Exception e) {
                plugin.getLogger().severe("Błąd podczas migracji: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Wykonuje początkową migrację SQL
     */
    private boolean executeInitialMigration() {
        try (Connection connection = databaseManager.getConnection()) {

            // Sprawdź czy migracja już była wykonana
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM migration_history WHERE migration_name = ?")) {
                ps.setString(1, "001_init.sql");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        plugin.getLogger().info("Migracja SQL już wykonana - pomijam");
                        return true;
                    }
                }
            } catch (Exception e) {
                // Tabela migration_history może nie istnieć - to normalne przy pierwszym uruchomieniu
                plugin.getLogger().info("Wykonuję pierwszą migrację SQL...");
            }

            // Wczytaj i wykonaj skrypt migracji
            InputStream sqlStream = plugin.getResource("sql/001_init.sql");
            if (sqlStream == null) {
                plugin.getLogger().severe("Nie można znaleźć pliku migracji 001_init.sql");
                return false;
            }

            StringBuilder sqlContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(sqlStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty() && !line.trim().startsWith("--")) {
                        sqlContent.append(line).append("\n");
                    }
                }
            }

            // Podziel na pojedyncze polecenia
            String[] sqlStatements = sqlContent.toString().split(";");

            try (Statement statement = connection.createStatement()) {
                for (String sql : sqlStatements) {
                    sql = sql.trim();
                    if (!sql.isEmpty()) {
                        statement.executeUpdate(sql);
                    }
                }
            }

            plugin.getLogger().info("Schemat bazy danych został utworzony");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Błąd podczas wykonywania migracji SQL: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Migruje dane z pliku YAML
     */
    private boolean migrateDataFromYaml() {
        File yamlFile = new File(plugin.getDataFolder(), "gildie.yml");
        if (!yamlFile.exists()) {
            plugin.getLogger().info("Plik gildie.yml nie istnieje - brak danych do migracji");
            return true;
        }

        try (Connection connection = databaseManager.getConnection()) {
            connection.setAutoCommit(false);

            FileConfiguration yamlConfig = YamlConfiguration.loadConfiguration(yamlFile);
            ConfigurationSection guildiesSection = yamlConfig.getConfigurationSection("gildie");

            if (guildiesSection == null) {
                plugin.getLogger().info("Brak sekcji gildii w YAML - brak danych do migracji");
                return true;
            }

            plugin.getLogger().info("Rozpoczynam migrację " + guildiesSection.getKeys(false).size() + " gildii z YAML...");

            for (String guildKey : guildiesSection.getKeys(false)) {
                ConfigurationSection guildSection = guildiesSection.getConfigurationSection(guildKey);
                if (guildSection == null) {
                    continue;
                }

                // Pobierz dane gildii
                String name = guildSection.getString("nazwa");
                String tag = guildSection.getString("tag");
                String leaderStr = guildSection.getString("lider");

                if (name == null || tag == null || leaderStr == null) {
                    plugin.getLogger().warning("Niepełne dane gildii: " + guildKey + " - pomijam");
                    continue;
                }

                UUID leaderUuid = UUID.fromString(leaderStr);
                long createdAt = guildSection.getLong("dataZalozenia", System.currentTimeMillis());

                // Wstaw gildię
                int guildId;
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO guilds (tag, name, leader_uuid, created_at, points, friendly_fire) VALUES (?, ?, ?, FROM_UNIXTIME(?), 0, TRUE)",
                        Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, tag);
                    ps.setString(2, name);
                    ps.setBytes(3, UUIDUtil.uuidToBytes(leaderUuid));
                    ps.setLong(4, createdAt / 1000); // Convert to seconds

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            guildId = keys.getInt(1);
                        } else {
                            throw new RuntimeException("Nie udało się pobrać ID gildii");
                        }
                    }
                }

                // Dodaj członków
                List<String> members = guildSection.getStringList("czlonkowie");
                List<String> deputies = guildSection.getStringList("zastepcy");

                // Dodaj lidera
                addMember(connection, guildId, leaderUuid, "LEADER", createdAt);

                // Dodaj zastępców
                for (String deputyStr : deputies) {
                    try {
                        UUID deputyUuid = UUID.fromString(deputyStr);
                        if (!deputyUuid.equals(leaderUuid)) {
                            addMember(connection, guildId, deputyUuid, "DEPUTY", createdAt);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Niepoprawny UUID zastępcy: " + deputyStr);
                    }
                }

                // Dodaj pozostałych członków
                for (String memberStr : members) {
                    try {
                        UUID memberUuid = UUID.fromString(memberStr);
                        if (!memberUuid.equals(leaderUuid) && !deputies.contains(memberStr)) {
                            addMember(connection, guildId, memberUuid, "MEMBER", createdAt);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Niepoprawny UUID członka: " + memberStr);
                    }
                }

                plugin.getLogger().info("Zmigrowano gildię: " + name + " [" + tag + "] (" + members.size() + " członków)");
            }

            connection.commit();
            plugin.getLogger().info("Migracja danych z YAML zakończona pomyślnie");

            // Opcjonalnie: stwórz backup YAML
            File backupFile = new File(plugin.getDataFolder(), "gildie.yml.backup");
            if (yamlFile.renameTo(backupFile)) {
                plugin.getLogger().info("Utworzono backup: gildie.yml.backup");
            }

            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Błąd podczas migracji danych z YAML: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void addMember(Connection connection, int guildId, UUID playerUuid, String role, long joinedAt) throws Exception {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT IGNORE INTO guild_members (guild_id, player_uuid, role, joined_at) VALUES (?, ?, ?, FROM_UNIXTIME(?))")) {

            ps.setInt(1, guildId);
            ps.setBytes(2, UUIDUtil.uuidToBytes(playerUuid));
            ps.setString(3, role);
            ps.setLong(4, joinedAt / 1000);

            ps.executeUpdate();
        }
    }

    private void markMigrationDone() {
        plugin.getConfig().set("migration.done", true);
        plugin.saveConfig();
    }

    /**
     * Sprawdza czy migracja została już wykonana
     */
    public boolean isMigrationDone() {
        return plugin.getConfig().getBoolean("migration.done", false);
    }
}
