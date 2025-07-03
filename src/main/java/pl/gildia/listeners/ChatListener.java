package pl.gildia.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import pl.gildia.GildiaPlugin;
import pl.gildia.managers.GildiaManager;
import pl.gildia.models.Gildia;

public class ChatListener implements Listener {

    private final GildiaPlugin plugin;
    private final GildiaManager gildiaManager;

    public ChatListener(GildiaPlugin plugin) {
        this.plugin = plugin;
        this.gildiaManager = plugin.getGildiaManager();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Sprawdź czy wiadomość zaczyna się od !
        if (message.startsWith("!")) {
            // Anuluj normalny czat
            event.setCancelled(true);

            // Usuń ! z wiadomości
            String gildiaMessage = message.substring(1).trim();

            if (gildiaMessage.isEmpty()) {
                return;
            }

            // Sprawdź czy gracz należy do gildii
            Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
            if (gildia == null) {
                player.sendMessage("§cNie należysz do żadnej gildii!");
                return;
            }

            // Wyślij wiadomość do czatu gildii
            gildiaManager.sendGildiaMessage(player, gildiaMessage);
        }
    }
}
