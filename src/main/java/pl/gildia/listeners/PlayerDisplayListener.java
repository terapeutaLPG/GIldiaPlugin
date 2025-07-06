package pl.gildia.listeners;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import pl.gildia.GildiaPlugin;
import pl.gildia.managers.GildiaManager;
import pl.gildia.models.Gildia;

public class PlayerDisplayListener implements Listener {

    private final GildiaPlugin plugin;
    private final GildiaManager gildiaManager;

    public PlayerDisplayListener(GildiaPlugin plugin) {
        this.plugin = plugin;
        this.gildiaManager = plugin.getGildiaManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updatePlayerDisplayName(player);
    }

    public void updatePlayerDisplayName(Player player) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());

        if (gildia != null) {
            // Ustaw tag gildii przed nickiem
            String displayName = ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.RESET + player.getName();
            player.setDisplayName(displayName);
            player.setPlayerListName(displayName);

            // Użyj scoreboardu do ustawienia prefiksu
            updateScoreboardTeam(player, gildia);
        } else {
            // Resetuj display name jeśli gracz nie ma gildii
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            removeFromScoreboardTeam(player);
        }
    }

    private void updateScoreboardTeam(Player player, Gildia gildia) {
        Scoreboard scoreboard = player.getServer().getScoreboardManager().getMainScoreboard();
        String teamName = "gildia_" + gildia.getTag().toLowerCase();

        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            team.setPrefix(ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.RESET);
        }

        team.addEntry(player.getName());
    }

    private void removeFromScoreboardTeam(Player player) {
        Scoreboard scoreboard = player.getServer().getScoreboardManager().getMainScoreboard();

        // Usuń gracza ze wszystkich drużyn
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith("gildia_")) {
                team.removeEntry(player.getName());
            }
        }
    }

    public void updateAllPlayersDisplayNames() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerDisplayName(player);
        }
    }
}
