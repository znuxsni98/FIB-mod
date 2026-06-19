package com.fib.fib.blockentity;

import com.fib.fib.gui.container.menu.IceMakerMenu;
import com.fib.fib.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IceMakerBlockEntity extends BlockEntity implements MenuProvider {



    private int progress = 0;
    // 输入槽索引
    private static final int INPUT_SLOT = 0;
    // 输入槽索引-流体
    private static final int INPUT_SLOT_FLUID = 1;
    // 输出槽索引
    private static final int OUTPUT_SLOT = 2;
    // 输出槽索引-流体
    private static final int OUTPUT_SLOT_FLUID = 3;


    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {

        /**
         * 当某个槽位内容发生变化时调用。
         *
         * 这里调用 setChanged()，告诉游戏：
         * 当前 BlockEntity 的数据已经发生修改，需要被标记为“已更改”，
         * 这样世界保存时才会把新数据写入存档。
         */
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        /**
         * 控制某个槽位是否允许放入指定物品。
         *
         * 当前实现中：
         * 输入槽允许放入物品
         * 输出槽不允许手动放入物品
         *
         * 这正符合大多数机器的常见逻辑：
         * 玩家把原料放进输入槽而非输出槽，产物只会出现在输出槽。
         */
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot == INPUT_SLOT || slot == INPUT_SLOT_FLUID;
        }
    };


     //将当前机器内部的所有物品掉落到世界中。
    public void drops() {
        // 创建一个临时容器，大小与机器槽位数量一致
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());

        // 将 itemHandler 中的每个槽位内容复制到临时容器中
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        // 将容器中的物品掉落到世界
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }


    protected final ContainerData data = new ContainerData() {

        /**
         * Menu 读取数据时调用。
         * 根据 index 返回对应的数据值。
         */
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                default -> 0;
            };
        }

        /**
         * Menu 写入数据时调用。
         * 客户端同步数据时会通过这里写回。
         */
        @Override
        public void set(int index, int value) {
            if (index == 0) progress = value;
        }

        /**
         * 返回需要同步的数据数量。
         *
         * 因为这里只有 progress 一个变量，
         * 所以返回 1。
         */
        @Override
        public int getCount() {
            return 1;
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
    public IceMakerBlockEntity(BlockPos pPos, BlockState pBlockState) {

        // 绑定 BlockEntityType + 世界坐标 + 当前状态
        // 这一步决定：
        //   1. 它属于哪种实体类型
        //   2. 它附着在哪个位置
        //   3. 它对应的方块状态是什么
        super(ModBlockEntities.ICE_MAKER_BE.get(), pPos, pBlockState);
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
     * 写入存档数据（NBT）。
     *
     * 当世界保存或区块卸载时调用。
     * 只有在这里写入的数据，才能在重进世界后恢复。
     */
    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        // 将内部物品栏序列化后写入 NBT
        // "inventory" 是这一组库存数据在存档中的键名
        pTag.put("inventory", itemHandler.serializeNBT());

        // 将 progress 写入 NBT
        pTag.putInt("Progress", progress);
    }

    /**
     * 从存档读取数据（NBT）。
     *
     * 当区块加载或方块实体被重建时调用。
     * 必须与 saveAdditional 使用相同的键名。
     */
    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        // 从 NBT 中读取库存数据并恢复到 itemHandler
        // 键名必须与 saveAdditional 中保持一致
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));

        // 从 NBT 中读取 progress
        progress = pTag.getInt("Progress");
    }

    /**
     * 返回界面标题。
     *
     * 当玩家打开 GUI 时，
     * Screen 会使用这个 Component 作为窗口标题。
     */
    @Override
    public Component getDisplayName() {
        return Component.translatable("be.title.ice_maker");
    }

    /**
     * 创建 Menu。
     *
     * 当玩家打开这个方块的界面时，
     * Forge 会调用这个方法来创建对应的 Menu。
     *
     * id：菜单同步 ID
     * inventory：玩家物品栏
     * player：打开界面的玩家
     *
     * 这里我们把当前 BlockEntity 与 ContainerData
     * 传入 Menu，使界面能够访问机器数据并进行同步。
     */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new IceMakerMenu(id, inventory, this, data);
    }

    /**
     * 返回当前机器内部的物品处理器。
     *
     * Menu 会通过这个方法获取库存，
     * 再基于它创建真正的 GUI 槽位。
     */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }
}