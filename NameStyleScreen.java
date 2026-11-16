package dev.namestyle;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public class NameStyleScreen extends Screen {
    private static final String[] FONTS = {"minecraft:default", "minecraft:uniform", "minecraft:alt", "minecraft:illageralt"};
    private static final String[] FONT_LABELS = {"Standard", "Uniform", "Runen (Galactic)", "Illager"};
    private static final String[] MODES = {"SOLID", "RAINBOW", "GRADIENT", "PULSE"};
    private static final String[] MODE_LABELS = {"Einfarbig", "Regenbogen (grell)", "Verlauf (animiert)", "Pulsieren"};

    private final Screen parent;
    private EditBox nameBox, hexBox1, hexBox2;
    private StringWidget preview;
    private NameStyleColorWheel wheel;
    private ValueSlider brightness;
    private boolean editing2 = false;
    private boolean syncing = false;
    private double h, s, v = 1.0;

    public NameStyleScreen(Screen parent) {
        super(Component.literal("NameStyle"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        NameStyleConfig c = NameStyleConfig.get();
        c.ensureTags();
        int cx = this.width / 2;
        int y = Math.max(6, this.height / 2 - 140);

        preview = new StringWidget(cx - 150, y, 300, 12, Component.literal(""), this.font);
        this.addRenderableWidget(preview);
        y += 20;

        nameBox = new EditBox(this.font, cx - 150, y, 300, 20, Component.literal("Name"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.literal("Eigener Name (leer = echter Name)"));
        nameBox.setValue(c.customName == null ? "" : c.customName);
        nameBox.setResponder(str -> { NameStyleConfig.get().customName = str; refresh(); });
        this.addRenderableWidget(nameBox);
        y += 26;

        double[] hsv = NameStyleConfig.rgbToHsv(NameStyleConfig.parseHex(c.color, 0xFF55FF));
        h = hsv[0]; s = hsv[1]; v = hsv[2];
        int top = y;

        // ---- linke Seite: Farbrad ----
        wheel = new NameStyleColorWheel(cx - 150, top, 96, (hh, ss) -> { h = hh; s = ss; colorFromWheel(); });
        wheel.setHsv(h, s, v);
        this.addRenderableWidget(wheel);

        // ---- rechte Seite ----
        int rx = cx - 40, rw = 190, ry = top;

        int modeIdx = 0;
        for (int i = 0; i < MODES.length; i++) if (MODES[i].equals(c.mode)) modeIdx = i;
        final int[] mi = {modeIdx};
        this.addRenderableWidget(Button.builder(Component.literal("Modus: " + MODE_LABELS[modeIdx]), btn -> {
            mi[0] = (mi[0] + 1) % MODES.length;
            NameStyleConfig.get().mode = MODES[mi[0]];
            btn.setMessage(Component.literal("Modus: " + MODE_LABELS[mi[0]]));
            refresh();
        }).bounds(rx, ry, rw, 20).build());
        ry += 22;

        hexBox1 = new EditBox(this.font, rx, ry, rw, 20, Component.literal("Farbe 1"));
        hexBox1.setMaxLength(7);
        hexBox1.setHint(Component.literal("Farbe 1 (#RRGGBB)"));
        hexBox1.setValue(c.color);
        hexBox1.setResponder(str -> {
            if (syncing || !NameStyleConfig.validHex(str)) return;
            NameStyleConfig.get().color = str.toUpperCase();
            if (!editing2) loadWheel(NameStyleConfig.get().color);
            refresh();
        });
        this.addRenderableWidget(hexBox1);
        ry += 22;

        hexBox2 = new EditBox(this.font, rx, ry, rw, 20, Component.literal("Farbe 2"));
        hexBox2.setMaxLength(7);
        hexBox2.setHint(Component.literal("Farbe 2 (#RRGGBB, fuer Verlauf)"));
        hexBox2.setValue(c.color2);
        hexBox2.setResponder(str -> {
            if (syncing || !NameStyleConfig.validHex(str)) return;
            NameStyleConfig.get().color2 = str.toUpperCase();
            if (editing2) loadWheel(NameStyleConfig.get().color2);
            refresh();
        });
        this.addRenderableWidget(hexBox2);
        ry += 22;

        this.addRenderableWidget(Button.builder(Component.literal("Farbrad bearbeitet: Farbe 1"), btn -> {
            editing2 = !editing2;
            btn.setMessage(Component.literal("Farbrad bearbeitet: Farbe " + (editing2 ? "2" : "1")));
            NameStyleConfig cfg = NameStyleConfig.get();
            loadWheel(editing2 ? cfg.color2 : cfg.color);
        }).bounds(rx, ry, rw, 20).build());
        ry += 22;

        brightness = new ValueSlider(rx, ry, rw, 20, "Helligkeit", v, f -> String.format("%d%%", (int) Math.round(f * 100)), f -> {
            v = Math.max(0.05, f);
            wheel.setValue(v);
            colorFromWheel();
        });
        this.addRenderableWidget(brightness);
        ry += 22;

        double sp = (c.speed - 0.2) / 3.8;
        this.addRenderableWidget(new ValueSlider(rx, ry, rw, 20, "Tempo", Math.max(0, Math.min(1, sp)),
                f -> String.format("%.1fx", 0.2 + f * 3.8),
                f -> { NameStyleConfig.get().speed = 0.2 + f * 3.8; refresh(); }));
        ry += 22;

        int idx = 0;
        for (int i = 0; i < FONTS.length; i++) if (FONTS[i].equals(c.font)) idx = i;
        final int[] fontIdx = {idx};
        this.addRenderableWidget(Button.builder(Component.literal("Schrift: " + FONT_LABELS[idx]), btn -> {
            fontIdx[0] = (fontIdx[0] + 1) % FONTS.length;
            NameStyleConfig.get().font = FONTS[fontIdx[0]];
            btn.setMessage(Component.literal("Schrift: " + FONT_LABELS[fontIdx[0]]));
            refresh();
        }).bounds(rx, ry, rw, 20).build());
        ry += 22;

        y = Math.max(ry, top + 100) + 6;

        this.addRenderableWidget(toggle(cx - 150, y, 72, "Fett", () -> NameStyleConfig.get().bold, b -> NameStyleConfig.get().bold = b));
        this.addRenderableWidget(toggle(cx - 74, y, 72, "Kursiv", () -> NameStyleConfig.get().italic, b -> NameStyleConfig.get().italic = b));
        this.addRenderableWidget(toggle(cx + 2, y, 72, "Unterstr.", () -> NameStyleConfig.get().underlined, b -> NameStyleConfig.get().underlined = b));
        this.addRenderableWidget(toggle(cx + 78, y, 72, "Zufall", () -> NameStyleConfig.get().obfuscated, b -> NameStyleConfig.get().obfuscated = b));
        y += 24;

        this.addRenderableWidget(Button.builder(Component.literal("Tags vor Namen..."), btn -> {
            NameStyleConfig.save();
            this.minecraft.gui.setScreen(new NameStyleTagsScreen(this));
        }).bounds(cx - 150, y, 98, 20).build());
        this.addRenderableWidget(toggle(cx - 48, y, 98, "Mod aktiv", () -> NameStyleConfig.get().enabled, b -> NameStyleConfig.get().enabled = b));
        this.addRenderableWidget(toggle(cx + 52, y, 98, "Eigener Nametag", () -> NameStyleConfig.get().showOwnNametag, b -> NameStyleConfig.get().showOwnNametag = b));
        y += 28;

        this.addRenderableWidget(Button.builder(Component.literal("Fertig"), btn -> this.onClose())
                .bounds(cx - 100, y, 200, 20).build());
        refresh();
    }

    private void loadWheel(String hex) {
        double[] hsv = NameStyleConfig.rgbToHsv(NameStyleConfig.parseHex(hex, 0xFFFFFF));
        h = hsv[0]; s = hsv[1]; v = hsv[2];
        wheel.setHsv(h, s, v);
        brightness.setFraction(v);
    }

    private void colorFromWheel() {
        String hex = String.format("#%06X", NameStyleConfig.hsvToRgb(h, s, v));
        syncing = true;
        if (!editing2) { NameStyleConfig.get().color = hex; hexBox1.setValue(hex); }
        else { NameStyleConfig.get().color2 = hex; hexBox2.setValue(hex); }
        syncing = false;
        refresh();
    }

    private void refresh() {
        String real = (this.minecraft != null && this.minecraft.getUser() != null) ? this.minecraft.getUser().getName() : "Player";
        if (preview != null) preview.setMessage(NameStyleConfig.get().build(real));
    }

    private Button toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        return Button.builder(toggleText(label, get.getAsBoolean()), btn -> {
            boolean val = !get.getAsBoolean();
            set.accept(val);
            btn.setMessage(toggleText(label, val));
            refresh();
        }).bounds(x, y, w, 20).build();
    }

    private static Component toggleText(String label, boolean on) {
        return Component.literal(label + ": " + (on ? "AN" : "AUS"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        refresh();   // Vorschau laeuft animiert mit
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        NameStyleConfig.save();
        if (this.minecraft != null) this.minecraft.gui.setScreen(parent);
    }

    private static class ValueSlider extends AbstractSliderButton {
        private final String label;
        private final DoubleFunction<String> fmt;
        private final DoubleConsumer onChange;

        ValueSlider(int x, int y, int w, int hgt, String label, double v01, DoubleFunction<String> fmt, DoubleConsumer onChange) {
            super(x, y, w, hgt, Component.empty(), v01);
            this.label = label;
            this.fmt = fmt;
            this.onChange = onChange;
            updateMessage();
        }

        void setFraction(double f) { this.value = Math.max(0, Math.min(1, f)); updateMessage(); }

        @Override protected void updateMessage() {
            if (label != null && fmt != null) setMessage(Component.literal(label + ": " + fmt.apply(this.value)));
        }

        @Override protected void applyValue() {
            if (onChange != null) onChange.accept(this.value);
        }
    }
}
