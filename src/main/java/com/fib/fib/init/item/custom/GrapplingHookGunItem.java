package com.fib.fib.init.item.custom;

import com.fib.fib.gui.container.menu.GrapplingHookMenu;
import com.fib.fib.init.block.ModBlocks;
import com.fib.fib.init.entity.custom.GrapplingHookEntity;
import com.fib.fib.init.item.custom.hook.*;
import com.fib.fib.init.sound.ModSounds;
import com.simibubi.create.content.equipment.armor.BacktankBlockEntity;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 钩爪枪核心物品类。
 * 具备双资源消耗（能量与耐久）、多槽位模块化升级、三态挖掘系统与背罐充能集成。
 */
public class GrapplingHookGunItem extends Item {
    public static final String TAG_ENERGY = "fib_energy";
    public static final String TAG_MODE = "fib_mode";
    public static final String TAG_MINING_MODE = "fib_mining_mode";
    public static final String TAG_UPGRADES = "fib_upgrades";
    public static final String TAG_STORAGE = "fib_storage";

    public static final float MAX_ENERGY = 20.0f;
    public static final int MAX_DURABILITY = 250;
    public static final float AIR_TO_ENERGY_RATIO = 1.0f; // 1秒背罐气体 = 1点能量

    public enum Mode {
        PULL("fib_mod.hook_mode.pull"),
        CLIMB("fib_mod.hook_mode.climb"),
        PICKUP("fib_mod.hook_mode.pickup"),
        /** 双钩模式：两枚钩爪互连成拴绳。 */
        DUAL("fib_mod.hook_mode.dual"),
        /** 快速模式：像蜘蛛侠一样连续钩住并拉向目标。 */
        QUICK("fib_mod.hook_mode.quick");

        private final String translationKey;

        Mode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    public enum MiningMode {
        OFF("fib_mod.mining_mode.off"),
        MINE_ALL("fib_mod.mining_mode.all"),
        MINE_BREAKABLE("fib_mod.mining_mode.breakable");

        private final String translationKey;

        MiningMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    public enum GrappleMode {
        STABLE("fib_mod.grapple_mode.stable"),
        PULL_TOWARDS("fib_mod.grapple_mode.pull_towards");

        private final String translationKey;

        GrappleMode(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    public GrapplingHookGunItem(Properties properties) {
        super(properties.durability(MAX_DURABILITY));
    }

    // ==================== 能量与 NBT 工具方法 ====================

    public static float getEnergy(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ENERGY)) {
            return MAX_ENERGY;
        }
        return tag.getFloat(TAG_ENERGY);
    }

    public static void setEnergy(ItemStack stack, float energy) {
        energy = Math.max(0.0f, Math.min(MAX_ENERGY, energy));
        stack.getOrCreateTag().putFloat(TAG_ENERGY, energy);
    }

    public static boolean consumeEnergy(ItemStack stack, float amount) {
        float current = getEnergy(stack);
        if (current >= amount) {
            setEnergy(stack, current - amount);
            return true;
        }
        return false;
    }

    /** 当前可用总能量 = 枪内能量 + 背罐气体可换算的能量。 */
    public static float getAvailableEnergy(@Nullable Player player, ItemStack stack) {
        float total = getEnergy(stack);
        if (player != null) {
            for (ItemStack tank : BacktankUtil.getAllWithAir(player)) {
                total += Math.max(0.0f, BacktankUtil.getAir(tank)) / AIR_TO_ENERGY_RATIO;
            }
        }
        return total;
    }

    /**
     * 消耗能量（方案 A：优先扣背罐气体，不足部分再扣枪内能量）。
     *
     * 换算：1 秒气体 = 1 点能量。
     * 穿戴背罐时，消耗会直接从背罐气体中扣除，枪内能量条保持不动；
     * 背罐气不足或未穿戴时，才回退到扣枪内能量。
     *
     * @return 是否成功扣除（两者合计足够）
     */
    public static boolean consumeEnergyWithBacktank(@Nullable Player player, ItemStack stack, float amount) {
        if (amount <= 0) {
            return true;
        }
        float remaining = amount;

        // 1. 优先从背罐气体中扣
        if (player != null && !player.level().isClientSide) {
            for (ItemStack tank : BacktankUtil.getAllWithAir(player)) {
                float air = BacktankUtil.getAir(tank);
                if (air <= 0) continue;

                float needAir = remaining * AIR_TO_ENERGY_RATIO;
                float usedAir = Math.min(air, needAir);
                BacktankUtil.consumeAir(player, tank, usedAir);

                remaining -= usedAir / AIR_TO_ENERGY_RATIO;
                if (remaining <= 1.0E-4f) {
                    return true;
                }
            }
        }

        // 2. 剩余部分从枪内能量扣
        return consumeEnergy(stack, remaining);
    }

    public static Mode getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        int idx = (tag != null && tag.contains(TAG_MODE)) ? tag.getInt(TAG_MODE) : -1;
        Mode[] values = Mode.values();
        Mode stored = (idx >= 0 && idx < values.length) ? values[idx] : null;

        // 装双钩模块：只允许 DUAL / QUICK 两种模式（默认 DUAL）
        if (hasDualHookModule(stack)) {
            return (stored == Mode.QUICK) ? Mode.QUICK : Mode.DUAL;
        }
        // 未装双钩模块：不允许停留在 DUAL/QUICK
        if (stored == null || stored == Mode.DUAL || stored == Mode.QUICK) {
            return Mode.PULL;
        }
        return stored;
    }

