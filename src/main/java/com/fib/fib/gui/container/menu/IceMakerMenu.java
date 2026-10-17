package com.fib.fib.gui.container.menu;

import com.fib.fib.blockentity.IceMakerBlockEntity;
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


public class IceMakerMenu extends AbstractContainerMenu {


    public final IceMakerBlockEntity blockEntity;

    private final Level level;

    private final ContainerData data;

    // 输入槽
    private static final int INPUT_SLOT = IceMakerBlockEntity.INPUT_SLOT;
    // 输入槽-流体
    private static final int INPUT_SLOT_FLUID = IceMakerBlockEntity.INPUT_SLOT_FLUID;
    // 输出槽
    private static final int OUTPUT_SLOT = IceMakerBlockEntity.OUTPUT_SLOT;
    // 输出槽-流体
    private static final int OUTPUT_SLOT_FLUID = IceMakerBlockEntity.OUTPUT_SLOT_FLUID;


    /**
     * 客户端构造：服务端通过 {@code NetworkHooks.openScreen(player, provider, pos)}
     * 只把方块坐标发过来，这里自己按坐标去客户端世界里找那个方块实体。
     */
    public IceMakerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()));
    }

    /** 客户端兜底构造：方块实体还没同步到位时用（少见，但 chunk 边界上会出现）。 */
    public IceMakerMenu(int id, Inventory inv, BlockEntity entity) {
        this(id, inv, entity,
                entity instanceof IceMakerBlockEntity be
                        ? be.getData()
                        : new SimpleContainerData(IceMakerBlockEntity.DATA_COUNT));
    }


    public IceMakerMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {

        super(ModMenuTypes.ICE_MAKER_MENU.get(), id);

        this.blockEntity = (IceMakerBlockEntity) entity;
        this.addMachineSlots(blockEntity.getItemHandler());

        this.level = inv.player.level();

        this.data = data;

        // 添加玩家背包与快捷栏。
        // 这里的坐标是"物品"的左上角，槽位方框画在 (x-1, y-1)。
        // 贴图里量出来的方框顶边在 y=83/101/119/141、左边在 x=7，所以物品坐标是 8 / 84、102、120 / 142。
        // 之前写成 7 / 83 和 141，整套背包（含标题）就整体偏到左上 1 像素了。
        addPlayerInventory(inv, 8, 84);
        addPlayerHotbar(inv, 8, 142);

        // ⚠ 这里注册的条目数是【本对象】data.getCount()。
        // 客户端原来传的是 new SimpleContainerData(1)，于是服务端推过来的下标 >= 1 的数据
        // （总进度、能量、液面）全部找不到接收者被丢掉 —— 表现就是进度条永远不动、
        // 液面永远是空的。客户端必须用和方块实体同样长度的 ContainerData。
        addDataSlots(data);
    }


    //向当前 Menu 中添加机器自身的槽位。
    private void addMachineSlots(IItemHandler handler) {
        this.addSlot(new SlotItemHandler(handler, INPUT_SLOT, 72, 35));
        this.addSlot(new SlotItemHandler(handler, INPUT_SLOT_FLUID, 14, 14));
        this.addSlot(new SlotItemHandler(handler, OUTPUT_SLOT, 132, 35));
        this.addSlot(new SlotItemHandler(handler, OUTPUT_SLOT_FLUID, 14, 53));
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


    // ========= 槽位区间 =========
    /** 机器自身槽位数 */
    private static final int TE_SLOT_COUNT = IceMakerBlockEntity.SLOT_COUNT;
    private static final int TE_START = 0;
    private static final int TE_END = TE_START + TE_SLOT_COUNT;      // [0, 4)
    private static final int INV_START = TE_END;
    private static final int INV_END = INV_START + 27;               // [4, 31)
    private static final int HOTBAR_START = INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;          // [31, 40)

    /**
     * Shift 点击快速移动。
     *
     * <p>上一版有个真实的吞物品 bug：{@code if (index == OUTPUT_SLOT) { ...移动... }}
     * 后面跟了一条 {@code else if (index >= TE_END && ...)} 的链，两者并不是同一条 if/else，
     * 于是输出槽分支执行完还会继续往下走到最后的 {@code else return ItemStack.EMPTY} ——
     * 物品确实动过了，但方法返回空栈，客户端据此认为什么都没发生，槽位就不同步了。
     *
     * <p>这里改成标准的二分支结构：机器槽 ↔ 玩家背包，互不干扰。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {

        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < TE_END) {
            // ---- 机器槽 -> 玩家背包 + 快捷栏 ----
            if (!this.moveItemStackTo(stack, INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // ---- 玩家背包 -> 机器 ----
            // 只往两个输入槽送。输出槽由机器自己填，玩家不能手动塞。
            // 先试流体槽（mayPlace 会挡住非流体容器），搬不动再试物品槽。
            // 两次都搬不动就返回空栈，让原槽纹丝不动。
            boolean moved = this.moveItemStackTo(stack, INPUT_SLOT_FLUID, INPUT_SLOT_FLUID + 1, false)
                    || this.moveItemStackTo(stack, INPUT_SLOT, INPUT_SLOT + 1, false);
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        // ========= 更新原槽位状态 =========
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        // 一个都没搬走 —— 返回空栈，别让客户端以为动过了
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);

        return copy;
    }

    //检查玩家是否仍然可以使用该界面
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

    public ContainerData getData() {
        return this.data;
    }
}
