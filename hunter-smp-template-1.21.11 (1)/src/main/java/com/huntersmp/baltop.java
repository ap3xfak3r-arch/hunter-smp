package com.huntersmp;

import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.*;
import java.util.*;

public class BalTop {
    static final Path FILE = Money.DIR.resolve("names.json");
    static Map<String, String> names = new HashMap<>();

    public static void init() {
        try {
            Files.createDirectories(Money.DIR);
            if (Files.exists(FILE)) {
                Map<String, String> m = Money.GSON.fromJson(Files.readString(FILE),
                        new TypeToken<Map<String, String>>() {}.getType());
                if (m != null) names = m;
            }
        } catch (Exception e) { e.printStackTrace(); }
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> remember(handler.getPlayer()));
    }

    static void remember(ServerPlayer p) {
        String n = p.getGameProfile().name();
        if (!n.equals(names.put(p.getUUID().toString(), n))) {
            try { Files.writeString(FILE, Money.GSON.toJson(names)); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("baltop").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Set<String> ids = new HashSet<>(names.keySet());
            ids.addAll(Money.bal.keySet());
            List<String> sorted = new ArrayList<>(ids);
            sorted.sort((a, b) -> Long.compare(Money.bal.getOrDefault(b, 0L), Money.bal.getOrDefault(a, 0L)));
            p.sendSystemMessage(Component.literal("§6§l--- Top Balances ---"));
            String[] colors = {"§6", "§f", "§c"};
            for (int i = 0; i < Math.min(10, sorted.size()); i++) {
                String id = sorted.get(i);
                String name = names.getOrDefault(id, id.substring(0, 8));
                String color = i < 3 ? colors[i] : "§7";
                p.sendSystemMessage(Component.literal(color + "#" + (i + 1) + " §f" + name
                        + " §a" + Money.fmt(Money.bal.getOrDefault(id, 0L))));
            }
            return 1;
        }));
    }
}
