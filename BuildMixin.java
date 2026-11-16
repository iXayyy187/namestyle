package dev.namestyleplus.mixin;

import dev.namestyle.NameStyleConfig;
import dev.namestyleplus.PlusStyler;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Haengt sich an NameStyleConfig.build() und stylt das Ergebnis nach (Tag-Stile + Saettigung). */
@Mixin(NameStyleConfig.class)
public abstract class BuildMixin {
    @Inject(method = "build", at = @At("RETURN"), cancellable = true)
    private void namestyleplus$post(String name, CallbackInfoReturnable<Component> cir) {
        Component result = cir.getReturnValue();
        if (result != null) {
            cir.setReturnValue(PlusStyler.apply(result));
        }
    }
}
