# AutoSell (Fabric 1.21.4)

Mod kliencki: GUI pod **Right Shift** (bind zmieniasz w GUI), autosell wybranej rudy w `/sklep`
na anarchia.gg co 2 minuty, współpraca z **Baritone**.

## Budowanie
Wymagane: JDK 21 oraz Gradle 8.10+ (lub import do IntelliJ IDEA jako projekt Gradle).

    gradle build

Gotowy plik: `build/libs/autosell-1.0.0.jar` -> wrzuć do `.minecraft/mods/`
(potrzebny Fabric Loader + Fabric API dla 1.21.4).

## Użycie
1. Wejdź na anarchia.gg, naciśnij Right Shift.
2. Wybierz rudę (np. Lapis), włącz AutoSell.
3. Z Baritone: mod sam wpisuje `mine <ruda>` i kopie; co 2 min wstrzymuje Baritone (`pause`),
   wpisuje `/sklep`, sprzedaje, zamyka GUI i wznawia (`resume`).
   Bez Baritone mod tylko sprzedaje to, co masz w ekwipunku.
4. Ustawienia: `config/autosell.json` (interwał, tylko anarchia.gg, auto-kopanie, debug).

## Uwaga: układ /sklep
Układ menu serwera jest dopasowany heurystycznie (szuka przedmiotu rudy, "sprzedaj wszystko",
kategorii z "rud"; pomija sloty "kup"). Jeśli nie klika właściwie: ustaw `"debug": true` w configu -
mod wypisze sloty menu na czacie, wyślij mi je, a dopasuję.
