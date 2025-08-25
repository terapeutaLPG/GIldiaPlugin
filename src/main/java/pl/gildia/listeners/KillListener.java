package pl.gildia.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import pl.gildia.GildiaPlugin;
import pl.gildia.managers.GildiaManager;
import pl.gildia.models.Gildia;

public class KillListener implements Listener {

    private final GildiaPlugin plugin;
    private final GildiaManager gildiaManager;

    public KillListener(GildiaPlugin plugin) {
        this.plugin = plugin;
        this.gildiaManager = plugin.getGildiaManager();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null) {
            return;
        }
        if (killer.getUniqueId().equals(victim.getUniqueId())) {
            return; // suicide
        }
        // award points to killer based on victim's points
        Gildia gildiaKiller = gildiaManager.getGildiaByPlayer(killer.getUniqueId());
        Gildia gildiaVictim = gildiaManager.getGildiaByPlayer(victim.getUniqueId());

        int victimPoints = 0;
        if (gildiaVictim != null) {
            victimPoints = gildiaVictim.getPunktyGracza(victim.getUniqueId());
        }

        // Calculate reward: base 5 + 10% of victim points (rounded)
        int reward = 5 + Math.round(victimPoints * 0.1f);
        int penalty = Math.round(victimPoints * 0.05f); // victim loses 5% of their points

        if (gildiaKiller != null) {
            gildiaKiller.addPunktyGracza(killer.getUniqueId(), reward);
            plugin.getLogger().info("DEBUG: Przyznano " + reward + " pkt graczowi " + killer.getName());
        }

        if (gildiaVictim != null) {
            gildiaVictim.addPunktyGracza(victim.getUniqueId(), -penalty);
            plugin.getLogger().info("DEBUG: Odebrano " + penalty + " pkt graczowi " + victim.getName());
        }

        // Zapisz gildie po zmianach
        gildiaManager.saveGildie();
        gildiaManager.updatePlayerDisplayName(killer.getUniqueId());
        gildiaManager.updatePlayerDisplayName(victim.getUniqueId());
    }
}
