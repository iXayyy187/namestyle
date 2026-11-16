package dev.namestyle.mixin;

import dev.namestyle.NameStyleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Falls Player getDisplayName selbst ueberschreibt (sonst wird dieser Mixin einfach ignoriert). */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 0)
    private void namestyle$displayName(CallbackInfoReturnable<Component> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null && (Object) this == mc.player) {
            Component c = NameStyleConfig.forPlayer();
            if (c != null) cir.setReturnValue(c);
        }
    }
}
