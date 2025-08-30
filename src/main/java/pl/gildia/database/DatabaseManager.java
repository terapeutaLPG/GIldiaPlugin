package pl.gildia.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import pl.gildia.GildiaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Manager bazy danych z HikariCP pool
 */
public class DatabaseManager {

    private final GildiaPlugin plugin;
    private HikariDataSource dataSource;
    private final Executor asyncExecutor;
    private boolean enabled = false;

    public DatabaseManager(GildiaPlugin plugin) {
        this.plugin = plugin;
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "GildiaPlugin-DB");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Inicjalizuje połączenie z bazą danych
     */
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                FileConfiguration config = plugin.getConfig();

                if (!config.getBoolean("database.enabled", false)) {
                    plugin.getLogger().info("Baza danych wyłączona - używam YAML");
                    return false;
                }

                String host = config.getString("database.host", "127.0.0.1");
                int port = config.getInt("database.port", 3306);
                String database = config.getString("database.name", "gildie");
                String username = config.getString("database.user", "gildie_user");
                String password = config.getString("database.password", "super_tajne");

                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
                hikariConfig.setUsername(username);
                hikariConfig.setPassword(password);
                hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");

                // Ustawienia puli połączeń
                hikariConfig.setMaximumPoolSize(config.getInt("database.pool.maximum-pool-size", 10));
                hikariConfig.setMinimumIdle(config.getInt("database.pool.minimum-idle", 2));
                hikariConfig.setConnectionTimeout(config.getLong("database.pool.connection-timeout", 30000));
                hikariConfig.setIdleTimeout(config.getLong("database.pool.idle-timeout", 600000));
                hikariConfig.setMaxLifetime(config.getLong("database.pool.max-lifetime", 1800000));

                // Testowanie połączenia
                hikariConfig.setConnectionTestQuery("SELECT 1");
                hikariConfig.setValidationTimeout(5000);

                // Pool name
                hikariConfig.setPoolName("GildiaPlugin-HikariCP");

                dataSource = new HikariDataSource(hikariConfig);

                // Test połączenia
                try (Connection connection = dataSource.getConnection()) {
                    plugin.getLogger().info("Połączono z bazą danych MySQL: " + host + ":" + port + "/" + database);
                }

                enabled = true;
                return true;

            } catch (Exception e) {
                plugin.getLogger().severe("Nie można połączyć z bazą danych: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, asyncExecutor);
    }

    /**
     * Pobiera połączenie z puli
     */
    public Connection getConnection() throws SQLException {
        if (!enabled || dataSource == null) {
            throw new SQLException("Baza danych nie jest włączona lub zainicjalizowana");
        }
        return dataSource.getConnection();
    }

    /**
     * Wykonuje zapytanie asynchronicznie
     */
    public CompletableFuture<Void> executeAsync(String sql, Object... params) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {

                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }

                statement.executeUpdate();

            } catch (SQLException e) {
                plugin.getLogger().severe("Błąd wykonywania zapytania: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }, asyncExecutor);
    }

    /**
     * Wykonuje transakcję asynchronicznie
     */
    public CompletableFuture<Void> executeTransaction(TransactionTask task) {
        return CompletableFuture.runAsync(() -> {
            Connection connection = null;
            try {
                connection = getConnection();
                connection.setAutoCommit(false);

                task.execute(connection);

                connection.commit();

            } catch (Exception e) {
                if (connection != null) {
                    try {
                        connection.rollback();
                        plugin.getLogger().warning("Transakcja została cofnięta z powodu błędu: " + e.getMessage());
                    } catch (SQLException rollbackEx) {
                        plugin.getLogger().severe("Błąd podczas rollback: " + rollbackEx.getMessage());
                    }
                }
                throw new RuntimeException(e);
            } finally {
                if (connection != null) {
                    try {
                        connection.setAutoCommit(true);
                        connection.close();
                    } catch (SQLException e) {
                        plugin.getLogger().warning("Błąd podczas zamykania połączenia: " + e.getMessage());
                    }
                }
            }
        }, asyncExecutor);
    }

    /**
     * Zamyka pool połączeń
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Pool połączeń MySQL został zamknięty");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Executor getAsyncExecutor() {
        return asyncExecutor;
    }

    @FunctionalInterface
    public interface TransactionTask {

        void execute(Connection connection) throws SQLException;
    }
}
