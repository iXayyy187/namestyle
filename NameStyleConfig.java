package dev.namestyle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Einstellungen (config/namestyle.json) + Aufbau des gestylten Namens. */
public final class NameStyleConfig {
    public boolean enabled = true;
    public boolean showOwnNametag = true;   // eigenen Namen in der 3. Person anzeigen
    public String customName = "";           // leer = echter Name
    public String color = "#FF55FF";
    public String font = "minecraft:default"; // auch eigene Fonts aus Resource Packs moeglich
    public boolean bold, italic, underlined, obfuscated;

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

    public static boolean validHex(String s) {
        return s != null && s.matches("^#[0-9a-fA-F]{6}$");
    }

    /** Gestylter Name (egal ob "enabled"), fuer die Vorschau im Menue. */
    public Component build(String realName) {
        String name = (customName == null || customName.isBlank()) ? realName : customName;
        String col = validHex(color) ? color : "#FFFFFF";
        String key = name + "|" + col + "|" + font + "|" + bold + italic + underlined + obfuscated;
        if (key.equals(cacheKey) && cacheValue != null) return cacheValue;

        JsonObject o = new JsonObject();
        o.addProperty("text", name);
        o.addProperty("color", col);
        if (font != null && !font.isBlank() && !font.equals("minecraft:default")) o.addProperty("font", font);
        if (bold) o.addProperty("bold", true);
        if (italic) o.addProperty("italic", true);
        if (underlined) o.addProperty("underlined", true);
        if (obfuscated) o.addProperty("obfuscated", true);

        Component c = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, o).result()
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
