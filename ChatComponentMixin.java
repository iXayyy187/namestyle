package dev.namestyle.mixin;

import dev.namestyle.NameStyleConfig;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Eigener Name im Chat (ersetzt den echten Namen in eingehenden Nachrichten) */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @ModifyVariable(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD"), argsOnly = true, require = 0)
    private Component namestyle$chat(Component message) {
        return NameStyleConfig.restyleChat(message);
    }
}
