package com.huntersmp;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public class Combat {
    static final int COMBAT_SECONDS = 30;   // change this number to change the timer
    static final Map<UUID, Long> until = new HashMap<>();

    public static boolean inCombat(ServerPlayer p) {
        Long t = until.get(p.getUUID());
        return t != null && t > System.currentTimeMillis();
    }

    /** Returns true (and warns the player) if they are in combat. */
    public static boolean blocked(ServerPlayer p) {
        if (!inCombat(p)) return false;
        p.sendSystemMessage(Component.literal("§cYou can't do that while in combat!"));
        return true;
    }

    static void tag(ServerPlayer p) {
        boolean was = inCombat(p);
        until.put(p.getUUID(), System.currentTimeMillis() + COMBAT_SECONDS * 1000L);
        if (!was) p.sendSystemMessage(Component.literal("§cYou are now in combat! Don't log out for " + COMBAT_SECONDS + " seconds."));
    }

    public static void init() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer victim && source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
                tag(victim);
                tag(attacker);
            }
            return true;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer p) until.remove(p.getUUID());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer p = handler.getPlayer();
            if (inCombat(p)) {
                until.remove(p.getUUID());
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§c" + p.getGameProfile().name() + " combat logged and died!"), false);
                p.kill((ServerLevel) p.level());
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 10 != 0) return;
            long now = System.currentTimeMillis();
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                Long t = until.get(p.getUUID());
                if (t == null) continue;
                if (t > now) {
                    p.displayClientMessage(Component.literal("§cCombat: " + ((t - now + 999) / 1000) + "s"), true);
                } else {
                    until.remove(p.getUUID());
                    p.displayClientMessage(Component.literal("§aYou are no longer in combat."), true);
                }
            }
        });
    }
}
