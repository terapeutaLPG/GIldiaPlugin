package pl.gildia.database.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity reprezentujące gildię w bazie danych
 */
public class GuildEntity {

    private int id;
    private String tag;
    private String name;
    private UUID leaderUuid;
    private LocalDateTime createdAt;
    private int points;
    private boolean friendlyFire;

    // Cache członków (ładowane z relacji)
    private Set<GuildMemberEntity> members = new HashSet<>();
    private Set<Integer> alliances = new HashSet<>();
    private Set<Integer> allianceInvites = new HashSet<>();

    public GuildEntity() {
    }

    public GuildEntity(String tag, String name, UUID leaderUuid) {
        this.tag = tag;
        this.name = name;
        this.leaderUuid = leaderUuid;
        this.createdAt = LocalDateTime.now();
        this.points = 0;
        this.friendlyFire = true;
    }

    // Gettery i settery
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(UUID leaderUuid) {
        this.leaderUuid = leaderUuid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public Set<GuildMemberEntity> getMembers() {
        return members;
    }

    public void setMembers(Set<GuildMemberEntity> members) {
        this.members = members;
    }

    public Set<Integer> getAlliances() {
        return alliances;
    }

    public void setAlliances(Set<Integer> alliances) {
        this.alliances = alliances;
    }

    public Set<Integer> getAllianceInvites() {
        return allianceInvites;
    }

    public void setAllianceInvites(Set<Integer> allianceInvites) {
        this.allianceInvites = allianceInvites;
    }

    // Utility methods
    public boolean isMember(UUID playerUuid) {
        return members.stream().anyMatch(m -> m.getPlayerUuid().equals(playerUuid));
    }

    public boolean isLeader(UUID playerUuid) {
        return leaderUuid.equals(playerUuid);
    }

    public boolean isDeputy(UUID playerUuid) {
        return members.stream().anyMatch(m
                -> m.getPlayerUuid().equals(playerUuid) && m.getRole() == MemberRole.DEPUTY);
    }

    public boolean canManage(UUID playerUuid) {
        return isLeader(playerUuid) || isDeputy(playerUuid);
    }

    public int getMemberCount() {
        return members.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GuildEntity)) {
            return false;
        }
        GuildEntity other = (GuildEntity) obj;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("Guild[id=%d, tag=%s, name=%s, leader=%s, members=%d]",
                id, tag, name, leaderUuid, members.size());
    }
}
