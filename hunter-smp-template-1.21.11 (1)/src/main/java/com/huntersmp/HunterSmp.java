package com.huntersmp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public class HunterSmp implements ModInitializer {
    @Override
    public void onInitialize() {
        Prices.load();
        Money.load();
        ServerLifecycleEvents.SERVER_STARTED.register(Auction::load);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> { Money.save(); Auction.save(s); });
        CommandRegistrationCallback.EVENT.register((d, reg, env) -> HunterCommands.register(d));
        ServerPlayConnectionEvents.DISCONNECT.register((h, s) -> HunterBoard.forget(h.getPlayer()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 == 0)
                for (ServerPlayer p : server.getPlayerList().getPlayers()) HunterBoard.update(p);
        });
    }
}
