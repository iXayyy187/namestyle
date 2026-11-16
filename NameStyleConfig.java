package dev.namestyle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Einstellungen (config/namestyle.json) + Aufbau des gestylten (animierten) Namens. */
public final class NameStyleConfig {
    public static final int TAG_COUNT = 5;

    public boolean enabled = true;
    public boolean showOwnNametag = true;
    public String customName = "";
    public String mode = "SOLID";               // SOLID, RAINBOW, GRADIENT, PULSE
    public String color = "#FF55FF";
    public String color2 = "#55FFFF";
    public double speed = 1.0;                  // 0.2 .. 4.0
    public String font = "minecraft:default";
    public boolean bold, italic, underlined, obfuscated;
    public String[] tagText = {"", "", "", "", ""};
    public String[] tagColor = {"#FFAA00", "#FF5555", "#55FF55", "#55FFFF", "#FFFFFF"};

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static NameStyleConfig instance = new NameStyleConfig();
    private static String cacheKey;
    private static Component cacheValue;

    public static NameStyleConfig get() { return instance; }

    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("namestyle.json"); }

    public static void load() {
        try {
            if (Files.exists(path())) {
                NameStyleConfig c = GSON.fromJson(Files.readString(path()), NameStyleConfig.class);
                if (c != null) instance = c;
            }
        } catch (Exception e) {
            System.err.println("[NameStyle] Konnte Config nicht laden: " + e);
        }
        instance.ensureTags();
        cacheKey = null;
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(instance));
        } catch (Exception e) {
            System.err.println("[NameStyle] Konnte Config nicht speichern: " + e);
        }
        cacheKey = null;
    }

    public void ensureTags() {
        String[] t = new String[TAG_COUNT];
        String[] c = new String[TAG_COUNT];
        for (int i = 0; i < TAG_COUNT; i++) {
            t[i] = (tagText != null && i < tagText.length && tagText[i] != null) ? tagText[i] : "";
            c[i] = (tagColor != null && i < tagColor.length && tagColor[i] != null) ? tagColor[i] : "#FFFFFF";
        }
        tagText = t;
        tagColor = c;
    }

    // ---------- Farb-Helfer ----------
    public static boolean validHex(String s) { return s != null && s.matches("^#[0-9a-fA-F]{6}$"); }

    public static int parseHex(String s, int fallback) {
        return validHex(s) ? Integer.parseInt(s.substring(1), 16) : fallback;
    }

    public static int hsvToRgb(double h, double s, double v) {
        h = h - Math.floor(h);
        double c = v * s;
        double x = c * (1 - Math.abs((h * 6) % 2 - 1));
        double m = v - c;
        double r, g, b;
        switch ((int) (h * 6) % 6) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        return ((int) Math.round((r + m) * 255) << 16) | ((int) Math.round((g + m) * 255) << 8) | (int) Math.round((b + m) * 255);
    }

    public static double[] rgbToHsv(int rgb) {
        double r = ((rgb >> 16) & 255) / 255.0, g = ((rgb >> 8) & 255) / 255.0, b = (rgb & 255) / 255.0;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        double d = max - min;
        double h;
        if (d == 0) h = 0;
        else if (max == r) h = (((g - b) / d) % 6) / 6;
        else if (max == g) h = (((b - r) / d) + 2) / 6;
        else h = (((r - g) / d) + 4) / 6;
        if (h < 0) h += 1;
        return new double[]{h, max == 0 ? 0 : d / max, max};
    }

    private static int lerp(int a, int b, double t) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int scale(int a, double f) {
        int r = Math.min(255, (int) Math.round(((a >> 16) & 255) * f));
        int g = Math.min(255, (int) Math.round(((a >> 8) & 255) * f));
        int b = Math.min(255, (int) Math.round((a & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    /** Farbe fuer Zeichen i von n zum Zeitpunkt phase (in Umlaeufen). */
    private static int colorAt(String mode, int i, int n, double phase, int c1, int c2) {
        switch (mode == null ? "SOLID" : mode) {
            case "RAINBOW": {
                double h = phase - i * 0.07;
                return hsvToRgb(h - Math.floor(h), 1.0, 1.0);   // volle Saettigung + Helligkeit = grell
            }
            case "GRADIENT": {
                double pos = n <= 1 ? 0 : (double) i / (n - 1);
                double w = 0.5 + 0.5 * Math.sin(2 * Math.PI * (phase - pos));
                return lerp(c1, c2, w);
            }
            case "PULSE": {
                double w = 0.5 + 0.5 * (0.5 + 0.5 * Math.sin(2 * Math.PI * phase));
                return scale(c1, w + 0.1);
            }
            default:
                return c1;
        }
    }

    private boolean animated() {
        if (mode != null && !mode.equals("SOLID")) return true;
        for (String c : tagColor) if ("rainbow".equalsIgnoreCase(c)) return true;
        return false;
    }

    private static List<String> chars(String s) {
        List<String> l = new ArrayList<>();
        s.codePoints().forEach(cp -> l.add(new String(Character.toChars(cp))));
        return l;
    }

    private static JsonObject node(String text, int rgb) {
        JsonObject o = new JsonObject();
        o.addProperty("text", text);
        o.addProperty("color", String.format("#%06X", rgb & 0xFFFFFF));
        return o;
    }

    /** Gestylter Name mit Tags (egal ob "enabled") - auch fuer die Vorschau im Menue. */
    public Component build(String realName) {
        ensureTags();
        String name = (customName == null || customName.isBlank()) ? realName : customName;
        long bucket = animated() ? System.currentTimeMillis() / 50 : 0;
        StringBuilder kb = new StringBuilder();
        kb.append(name).append('|').append(mode).append('|').append(color).append('|').append(color2).append('|')
          .append(speed).append('|').append(font).append('|').append(bold).append(italic).append(underlined).append(obfuscated).append('|').append(bucket);
        for (int i = 0; i < TAG_COUNT; i++) kb.append('|').append(tagText[i]).append(tagColor[i]);
        String key = kb.toString();
        if (key.equals(cacheKey) && cacheValue != null) return cacheValue;

        double period = 3.0 / Math.max(0.1, speed);
        double phase = (System.currentTimeMillis() / 1000.0) / period;
        int c1 = parseHex(color, 0xFFFFFF), c2 = parseHex(color2, 0xFFFFFF);

        JsonArray extra = new JsonArray();
        // Tags vor dem Namen
        for (int i = 0; i < TAG_COUNT; i++) {
            String t = tagText[i];
            if (t == null || t.isBlank()) continue;
            t = t.trim() + " ";
            if ("rainbow".equalsIgnoreCase(tagColor[i])) {
                List<String> cs = chars(t);
                for (int k = 0; k < cs.size(); k++) extra.add(node(cs.get(k), colorAt("RAINBOW", k, cs.size(), phase, 0, 0)));
            } else {
                extra.add(node(t, parseHex(tagColor[i], 0xFFFFFF)));
            }
        }
        // Name, Zeichen fuer Zeichen gefaerbt
        JsonObject nameNode = new JsonObject();
        nameNode.addProperty("text", "");
        if (font != null && !font.isBlank() && !font.equals("minecraft:default")) nameNode.addProperty("font", font);
        if (bold) nameNode.addProperty("bold", true);
        if (italic) nameNode.addProperty("italic", true);
        if (underlined) nameNode.addProperty("underlined", true);
        if (obfuscated) nameNode.addProperty("obfuscated", true);
        JsonArray letters = new JsonArray();
        List<String> cs = chars(name);
        for (int i = 0; i < cs.size(); i++) letters.add(node(cs.get(i), colorAt(mode, i, cs.size(), phase, c1, c2)));
        nameNode.add("extra", letters);
        extra.add(nameNode);

        JsonObject root = new JsonObject();
        root.addProperty("text", "");
        root.add("extra", extra);

        Component c = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, root).result()
                .orElseGet(() -> Component.literal(name));
        cacheKey = key;
        cacheValue = c;
        return c;
    }

    /** Gestylter Name fuer das Spiel - null wenn die Mod aus ist. */
    public static Component forPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getUser() == null || !instance.enabled) return null;
        return instance.build(mc.getUser().getName());
    }

    /** Ersetzt den eigenen Namen in einer Chatnachricht durch den gestylten Namen. */
    public static Component restyleChat(Component msg) {
        Component styled = forPlayer();
        if (styled == null || msg == null) return msg;
        String real = Minecraft.getInstance().getUser().getName();
        if (!msg.getString().contains(real)) return msg;

        Pattern p = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(real) + "(?![A-Za-z0-9_])");
        MutableComponent out = Component.empty();
        msg.visit((style, text) -> {
            Matcher m = p.matcher(text);
            int last = 0;
            while (m.find()) {
                if (m.start() > last) out.append(Component.literal(text.substring(last, m.start())).setStyle(style));
                out.append(styled.copy().setStyle(styled.getStyle().applyTo(style)));
                last = m.end();
            }
            if (last < text.length()) out.append(Component.literal(text.substring(last)).setStyle(style));
            return Optional.<Object>empty();
        }, Style.EMPTY);
        return out;
    }
}
