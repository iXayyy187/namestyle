package dev.namestyleplus;

import dev.namestyle.NameStyleColorWheel;
import dev.namestyle.NameStyleConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

/**
 * Schwarz-weisse Oberflaeche fuer NameStyle mit drei Tabs:
 * NAME (alles Bisherige), TAGS (inkl. fett/kursiv/unterstrichen pro Tag), SAETTIGUNG (Regler).
 *
 * Flaechen werden von einem unsichtbaren "Backdrop"-Widget gezeichnet, das als erstes Widget
 * registriert wird. Texte und Eingabefelder sind normale Widgets darueber.
 */
public class PlusScreen extends Screen {
    private static final int BLACK = 0xFF000000;
    private static final int PANEL = 0xFF0A0A0A;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFF6A6A6A;
    private static final int HOVER = 0xFF262626;

    private static final String[] FONTS = {"minecraft:default", "minecraft:uniform", "minecraft:alt", "minecraft:illageralt"};
    private static final String[] FONT_LABELS = {"Standard", "Uniform", "Runen", "Illager"};
    private static final String[] MODES = {"SOLID", "RAINBOW", "GRADIENT", "PULSE"};
    private static final String[] MODE_LABELS = {"Einfarbig", "Regenbogen", "Verlauf", "Pulsieren"};

    private final Screen parent;
    private int tab = 0;
    private int px;
    private int py;
    private int pw;
    private int ph;

    private final List<Btn> buttons = new ArrayList<>();
    private final List<Slider> sliders = new ArrayList<>();
    private Slider activeSlider;

    private StringWidget preview;
    private StringWidget swatchLabel1;
    private StringWidget swatchLabel2;
    private EditBox hex1;
    private EditBox hex2;
    private NameStyleColorWheel wheel;
    private Slider brightness;
    private boolean editing2;
    private boolean syncing;
    private double h;
    private double s;
    private double v = 1.0;

    public PlusScreen(Screen parent) {
        super(Component.literal("NameStyle Plus"));
        this.parent = parent;
    }

    public Screen parentScreen() {
        return parent;
    }

    // ------------------------------------------------------------------ Aufbau

    @Override
    protected void init() {
        buttons.clear();
        sliders.clear();
        activeSlider = null;
        swatchLabel1 = null;
        swatchLabel2 = null;

        NameStyleConfig c = NameStyleConfig.get();
        c.ensureTags();
        PlusConfig p = PlusConfig.get();
        p.ensure();

        pw = Math.min(360, width - 8);
        ph = Math.min(250, height - 8);
        px = (width - pw) / 2;
        py = (height - ph) / 2;

        // Backdrop MUSS zuerst hinzugefuegt werden (wird unter allem anderen gezeichnet).
        addRenderableWidget(new Backdrop(this, width, height));

        preview = new StringWidget(px + 12, py + 10, pw - 24, 12, Component.empty(), font);
        addRenderableWidget(preview);

        String[] tabNames = {"NAME", "TAGS", "SÄTTIGUNG"};
        int tw = (pw - 24 - 8) / 3;
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            addBtn(px + 12 + i * (tw + 4), py + 30, tw, 16, () -> tabNames[idx], 0, () -> {
                if (tab != idx) {
                    tab = idx;
                    clearWidgets();
                    init();
                }
            }, () -> tab == idx);
        }

        switch (tab) {
            case 0 -> initName(c);
            case 1 -> initTags(c, p);
            default -> initSaturation(p);
        }

