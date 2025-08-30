package pl.gildia.database.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import pl.gildia.database.DatabaseManager;

/**
 * Repository do zarządzania sojuszami między gildiami
 */
public class AllianceRepository {

    private final DatabaseManager databaseManager;

    public AllianceRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Tworzy sojusz między gildiami
     */
    public CompletableFuture<Void> createAlliance(int guildIdA, int guildIdB) {
        return databaseManager.executeTransaction(connection -> {
            // Najpierw usuń zaproszenia w obu kierunkach
            try (PreparedStatement ps1 = connection.prepareStatement(
                    "DELETE FROM alliance_invites WHERE "
                    + "(from_guild_id = ? AND to_guild_id = ?) OR "
                    + "(from_guild_id = ? AND to_guild_id = ?)")) {

                ps1.setInt(1, guildIdA);
                ps1.setInt(2, guildIdB);
                ps1.setInt(3, guildIdB);
                ps1.setInt(4, guildIdA);
                ps1.executeUpdate();
            }

            // Następnie utwórz sojusz (zawsze mniejsze ID pierwsze dla unikania duplikatów)
            int minId = Math.min(guildIdA, guildIdB);
            int maxId = Math.max(guildIdA, guildIdB);

            try (PreparedStatement ps2 = connection.prepareStatement(
                    "INSERT IGNORE INTO guild_alliances (guild_id_a, guild_id_b, created_at) VALUES (?, ?, NOW())")) {

                ps2.setInt(1, minId);
                ps2.setInt(2, maxId);
                ps2.executeUpdate();
            }
        });
    }

    /**
     * Usuwa sojusz między gildiami
     */
    public CompletableFuture<Boolean> removeAlliance(int guildIdA, int guildIdB) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM guild_alliances WHERE "
                    + "(guild_id_a = ? AND guild_id_b = ?) OR "
                    + "(guild_id_a = ? AND guild_id_b = ?)")) {

                ps.setInt(1, guildIdA);
                ps.setInt(2, guildIdB);
                ps.setInt(3, guildIdB);
                ps.setInt(4, guildIdA);

                int affectedRows = ps.executeUpdate();
                return affectedRows > 0;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas usuwania sojuszu: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Sprawdza czy gildie są sojusznikami
     */
    public CompletableFuture<Boolean> areAllies(int guildIdA, int guildIdB) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM guild_alliances WHERE "
                    + "(guild_id_a = ? AND guild_id_b = ?) OR "
                    + "(guild_id_a = ? AND guild_id_b = ?)")) {

                ps.setInt(1, guildIdA);
                ps.setInt(2, guildIdB);
                ps.setInt(3, guildIdB);
                ps.setInt(4, guildIdA);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas sprawdzania sojuszu: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Pobiera nazwy sojuszniczych gildii
     */
    public CompletableFuture<List<String>> getAllyGuildNames(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> allies = new ArrayList<>();

            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT CASE "
                    + "  WHEN ga.guild_id_a = ? THEN gb.name "
                    + "  ELSE ga_guild.name "
                    + "END as ally_name "
                    + "FROM guild_alliances ga "
                    + "LEFT JOIN guilds gb ON ga.guild_id_b = gb.id "
                    + "LEFT JOIN guilds ga_guild ON ga.guild_id_a = ga_guild.id "
                    + "WHERE ga.guild_id_a = ? OR ga.guild_id_b = ? "
                    + "ORDER BY ga.created_at DESC")) {

                ps.setInt(1, guildId);
                ps.setInt(2, guildId);
                ps.setInt(3, guildId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        allies.add(rs.getString("ally_name"));
                    }
                }

                return allies;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas pobierania sojuszników: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Pobiera ID sojuszniczych gildii
     */
    public CompletableFuture<List<Integer>> getAllyGuildIds(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            List<Integer> allies = new ArrayList<>();

            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT CASE "
                    + "  WHEN guild_id_a = ? THEN guild_id_b "
                    + "  ELSE guild_id_a "
                    + "END as ally_id "
                    + "FROM guild_alliances "
                    + "WHERE guild_id_a = ? OR guild_id_b = ?")) {

                ps.setInt(1, guildId);
                ps.setInt(2, guildId);
                ps.setInt(3, guildId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        allies.add(rs.getInt("ally_id"));
                    }
                }

                return allies;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas pobierania ID sojuszników: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Usuwa wszystkie sojusze gildii (przy usuwaniu gildii)
     */
    public CompletableFuture<Void> removeAllGuildAlliances(int guildId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM guild_alliances WHERE guild_id_a = ? OR guild_id_b = ?")) {

                ps.setInt(1, guildId);
                ps.setInt(2, guildId);
                ps.executeUpdate();

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas usuwania wszystkich sojuszy gildii: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }
}
