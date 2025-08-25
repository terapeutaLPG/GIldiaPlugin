package pl.gildia.listeners;

import java.lang.reflect.Method;
import java.util.logging.Level;

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

        // Spróbuj pobrać prefix (ranga) przez Vault Chat (jeśli dostępny) - używamy reflection aby nie wymagać zależności
        String prefix = "";
        try {
            Class<?> chatClass = Class.forName("net.milkbowl.vault.chat.Chat");
            org.bukkit.plugin.RegisteredServiceProvider<?> rsp = plugin.getServer().getServicesManager().getRegistration(chatClass);
            if (rsp != null && rsp.getProvider() != null) {
                Object chatProvider = rsp.getProvider();
                Method m = null;
                try {
                    m = chatProvider.getClass().getMethod("getPlayerPrefix", String.class, String.class);
                    Object res = m.invoke(chatProvider, player.getWorld().getName(), player.getName());
                    if (res != null) {
                        prefix = res.toString();
                    }
                } catch (NoSuchMethodException ns) {
                    try {
                        m = chatProvider.getClass().getMethod("getPlayerPrefix", String.class);
                        Object res = m.invoke(chatProvider, player.getName());
                        if (res != null) {
                            prefix = res.toString();
                        }
                    } catch (NoSuchMethodException ignored) {
                    }
                }
            }
        } catch (ClassNotFoundException cnf) {
            // Vault nie jest zainstalowany - fallback
        } catch (Exception ex) {
            plugin.getLogger().log(Level.FINE, "Vault chat prefix reflection failed", ex);
        }

        // Fallback: proste permission-based ranky (konfigurowalne w LuckPerms przez przydzielanie permissionów gildia.rank.*)
        if (prefix == null || prefix.isEmpty()) {
            if (player.hasPermission("gildia.rank.admin")) {
                prefix = "[ADMIN]"; 
            }else if (player.hasPermission("gildia.rank.mod")) {
                prefix = "[MOD]"; 
            }else if (player.hasPermission("gildia.rank.vip")) {
                prefix = "[VIP]"; 
            }else {
                prefix = "";
            }
        }

        // Gildia tag
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        String tagPart = "";
        if (gildia != null) {
            tagPart = ChatColor.LIGHT_PURPLE + "[" + gildia.getTag() + "] " + ChatColor.RESET;
        }

        // Nick w kolorze szarym
        String nickPart = ChatColor.GRAY + player.getName() + ChatColor.RESET;

        // Jeśli prefix jest niepusty i różny od domyślnego, pokaż przed tagiem
        String displayPrefix = "";
        if (prefix != null && !prefix.trim().isEmpty()) {
            displayPrefix = ChatColor.GOLD + prefix + " " + ChatColor.RESET;
        }

        // Finalny format: [prefix] [TAG] nick: message
        String finalMessage = displayPrefix + tagPart + nickPart + ChatColor.WHITE + ": " + ChatColor.RESET + message;

        // Wyślij synchronnie
        String out = finalMessage;
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(out));
    }
}
