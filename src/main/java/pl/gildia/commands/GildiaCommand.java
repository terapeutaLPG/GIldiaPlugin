package pl.gildia.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import pl.gildia.GildiaPlugin;
import pl.gildia.managers.GildiaManager;
import pl.gildia.models.Gildia;

public class GildiaCommand implements CommandExecutor, TabCompleter {

    private final GildiaPlugin plugin;
    private final GildiaManager gildiaManager;

    public GildiaCommand(GildiaPlugin plugin) {
        this.plugin = plugin;
        this.gildiaManager = plugin.getGildiaManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Ta komenda może być używana tylko przez graczy!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "zaloz":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPoprawne użycie: /gildia zaloz <tag> <nazwa>"));
                    return true;
                }
                handleCreateGildia(player, args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                break;

            case "info":
                if (args.length == 1) {
                    handleOwnGildiaInfo(player);
                } else {
                    handleGildiaInfo(player, args[1]);
                }
                break;

            case "infogracz":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPoprawne użycie: /gildia infogracz <gracz>"));
                    return true;
                }
                Player targetInfo = Bukkit.getPlayer(args[1]);
                if (targetInfo == null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cGracz jest offline."));
                    return true;
                }
                handlePlayerInfo(player, targetInfo);
                break;

            case "usun":
                handleDeleteGildia(player, args.length > 1 && args[1].equalsIgnoreCase("potwierdz"));
                break;

