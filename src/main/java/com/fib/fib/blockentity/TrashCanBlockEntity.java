package com.fib.fib.blockentity;

import com.fib.fib.gui.container.menu.TrashCanMenu;
import com.fib.fib.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TrashCanBlockEntity extends BlockEntity implements MenuProvider {

    private int progress = 0;



    private final SimpleContainer inventory = new SimpleContainer(27) {
        @Override
        public void setChanged() {
            super.setChanged();
            TrashCanBlockEntity.this.setChanged();
        }
    };



    /**
     * 方块实体（BlockEntity）。
     *
     * BlockEntity 用于为方块提供“可存储的数据与运行逻辑”。
     * 与普通 Block 不同，它可以：
     *  - 保存数据（NBT）
     *  - 在每 tick 执行逻辑
     *  - 在世界重新加载后恢复状态
     *
     * @param pPos         方块在世界中的位置
     * @param pBlockState  当前方块状态（BlockState）
     */
    public TrashCanBlockEntity(BlockPos pPos, BlockState pBlockState) {

        // 绑定 BlockEntityType + 世界坐标 + 当前状态
        // 这一步决定：
        //   1. 它属于哪种实体类型
        //   2. 它附着在哪个位置
        //   3. 它对应的方块状态是什么
        super(ModBlockEntities.TRASH_CAN_BE.get(), pPos, pBlockState);
    }

    /**
     * 每游戏刻执行一次（前提是 Block 中注册了 ticker）。
     *
     * 这里我们让 progress 每 tick 自增，
     * 用于证明 BlockEntity 正在参与游戏循环。
     *
     * setChanged() 表示数据已被修改，
     * 告诉游戏该实体需要被保存。
     */
    public void tick() {
        progress++;
        setChanged();
    }


    public int getProgress() {
        return progress;
    }

    /**
     * 返回界面标题。
     *
     * 当玩家打开 GUI 时，
     * Screen 会使用这个 Component 作为窗口标题。
     */
    @Override
    public Component getDisplayName() {return Component.translatable("be.title.crate");}

    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> new InvWrapper(inventory));

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("Items", inventory.createTag());
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        inventory.fromTag(nbt.getList("Items", Tag.TAG_COMPOUND));
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new TrashCanMenu(id, inventory, this.inventory);
    }

    public Container getInventory() {
        return this.inventory;
    }
}