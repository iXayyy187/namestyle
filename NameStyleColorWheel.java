package dev.namestyle;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Echtes Farbrad: Winkel = Farbton, Abstand vom Zentrum = Saettigung. */
public class NameStyleColorWheel extends AbstractWidget {
    public interface Listener { void onPick(double hue, double sat); }

    private static final int CELL = 3;
    private final Listener listener;
    private double hue = 0, sat = 1, val = 1;

    public NameStyleColorWheel(int x, int y, int size, Listener listener) {
        super(x, y, size, size, Component.empty());
        this.listener = listener;
    }

    public void setHsv(double h, double s, double v) { this.hue = h; this.sat = s; this.val = v; }

    public void setValue(double v) { this.val = v; }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int r = this.width / 2;
        int x0 = getX(), y0 = getY();
        g.fillGradient(x0 - 1, y0 - 1, x0 + this.width + 1, y0 + this.height + 1, 0xFF1A1A1A, 0xFF1A1A1A);
        for (int py = 0; py < this.height; py += CELL) {
            for (int px = 0; px < this.width; px += CELL) {
                double dx = px + CELL / 2.0 - r, dy = py + CELL / 2.0 - r;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > r) continue;
                double h = (Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1;
                int rgb = NameStyleConfig.hsvToRgb(h, Math.min(1, dist / r), val) | 0xFF000000;
                g.fillGradient(x0 + px, y0 + py, x0 + px + CELL, y0 + py + CELL, rgb, rgb);
            }
        }
        double ang = hue * 2 * Math.PI;
        int mx = (int) (x0 + r + Math.cos(ang) * sat * r);
        int my = (int) (y0 + r + Math.sin(ang) * sat * r);
        g.fillGradient(mx - 3, my - 3, mx + 4, my + 4, 0xFFFFFFFF, 0xFFFFFFFF);
        g.fillGradient(mx - 2, my - 2, mx + 3, my + 3, 0xFF000000, 0xFF000000);
        int cur = NameStyleConfig.hsvToRgb(hue, sat, val) | 0xFF000000;
        g.fillGradient(mx - 1, my - 1, mx + 2, my + 2, cur, cur);
    }

    private void pick(double mouseX, double mouseY) {
        int r = this.width / 2;
        double dx = mouseX - (getX() + r), dy = mouseY - (getY() + r);
        double dist = Math.sqrt(dx * dx + dy * dy);
        hue = (Math.atan2(dy, dx) / (2 * Math.PI) + 1) % 1;
        sat = Math.min(1, dist / r);
        listener.onPick(hue, sat);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.isMouseOver(event.x(), event.y())) {
            pick(event.x(), event.y());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        pick(event.x(), event.y());
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) { }
}
