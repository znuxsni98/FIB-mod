package com.fib.fib.init.item.custom;

import com.fib.fib.gui.container.menu.GrapplingHookMenu;
import com.fib.fib.init.block.ModBlocks;
import com.fib.fib.init.entity.custom.GrapplingHookEntity;
import com.fib.fib.init.item.ModItems;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
    public static final float AIR_TO_ENERGY_RATIO = 15.0f; // 15秒背罐气体 = 1点能量

    public enum Mode {
        PULL("fib_mod.hook_mode.pull"),
        CLIMB("fib_mod.hook_mode.climb"),
        PICKUP("fib_mod.hook_mode.pickup");

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

    public static Mode getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_MODE)) {
            int idx = tag.getInt(TAG_MODE);
            Mode[] values = Mode.values();
            if (idx >= 0 && idx < values.length) {
                return values[idx];
            }
        }
        return Mode.PULL;
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

    public static ItemStackHandler getStorageHandler(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(16);
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_STORAGE)) {
            handler.deserializeNBT(tag.getCompound(TAG_STORAGE));
        }
        return handler;
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

    public static boolean hasItemProtectionModule(ItemStack stack) {
        ItemStackHandler upgrades = getUpgradesHandler(stack);
        return upgrades.getStackInSlot(4).getItem() instanceof ItemProtectionModuleItem;
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
        if (!hasGrappleModule(stack)) {
            player.displayClientMessage(Component.translatable("fib_mod.hook.need_grapple_module").withStyle(ChatFormatting.RED), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.2f);
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
        if (!hasSmartUpgradeModule(stack)) {
            player.displayClientMessage(Component.translatable("fib_mod.hook.need_smart_module").withStyle(ChatFormatting.RED), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.2f);
            return;
        }
        Mode current = getMode(stack);
        Mode next = switch (current) {
            case PULL -> Mode.CLIMB;
            case CLIMB -> Mode.PICKUP;
            case PICKUP -> Mode.PULL;
        };
        setMode(stack, next);
        player.displayClientMessage(Component.translatable("fib_mod.hook.mode_switched", Component.translatable(next.getTranslationKey())).withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 0.6f, 1.5f);
    }

    public static void cycleMiningMode(ItemStack stack, Player player) {
        if (hasDualHookModule(stack)) {
            player.displayClientMessage(Component.translatable("fib_mod.hook.dual_hook_blocks_mining").withStyle(ChatFormatting.RED), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.2f);
            return;
        }
        if (!hasMiningModule(stack)) {
            player.displayClientMessage(Component.translatable("fib_mod.hook.need_mining_module").withStyle(ChatFormatting.RED), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.2f);
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 获取玩家当前已发射的活动钩爪
        List<GrapplingHookEntity> activeHooks = level.getEntitiesOfClass(
                GrapplingHookEntity.class,
                player.getBoundingBox().inflate(64.0),
                hook -> hook.getShooter() == player && !hook.isRemoved()
        );

        // 1. 潜行 + 右键
        if (player.isShiftKeyDown()) {
            if (!activeHooks.isEmpty()) {
                // 如果钩爪处于活动中，潜行+右键触发回收
                // 双钩/多钩模式：若准星对准某一枚钩爪，则仅回收对准的那一根
                GrapplingHookEntity targetedHook = null;
                if (activeHooks.size() >= 2) {
                    Vec3 eyePos = player.getEyePosition();
                    Vec3 lookVec = player.getViewVector(1.0F).normalize();
                    double maxDot = 0.75;
                    for (GrapplingHookEntity hook : activeHooks) {
                        Vec3 hookPos = hook.isHooked() ? hook.getAnchorPos() : hook.position();
                        Vec3 toHook = hookPos.subtract(eyePos);
                        if (toHook.lengthSqr() > 0.01) {
                            double dot = lookVec.dot(toHook.normalize());
                            if (dot > maxDot) {
                                maxDot = dot;
                                targetedHook = hook;
                            }
                        }
                    }
                }

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
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HOOK_RETRACT.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
                    player.displayClientMessage(Component.translatable("fib_mod.hook.retracted_targeted").withStyle(ChatFormatting.YELLOW), true);
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                }

                if (!level.isClientSide) {
                    for (GrapplingHookEntity hook : activeHooks) {
                        hook.setRetracting();
                        hook.clearPartnerHook();
                    }
                }
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HOOK_RETRACT.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            } else {
                // 如果处于空闲状态，潜行+右键打开配置升级界面
                if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                    openUpgradeMenu(serverPlayer, stack, hand);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
        }

        // 2. 普通右键：活动钩爪交互
        if (!activeHooks.isEmpty()) {
            // 双钩模块检测：若主钩已命中且次钩未发射
            if (hasDualHookModule(stack) && activeHooks.size() == 1) {
                GrapplingHookEntity primaryHook = activeHooks.get(0);
                if (primaryHook.isHooked()) {
                    // 准星射线判定：若玩家精确指向一个未钩取的新方块目标，发射第2根钩爪；
                    // 若玩家指向空旷虚空、空气或朝向第1根钩爪方向，则直接触发单钩牵引拉取！
                    Vec3 eye = player.getEyePosition();
                    Vec3 look = player.getViewVector(1.0F);
                    double range = hasPressurizedModule(stack) ? 24.0 : 16.0;
                    Vec3 reach = eye.add(look.scale(range));
                    BlockHitResult bHit = level.clip(new ClipContext(eye, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

                    if (bHit.getType() == HitResult.Type.BLOCK && bHit.getLocation().distanceTo(primaryHook.getAnchorPos()) >= 1.5) {
                        if (shootHook(level, player, stack, hand, primaryHook)) {
                            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                        }
                        return InteractionResultHolder.fail(stack);
                    }
                    if (!level.isClientSide) {
                        primaryHook.startPulling();
                    }
                    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
                }
            }

            // 针对已存在的钩爪进行交互（拉取 / 回收）
            if (!level.isClientSide) {
                Mode mode = getMode(stack);
                if (mode == Mode.PICKUP) {
                    GrapplingHookEntity targetedHook = null;
                    if (activeHooks.size() >= 2) {
                        Vec3 eyePos = player.getEyePosition();
                        Vec3 lookVec = player.getViewVector(1.0F).normalize();
                        double maxDot = 0.75;
                        for (GrapplingHookEntity hook : activeHooks) {
                            Vec3 hookPos = hook.isHooked() ? hook.getAnchorPos() : hook.position();
                            Vec3 toHook = hookPos.subtract(eyePos);
                            if (toHook.lengthSqr() > 0.01) {
                                double dot = lookVec.dot(toHook.normalize());
                                if (dot > maxDot) {
                                    maxDot = dot;
                                    targetedHook = hook;
                                }
                            }
                        }
                    }

                    if (targetedHook != null) {
                        targetedHook.setRetracting();
                        targetedHook.clearPartnerHook();
                        for (GrapplingHookEntity hook : activeHooks) {
                            if (hook != targetedHook) {
                                hook.clearPartnerHook();
                            }
                        }
                        player.displayClientMessage(Component.translatable("fib_mod.hook.retracted_targeted").withStyle(ChatFormatting.YELLOW), true);
                    } else {
                        for (GrapplingHookEntity hook : activeHooks) {
                            hook.setRetracting();
                            hook.clearPartnerHook();
                        }
                    }
                } else {
                    for (GrapplingHookEntity hook : activeHooks) {
                        if (mode == Mode.PULL) {
                            if (hook.isHooked()) {
                                hook.startPulling();
                            }
                        } else if (mode == Mode.CLIMB) {
                            if (hook.isHooked()) {
                                hook.toggleAutoAscend();
                            }
                        }
                    }
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // 3. 空闲发射新钩爪
        if (shootHook(level, player, stack, hand, null)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return InteractionResultHolder.fail(stack);
    }

    private boolean shootHook(Level level, Player player, ItemStack stack, InteractionHand hand, @Nullable GrapplingHookEntity partnerHook) {
        boolean pressurized = hasPressurizedModule(stack);
        boolean dualHook = hasDualHookModule(stack);

        float energyCost = 0.10f; // 调为现在的十分之一（原 1.0f）
        if (pressurized) energyCost *= 2.0f;
        if (dualHook) energyCost *= 1.5f;

        if (getEnergy(stack) < energyCost) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8f, 1.2f);
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("fib_mod.hook.no_energy").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }

        // 消耗能量与耐久
        if (!player.getAbilities().instabuild) {
            consumeEnergy(stack, energyCost);
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        }

        if (!level.isClientSide) {
            GrapplingHookEntity hook = new GrapplingHookEntity(level, player, stack);
            float speed = pressurized ? 1.8f : 1.2f;
            hook.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, speed, 0.1f);
            if (partnerHook != null) {
                hook.setPartnerHook(partnerHook);
                partnerHook.setPartnerHook(hook);
            }
            level.addFreshEntity(hook);
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

        // 背罐自动充能：穿戴背罐时每秒转换 15s 气体为 1 点能量
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
