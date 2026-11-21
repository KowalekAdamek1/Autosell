package pl.autosell;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ustawienia zapisywane w config/autosell.json */
public class AutoSellConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("autosell.json");

    public boolean enabled = false;
    public String ore = Ore.COPPER.name();
    /** Kod klawisza GLFW otwierającego GUI (domyślnie Right Shift). */
    public int guiKey = GLFW.GLFW_KEY_RIGHT_SHIFT;
    public int intervalSeconds = 120;
    /** Działa tylko na serwerze anarchia.gg. */
    public boolean onlyOnAnarchia = true;
    /** Gdy Baritone jest zainstalowany: sam kopie wybraną rudę (komenda "mine"). */
    public boolean autoMine = true;
    /** Wstrzymuje Baritone na czas sprzedaży i wznawia po niej. */
    public boolean pauseBaritoneWhileSelling = true;
    /** Wypisuje na czacie (tylko u Ciebie) sloty menu sklepu - pomaga w dopasowaniu. */
    public boolean debug = false;

    public Ore ore() {
        return Ore.byName(ore);
    }

    public static AutoSellConfig load() {
        if (Files.exists(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                AutoSellConfig cfg = GSON.fromJson(r, AutoSellConfig.class);
                if (cfg != null) {
                    if (cfg.intervalSeconds < 10) cfg.intervalSeconds = 120;
                    return cfg;
                }
            } catch (Exception e) {
                AutoSellMod.LOGGER.warn("Nie udało się wczytać configu, używam domyślnego", e);
            }
        }
        AutoSellConfig cfg = new AutoSellConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(this, w);
            }
        } catch (IOException e) {
            AutoSellMod.LOGGER.warn("Nie udało się zapisać configu", e);
        }
    }
}
