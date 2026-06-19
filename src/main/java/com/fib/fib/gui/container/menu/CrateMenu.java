package com.fib.fib.gui.container.menu;

import com.fib.fib.blockentity.CrateBlockEntity;
import com.fib.fib.init.ModMenuTypes;
import com.fib.fib.init.block.ModBlocks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;


public class CrateMenu extends AbstractContainerMenu {


    public final CrateBlockEntity blockEntity;

    private final Level level;

    private final ContainerData data;

    private static final int INPUT1 = 0;
    private static final int INPUT2 = 1;
    private static final int INPUT3 = 2;
    private static final int INPUT4 = 3;
    private static final int INPUT5 = 4;
    private static final int INPUT6 = 5;
    private static final int INPUT7 = 6;
    private static final int INPUT8 = 7;
    private static final int INPUT9 = 8;
    private static final int INPUT10 = 9;
    private static final int INPUT11 = 10;
    private static final int INPUT12 = 11;
    private static final int INPUT13 = 12;
    private static final int INPUT14 = 13;
    private static final int INPUT15 = 14;
    private static final int INPUT16 = 15;
    private static final int INPUT17 = 16;
    private static final int INPUT18 = 17;
    private static final int INPUT19 = 18;
    private static final int INPUT20 = 19;
    private static final int INPUT21 = 20;
    private static final int INPUT22 = 21;
    private static final int INPUT23 = 22;
    private static final int INPUT24 = 23;
    private static final int INPUT25 = 24;
    private static final int INPUT26 = 25;
    private static final int INPUT27 = 26;;



    public CrateMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv,
                inv.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(1));
    }


    public CrateMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {

        super(ModMenuTypes.CRATE_MENU.get(), id);

        this.blockEntity = (CrateBlockEntity) entity;
        this.addMachineSlots(blockEntity.getItemHandler());

        this.level = inv.player.level();

        this.data = data;

        // 添加玩家背包与快捷栏
        addPlayerInventory(inv, 7, 83);
        addPlayerHotbar(inv, 7, 141);

        addDataSlots(data);
    }


    //向当前 Menu 中添加机器自身的槽位。
    private void addMachineSlots(IItemHandler handler) {
        // 循环 27 次，对应 INPUT1 到 INPUT27
        for (int i = 0; i < 27; i++) {
            // 计算当前是第几行（0, 1, 2）
            int row = i / 9;
            // 计算当前是第几列（0 到 8）
            int col = i % 9;

            // 计算 X 坐标：基准 8，每列增加 18
            int x = 8 + col * 18;
            // 计算 Y 坐标：基准 18，每行增加 18
            int y = 18 + row * 18;

            // 添加槽位，注意 handler 的槽位索引是从 0 开始的，所以直接使用 i
            this.addSlot(new SlotItemHandler(handler, i, x, y));
        }
    }

  //添加玩家背包
    private void addPlayerInventory(Inventory inv, int leftCol, int topRow) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(
                        inv,
                        col + row * 9 + 9,
                        leftCol + col * 18,
                        topRow + row * 18
                ));
            }
        }
    }
   //添加玩家快捷栏
    private void addPlayerHotbar(Inventory inv, int leftCol, int topRow) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(
                    inv,
                    col,
                    leftCol + col * 18,
                    topRow
            ));
        }
    }


    //Shift 点击快速移动物品的逻辑。
    //槽位数
    private static final int TE_SLOT_COUNT = 4;

    @Override
    public ItemStack quickMoveStack(Player player, int index) {

        // 先拿到当前被 Shift 点击的槽位
        Slot slot = this.slots.get(index);

        // 如果槽位不存在，或者槽位里没有物品，直接返回空
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        // 当前槽位中的物品
        ItemStack stack = slot.getItem();

        // 复制一份原物品，作为方法返回值
        ItemStack copy = stack.copy();

        // ========= 槽位区间划分 =========
        // 机器槽位：
        // 0 -> 输入槽
        // 2 -> 输入槽-流体
        // 2 -> 输出槽
        // 3 -> 输出槽-流体
        final int INPUT_SLOT = 0;
        final int INPUT_SLOT_FLUID = 1;
        final int OUTPUT_SLOT = 2;
        final int OUTPUT_SLOT_FLUID = 3;

        final int TE_START = 0;
        final int TE_END = TE_START + TE_SLOT_COUNT;   // [0, 2)

        // 玩家主背包：27 格
        final int PLAYER_INV_START = TE_END;
        final int PLAYER_INV_END = PLAYER_INV_START + 27;   // [2, 29)

        // 玩家快捷栏：9 格
        final int HOTBAR_START = PLAYER_INV_END;
        final int HOTBAR_END = HOTBAR_START + 9;            // [29, 38)

        // ========= 快速移动逻辑 =========

        // 如果点击的是输出槽
        // 优先移动到快捷栏，快捷栏放不下再移动到主背包
        if (index == OUTPUT_SLOT) {
            if (!this.moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                if (!this.moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        // 如果点击的是输入槽
        // 直接移动到玩家背包 + 快捷栏
        else if (index == INPUT_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        }


        // 如果点击的是玩家背包或快捷栏
        // 只尝试进入输入槽，不会进入输出槽
        else if (index >= TE_END && index < HOTBAR_END) {
            if (!this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        // 其他异常情况，直接返回空
        else {
            return ItemStack.EMPTY;
        }

        // ========= 更新原槽位状态 =========

        // 如果原物品已经被搬空，就把原槽位设为空
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        // 触发槽位取出逻辑
        slot.onTake(player, stack);

        return copy;
    }

    //检查玩家是否仍然可以使用该界面
    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player,
                ModBlocks.CRATE.get()
        );
    }


    public CrateBlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}