        addBtn(px + pw - 12 - 90, py + ph - 26, 90, 16, () -> "FERTIG", 0, this::onClose, () -> false);
        refresh();
    }

    private void initName(NameStyleConfig c) {
        int lx = px + 12;
        int lw = 170;
        int rx = px + 192;
        int y0 = py + 56;

        EditBox nameBox = new EditBox(font, lx, y0, lw, 18, Component.literal("Name"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.literal("Eigener Name (leer = echter Name)"));
        nameBox.setValue(c.customName == null ? "" : c.customName);
        nameBox.setResponder(val -> c.customName = val);
        addRenderableWidget(nameBox);

        addBtn(lx, y0 + 24, lw, 18,
                () -> "MODUS: " + MODE_LABELS[idx(MODES, c.mode)].toUpperCase(Locale.ROOT), 0,
                () -> c.mode = MODES[(idx(MODES, c.mode) + 1) % MODES.length], () -> false);
        addBtn(lx, y0 + 48, lw, 18,
                () -> "SCHRIFT: " + FONT_LABELS[idx(FONTS, c.font)].toUpperCase(Locale.ROOT), 0,
                () -> c.font = FONTS[(idx(FONTS, c.font) + 1) % FONTS.length], () -> false);

        int bw = (lw - 4) / 2;
        addBtn(lx, y0 + 72, bw, 18, () -> "FETT", 1, () -> c.bold = !c.bold, () -> c.bold);
        addBtn(lx + bw + 4, y0 + 72, bw, 18, () -> "KURSIV", 2, () -> c.italic = !c.italic, () -> c.italic);
        addBtn(lx, y0 + 94, bw, 18, () -> "UNTERSTR.", 3, () -> c.underlined = !c.underlined, () -> c.underlined);
        addBtn(lx + bw + 4, y0 + 94, bw, 18, () -> "ZUFALL", 0, () -> c.obfuscated = !c.obfuscated, () -> c.obfuscated);

        new Slider(lx, y0 + 118, lw, "TEMPO", (c.speed - 0.2) / 3.6,
                f -> String.format(Locale.ROOT, "%.1fx", 0.2 + f * 3.6),
                f -> c.speed = Math.round((0.2 + f * 3.6) * 10.0) / 10.0);

        addBtn(lx, y0 + 142, bw, 18, () -> "MOD AKTIV", 0, () -> c.enabled = !c.enabled, () -> c.enabled);
        addBtn(lx + bw + 4, y0 + 142, bw, 18, () -> "EIGENER TAG", 0,
                () -> c.showOwnNametag = !c.showOwnNametag, () -> c.showOwnNametag);

        // Rechte Spalte: Farbrad + Hex-Felder
        wheel = new NameStyleColorWheel(rx, y0, 90, (hue, sat) -> {
            h = hue;
            s = sat;
            colorFromWheel();
        });
        addRenderableWidget(wheel);

        hex1 = new EditBox(font, rx, y0 + 96, 72, 18, Component.literal("Farbe 1"));
        hex1.setMaxLength(7);
        hex1.setHint(Component.literal("#RRGGBB"));
        hex1.setValue(c.color);
        hex1.setResponder(val -> {
            if (NameStyleConfig.validHex(val)) {
                c.color = val;
                if (!syncing && !editing2) {
                    loadWheel(val);
                }
            }
        });
        addRenderableWidget(hex1);

        hex2 = new EditBox(font, rx + 78, y0 + 96, 72, 18, Component.literal("Farbe 2"));
        hex2.setMaxLength(7);
        hex2.setHint(Component.literal("#RRGGBB"));
        hex2.setValue(c.color2);
        hex2.setResponder(val -> {
            if (NameStyleConfig.validHex(val)) {
                c.color2 = val;
                if (!syncing && editing2) {
                    loadWheel(val);
                }
            }
        });
        addRenderableWidget(hex2);

        addBtn(rx, y0 + 118, 150, 18, () -> "RAD BEARBEITET: FARBE " + (editing2 ? "2" : "1"), 0, () -> {
            editing2 = !editing2;
            loadWheel(editing2 ? c.color2 : c.color);
        }, () -> false);

        brightness = new Slider(rx, y0 + 142, 150, "HELLIGKEIT", v,
                f -> String.format(Locale.ROOT, "%d%%", Math.round(f * 100.0)),
                f -> {
                    v = f;
                    wheel.setValue(f);
                    colorFromWheel();
                });
        loadWheel(editing2 ? c.color2 : c.color);
    }

    private void initTags(NameStyleConfig c, PlusConfig p) {
        int y0 = py + 56;
        label(px + 12, y0, pw - 24, "Tags vor dem Namen  |  Farbe: #RRGGBB oder rainbow  |  B = fett, I = kursiv, U = unterstrichen", GRAY);
        for (int i = 0; i < NameStyleConfig.TAG_COUNT; i++) {
            final int t = i;
            int y = y0 + 16 + i * 26;

            EditBox text = new EditBox(font, px + 12, y, 150, 18, Component.literal("Tag"));
            text.setMaxLength(20);
            text.setHint(Component.literal("Tag " + (i + 1)));
            text.setValue(c.tagText[t]);
            text.setResponder(val -> c.tagText[t] = val);
            addRenderableWidget(text);

            EditBox col = new EditBox(font, px + 168, y, 70, 18, Component.literal("Farbe"));
            col.setMaxLength(7);
            col.setHint(Component.literal("#FFAA00"));
            col.setValue(c.tagColor[t]);
            col.setResponder(val -> c.tagColor[t] = val);
            addRenderableWidget(col);

            addBtn(px + 244, y, 26, 18, () -> "B", 1, () -> p.tagBold[t] = !p.tagBold[t], () -> p.tagBold[t]);
            addBtn(px + 274, y, 26, 18, () -> "I", 2, () -> p.tagItalic[t] = !p.tagItalic[t], () -> p.tagItalic[t]);
            addBtn(px + 304, y, 26, 18, () -> "U", 3, () -> p.tagUnderlined[t] = !p.tagUnderlined[t], () -> p.tagUnderlined[t]);
        }
    }

    private void initSaturation(PlusConfig p) {
        int y0 = py + 56;
        new Slider(px + 12, y0, pw - 24, "SÄTTIGUNG", p.saturation / 2.0,
                f -> String.format(Locale.ROOT, "%d%%", Math.round(f * 200.0)),
                f -> p.saturation = Math.round(f * 200.0) / 100.0);
        addBtn(px + 12, y0 + 30, 170, 18, () -> "ZURÜCKSETZEN (100%)", 0, () -> {
            p.saturation = 1.0;
            clearWidgets();
            init();
        }, () -> false);
        addBtn(px + 188, y0 + 30, 140, 18, () -> "SCHWARZ-WEISS (0%)", 0, () -> {
            p.saturation = 0.0;
            clearWidgets();
            init();
        }, () -> false);
        swatchLabel1 = label(px + 12, y0 + 62, 200, "ORIGINAL", GRAY);
        swatchLabel2 = label(px + 12, y0 + 98, 200, "MIT SÄTTIGUNG", GRAY);
    }

    // ------------------------------------------------------------------ Logik

    private static int idx(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private void loadWheel(String hex) {
        int rgb = NameStyleConfig.parseHex(hex, 0xFFFFFF);
        double[] hsv = NameStyleConfig.rgbToHsv(rgb);
        h = hsv[0];
        s = hsv[1];
        v = hsv[2];
        if (wheel != null) {
            wheel.setHsv(h, s, v);
        }
        if (brightness != null) {
            brightness.value = v;
        }
    }

    private void colorFromWheel() {
        NameStyleConfig c = NameStyleConfig.get();
        String hex = String.format("#%06X", NameStyleConfig.hsvToRgb(h, s, v) & 0xFFFFFF);
        syncing = true;
        if (editing2) {
            c.color2 = hex;
            hex2.setValue(hex);
        } else {
            c.color = hex;
            hex1.setValue(hex);
        }
        syncing = false;
    }

    private void refresh() {
        String name = minecraft != null && minecraft.getUser() != null ? minecraft.getUser().getName() : "Player";
        if (preview != null) {
            preview.setMessage(NameStyleConfig.get().build(name));
        }
    }

    @Override
    public void onClose() {
        NameStyleConfig.save();
        PlusConfig.save();
        minecraft.gui.setScreen(parent);
    }

    // ------------------------------------------------------------------ Eingabe

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        for (Btn b : buttons) {
            if (b.hit(mx, my)) {
                b.action.run();
                return true;
            }
        }
        for (Slider sl : sliders) {
            if (sl.hit(mx, my)) {
                activeSlider = sl;
                sl.drag(mx);
                return true;
            }
        }
        activeSlider = null;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (activeSlider != null) {
            activeSlider.drag(event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    // ------------------------------------------------------------------ Zeichnen

    private static void rect(GuiGraphicsExtractor g, int x, int y, int w, int hgt, int argb) {
        g.fillGradient(x, y, x + w, y + hgt, argb, argb);
    }

    private static void outline(GuiGraphicsExtractor g, int x, int y, int w, int hgt, int argb) {
        rect(g, x, y, w, 1, argb);
        rect(g, x, y + hgt - 1, w, 1, argb);
        rect(g, x, y, 1, hgt, argb);
        rect(g, x + w - 1, y, 1, hgt, argb);
    }

    private static Component colored(String text, int rgb, int deco) {
        Style st = Style.EMPTY.withColor(TextColor.fromRgb(rgb));
        if (deco == 1) {
            st = st.withBold(true);
        } else if (deco == 2) {
            st = st.withItalic(true);
        } else if (deco == 3) {
            st = st.withUnderlined(true);
        }
        return Component.literal(text).setStyle(st);
    }

    private StringWidget label(int x, int y, int w, String text, int argb) {
        StringWidget sw = new StringWidget(x, y, w, 9, colored(text, argb & 0xFFFFFF, 0), font);
        addRenderableWidget(sw);
        return sw;
    }

    void drawBackdrop(GuiGraphicsExtractor g, int mx, int my) {
        refresh();
        rect(g, 0, 0, width, height, BLACK);
        rect(g, px, py, pw, ph, PANEL);
        outline(g, px, py, pw, ph, WHITE);
        rect(g, px + 12, py + 24, pw - 24, 1, GRAY);
        rect(g, px + 12, py + ph - 32, pw - 24 - 100, 1, GRAY);

        for (Btn b : buttons) {
            b.draw(g, mx, my);
        }
        for (Slider sl : sliders) {
            sl.draw(g);
        }

        if (tab == 2) {
            double sat = PlusConfig.get().saturation;
            int y0 = py + 56;
            int n = 10;
            int sw = (pw - 24) / n;
            for (int i = 0; i < n; i++) {
                int base = NameStyleConfig.hsvToRgb(i / (double) n, 1.0, 1.0) & 0xFFFFFF;
                rect(g, px + 12 + i * sw, y0 + 74, sw - 2, 16, 0xFF000000 | base);
                rect(g, px + 12 + i * sw, y0 + 110, sw - 2, 16, 0xFF000000 | PlusStyler.saturate(base, sat));
            }
        }
    }

    // ------------------------------------------------------------------ Bausteine

    private void addBtn(int x, int y, int w, int hgt, Supplier<String> text, int deco, Runnable action, BooleanSupplier selected) {
        StringWidget sw = new StringWidget(x, y + (hgt - 8) / 2, w, 9, Component.empty(), font);
        addRenderableWidget(sw);
        buttons.add(new Btn(x, y, w, hgt, text, deco, action, selected, sw));
    }

    private final class Btn {
        final int x;
        final int y;
        final int w;
        final int hgt;
        final Supplier<String> text;
        final int deco;
        final Runnable action;
        final BooleanSupplier selected;
        final StringWidget sw;

        Btn(int x, int y, int w, int hgt, Supplier<String> text, int deco, Runnable action,
            BooleanSupplier selected, StringWidget sw) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.hgt = hgt;
            this.text = text;
            this.deco = deco;
            this.action = action;
            this.selected = selected;
            this.sw = sw;
        }

        boolean hit(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + hgt;
        }

        void draw(GuiGraphicsExtractor g, int mx, int my) {
            boolean sel = selected.getAsBoolean();
            boolean hover = hit(mx, my);
            rect(g, x, y, w, hgt, sel ? WHITE : (hover ? HOVER : BLACK));
            outline(g, x, y, w, hgt, sel || hover ? WHITE : GRAY);
            String t = text.get();
            int tw = font.width(t);
            sw.setX(x + Math.max(2, (w - tw) / 2));
            sw.setWidth(Math.max(tw + 2, 4));
            sw.setMessage(colored(t, sel ? 0x000000 : 0xFFFFFF, deco));
        }
    }

    private final class Slider {
        final int x;
        final int y;
        final int w;
        final String name;
        final DoubleFunction<String> fmt;
        final DoubleConsumer onChange;
        final StringWidget sw;
        double value;

        Slider(int x, int y, int w, String name, double value, DoubleFunction<String> fmt, DoubleConsumer onChange) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.name = name;
            this.value = Math.max(0.0, Math.min(1.0, value));
            this.fmt = fmt;
            this.onChange = onChange;
            this.sw = new StringWidget(x, y, w, 9, Component.empty(), font);
            addRenderableWidget(sw);
            sliders.add(this);
        }

        boolean hit(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + 20;
        }

        void drag(double mx) {
            value = Math.max(0.0, Math.min(1.0, (mx - x) / w));
            onChange.accept(value);
        }

        void draw(GuiGraphicsExtractor g) {
            sw.setMessage(colored(name + ": " + fmt.apply(value), 0xFFFFFF, 0));
            int ty = y + 12;
            rect(g, x, ty, w, 6, BLACK);
            outline(g, x, ty, w, 6, GRAY);
            int filled = (int) Math.round(value * (w - 2));
            rect(g, x + 1, ty + 1, filled, 4, WHITE);
            rect(g, x + Math.min(w - 3, filled), ty - 2, 3, 10, WHITE);
        }
    }

    /** Unsichtbares Widget, das alle Flaechen zeichnet und keine Klicks schluckt. */
    private static final class Backdrop extends AbstractWidget {
        private final PlusScreen screen;

        Backdrop(PlusScreen screen, int w, int h) {
            super(0, 0, w, h, Component.empty());
            this.screen = screen;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
            screen.drawBackdrop(g, mx, my);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            return false;
        }

        @Override
        public boolean isMouseOver(double mx, double my) {
            return false;
        }
    }
}
