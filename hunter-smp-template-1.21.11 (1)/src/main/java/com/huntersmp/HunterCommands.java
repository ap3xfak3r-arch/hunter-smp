package com.huntersmp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class HunterCommands {
    static void msg(ServerPlayer p, String s) { p.sendSystemMessage(Component.literal(s)); }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("bal").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            msg(p, "§aBalance: §f" + Money.fmt(Money.get(p.getUUID())));
            return 1;
        }));

        d.register(literal("pay").then(argument("player", EntityArgument.player())
                .then(argument("amount", LongArgumentType.longArg(1)).executes(c -> {
                    ServerPlayer from = c.getSource().getPlayerOrException();
                    ServerPlayer to = EntityArgument.getPlayer(c, "player");
                    long amt = LongArgumentType.getLong(c, "amount");
                    if (!Money.take(from.getUUID(), amt)) { msg(from, "§cNot enough money."); return 0; }
                    Money.add(to.getUUID(), amt);
                    msg(from, "§aPaid " + to.getGameProfile().name() + " " + Money.fmt(amt));
                    msg(to, "§a" + from.getGameProfile().name() + " paid you " + Money.fmt(amt));
                    return 1;
                }))));

        // Admin command - the only "free money" source besides selling
        d.register(literal("eco").requires(s -> s.hasPermission(2))
                .then(literal("give").then(argument("player", EntityArgument.player()).then(argument("amount", LongArgumentType.longArg(0)).executes(c -> {
                    ServerPlayer t = EntityArgument.getPlayer(c, "player");
                    Money.add(t.getUUID(), LongArgumentType.getLong(c, "amount")); return 1;
                }))))
                .then(literal("take").then(argument("player", EntityArgument.player()).then(argument("amount", LongArgumentType.longArg(0)).executes(c -> {
                    ServerPlayer t = EntityArgument.getPlayer(c, "player");
                    Money.set(t.getUUID(), Money.get(t.getUUID()) - LongArgumentType.getLong(c, "amount")); return 1;
                }))))
                .then(literal("set").then(argument("player", EntityArgument.player()).then(argument("amount", LongArgumentType.longArg(0)).executes(c -> {
                    ServerPlayer t = EntityArgument.getPlayer(c, "player");
                    Money.set(t.getUUID(), LongArgumentType.getLong(c, "amount")); return 1;
                })))));

        d.register(literal("sell")
                .executes(c -> {   // sell held stack
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ItemStack s = p.getMainHandItem();
                    Long price = Prices.sell.get(Prices.idOf(s.getItem()));
                    if (s.isEmpty() || price == null) { msg(p, "§cYou can't sell that."); return 0; }
                    long total = price * s.getCount();
                    s.setCount(0);
                    Money.add(p.getUUID(), total);
                    msg(p, "§aSold for " + Money.fmt(total));
                    return 1;
                })
                .then(literal("all").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    long total = 0;
                    for (int i = 0; i < 36; i++) {
                        ItemStack s = p.getInventory().getItem(i);
                        Long price = s.isEmpty() ? null : Prices.sell.get(Prices.idOf(s.getItem()));
                        if (price != null) { total += price * s.getCount(); p.getInventory().setItem(i, ItemStack.EMPTY); }
                    }
                    if (total == 0) { msg(p, "§cNothing sellable in your inventory."); return 0; }
                    Money.add(p.getUUID(), total);
                    msg(p, "§aSold everything for " + Money.fmt(total));
                    return 1;
                })));

        d.register(literal("shop").executes(c -> { Shop.openShop(c.getSource().getPlayerOrException(), 0); return 1; }));

        LiteralCommandNode<CommandSourceStack> ah = d.register(literal("ah")
                .executes(c -> { Shop.openAuction(c.getSource().getPlayerOrException(), 0); return 1; })
                .then(literal("sell").then(argument("price", LongArgumentType.longArg(1)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ItemStack held = p.getMainHandItem();
                    if (held.isEmpty()) { msg(p, "§cHold the item you want to sell."); return 0; }
                    long price = LongArgumentType.getLong(c, "price");
                    Auction.add(p, held.copy(), price);
                    held.setCount(0);
                    msg(p, "§aListed for " + Money.fmt(price));
                    return 1;
                }))));
        d.register(literal("auction").redirect(ah));
    }
}
