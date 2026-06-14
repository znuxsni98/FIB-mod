package com.fib.fib.container.menu;

import com.fib.fib.blockentity.IceMakerBlockEntity;
import com.fib.fib.init.ModMenuTypes;
import com.fib.fib.init.block.ModBlocks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;


public class IceMakerMenu extends AbstractContainerMenu {


    public final IceMakerBlockEntity blockEntity;

    private final Level level;

    private final ContainerData data;


    public IceMakerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(1));
    }


    public IceMakerMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {

        super(ModMenuTypes.ICE_MAKER_MENU.get(), id);

        this.blockEntity = (IceMakerBlockEntity) entity;

        this.level = inv.player.level();

        this.data = data;

        addDataSlots(data);
    }

    /**
     * Shift 点击快速移动物品的逻辑。
     *
     * 由于当前菜单还没有任何物品槽位，
     * 因此这里暂时返回 null。
     * 在后续实现物品槽时，这里会被完善。
     * 因为没有槽位，所以目前返回 null 是安全的
     *
     */
    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }


    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player,
                ModBlocks.ICE_MAKER.get()
        );
    }


    public IceMakerBlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}
