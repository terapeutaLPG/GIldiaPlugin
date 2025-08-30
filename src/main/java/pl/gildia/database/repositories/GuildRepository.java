package pl.gildia.database.repositories;

import pl.gildia.database.DatabaseManager;
import pl.gildia.database.entities.GuildEntity;
import pl.gildia.database.entities.GuildMemberEntity;
import pl.gildia.database.entities.MemberRole;
import pl.gildia.utils.UUIDUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Repository do zarządzania gildiami w bazie danych
 */
public class GuildRepository {

    private final DatabaseManager databaseManager;

    public GuildRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Tworzy nową gildię
     */
    public CompletableFuture<GuildEntity> createGuild(String tag, String name, UUID leaderUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection()) {
                connection.setAutoCommit(false);

                // 1. Wstaw gildię
                int guildId;
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO guilds (tag, name, leader_uuid, created_at, points, friendly_fire) VALUES (?, ?, ?, NOW(), 0, TRUE)",
                        Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, tag);
                    ps.setString(2, name);
                    ps.setBytes(3, UUIDUtil.uuidToBytes(leaderUuid));
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            guildId = keys.getInt(1);
                        } else {
                            throw new RuntimeException("Nie udało się pobrać ID gildii");
                        }
                    }
                }

                // 2. Dodaj lidera jako członka
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO guild_members (guild_id, player_uuid, role, joined_at) VALUES (?, ?, 'LEADER', NOW())")) {

                    ps.setInt(1, guildId);
                    ps.setBytes(2, UUIDUtil.uuidToBytes(leaderUuid));
                    ps.executeUpdate();
                }

                connection.commit();

                // 3. Zwróć pełne dane gildii
                return findById(guildId).join();

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas tworzenia gildii: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Znajduje gildię po ID
     */
    public CompletableFuture<GuildEntity> findById(int guildId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, tag, name, leader_uuid, created_at, points, friendly_fire FROM guilds WHERE id = ?")) {

                ps.setInt(1, guildId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        GuildEntity guild = mapGuildFromResultSet(rs);
                        loadGuildMembers(guild, connection);
                        return guild;
                    }
                }

                return null;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas wyszukiwania gildii: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Znajduje gildię po tagu
     */
    public CompletableFuture<GuildEntity> findByTag(String tag) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, tag, name, leader_uuid, created_at, points, friendly_fire FROM guilds WHERE tag = ?")) {

                ps.setString(1, tag);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        GuildEntity guild = mapGuildFromResultSet(rs);
                        loadGuildMembers(guild, connection);
                        return guild;
                    }
                }

                return null;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas wyszukiwania gildii po tagu: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Znajduje gildię po nazwie
     */
    public CompletableFuture<GuildEntity> findByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, tag, name, leader_uuid, created_at, points, friendly_fire FROM guilds WHERE name = ?")) {

                ps.setString(1, name);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        GuildEntity guild = mapGuildFromResultSet(rs);
                        loadGuildMembers(guild, connection);
                        return guild;
                    }
                }

                return null;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas wyszukiwania gildii po nazwie: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Znajduje gildię gracza
     */
    public CompletableFuture<GuildEntity> findByPlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT g.id, g.tag, g.name, g.leader_uuid, g.created_at, g.points, g.friendly_fire "
                    + "FROM guilds g "
                    + "JOIN guild_members gm ON g.id = gm.guild_id "
                    + "WHERE gm.player_uuid = ?")) {

                ps.setBytes(1, UUIDUtil.uuidToBytes(playerUuid));

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        GuildEntity guild = mapGuildFromResultSet(rs);
                        loadGuildMembers(guild, connection);
                        return guild;
                    }
                }

                return null;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas wyszukiwania gildii gracza: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Pobiera wszystkie gildie
     */
    public CompletableFuture<List<GuildEntity>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<GuildEntity> guilds = new ArrayList<>();

            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, tag, name, leader_uuid, created_at, points, friendly_fire FROM guilds ORDER BY created_at DESC")) {

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        GuildEntity guild = mapGuildFromResultSet(rs);
                        loadGuildMembers(guild, connection);
                        guilds.add(guild);
                    }
                }

                return guilds;

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas pobierania wszystkich gildii: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Aktualizuje friendly fire dla gildii
     */
    public CompletableFuture<Void> updateFriendlyFire(int guildId, boolean friendlyFire) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "UPDATE guilds SET friendly_fire = ? WHERE id = ?")) {

                ps.setBoolean(1, friendlyFire);
                ps.setInt(2, guildId);
                ps.executeUpdate();

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas aktualizacji friendly fire: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Usuwa gildię (wraz ze wszystkimi powiązanymi danymi)
     */
    public CompletableFuture<Void> deleteGuild(int guildId) {
        return databaseManager.executeTransaction(connection -> {
            // Usuwanie zaproszeń do gildii
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM join_invites WHERE guild_id = ?")) {
                ps.setInt(1, guildId);
                ps.executeUpdate();
            }

            // Usuwanie zaproszeń do sojuszy
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM alliance_invites WHERE from_guild_id = ? OR to_guild_id = ?")) {
                ps.setInt(1, guildId);
                ps.setInt(2, guildId);
                ps.executeUpdate();
            }

            // Usuwanie sojuszy
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM guild_alliances WHERE guild_id_a = ? OR guild_id_b = ?")) {
                ps.setInt(1, guildId);
                ps.setInt(2, guildId);
                ps.executeUpdate();
            }

            // Usuwanie członków (CASCADE powinno to zrobić automatycznie, ale na wszelki wypadek)
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM guild_members WHERE guild_id = ?")) {
                ps.setInt(1, guildId);
                ps.executeUpdate();
            }

            // Usuwanie gildii
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM guilds WHERE id = ?")) {
                ps.setInt(1, guildId);
                ps.executeUpdate();
            }
        });
    }

    /**
     * Sprawdza czy gildia o podanej nazwie lub tagu już istnieje
     */
    public CompletableFuture<Boolean> exists(String name, String tag) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = databaseManager.getConnection(); PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM guilds WHERE name = ? OR tag = ?")) {

                ps.setString(1, name);
                ps.setString(2, tag);

                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }

            } catch (Exception e) {
                throw new RuntimeException("Błąd podczas sprawdzania istnienia gildii: " + e.getMessage(), e);
            }
        }, databaseManager.getAsyncExecutor());
    }

    /**
     * Mapuje ResultSet na GuildEntity
     */
    private GuildEntity mapGuildFromResultSet(ResultSet rs) throws SQLException {
        GuildEntity guild = new GuildEntity();
        guild.setId(rs.getInt("id"));
        guild.setTag(rs.getString("tag"));
        guild.setName(rs.getString("name"));
        guild.setLeaderUuid(UUIDUtil.bytesToUuid(rs.getBytes("leader_uuid")));
        guild.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        guild.setPoints(rs.getInt("points"));
        guild.setFriendlyFire(rs.getBoolean("friendly_fire"));
        return guild;
    }

    /**
     * Ładuje członków gildii
     */
    private void loadGuildMembers(GuildEntity guild, Connection connection) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT id, player_uuid, role, joined_at FROM guild_members WHERE guild_id = ?")) {

            ps.setInt(1, guild.getId());

            try (ResultSet rs = ps.executeQuery()) {
                Set<GuildMemberEntity> members = new HashSet<>();

                while (rs.next()) {
                    GuildMemberEntity member = new GuildMemberEntity();
                    member.setId(rs.getInt("id"));
                    member.setGuildId(guild.getId());
                    member.setPlayerUuid(UUIDUtil.bytesToUuid(rs.getBytes("player_uuid")));
                    member.setRole(MemberRole.fromString(rs.getString("role")));
                    member.setJoinedAt(rs.getTimestamp("joined_at").toLocalDateTime());

                    members.add(member);
                }

                guild.setMembers(members);
            }
        }
    }
}
