# Integracja z PvP Stats Plugin

## ✅ Zmiany wprowadzone

Plugin GildiaPlugin został zmodyfikowany, aby usunąć wbudowany system punktów i zintegrować się z zewnętrznym pluginem **PVP Stats**.

### Usunięte funkcje:

- ❌ Wbudowany system punktów gildii
- ❌ Komendy `/gildia pkt` i `/gildia ustawpkt`
- ❌ Automatyczne zarządzanie punktami przy zabójstwach
- ❌ Wyświetlanie punktów w informacjach o gildii

### Dodane funkcje:

- ✅ Integracja z PlaceholderAPI
- ✅ Wyświetlanie statystyk PvP Stats w tagach graczy
- ✅ Automatyczna synchronizacja z PvP Stats

## 📋 Wymagania

Plugin wymaga teraz następujących zależności:

### Wymagane pluginy (MUST HAVE):

1. **PVPStats** - zarządza statystykami PvP
2. **PlaceholderAPI** - umożliwia wyświetlanie placeholders

### Opcjonalne pluginy:

1. **Vault** - dla systemu rang (jeśli używany)

## 🎮 Format wyświetlania

### W TAB-ie:

```
[TAG] Nick [K:15 D:3 S:8]
```

### W czacie:

```
(Ranga) [TAG] Nick [K:15 D:3 S:8]
```

### Nad głową gracza (scoreboard):

```
[TAG] Nick [15K]
```

Gdzie:

- **K** = Kills (zabójstwa)
- **D** = Deaths (śmierci)
- **S** = Streak (aktualna passa)

## 📊 Dostępne placeholders z PvP Stats

Plugin używa następujących placeholders:

| Placeholder                   | Opis                    |
| ----------------------------- | ----------------------- |
| `%slipcorpvpstats_kills%`     | Liczba zabójstw         |
| `%slipcorpvpstats_deaths%`    | Liczba śmierci          |
| `%slipcorpvpstats_streak%`    | Aktualna passa zabójstw |
| `%slipcorpvpstats_maxstreak%` | Najwyższa passa         |
| `%slipcorpvpstats_elo%`       | Punkty ELO              |
| `%slipcorpvpstats_ratio%`     | Współczynnik K/D        |

## 🔧 Instalacja

1. **Pobierz i zainstaluj wymagane pluginy:**

   - PVP Stats: https://www.spigotmc.org/resources/pvp-stats.59124/
   - PlaceholderAPI: https://www.spigotmc.org/resources/placeholderapi.6245/

2. **Zainstaluj GildiaPlugin:**

   - Skopiuj `GildiaPlugin-1.0.jar` do folderu `plugins`
   - Restart serwera

3. **Konfiguracja PlaceholderAPI:**
   ```
   /papi ecloud download PVPStats
   /papi reload
   ```

## 🎯 Korzyści z integracji

### ✅ Profesjonalne statystyki:

- PvP Stats oferuje zaawansowane funkcje takie jak ELO, ranking, bazy danych
- Automatyczne backupy i zarządzanie danymi
- Kompatybilność z hologramami i innymi pluginami

### ✅ Mniejsza złożoność:

- GildiaPlugin skupia się na zarządzaniu gildiami
- Brak duplikacji funkcjonalności
- Łatwiejsze utrzymanie kodu

### ✅ Więcej możliwości:

- Dostęp do wszystkich funkcji PvP Stats
- Leaderboardy i rankingi
- Szczegółowe statystyki per gracz

## 📝 Komendy po zmianach

Plugin GildiaPlugin nadal oferuje wszystkie podstawowe komendy gildii:

| Komenda                                 | Opis                    |
| --------------------------------------- | ----------------------- |
| `/gildia zaloz <tag> <nazwa>`           | Założenie gildii        |
| `/gildia info [nazwa]`                  | Informacje o gildii     |
| `/gildia infogracz <gracz>`             | Informacje o graczu     |
| `/gildia zapros <gracz>`                | Zaproszenie do gildii   |
| `/gildia dolacz <gildia>`               | Dołączenie do gildii    |
| `/gildia wyrzuc <gracz>`                | Wyrzucenie z gildii     |
| `/gildia opusc`                         | Opuszczenie gildii      |
| `/gildia zastepca <dodaj/usun> <gracz>` | Zarządzanie zastępcami  |
| `/gildia sojusz <akcja> <gildia>`       | Zarządzanie sojuszami   |
| `/gildia usun`                          | Usunięcie gildii        |
| `/gildia adminusun <gildia> <powód>`    | Admin: usunięcie gildii |

## 🔥 Przykładowe konfiguracje

### PvP Stats - config.yml:

```yaml
# Włącz system ELO dla bardziej zaawansowanych statystyk
elo:
  enabled: true
  default: 1000

# Konfiguracja bazy danych (zalecane SQLite lub MySQL)
database:
  type: "SQLite"
```

### PlaceholderAPI - sprawdzenie:

```
/papi parse <gracz> %slipcorpvpstats_kills%
```

## 🎉 Podsumowanie

Plugin został pomyślnie zintegrowany z PvP Stats. Gracze będą teraz widzieć swoje statystyki PvP bezpośrednio w tagach gildii, co zapewnia profesjonalne i spójne doświadczenie gry.

**Plik JAR do użycia:** `target/GildiaPlugin-1.0.jar`