            case "zapros":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPoprawne użycie: /gildia zapros <gracz>"));
                    return true;
                }
                handleInvitePlayer(player, args[1]);
                break;

            case "dolacz":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPoprawne użycie: /gildia dolacz <nazwa gildii>"));
                    return true;
                }
                handleJoinGildia(player, args[1]);
                break;

            case "wyrzuc":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPoprawne użycie: /gildia wyrzuc <gracz>"));
                    return true;
                }
                handleKickPlayer(player, args[1]);
                break;

            case "opusc":
                handleLeaveGildia(player);
                break;

            case "zastepca":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPoprawne użycie: /gildia zastepca <dodaj|usun> <gracz>"));
                    return true;
                }
                handleDeputy(player, args[1], args[2]);
                break;

            case "sojusz":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPoprawne użycie: /gildia sojusz <zapros|akceptuj|odrzuc|usun> <gildia>"));
                    return true;
                }
                handleAlliance(player, args[1], args[2]);
                break;

            case "adminusun":
                if (!player.hasPermission("gildia.admin")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie masz uprawnień."));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPoprawne użycie: /gildia adminusun <nazwa_gildii> <powód>"));
                    return true;
                }
                handleAdminDeleteGildia(player, args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                break;

            case "pkt":
                if (!player.hasPermission("gildia.pkt")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie masz uprawnień."));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPoprawne użycie: /gildia pkt <gracz>"));
                    return true;
                }
                Player targetPkt = Bukkit.getPlayer(args[1]);
                if (targetPkt == null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cGracz jest offline."));
                    return true;
                }
                Gildia gildiaPkt = gildiaManager.getGildiaByPlayer(targetPkt.getUniqueId());
                if (gildiaPkt == null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cGracz nie jest w żadnej gildii."));
                    return true;
                }
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d" + targetPkt.getName() + " &5posiada &d" + gildiaPkt.getPunktyGracza(targetPkt.getUniqueId()) + " &5punktów."));
                break;

            case "ustawpkt":
                if (!player.hasPermission("gildia.admin")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie masz uprawnień."));
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPoprawne użycie: /gildia ustawpkt <gracz> <ilość>"));
                    return true;
                }
                Player targetSetPkt = Bukkit.getPlayer(args[1]);
                if (targetSetPkt == null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cGracz jest offline."));
                    return true;
                }
                Gildia gildiaSetPkt = gildiaManager.getGildiaByPlayer(targetSetPkt.getUniqueId());
                if (gildiaSetPkt == null) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cGracz nie jest w żadnej gildii."));
                    return true;
                }
                try {
                    int pkt = Integer.parseInt(args[2]);
                    gildiaSetPkt.setPunktyGracza(targetSetPkt.getUniqueId(), pkt);
                    gildiaManager.saveGildie();
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5Ustawiono &d" + pkt + " &5pkt dla gracza &d" + targetSetPkt.getName()));
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cIlość punktów musi być liczbą."));
                }
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleCreateGildia(Player player, String tag, String nazwa) {
        if (tag.length() < 2 || tag.length() > 4) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cTag musi mieć od 2 do 4 znaków."));
            return;
        }

        if (gildiaManager.createGildia(nazwa, tag, player.getUniqueId())) {
            // Wiadomość o sukcesie jest wysyłana globalnie z GildiaManager
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cTaka gildia już istnieje lub jesteś już w innej gildii."));
        }
    }

    private void handleOwnGildiaInfo(Player player) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie należysz do żadnej gildii."));
            return;
        }
        showGildiaInfo(player, gildia);
    }

    private void handleGildiaInfo(Player player, String nazwa) {
        Gildia gildia = gildiaManager.getGildia(nazwa);
        if (gildia == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cGildia o tej nazwie nie istnieje."));
            return;
        }
        showGildiaInfo(player, gildia);
    }

    private void showGildiaInfo(Player player, Gildia gildia) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5═══════════════════════════════"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&dInformacje o gildii:"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&fNazwa: &d" + gildia.getNazwa()));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&fTag: &d[" + gildia.getTag() + "]"));

        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&fData założenia: &d" + dateFormat.format(gildia.getDataZalozenia())));

        OfflinePlayer lider = Bukkit.getOfflinePlayer(gildia.getLider());
        String liderName = lider.getName() != null ? lider.getName() : "Nieznany";
        String liderStatus = (lider.isOnline()) ? "&a (Online)" : "&c (Offline)";
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&fLider: &d" + liderName + liderStatus));

        if (!gildia.getZastepcy().isEmpty()) {
            StringBuilder zastepcyList = new StringBuilder();
            for (UUID uuid : gildia.getZastepcy()) {
                OfflinePlayer zastepca = Bukkit.getOfflinePlayer(uuid);
                String zastepcaName = zastepca.getName() != null ? zastepca.getName() : "Nieznany";
                String zastepcaStatus = (zastepca.isOnline()) ? "&a(Online)" : "&c(Offline)";
                zastepcyList.append("&d").append(zastepcaName).append(" ").append(zastepcaStatus).append(", ");
            }
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&fZastępcy: " + zastepcyList.substring(0, zastepcyList.length() - 2)));
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&fPunkty: &d" + gildia.getPunkty()));

        if (!gildia.getSojusze().isEmpty()) {
            StringBuilder sojuszeList = new StringBuilder();
            for (String sojuszNazwa : gildia.getSojusze()) {
                Gildia sojusz = gildiaManager.getGildia(sojuszNazwa);
                if (sojusz != null) {
                    sojuszeList.append("&d").append(sojusz.getNazwa()).append(" [&d").append(sojusz.getTag()).append("&d], ");
                }
            }
            if (sojuszeList.length() > 2) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&fSojusze: " + sojuszeList.substring(0, sojuszeList.length() - 2)));
            }
        }

        if (gildia.czyLider(player.getUniqueId()) && !gildia.getZaproszeniaSojuszy().isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&fOczekujące zaproszenia do sojuszu: &d" + String.join(", ", gildia.getZaproszeniaSojuszy())));
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&fWszyscy członkowie (" + gildia.getCzlonkowie().size() + "):"));
        for (UUID uuid : gildia.getCzlonkowie()) {
            OfflinePlayer członek = Bukkit.getOfflinePlayer(uuid);
            String ranga = "Czlonek";
            if (gildia.czyLider(uuid)) {
                ranga = "Lider"; 
            }else if (gildia.czyZastepca(uuid)) {
                ranga = "Zastepca";
            }

            String czlonekName = członek.getName() != null ? członek.getName() : "Nieznany";
            String czlonekStatus = (członek.isOnline()) ? "&a(Online)" : "&c(Offline)";
            int pkt = gildia.getPunktyGracza(uuid);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8- &d" + czlonekName + " &7(&f" + ranga + "&7) &7- &f" + pkt + " pkt " + czlonekStatus));
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5═══════════════════════════════"));
    }

    private void handleInvitePlayer(Player player, String targetName) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null || !gildia.czyMozeZarzadzac(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie masz uprawnień w gildii, aby to zrobić."));
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cGracz jest offline."));
            return;
        }

        if (gildiaManager.getGildiaByPlayer(target.getUniqueId()) != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cTen gracz jest już w innej gildii."));
            return;
        }

        gildiaManager.addInvite(target.getUniqueId(), gildia.getNazwa());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5Wysłano zaproszenie do gildii do gracza &d" + target.getName()));
        target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5Otrzymałeś zaproszenie do gildii &d" + gildia.getNazwa() + "&5. Wpisz &d/gildia dolacz " + gildia.getNazwa() + " &5aby dołączyć."));
    }

    private void handleJoinGildia(Player player, String gildiaName) {
        String invite = gildiaManager.getInvite(player.getUniqueId());
        if (invite == null || !invite.equalsIgnoreCase(gildiaName)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie masz zaproszenia do tej gildii."));
            return;
        }

        if (gildiaManager.addPlayerToGildia(player.getUniqueId(), gildiaName)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5Dołączyłeś do gildii &d" + gildiaName));
            gildiaManager.removeInvite(player.getUniqueId());
            Gildia gildia = gildiaManager.getGildia(gildiaName);
            if (gildia != null) {
                gildia.broadcastToMembers("&d" + player.getName() + " &5dołączył do gildii!");
            }
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cWystąpił błąd podczas dołączania do gildii."));
        }
    }

    private void handleKickPlayer(Player player, String targetName) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null || !gildia.czyMozeZarzadzac(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie masz uprawnień w gildii, aby to zrobić."));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!gildia.czyCzlonek(target.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cTego gracza nie ma w Twojej gildii."));
            return;
        }

        if (gildia.czyLider(target.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie możesz wyrzucić lidera gildii."));
            return;
        }

        if (gildia.czyZastepca(target.getUniqueId()) && !gildia.czyLider(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cJako zastępca nie możesz wyrzucić innego zastępcy."));
            return;
        }

        if (gildiaManager.removePlayerFromGildia(target.getUniqueId())) {
            gildia.broadcastToMembers("&d" + target.getName() + " &5został wyrzucony z gildii przez &d" + player.getName());
            if (target.isOnline()) {
                ((Player) target).sendMessage(ChatColor.translateAlternateColorCodes('&', "&cZostałeś wyrzucony z gildii."));
            }
        }
    }

    private void handleLeaveGildia(Player player) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie należysz do żadnej gildii."));
            return;
        }

        if (gildia.czyLider(player.getUniqueId()) && gildia.getCzlonkowie().size() > 1) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie możesz opuścić gildii, będąc liderem. Najpierw przekaż przywództwo lub usuń gildię."));
            return;
        }

        if (gildiaManager.removePlayerFromGildia(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5Opuściłeś gildię."));
            gildia.broadcastToMembers("&d" + player.getName() + " &5opuścił gildię.");
        }
    }

    private void handleDeputy(Player player, String action, String targetName) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null || !gildia.czyLider(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie jesteś liderem gildii."));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!gildia.czyCzlonek(target.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cTen gracz nie jest w Twojej gildii."));
            return;
        }

        if (action.equalsIgnoreCase("dodaj")) {
            if (gildia.czyZastepca(target.getUniqueId())) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cTen gracz jest już zastępcą."));
                return;
            }
            gildia.dodajZastepce(target.getUniqueId());
            gildiaManager.saveGildie();
            gildia.broadcastToMembers("&d" + target.getName() + " &5został mianowany na zastępcę przez &d" + player.getName());
        } else if (action.equalsIgnoreCase("usun")) {
            if (!gildia.czyZastepca(target.getUniqueId())) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cTen gracz nie jest zastępcą."));
                return;
            }
            gildia.usunZastepce(target.getUniqueId());
            gildiaManager.saveGildie();
            gildia.broadcastToMembers("&d" + target.getName() + " &5został zdegradowany z zastępcy przez &d" + player.getName());
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPoprawne użycie: /gildia zastepca <dodaj|usun> <gracz>"));
        }
    }

    private void handleAlliance(Player player, String action, String targetGildiaName) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null || !gildia.czyLider(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie jesteś liderem gildii."));
            return;
        }

        Gildia targetGildia = gildiaManager.getGildia(targetGildiaName);
        if (targetGildia == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cGildia o tej nazwie nie istnieje."));
            return;
        }

        if (gildia.getNazwa().equalsIgnoreCase(targetGildia.getNazwa())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie możesz zawrzeć sojuszu z własną gildią."));
            return;
        }

        switch (action.toLowerCase()) {
            case "zapros":
                if (gildia.getSojusze().contains(targetGildia.getNazwa().toLowerCase())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cMasz już sojusz z tą gildią."));
                    return;
                }
                targetGildia.dodajZaproszenieSojusz(gildia.getNazwa());
                gildiaManager.saveGildie();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5Wysłano zaproszenie do sojuszu do gildii &d" + targetGildia.getNazwa()));
                Player targetLider = Bukkit.getPlayer(targetGildia.getLider());
                if (targetLider != null) {
                    targetLider.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5Gildia &d" + gildia.getNazwa() + " &5chce zawrzeć z wami sojusz! Użyj &d/gildia sojusz akceptuj " + gildia.getNazwa()));
                }
                break;
            case "akceptuj":
                if (!gildia.czyMaZaproszenieSojusz(targetGildia.getNazwa())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie masz zaproszenia do sojuszu od tej gildii."));
                    return;
                }
                gildia.dodajSojusz(targetGildia.getNazwa());
                targetGildia.dodajSojusz(gildia.getNazwa());
                gildia.usunZaproszenieSojusz(targetGildia.getNazwa());
                gildiaManager.saveGildie();
                gildia.broadcastToMembers("&5Gildia &d" + gildia.getNazwa() + " &5zawarła sojusz z &d" + targetGildia.getNazwa());
                targetGildia.broadcastToMembers("&5Gildia &d" + targetGildia.getNazwa() + " &5zawarła sojusz z &d" + gildia.getNazwa());
                break;
            case "odrzuc":
                if (!gildia.czyMaZaproszenieSojusz(targetGildia.getNazwa())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie masz zaproszenia do sojuszu od tej gildii."));
                    return;
                }
                gildia.usunZaproszenieSojusz(targetGildia.getNazwa());
                gildiaManager.saveGildie();
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5Odrzucono zaproszenie do sojuszu od gildii &d" + targetGildia.getNazwa()));
                break;
            case "usun":
                if (!gildia.getSojusze().contains(targetGildia.getNazwa().toLowerCase())) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie masz sojuszu z tą gildią."));
                    return;
                }
                gildia.usunSojusz(targetGildia.getNazwa());
                targetGildia.usunSojusz(gildia.getNazwa());
                gildiaManager.saveGildie();
                gildia.broadcastToMembers("&dTwoja gildia zerwała sojusz z &d" + targetGildia.getNazwa());
                targetGildia.broadcastToMembers("&dGildia &d" + gildia.getNazwa() + " &dzerwała z wami sojusz.");
                break;
            default:
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cPoprawne użycie: /gildia sojusz <zapros|akceptuj|odrzuc|usun> <gildia>"));
                break;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&m-----------------&d GildiaPlugin &5&m-----------------"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/gildia zaloz <tag> <nazwa> &7- Tworzy nową gildię"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/gildia info [nazwa] &7- Informacje o gildii"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/gildia infogracz <gracz> &7- Informacje o graczu"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/gildia zapros <gracz> &7- Zaprasza gracza do gildii"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/gildia dolacz <nazwa> &7- Akceptuje zaproszenie do gildii"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/gildia wyrzuc <gracz> &7- Wyrzuca gracza z gildii"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/gildia opusc &7- Opuszcza gildię"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/gildia usun &7- Usuwa Twoją gildię"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/gildia zastepca <dodaj|usun> <gracz> &7- Zarządza zastępcami"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&d/gildia sojusz <zapros|akceptuj|odrzuc|usun> <gildia> &7- Zarządza sojuszami"));
        if (player.hasPermission("gildia.admin")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c/gildia adminusun <nazwa> <powod> &7- Usuwa gildię jako admin"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c/gildia ustawpkt <gracz> <ilosc> &7- Ustawia punkty gracza"));
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5&m--------------------------------------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("zaloz", "info", "infogracz", "zapros", "dolacz", "wyrzuc", "opusc", "usun", "zastepca", "sojusz", "adminusun", "pkt", "ustawpkt");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info")) {
                return new ArrayList<>(gildiaManager.getAllGildie().keySet());
            }
            if (args[0].equalsIgnoreCase("sojusz")) {
                return Arrays.asList("zapros", "akceptuj", "odrzuc", "usun");
            }
            if (args[0].equalsIgnoreCase("zastepca")) {
                return Arrays.asList("dodaj", "usun");
            }
        }
        return null;
    }

    private void handleDeleteGildia(Player player, boolean confirmed) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null || !gildia.czyLider(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie jesteś liderem żadnej gildii."));
            return;
        }

        if (!confirmed) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lJesteś pewien? &7Użyj &c/gildia usun potwierdz&7, aby na zawsze usunąć gildię. Tej akcji nie można cofnąć."));
            return;
        }

        gildiaManager.deleteGildia(gildia.getNazwa());
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&5Gildia &d" + gildia.getNazwa() + " &5została rozwiązana przez lidera."));
    }

    private void handlePlayerInfo(Player player, Player targetPlayer) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(targetPlayer.getUniqueId());
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5══════════ &dInformacje o graczu &5══════════"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&fGracz: &d" + targetPlayer.getName()));
        if (gildia != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&fGildia: &d" + gildia.getNazwa() + " [" + gildia.getTag() + "]"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&fPunkty: &d" + gildia.getPunktyGracza(targetPlayer.getUniqueId())));
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&fGildia: &7Brak"));
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&5═════════════════════════════════════════"));
    }

    private void handleAdminDeleteGildia(Player player, String gildiaName, String powod) {
        if (!player.hasPermission("gildia.admin")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cNie masz uprawnień."));
            return;
        }
        Gildia gildia = gildiaManager.getGildia(gildiaName);
        if (gildia == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cGildia o tej nazwie nie istnieje."));
            return;
        }
        gildiaManager.deleteGildia(gildiaName);
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', "&c&lGildia &4" + gildia.getNazwa() + " &c&lzostała usunięta przez administratora &4" + player.getName() + "&c&l. Powód: &4" + powod));
    }
}
