package pl.gildia.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

        // Gildia chat (wiadomości zaczynające się od '!')
        if (message.startsWith("!")) {
            event.setCancelled(true);
            String gildiaMessage = message.substring(1).trim();
            if (gildiaMessage.isEmpty()) {
                return;
            }
            Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
            if (gildia == null) {
                player.sendMessage(ChatColor.RED + "Nie należysz do żadnej gildii!");
                return;
            }
            gildiaManager.sendGildiaMessage(player, gildiaMessage);
            return;
        }

        // Normalny globalny chat - anuluj i sformatuj
        event.setCancelled(true);

        // Spróbuj pobrać prefix (ranga) przez Vault Chat (jeśli dostępny)
        String prefix = "";
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            net.milkbowl.vault.chat.Chat chat = plugin.getServer().getServicesManager().load(net.milkbowl.vault.chat.Chat.class);
            if (chat != null) {
                prefix = chat.getPlayerPrefix(player);
            }
        }

        // Gildia tag
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        String tagPart = "";
        if (gildia != null) {
            tagPart = "&d[" + gildia.getTag() + "]&r ";
        }

        String chatFormat = plugin.getConfig().getString("chat-format", "{PREFIX} {TAG}{PLAYER}&7: &f{MESSAGE}");

        String finalMessage = chatFormat
                .replace("{PREFIX}", prefix != null ? prefix : "")
                .replace("{TAG}", tagPart)
                .replace("{PLAYER}", player.getName())
                .replace("{MESSAGE}", message);

        finalMessage = ChatColor.translateAlternateColorCodes('&', finalMessage);

        // Wyślij synchronnie
        String out = finalMessage;
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(out));
    }
}
