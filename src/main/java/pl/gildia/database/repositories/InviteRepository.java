package pl.gildia.database.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import pl.gildia.database.DatabaseManager;
import pl.gildia.utils.UUIDUtil;

/**
 * Repository do zarządzania zaproszeniami do gildii
 */
public class InviteRepository {

    private final DatabaseManager databaseManager;

    public InviteRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Dodaje zaproszenie do gildii
     */
    public CompletableFuture<Void> addJoinInvite(int guildId, UUID playerUuid, UUID inviterUuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO join_invites (guild_id, player_uuid, inviter_uuid, sent_at) "
                    + "VALUES (?, ?, ?, NOW()) "
                    + "ON DUPLICATE KEY UPDATE sent_at = NOW(), inviter_uuid = ?")) {

                ps.setInt(1, guildId);
                ps.setBytes(2, UUIDUtil.uuidToBytes(playerUuid));
                ps.setBytes(3, UUIDUtil.uuidToBytes(inviterUuid));
                ps.setBytes(4, UUIDUtil.uuidToBytes(inviterUuid));

                ps.executeUpdate();

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas dodawania zaproszenia: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Usuwa zaproszenie do gildii
     */
    public CompletableFuture<Boolean> removeJoinInvite(int guildId, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM join_invites WHERE guild_id = ? AND player_uuid = ?")) {

                ps.setInt(1, guildId);
                ps.setBytes(2, UUIDUtil.uuidToBytes(playerUuid));

                int affectedRows = ps.executeUpdate();
                return affectedRows > 0;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas usuwania zaproszenia: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Sprawdza czy gracz ma zaproszenie do konkretnej gildii
     */
    public CompletableFuture<Boolean> hasJoinInvite(int guildId, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM join_invites WHERE guild_id = ? AND player_uuid = ?")) {

                ps.setInt(1, guildId);
                ps.setBytes(2, UUIDUtil.uuidToBytes(playerUuid));

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas sprawdzania zaproszenia: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Pobiera gildię do której gracz ma zaproszenie (po nazwie)
     */
    public CompletableFuture<String> getJoinInviteGuildName(UUID playerUuid, String guildName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT g.name FROM join_invites ji "
                    + "JOIN guilds g ON ji.guild_id = g.id "
                    + "WHERE ji.player_uuid = ? AND g.name = ?")) {

                ps.setBytes(1, UUIDUtil.uuidToBytes(playerUuid));
                ps.setString(2, guildName);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString("name") : null;
                }

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas wyszukiwania zaproszenia po nazwie gildii: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Dodaje zaproszenie do sojuszu
     */
    public CompletableFuture<Void> addAllianceInvite(int fromGuildId, int toGuildId) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO alliance_invites (from_guild_id, to_guild_id, sent_at) "
                    + "VALUES (?, ?, NOW()) "
                    + "ON DUPLICATE KEY UPDATE sent_at = NOW()")) {

                ps.setInt(1, fromGuildId);
                ps.setInt(2, toGuildId);

                ps.executeUpdate();

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas dodawania zaproszenia do sojuszu: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Usuwa zaproszenie do sojuszu
     */
    public CompletableFuture<Boolean> removeAllianceInvite(int fromGuildId, int toGuildId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM alliance_invites WHERE from_guild_id = ? AND to_guild_id = ?")) {

                ps.setInt(1, fromGuildId);
                ps.setInt(2, toGuildId);

                int affectedRows = ps.executeUpdate();
                return affectedRows > 0;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas usuwania zaproszenia do sojuszu: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Pobiera zaproszenia do sojuszy dla gildii
     */
    public CompletableFuture<List<String>> getAllianceInvites(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> invites = new ArrayList<>();

            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT g.name FROM alliance_invites ai "
                    + "JOIN guilds g ON ai.from_guild_id = g.id "
                    + "WHERE ai.to_guild_id = ? "
                    + "ORDER BY ai.sent_at DESC")) {

                ps.setInt(1, guildId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        invites.add(rs.getString("name"));
                    }
                }

                return invites;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas pobierania zaproszeń do sojuszy: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Sprawdza czy istnieje zaproszenie do sojuszu między gildiami
     */
    public CompletableFuture<Boolean> hasAllianceInvite(int fromGuildId, int toGuildId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM alliance_invites WHERE from_guild_id = ? AND to_guild_id = ?")) {

                ps.setInt(1, fromGuildId);
                ps.setInt(2, toGuildId);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas sprawdzania zaproszenia do sojuszu: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }
}
