package com.someonewashere.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class SomeoneWasHereClient implements ClientModInitializer {
    private long ticks;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) { ticks = 0; return; }
            ticks++;
        });
    }
}
