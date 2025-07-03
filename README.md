# GildiaPlugin - Plugin Minecraft na Gildie

## Opis

Plugin umożliwia tworzenie i zarządzanie gildiami na serwerze Minecraft. Zawiera system punktów, zastępców, sojuszy i czatu gildii.

## Funkcje

- **Tworzenie gildii** - z 2-4 literowym tagiem i nazwą
- **Zarządzanie członkami** - zapraszanie i wyrzucanie graczy
- **System zastępców** - dodawanie i usuwanie zastępców gildii
- **Sojusze** - tworzenie sojuszy między gildiami
- **Czat gildii** - prywatny czat dla członków gildii (wiadomości zaczynające się od "!")
- **System punktów** - punkty przyznawane za aktywność
- **Szczegółowe informacje** - informacje o gildii i graczach
- **Usuwanie gildii** - z systemem potwierdzenia dla bezpieczeństwa
- **Tab-completion** - automatyczne uzupełnianie komend i argumentów
- **System zastępców** - dodatni gracze którzy mogą zarządzać gildią
- **Punkty gildii** - system punktowy powiązany z członkami
- **Sojusze** - możliwość zawierania sojuszy z innymi gildiami
- **Czat gildii** - prywatny czat dla członków gildii i sojuszników
- **Kolorowe ogłoszenia** - piękne powiadomienia o założeniu gildii
- **Tab completion** - automatyczne uzupełnianie komend i argumentów

## Komendy

### Podstawowe komendy:

- `/gildia zaloz <tag> <nazwa>` - Założ nową gildię (tag musi mieć od 2 do 4 znaków)
- `/gildia info [nazwa]` - Wyświetl szczegółowe informacje o gildii (lub swojej gildii)
- `/gildia infogracz <gracz>` - Wyświetl informacje o graczu i jego gildii
- `/gildia opusc` - Opuść gildię

### Komendy zarządzania (lider/zastępca):

- `/gildia zapros <gracz>` - Zaproś gracza do gildii
- `/gildia wyrzuc <gracz>` - Wyrzuć gracza z gildii

### Komendy lidera:

- `/gildia usun` - Usuń gildię (wymaga potwierdzenia)
- `/gildia zastepca dodaj <gracz>` - Mianuj zastępcę
- `/gildia zastepca usun <gracz>` - Usuń zastępcę
- `/gildia sojusz dodaj <gildia>` - Zawrzyj sojusz z gildią
- `/gildia sojusz usun <gildia>` - Zerwij sojusz z gildią

### Komendy administracyjne (tylko operatorzy):

- `/gildia adminusun <nazwa_gildii> <powód>` - Usuń gildię z podaniem powodu (publiczne ogłoszenie)

## Czat gildii

Aby pisać na czacie gildii, rozpocznij wiadomość od znaku `!`

Przykład: `!Witajcie w naszej gildii!`

Wiadomości będą widoczne dla:

- Wszystkich członków Twojej gildii
- Wszystkich członków sojuszniczych gildii

## Instalacja

1. Skopiuj plik `GildiaPlugin-1.0.jar` do folderu `plugins` na serwerze
2. Uruchom ponownie serwer lub użyj `/reload`
3. Plugin automatycznie utworzy plik `gildie.yml` w folderze `plugins/GildiaPlugin/`

## Wymagania

- Minecraft 1.20+
- Java 17+
- Serwer Spigot/Paper

## Szczegółowe informacje o gildii

Komenda `/gildia info` pokazuje:

- **Nazwa i tag gildii**
- **Data założenia** - kiedy gildia została utworzona
- **Lider** - z informacją o statusie online/offline
- **Zastępcy** - lista wszystkich zastępców z statusem
- **Wszyscy członkowie** - z rangą (Lider/Zastępca/Członek) i statusem online/offline
- **Punkty gildii** - aktualna liczba punktów
- **Sojusze** - lista sojuszniczych gildii z ich tagami

Komenda `/gildia infogracz <gracz>` pokazuje:

- **Nazwa gracza**
- **Gildia** - do której należy (jeśli należy)
- **Ranga** - Lider, Zastępca lub Członek
- **Punkty gildii** - punkty gildii gracza

## Uprawnienia

- `gildia.use` - Podstawowe komendy gildii (domyślnie: wszyscy)
- `gildia.admin` - Komendy administracyjne (domyślnie: operatorzy)

## Pliki

- `gildie.yml` - Przechowuje dane wszystkich gildii
- `plugin.yml` - Konfiguracja pluginu

## Funkcje specjalne

- **Kolorowe ogłoszenia** - gdy ktoś założy gildię, zostanie wyświetlone kolorowe ogłoszenie na czacie
- **Automatyczne zarządzanie przywództwem** - gdy lider opuści gildię, automatycznie zostanie wybrany nowy lider
- **Persistent data** - wszystkie dane są zapisywane w pliku YAML
- **Sojusze dwustronne** - sojusze działają w obie strony automatycznie
- **Tab completion** - wszystkie komendy i argumenty są automatycznie uzupełniane przez Tab

## Tab Completion

Plugin obsługuje automatyczne uzupełnianie komend:

- `/gildia <TAB>` - wyświetli wszystkie dostępne komendy
- `/gildia zapros <TAB>` - pokaże online graczy
- `/gildia infogracz <TAB>` - pokaże online graczy
- `/gildia info <TAB>` - pokaże nazwy gildii
- `/gildia usun <TAB>` - pokaże opcję "potwierdz"
- `/gildia zastepca <TAB>` - pokaże opcje dodaj/usun
- `/gildia zastepca dodaj <TAB>` - pokaże członków Twojej gildii
- `/gildia sojusz <TAB>` - pokaże opcje dodaj/usun
- `/gildia sojusz dodaj <TAB>` - pokaże nazwy innych gildii
- `/gildia adminusun <TAB>` - pokaże nazwy gildii (tylko dla adminów)

## Przykład użycia

```
/gildia zaloz DR Smoczy_Klan
/gildia zapros Gracz123
/gildia zastepca dodaj Gracz123
/gildia sojusz dodaj Inny_Klan
!Witajcie w naszej gildii!
```

### Przykład dla administratorów:

```
/gildia adminusun Smoczy_Klan Łamanie regulaminu serwera
```

## Nowe funkcje

### Usuwanie gildii

```
/gildia usun              # Wyświetla informacje i prosi o potwierdzenie
/gildia usun potwierdz    # Usuwa gildię (tylko lider)
```

### Sprawdzanie informacji o graczu

```
/gildia info Gracz123     # Pokazuje w jakiej gildii jest gracz
/gildia info NazwaGildii  # Pokazuje informacje o gildii
/gildia info              # Pokazuje informacje o Twojej gildii
```

## Autor

Stworzony dla serwera Minecraft
