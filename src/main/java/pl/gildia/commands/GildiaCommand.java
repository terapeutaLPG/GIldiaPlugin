package pl.gildia.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

        switch (args[0].toLowerCase()) {
            case "zaloz":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Użyj: /gildia zaloz <tag> <nazwa>");
                    return true;
                }
                handleCreateGildia(player, args[1], args[2]);
                break;

            case "info":
                if (args.length < 2) {
                    handleOwnGildiaInfo(player);
                } else {
                    handleGildiaInfo(player, args[1]);
                }
                break;

            case "infogracz":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użyj: /gildia infogracz <gracz>");
                    return true;
                }
                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    player.sendMessage(ChatColor.RED + "Gracz " + args[1] + " nie jest online!");
                    return true;
                }
                handlePlayerInfo(player, targetPlayer);
                break;

            case "usun":
                if (args.length < 2) {
                    handleDeleteGildia(player, false);
                } else if (args[1].equalsIgnoreCase("potwierdz")) {
                    handleDeleteGildia(player, true);
                } else {
                    player.sendMessage(ChatColor.RED + "Użyj: /gildia usun lub /gildia usun potwierdz");
                }
                break;

            case "zapros":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użyj: /gildia zapros <gracz>");
                    return true;
                }
                handleInvitePlayer(player, args[1]);
                break;

            case "wyrzuc":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Użyj: /gildia wyrzuc <gracz>");
                    return true;
                }
                handleKickPlayer(player, args[1]);
                break;

            case "opusc":
                handleLeaveGildia(player);
                break;

            case "zastepca":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Użyj: /gildia zastepca <dodaj/usun> <gracz>");
                    return true;
                }
                handleDeputy(player, args[1], args[2]);
                break;

            case "sojusz":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Użyj: /gildia sojusz <dodaj/usun> <gildia>");
                    return true;
                }
                handleAlliance(player, args[1], args[2]);
                break;

            case "adminusun":
                if (!player.hasPermission("gildia.admin")) {
                    player.sendMessage(ChatColor.RED + "Nie masz uprawnień do używania tej komendy!");
                    return true;
                }
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Użyj: /gildia adminusun <nazwa_gildii> <powód>");
                    return true;
                }
                // Łącz wszystkie argumenty od 2 jako powód
                StringBuilder powod = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    powod.append(args[i]);
                    if (i < args.length - 1) {
                        powod.append(" ");
                    }
                }
                handleAdminDeleteGildia(player, args[1], powod.toString());
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleCreateGildia(Player player, String tag, String nazwa) {
        if (tag.length() < 2 || tag.length() > 4) {
            player.sendMessage(ChatColor.RED + "Tag gildii musi mieć od 2 do 4 znaków!");
            return;
        }

        if (gildiaManager.createGildia(nazwa, tag, player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Pomyślnie założono gildię " + ChatColor.AQUA + "[" + tag + "] " + ChatColor.YELLOW + nazwa);
        } else {
            player.sendMessage(ChatColor.RED + "Nie można założyć gildii! Możliwe przyczyny:");
            player.sendMessage(ChatColor.RED + "- Już należysz do gildii");
            player.sendMessage(ChatColor.RED + "- Gildia o tej nazwie już istnieje");
            player.sendMessage(ChatColor.RED + "- Tag jest już zajęty");
        }
    }

    private void handleOwnGildiaInfo(Player player) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null) {
            player.sendMessage(ChatColor.RED + "Nie należysz do żadnej gildii!");
            return;
        }
        showGildiaInfo(player, gildia);
    }

    private void handleGildiaInfo(Player player, String nazwa) {
        Gildia gildia = gildiaManager.getGildia(nazwa);
        if (gildia == null) {
            player.sendMessage(ChatColor.RED + "Gildia o nazwie " + nazwa + " nie istnieje!");
            return;
        }
        showGildiaInfo(player, gildia);
    }

    private void showGildiaInfo(Player player, Gildia gildia) {
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
        player.sendMessage(ChatColor.AQUA + "Informacje o gildii:");
        player.sendMessage(ChatColor.YELLOW + "Nazwa: " + ChatColor.WHITE + gildia.getNazwa());
        player.sendMessage(ChatColor.YELLOW + "Tag: " + ChatColor.AQUA + "[" + gildia.getTag() + "]");

        // Data założenia
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
        player.sendMessage(ChatColor.YELLOW + "Data założenia: " + ChatColor.WHITE + dateFormat.format(gildia.getDataZalozenia()));

        // Lider
        Player lider = Bukkit.getPlayer(gildia.getLider());
        String liderName = lider != null ? lider.getName() : "OFFLINE";
        String liderStatus = lider != null && lider.isOnline() ? ChatColor.GREEN + " (Online)" : ChatColor.RED + " (Offline)";
        player.sendMessage(ChatColor.YELLOW + "Lider: " + ChatColor.GOLD + liderName + liderStatus);

        // Zastępcy
        if (!gildia.getZastepcy().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Zastępcy (" + gildia.getZastepcy().size() + "):");
            for (UUID uuid : gildia.getZastepcy()) {
                Player zastepca = Bukkit.getPlayer(uuid);
                String zastepcaname = zastepca != null ? zastepca.getName() : "OFFLINE";
                String zastepcastatus = zastepca != null && zastepca.isOnline() ? ChatColor.GREEN + " (Online)" : ChatColor.RED + " (Offline)";
                player.sendMessage(ChatColor.WHITE + "- " + ChatColor.YELLOW + zastepcaname + zastepcastatus);
            }
        }

        player.sendMessage(ChatColor.YELLOW + "Punkty: " + ChatColor.GREEN + gildia.getPunkty());
        player.sendMessage(ChatColor.YELLOW + "Wszyscy członkowie (" + gildia.getCzlonkowie().size() + "):");

        for (UUID uuid : gildia.getCzlonkowie()) {
            Player member = Bukkit.getPlayer(uuid);
            String memberName = member != null ? member.getName() : "OFFLINE";
            String memberStatus = member != null && member.isOnline() ? ChatColor.GREEN + " (Online)" : ChatColor.RED + " (Offline)";
            String status = "";
            if (gildia.czyLider(uuid)) {
                status = ChatColor.GOLD + " [LIDER]";
            } else if (gildia.czyZastepca(uuid)) {
                status = ChatColor.YELLOW + " [ZASTĘPCA]";
            } else {
                status = ChatColor.WHITE + " [CZŁONEK]";
            }
            player.sendMessage(ChatColor.WHITE + "- " + memberName + status + memberStatus);
        }

        if (!gildia.getSojusze().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Sojusze (" + gildia.getSojusze().size() + "):");
            for (String sojusz : gildia.getSojusze()) {
                Gildia sojuszGildia = gildiaManager.getGildia(sojusz);
                if (sojuszGildia != null) {
                    player.sendMessage(ChatColor.WHITE + "- " + ChatColor.AQUA + "[" + sojuszGildia.getTag() + "] " + ChatColor.WHITE + sojuszGildia.getNazwa());
                } else {
                    player.sendMessage(ChatColor.WHITE + "- " + sojusz);
                }
            }
        }

        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
    }

    private void handleInvitePlayer(Player player, String targetName) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null) {
            player.sendMessage(ChatColor.RED + "Nie należysz do żadnej gildii!");
            return;
        }

        if (!gildia.czyMozeZarzadzac(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Nie masz uprawnień do zapraszania graczy!");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Gracz " + targetName + " nie jest online!");
            return;
        }

        if (gildiaManager.addPlayerToGildia(target.getUniqueId(), gildia.getNazwa())) {
            player.sendMessage(ChatColor.GREEN + "Gracz " + target.getName() + " został dodany do gildii!");
            target.sendMessage(ChatColor.GREEN + "Zostałeś dodany do gildii " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.YELLOW + gildia.getNazwa());
        } else {
            player.sendMessage(ChatColor.RED + "Nie można dodać gracza do gildii! Możliwe że już należy do innej gildii.");
        }
    }

    private void handleKickPlayer(Player player, String targetName) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null) {
            player.sendMessage(ChatColor.RED + "Nie należysz do żadnej gildii!");
            return;
        }

        if (!gildia.czyMozeZarzadzac(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Nie masz uprawnień do wyrzucania graczy!");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Gracz " + targetName + " nie jest online!");
            return;
        }

        if (gildia.czyLider(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Nie możesz wyrzucić lidera gildii!");
            return;
        }

        if (gildiaManager.removePlayerFromGildia(target.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Gracz " + target.getName() + " został wyrzucony z gildii!");
            target.sendMessage(ChatColor.RED + "Zostałeś wyrzucony z gildii " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.YELLOW + gildia.getNazwa());
        } else {
            player.sendMessage(ChatColor.RED + "Nie można wyrzucić gracza z gildii!");
        }
    }

    private void handleLeaveGildia(Player player) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null) {
            player.sendMessage(ChatColor.RED + "Nie należysz do żadnej gildii!");
            return;
        }

        if (gildia.czyLider(player.getUniqueId()) && gildia.getCzlonkowie().size() > 1) {
            player.sendMessage(ChatColor.RED + "Nie możesz opuścić gildii jako lider! Najpierw przekaż przywództwo lub rozwiąż gildię.");
            return;
        }

        if (gildiaManager.removePlayerFromGildia(player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "Opuściłeś gildię " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.YELLOW + gildia.getNazwa());
        } else {
            player.sendMessage(ChatColor.RED + "Nie można opuścić gildii!");
        }
    }

    private void handleDeputy(Player player, String action, String targetName) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null) {
            player.sendMessage(ChatColor.RED + "Nie należysz do żadnej gildii!");
            return;
        }

        if (!gildia.czyLider(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Tylko lider może zarządzać zastępcami!");
            return;
        }

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Gracz " + targetName + " nie jest online!");
            return;
        }

        if (!gildia.czyCzlonek(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Gracz nie należy do tej gildii!");
            return;
        }

        if (action.equalsIgnoreCase("dodaj")) {
            gildia.dodajZastepce(target.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Gracz " + target.getName() + " został mianowany zastępcą!");
            target.sendMessage(ChatColor.GREEN + "Zostałeś mianowany zastępcą gildii " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.YELLOW + gildia.getNazwa());
        } else if (action.equalsIgnoreCase("usun")) {
            gildia.usunZastepce(target.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Gracz " + target.getName() + " został usunięty z pozycji zastępcy!");
            target.sendMessage(ChatColor.YELLOW + "Zostałeś usunięty z pozycji zastępcy gildii " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.YELLOW + gildia.getNazwa());
        } else {
            player.sendMessage(ChatColor.RED + "Użyj: /gildia zastepca <dodaj/usun> <gracz>");
        }

        gildiaManager.saveGildie();
    }

    private void handleAlliance(Player player, String action, String targetGildiaName) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null) {
            player.sendMessage(ChatColor.RED + "Nie należysz do żadnej gildii!");
            return;
        }

        if (!gildia.czyLider(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Tylko lider może zarządzać sojuszami!");
            return;
        }

        Gildia targetGildia = gildiaManager.getGildia(targetGildiaName);
        if (targetGildia == null) {
            player.sendMessage(ChatColor.RED + "Gildia o nazwie " + targetGildiaName + " nie istnieje!");
            return;
        }

        if (action.equalsIgnoreCase("dodaj")) {
            gildia.dodajSojusz(targetGildia.getNazwa().toLowerCase());
            targetGildia.dodajSojusz(gildia.getNazwa().toLowerCase());
            player.sendMessage(ChatColor.GREEN + "Utworzono sojusz z gildią " + targetGildia.getNazwa());

            Player targetLider = Bukkit.getPlayer(targetGildia.getLider());
            if (targetLider != null) {
                targetLider.sendMessage(ChatColor.GREEN + "Gildia " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.YELLOW + gildia.getNazwa() + ChatColor.GREEN + " zawarła z wami sojusz!");
            }
        } else if (action.equalsIgnoreCase("usun")) {
            gildia.usunSojusz(targetGildia.getNazwa().toLowerCase());
            targetGildia.usunSojusz(gildia.getNazwa().toLowerCase());
            player.sendMessage(ChatColor.GREEN + "Usunięto sojusz z gildią " + targetGildia.getNazwa());

            Player targetLider = Bukkit.getPlayer(targetGildia.getLider());
            if (targetLider != null) {
                targetLider.sendMessage(ChatColor.RED + "Gildia " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.YELLOW + gildia.getNazwa() + ChatColor.RED + " zerwała z wami sojusz!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Użyj: /gildia sojusz <dodaj/usun> <gildia>");
        }

        gildiaManager.saveGildie();
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
        player.sendMessage(ChatColor.AQUA + "Komendy gildii:");
        player.sendMessage(ChatColor.YELLOW + "/gildia zaloz <tag> <nazwa>" + ChatColor.WHITE + " - Założ gildię");
        player.sendMessage(ChatColor.YELLOW + "/gildia info [nazwa]" + ChatColor.WHITE + " - Informacje o gildii");
        player.sendMessage(ChatColor.YELLOW + "/gildia infogracz <gracz>" + ChatColor.WHITE + " - Informacje o graczu");
        player.sendMessage(ChatColor.YELLOW + "/gildia zapros <gracz>" + ChatColor.WHITE + " - Zaproś gracza");
        player.sendMessage(ChatColor.YELLOW + "/gildia wyrzuc <gracz>" + ChatColor.WHITE + " - Wyrzuć gracza");
        player.sendMessage(ChatColor.YELLOW + "/gildia opusc" + ChatColor.WHITE + " - Opuść gildię");
        player.sendMessage(ChatColor.YELLOW + "/gildia usun" + ChatColor.WHITE + " - Usuń gildię (tylko lider)");
        player.sendMessage(ChatColor.YELLOW + "/gildia zastepca <dodaj/usun> <gracz>" + ChatColor.WHITE + " - Zarządzaj zastępcami");
        player.sendMessage(ChatColor.YELLOW + "/gildia sojusz <dodaj/usun> <gildia>" + ChatColor.WHITE + " - Zarządzaj sojuszami");
        if (player.hasPermission("gildia.admin")) {
            player.sendMessage(ChatColor.RED + "/gildia adminusun <gildia> <powód>" + ChatColor.WHITE + " - Usuń gildię (tylko admin)");
        }
        player.sendMessage(ChatColor.GREEN + "Czat gildii: " + ChatColor.WHITE + "Napisz wiadomość zaczynającą się od " + ChatColor.YELLOW + "!");
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Pierwsza opcja - główne komendy
            List<String> commands = Arrays.asList("zaloz", "info", "infogracz", "zapros", "wyrzuc", "opusc", "zastepca", "sojusz", "usun", "adminusun");
            for (String cmd : commands) {
                if (cmd.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            String mainCommand = args[0].toLowerCase();

            switch (mainCommand) {
                case "zapros":
                case "wyrzuc":
                case "infogracz":
                    // Dodaj online graczy
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(onlinePlayer.getName());
                        }
                    }
                    break;

                case "zastepca":
                    // Dodaj opcje dodaj/usun
                    List<String> deputyOptions = Arrays.asList("dodaj", "usun");
                    for (String option : deputyOptions) {
                        if (option.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(option);
                        }
                    }
                    break;

                case "sojusz":
                    // Dodaj opcje dodaj/usun
                    List<String> allianceOptions = Arrays.asList("dodaj", "usun");
                    for (String option : allianceOptions) {
                        if (option.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(option);
                        }
                    }
                    break;

                case "info":
                    // Dodaj nazwy gildii
                    for (String gildiaName : gildiaManager.getAllGildie().keySet()) {
                        if (gildiaName.toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(gildiaManager.getGildia(gildiaName).getNazwa());
                        }
                    }
                    break;

                case "usun":
                    // Dodaj opcję potwierdz
                    if ("potwierdz".toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add("potwierdz");
                    }
                    break;

                case "adminusun":
                    // Dodaj nazwy gildii (tylko dla adminów)
                    if (player.hasPermission("gildia.admin")) {
                        for (String gildiaName : gildiaManager.getAllGildie().keySet()) {
                            if (gildiaName.toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(gildiaManager.getGildia(gildiaName).getNazwa());
                            }
                        }
                    }
                    break;
            }
        } else if (args.length == 3) {
            String mainCommand = args[0].toLowerCase();
            String subCommand = args[1].toLowerCase();

            if (mainCommand.equals("zastepca") && (subCommand.equals("dodaj") || subCommand.equals("usun"))) {
                // Dodaj członków gildii gracza
                Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
                if (gildia != null) {
                    for (UUID uuid : gildia.getCzlonkowie()) {
                        Player member = Bukkit.getPlayer(uuid);
                        if (member != null && member.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(member.getName());
                        }
                    }
                }
            } else if (mainCommand.equals("sojusz") && (subCommand.equals("dodaj") || subCommand.equals("usun"))) {
                // Dodaj nazwy innych gildii
                for (String gildiaName : gildiaManager.getAllGildie().keySet()) {
                    Gildia gildia = gildiaManager.getGildia(gildiaName);
                    if (gildia != null && !gildia.czyCzlonek(player.getUniqueId())) {
                        if (gildia.getNazwa().toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(gildia.getNazwa());
                        }
                    }
                }
            }
        }

        return completions;
    }

    private void handleDeleteGildia(Player player, boolean confirmed) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(player.getUniqueId());
        if (gildia == null) {
            player.sendMessage(ChatColor.RED + "Nie należysz do żadnej gildii!");
            return;
        }

        if (!gildia.czyLider(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Tylko lider może usunąć gildię!");
            return;
        }

        if (!confirmed) {
            player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
            player.sendMessage(ChatColor.RED + "UWAGA! Ta akcja jest nieodwracalna!");
            player.sendMessage(ChatColor.YELLOW + "Usuniesz gildię: " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.YELLOW + gildia.getNazwa());
            player.sendMessage(ChatColor.YELLOW + "Członkowie: " + ChatColor.WHITE + gildia.getCzlonkowie().size());
            player.sendMessage(ChatColor.YELLOW + "Punkty: " + ChatColor.WHITE + gildia.getPunkty());
            player.sendMessage(ChatColor.RED + "Aby potwierdzić użyj: " + ChatColor.WHITE + "/gildia usun potwierdz");
            player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
            return;
        }

        // Powiadom wszystkich członków
        for (UUID uuid : gildia.getCzlonkowie()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.RED + "Gildia " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.YELLOW + gildia.getNazwa() + ChatColor.RED + " została usunięta przez lidera!");
            }
        }

        // Usuń gildię
        gildiaManager.deleteGildia(gildia.getNazwa());
        player.sendMessage(ChatColor.GREEN + "Gildia została pomyślnie usunięta!");
    }

    private void handlePlayerInfo(Player player, Player targetPlayer) {
        Gildia gildia = gildiaManager.getGildiaByPlayer(targetPlayer.getUniqueId());

        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
        player.sendMessage(ChatColor.AQUA + "Informacje o graczu: " + ChatColor.WHITE + targetPlayer.getName());

        if (gildia == null) {
            player.sendMessage(ChatColor.YELLOW + "Status: " + ChatColor.RED + "Nie należy do żadnej gildii");
        } else {
            player.sendMessage(ChatColor.YELLOW + "Gildia: " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.WHITE + gildia.getNazwa());

            String rank;
            if (gildia.czyLider(targetPlayer.getUniqueId())) {
                rank = ChatColor.GOLD + "Lider";
            } else if (gildia.czyZastepca(targetPlayer.getUniqueId())) {
                rank = ChatColor.YELLOW + "Zastępca";
            } else {
                rank = ChatColor.WHITE + "Członek";
            }
            player.sendMessage(ChatColor.YELLOW + "Ranga: " + rank);
            player.sendMessage(ChatColor.YELLOW + "Punkty gildii: " + ChatColor.GREEN + gildia.getPunkty());
        }

        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
    }

    private void handleAdminDeleteGildia(Player player, String gildiaName, String powod) {
        Gildia gildia = gildiaManager.getGildia(gildiaName);
        if (gildia == null) {
            player.sendMessage(ChatColor.RED + "Gildia o nazwie " + gildiaName + " nie istnieje!");
            return;
        }

        // Powiadom wszystkich członków gildii
        for (UUID uuid : gildia.getCzlonkowie()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.isOnline()) {
                member.sendMessage(ChatColor.RED + "Twoja gildia " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.YELLOW + gildia.getNazwa() + ChatColor.RED + " została usunięta przez administratora!");
                member.sendMessage(ChatColor.YELLOW + "Powód: " + ChatColor.WHITE + powod);
            }
        }

        // Ogłoszenie publiczne na całym serwerze
        String publicMessage = ChatColor.GOLD + "★ " + ChatColor.RED + "ADMIN " + ChatColor.YELLOW + player.getName()
                + ChatColor.RED + " usunął gildię " + ChatColor.AQUA + "[" + gildia.getTag() + "] "
                + ChatColor.YELLOW + gildia.getNazwa() + ChatColor.GOLD + " ★";
        String reasonMessage = ChatColor.YELLOW + "Powód: " + ChatColor.WHITE + powod;

        Bukkit.broadcastMessage(publicMessage);
        Bukkit.broadcastMessage(reasonMessage);

        // Usuń gildię
        gildiaManager.deleteGildia(gildia.getNazwa());
        player.sendMessage(ChatColor.GREEN + "Gildia " + ChatColor.AQUA + "[" + gildia.getTag() + "] " + ChatColor.YELLOW + gildia.getNazwa() + ChatColor.GREEN + " została pomyślnie usunięta!");
    }
}
