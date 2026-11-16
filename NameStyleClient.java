package dev.namestyle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class NameStyleClient implements ClientModInitializer {
    private static volatile boolean openRequested = false;

    @Override
    public void onInitializeClient() {
        NameStyleConfig.load();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> register(dispatcher));
        // Menue erst im naechsten Tick oeffnen, sonst schliesst der Chat es sofort wieder
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (openRequested) {
                openRequested = false;
                mc.gui.setScreen(new NameStyleScreen(mc.gui.screen()));
            }
        });
    }

    private static <S> void register(CommandDispatcher<S> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<S>literal("namestyle").executes(ctx -> {
            openRequested = true;
            return 1;
        }));
    }
}