    /** 记住当前模式（安装双钩模块前调用），供卸下后恢复。 */
    public static void rememberPreDualMode(ItemStack stack) {
        if (hasDualHookModule(stack)) {
            return; // 已经是双钩状态，不覆盖
        }
        stack.getOrCreateTag().putInt(TAG_PRE_DUAL_MODE, getMode(stack).ordinal());
    }

    /** 卸下双钩模块后恢复此前记录的模式（无记录则回 PULL）。 */
    public static void restorePreDualMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_PRE_DUAL_MODE)) {
            int idx = tag.getInt(TAG_PRE_DUAL_MODE);
            Mode[] values = Mode.values();
            if (idx >= 0 && idx < values.length && values[idx] != Mode.DUAL) {
                stack.getOrCreateTag().putInt(TAG_MODE, idx);
                return;
            }
        }
        stack.getOrCreateTag().putInt(TAG_MODE, Mode.PULL.ordinal());
    }

    public static void setMode(ItemStack stack, Mode mode) {
        stack.getOrCreateTag().putInt(TAG_MODE, mode.ordinal());
    }

    public static MiningMode getMiningMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_MINING_MODE)) {
            int idx = tag.getInt(TAG_MINING_MODE);
            MiningMode[] values = MiningMode.values();
            if (idx >= 0 && idx < values.length) {
                return values[idx];
            }
        }
        return MiningMode.OFF;
    }

    public static void setMiningMode(ItemStack stack, MiningMode mode) {
        stack.getOrCreateTag().putInt(TAG_MINING_MODE, mode.ordinal());
    }

    public static ItemStackHandler getUpgradesHandler(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(6);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_UPGRADES)) {
            handler.deserializeNBT(tag.getCompound(TAG_UPGRADES));
        }
        return handler;
    }

    public static void saveUpgradesHandler(ItemStack stack, ItemStackHandler handler) {
        stack.getOrCreateTag().put(TAG_UPGRADES, handler.serializeNBT());
    }

    /** 存储仓格数。 */
    public static final int STORAGE_SLOTS = 8;

    public static ItemStackHandler getStorageHandler(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(STORAGE_SLOTS);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_STORAGE)) {
            // 兼容旧存档（可能存了 16 格）：先按原尺寸反序列化，再截取前 8 格
            CompoundTag storage = tag.getCompound(TAG_STORAGE);
            ItemStackHandler legacy = new ItemStackHandler(16);
            legacy.deserializeNBT(storage);
            for (int i = 0; i < STORAGE_SLOTS; i++) {
                handler.setStackInSlot(i, legacy.getStackInSlot(i));
            }
        }
        return handler;
    }

    /**
     * 从存储仓中取出一枚双钩钩爪放入钩爪槽（自动补充）。
     * 仅在装双钩模块、钩爪槽为空、存储仓有双钩物品时生效。
     */
    public static void tryAutoRefillDualHook(ItemStack gun) {
        if (!hasDualHookModule(gun)) return;

        ItemStackHandler upgrades = getUpgradesHandler(gun);
        if (!upgrades.getStackInSlot(0).isEmpty()) return;

        ItemStackHandler storage = getStorageHandler(gun);
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            ItemStack s = storage.getStackInSlot(i);
            if (s.getItem() instanceof com.fib.fib.init.item.custom.hook.DualHookItem) {
                upgrades.setStackInSlot(0, s.copyWithCount(1));
                storage.extractItem(i, 1, false);
                saveUpgradesHandler(gun, upgrades);
                saveStorageHandler(gun, storage);
                setLoadedHooks(gun, MAX_DUAL_HOOKS);
                return;
            }
        }
    }

    public static void saveStorageHandler(ItemStack stack, ItemStackHandler handler) {
        stack.getOrCreateTag().put(TAG_STORAGE, handler.serializeNBT());
    }

    // ==================== 升级槽位检测工具 ====================

    public static IHookMaterial getHookMaterial(ItemStack stack) {
        ItemStackHandler upgrades = getUpgradesHandler(stack);
        ItemStack hookStack = upgrades.getStackInSlot(0);
        if (hookStack.getItem() instanceof IHookMaterial material) {
            return material;
        }
        // 默认基础材质
        return new IHookMaterial() {
            @Override
            public float getDamage(boolean inWater) {
                return 4.0f;
            }

            @Override
            public boolean canHookBlocks() {
                return true;
            }

            @Override
            public boolean canHookHardBlocks() {
                return false;
            }

            @Override
            public boolean hasWaterResistance() {
                return false;
            }
        };
    }

    public static boolean hasPressurizedModule(ItemStack stack) {
        ItemStackHandler upgrades = getUpgradesHandler(stack);
        return upgrades.getStackInSlot(1).getItem() instanceof PressurizedModuleItem
                || upgrades.getStackInSlot(2).getItem() instanceof PressurizedModuleItem;
    }

    public static boolean hasSmartUpgradeModule(ItemStack stack) {
        ItemStackHandler upgrades = getUpgradesHandler(stack);
        return upgrades.getStackInSlot(3).getItem() instanceof SmartUpgradeModuleItem;
    }

    public static boolean hasDualHookModule(ItemStack stack) {
        ItemStackHandler upgrades = getUpgradesHandler(stack);
        return upgrades.getStackInSlot(4).getItem() instanceof DualHookModuleItem;
    }

    /**
     * 校验双钩依赖：若槽位 0 是双头钩爪但槽位 4 已无双钩模块，
     * 则把双头钩爪弹出（返回该物品，由调用方还给玩家），并重置装填计数。
     * 返回被弹出的物品（无则 EMPTY）。
     */
    public static ItemStack validateDualHookDependency(ItemStack stack) {
        ItemStackHandler upgrades = getUpgradesHandler(stack);
        ItemStack slot0 = upgrades.getStackInSlot(0);
        if (slot0.isEmpty()) {
            return ItemStack.EMPTY;
        }

        boolean dualModule = hasDualHookModule(stack);
        boolean dualItem = slot0.getItem() instanceof com.fib.fib.init.item.custom.hook.DualHookItem;

        // 装了双钩模块：钩爪槽只能是双头钩爪，普通钩爪弹出
        // 未装双钩模块：钩爪槽不能是双头钩爪，弹出
        if (dualModule == dualItem) {
            return ItemStack.EMPTY;
        }

        upgrades.setStackInSlot(0, ItemStack.EMPTY);
        saveUpgradesHandler(stack, upgrades);
        stack.getOrCreateTag().putInt(TAG_LOADED_HOOKS, 0);
        return slot0.copy();
    }

    public static boolean hasItemProtectionModule(ItemStack stack) {
        ItemStackHandler upgrades = getUpgradesHandler(stack);
        return upgrades.getStackInSlot(4).getItem() instanceof ItemProtectionModuleItem;
    }

    /**
     * 挖掘模块是否「实际生效」。
     * 规则：双钩模块激活时，挖掘/抓取等钩爪模块一律不发挥作用（设计文档 §7.4）。
     */
    public static boolean isMiningModuleEffective(ItemStack stack) {
        return hasMiningModule(stack) && !hasDualHookModule(stack);
    }

    /**
     * 抓取模块是否「实际生效」。
     * 规则同上：双钩模块激活时一律不生效。
     */
    public static boolean isGrappleModuleEffective(ItemStack stack) {
        return hasGrappleModule(stack) && !hasDualHookModule(stack);
    }

    public static boolean hasMiningModule(ItemStack stack) {
        ItemStackHandler upgrades = getUpgradesHandler(stack);
        return upgrades.getStackInSlot(5).getItem() instanceof MiningModuleItem;
    }

    public static boolean hasGrappleModule(ItemStack stack) {
        ItemStackHandler upgrades = getUpgradesHandler(stack);
        return upgrades.getStackInSlot(5).getItem() instanceof com.fib.fib.init.item.custom.hook.GrappleModuleItem;
    }

    public static final String TAG_GRAPPLE_MODE = "GrappleMode";
    /** 装双钩模块前的模式（卸下后恢复）。 */
    public static final String TAG_PRE_DUAL_MODE = "fib_pre_dual_mode";
    public static final String TAG_LOADED_HOOKS = "fib_loaded_hooks";
    /** 钩爪槽（槽位 0）中是否装有钩爪。 */
    public static boolean hasHookLoaded(ItemStack stack) {
        return !getUpgradesHandler(stack).getStackInSlot(0).isEmpty();
    }

    /** 钩爪槽中装的是否为双钩物品。 */
    public static boolean hasDualHookItem(ItemStack stack) {
        return getUpgradesHandler(stack).getStackInSlot(0).getItem()
                instanceof com.fib.fib.init.item.custom.hook.DualHookItem;
    }

    /** 清空钩爪槽（双钩用尽时消耗）。 */
    public static void clearHookSlot(ItemStack stack) {
        ItemStackHandler handler = getUpgradesHandler(stack);
        handler.setStackInSlot(0, ItemStack.EMPTY);
        saveUpgradesHandler(stack, handler);
    }
    /** 双钩模块下最多装填的钩爪数。 */
    public static final int MAX_DUAL_HOOKS = 2;

    /** 当前已装填的钩爪数；非双钩模块返回 -1（不限）。 */
    public static int getLoadedHooks(ItemStack stack) {
        if (!hasDualHookModule(stack)) {
            return -1;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_LOADED_HOOKS)) {
            return tag.getInt(TAG_LOADED_HOOKS);
        }
        // 首次装填：有双钩物品则视为满装
        return hasDualHookItem(stack) ? MAX_DUAL_HOOKS : 0;
    }

    public static void setLoadedHooks(ItemStack stack, int n) {
        stack.getOrCreateTag().putInt(TAG_LOADED_HOOKS, Math.max(0, Math.min(MAX_DUAL_HOOKS, n)));
    }

    public static GrappleMode getGrappleMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_GRAPPLE_MODE)) {
            int idx = tag.getInt(TAG_GRAPPLE_MODE);
            GrappleMode[] values = GrappleMode.values();
            if (idx >= 0 && idx < values.length) {
                return values[idx];
            }
        }
        return GrappleMode.STABLE;
    }

    public static void setGrappleMode(ItemStack stack, GrappleMode mode) {
        stack.getOrCreateTag().putInt(TAG_GRAPPLE_MODE, mode.ordinal());
    }

    public static void cycleGrappleMode(ItemStack stack, Player player) {
        if (!isGrappleModuleEffective(stack)) {
            // 未安装抓取模块，或双钩模块激活导致抓取不生效：静默忽略。
            return;
        }
        GrappleMode current = getGrappleMode(stack);
        GrappleMode next = (current == GrappleMode.STABLE) ? GrappleMode.PULL_TOWARDS : GrappleMode.STABLE;
        setGrappleMode(stack, next);
        player.displayClientMessage(Component.translatable("fib_mod.hook.grapple_mode_switched", Component.translatable(next.getTranslationKey())).withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    // ==================== 模式切换 ====================

    public static void cycleMode(ItemStack stack, Player player) {
        // 双钩模块激活时：在 DUAL 与 QUICK 之间切换（无需智能升级模块）
        if (hasDualHookModule(stack)) {
            Mode cur = getMode(stack);
            Mode nxt = (cur == Mode.QUICK) ? Mode.DUAL : Mode.QUICK;
            setMode(stack, nxt);
            player.displayClientMessage(
                    Component.translatable("fib_mod.hook.mode_switched",
                            Component.translatable(nxt.getTranslationKey())).withStyle(ChatFormatting.GOLD), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 0.6f, 1.5f);
            return;
        }
        if (!hasSmartUpgradeModule(stack)) {
            // 未安装智能升级模块：静默忽略，不提示。
            return;
        }
        Mode current = getMode(stack);
        Mode next = switch (current) {
            case PULL -> Mode.CLIMB;
            case CLIMB -> Mode.PICKUP;
            case PICKUP -> Mode.PULL;
            case DUAL, QUICK -> Mode.PULL; // 双钩系模式不会走到这里，占位保证穷尽
        };
        setMode(stack, next);
        player.displayClientMessage(Component.translatable("fib_mod.hook.mode_switched", Component.translatable(next.getTranslationKey())).withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 0.6f, 1.5f);
    }

    public static void cycleMiningMode(ItemStack stack, Player player) {
        if (!isMiningModuleEffective(stack)) {
            // 未安装挖掘模块，或双钩模块激活导致挖掘不生效：静默忽略。
            return;
        }
        MiningMode current = getMiningMode(stack);
        MiningMode next = switch (current) {
            case OFF -> MiningMode.MINE_ALL;
            case MINE_ALL -> MiningMode.MINE_BREAKABLE;
            case MINE_BREAKABLE -> MiningMode.OFF;
        };
        setMiningMode(stack, next);
        player.displayClientMessage(Component.translatable("fib_mod.hook.mining_mode_switched", Component.translatable(next.getTranslationKey())).withStyle(ChatFormatting.AQUA), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.8f, 1.8f);
    }

    // ==================== 右键使用与射击逻辑 ====================

    /**
     * 收集玩家当前的活动钩爪。
     * 排除 RETRACTING（正在飞回）与 DROPPING（已脱落/落地）：
     * 它们不应阻止新发射，也不参与双钩连接判定。
     */
    private static List<GrapplingHookEntity> collectActiveHooks(Level level, Player player) {
        return level.getEntitiesOfClass(
                GrapplingHookEntity.class,
                player.getBoundingBox().inflate(256.0),
                hook -> hook.getShooter() == player
                        && !hook.isRemoved()
                        && hook.getHookState() != GrapplingHookEntity.State.RETRACTING
                        && hook.getHookState() != GrapplingHookEntity.State.DROPPING
        );
    }

    /**
     * 潜行 + 右键：回收钩爪。
     *
     * 规则：
     *  - 快速模式：回收全部快速钩爪；
     *  - 其它模式：额外清理已落地（DROPPING）的钩爪；
     *  - 非双钩模块：已成对连接的双钩是独立部署装备，不参与回收；
     *  - 双钩模块：需靠近（1.5 格内）且对准钩爪或绳段才允许回收；
     *  - 多枚钩爪时，准星对准哪一枚就只回收哪一枚。
     */
    private InteractionResultHolder<ItemStack> handleSneakRightClick(Level level, Player player,
                                                                     ItemStack stack,
                                                                     List<GrapplingHookEntity> activeHooks) {
        // 1. 快速模式：回收全部快速钩爪
        if (getMode(stack) == Mode.QUICK) {
            if (!level.isClientSide) {
                for (GrapplingHookEntity h : activeHooks) {
                    if (h.isQuickMode()) {
                        h.setRetracting();
                    }
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // 2. 顺带清理已落地（DROPPING）的钩爪
        if (!level.isClientSide) {
            List<GrapplingHookEntity> droppedHooks = level.getEntitiesOfClass(
                    GrapplingHookEntity.class,
                    player.getBoundingBox().inflate(16.0),
                    h -> h.getShooter() == player
                            && !h.isRemoved()
                            && h.getHookState() == GrapplingHookEntity.State.DROPPING);
            for (GrapplingHookEntity d : droppedHooks) {
                d.discard();
            }
        }

        // 3. 非双钩模块：排除已成对的双钩
        if (!hasDualHookModule(stack)) {
            activeHooks.removeIf(GrapplingHookEntity::isPaired);
        }

        if (activeHooks.isEmpty()) {
            // 空闲状态下的潜行右键不再打开界面（改装界面改由 H 键打开）
            return InteractionResultHolder.pass(stack);
        }

        // 4. 双钩模块：需靠近 + 对准
        if (hasDualHookModule(stack)) {
            boolean nearAny = false;
            for (GrapplingHookEntity h : activeHooks) {
                if (h.position().distanceTo(player.position()) <= RETRACT_RANGE) {
                    nearAny = true;
                    break;
                }
            }
            if (!nearAny || !aimsAtAnyHookOrRope(player, activeHooks)) {
                return InteractionResultHolder.pass(stack);
            }
        }

        // 5. 多枚钩爪时，准星对准的那一枚优先
        GrapplingHookEntity targetedHook = findAimedHook(player, activeHooks);
        if (targetedHook != null) {
            if (!level.isClientSide) {
                targetedHook.setRetracting();
                targetedHook.clearPartnerHook();
                for (GrapplingHookEntity hook : activeHooks) {
                    if (hook != targetedHook) {
                        hook.clearPartnerHook();
                    }
                }
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.HOOK_RETRACT.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
            player.displayClientMessage(
                    Component.translatable("fib_mod.hook.retracted_targeted")
                            .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // 6. 全部回收
        if (!level.isClientSide) {
            for (GrapplingHookEntity hook : activeHooks) {
                hook.setRetracting();
                hook.clearPartnerHook();
            }
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.HOOK_RETRACT.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * 快速模式右键：
     *  1. 没有钩爪 -> 发射第 1 枚；
     *  2. 第 1 枚已命中（牵引/悬停）-> 发射第 2 枚；
     *  3. 第 1 枚仍在飞行 -> 不允许发射第 2 枚。
     */
    private InteractionResultHolder<ItemStack> handleQuickMode(Level level, Player player,
                                                              ItemStack stack, InteractionHand hand,
                                                              List<GrapplingHookEntity> activeHooks) {
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        // 找出该玩家已存在的快速模式钩爪
        List<GrapplingHookEntity> quickHooks = new java.util.ArrayList<>();
        for (GrapplingHookEntity h : activeHooks) {
            if (h.isQuickMode()) quickHooks.add(h);
        }

        // 1. 没有钩爪：发射第 1 枚
        if (quickHooks.isEmpty()) {
            if (shootHook(level, player, stack, hand, null, true, false)) {
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
            return InteractionResultHolder.fail(stack);
        }

        // 2. 第 1 枚已命中 -> 发射第 2 枚
        boolean firstHooked = false;
        for (GrapplingHookEntity h : quickHooks) {
            if (h.isQuickFirstHooked()) {
                firstHooked = true;
                break;
            }
        }
        if (firstHooked) {
            if (shootHook(level, player, stack, hand, null, true, true)) {
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
            return InteractionResultHolder.fail(stack);
        }

        // 3. 第 1 枚仍在飞行：不允许发射第 2 枚
        return InteractionResultHolder.pass(stack);
    }

    /**
     * 双钩模式右键：
     *  1. 还有装填 -> 发射下一枚（与未配对的钩爪建立拴绳连接），发完即消耗槽位；
     *  2. 已用尽 -> 靠近（1.5 格内）且对准钩爪/绳段时回收全部，并尝试自动补充。
     */
    private InteractionResultHolder<ItemStack> handleDualMode(Level level, Player player,
                                                              ItemStack stack, InteractionHand hand,
                                                              List<GrapplingHookEntity> activeHooks) {
        int loaded = getLoadedHooks(stack);

        // 仅当槽位仍有双钩物品时才重置（否则说明已消耗完）
        if (loaded <= 0 && hasDualHookItem(stack)) {
            setLoadedHooks(stack, MAX_DUAL_HOOKS);
            loaded = MAX_DUAL_HOOKS;
        }

        // ---- 1. 还有钩爪未发射 ----
        if (loaded > 0) {
            // 选择「最近发射（id 最大）且尚未配对」的钩爪作为连接目标。
            // 不能取列表第一个：那会让新钩爪连到旧组的钩爪上，造成跨组串线。
            GrapplingHookEntity anchorHook = null;
            for (GrapplingHookEntity h : activeHooks) {
                if (h.getPartnerHook() != null) continue;
                if (anchorHook == null || h.getId() > anchorHook.getId()) {
                    anchorHook = h;
                }
            }

            if (!shootHook(level, player, stack, hand, anchorHook)) {
                return InteractionResultHolder.fail(stack);
            }
            if (!level.isClientSide) {
                setLoadedHooks(stack, loaded - 1);
                if (loaded - 1 <= 0) {
                    clearHookSlot(stack);   // 发完即消耗
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // ---- 2. 钩爪已用尽：需靠近 + 对准 ----
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        boolean nearAny = false;
        for (GrapplingHookEntity h : activeHooks) {
            if (h.position().distanceTo(player.position()) <= RETRACT_RANGE) {
                nearAny = true;
                break;
            }
        }
        if (!nearAny || !aimsAtAnyHookOrRope(player, activeHooks)) {
            return InteractionResultHolder.pass(stack);
        }

        for (GrapplingHookEntity hook : activeHooks) {
            hook.setRetracting();
            hook.clearPartnerHook();
        }
        // 两枚钩爪都已发射：消耗钩爪槽中的双钩物品，并尝试自动补充
        clearHookSlot(stack);
        tryAutoRefillDualHook(stack);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.HOOK_RETRACT.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** 回收钩爪所需的最近距离（格）。 */
    private static final double RETRACT_RANGE = 1.5;

    /** 在多个钩爪中，找出准星最对准的那一枚（无命中返回 null）。 */
    @Nullable
    private static GrapplingHookEntity findAimedHook(Player player, List<GrapplingHookEntity> hooks) {
        if (hooks.size() < 2) {
            return null;
        }
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F).normalize();
        double maxDot = 0.75;
        GrapplingHookEntity best = null;
        for (GrapplingHookEntity hook : hooks) {
            Vec3 hookPos = hook.isHooked() ? hook.getAnchorPos() : hook.position();
            Vec3 toHook = hookPos.subtract(eyePos);
            if (toHook.lengthSqr() > 0.01) {
                double dot = lookVec.dot(toHook.normalize());
                if (dot > maxDot) {
                    maxDot = dot;
                    best = hook;
                }
            }
        }
        return best;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // ① 依赖校验：卸下双钩模块时，残留的双头钩爪弹出
        if (!level.isClientSide) {
            ItemStack ejected = validateDualHookDependency(stack);
            if (!ejected.isEmpty()) {
                if (!player.getInventory().add(ejected)) {
                    player.drop(ejected, false);
                }
            }
        }

        // ② 收集活动钩爪（不含收回中/下落中）
        List<GrapplingHookEntity> activeHooks = collectActiveHooks(level, player);

        // ③ 潜行 + 右键
        if (player.isShiftKeyDown()) {
            return handleSneakRightClick(level, player, stack, activeHooks);
        }

        // ===== 快速模式：独立流程（优先于其它所有交互） =====
        if (getMode(stack) == Mode.QUICK) {
            return handleQuickMode(level, player, stack, hand, activeHooks);
        }

        // 非双钩模式下：已成对连接的双钩属于“已部署装备”，
        // 不应阻塞单钩的发射与交互（它们只受两钩间拴绳约束）。
        if (!(hasDualHookModule(stack) && hasDualHookItem(stack))) {
            activeHooks.removeIf(GrapplingHookEntity::isPaired);
        }

        // 2. 普通右键：活动钩爪交互
        if (!activeHooks.isEmpty()) {
            // ===== 双钩模块：发射 → 发射第二枚 → 用尽后右键回收 =====
            if (hasDualHookModule(stack) && hasDualHookItem(stack)) {
                return handleDualMode(level, player, stack, hand, activeHooks);
            }

            // 针对已存在的钩爪进行交互（拉取 / 回收）
            return handleNormalInteraction(level, player, stack, activeHooks);
        }

        // 3. 空闲发射新钩爪
        return handleIdleFire(level, player, stack, hand);
    }

    /**
     * 普通模式（PULL / CLIMB / PICKUP）下对已有钩爪的交互。
     * <p>
     * - PICKUP：对钩住的生物/掉落物执行牵引；方块目标只回收不牵引；
     * - PULL：方块/生物/掉落物一律牵引；
     * - CLIMB：只对生物/掉落物执行牵引。
     */
    private InteractionResultHolder<ItemStack> handleNormalInteraction(Level level, Player player,
                                                                       ItemStack stack,
                                                                       List<GrapplingHookEntity> activeHooks) {
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        // 已成对连接的双钩属于独立的“已部署装备”，不参与普通钩爪交互
        List<GrapplingHookEntity> normalHooks = new java.util.ArrayList<>();
        for (GrapplingHookEntity h : activeHooks) {
            if (h.getPartnerHook() == null) {
                normalHooks.add(h);
            }
        }
        if (normalHooks.isEmpty()) {
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        Mode mode = getMode(stack);
        if (mode == Mode.PICKUP) {
            applyPickupInteraction(player, normalHooks);
        } else {
            applyPullOrClimbInteraction(mode, normalHooks);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /**
     * 拾取模式：对生物/掉落物执行牵引（把目标拉向玩家），方块目标只回收不牵引。
     */
    private void applyPickupInteraction(Player player, List<GrapplingHookEntity> normalHooks) {
        for (GrapplingHookEntity hook : normalHooks) {
            if (!hook.isHooked()) continue;
            GrapplingHookEntity.TargetType t = hook.getTargetType();
            if (t == GrapplingHookEntity.TargetType.LIVING
                    || t == GrapplingHookEntity.TargetType.ITEM) {
                hook.startPulling();
            }
        }

        // 方块目标：只回收不牵引；多枚时优先回收准星对准的那一枚
        GrapplingHookEntity targetedHook = findAimedHook(player, normalHooks);
        if (targetedHook != null) {
            targetedHook.setRetracting();
            targetedHook.clearPartnerHook();
            player.displayClientMessage(
                    Component.translatable("fib_mod.hook.retracted_targeted")
                            .withStyle(ChatFormatting.YELLOW), true);
        } else {
            for (GrapplingHookEntity hook : normalHooks) {
                hook.setRetracting();
                hook.clearPartnerHook();
            }
        }
    }

    /**
     * 牵引/攀岩模式：PULL 对所有目标牵引，CLIMB 仅对生物/掉落物牵引。
     */
    private void applyPullOrClimbInteraction(Mode mode, List<GrapplingHookEntity> normalHooks) {
        for (GrapplingHookEntity hook : normalHooks) {
            if (!hook.isHooked()) continue;
            if (mode == Mode.PULL) {
                hook.startPulling();
            } else {
                GrapplingHookEntity.TargetType t = hook.getTargetType();
                if (t == GrapplingHookEntity.TargetType.LIVING
                        || t == GrapplingHookEntity.TargetType.ITEM) {
                    hook.startPulling();
                }
            }
        }
    }

    /**
     * 空闲状态右键：尝试补充双钩 -> 检查装填 -> 发射。
     */
    private InteractionResultHolder<ItemStack> handleIdleFire(Level level, Player player,
                                                              ItemStack stack, InteractionHand hand) {
        if (!level.isClientSide) {
            tryAutoRefillDualHook(stack);
        }
        if (!level.isClientSide && !hasHookLoaded(stack)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.2f);
            return InteractionResultHolder.fail(stack);
        }

        if (!shootHook(level, player, stack, hand, null)) {
            return InteractionResultHolder.fail(stack);
        }

        // 仅双钩物品会随发射消耗；普通钩爪不消耗
        if (!level.isClientSide && hasDualHookItem(stack) && hasDualHookModule(stack)) {
            int loaded = getLoadedHooks(stack);
            if (loaded <= 0) {
                loaded = MAX_DUAL_HOOKS;
            }
            setLoadedHooks(stack, Math.max(0, loaded - 1));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * 判断玩家准星是否对准了任一钩爪本体，或两钩之间的连接绳段。
     * 用于双钩模式下「对准才能回收」的判定。
     */
    private static boolean aimsAtAnyHookOrRope(Player player, List<GrapplingHookEntity> hooks) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        final double maxAngleCos = Math.cos(Math.toRadians(12.0)); // 约 12° 容差

        for (GrapplingHookEntity hook : hooks) {
            // 1. 对准钩爪本体
            Vec3 toHook = hook.position().subtract(eye);
            double len = toHook.length();
            if (len < 64.0 && len > 0.01 && look.dot(toHook.scale(1.0 / len)) >= maxAngleCos) {
                return true;
            }

            // 2. 对准两钩之间的绳段
            GrapplingHookEntity partner = hook.getPartnerHook();
            if (partner != null && !partner.isRemoved() && hook.getId() < partner.getId()) {
                if (rayNearSegment(eye, look, hook.position(), partner.position(), 0.75)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 射线与线段最近距离是否小于 tolerance（用于判定是否瞄准绳索）。 */
    private static boolean rayNearSegment(Vec3 origin, Vec3 dir, Vec3 a, Vec3 b, double tolerance) {
        Vec3 seg = b.subtract(a);
        double segLen2 = seg.lengthSqr();
        if (segLen2 < 1e-6) return false;

        // 采样若干点近似判定（简单可靠）
        final int samples = 24;
        for (int i = 0; i <= samples; i++) {
            Vec3 p = a.add(seg.scale((double) i / samples));
            Vec3 toP = p.subtract(origin);
            double t = toP.dot(dir);
            if (t <= 0 || t > 64.0) continue;
            Vec3 closest = origin.add(dir.scale(t));
            if (closest.distanceToSqr(p) <= tolerance * tolerance) {
                return true;
            }
        }
        return false;
    }

    /** 对外暴露的发射入口（供网络包调用）。 */
    public boolean fireHook(Player player, ItemStack stack, InteractionHand hand) {
        return shootHook(player.level(), player, stack, hand, null);
    }

    private boolean shootHook(Level level, Player player, ItemStack stack, InteractionHand hand, @Nullable GrapplingHookEntity partnerHook) {
        return shootHook(level, player, stack, hand, partnerHook, false, false);
    }

    /**
     * 发射钩爪。
     * @param quick        是否为快速模式发射
     * @param quickSecond  是否为快速模式的第二枚
     */
    private boolean shootHook(Level level, Player player, ItemStack stack, InteractionHand hand,
                              @Nullable GrapplingHookEntity partnerHook,
                              boolean quick, boolean quickSecond) {
        boolean pressurized = hasPressurizedModule(stack);
        boolean dualHook = hasDualHookModule(stack);

        float energyCost = 0.10f; // 调为现在的十分之一（原 1.0f）
        if (pressurized) energyCost *= 2.0f;
        // 快速模式不额外加价（仅普通发射按双钩模块加成）
        if (dualHook && !quick) energyCost *= 1.5f;

        // 能量检查：枪内 + 背罐气合计（方案 A：优先消耗背罐气体）
        if (getAvailableEnergy(player, stack) < energyCost) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.2f);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("fib_mod.hook.no_energy").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        // 消耗能量与耐久
        if (!player.getAbilities().instabuild) {
            consumeEnergyWithBacktank(player, stack, energyCost);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }

        if (!level.isClientSide) {
            GrapplingHookEntity hook = new GrapplingHookEntity(level, player, stack);
            float speed = pressurized ? 1.8f : 1.2f;
            hook.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, speed, 0.1f);
            hook.setQuickMode(quick, quickSecond);
            level.addFreshEntity(hook);
            if (partnerHook != null && !partnerHook.isRemoved()) {
                hook.setPartnerHook(partnerHook);
                partnerHook.setPartnerHook(hook);
            }
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HOOK_SHOOT.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        player.getCooldowns().addCooldown(this, 5); // 0.25s 冷却
        return true;
    }

    // ==================== 右键方块（工程师工作台与背罐手动充能） ====================

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        // 1. 右键工程师工作台打开升级界面
        if (level.getBlockState(pos).is(ModBlocks.ENGINEER_WORKBENCH.get())) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                openUpgradeMenu(serverPlayer, stack, context.getHand());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // 2. 右键放置的机械动力背罐方块进行手动充能
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BacktankBlockEntity backtankBE) {
            float missing = MAX_ENERGY - getEnergy(stack);
            if (missing > 0.01f) {
                int missingEnergy = (int) Math.ceil(missing);
                int airAvailable = backtankBE.getAirLevel();
                int canCharge = Math.min(missingEnergy, (int) (airAvailable / AIR_TO_ENERGY_RATIO));
                if (canCharge > 0) {
                    if (!level.isClientSide) {
                        backtankBE.setAirLevel((int) (airAvailable - canCharge * AIR_TO_ENERGY_RATIO));
                        setEnergy(stack, getEnergy(stack) + canCharge);
                        if (player != null) {
                            player.displayClientMessage(Component.translatable("fib_mod.hook.charged", canCharge).withStyle(ChatFormatting.GREEN), true);
                        }
                    }
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1.6f);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }

        return super.useOn(context);
    }

    // ==================== 背包 Tick（背罐自动充能与切换物品收回） ====================

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) return;

        // 仅在主手持有时处理充能与检测
        boolean isMainHand = player.getMainHandItem() == stack;
        if (!isMainHand) return;

        // 背罐自动充能：穿戴背罐时每秒转换 1s 气体为 1 点能量
        if (player.tickCount % 20 == 0) {
            float currentEnergy = getEnergy(stack);
            if (currentEnergy < MAX_ENERGY) {
                List<ItemStack> backtanks = BacktankUtil.getAllWithAir(player);
                for (ItemStack backtank : backtanks) {
                    float air = BacktankUtil.getAir(backtank);
                    if (air >= AIR_TO_ENERGY_RATIO) {
                        BacktankUtil.consumeAir(player, backtank, AIR_TO_ENERGY_RATIO);
                        setEnergy(stack, currentEnergy + 1.0f);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.2f, 2.0f);
                        break;
                    }
                }
            }
        }
    }

    // ==================== 打开升级与存储界面 ====================

    public static void openUpgradeMenu(ServerPlayer player, ItemStack stack, InteractionHand hand) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new GrapplingHookMenu(containerId, playerInventory, stack, hand),
                Component.translatable("fib_mod.gui.grappling_hook.title")
        ), buf -> {
            buf.writeItem(stack);
            buf.writeBoolean(hand == InteractionHand.MAIN_HAND);
        });
    }

    // ==================== 物品信息提示 ====================

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        float energy = getEnergy(stack);
        tooltip.add(Component.translatable("fib_mod.hook.energy_tooltip", String.format("%.1f", energy), (int) MAX_ENERGY).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("fib_mod.hook.mode_tooltip", Component.translatable(getMode(stack).getTranslationKey())).withStyle(ChatFormatting.GOLD));

        if (hasMiningModule(stack)) {
            tooltip.add(Component.translatable("fib_mod.hook.mining_tooltip", Component.translatable(getMiningMode(stack).getTranslationKey())).withStyle(ChatFormatting.YELLOW));
        }

        if (hasGrappleModule(stack)) {
            tooltip.add(Component.translatable("fib_mod.hook.grapple_tooltip", Component.translatable(getGrappleMode(stack).getTranslationKey())).withStyle(ChatFormatting.GOLD));
        }

        ItemStackHandler upgrades = getUpgradesHandler(stack);
        tooltip.add(Component.translatable("fib_mod.hook.upgrades_header").withStyle(ChatFormatting.GRAY));
        String[] slotNames = {"hook", "module_1", "module_2", "function", "improvement", "hook_module"};
        for (int i = 0; i < 6; i++) {
            ItemStack up = upgrades.getStackInSlot(i);
            if (!up.isEmpty()) {
                tooltip.add(Component.literal(" - ").append(up.getHoverName()).withStyle(ChatFormatting.DARK_GREEN));
            }
        }

        super.appendHoverText(stack, level, tooltip, isAdvanced);
    }
}
