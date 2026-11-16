package dev.namestyleplus.mixin;

import dev.namestyle.NameStyleScreen;
import dev.namestyle.NameStyleTagsScreen;
import dev.namestyleplus.PlusScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Ersetzt die originalen NameStyle-Screens durch die neue schwarz-weisse Oberflaeche. */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow
    public abstract Screen screen();

    @ModifyVariable(
            method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private Screen namestyleplus$swap(Screen requested) {
        if (requested instanceof NameStyleScreen || requested instanceof NameStyleTagsScreen) {
            Screen current = this.screen();
            if (current instanceof PlusScreen plus) {
                current = plus.parentScreen();
            }
            return new PlusScreen(current);
        }
        return requested;
    }
}
