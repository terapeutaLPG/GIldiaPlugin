package pl.gildia.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import pl.gildia.GildiaPlugin;
import pl.gildia.managers.GildiaManager;
import pl.gildia.models.Gildia;

public class TagDebugListener implements Listener {

    private final GildiaPlugin plugin;
    private final GildiaManager gildiaManager;

    public TagDebugListener(GildiaPlugin plugin) {
        this.plugin = plugin;
        this.gildiaManager = plugin.getGildiaManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Debug info - sprawdź czy gracz ma gildię
        new BukkitRunnable() {
            @Override
            public void run() {
                Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
                if (gildia != null) {
                    plugin.getLogger().info("DEBUG: Gracz " + player.getName() + " ma gildię: " + gildia.getNazwa() + " [" + gildia.getTag() + "]");
                    plugin.getLogger().info("DEBUG: DisplayName: " + player.getDisplayName());
                    plugin.getLogger().info("DEBUG: PlayerListName: " + player.getPlayerListName());
                    plugin.getLogger().info("DEBUG: Scoreboard: " + player.getScoreboard().toString());
                } else {
                    plugin.getLogger().info("DEBUG: Gracz " + player.getName() + " nie ma gildii");
                }
            }
        }.runTaskLater(plugin, 40L); // 2 sekundy opóźnienia
    }
}
