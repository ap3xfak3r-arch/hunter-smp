package com.huntersmp;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import java.util.*;

public class Shop {
    static ItemStack button(String name) {
        ItemStack s = new ItemStack(Items.ARROW);
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return s;
    }

    static ItemStack withLore(ItemStack s, String... lines) {
        List<Component> l = new ArrayList<>();
        for (String x : lines) l.add(Component.literal(x));
        s.set(DataComponents.LORE, new ItemLore(l));
        return s;
    }

    // ---------------- /shop ----------------
    public static void openShop(ServerPlayer p, int page) {
        List<String> ids = new ArrayList<>(Prices.shop.keySet());
        int per = 45, pages = Math.max(1, (ids.size() + per - 1) / per);
        page = Math.max(0, Math.min(page, pages - 1));
        SimpleContainer c = new SimpleContainer(54);
        List<String> shown = new ArrayList<>();
        for (int i = page * per; i < Math.min(ids.size(), (page + 1) * per); i++) {
            String id = ids.get(i);
            Item item = Prices.item(id);
            if (item == null) { shown.add(null); continue; }
            Auction.Listing l = Auction.lowest(id, p.getUUID());
            ItemStack icon = item.getDefaultInstance();
            if (l != null) {
                icon = l.stack.copy();   // show the actual auction item
                withLore(icon, "§6Auction listing", "§7Price: §a" + Money.fmt(l.price),
                        "§7Seller: §f" + l.sellerName, "§eClick to buy");
            } else {
                withLore(icon, "§7Price: §a" + Money.fmt(Prices.shop.get(id)) + " §7each",
                        "§eClick: buy 1", "§eShift-click: buy 64");
            }
            c.setItem(shown.size(), icon);
            shown.add(id);
        }
        c.setItem(45, button("§ePrevious page"));
        c.setItem(53, button("§eNext page"));
        final int pg = page;
        Gui.open(p, Component.literal("Shop - page " + (pg + 1)), c, (pl, slot, type) -> {
            if (slot == 45) { openShop(pl, pg - 1); return; }
            if (slot == 53) { openShop(pl, pg + 1); return; }
            if (slot >= shown.size() || shown.get(slot) == null) return;
            String id = shown.get(slot);
            Auction.Listing l = Auction.lowest(id, pl.getUUID());
            if (l != null) { Auction.buy(pl, l); }     // item is on /ah -> buy cheapest listing
            else {                                      // otherwise normal price
                int qty = type == ClickType.QUICK_MOVE ? 64 : 1;
                long cost = Prices.shop.get(id) * qty;
                if (!Money.take(pl.getUUID(), cost)) { pl.sendSystemMessage(Component.literal("§cYou need " + Money.fmt(cost) + ".")); return; }
                pl.getInventory().placeItemBackInInventory(new ItemStack(Prices.item(id), qty));
                pl.sendSystemMessage(Component.literal("§aBought " + qty + "x for " + Money.fmt(cost)));
            }
            openShop(pl, pg);
        });
    }

    // ---------------- /ah ----------------
    public static void openAuction(ServerPlayer p, int page) {
        List<Auction.Listing> all = Auction.sorted();
        int per = 45, pages = Math.max(1, (all.size() + per - 1) / per);
        page = Math.max(0, Math.min(page, pages - 1));
        SimpleContainer c = new SimpleContainer(54);
        List<Auction.Listing> shown = new ArrayList<>();
        for (int i = page * per; i < Math.min(all.size(), (page + 1) * per); i++) {
            Auction.Listing l = all.get(i);
            boolean own = l.seller.equals(p.getUUID().toString());
            c.setItem(shown.size(), withLore(l.stack.copy(), "§7Price: §a" + Money.fmt(l.price),
                    "§7Seller: §f" + l.sellerName, own ? "§cClick to cancel" : "§eClick to buy"));
            shown.add(l);
        }
        c.setItem(45, button("§ePrevious page"));
        c.setItem(53, button("§eNext page"));
        final int pg = page;
        Gui.open(p, Component.literal("Auction House - page " + (pg + 1)), c, (pl, slot, type) -> {
            if (slot == 45) { openAuction(pl, pg - 1); return; }
            if (slot == 53) { openAuction(pl, pg + 1); return; }
            if (slot < shown.size()) { Auction.buy(pl, shown.get(slot)); openAuction(pl, pg); }
        });
    }
}
