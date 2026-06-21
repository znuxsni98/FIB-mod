package com.fib.fib.blockentity;

import com.fib.fib.gui.container.menu.CrateMenu;
import com.fib.fib.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrateBlockEntity extends RandomizableContainerBlockEntity implements MenuProvider {
    private final int size = 27;
    private NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> new InvWrapper(this));


    public CrateBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.CRATE_BE.get(), pPos, pBlockState);
    }


    @Override
    public @NotNull Component getDisplayName() {return Component.translatable("be.title.crate");}

    @Override
    protected @NotNull Component getDefaultName() {return Component.translatable("be.title.crate");}


    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(@NotNull NonNullList<ItemStack> pItems) {
        this.items = pItems;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!this.remove && cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public void startOpen(@NotNull Player player) {
        super.startOpen(player);
        this.unpackLootTable(player);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag nbt) {
        super.saveAdditional(nbt);

        if (!this.trySaveLootTable(nbt)) {
            ContainerHelper.saveAllItems(nbt, this.items);
        }
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);

        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(nbt)) {
            ContainerHelper.loadAllItems(nbt, this.items);
        }
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory) {
        return new CrateMenu(id, playerInventory, getInventory());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
        return this.createMenu(id, playerInventory);
    }

    public Container getInventory() {
        return this;
    }


    @Override
    public int getContainerSize() {
        return size;
    }
}