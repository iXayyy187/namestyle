package dev.namestyle.mixin;

import dev.namestyle.NameStyleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Eigenen Nametag in der 3. Person anzeigen (Vanilla blendet ihn aus). */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true, require = 0)
    private void namestyle$showOwn(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        NameStyleConfig cfg = NameStyleConfig.get();
        if (mc != null && entity == mc.player && cfg.enabled && cfg.showOwnNametag
                && !mc.options.getCameraType().isFirstPerson() && !mc.gui.hud.isHidden()) {
            cir.setReturnValue(true);
        }
    }
}
