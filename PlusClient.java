package dev.namestyleplus;

import net.fabricmc.api.ClientModInitializer;

public class PlusClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PlusConfig.load();
    }
}
