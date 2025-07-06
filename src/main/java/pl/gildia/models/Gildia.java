package pl.gildia.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class Gildia {

    private String nazwa;
    private String tag;
    private UUID lider;
    private List<UUID> czlonkowie;
    private List<UUID> zastepcy;
    private List<String> sojusze;
    private List<String> zaproszeniaSojuszy; // Zaproszenia do sojuszy oczekujące na akceptację
    private int punkty;
    private Date dataZalozenia;

    public Gildia(String nazwa, String tag, UUID lider) {
        this.nazwa = nazwa;
        this.tag = tag;
        this.lider = lider;
        this.czlonkowie = new ArrayList<>();
        this.zastepcy = new ArrayList<>();
        this.sojusze = new ArrayList<>();
        this.zaproszeniaSojuszy = new ArrayList<>();
        this.punkty = 0;
        this.dataZalozenia = new Date();

        // Lider jest automatycznie członkiem
        this.czlonkowie.add(lider);
    }

    public String getNazwa() {
        return nazwa;
    }

    public void setNazwa(String nazwa) {
        this.nazwa = nazwa;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public UUID getLider() {
        return lider;
    }

    public void setLider(UUID lider) {
        this.lider = lider;
    }

    public List<UUID> getCzlonkowie() {
        return czlonkowie;
    }

    public void setCzlonkowie(List<UUID> czlonkowie) {
        this.czlonkowie = czlonkowie;
    }

    public List<UUID> getZastepcy() {
        return zastepcy;
    }

    public void setZastepcy(List<UUID> zastepcy) {
        this.zastepcy = zastepcy;
    }

    public List<String> getSojusze() {
        return sojusze;
    }

    public void setSojusze(List<String> sojusze) {
        this.sojusze = sojusze;
    }

    public List<String> getZaproszeniaSojuszy() {
        return zaproszeniaSojuszy;
    }

    public void setZaproszeniaSojuszy(List<String> zaproszeniaSojuszy) {
        this.zaproszeniaSojuszy = zaproszeniaSojuszy;
    }

    public int getPunkty() {
        return punkty;
    }

    public void setPunkty(int punkty) {
        this.punkty = punkty;
    }

    public Date getDataZalozenia() {
        return dataZalozenia;
    }

    public void setDataZalozenia(Date dataZalozenia) {
        this.dataZalozenia = dataZalozenia;
    }

    public void dodajPunkty(int punkty) {
        this.punkty += punkty;
    }

    public void odejmijPunkty(int punkty) {
        this.punkty = Math.max(0, this.punkty - punkty);
    }

    public void dodajCzlonka(UUID gracz) {
        if (!czlonkowie.contains(gracz)) {
            czlonkowie.add(gracz);
        }
    }

    public void usunCzlonka(UUID gracz) {
        czlonkowie.remove(gracz);
        zastepcy.remove(gracz);
    }

    public void dodajZastepce(UUID gracz) {
        if (czlonkowie.contains(gracz) && !zastepcy.contains(gracz)) {
            zastepcy.add(gracz);
        }
    }

    public void usunZastepce(UUID gracz) {
        zastepcy.remove(gracz);
    }

    public void dodajSojusz(String gildia) {
        if (!sojusze.contains(gildia)) {
            sojusze.add(gildia);
        }
    }

    public void usunSojusz(String gildia) {
        sojusze.remove(gildia);
    }

    public void dodajZaproszenieSojusz(String gildia) {
        if (!zaproszeniaSojuszy.contains(gildia)) {
            zaproszeniaSojuszy.add(gildia);
        }
    }

    public void usunZaproszenieSojusz(String gildia) {
        zaproszeniaSojuszy.remove(gildia);
    }

    public boolean czyMaZaproszenieSojusz(String gildia) {
        return zaproszeniaSojuszy.contains(gildia);
    }

    public boolean czyLider(UUID gracz) {
        return lider.equals(gracz);
    }

    public boolean czyZastepca(UUID gracz) {
        return zastepcy.contains(gracz);
    }

    public boolean czyCzlonek(UUID gracz) {
        return czlonkowie.contains(gracz);
    }

    public boolean czyMozeZarzadzac(UUID gracz) {
        return czyLider(gracz) || czyZastepca(gracz);
    }
}
