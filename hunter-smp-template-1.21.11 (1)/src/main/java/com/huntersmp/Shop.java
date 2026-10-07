package com.huntersmp;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import java.util.*;

public class Shop {
    static ItemStack named(ItemStack s, String name) {
        s.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return s;
    }

    static ItemStack button(String name) { return named(new ItemStack(Items.ARROW), name); }

    static ItemStack withLore(ItemStack s, String... lines) {
        List<Component> l = new ArrayList<>();
        for (String x : lines) l.add(Component.literal(x));
        s.set(DataComponents.LORE, new ItemLore(l));
        return s;
    }

    static ItemStack pane(Item item, String name, String... lore) {
        return withLore(named(new ItemStack(item), name), lore);
    }

    static void fill(SimpleContainer c) {
        for (int i = 0; i < 54; i++) c.setItem(i, pane(Items.GRAY_STAINED_GLASS_PANE, " "));
    }

    static void bottomRow(SimpleContainer c, ServerPlayer p) {
        for (int i = 45; i < 54; i++) c.setItem(i, pane(Items.GRAY_STAINED_GLASS_PANE, " "));
        c.setItem(49, pane(Items.GOLD_INGOT, "§6Balance", "§a" + Money.fmt(Money.get(p.getUUID()))));
        c.setItem(53, pane(Items.EMERALD, "§aAuction House", "§7Click to open /ah"));
    }

    // ---------------- /shop main menu ----------------
    public static void openShop(ServerPlayer p, int ignored) { openShop(p); }

