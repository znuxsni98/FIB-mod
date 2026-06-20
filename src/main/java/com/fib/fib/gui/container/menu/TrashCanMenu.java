package com.fib.fib.gui.container.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;


public class TrashCanMenu extends ChestMenu {


    public TrashCanMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(27));
    }
    public TrashCanMenu(int containerId, Inventory playerInventory, Container container) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
    }
}
