package pl.gildia.database.repositories;

import pl.gildia.database.DatabaseManager;
import pl.gildia.database.entities.GuildMemberEntity;
import pl.gildia.database.entities.MemberRole;
import pl.gildia.utils.UUIDUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Repository do zarządzania członkami gildii
 */
public class MemberRepository {

    private final DatabaseManager databaseManager;

    public MemberRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Dodaje członka do gildii
     */
    public CompletableFuture<GuildMemberEntity> addMember(int guildId, UUID playerUuid, MemberRole role) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO guild_members (guild_id, player_uuid, role, joined_at) VALUES (?, ?, ?, NOW())",
                    Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, guildId);
                ps.setBytes(2, UUIDUtil.uuidToBytes(playerUuid));
                ps.setString(3, role.name());
                ps.executeUpdate();

                int memberId;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        memberId = keys.getInt(1);
                    } else {
                        throw new RuntimeException("Nie udało się pobrać ID członka");
                    }
                }

                // Zwróć pełne dane członka
                return findById(memberId).join();

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas dodawania członka: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Usuwa członka z gildii
     */
    public CompletableFuture<Boolean> removeMember(int guildId, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM guild_members WHERE guild_id = ? AND player_uuid = ?")) {

                ps.setInt(1, guildId);
                ps.setBytes(2, UUIDUtil.uuidToBytes(playerUuid));

                int affectedRows = ps.executeUpdate();
                return affectedRows > 0;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas usuwania członka: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Aktualizuje rolę członka
     */
    public CompletableFuture<Boolean> updateMemberRole(int guildId, UUID playerUuid, MemberRole newRole) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "UPDATE guild_members SET role = ? WHERE guild_id = ? AND player_uuid = ?")) {

                ps.setString(1, newRole.name());
                ps.setInt(2, guildId);
                ps.setBytes(3, UUIDUtil.uuidToBytes(playerUuid));

                int affectedRows = ps.executeUpdate();
                return affectedRows > 0;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas aktualizacji roli członka: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Pobiera członka po ID
     */
    public CompletableFuture<GuildMemberEntity> findById(int memberId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, guild_id, player_uuid, role, joined_at FROM guild_members WHERE id = ?")) {

                ps.setInt(1, memberId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapMemberFromResultSet(rs);
                    }
                }

                return null;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas wyszukiwania członka: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Pobiera członka gildii
     */
    public CompletableFuture<GuildMemberEntity> findMember(int guildId, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, guild_id, player_uuid, role, joined_at FROM guild_members WHERE guild_id = ? AND player_uuid = ?")) {

                ps.setInt(1, guildId);
                ps.setBytes(2, UUIDUtil.uuidToBytes(playerUuid));

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapMemberFromResultSet(rs);
                    }
                }

                return null;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas wyszukiwania członka gildii: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Pobiera wszystkich członków gildii
     */
    public CompletableFuture<List<GuildMemberEntity>> findByGuildId(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            List<GuildMemberEntity> members = new ArrayList<>();

            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, guild_id, player_uuid, role, joined_at FROM guild_members WHERE guild_id = ? ORDER BY joined_at ASC")) {

                ps.setInt(1, guildId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        members.add(mapMemberFromResultSet(rs));
                    }
                }

                return members;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas pobierania członków gildii: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Sprawdza czy gracz jest członkiem gildii
     */
    public CompletableFuture<Boolean> isMember(int guildId, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM guild_members WHERE guild_id = ? AND player_uuid = ?")) {

                ps.setInt(1, guildId);
                ps.setBytes(2, UUIDUtil.uuidToBytes(playerUuid));

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas sprawdzania członkostwa: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Pobiera liczbę członków gildii
     */
    public CompletableFuture<Integer> getMemberCount(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM guild_members WHERE guild_id = ?")) {

                ps.setInt(1, guildId);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas pobierania liczby członków: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Mapuje ResultSet na GuildMemberEntity
     */
    private GuildMemberEntity mapMemberFromResultSet(ResultSet rs) throws SQLException {
        GuildMemberEntity member = new GuildMemberEntity();
        member.setId(rs.getInt("id"));
        member.setGuildId(rs.getInt("guild_id"));
        member.setPlayerUuid(UUIDUtil.bytesToUuid(rs.getBytes("player_uuid")));
        member.setRole(MemberRole.fromString(rs.getString("role")));
        member.setJoinedAt(rs.getTimestamp("joined_at").toLocalDateTime());
        return member;
    }
}
