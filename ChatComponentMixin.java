package dev.namestyle.mixin;

import dev.namestyle.NameStyleConfig;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Eigener Name im Chat. In 26.2 ist addMessage privat und hat 4 Parameter (mit GuiMessageSource). */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @ModifyVariable(
        method = {
            "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V"
        },
        at = @At("HEAD"), argsOnly = true, require = 0)
    private Component namestyle$chat(Component message) {
        return NameStyleConfig.restyleChat(message);
    }
}
