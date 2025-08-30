package pl.gildia.database.entities;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity reprezentujące członka gildii w bazie danych
 */
public class GuildMemberEntity {

    private int id;
    private int guildId;
    private UUID playerUuid;
    private MemberRole role;
    private LocalDateTime joinedAt;

    public GuildMemberEntity() {
    }

    public GuildMemberEntity(int guildId, UUID playerUuid, MemberRole role) {
        this.guildId = guildId;
        this.playerUuid = playerUuid;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

    // Gettery i settery
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGuildId() {
        return guildId;
    }

    public void setGuildId(int guildId) {
        this.guildId = guildId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    public MemberRole getRole() {
        return role;
    }

    public void setRole(MemberRole role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GuildMemberEntity)) {
            return false;
        }
        GuildMemberEntity other = (GuildMemberEntity) obj;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("Member[id=%d, guildId=%d, player=%s, role=%s]",
                id, guildId, playerUuid, role);
    }
}
