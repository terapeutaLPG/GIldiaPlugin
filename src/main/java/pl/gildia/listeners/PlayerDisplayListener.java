package pl.gildia.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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

            // Ustaw tylko nick w TAB
            player.setPlayerListName(player.getName());

            // Ustaw display name dla czatu: (ranga) [TAG] Nick (pkt)
            int pkt = gildia.getPunktyGracza(player.getUniqueId());
            String displayName = "(" + ranga + ") " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.WHITE + player.getName() + ChatColor.GRAY + " (" + pkt + ")";
            player.setDisplayName(displayName);
        } else {
            player.setPlayerListName(player.getName());
            player.setDisplayName(player.getName());
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

            // Ustaw prefix (max 16 znaków w starszych wersjach)
            String prefix = ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.WHITE;
            if (prefix.length() > 16) {
                prefix = ChatColor.AQUA + "[" + gildia.getTag() + "]" + ChatColor.WHITE;
            }
            team.setPrefix(prefix);

            // Ustaw opcje drużyny
            team.setCanSeeFriendlyInvisibles(false);
            team.setAllowFriendlyFire(true);
        }

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
