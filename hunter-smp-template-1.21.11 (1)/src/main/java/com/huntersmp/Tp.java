package com.huntersmp;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class Tp {
    static final int WARMUP_SECONDS = 3;
    static final long EXPIRE_MS = 60_000;

    static class Req { UUID from, to; boolean here; long expires; }
    static class Pending { ServerPlayer p; Vec3 start; int ticks; Runnable action; }
    static final List<Req> reqs = new ArrayList<>();
    static final Set<UUID> auto = new HashSet<>();
    static final List<Pending> pending = new ArrayList<>();

    static void msg(ServerPlayer p, String s) { p.sendSystemMessage(Component.literal(s)); }
    static ServerPlayer online(ServerPlayer ctx, UUID id) { return ctx.level().getServer().getPlayerList().getPlayer(id); }
    static String name(ServerPlayer p) { return p.getGameProfile().name(); }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Iterator<Pending> it = pending.iterator(); it.hasNext();) {
                Pending w = it.next();
                if (w.p.isRemoved()) { it.remove(); continue; }
                if (w.p.position().distanceToSqr(w.start) > 0.5) {
                    msg(w.p, "§cTeleport cancelled because you moved.");
                    it.remove();
                    continue;
                }
                if (--w.ticks <= 0) { it.remove(); w.action.run(); }
            }
        });
    }

    public static void warmup(ServerPlayer p, Runnable action) {
        pending.removeIf(w -> w.p == p);
        Pending w = new Pending();
        w.p = p; w.start = p.position(); w.ticks = WARMUP_SECONDS * 20; w.action = action;
        pending.add(w);
        msg(p, "§eTeleporting in " + WARMUP_SECONDS + " seconds. Don't move!");
    }

    static void tpTo(ServerPlayer mover, ServerPlayer dest) {
        mover.teleportTo((ServerLevel) dest.level(), dest.getX(), dest.getY(), dest.getZ(),
                Set.of(), dest.getYRot(), dest.getXRot(), true);
        msg(mover, "§aTeleported!");
    }

    static Req find(ServerPlayer me, ServerPlayer from) {
        long now = System.currentTimeMillis();
        reqs.removeIf(r -> r.expires < now);
        Req best = null;
        for (Req r : reqs)
            if (r.to.equals(me.getUUID()) && (from == null || r.from.equals(from.getUUID()))) best = r;
        return best;
    }

    static int send(ServerPlayer from, ServerPlayer to, boolean here) {
        if (from == to) { msg(from, "§cYou can't send a request to yourself."); return 0; }
        reqs.removeIf(r -> r.from.equals(from.getUUID()) && r.to.equals(to.getUUID()));
        Req r = new Req();
        r.from = from.getUUID(); r.to = to.getUUID(); r.here = here;
        r.expires = System.currentTimeMillis() + EXPIRE_MS;
        reqs.add(r);
        msg(from, "§aRequest sent to " + name(to) + ".");
        if (auto.contains(to.getUUID())) return accept(to, from);
        msg(to, "§e" + name(from) + (here ? " wants you to teleport to them." : " wants to teleport to you.")
                + " §7Use /tpaccept or /tpdeny");
        return 1;
    }

    static int accept(ServerPlayer me, ServerPlayer from) {
        Req r = find(me, from);
        if (r == null) { msg(me, "§cNo pending request."); return 0; }
        reqs.remove(r);
        ServerPlayer sender = online(me, r.from);
        if (sender == null) { msg(me, "§cThat player is offline."); return 0; }
        ServerPlayer mover = r.here ? me : sender;
        ServerPlayer dest = r.here ? sender : me;
        msg(me, "§aRequest accepted.");
        msg(sender, "§a" + name(me) + " accepted your request.");
        warmup(mover, () -> tpTo(mover, dest));
        return 1;
    }

    static int deny(ServerPlayer me, ServerPlayer from) {
        Req r = find(me, from);
        if (r == null) { msg(me, "§cNo pending request."); return 0; }
        reqs.remove(r);
        msg(me, "§eRequest denied.");
        ServerPlayer sender = online(me, r.from);
        if (sender != null) msg(sender, "§c" + name(me) + " denied your request.");
        return 1;
    }

    static int cancel(ServerPlayer me, ServerPlayer to) {
        boolean removed = reqs.removeIf(r -> r.from.equals(me.getUUID()) && (to == null || r.to.equals(to.getUUID())));
        msg(me, removed ? "§eRequest cancelled." : "§cYou have no request to cancel.");
        return removed ? 1 : 0;
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("tpa").then(argument("player", EntityArgument.player()).executes(c ->
                send(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player"), false))));
        d.register(literal("tpahere").then(argument("player", EntityArgument.player()).executes(c ->
                send(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player"), true))));
        d.register(literal("tpaccept")
                .executes(c -> accept(c.getSource().getPlayerOrException(), null))
                .then(argument("player", EntityArgument.player()).executes(c ->
                        accept(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player")))));
        d.register(literal("tpdeny")
                .executes(c -> deny(c.getSource().getPlayerOrException(), null))
                .then(argument("player", EntityArgument.player()).executes(c ->
                        deny(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player")))));
        d.register(literal("tpacancel")
                .executes(c -> cancel(c.getSource().getPlayerOrException(), null))
                .then(argument("player", EntityArgument.player()).executes(c ->
                        cancel(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "player")))));
        d.register(literal("tpauto").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (auto.remove(p.getUUID())) msg(p, "§eAuto-accept is now OFF.");
            else { auto.add(p.getUUID()); msg(p, "§aAuto-accept is now ON."); }
            return 1;
        }));
    }
}
