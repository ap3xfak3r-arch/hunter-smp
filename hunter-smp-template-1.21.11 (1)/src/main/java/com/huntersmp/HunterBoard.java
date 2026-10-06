package com.huntersmp;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import java.util.*;

/** Per-player sidebar sent with packets, so every player sees their own numbers. */
public class HunterBoard {
    static final String OBJ = "hunter_smp";
    static final Scoreboard DUMMY = new Scoreboard();
    static final Objective OBJECTIVE = new Objective(DUMMY, OBJ, ObjectiveCriteria.DUMMY,
            Component.literal("§6§lHUNTER SMP"), ObjectiveCriteria.RenderType.INTEGER, false, null);
    static final Set<UUID> initialised = new HashSet<>();

    public static void forget(ServerPlayer p) { initialised.remove(p.getUUID()); }

    static String playtime(ServerPlayer p) {
        int ticks = p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        long mins = ticks / 20 / 60;
        return (mins / 60) + "h " + (mins % 60) + "m";
    }

    public static void update(ServerPlayer p) {
        if (initialised.add(p.getUUID())) {
            p.connection.send(new ClientboundSetObjectivePacket(OBJECTIVE, 0));
            p.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, OBJECTIVE));
        }
        int kills = p.getStats().getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));
        int deaths = p.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
        String[] lines = {
                " ",
                "§a§l$ §fMoney §a" + Money.fmt(Money.get(p.getUUID())),
                "§c§l⚔ §fKills §c" + kills,
                "§4§l☠ §fDeaths §c" + deaths,
                "§e§l⌚ §fPlaytime §e" + playtime(p),
                "§b§l⚡ §fPing §b" + p.connection.latency() + "ms",
                "  ",
                "§7huntersmp.net"   // change to your IP
        };
        for (int i = 0; i < lines.length; i++) {
            p.connection.send(new ClientboundSetScorePacket("line" + i, OBJ, lines.length - i,
                    Optional.of(Component.literal(lines[i])), Optional.of(BlankFormat.INSTANCE)));
        }
    }
}
