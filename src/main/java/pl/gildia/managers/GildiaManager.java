package pl.gildia.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import pl.gildia.GildiaPlugin;
import pl.gildia.models.Gildia;

public class GildiaManager {

    private GildiaPlugin plugin;
    private Map<String, Gildia> gildie;
    private Map<UUID, String> graczGildia;

    public GildiaManager(GildiaPlugin plugin) {
        this.plugin = plugin;
        this.gildie = new HashMap<>();
        this.graczGildia = new HashMap<>();
        loadGildie();
    }

    public boolean createGildia(String nazwa, String tag, UUID lider) {
        if (gildie.containsKey(nazwa.toLowerCase())) {
            return false;
        }

        if (graczGildia.containsKey(lider)) {
            return false;
        }

        if (tag.length() < 2 || tag.length() > 4) {
            return false;
        }

        // Sprawdź czy tag już istnieje
        for (Gildia g : gildie.values()) {
            if (g.getTag().equalsIgnoreCase(tag)) {
                return false;
            }
        }

        Gildia gildia = new Gildia(nazwa, tag, lider);
        gildie.put(nazwa.toLowerCase(), gildia);
        graczGildia.put(lider, nazwa.toLowerCase());

        saveGildie();

        // Ogłoszenie na czacie
        Player gracz = Bukkit.getPlayer(lider);
        if (gracz != null) {
            String message = ChatColor.GOLD + "★ " + ChatColor.YELLOW + gracz.getName()
                    + ChatColor.GREEN + " założył gildię " + ChatColor.AQUA + "[" + tag + "] "
                    + ChatColor.YELLOW + nazwa + ChatColor.GOLD + " ★";
            Bukkit.broadcastMessage(message);
        }

        // Aktualizuj tagi graczy
        updatePlayerDisplayName(lider);

        return true;
    }

    public void deleteGildia(String nazwa) {
        Gildia gildia = gildie.get(nazwa.toLowerCase());
        if (gildia != null) {
            // Usuń wszystkich graczy z mapy i zaktualizuj ich tagi
            for (UUID gracz : gildia.getCzlonkowie()) {
                graczGildia.remove(gracz);
                updatePlayerDisplayName(gracz);
            }

            // Usuń sojusze
            for (String sojuszNazwa : gildia.getSojusze()) {
                Gildia sojusz = gildie.get(sojuszNazwa);
                if (sojusz != null) {
                    sojusz.usunSojusz(nazwa.toLowerCase());
                }
            }

            gildie.remove(nazwa.toLowerCase());
            saveGildie();
        }
    }

    public Gildia getGildia(String nazwa) {
        return gildie.get(nazwa.toLowerCase());
    }

    public Gildia getGildiaByPlayer(UUID gracz) {
        String gildiaName = graczGildia.get(gracz);
        if (gildiaName != null) {
            return gildie.get(gildiaName);
        }
        return null;
    }

    public boolean addPlayerToGildia(UUID gracz, String gildiaName) {
        if (graczGildia.containsKey(gracz)) {
            return false;
        }

        Gildia gildia = gildie.get(gildiaName.toLowerCase());
        if (gildia == null) {
            return false;
        }

        gildia.dodajCzlonka(gracz);
        graczGildia.put(gracz, gildiaName.toLowerCase());
        saveGildie();

        // Aktualizuj displayName gracza
        updatePlayerDisplayName(gracz);

        return true;
    }

    public boolean removePlayerFromGildia(UUID gracz) {
        String gildiaName = graczGildia.get(gracz);
        if (gildiaName == null) {
            return false;
        }

        Gildia gildia = gildie.get(gildiaName);
        if (gildia == null) {
            return false;
        }

        gildia.usunCzlonka(gracz);
        graczGildia.remove(gracz);

        // Jeśli to był lider i gildia ma innych członków, wybierz nowego lidera
        if (gildia.czyLider(gracz)) {
            if (gildia.getCzlonkowie().isEmpty()) {
                // Usuń gildię jeśli nie ma członków
                deleteGildia(gildia.getNazwa());
                return true;
            } else {
                // Wybierz nowego lidera (pierwszy zastępca lub pierwszy członek)
                UUID nowyLider = null;
                if (!gildia.getZastepcy().isEmpty()) {
                    nowyLider = gildia.getZastepcy().get(0);
                } else if (!gildia.getCzlonkowie().isEmpty()) {
                    nowyLider = gildia.getCzlonkowie().get(0);
                }

                if (nowyLider != null) {
                    gildia.setLider(nowyLider);
                    gildia.usunZastepce(nowyLider);
                }
            }
        }

        saveGildie();

        // Aktualizuj displayName gracza
        updatePlayerDisplayName(gracz);

        return true;
    }

    public void sendGildiaMessage(Player sender, String message) {
        Gildia gildia = getGildiaByPlayer(sender.getUniqueId());
        if (gildia == null) {
            return;
        }

        String formattedMessage = ChatColor.DARK_GREEN + "[CZAT GILDII] "
                + ChatColor.AQUA + "[" + gildia.getTag() + "] "
                + ChatColor.WHITE + sender.getName() + ChatColor.GRAY + ": "
                + ChatColor.GREEN + message;

        // Wyślij do wszystkich członków gildii
        for (UUID czlonek : gildia.getCzlonkowie()) {
            Player gracz = Bukkit.getPlayer(czlonek);
            if (gracz != null && gracz.isOnline()) {
                gracz.sendMessage(formattedMessage);
            }
        }

        // Wyślij do sojuszników
        for (String sojuszNazwa : gildia.getSojusze()) {
            Gildia sojusz = gildie.get(sojuszNazwa);
            if (sojusz != null) {
                String sojuszMessage = ChatColor.DARK_GREEN + "[CZAT SOJUSZ] "
                        + ChatColor.AQUA + "[" + gildia.getTag() + "] "
                        + ChatColor.WHITE + sender.getName() + ChatColor.GRAY + ": "
                        + ChatColor.GREEN + message;

                for (UUID czlonek : sojusz.getCzlonkowie()) {
                    Player gracz = Bukkit.getPlayer(czlonek);
                    if (gracz != null && gracz.isOnline()) {
                        gracz.sendMessage(sojuszMessage);
                    }
                }
            }
        }
    }

