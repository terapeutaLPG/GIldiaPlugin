package pl.gildia.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import pl.gildia.GildiaPlugin;
import pl.gildia.managers.GildiaManager;
import pl.gildia.models.Gildia;

public class PlayerDisplayListener implements Listener {

    private final GildiaPlugin plugin;
    private final GildiaManager gildiaManager;
    private Scoreboard gildiaScoreboard;

    public PlayerDisplayListener(GildiaPlugin plugin) {
        this.plugin = plugin;
        this.gildiaManager = plugin.getGildiaManager();
        this.gildiaScoreboard = plugin.getServer().getScoreboardManager().getNewScoreboard();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Opóźnienie aby upewnić się że gracz jest w pełni załadowany
        new BukkitRunnable() {
            @Override
            public void run() {
                updatePlayerDisplayName(player);
            }
        }.runTaskLater(plugin, 20L); // 1 sekunda opóźnienia
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeFromScoreboardTeam(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();

        // Opóźnienie 1 sekunda żeby PvP Stats zdążył zaktualizować statystyki
        new BukkitRunnable() {
            @Override
            public void run() {
                // Aktualizuj gracza który zginął
                updatePlayerDisplayName(player);

                // Aktualizuj zabójcę jeśli istnieje
                if (killer != null) {
                    updatePlayerDisplayName(killer);
                }
            }
        }.runTaskLater(plugin, 20L); // 1 sekunda opóźnienia
    }

    public void updatePlayerDisplayName(Player player) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());

        if (gildia != null) {
            // określ rangę
            String ranga = "Czlonek";
            if (gildia.czyLider(player.getUniqueId())) {
                ranga = "Lider";
            } else if (gildia.czyZastepca(player.getUniqueId())) {
                ranga = "Zastepca";
            }

            // Sprawdź czy PlaceholderAPI i PvP Stats są dostępne
            String pvpStats = "";
            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null
                    && plugin.getServer().getPluginManager().getPlugin("PVPStats") != null) {
                try {
                    // Używaj PlaceholderAPI do pobrania statystyk PvP Stats
                    String kills = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%slipcorpvpstats_kills%");
                    String deaths = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%slipcorpvpstats_deaths%");
                    String streak = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%slipcorpvpstats_streak%");
                    pvpStats = " " + ChatColor.GOLD + "[K:" + kills + " D:" + deaths + " S:" + streak + "]";
                } catch (Exception e) {
                    // Jeśli wystąpi błąd, po prostu nie wyświetlaj statystyk
                    pvpStats = "";
                }
            }

            // Ustaw nazwę w TAB z PvP Stats: [TAG] Nick [K:X D:X S:X]
            String tabName = ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.WHITE + player.getName() + pvpStats;
            player.setPlayerListName(tabName);

            // Ustaw display name dla czatu: (ranga) [TAG] Nick [K:X D:X S:X]
            String displayName = "(" + ranga + ") " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.WHITE + player.getName() + pvpStats;
            player.setDisplayName(displayName);

            // Aktualizuj scoreboard team dla tagów nad głowami
            updateScoreboardTeam(player, gildia);
        } else {
            player.setPlayerListName(player.getName());
            player.setDisplayName(player.getName());
            removeFromScoreboardTeam(player);
        }
    }

    private void updateScoreboardTeam(Player player, Gildia gildia) {
        // Usuń gracza z poprzednich drużyn
        removeFromScoreboardTeam(player);

        // Twórz unikalną nazwę drużyny (max 16 znaków)
        String teamName = "g_" + gildia.getTag().toLowerCase();
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }

        Team team = gildiaScoreboard.getTeam(teamName);
        if (team == null) {
            team = gildiaScoreboard.registerNewTeam(teamName);

            // Ustaw prefix z tagiem gildii (max 16 znaków w starszych wersjach)
            String prefix = ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.WHITE;
            if (prefix.length() > 16) {
                prefix = ChatColor.AQUA + "[" + gildia.getTag() + "]" + ChatColor.WHITE;
            }
            team.setPrefix(prefix);

            // Ustaw opcje drużyny
            team.setCanSeeFriendlyInvisibles(false);
            team.setAllowFriendlyFire(true);
        }

        // ZAWSZE aktualizuj suffix z najnowszymi statystykami PvP
        String suffix = "";
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null
                && plugin.getServer().getPluginManager().getPlugin("PVPStats") != null) {
            try {
                String elo = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%slipcorpvpstats_elo%");
                suffix = ChatColor.DARK_PURPLE + " [" + elo + "]";
            } catch (Exception e) {
                suffix = "";
            }
        }
        team.setSuffix(suffix);

        // Dodaj gracza do drużyny
        team.addEntry(player.getName());

        // Ustaw scoreboard dla gracza
        player.setScoreboard(gildiaScoreboard);

        // Aktualizuj scoreboard dla wszystkich graczy
        updateScoreboardForAllPlayers();
    }

    private void removeFromScoreboardTeam(Player player) {
        // Usuń gracza ze wszystkich drużyn gildii
        for (Team team : gildiaScoreboard.getTeams()) {
            if (team.getName().startsWith("g_")) {
                team.removeEntry(player.getName());

                // Usuń pustą drużynę
                if (team.getEntries().isEmpty()) {
                    team.unregister();
                }
            }
        }
    }

    private void updateScoreboardForAllPlayers() {
        // Ustaw ten sam scoreboard dla wszystkich graczy online
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            onlinePlayer.setScoreboard(gildiaScoreboard);
        }
    }

    public void updateAllPlayersDisplayNames() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerDisplayName(player);
        }
    }

    public void refreshScoreboard() {
        // Odśwież scoreboard - usuń wszystkie drużyny i odtwórz
        for (Team team : gildiaScoreboard.getTeams()) {
            team.unregister();
        }

        updateAllPlayersDisplayNames();
    }
}
