package dev.namestyleplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Zusatz-Einstellungen: Tag-Stile (fett/kursiv/unterstrichen) und globale Saettigung. */
public final class PlusConfig {
    public static final int TAGS = 5;

    /** 0.0 = schwarz-weiss, 1.0 = unveraendert, 2.0 = doppelt so satt. */
    public double saturation = 1.0;
    public boolean[] tagBold = new boolean[TAGS];
    public boolean[] tagItalic = new boolean[TAGS];
    public boolean[] tagUnderlined = new boolean[TAGS];

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PlusConfig instance = new PlusConfig();

    public static PlusConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("namestyle-plus.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                PlusConfig c = GSON.fromJson(Files.readString(p), PlusConfig.class);
                if (c != null) {
                    instance = c;
                }
            }
        } catch (Exception e) {
            System.err.println("[NameStylePlus] Konnte Config nicht laden: " + e);
        }
        instance.ensure();
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(instance));
        } catch (Exception e) {
            System.err.println("[NameStylePlus] Konnte Config nicht speichern: " + e);
        }
    }

    public void ensure() {
        tagBold = fit(tagBold);
        tagItalic = fit(tagItalic);
        tagUnderlined = fit(tagUnderlined);
        if (Double.isNaN(saturation)) {
            saturation = 1.0;
        }
        saturation = Math.max(0.0, Math.min(2.0, saturation));
    }

    private static boolean[] fit(boolean[] a) {
        boolean[] r = new boolean[TAGS];
        if (a != null) {
            System.arraycopy(a, 0, r, 0, Math.min(a.length, TAGS));
        }
        return r;
    }
}
