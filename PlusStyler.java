package dev.namestyleplus;

import dev.namestyle.NameStyleConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;

/**
 * Bearbeitet das fertige Name-Component von NameStyle nachtraeglich:
 * - Tag-Stile (fett/kursiv/unterstrichen) pro Tag
 * - globale Farbsaettigung auf alle Farben
 *
 * Aufbau des Components von NameStyleConfig.build():
 *   leere Wurzel -> [Tag-Knoten ...] [Namens-Knoten mit einem Kind pro Zeichen]
 * Ein Tag ist ein Knoten, ausser bei "rainbow": dann ein Knoten pro Zeichen.
 */
public final class PlusStyler {
    private PlusStyler() {
    }

    public static Component apply(Component root) {
        PlusConfig p = PlusConfig.get();
        NameStyleConfig c = NameStyleConfig.get();
        double sat = p.saturation;

        boolean anyTagStyle = false;
        for (int t = 0; t < PlusConfig.TAGS; t++) {
            if (p.tagBold[t] || p.tagItalic[t] || p.tagUnderlined[t]) {
                anyTagStyle = true;
            }
        }
        if (!anyTagStyle && Math.abs(sat - 1.0) < 1.0e-6) {
            return root;
        }

        List<Component> sib = root.getSiblings();
        MutableComponent out = MutableComponent.create(root.getContents());
        out.setStyle(root.getStyle());

        int i = 0;
        for (int t = 0; t < NameStyleConfig.TAG_COUNT && i < sib.size(); t++) {
            String text = c.tagText[t];
            if (text == null || text.isBlank()) {
                continue;
            }
            int count = 1;
            if ("rainbow".equalsIgnoreCase(c.tagColor[t])) {
                String trimmed = text.trim();
                count = trimmed.codePointCount(0, trimmed.length());
            }
            for (int k = 0; k < count && i < sib.size(); k++) {
                out.append(map(sib.get(i++), sat, p.tagBold[t], p.tagItalic[t], p.tagUnderlined[t]));
            }
        }
        while (i < sib.size()) {
            out.append(map(sib.get(i++), sat, false, false, false));
        }
        return out;
    }

    private static MutableComponent map(Component c, double sat, boolean bold, boolean italic, boolean underlined) {
        MutableComponent m = MutableComponent.create(c.getContents());
        Style st = c.getStyle();
        TextColor tc = st.getColor();
        if (tc != null) {
            st = st.withColor(TextColor.fromRgb(saturate(tc.getValue(), sat)));
        }
        if (bold) {
            st = st.withBold(true);
        }
        if (italic) {
            st = st.withItalic(true);
        }
        if (underlined) {
            st = st.withUnderlined(true);
        }
        m.setStyle(st);
        for (Component s : c.getSiblings()) {
            m.append(map(s, sat, bold, italic, underlined));
        }
        return m;
    }

    /** Skaliert die HSV-Saettigung einer RGB-Farbe (0 = grau, 1 = original, 2 = doppelt, max. 100%). */
    public static int saturate(int rgb, double sat) {
        rgb &= 0xFFFFFF;
        if (Math.abs(sat - 1.0) < 1.0e-6) {
            return rgb;
        }
        double[] hsv = NameStyleConfig.rgbToHsv(rgb);
        double s = Math.max(0.0, Math.min(1.0, hsv[1] * sat));
        return NameStyleConfig.hsvToRgb(hsv[0], s, hsv[2]) & 0xFFFFFF;
    }
}
