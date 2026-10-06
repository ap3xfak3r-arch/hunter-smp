package com.huntersmp;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;

/** Balances. Nothing here gives money by itself: only /eco, /sell, /ah sales and /pay call add(). */
public class Money {
    static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("huntersmp");
    static final Path FILE = DIR.resolve("balances.json");
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static Map<String, Long> bal = new HashMap<>();

    public static void load() {
        try {
            Files.createDirectories(DIR);
            if (Files.exists(FILE))
                bal = GSON.fromJson(Files.readString(FILE), new TypeToken<Map<String, Long>>() {}.getType());
        } catch (Exception e) { e.printStackTrace(); }
        if (bal == null) bal = new HashMap<>();
    }

    public static void save() {
        try { Files.createDirectories(DIR); Files.writeString(FILE, GSON.toJson(bal)); }
        catch (Exception e) { e.printStackTrace(); }
    }

    public static long get(UUID id) { return bal.getOrDefault(id.toString(), 0L); }
    public static void set(UUID id, long v) { bal.put(id.toString(), Math.max(0, v)); save(); }
    public static void add(UUID id, long v) { set(id, get(id) + v); }
    public static boolean take(UUID id, long v) {
        if (get(id) < v) return false;
        set(id, get(id) - v);
        return true;
    }

    /** 1500 -> $1.5K, 2000000 -> $2M (DonutSMP style) */
    public static String fmt(long v) {
        String[] suf = {"", "K", "M", "B", "T"};
        double d = v; int i = 0;
        while (d >= 1000 && i < suf.length - 1) { d /= 1000; i++; }
        String s = (d == Math.floor(d)) ? String.valueOf((long) d) : String.format("%.1f", d);
        return "$" + s + suf[i];
    }
}
