package pl.gildia.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import pl.gildia.GildiaPlugin;
import pl.gildia.models.Gildia;

public class PvPListener implements Listener {

    private final GildiaPlugin plugin;

    public PvPListener(GildiaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Sprawdź czy to PvP
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Sprawdź czy obaj gracze należą do tej samej gildii
        Gildia victimGildia = plugin.getGildiaManager().getGildiaByPlayer(victim.getUniqueId());
        Gildia attackerGildia = plugin.getGildiaManager().getGildiaByPlayer(attacker.getUniqueId());

        if (victimGildia == null || attackerGildia == null) {
            return; // Jeden z graczy nie należy do gildii
        }

        // Sprawdź czy to ta sama gildia
        if (!victimGildia.equals(attackerGildia)) {
            return; // Różne gildie - można atakować
        }

        // Ta sama gildia - sprawdź ustawienia friendly fire
        if (victimGildia.isFriendlyFireDisabled()) {
            event.setCancelled(true);
            attacker.sendMessage("§cNie możesz atakować członków swojej gildii! Friendly fire jest wyłączone.");
        }
    }
}
