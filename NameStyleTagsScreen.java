package dev.namestyle;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Eigene Tags vor dem Namen, z.B. [VIP] oder ★ */
public class NameStyleTagsScreen extends Screen {
    private final Screen parent;
    private StringWidget preview;

    public NameStyleTagsScreen(Screen parent) {
        super(Component.literal("NameStyle Tags"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        NameStyleConfig c = NameStyleConfig.get();
        c.ensureTags();
        int cx = this.width / 2;
        int y = Math.max(6, this.height / 2 - 100);

        preview = new StringWidget(cx - 150, y, 300, 12, Component.literal(""), this.font);
        this.addRenderableWidget(preview);
        y += 22;

        this.addRenderableWidget(new StringWidget(cx - 150, y, 300, 12,
                Component.literal("Tags vor dem Namen (Farbe: #RRGGBB oder rainbow)"), this.font));
        y += 16;

        for (int i = 0; i < NameStyleConfig.TAG_COUNT; i++) {
            final int idx = i;
            EditBox text = new EditBox(this.font, cx - 150, y, 170, 20, Component.literal("Tag"));
            text.setMaxLength(20);
            text.setHint(Component.literal("Tag " + (i + 1) + ", z.B. [VIP]"));
            text.setValue(c.tagText[i]);
            text.setResponder(str -> { NameStyleConfig.get().tagText[idx] = str; refresh(); });
            this.addRenderableWidget(text);

            EditBox col = new EditBox(this.font, cx + 26, y, 124, 20, Component.literal("Farbe"));
            col.setMaxLength(7);
            col.setHint(Component.literal("#FFAA00"));
            col.setValue(c.tagColor[i]);
            col.setResponder(str -> { NameStyleConfig.get().tagColor[idx] = str; refresh(); });
            this.addRenderableWidget(col);
            y += 24;
        }
        y += 6;
        this.addRenderableWidget(Button.builder(Component.literal("Fertig"), btn -> this.onClose())
                .bounds(cx - 100, y, 200, 20).build());
        refresh();
    }

    private void refresh() {
        String real = (this.minecraft != null && this.minecraft.getUser() != null) ? this.minecraft.getUser().getName() : "Player";
        if (preview != null) preview.setMessage(NameStyleConfig.get().build(real));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        refresh();
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        NameStyleConfig.save();
        if (this.minecraft != null) this.minecraft.gui.setScreen(parent);
    }
}
