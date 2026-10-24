package com.fib.fib.gui.container.menu;

import com.fib.fib.init.ModMenuTypes;
import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import com.fib.fib.init.item.custom.hook.HookUpgradeType;
import com.fib.fib.init.item.custom.hook.IHookMaterial;
import com.fib.fib.init.item.custom.hook.SmartUpgradeModuleItem;
import com.fib.fib.init.item.custom.hook.GrapplingHookUpgradeItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * 钩爪枪升级与仓储配置容器菜单。
 * 管理 5 大类升级部件槽位（含智能升级模块解锁的第 2 模块槽）与 8 格掉落物暂存仓。
 */
public class GrapplingHookMenu extends AbstractContainerMenu {
    private final ItemStack gunStack;
    private final InteractionHand hand;
    private final ItemStackHandler upgradesHandler;
    private final ItemStackHandler storageHandler;

    public GrapplingHookMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readItem(), extraData.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public GrapplingHookMenu(int containerId, Inventory playerInventory, ItemStack gunStack, InteractionHand hand) {
        super(ModMenuTypes.GRAPPLING_HOOK_MENU.get(), containerId);
        this.gunStack = gunStack;
        this.hand = hand;
        this.upgradesHandler = GrapplingHookGunItem.getUpgradesHandler(gunStack);
        this.storageHandler = GrapplingHookGunItem.getStorageHandler(gunStack);

        // 1. 升级部件槽位 (Slots 0 ~ 5)
        // 槽位 0: 钩爪材质槽
        addSlot(new SlotItemHandler(upgradesHandler, 0, 16, 20) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                boolean dualItem = stack.getItem() instanceof com.fib.fib.init.item.custom.hook.DualHookItem;
                // 实时读取槽位 4，而不是构造时的 gunStack 快照：
                // 否则刚放入双钩模块时 NBT 尚未保存，会导致双头钩爪放不进去
                boolean dualModule = upgradesHandler.getStackInSlot(4).getItem()
                        instanceof com.fib.fib.init.item.custom.hook.DualHookModuleItem;
                // 双钩模块与双头钩爪必须配套：装了模块只能放双头钩爪，未装模块不能放双头钩爪
                if (dualModule != dualItem) {
                    return false;
                }
                return stack.getItem() instanceof IHookMaterial;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // 槽位 1: 性能模块槽 1
        addSlot(new SlotItemHandler(upgradesHandler, 1, 38, 20) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof GrapplingHookUpgradeItem up && up.getUpgradeType() == HookUpgradeType.MODULE;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // 槽位 2: 性能模块槽 2（需安装智能升级模块解锁）
        addSlot(new SlotItemHandler(upgradesHandler, 2, 38, 42) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return isSlot2Unlocked() && stack.getItem() instanceof GrapplingHookUpgradeItem up && up.getUpgradeType() == HookUpgradeType.MODULE;
            }

            @Override
            public boolean isActive() {
                return isSlot2Unlocked();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // 槽位 3: 功能槽（智能升级模块）
        addSlot(new SlotItemHandler(upgradesHandler, 3, 16, 42) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof GrapplingHookUpgradeItem up && up.getUpgradeType() == HookUpgradeType.FUNCTION;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // 槽位 4: 改进槽（双钩模块 / 掉落物保护模块）
        addSlot(new SlotItemHandler(upgradesHandler, 4, 16, 64) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof GrapplingHookUpgradeItem up && up.getUpgradeType() == HookUpgradeType.IMPROVEMENT;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void set(@NotNull ItemStack stack) {
                boolean wasDual = getItem().getItem() instanceof com.fib.fib.init.item.custom.hook.DualHookModuleItem;
                boolean willDual = stack.getItem() instanceof com.fib.fib.init.item.custom.hook.DualHookModuleItem;

                // 放入双钩模块前：记住当前模式
                if (!wasDual && willDual) {
                    GrapplingHookGunItem.rememberPreDualMode(gunStack);
                    // 安装双钩模块：回收当前所有已发射钩爪（普通单钩不再适用）
                    var player = playerInventory.player;
                    if (!player.level().isClientSide) {
                        for (var hook : player.level().getEntitiesOfClass(
                                com.fib.fib.init.entity.custom.GrapplingHookEntity.class,
                                player.getBoundingBox().inflate(256.0),
                                h -> h.getShooter() == player && !h.isRemoved()
                                        && h.getHookState() != com.fib.fib.init.entity.custom.GrapplingHookEntity.State.RETRACTING
                                        && h.getHookState() != com.fib.fib.init.entity.custom.GrapplingHookEntity.State.DROPPING)) {
                            hook.setRetracting();
                            hook.clearPartnerHook();
                        }
                    }
                }
                super.set(stack);
                // 卸下双钩模块后：恢复此前模式
                if (wasDual && !willDual) {
                    GrapplingHookGunItem.restorePreDualMode(gunStack);
                    var player = playerInventory.player;
                    if (!player.level().isClientSide) {
                        // 回收该玩家所有已发射的双钩钩爪（含「发射一半」的单枚与已成对的）
                        var hooks = player.level().getEntitiesOfClass(
                                com.fib.fib.init.entity.custom.GrapplingHookEntity.class,
                                player.getBoundingBox().inflate(256.0),
                                h -> h.getShooter() == player && !h.isRemoved()
                                        && h.getHookState() != com.fib.fib.init.entity.custom.GrapplingHookEntity.State.RETRACTING
                                        && h.getHookState() != com.fib.fib.init.entity.custom.GrapplingHookEntity.State.DROPPING);

                        // 统计成对组数：只有成对才算「已消耗」，才需要返还
                        int pairCount = 0;
                        java.util.Set<com.fib.fib.init.entity.custom.GrapplingHookEntity> visited = new java.util.HashSet<>();
                        for (var hook : hooks) {
                            if (visited.contains(hook)) continue;
                            visited.add(hook);
                            var partner = hook.getPartnerHook();
                            if (partner != null && !partner.isRemoved() && hooks.contains(partner)) {
                                visited.add(partner);
                                pairCount++;
                            }
                        }
                        for (var hook : hooks) {
                            hook.setRetracting();
                            hook.clearPartnerHook();
                        }

                        // 成对的按数量返还（存储仓 -> 背包 -> 掉落）；「发射一半」的单枚不返还——
                        // 其双钩物品仍在钩爪槽，关闭界面时由 validateDualHookDependency 弹出。
                        GrapplingHookGunItem.refundDualHookRaw(player.level(), player, gunStack, pairCount);
                    }
                }
            }
        });

        // 槽位 5: 钩爪模块槽（挖掘模块）
        addSlot(new SlotItemHandler(upgradesHandler, 5, 38, 64) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof GrapplingHookUpgradeItem up && up.getUpgradeType() == HookUpgradeType.HOOK_MODULE;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        // 2. 钩爪存储仓槽位 (8 格, 4x2, Slots 6 ~ 13)
        // 贴图中第一格框左上角 (93,33)，槽位填物品坐标，故为 (94,34)
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                int slotIndex = row * 4 + col;
                addSlot(new SlotItemHandler(storageHandler, slotIndex, 94 + col * 18, 34 + row * 18));
            }
        }

        // 3. 玩家背包与快捷栏 (Slots 22 ~ 57)
        int playerInvY = 104;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInvY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 162));
        }
    }

    public boolean isSlot2Unlocked() {
        return upgradesHandler.getStackInSlot(3).getItem() instanceof SmartUpgradeModuleItem;
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack held = player.getItemInHand(hand);
        return !held.isEmpty() && held.getItem() instanceof GrapplingHookGunItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // 保存升级槽与存储数据进物品 NBT
        if (!player.level().isClientSide) {
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof GrapplingHookGunItem) {
                GrapplingHookGunItem.saveUpgradesHandler(held, upgradesHandler);
                GrapplingHookGunItem.saveStorageHandler(held, storageHandler);

                // 双钩模块被卸下时，强制卸载双头钩爪并归还给玩家
                ItemStack ejected = GrapplingHookGunItem.validateDualHookDependency(held);
                if (!ejected.isEmpty() && !player.getInventory().add(ejected)) {
                    player.drop(ejected, false);
                }

                // 双钩回收已移至槽位 4 的 set()：只有「卸下模块」这一事件才回收，
                // 不能在 removed() 里判断模块是否存在，否则每次关闭 GUI 都会误回收。
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            // 升级槽 (0-5) 或存储仓 (6-13) -> 移动到玩家背包 (14-49)
            if (index < 14) {
                if (!this.moveItemStackTo(slotStack, 14, 50, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // 玩家背包 -> 尝试移入升级槽或存储仓
            else {
                // 优先尝试放入升级槽
                boolean placedInUpgrade = false;
                if (slotStack.getItem() instanceof IHookMaterial) {
                    placedInUpgrade = this.moveItemStackTo(slotStack, 0, 1, false);
                } else if (slotStack.getItem() instanceof GrapplingHookUpgradeItem up) {
                    switch (up.getUpgradeType()) {
                        case MODULE -> placedInUpgrade = this.moveItemStackTo(slotStack, 1, isSlot2Unlocked() ? 3 : 2, false);
                        case FUNCTION -> placedInUpgrade = this.moveItemStackTo(slotStack, 3, 4, false);
                        case IMPROVEMENT -> placedInUpgrade = this.moveItemStackTo(slotStack, 4, 5, false);
                        case HOOK_MODULE -> placedInUpgrade = this.moveItemStackTo(slotStack, 5, 6, false);
                    }
                }

                // 若非升级模块或升级槽已满，放入 8 格存储仓
                if (!placedInUpgrade) {
                    if (!this.moveItemStackTo(slotStack, 6, 14, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }
}
