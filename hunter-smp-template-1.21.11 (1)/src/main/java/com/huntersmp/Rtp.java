package com.huntersmp;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;
import static net.minecraft.commands.Commands.literal;

public class Rtp {
    static final Random RNG = new Random();
    static final Deque<UUID> queue = new ArrayDeque<>();

    static ItemStack icon(ItemStack s, String name, String lore) {
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return Shop.withLore(s, lore);
    }

    public static void openMenu(ServerPlayer p) {
        SimpleContainer c = new SimpleContainer(54);
        c.setItem(20, icon(new ItemStack(Items.GRASS_BLOCK), "§aOverworld", "§7Click to teleport"));
        c.setItem(22, icon(new ItemStack(Items.NETHERRACK), "§cNether", "§7Click to teleport"));
        c.setItem(24, icon(new ItemStack(Items.END_STONE), "§eThe End", "§7Click to teleport"));
        Gui.open(p, Component.literal("Random Teleport"), c, (pl, slot, type) -> {
            ResourceKey<Level> dim = slot == 20 ? Level.OVERWORLD : slot == 22 ? Level.NETHER : slot == 24 ? Level.END : null;
            if (dim == null) return;
            pl.closeContainer();
            Tp.warmup(pl, () -> rtp(pl, dim));
        });
    }

    static void rtp(ServerPlayer p, ResourceKey<Level> dim) {
        ServerLevel lvl = p.level().getServer().getLevel(dim);
        BlockPos pos = lvl == null ? null : randomSpot(lvl, dim);
        if (pos == null) { Tp.msg(p, "§cCouldn't find a safe spot, try again."); return; }
        go(p, lvl, pos);
    }

    static void go(ServerPlayer p, ServerLevel lvl, BlockPos pos) {
        p.teleportTo(lvl, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Set.of(), p.getYRot(), p.getXRot(), true);
        Tp.msg(p, "§aTeleported to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }

    // Distance ranges from 0,0 - change these numbers to taste
    static BlockPos randomSpot(ServerLevel lvl, ResourceKey<Level> dim) {
        boolean nether = dim.equals(Level.NETHER), end = dim.equals(Level.END);
        int min = nether ? 100 : end ? 0 : 500;
        int max = nether ? 1500 : end ? 80 : 5000;
        for (int i = 0; i < 40; i++) {
            int x = (min + RNG.nextInt(max - min + 1)) * (RNG.nextBoolean() ? 1 : -1);
            int z = (min + RNG.nextInt(max - min + 1)) * (RNG.nextBoolean() ? 1 : -1);
            BlockPos pos = safe(lvl, x, z, nether);
            if (pos != null) return pos;
        }
        return null;
    }

    static BlockPos safe(ServerLevel lvl, int x, int z, boolean nether) {
        lvl.getChunk(x >> 4, z >> 4);
        if (nether) {
            for (int y = 100; y > 32; y--) {
                BlockPos p = new BlockPos(x, y, z);
                if (lvl.getBlockState(p).isAir() && lvl.getBlockState(p.above()).isAir()
                        && ok(lvl.getBlockState(p.below()))) return p;
            }
            return null;
        }
        int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos p = new BlockPos(x, y, z);
        return ok(lvl.getBlockState(p.below())) ? p : null;
    }

    static boolean ok(BlockState s) {
        return s.blocksMotion() && s.getFluidState().isEmpty() && !s.is(Blocks.MAGMA_BLOCK)
                && !s.is(Blocks.CACTUS) && !s.is(Blocks.CAMPFIRE) && !s.is(Blocks.FIRE);
    }

    static int toggle(ServerPlayer p) {
        if (queue.remove(p.getUUID())) { Tp.msg(p, "§eYou left the RTP queue."); return 1; }
        ServerPlayer other = null;
        while (other == null && !queue.isEmpty())
            other = p.level().getServer().getPlayerList().getPlayer(queue.poll());
        if (other == null) {
            queue.add(p.getUUID());
            Tp.msg(p, "§aYou joined the RTP queue. Waiting for another player... (run /rtpqueue again to leave)");
            return 1;
        }
        ServerLevel lvl = p.level().getServer().getLevel(Level.OVERWORLD);
        BlockPos pos = randomSpot(lvl, Level.OVERWORLD);
        if (pos == null) {
            queue.addFirst(other.getUUID());
            Tp.msg(p, "§cCouldn't find a safe spot, try again.");
            return 0;
        }
        go(p, lvl, pos);
        go(other, lvl, pos);
        Tp.msg(p, "§aMatched with " + Tp.name(other) + "!");
        Tp.msg(other, "§aMatched with " + Tp.name(p) + "!");
        return 1;
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("rtp").executes(c -> { openMenu(c.getSource().getPlayerOrException()); return 1; }));
        d.register(literal("rtpqueue").executes(c -> toggle(c.getSource().getPlayerOrException())));
    }
}
