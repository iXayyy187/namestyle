package dev.namestyle;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class NameStyleScreen extends Screen {
    private static final String[] FONTS = {"minecraft:default", "minecraft:uniform", "minecraft:alt", "minecraft:illageralt"};
    private static final String[] FONT_LABELS = {"Standard", "Uniform", "Runen (Galactic)", "Illager"};

    private final Screen parent;
    private EditBox nameBox, hexBox;
    private ChannelSlider sr, sg, sb;
    private StringWidget preview;
    private Button fontBtn;
    private int r, g, b;
    private boolean syncing = false;

    public NameStyleScreen(Screen parent) {
        super(Component.literal("NameStyle"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        NameStyleConfig c = NameStyleConfig.get();
        int cx = this.width / 2;
        int y = Math.max(8, this.height / 2 - 125);

        int rgb = NameStyleConfig.validHex(c.color) ? Integer.parseInt(c.color.substring(1), 16) : 0xFFFFFF;
        r = (rgb >> 16) & 255; g = (rgb >> 8) & 255; b = rgb & 255;

        preview = new StringWidget(cx - 100, y, 200, 12, Component.literal(""), this.font);
        preview.alignCenter();
        this.addRenderableWidget(preview);
        y += 22;

        nameBox = new EditBox(this.font, cx - 100, y, 200, 20, Component.literal("Name"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.literal("Eigener Name (leer = echter Name)"));
        nameBox.setValue(c.customName == null ? "" : c.customName);
        nameBox.setResponder(s -> { NameStyleConfig.get().customName = s; refresh(); });
        this.addRenderableWidget(nameBox);
        y += 26;

        hexBox = new EditBox(this.font, cx - 100, y, 200, 20, Component.literal("Farbe"));
        hexBox.setMaxLength(7);
        hexBox.setValue(hex());
        hexBox.setResponder(s -> {
            if (syncing || !NameStyleConfig.validHex(s)) return;
            int v = Integer.parseInt(s.substring(1), 16);
            r = (v >> 16) & 255; g = (v >> 8) & 255; b = v & 255;
            syncing = true; sr.setInt(r); sg.setInt(g); sb.setInt(b); syncing = false;
            NameStyleConfig.get().color = s.toUpperCase();
            refresh();
        });
        this.addRenderableWidget(hexBox);
        y += 24;

        sr = new ChannelSlider(cx - 100, y, 200, 20, "Rot", r, v -> { r = v; colorChanged(); });
        this.addRenderableWidget(sr); y += 22;
        sg = new ChannelSlider(cx - 100, y, 200, 20, "Gruen", g, v -> { g = v; colorChanged(); });
        this.addRenderableWidget(sg); y += 22;
        sb = new ChannelSlider(cx - 100, y, 200, 20, "Blau", b, v -> { b = v; colorChanged(); });
        this.addRenderableWidget(sb); y += 26;

        int idx = 0;
        for (int i = 0; i < FONTS.length; i++) if (FONTS[i].equals(c.font)) idx = i;
        final int[] fontIdx = {idx};
        fontBtn = Button.builder(Component.literal("Schrift: " + FONT_LABELS[idx]), btn -> {
            fontIdx[0] = (fontIdx[0] + 1) % FONTS.length;
            NameStyleConfig.get().font = FONTS[fontIdx[0]];
            btn.setMessage(Component.literal("Schrift: " + FONT_LABELS[fontIdx[0]]));
            refresh();
        }).bounds(cx - 100, y, 200, 20).build();
        this.addRenderableWidget(fontBtn);
        y += 24;

        this.addRenderableWidget(toggle(cx - 100, y, 98, "Fett", () -> NameStyleConfig.get().bold, v -> NameStyleConfig.get().bold = v));
        this.addRenderableWidget(toggle(cx + 2, y, 98, "Kursiv", () -> NameStyleConfig.get().italic, v -> NameStyleConfig.get().italic = v));
        y += 22;
        this.addRenderableWidget(toggle(cx - 100, y, 98, "Unterstrichen", () -> NameStyleConfig.get().underlined, v -> NameStyleConfig.get().underlined = v));
        this.addRenderableWidget(toggle(cx + 2, y, 98, "Zufallstext", () -> NameStyleConfig.get().obfuscated, v -> NameStyleConfig.get().obfuscated = v));
        y += 26;

        this.addRenderableWidget(toggle(cx - 100, y, 98, "Mod aktiv", () -> NameStyleConfig.get().enabled, v -> NameStyleConfig.get().enabled = v));
        this.addRenderableWidget(toggle(cx + 2, y, 98, "Eigener Nametag", () -> NameStyleConfig.get().showOwnNametag, v -> NameStyleConfig.get().showOwnNametag = v));
        y += 28;

        this.addRenderableWidget(Button.builder(Component.literal("Fertig"), btn -> this.onClose())
                .bounds(cx - 100, y, 200, 20).build());
        refresh();
    }

    private String hex() { return String.format("#%02X%02X%02X", r, g, b); }

    private void colorChanged() {
        if (syncing) return;
        syncing = true;
        hexBox.setValue(hex());
        syncing = false;
        NameStyleConfig.get().color = hex();
        refresh();
    }

    private void refresh() {
        String real = this.minecraft != null && this.minecraft.getUser() != null ? this.minecraft.getUser().getName() : "Player";
        if (preview != null) preview.setMessage(NameStyleConfig.get().build(real));
    }

    private Button toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
        return Button.builder(toggleText(label, get.getAsBoolean()), btn -> {
            boolean v = !get.getAsBoolean();
            set.accept(v);
            btn.setMessage(toggleText(label, v));
            refresh();
        }).bounds(x, y, w, 20).build();
    }

    private static Component toggleText(String label, boolean on) {
        return Component.literal(label + ": " + (on ? "AN" : "AUS"));
    }

    @Override
    public void onClose() {
        NameStyleConfig.save();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    private static class ChannelSlider extends AbstractSliderButton {
        private final String label;
        private final IntConsumer onChange;

        ChannelSlider(int x, int y, int w, int h, String label, int v, IntConsumer onChange) {
            super(x, y, w, h, Component.empty(), v / 255.0);
            this.label = label;
            this.onChange = onChange;
            updateMessage();
        }

        void setInt(int v) { this.value = v / 255.0; updateMessage(); }

        @Override protected void updateMessage() {
            if (label != null) setMessage(Component.literal(label + ": " + (int) Math.round(this.value * 255)));
        }

        @Override protected void applyValue() {
            if (onChange != null) onChange.accept((int) Math.round(this.value * 255));
        }
    }
}