    public void saveGildie() {
        ConfigurationSection gildieSection = plugin.getGildieConfig().createSection("gildie");

        for (Map.Entry<String, Gildia> entry : gildie.entrySet()) {
            Gildia gildia = entry.getValue();
            ConfigurationSection gildiaSection = gildieSection.createSection(entry.getKey());

            gildiaSection.set("nazwa", gildia.getNazwa());
            gildiaSection.set("tag", gildia.getTag());
            gildiaSection.set("lider", gildia.getLider().toString());
            gildiaSection.set("punkty", gildia.getPunkty());

            List<String> czlonkowie = new ArrayList<>();
            for (UUID uuid : gildia.getCzlonkowie()) {
                czlonkowie.add(uuid.toString());
            }
            gildiaSection.set("czlonkowie", czlonkowie);

            // Zapisz punkty członków
            ConfigurationSection punktySection = gildiaSection.createSection("punktyCzlonkow");
            for (Map.Entry<UUID, Integer> e : gildia.getPunktyCzlonkow().entrySet()) {
                punktySection.set(e.getKey().toString(), e.getValue());
            }

            List<String> zastepcy = new ArrayList<>();
            for (UUID uuid : gildia.getZastepcy()) {
                zastepcy.add(uuid.toString());
            }
            gildiaSection.set("zastepcy", zastepcy);

            gildiaSection.set("sojusze", gildia.getSojusze());
            gildiaSection.set("zaproszeniaSojuszy", gildia.getZaproszeniaSojuszy());

            // Zapisz datę założenia
            if (gildia.getDataZalozenia() != null) {
                gildiaSection.set("dataZalozenia", gildia.getDataZalozenia().getTime());
            }
        }

        plugin.saveGildieConfig();
    }

    public void loadGildie() {
        ConfigurationSection gildieSection = plugin.getGildieConfig().getConfigurationSection("gildie");
        if (gildieSection == null) {
            return;
        }

        for (String key : gildieSection.getKeys(false)) {
            ConfigurationSection gildiaSection = gildieSection.getConfigurationSection(key);
            if (gildiaSection == null) {
                continue;
            }

            String nazwa = gildiaSection.getString("nazwa");
            String tag = gildiaSection.getString("tag");
            UUID lider = UUID.fromString(gildiaSection.getString("lider"));
            int punkty = gildiaSection.getInt("punkty");

            Gildia gildia = new Gildia(nazwa, tag, lider);
            gildia.setPunkty(punkty);

            List<String> czlonkowie = gildiaSection.getStringList("czlonkowie");
            List<UUID> czlonkowieUUID = new ArrayList<>();
            for (String uuid : czlonkowie) {
                UUID graczUUID = UUID.fromString(uuid);
                czlonkowieUUID.add(graczUUID);
                graczGildia.put(graczUUID, key);
            }
            gildia.setCzlonkowie(czlonkowieUUID);

            List<String> zastepcy = gildiaSection.getStringList("zastepcy");
            List<UUID> zastepcy_UUID = new ArrayList<>();
            for (String uuid : zastepcy) {
                zastepcy_UUID.add(UUID.fromString(uuid));
            }
            gildia.setZastepcy(zastepcy_UUID);

            List<String> sojusze = gildiaSection.getStringList("sojusze");
            gildia.setSojusze(new ArrayList<>(sojusze));

            List<String> zaproszeniaSojuszy = gildiaSection.getStringList("zaproszeniaSojuszy");
            gildia.setZaproszeniaSojuszy(new ArrayList<>(zaproszeniaSojuszy));

            // Wczytaj punkty członków
            if (gildiaSection.contains("punktyCzlonkow")) {
                ConfigurationSection punktySection = gildiaSection.getConfigurationSection("punktyCzlonkow");
                Map<UUID, Integer> punktyMap = new HashMap<>();
                for (String uuidStr : punktySection.getKeys(false)) {
                    try {
                        UUID id = UUID.fromString(uuidStr);
                        int val = punktySection.getInt(uuidStr);
                        punktyMap.put(id, val);
                    } catch (Exception ex) {
                        // ignore malformed
                    }
                }
                gildia.setPunktyCzlonkow(punktyMap);
            }

            // Wczytaj datę założenia
            if (gildiaSection.contains("dataZalozenia")) {
                long timestamp = gildiaSection.getLong("dataZalozenia");
                gildia.setDataZalozenia(new java.util.Date(timestamp));
            }

            gildie.put(key, gildia);
        }
    }

    public Map<String, Gildia> getAllGildie() {
        return gildie;
    }

    public void updatePlayerDisplayName(UUID gracz) {
        Player player = Bukkit.getPlayer(gracz);
        if (player != null && player.isOnline()) {
            // Znajdź PlayerDisplayListener z pluginu i wywołaj aktualizację
            plugin.getPlayerDisplayListener().updatePlayerDisplayName(player);
        }
    }

    public void updateAllPlayersDisplayNames() {
        plugin.getPlayerDisplayListener().updateAllPlayersDisplayNames();
    }
}
