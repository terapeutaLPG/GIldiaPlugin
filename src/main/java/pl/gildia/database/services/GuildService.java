package pl.gildia.database.services;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;

import pl.gildia.GildiaPlugin;
import pl.gildia.database.DatabaseManager;
import pl.gildia.database.entities.GuildEntity;
import pl.gildia.database.entities.GuildMemberEntity;
import pl.gildia.database.entities.MemberRole;
import pl.gildia.database.repositories.AllianceRepository;
import pl.gildia.database.repositories.GuildRepository;
import pl.gildia.database.repositories.InviteRepository;
import pl.gildia.database.repositories.MemberRepository;

/**
 * Serwis do zarządzania gildiami z cache'owaniem
 */
public class GuildService {

    private final GildiaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final GuildRepository guildRepository;
    private final MemberRepository memberRepository;
    private final AllianceRepository allianceRepository;
    private final InviteRepository inviteRepository;

    // Cache
    private final Map<String, GuildEntity> guildsByName = new ConcurrentHashMap<>();
    private final Map<String, GuildEntity> guildsByTag = new ConcurrentHashMap<>();
    private final Map<UUID, GuildEntity> playerGuilds = new ConcurrentHashMap<>();

    public GuildService(GildiaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.guildRepository = new GuildRepository(databaseManager);
        this.memberRepository = new MemberRepository(databaseManager);
        this.allianceRepository = new AllianceRepository(databaseManager);
        this.inviteRepository = new InviteRepository(databaseManager);
    }

