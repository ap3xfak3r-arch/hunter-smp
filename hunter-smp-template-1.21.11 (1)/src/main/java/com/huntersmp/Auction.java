package com.huntersmp;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.nio.file.*;
import java.util.*;

public class Auction {
    public static class Listing {
        long id; String seller; String sellerName; long price; JsonElement item;
        transient ItemStack stack;
        public long perUnit() { return Math.max(1, price / Math.max(1, stack.getCount())); }
    }

    static final Path FILE = Money.DIR.resolve("auction.json");
    static final List<Listing> listings = new ArrayList<>();
    static long nextId = 1;

    public static void load(MinecraftServer s) {
        try {
            if (!Files.exists(FILE)) return;
            List<Listing> l = Money.GSON.fromJson(Files.readString(FILE), new TypeToken<List<Listing>>() {}.getType());
            if (l == null) return;
            for (Listing x : l) {
                x.stack = ItemStack.CODEC.parse(s.registryAccess().createSerializationContext(JsonOps.INSTANCE), x.item)
                        .result().orElse(ItemStack.EMPTY);
                if (!x.stack.isEmpty()) { listings.add(x); nextId = Math.max(nextId, x.id + 1); }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void save(MinecraftServer s) {
        try {
            for (Listing x : listings)
                x.item = ItemStack.CODEC.encodeStart(s.registryAccess().createSerializationContext(JsonOps.INSTANCE), x.stack).getOrThrow();
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, Money.GSON.toJson(listings));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void add(ServerPlayer p, ItemStack stack, long price) {
        Listing l = new Listing();
        l.id = nextId++; l.seller = p.getUUID().toString(); l.sellerName = p.getGameProfile().name();
        l.price = price; l.stack = stack;
        listings.add(l);
        save(p.level().getServer());
    }

    /** Lowest per-unit listing of this item, ignoring the viewer's own listings. */
    public static Listing lowest(String itemId, UUID viewer) {
        return listings.stream()
                .filter(l -> Prices.idOf(l.stack.getItem()).equals(itemId) && !l.seller.equals(viewer.toString()))
                .min(Comparator.comparingLong(Listing::perUnit)).orElse(null);
    }

    public static List<Listing> sorted() {
        List<Listing> l = new ArrayList<>(listings);
        l.sort(Comparator.comparingLong(Listing::perUnit));
        return l;
    }

    public static void buy(ServerPlayer p, Listing l) {
        if (!listings.contains(l)) { p.sendSystemMessage(Component.literal("§cThat listing is gone.")); return; }
        if (l.seller.equals(p.getUUID().toString())) {   // own listing = cancel
            listings.remove(l);
            p.getInventory().placeItemBackInInventory(l.stack.copy());
            p.sendSystemMessage(Component.literal("§eListing cancelled."));
            save(p.level().getServer());
            return;
        }
        if (!Money.take(p.getUUID(), l.price)) { p.sendSystemMessage(Component.literal("§cYou need " + Money.fmt(l.price) + ".")); return; }
        listings.remove(l);
        UUID seller = UUID.fromString(l.seller);
        Money.add(seller, l.price);   // money for the seller
        p.getInventory().placeItemBackInInventory(l.stack.copy());
        p.sendSystemMessage(Component.literal("§aBought " + l.stack.getCount() + "x " + l.stack.getHoverName().getString() + " for " + Money.fmt(l.price)));
        ServerPlayer sp = p.level().getServer().getPlayerList().getPlayer(seller);
        if (sp != null) sp.sendSystemMessage(Component.literal("§a" + p.getGameProfile().name() + " bought your " + l.stack.getHoverName().getString() + " for " + Money.fmt(l.price)));
        save(p.level().getServer());
    }
}