    public static void openShop(ServerPlayer p) {
        SimpleContainer c = new SimpleContainer(54);
        fill(c);
        List<String> cats = new ArrayList<>(Prices.categories.keySet());
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 28, 29, 30, 31, 32, 33, 34};
        int n = Math.min(cats.size(), slots.length);
        for (int i = 0; i < n; i++) {
            String cat = cats.get(i);
            Item icon = Prices.item(Prices.icons.getOrDefault(cat, "minecraft:chest"));
            if (icon == null) icon = Items.CHEST;
            c.setItem(slots[i], pane(icon, "§e§l" + cat, "§7" + Prices.categories.get(cat).size() + " items", "§eClick to browse"));
        }
        bottomRow(c, p);
        Gui.open(p, Component.literal("Shop"), c, (pl, slot, type) -> {
            if (slot == 53) { openAuction(pl, 0); return; }
            for (int i = 0; i < n; i++) if (slots[i] == slot) { openCategory(pl, cats.get(i), 0); return; }
        });
    }

    // ---------------- category page ----------------
    static void openCategory(ServerPlayer p, String cat, int page) {
        List<String> ids = new ArrayList<>();
        for (String id : Prices.categories.getOrDefault(cat, List.of()))
            if (Prices.item(id) != null && Prices.shop.containsKey(id)) ids.add(id);
        int per = 45, pages = Math.max(1, (ids.size() + per - 1) / per);
        page = Math.max(0, Math.min(page, pages - 1));
        SimpleContainer c = new SimpleContainer(54);
        List<String> shown = new ArrayList<>();
        for (int i = page * per; i < Math.min(ids.size(), (page + 1) * per); i++) {
            String id = ids.get(i);
            Auction.Listing l = Auction.lowest(id, p.getUUID());
            ItemStack icon = Prices.item(id).getDefaultInstance();
            if (l != null) withLore(icon, "§7Shop price: §a" + Money.fmt(Prices.shop.get(id)) + " §7each",
                    "§6Cheapest auction: §a" + Money.fmt(l.price), "§eClick to select");
            else withLore(icon, "§7Price: §a" + Money.fmt(Prices.shop.get(id)) + " §7each", "§eClick to select");
            c.setItem(shown.size(), icon);
            shown.add(id);
        }
        bottomRow(c, p);
        c.setItem(45, pane(Items.ARROW, "§eBack"));
        c.setItem(48, pane(Items.ARROW, "§ePrevious page"));
        c.setItem(50, pane(Items.ARROW, "§eNext page"));
        final int pg = page;
        Gui.open(p, Component.literal(cat + " - page " + (pg + 1)), c, (pl, slot, type) -> {
            if (slot == 45) { openShop(pl); return; }
            if (slot == 48) { openCategory(pl, cat, pg - 1); return; }
            if (slot == 50) { openCategory(pl, cat, pg + 1); return; }
            if (slot == 53) { openAuction(pl, 0); return; }
            if (slot < shown.size()) openItem(pl, cat, shown.get(slot), 1, pg);
        });
    }

    // ---------------- purchase screen ----------------
    static void openItem(ServerPlayer p, String cat, String id, int qty, int page) {
        Item item = Prices.item(id);
        Auction.Listing l = Auction.lowest(id, p.getUUID());
        long unit = Prices.shop.get(id);
        int max = item.getDefaultInstance().getMaxStackSize();
        int q = Math.max(1, Math.min(qty, max));
        long total = l != null ? l.price : unit * q;

        SimpleContainer c = new SimpleContainer(54);
        fill(c);
        ItemStack show;
        if (l != null) {
            show = withLore(l.stack.copy(), "§6Auction listing", "§7Seller: §f" + l.sellerName,
                    "§7Price: §a" + Money.fmt(l.price));
        } else {
            show = withLore(new ItemStack(item, q), "§7Price each: §a" + Money.fmt(unit),
                    "§7Quantity: §f" + q, "§7Total: §a" + Money.fmt(total));
        }
        c.setItem(22, show);

        int[] qs = {1, 8, 16, 32, 48, 64};
        int[] qSlots = {19, 20, 21, 23, 24, 25};
        if (l == null) {
            for (int i = 0; i < qs.length; i++) {
                if (qs[i] > max && qs[i] != 1) continue;
                ItemStack b = new ItemStack(q == qs[i] ? Items.LIME_STAINED_GLASS_PANE : Items.WHITE_STAINED_GLASS_PANE, Math.min(qs[i], 64));
                c.setItem(qSlots[i], withLore(named(b, "§aQuantity: " + qs[i]), "§7Click to set"));
            }
        }
        c.setItem(40, pane(Items.LIME_CONCRETE, "§a§lConfirm purchase", "§7Total: §a" + Money.fmt(total)));
        bottomRow(c, p);
        c.setItem(45, pane(Items.ARROW, "§eBack"));

        Gui.open(p, Component.literal("Buy " + item.getDefaultInstance().getHoverName().getString()), c, (pl, slot, type) -> {
            if (slot == 45) { openCategory(pl, cat, page); return; }
            if (slot == 53) { openAuction(pl, 0); return; }
            if (slot == 40) {
                Auction.Listing cur = Auction.lowest(id, pl.getUUID());
                if (cur != null) {                                   // item is on /ah -> buy the cheapest listing
                    Auction.buy(pl, cur);
                } else {                                             // otherwise normal shop price
                    long cost = unit * q;
                    if (!Money.take(pl.getUUID(), cost)) {
                        pl.sendSystemMessage(Component.literal("§cYou need " + Money.fmt(cost) + "."));
                    } else {
                        int left = q;
                        while (left > 0) {
                            int n = Math.min(left, max);
                            pl.getInventory().placeItemBackInInventory(new ItemStack(item, n));
                            left -= n;
                        }
                        pl.sendSystemMessage(Component.literal("§aBought " + q + "x for " + Money.fmt(cost)));
                    }
                }
                openItem(pl, cat, id, q, page);
                return;
            }
            if (l == null)
                for (int i = 0; i < qs.length; i++) if (qSlots[i] == slot) { openItem(pl, cat, id, qs[i], page); return; }
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
        bottomRow(c, p);
        c.setItem(45, pane(Items.ARROW, "§eShop"));
        c.setItem(48, pane(Items.ARROW, "§ePrevious page"));
        c.setItem(50, pane(Items.ARROW, "§eNext page"));
        c.setItem(53, pane(Items.GRAY_STAINED_GLASS_PANE, " "));
        final int pg = page;
        Gui.open(p, Component.literal("Auction House - page " + (pg + 1)), c, (pl, slot, type) -> {
            if (slot == 45) { openShop(pl); return; }
            if (slot == 48) { openAuction(pl, pg - 1); return; }
            if (slot == 50) { openAuction(pl, pg + 1); return; }
            if (slot < shown.size()) { Auction.buy(pl, shown.get(slot)); openAuction(pl, pg); }
        });
    }
}
