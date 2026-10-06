package com.huntersmp;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import java.nio.file.*;
import java.util.*;

/** Loads config/huntersmp/prices.json. "sell" = what /sell pays per item, "shop" = /shop price per item. */
public class Prices {
    static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("huntersmp/prices.json");
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Map<String, Long> sell = new LinkedHashMap<>();
    public static Map<String, Long> shop = new LinkedHashMap<>();
    static final Map<String, Item> ITEMS = new HashMap<>();

    static class Data { Map<String, Long> sell = new LinkedHashMap<>(); Map<String, Long> shop = new LinkedHashMap<>(); }

    public static void load() {
        for (Item i : BuiltInRegistries.ITEM) ITEMS.put(BuiltInRegistries.ITEM.getKey(i).toString(), i);
        try {
            Files.createDirectories(FILE.getParent());
            if (!Files.exists(FILE)) {
                Data d = new Data();
                // PLACEHOLDER NUMBERS - replace with the real DonutSMP prices you want
                d.sell.put("minecraft:coal", 5L);
                d.sell.put("minecraft:raw_iron", 10L);
                d.sell.put("minecraft:iron_ingot", 10L);
                d.sell.put("minecraft:raw_gold", 20L);
                d.sell.put("minecraft:gold_ingot", 20L);
                d.sell.put("minecraft:redstone", 5L);
                d.sell.put("minecraft:lapis_lazuli", 5L);
                d.sell.put("minecraft:diamond", 100L);
                d.sell.put("minecraft:emerald", 50L);
                d.sell.put("minecraft:netherite_scrap", 500L);
                d.shop.put("minecraft:coal", 10L);
                d.shop.put("minecraft:iron_ingot", 20L);
                d.shop.put("minecraft:gold_ingot", 40L);
                d.shop.put("minecraft:diamond", 200L);
                d.shop.put("minecraft:emerald", 100L);
                Files.writeString(FILE, GSON.toJson(d));
            }
            Data d = GSON.fromJson(Files.readString(FILE), Data.class);
            sell = d.sell; shop = d.shop;
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static Item item(String id) { return ITEMS.get(id); }
    public static String idOf(Item i) { return BuiltInRegistries.ITEM.getKey(i).toString(); }
}
