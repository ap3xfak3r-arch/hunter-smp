package com.huntersmp;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import java.nio.file.*;
import java.util.*;

/** Loads config/huntersmp/prices.json. "sell" = /sell price per item, "shop" = /shop price per item. */
public class Prices {
    static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("huntersmp/prices.json");
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Map<String, Long> sell = new LinkedHashMap<>();
    public static Map<String, Long> shop = new LinkedHashMap<>();
    public static Map<String, List<String>> categories = new LinkedHashMap<>();
    public static Map<String, String> icons = new LinkedHashMap<>();
    static final Map<String, Item> ITEMS = new HashMap<>();
       static final long DEFAULT_BUY = 20, DEFAULT_SELL = 10;
   static final List<String> BANNED = List.of("spawn_egg", "command_block", "barrier", "bedrock", "structure_", "jigsaw",
           "debug_stick", "light", "knowledge_book", "spawner", "reinforced_deepslate", "end_portal", "test_",
           "petrified", "farmland", "dirt_path", "chorus_plant", "frogspawn", "budding", "infested", "player_head", "minecraft:air");

    static class Data {
        Map<String, Long> sell = new LinkedHashMap<>();
        Map<String, Long> shop = new LinkedHashMap<>();
        Map<String, List<String>> categories = new LinkedHashMap<>();
        Map<String, String> icons = new LinkedHashMap<>();
    }

    // {category, icon item, "item:buyPrice[:sellPrice] ..."}  sell defaults to half of buy.
    // THESE ARE ESTIMATES, NOT REAL DONUTSMP PRICES. Edit prices.json after the first start.
    static final String[][] DEFAULTS = {
        {"Ores", "diamond", "coal:8 raw_copper:6 copper_ingot:8 raw_iron:15 iron_ingot:16 raw_gold:25 gold_ingot:28 redstone:6 lapis_lazuli:8 quartz:10 amethyst_shard:20 emerald:120 diamond:250 coal_block:72 copper_block:72 iron_block:144 gold_block:252 redstone_block:54 lapis_block:72 emerald_block:1080 diamond_block:2250"},
        {"Blocks", "grass_block", "cobblestone:2 stone:3 deepslate:3 dirt:1 grass_block:2 sand:2 gravel:2 andesite:2 diorite:2 granite:2 sandstone:3 clay:4 terracotta:6 glass:4 bricks:8 stone_bricks:4 oak_log:6 spruce_log:6 birch_log:6 jungle_log:6 acacia_log:6 dark_oak_log:6 cherry_log:6 mangrove_log:6 oak_planks:2 spruce_planks:2 birch_planks:2 jungle_planks:2 acacia_planks:2 dark_oak_planks:2 cherry_planks:2 mangrove_planks:2 white_wool:10 obsidian:40 ice:4 snow_block:3 prismarine:8 sea_lantern:25 bookshelf:60"},
        {"Food", "bread", "bread:8 apple:5 cooked_beef:12 cooked_porkchop:12 cooked_chicken:10 cooked_mutton:10 cooked_salmon:10 cooked_cod:8 baked_potato:5 golden_carrot:20 golden_apple:150 enchanted_golden_apple:5000 carrot:3 potato:3 wheat:4 wheat_seeds:1 beetroot:3 sugar_cane:3 sugar:3 pumpkin:6 melon_slice:2 cactus:3 bone_meal:5 cocoa_beans:4 sweet_berries:2 honey_bottle:15 cake:60 pumpkin_pie:12"},
        {"Combat", "diamond_sword", "diamond_sword:600 diamond_helmet:1250 diamond_chestplate:2000 diamond_leggings:1750 diamond_boots:1000 diamond_pickaxe:750 diamond_axe:750 diamond_shovel:250 iron_sword:60 iron_helmet:80 iron_chestplate:130 iron_leggings:110 iron_boots:65 bow:40 crossbow:60 arrow:2 shield:40 totem_of_undying:3000 experience_bottle:40 tnt:80 trident:2500"},
        {"Nether", "netherrack", "netherrack:1 nether_bricks:4 soul_sand:5 soul_soil:5 glowstone:20 glowstone_dust:8 nether_wart:10 blaze_rod:40 ghast_tear:100 magma_cream:30 crying_obsidian:80 ancient_debris:1500 netherite_scrap:1500 netherite_ingot:7000 netherite_block:63000 nether_star:8000 wither_skeleton_skull:3000 basalt:2 blackstone:2 magma_block:8 quartz_block:40"},
        {"End", "end_stone", "end_stone:3 purpur_block:8 chorus_fruit:10 popped_chorus_fruit:12 ender_pearl:60 ender_eye:120 end_crystal:400 dragon_breath:200 shulker_shell:800 elytra:10000 end_rod:15 dragon_egg:100000"},
        {"Misc", "hopper", "repeater:20 comparator:40 piston:30 sticky_piston:50 observer:30 hopper:60 dispenser:30 dropper:30 slime_ball:15 string:3 gunpowder:12 bone:4 spider_eye:5 feather:3 leather:6 ink_sac:4 glow_ink_sac:8 name_tag:100 saddle:150 lead:20 bucket:40 water_bucket:45 lava_bucket:60 anvil:150 enchanting_table:300 book:12 rail:6 powered_rail:20 minecart:40 torch:2 lantern:8 chest:8 ender_chest:200 shulker_box:900 beacon:9000"}
    };

    public static void load() {
        for (Item i : BuiltInRegistries.ITEM) ITEMS.put(BuiltInRegistries.ITEM.getKey(i).toString(), i);
        try {
            Files.createDirectories(FILE.getParent());
            Data d = Files.exists(FILE) ? GSON.fromJson(Files.readString(FILE), Data.class) : new Data();
            if (d == null) d = new Data();
            if (d.categories == null || d.categories.isEmpty()) {
                d.categories = new LinkedHashMap<>();
                d.icons = new LinkedHashMap<>();
                for (String[] row : DEFAULTS) {
                    List<String> ids = new ArrayList<>();
                    for (String tok : row[2].trim().split("\\s+")) {
                        String[] p = tok.split(":");
                        String id = "minecraft:" + p[0];
                        long buy = Long.parseLong(p[1]);
                        long sellPrice = p.length > 2 ? Long.parseLong(p[2]) : Math.max(1, buy / 2);
                        ids.add(id);
                        d.shop.putIfAbsent(id, buy);   // keeps any price you already set
                        d.sell.putIfAbsent(id, sellPrice);
                    }
                    d.categories.put(row[0], ids);
                    d.icons.put(row[0], "minecraft:" + row[1]);
                }
                Files.writeString(FILE, GSON.toJson(d));
            }
            sell = d.sell; shop = d.shop; categories = d.categories; icons = d.icons;
            Set<String> used = new HashSet<>();
for (List<String> l : categories.values()) used.addAll(l);
List<String> rest = new ArrayList<>();
for (String id : ITEMS.keySet()) {
    if (used.contains(id) || BANNED.stream().anyMatch(id::contains)) continue;
    rest.add(id);
}
Collections.sort(rest);
for (String id : rest) { shop.putIfAbsent(id, DEFAULT_BUY); sell.putIfAbsent(id, DEFAULT_SELL); }
categories.put("Everything Else", rest);
icons.put("Everything Else", "minecraft:chest");
        } catch (Exception e) { e.printStackTrace();  
    }

    public static Item item(String id) { return ITEMS.get(id); }
    public static String idOf(Item i) { return BuiltInRegistries.ITEM.getKey(i).toString(); }
}
