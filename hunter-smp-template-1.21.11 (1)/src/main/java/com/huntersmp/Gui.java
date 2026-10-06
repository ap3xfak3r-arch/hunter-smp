package com.huntersmp;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Server-side-only 6x9 chest GUI. All clicks are cancelled and forwarded to a handler. */
public class Gui extends ChestMenu {
    public interface Click { void on(ServerPlayer p, int slot, ClickType type); }
    private final Click click;

    private Gui(int id, Inventory inv, SimpleContainer c, Click click) {
        super(MenuType.GENERIC_9x6, id, inv, c, 6);
        this.click = click;
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot >= 0 && slot < 54 && player instanceof ServerPlayer sp) click.on(sp, slot, type);
        sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }

    public static void open(ServerPlayer p, Component title, SimpleContainer c, Click click) {
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new Gui(id, inv, c, click), title));
    }
}