    /**
     * Inicjalizuje cache gildii
     */
    public CompletableFuture<Void> loadCache() {
        return guildRepository.findAll().thenAcceptAsync(guilds -> {
            guildsByName.clear();
            guildsByTag.clear();
            playerGuilds.clear();

            for (GuildEntity guild : guilds) {
                guildsByName.put(guild.getName().toLowerCase(), guild);
                guildsByTag.put(guild.getTag().toLowerCase(), guild);

                // Dodaj wszystkich członków do cache
                for (GuildMemberEntity member : guild.getMembers()) {
                    playerGuilds.put(member.getPlayerUuid(), guild);
                }
            }

            plugin.getLogger().info("Załadowano " + guilds.size() + " gildii do cache");
        }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    /**
     * Tworzy nową gildię
     */
    public CompletableFuture<Boolean> createGuild(String tag, String name, UUID leaderUuid) {
        // Sprawdź limit 64 członków (teoretycznie nie dotyczy nowej gildii, ale zostawiamy dla spójności)
        return guildRepository.exists(name, tag).thenCompose(exists -> {
            if (exists) {
                return CompletableFuture.completedFuture(false);
            }

            // Sprawdź czy gracz już nie należy do gildii
            if (playerGuilds.containsKey(leaderUuid)) {
                return CompletableFuture.completedFuture(false);
            }

            return guildRepository.createGuild(tag, name, leaderUuid).thenApplyAsync(guild -> {
                if (guild != null) {
                    // Aktualizuj cache
                    guildsByName.put(name.toLowerCase(), guild);
                    guildsByTag.put(tag.toLowerCase(), guild);
                    playerGuilds.put(leaderUuid, guild);

                    // Aktualizuj display name na main thread
                    Bukkit.getScheduler().runTask(plugin, ()
                            -> updatePlayerDisplayName(leaderUuid));

                    return true;
                }
                return false;
            }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
        });
    }

    /**
     * Dodaje gracza do gildii
     */
    public CompletableFuture<Boolean> addPlayerToGuild(String guildName, UUID playerUuid, UUID inviterUuid) {
        GuildEntity guild = guildsByName.get(guildName.toLowerCase());
        if (guild == null) {
            return CompletableFuture.completedFuture(false);
        }

        // Sprawdź limit 64 członków
        if (guild.getMemberCount() >= 64) {
            return CompletableFuture.completedFuture(false);
        }

        // Sprawdź czy gracz już nie należy do gildii
        if (playerGuilds.containsKey(playerUuid)) {
            return CompletableFuture.completedFuture(false);
        }

        return memberRepository.addMember(guild.getId(), playerUuid, MemberRole.MEMBER)
                .thenCompose(member -> {
                    if (member != null) {
                        // Usuń zaproszenie
                        return inviteRepository.removeJoinInvite(guild.getId(), playerUuid)
                                .thenApplyAsync(removed -> {
                                    // Aktualizuj cache
                                    guild.getMembers().add(member);
                                    playerGuilds.put(playerUuid, guild);

                                    // Aktualizuj display name na main thread
                                    Bukkit.getScheduler().runTask(plugin, ()
                                            -> updatePlayerDisplayName(playerUuid));

                                    return true;
                                }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
                    }
                    return CompletableFuture.completedFuture(false);
                });
    }

    /**
     * Usuwa gracza z gildii
     */
    public CompletableFuture<Boolean> removePlayerFromGuild(UUID playerUuid) {
        GuildEntity guild = playerGuilds.get(playerUuid);
        if (guild == null) {
            return CompletableFuture.completedFuture(false);
        }

        return memberRepository.removeMember(guild.getId(), playerUuid)
                .thenApplyAsync(success -> {
                    if (success) {
                        // Aktualizuj cache
                        guild.getMembers().removeIf(m -> m.getPlayerUuid().equals(playerUuid));
                        playerGuilds.remove(playerUuid);

                        // Aktualizuj display name na main thread
                        Bukkit.getScheduler().runTask(plugin, ()
                                -> updatePlayerDisplayName(playerUuid));
                    }
                    return success;
                }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    /**
     * Usuwa gildię (transakcja)
     */
    public CompletableFuture<Void> deleteGuild(int guildId) {
        GuildEntity guild = guildsByName.values().stream()
                .filter(g -> g.getId() == guildId)
                .findFirst()
                .orElse(null);

        if (guild == null) {
            return CompletableFuture.completedFuture(null);
        }

        return guildRepository.deleteGuild(guildId).thenRunAsync(() -> {
            // Usuń z cache
            guildsByName.remove(guild.getName().toLowerCase());
            guildsByTag.remove(guild.getTag().toLowerCase());

            // Usuń wszystkich członków z cache
            for (GuildMemberEntity member : guild.getMembers()) {
                playerGuilds.remove(member.getPlayerUuid());

                // Aktualizuj display name na main thread
                Bukkit.getScheduler().runTask(plugin, ()
                        -> updatePlayerDisplayName(member.getPlayerUuid()));
            }
        }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    /**
     * Przełącza friendly fire
     */
    public CompletableFuture<Boolean> toggleFriendlyFire(UUID playerUuid) {
        GuildEntity guild = playerGuilds.get(playerUuid);
        if (guild == null || !guild.isLeader(playerUuid)) {
            return CompletableFuture.completedFuture(false);
        }

        boolean newState = !guild.isFriendlyFire();

        return guildRepository.updateFriendlyFire(guild.getId(), newState)
                .thenApplyAsync(v -> {
                    // Aktualizuj cache
                    guild.setFriendlyFire(newState);
                    return true;
                }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    /**
     * Zarządzanie zastępcami
     */
    public CompletableFuture<Boolean> setDeputy(UUID leaderUuid, UUID targetUuid, boolean makeDeputy) {
        GuildEntity guild = playerGuilds.get(leaderUuid);
        if (guild == null || !guild.isLeader(leaderUuid)) {
            return CompletableFuture.completedFuture(false);
        }

        GuildEntity targetGuild = playerGuilds.get(targetUuid);
        if (targetGuild == null || targetGuild.getId() != guild.getId()) {
            return CompletableFuture.completedFuture(false);
        }

        MemberRole newRole = makeDeputy ? MemberRole.DEPUTY : MemberRole.MEMBER;

        return memberRepository.updateMemberRole(guild.getId(), targetUuid, newRole)
                .thenApplyAsync(success -> {
                    if (success) {
                        // Aktualizuj cache
                        guild.getMembers().stream()
                                .filter(m -> m.getPlayerUuid().equals(targetUuid))
                                .findFirst()
                                .ifPresent(member -> member.setRole(newRole));
                    }
                    return success;
                }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    // Gettery dla cache
    public GuildEntity getGuildByName(String name) {
        return guildsByName.get(name.toLowerCase());
    }

    public GuildEntity getGuildByTag(String tag) {
        return guildsByTag.get(tag.toLowerCase());
    }

    public GuildEntity getPlayerGuild(UUID playerUuid) {
        return playerGuilds.get(playerUuid);
    }

    public Map<String, GuildEntity> getAllGuilds() {
        return guildsByName;
    }

    // Repository gettery dla komend
    public GuildRepository getGuildRepository() {
        return guildRepository;
    }

    public MemberRepository getMemberRepository() {
        return memberRepository;
    }

    public AllianceRepository getAllianceRepository() {
        return allianceRepository;
    }

    public InviteRepository getInviteRepository() {
        return inviteRepository;
    }

    /**
     * Aktualizuje display name gracza
     */
    private void updatePlayerDisplayName(UUID playerUuid) {
        if (plugin.getPlayerDisplayListener() != null) {
            org.bukkit.entity.Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                plugin.getPlayerDisplayListener().updatePlayerDisplayName(player);
            }
        }
    }
}
