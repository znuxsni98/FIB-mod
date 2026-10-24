package com.fib.fib.init.entity.custom;

import com.fib.fib.init.entity.ModEntities;
import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import com.fib.fib.init.item.custom.hook.IHookMaterial;
import com.fib.fib.init.sound.ModSounds;
import com.fib.fib.network.ModMessages;
import com.fib.fib.network.packet.HookClimbPacket;
import com.fib.fib.network.packet.SyncPlayerMotionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 钩爪实体类。
 * 驱动状态机（飞行、锚定、牵引、攀爬、远程挖掘、收回与双钩拴绳），
 * 负责方块/生物碰撞检测、物理移动矢量同步及音效粒子表现。
 */
public class GrapplingHookEntity extends Projectile implements IEntityAdditionalSpawnData {

    public enum State {
        FLYING,
        HOOKED,
        PULLING,
        CLIMBING,
        MINING,
        RETRACTING,
        /** 附着方块消失后自由下落（像钓鱼浮标一样掉下去）。 */
        DROPPING
    }

    public enum TargetType {
        NONE,
        BLOCK,
        LIVING,
        ITEM
    }

    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_TYPE =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TARGET_ENTITY_ID =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ROPE_LENGTH =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_AUTO_ASCEND =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_PARTNER_HOOK_ID =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_HOOK_MATERIAL_ID =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ANCHOR_X =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ANCHOR_Y =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ANCHOR_Z =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_OWNER_ID =
            SynchedEntityData.defineId(GrapplingHookEntity.class, EntityDataSerializers.INT);

    private ItemStack gunStack = ItemStack.EMPTY;
    /** 发射时那把枪在服务端的唯一标识（用于区分多把钩爪枪）。 */
    private ItemStack sourceGun = ItemStack.EMPTY;
    /** 是否曾切离发射时的原枪（用于实现“切走再切回才收回”）。 */
    private boolean leftSourceGun = false;
    /** 发射时那把枪所在的物品栏槽位（主手=快捷栏索引，副手=40）。 */
    private int sourceSlot = -1;

    /** 取玩家主手/副手中的钩爪枪（主手优先）。 */
    @Nullable
    private static ItemStack getHeldGun(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof GrapplingHookGunItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof GrapplingHookGunItem) return off;
        return null;
    }

    /**
     * 同步钩爪枪当前状态（模式/挖掘模式等）到已发射的钩爪实体。
     * 让“发射后再切换钻头模式”能即时生效。
     */
    public void refreshGunState(Player shooter) {
        ItemStack held = getHeldGun(shooter);
        if (held == null || !(held.getItem() instanceof GrapplingHookGunItem)) {
            return;
        }
        // 只有手上拿的确实是「发射时那把枪」才同步，避免用另一把枪的状态污染本钩爪
        if (sourceSlot >= 0 && sourceSlot < shooter.getInventory().getContainerSize()) {
            ItemStack inSlot = shooter.getInventory().getItem(sourceSlot);
            if (inSlot != held) {
                return;
            }
        }
        this.gunStack = held.copy();
    }

    /**
     * 让已锚定钩爪的状态与当前模式保持一致（发射后再切换模式时生效）：
     * - 方块目标：PULL ↔ CLIMB 之间随模式切换；
     * - 生物/掉落物目标：始终保持 PULLING（无法攀岩）；
     * - 快速模式 / 双钩模式：各有独立状态机，不参与同步。
     */
    private void syncStateWithMode() {
        if (quickMode) return;
        if (getTargetType() != TargetType.BLOCK) return;
        State st = getHookState();
        if (st != State.HOOKED && st != State.PULLING && st != State.CLIMBING) return;

        GrapplingHookGunItem.Mode mode = GrapplingHookGunItem.getMode(gunStack);
        if (mode == GrapplingHookGunItem.Mode.CLIMB && (st == State.HOOKED || st == State.PULLING)) {
            entityData.set(DATA_AUTO_ASCEND, false);
            setHookState(State.CLIMBING);
        } else if (mode == GrapplingHookGunItem.Mode.PULL && st == State.CLIMBING) {
            entityData.set(DATA_AUTO_ASCEND, false);
            // 4A：切到牵引后不立即拉，先静止悬挂。
            // 把绳长锁到当前距离，玩家才会被"挂在当前位置"而不是掉回球底。
            Player shooter = getShooter();
            if (shooter != null) {
                setRopeLength((float) shooter.position().distanceTo(getAnchorPos()));
            }
            ropeLockedAfterClimb = true;
            setHookState(State.HOOKED);
        }
    }

    private int obstructedTicks = 0;
    private Vec3 lastShooterPos = Vec3.ZERO;
    private int miningTicks = 0;
    private int maxMiningTicks = 20;
    private BlockPos miningBlockPos = BlockPos.ZERO;
    /** 锚定的方块坐标（与锚点 Vec3 分离，避免用 Vec3 反推坐标出错）。 */
    private BlockPos hookedBlockPos = null;
    private int continuousDamageTimer = 0;
    private boolean pressurized = false;

    private boolean itemProtection = false;


    @Nullable
    private Entity hookedEntity = null;
    @Nullable
    private GrapplingHookEntity partnerHook = null;

    /** 快速模式：本钩爪属于快速模式。 */
    private boolean quickMode = false;
    /** 快速模式：是否为第二次发射的钩爪。 */
    private boolean quickSecond = false;
    /** 快速模式：已到达目标并处于悬停。 */
    private boolean quickHold = false;
    /** 从攀岩切换到牵引后，是否处于"锁定当前位置"状态（绳长球面约束）。 */
    private boolean ropeLockedAfterClimb = false;
    /**
     * 快速模式到达半径。
     * 玩家碰撞箱半宽 0.3，钩爪贴在方块表面，玩家中心物理极限约 0.3 格。
     * 取 0.5：既"拉到钩爪位置"，又保证一定能判定到达（0.1 永远到不了 → 持续耗能 → 被收回）。
     */
    private static final double QUICK_ARRIVE_RADIUS = 0.5;
    /** 快速模式：悬停后可自由活动的球面半径（到达后才生效）。 */
    private static final double QUICK_HOLD_RADIUS = 2.0;

    public GrapplingHookEntity(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = false;
        this.noCulling = true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }

    /** 禁止玩家/生物推动钩爪实体，避免靠近时产生阻力与站上去的错觉。 */
    @Override
    public boolean isPushable() {
        return false;
    }

    /** 不参与实体碰撞。 */
    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    /** 钩爪头不可被伤害：任何伤害来源一律无效（含钻头、AOE、爆炸等）。 */
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return true;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    public GrapplingHookEntity(Level level, Player shooter, ItemStack gunStack) {
        this(level, shooter, gunStack, shooter.getMainHandItem() == gunStack
                ? shooter.getInventory().selected : 40);
    }

    public GrapplingHookEntity(Level level, Player shooter, ItemStack gunStack, int sourceSlot) {
        this(ModEntities.GRAPPLING_HOOK.get(), level);
        setOwner(shooter);
        entityData.set(DATA_OWNER_ID, shooter.getId());
        this.gunStack = gunStack.copy();
        this.sourceGun = gunStack.copy();
        this.sourceSlot = sourceSlot;
        setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());

        this.pressurized = GrapplingHookGunItem.hasPressurizedModule(gunStack);
        this.itemProtection = GrapplingHookGunItem.hasItemProtectionModule(gunStack);

        IHookMaterial material = GrapplingHookGunItem.getHookMaterial(gunStack);
        int matId = 0;
        if (material.hasWaterResistance()) matId = 1; // Prismarine
        else if (!material.canHookBlocks()) matId = 2; // Wool
        else if (material.canHookHardBlocks()) matId = 3; // Netherite
        entityData.set(DATA_HOOK_MATERIAL_ID, matId);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DATA_STATE, State.FLYING.ordinal());
        entityData.define(DATA_TARGET_TYPE, TargetType.NONE.ordinal());
        entityData.define(DATA_TARGET_ENTITY_ID, -1);
        entityData.define(DATA_ROPE_LENGTH, 16.0f);
        entityData.define(DATA_AUTO_ASCEND, false);
        entityData.define(DATA_PARTNER_HOOK_ID, -1);
        entityData.define(DATA_HOOK_MATERIAL_ID, 0);
        entityData.define(DATA_ANCHOR_X, 0.0f);
        entityData.define(DATA_ANCHOR_Y, 0.0f);
        entityData.define(DATA_ANCHOR_Z, 0.0f);
        entityData.define(DATA_OWNER_ID, -1);
    }

    public State getHookState() {
        int idx = entityData.get(DATA_STATE);
        State[] values = State.values();
        return (idx >= 0 && idx < values.length) ? values[idx] : State.FLYING;
    }

    public void setHookState(State state) {
        entityData.set(DATA_STATE, state.ordinal());
        // 离开 HOOKED（进入 PULLING/CLIMBING/RETRACTING 等）即解除"切模式锁定"
        if (state != State.HOOKED) {
            ropeLockedAfterClimb = false;
        }
    }

    public TargetType getTargetType() {
        int idx = entityData.get(DATA_TARGET_TYPE);
        TargetType[] values = TargetType.values();
        return (idx >= 0 && idx < values.length) ? values[idx] : TargetType.NONE;
    }

    public void setTargetType(TargetType type) {
        entityData.set(DATA_TARGET_TYPE, type.ordinal());
    }

    public int getHookMaterialId() {
        return entityData.get(DATA_HOOK_MATERIAL_ID);
    }

    public Vec3 getAnchorPos() {
        return new Vec3(entityData.get(DATA_ANCHOR_X), entityData.get(DATA_ANCHOR_Y), entityData.get(DATA_ANCHOR_Z));
    }

    public void setAnchorPos(Vec3 pos) {
        entityData.set(DATA_ANCHOR_X, (float) pos.x);
        entityData.set(DATA_ANCHOR_Y, (float) pos.y);
        entityData.set(DATA_ANCHOR_Z, (float) pos.z);
    }

    public float getRopeLength() {
        return entityData.get(DATA_ROPE_LENGTH);
    }

    public void setRopeLength(float length) {
        entityData.set(DATA_ROPE_LENGTH, Math.max(1.0f, length));
    }

    public boolean isHooked() {
        State s = getHookState();
        return s == State.HOOKED || s == State.PULLING || s == State.CLIMBING || s == State.MINING;
    }

    public void setRetracting() {
        setHookState(State.RETRACTING);
        noPhysics = true;
        dropLanded = false;
        if (hookedEntity instanceof ItemEntity item) {
            item.setNoGravity(false);
        }
    }

    public void startPulling() {
        setHookState(State.PULLING);
        obstructedTicks = 0;
    }

    public void toggleAutoAscend() {
        boolean current = entityData.get(DATA_AUTO_ASCEND);
        entityData.set(DATA_AUTO_ASCEND, !current);
    }

    /** 连接长度（单向刚性：距离不得超过此值）。上限 20。 */
    private double leashLength = 20.0;

    /**
     * 建立连接（保证双向唯一）：
     * 先解除双方已有的旧连接，再互设 partner，避免出现“多个钩爪连到同一目标”的错乱。
     */
    public void setPartnerHook(GrapplingHookEntity partner) {
        if (partner == null || partner == this) {
            return;
        }
        // 解除双方可能残留的旧连接
        this.clearPartnerHook();
        partner.clearPartnerHook();

        this.partnerHook = partner;
        entityData.set(DATA_PARTNER_HOOK_ID, partner.getId());
        partner.partnerHook = this;
        partner.entityData.set(DATA_PARTNER_HOOK_ID, this.getId());

        // 建立连接时记录两端距离作为拴绳长度（上限 20）
        double d = position().distanceTo(partner.position());
        double len = Math.min(20.0, Math.max(0.0, d));
        this.leashLength = len;
        partner.leashLength = len;
    }

    public double getLeashLength() {
        return leashLength;
    }

    @Nullable
    public GrapplingHookEntity getPartnerHook() {
        if (partnerHook == null && entityData.get(DATA_PARTNER_HOOK_ID) != -1) {
            Entity ent = level().getEntity(entityData.get(DATA_PARTNER_HOOK_ID));
            if (ent instanceof GrapplingHookEntity hook) {
                partnerHook = hook;
            }
        }
        // 缓存指向已移除的实体 -> 视为断开
        if (partnerHook != null && partnerHook.isRemoved()) {
            partnerHook = null;
            entityData.set(DATA_PARTNER_HOOK_ID, -1);
        }
        return partnerHook;
    }

    /** 双向解除配对，避免一方的 partner 残留导致多个钩爪连接错乱。 */
    public void clearPartnerHook() {
        GrapplingHookEntity partner = this.partnerHook;
        this.partnerHook = null;
        entityData.set(DATA_PARTNER_HOOK_ID, -1);
        this.leashLength = 20.0;
        if (partner != null && partner.partnerHook == this) {
            partner.partnerHook = null;
            partner.entityData.set(DATA_PARTNER_HOOK_ID, -1);
            partner.leashLength = 20.0;
        }
    }

    /** 是否已成对连接（用于屏蔽普通钩爪交互）。 */
    public boolean isPaired() {
        return getPartnerHook() != null;
    }

    /** 同步字段中的 partner id（-1 表示从未配对）。供渲染器判断"该配对但对方未加载"。 */
    public int getPartnerHookId() {
        return entityData.get(DATA_PARTNER_HOOK_ID);
    }

    /**
     * 是否为「双钩模式」发射的钩爪（绳只连两钩之间，永不连玩家手）。
     * 注意排除快速模式：QUICK 同样需要双钩模块，但它的绳必须连玩家手（蜘蛛侠摆荡）。
     */
    public boolean isDualHookMode() {
        // 不用 getMode(gunStack)==DUAL：从 DUAL 切到 QUICK 后，已发射的双钩钩爪
        // 会被 refreshGunState 刷成 QUICK 模式，但那两枚钩爪本身仍是双钩配对，
        // 绳必须继续画在两钩之间。用 quickMode 区分即可。
        return !quickMode && GrapplingHookGunItem.hasDualHookModule(gunStack);
    }

    public void setQuickMode(boolean quick, boolean second) {
        this.quickMode = quick;
        this.quickSecond = second;
    }

    public boolean isQuickMode() {
        return quickMode;
    }

    public boolean isQuickSecond() {
        return quickSecond;
    }

    /** 快速模式第 1 枚是否已命中（已进入牵引或悬停）。 */
    public boolean isQuickFirstHooked() {
        if (!quickMode || quickSecond) return false;
        State s = getHookState();
        return s == State.PULLING || s == State.HOOKED
                || s == State.CLIMBING || s == State.MINING;
    }

    @Nullable
    public Player getShooter() {
        Entity owner = getOwner();
        if (owner instanceof Player player) return player;
        int ownerId = entityData.get(DATA_OWNER_ID);
        if (ownerId != -1) {
            Entity ent = level().getEntity(ownerId);
            if (ent instanceof Player player) {
                setOwner(player);
                return player;
            }
        }
        return null;
    }

    // ==================== 主循环 Tick ====================

    @Override
    public void tick() {
        super.tick();

        Player shooter = getShooter();
        if (!level().isClientSide) {
            if (shooter == null || !shooter.isAlive()) {
                discard();
                return;
            }
            // 切换维度后旧维度残留的钩爪直接移除
            if (shooter.level() != this.level()) {
                discard();
                return;
            }
            State st = getHookState();
            // 以下情况不因距离强制收回：
            //  - 已成对的双钩（已部署装备）
            //  - 快速模式（设计上就是远距离摆荡，钩爪常离玩家 80+ 格）
            if (st != State.RETRACTING && st != State.DROPPING
                    && !isPaired()
                    && !quickMode
                    && position().distanceTo(shooter.position()) > 80.0) {
                setRetracting();
            }
            // 落地后的钩爪：玩家走远（> 80 格）时移除，避免世界内长期堆积
            if (st == State.DROPPING && dropLanded
                    && position().distanceTo(shooter.position()) > 80.0) {
                discard();
                return;
            }
            checkItemSwitch(shooter);
            // 每 tick 同步枪的状态（模式/挖掘模式），让发射后切换钻头模式即时生效
            refreshGunState(shooter);
            // 发射后再切换 PULL/CLIMB 模式：让已锚定的方块目标跟随模式切换状态
            syncStateWithMode();

            switch (getHookState()) {
                case FLYING -> tickFlying(shooter);
                case HOOKED -> tickHooked(shooter);
                case PULLING -> tickPulling(shooter);
                case CLIMBING -> tickClimbing(shooter);
                case MINING -> tickMining(shooter);
                case RETRACTING -> tickRetracting(shooter);
                case DROPPING -> tickDropping();
            }

            // ===== 双钩「拴绳式」连接（单向刚性）=====
            // 连接建立时记录两端距离 leashLength（上限 20）；
            // 距离超过 leashLength 才把两端被钩住的实体拉近，允许靠近。
            {
                GrapplingHookEntity partner = getPartnerHook();
                State selfState = getHookState();
                if (partner != null && partner.isRemoved()) {
                    // 对方已消失：断开本端连接，避免继续画绳或错误约束
                    clearPartnerHook();
                    partner = null;
                }
                if (partner != null
                        && partner.getHookState() != State.RETRACTING
                        && selfState != State.RETRACTING
                        && selfState != State.DROPPING
                        && partner.getHookState() != State.DROPPING) {
                    constrainPair(this, partner, position().distanceTo(partner.position()));
                }
            }
        } else {
            // 客户端：重试解析 partner（实体可能晚于本钩爪 spawn）
            if (entityData.get(DATA_PARTNER_HOOK_ID) != -1 && partnerHook == null) {
                getPartnerHook();
            }
            // DROPPING：客户端直接采用服务端同步来的位置（不额外吸附）
            // 其它锚定类状态：吸附到锚点/目标实体，保证视觉稳定
            if (isHooked()) {
                if (getTargetType() == TargetType.BLOCK) {
                    Vec3 anchor = getAnchorPos();
                    if (anchor.lengthSqr() > 0.001) {
                        setPos(anchor.x, anchor.y, anchor.z);
                    }
                } else if (getTargetType() == TargetType.LIVING || getTargetType() == TargetType.ITEM) {
                    int targetId = entityData.get(DATA_TARGET_ENTITY_ID);
                    if (targetId != -1) {
                        Entity ent = level().getEntity(targetId);
                        if (ent != null) {
                            setPos(ent.getX(), ent.getY() + ent.getBbHeight() / 2.0, ent.getZ());
                        }
                    }
                }
            }
        }
    }

    /**
     * 切换手持物品时的收回规则（设计文档 §3.3）：
     * - 切到非钩爪枪物品：立即收回；
     * - 切到另一把钩爪枪：保留钩爪，并记录“已离开原枪”；
     * - 在离开过原枪的前提下切回原枪：立即收回。
     *
     * 注意：发射瞬间玩家手上拿的就是原枪，因此不能简单地“拿着原枪就收回”，
     * 必须经过“切走 → 切回”才触发收回，否则钩爪刚射出就会被立刻收掉。
     */
    private void checkItemSwitch(Player shooter) {
        // 双钩已成对连接：视为已部署的装备，切换物品栏不收回
        GrapplingHookEntity partner = getPartnerHook();
        if (partner != null && !partner.isRemoved()
                && getHookState() != State.RETRACTING && partner.getHookState() != State.RETRACTING) {
            return;
        }

        // 1. 手上（主手/副手）没有任何钩爪枪 -> 立即收回
        if (getHeldGun(shooter) == null) {
            setRetracting();
            return;
        }

        // 快速模式：枪仍在手（主手或副手）时，允许自由使用主手物品
        // （吃东西 / 放方块 / 打怪），切走再切回枪不收回钩爪。
        // 这豁免的是下方"切回原枪即收回"的逻辑，不影响"枪离手即收回"。
        if (quickMode) {
            return;
        }

        if (sourceGun.isEmpty()) {
            return;
        }

        // 2. 用槽位识别“原枪”：NBT（能量/模式）每 tick 都在变，不能作为身份依据。
        ItemStack inSlot = sourceSlot >= 0 && sourceSlot < shooter.getInventory().getContainerSize()
                ? shooter.getInventory().getItem(sourceSlot)
                : ItemStack.EMPTY;
        boolean stillInSourceSlot = inSlot.getItem() instanceof GrapplingHookGunItem;

        if (stillInSourceSlot) {
            // 3. 原枪仍在原位：若此前离开过，则视为“切回原枪”-> 收回
            if (leftSourceGun) {
                setRetracting();
            }
        } else {
            // 4. 原枪已不在原位（切到别的枪或别的物品）：保留钩爪，记录已离开
            leftSourceGun = true;
        }
    }

    // ==================== 1. 飞行阶段 ====================

    /** 快速模式飞行超时（tick）：5 秒未命中则收回，避免射偏的钩爪永久占用 quickSecond 槽位。 */
    private static final int QUICK_FLY_TIMEOUT_TICKS = 100;

    private void tickFlying(Player shooter) {
        Vec3 pos = position();
        Vec3 motion = getDeltaMovement();

        // 射程无距离限制（设计文档 §8）：飞行阶段不做距离判定。
        // 但快速模式需要超时保护：第 2 枚射偏后会一直飞，占用 quickSecond，
        // 导致玩家既发不了新的第 2 枚，也无法摆荡。
        if (quickMode && tickCount > QUICK_FLY_TIMEOUT_TICKS) {
            setRetracting();
            return;
        }

        // 水中阻力
        if (isInWater()) {
            if (getHookMaterialId() == 1) {
                // 海晶钩爪：水中无阻力并加速 20%
                setDeltaMovement(motion.scale(1.2));
            } else {
                // 普通材质：水中速度严重衰减
                setDeltaMovement(motion.scale(0.6).subtract(0, 0.05, 0));
            }
        } else {
            // 空气重力
            setDeltaMovement(motion.subtract(0, 0.03, 0));
        }

        // 拾取模式磁吸：命中判定之前先尝试锁定附近掉落物
        if (tryMagnetPickup()) {
            return;
        }

        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            onHit(hitResult);
            // 命中后立即停止位移，避免残余速度把钩爪“弹飞”
            setDeltaMovement(Vec3.ZERO);
        } else {
            move(MoverType.SELF, getDeltaMovement());
            handleFlyingCollision();
        }
    }

    /**
     * 拾取模式磁吸：飞行中对 1.8 格内的掉落物直接建立连接。
     * @return true 表示已锁定目标（调用方应停止本 tick 后续流程）
     */
    private boolean tryMagnetPickup() {
        if (GrapplingHookGunItem.getMode(gunStack) != GrapplingHookGunItem.Mode.PICKUP
                || GrapplingHookGunItem.hasDualHookModule(gunStack)) {
            return false;
        }
        AABB magnetBox = getBoundingBox().inflate(1.8);
        List<ItemEntity> nearbyItems = level().getEntitiesOfClass(ItemEntity.class, magnetBox, Entity::isAlive);
        if (nearbyItems.isEmpty()) {
            return false;
        }
        onHitEntity(new EntityHitResult(nearbyItems.get(0)));
        return true;
    }

    /**
     * 飞行中发生方块碰撞（射线擦边未命中）时的兜底锚定。
     * 锚点必须用钩爪当前坐标，不能瞬移到方块中心（那会穿进方块内部）。
     */
    private void handleFlyingCollision() {
        if (!this.horizontalCollision && !this.verticalCollision) {
            return;
        }
        BlockPos anchor = HookGeometry.findSolidBlockNearby(level(), position());
        if (anchor != null) {
            Vec3 loc = position();
            onHit(new BlockHitResult(loc, Direction.UP, anchor, false));
        } else {
            setRetracting();
        }
        setDeltaMovement(Vec3.ZERO);
    }

    /**
     * 把锚点钳制到目标方块的表面：
     * - 先取方块 AABB 内最近点；
     * - 若点仍落在方块内部，则沿最短轴推出到表面。
     * 这样即使命中点/兜底坐标在空气中，锚点也会贴住方块，不会“悬空”。
     */
    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target) || target == getOwner() || target instanceof GrapplingHookEntity) {
            return false;
        }
        // 快速模式：钩爪应一路飞向方块/生物，不能被沿途掉落物拦截
        // （QUICK 同样需要双钩模块，若走 onHitEntity 会被"双钩不钩掉落物"分支收回）
        if (quickMode && target instanceof ItemEntity) {
            return false;
        }
        return true;
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (level().isClientSide) return;

        // 抓取模块稳固模式：自身不动，遇方块碰撞即收回
        if (GrapplingHookGunItem.isGrappleModuleEffective(gunStack)
                && GrapplingHookGunItem.getGrappleMode(gunStack) == GrapplingHookGunItem.GrappleMode.STABLE) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.2f);
            setRetracting();
            return;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level().getBlockState(pos);

        // 命中坐标落在空气（贴面/擦边）时，只用「命中面反推」找真实方块。
        // 不做“附近搜索”：那会把锚点记成玩家并未瞄准的相邻方块，
        // 导致玩家破坏真正那块方块时无法触发下落（连接处破坏尤其明显）。
        if (state.isAir()) {
            BlockPos opposite = pos.relative(hitResult.getDirection().getOpposite());
            if (!level().getBlockState(opposite).isAir()) {
                pos = opposite;
                state = level().getBlockState(pos);
            }
        }

        // 方块有效性校验（空气 / 羊毛钩爪 / 高硬度）：不通过即收回
        float hardness = state.getDestroySpeed(level(), pos);
        if (!validateHookedBlock(state, hardness)) {
            return;
        }

        // 锚定方块：锚点必须落在该方块的表面（不能是空气位置，否则视觉上会“悬空钩住”）
        Vec3 hitPos = HookGeometry.clampToBlockSurface(level(), hitResult.getLocation(), pos);
        setPos(hitPos.x, hitPos.y, hitPos.z);
        setAnchorPos(hitPos);
        this.hookedBlockPos = pos;
        setDeltaMovement(Vec3.ZERO);
        setTargetType(TargetType.BLOCK);

        Player shooter = getShooter();
        if (shooter != null) {
            setRopeLength((float) hitPos.distanceTo(shooter.position()));
        }

        level().playSound(null, hitPos.x, hitPos.y, hitPos.z, ModSounds.HOOK_HIT_BLOCK.get(), SoundSource.PLAYERS, 0.9f, 1.0f);

        // 锚定状态选择（挖掘 / 快速 / 双钩 / 攀岩 / 普通）
        applyHookedStateAfterHit(hardness, pos);
    }

    /**
     * 校验命中的方块是否可作为锚点。不通过时直接收回。
     * @return true 表示有效
     */
    private boolean validateHookedBlock(BlockState state, float hardness) {
        if (state.isAir()) {
            // 无法确定锚定方块 -> 收回
            setRetracting();
            return false;
        }
        IHookMaterial material = GrapplingHookGunItem.getHookMaterial(gunStack);

        // 羊毛钩爪无法钩方块
        if (!material.canHookBlocks()) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.WOOL_FALL, SoundSource.PLAYERS, 0.8f, 1.0f);
            setRetracting();
            return false;
        }

        // 高硬度方块（黑曜石等）判定
        if (hardness > 25.0f && !material.canHookHardBlocks()) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_HIT, SoundSource.PLAYERS, 0.8f, 1.8f);
            setRetracting();
            return false;
        }
        return true;
    }

    /**
     * 命中方块后的状态选择：
     * - 挖掘模块开启 -> MINING
     * - 快速模式 -> PULLING（第 2 枚命中则升级为第 1 枚并回收旧第 1 枚）
     * - 双钩 -> HOOKED（仅受拴绳约束）
     * - 攀岩 -> CLIMBING
     * - 其余 -> HOOKED
     */
    private void applyHookedStateAfterHit(float hardness, BlockPos pos) {
        // 挖掘模式检测：若装备挖掘模块且处于开启状态（双钩模块激活时挖掘不生效）
        if (GrapplingHookGunItem.isMiningModuleEffective(gunStack)) {
            GrapplingHookGunItem.MiningMode mMode = GrapplingHookGunItem.getMiningMode(gunStack);
            if (mMode != GrapplingHookGunItem.MiningMode.OFF && hardness >= 0.0f) {
                this.miningBlockPos = pos;
                this.miningTicks = 0;
                this.maxMiningTicks = Math.max(10, (int) (hardness * 12));
                setHookState(State.MINING);
                return;
            }
        }

        GrapplingHookGunItem.Mode mode = GrapplingHookGunItem.getMode(gunStack);
        if (quickMode) {
            setHookState(State.PULLING);
            if (quickSecond) {
                // 本枚是第 2 枚：回收旧的第 1 枚，然后本枚升级为新的第 1 枚，
                // 这样还能继续发第 3 枚、第 4 枚……实现连续摆荡。
                if (level() instanceof ServerLevel sl) {
                    List<GrapplingHookEntity> others = sl.getEntitiesOfClass(
                            GrapplingHookEntity.class,
                            getBoundingBox().inflate(256.0),
                            h -> h != this && h.isQuickMode() && !h.isQuickSecond()
                                    && h.getShooter() == getShooter() && !h.isRemoved());
                    for (GrapplingHookEntity h : others) {
                        h.setRetracting();
                    }
                }
                this.quickSecond = false;   // 升级为新的第 1 枚
            }
        } else if (mode == GrapplingHookGunItem.Mode.DUAL) {
            setHookState(State.HOOKED);
        } else if (mode == GrapplingHookGunItem.Mode.CLIMB) {
            setHookState(State.CLIMBING);
        } else {
            setHookState(State.HOOKED);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (level().isClientSide) return;

        Entity target = hitResult.getEntity();
        Player shooter = getShooter();
        if (shooter == null || target == shooter) return;

        // 双钩模块不钩掉落物：直接收回，避免钩爪停留在命中点悬空
        if (target instanceof ItemEntity item && GrapplingHookGunItem.hasDualHookModule(gunStack)) {
            setRetracting();
            return;
        }

        // 掉落物处理
        if (target instanceof ItemEntity item) {
            hookedEntity = item;
            entityData.set(DATA_TARGET_ENTITY_ID, item.getId());
            setTargetType(TargetType.ITEM);
            setAnchorPos(item.position());
            setHookState(State.PULLING);

            if (itemProtection) {
                item.setInvulnerable(true);
                item.clearFire();
            }
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8f, 1.2f);
            return;
        }

        // 生物处理
        if (target instanceof LivingEntity living) {
            IHookMaterial material = GrapplingHookGunItem.getHookMaterial(gunStack);
            float damage = material.getDamage(isInWater());

            if (damage > 0.0f) {
                living.hurt(level().damageSources().playerAttack(shooter), damage);
            }

            // Boss 或免疫牵引实体判定：仅造成伤害立即收回
            if (!living.canChangeDimensions() || living.getType().getDescriptionId().contains("boss")
                    || living.getType() == EntityType.WITHER || living.getType() == EntityType.ENDER_DRAGON) {
                setRetracting();
                return;
            }

            hookedEntity = living;
            entityData.set(DATA_TARGET_ENTITY_ID, living.getId());
            setTargetType(TargetType.LIVING);
            setAnchorPos(living.position().add(0, living.getBbHeight() / 2.0, 0));
            setRopeLength((float) living.position().distanceTo(shooter.position()));

            level().playSound(null, getX(), getY(), getZ(), ModSounds.HOOK_HIT_ENTITY.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

            // 快速模式：能拉则拉（玩家被拉向目标，蜘蛛侠式），Boss 等不可拉实体已在上方收回
            if (quickMode) {
                setHookState(State.PULLING);
                if (quickSecond) {
                    // 本枚升级为新的第 1 枚（与方块命中逻辑一致）
                    if (level() instanceof ServerLevel sl) {
                        List<GrapplingHookEntity> others = sl.getEntitiesOfClass(
                                GrapplingHookEntity.class,
                                getBoundingBox().inflate(256.0),
                                h -> h != this && h.isQuickMode() && !h.isQuickSecond()
                                        && h.getShooter() == getShooter() && !h.isRemoved());
                        for (GrapplingHookEntity h : others) {
                            h.setRetracting();
                        }
                    }
                    this.quickSecond = false;
                }
                return;
            }

            // 若激活挖掘模块（且未装双钩），切入持续磨血状态
            if (GrapplingHookGunItem.isMiningModuleEffective(gunStack)
                    && GrapplingHookGunItem.getMiningMode(gunStack) != GrapplingHookGunItem.MiningMode.OFF) {
                setHookState(State.MINING);
            } else {
                setHookState(State.HOOKED);
            }
        }
    }

    // ==================== 2. 锚定阶段 ====================

    /**
     * 锚定阶段。
     *
     * 按模式严格分层，每种模式只跑自己的逻辑后立即返回，互不干扰：
     *  - 快速模式：悬停球面约束 + 方块消失检测
     *  - 双钩模式：只做方块消失检测（牵引由拴绳约束负责）
     *  - 其它模式：方块消失检测 -> 挖掘判定 -> 终点潜行收回
     */
    private void tickHooked(Player shooter) {
        updateAnchorTarget();

        // ---------- 1. 快速模式 ----------
        if (quickMode) {
            if (quickHold) {
                applyQuickHold(shooter);
            }
            // 方块消失检测
            if (getTargetType() == TargetType.BLOCK) {
                BlockPos checkPos = resolveHookedBlockPos();
                if (checkPos == null || level().getBlockState(checkPos).isAir()) {
                    startDropping();
                }
            }
            return;
        }

        // ---------- 2. 双钩模式 ----------
        // 同上：用 isDualHookMode() 而非 getMode(gunStack)，避免切到 QUICK 后双钩钩爪误入其它分支
        if (isDualHookMode()) {
            if (getTargetType() == TargetType.BLOCK) {
                BlockPos checkPos = resolveHookedBlockPos();
                if (checkPos == null || level().getBlockState(checkPos).isAir()) {
                    startDropping();
                }
            }
            return;
        }

        // ---------- 3. 其它模式 ----------
        // 方块消失检测（必须在挖掘判定之前）
        if (getTargetType() == TargetType.BLOCK) {
            BlockPos checkPos = resolveHookedBlockPos();
            if (checkPos == null || level().getBlockState(checkPos).isAir()) {
                startDropping();
                return;
            }
        }

        // 挖掘：生物目标直接进入磨血
        boolean miningOn = GrapplingHookGunItem.isMiningModuleEffective(gunStack)
                && GrapplingHookGunItem.getMiningMode(gunStack) != GrapplingHookGunItem.MiningMode.OFF;

        if (miningOn && getTargetType() == TargetType.LIVING && hookedEntity instanceof LivingEntity) {
            this.continuousDamageTimer = 0;
            setHookState(State.MINING);
            return;
        }

        // 挖掘：方块目标
        if (miningOn && getTargetType() == TargetType.BLOCK) {
            BlockPos targetPos = resolveHookedBlockPos();
            if (targetPos != null) {
                BlockState st = level().getBlockState(targetPos);
                if (!st.isAir()) {
                    this.miningBlockPos = targetPos;
                    float hd = st.getDestroySpeed(level(), targetPos);
                    if (hd >= 0.0f) {
                        this.miningTicks = 0;
                        this.maxMiningTicks = Math.max(10, (int) (hd * 12));
                        setHookState(State.MINING);
                        return;
                    }
                }
            }
        }

        // 从攀岩切到牵引后：把玩家"挂在当前位置"（绳长球面约束）。
        // 仅在 CLIMB->PULL 切换后生效，不影响双钩/普通 HOOKED 的原有行为。
        if (getTargetType() == TargetType.BLOCK && ropeLockedAfterClimb) {
            Vec3 anchor = getAnchorPos();
            Vec3 pPos = shooter.position();
            double dist = pPos.distanceTo(anchor);
            double ropeLen = getRopeLength();
            if (dist > ropeLen && dist > 0.001) {
                Vec3 radialDir = anchor.subtract(pPos).scale(1.0 / dist);
                Vec3 cur = shooter.getDeltaMovement();
                double radial = cur.dot(radialDir);   // <0 表示远离
                if (radial < 0) {
                    applyMotionToPlayer(shooter, cur.subtract(radialDir.scale(radial)));
                }
            }
            shooter.resetFallDistance();
        }
    }

    /**
     * 快速模式悬停约束：
     * 玩家被绳子挂住，限制在锚点 2 格球内。
     * 不消除重力 -> 会自然下垂到球底（像挂在绳上），
     * 超出半径时只挡住向外的速度分量。
     */
    private void applyQuickHold(Player shooter) {
        Vec3 anchor = getAnchorPos();
        Vec3 pPos = shooter.position();
        double d = pPos.distanceTo(anchor);
        if (d > QUICK_HOLD_RADIUS) {
            Vec3 dir = anchor.subtract(pPos).normalize();   // 指向锚点
            Vec3 cur = shooter.getDeltaMovement();
            double outward = -cur.dot(dir);                 // >0 表示正在远离
            if (outward > 0) {
                cur = cur.add(dir.scale(outward));          // 消除向外的分量
            }
            // 轻微的向内回拉，帮助贴回球面（不做重力抵消）
            cur = cur.add(dir.scale(Math.min(0.25, (d - QUICK_HOLD_RADIUS) * 0.15)));
            applyMotionToPlayer(shooter, cur);
        }
        // 在球内时不覆盖玩家速度：保留本地输入/重力（与攀岩的"无阻力"一致）
        shooter.resetFallDistance();
    }

    /**
     * 解析当前锚定的方块坐标：
     * 1. hookedBlockPos 有效且非空气 -> 直接返回；
     * 2. 否则按锚点位置及周围搜索真实方块；
     * 3. 都找不到 -> null（视为方块已消失）。
     */
    @Nullable
    private BlockPos resolveHookedBlockPos() {
        // 只认锚定时记录的坐标：该方块一旦变成空气即视为“已消失”。
        // 不能向周围搜索替代方块，否则相邻方块会让判定永远成立。
        if (hookedBlockPos != null) {
            return hookedBlockPos;
        }
        // 旧存档/重载后可能缺失，此时才用锚点反推（仅当该坐标确实是实心方块）
        BlockPos base = BlockPos.containing(getAnchorPos().subtract(0, 0.1, 0));
        return level().getBlockState(base).isAir() ? null : base;
    }

    private void updateAnchorTarget() {
        if (getTargetType() == TargetType.LIVING || getTargetType() == TargetType.ITEM) {
            if (hookedEntity == null || !hookedEntity.isAlive() || hookedEntity.isRemoved()) {
                setRetracting();
                return;
            }
            Vec3 pos = hookedEntity.position().add(0, hookedEntity.getBbHeight() / 2.0, 0);
            setPos(pos.x, pos.y, pos.z);
            setAnchorPos(pos);
        }
    }

    // ==================== 3. 牵引阶段 ====================

    private void tickPulling(Player shooter) {
        updateAnchorTarget();

        // 方块消失检测：与 tickHooked / tickClimbing / tickMining 一致。
        // 缺少这一步时，方块被破坏后钩爪会继续拉向空气位置，玩家看到"悬空牵引"。
        if (getTargetType() == TargetType.BLOCK) {
            BlockPos checkPos = resolveHookedBlockPos();
            if (checkPos == null || level().getBlockState(checkPos).isAir()) {
                startDropping();
                return;
            }
        }

        // 牵引能量持续消耗（0.005/tick，加压模块下 0.010/tick，调为原速度十分之一）
        float energyCost = pressurized ? 0.010f : 0.005f;
        ItemStack held = getHeldGun(shooter);
        if (held == null || !GrapplingHookGunItem.consumeEnergyWithBacktank(shooter, held, energyCost)) {
            setRetracting();
            return;
        }

        TargetType tType = getTargetType();
        Vec3 anchor = getAnchorPos();

        // A. 方块牵引
        if (tType == TargetType.BLOCK) {
            pullTowardsBlock(shooter, anchor);
        }

        // B. 生物牵引
        else if (tType == TargetType.LIVING && hookedEntity instanceof LivingEntity target) {
            pullTowardsLiving(shooter, target);
        }

        // C. 掉落物牵引
        else if (tType == TargetType.ITEM && hookedEntity instanceof ItemEntity item) {
            pullTowardsItem(shooter, item);
        }
    }

    /** 方块牵引：快速模式直线拉近；普通模式绳索张力 + 钟摆摆动。 */
    private void pullTowardsBlock(Player shooter, Vec3 anchor) {
        Vec3 playerPos = shooter.position();
        double distance = playerPos.distanceTo(anchor);

        // 到达判定：快速模式要真正拉到钩爪位置，普通模式仍为 2 格
        double arriveRadius = quickMode ? QUICK_ARRIVE_RADIUS : 2.0;
        if (distance <= arriveRadius) {
            if (quickMode) {
                // 快速模式：进入悬停状态（玩家在钩爪附近小范围自由活动）
                quickHold = true;
            }
            setHookState(State.HOOKED);
            applyMotionToPlayer(shooter, Vec3.ZERO);
            shooter.resetFallDistance();
            return;
        }

        // 障碍物阻挡检测：0.5s（10 ticks）位移过小则处理
        if (lastShooterPos != Vec3.ZERO && playerPos.distanceTo(lastShooterPos) < 0.05) {
            obstructedTicks++;
            if (obstructedTicks >= 10) {
                if (quickMode) {
                    // 快速模式撞上障碍：转入悬停，不收回（收回会让钩爪"拉一会就没了"）
                    quickHold = true;
                    setHookState(State.HOOKED);
                } else {
                    setRetracting();
                }
                return;
            }
        } else {
            obstructedTicks = 0;
        }
        lastShooterPos = playerPos;

        // ===== 快速模式：直接沿绳方向拉向目标（蜘蛛侠式，不做钟摆）=====
        if (quickMode) {
            Vec3 dir = anchor.subtract(playerPos).normalize();
            float maxSpeed = pressurized ? 1.2f : 0.9f;
            // 临近目标线性减速（终点为钩爪位置，而非 2 格外）
            float speed = (float) Math.max(0.12, Math.min(maxSpeed, (distance - QUICK_ARRIVE_RADIUS) * 0.5));
            applyMotionToPlayer(shooter, dir.scale(speed));
            shooter.resetFallDistance();
            if (tickCount % 6 == 0) {
                level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                        ModSounds.HOOK_PULL.get(), SoundSource.PLAYERS, 0.4f, 1.2f);
            }
            return;
        }

        // ===== 绳索张力 + 钟摆摆动（物理化牵引）=====
        // 不再“直线平移”，而是：
        //  1. 只沿绳方向施加拉力（径向加速度），越远拉力越大；
        //  2. 完整保留切向速度 -> 玩家会像钟摆一样荡出弧线；
        //  3. 切向速度加轻微阻尼（绳索摩擦），避免永动；
        //  4. 限制总速度，防止过冲。
        float maxPullSpeed = pressurized ? 0.9f : 0.6f;

        Vec3 dir = anchor.subtract(playerPos).normalize();   // 指向锚点
        Vec3 cur = shooter.getDeltaMovement();

        double radial = cur.dot(dir);                        // 径向分量（正=朝锚点）
        Vec3 radialPart = dir.scale(radial);
        Vec3 tangential = cur.subtract(radialPart);          // 切向分量（摆动）

        // 1) 径向拉力：距离越远加速度越大（0.06 ~ 0.22）
        double distFactor = Math.min(1.0, Math.max(0.0, (distance - 2.0) / 10.0));
        double pullAccel = 0.06 + 0.16 * distFactor;
        Vec3 newVel = cur.add(dir.scale(pullAccel));

        // 2) 切向轻微阻尼（绳索摩擦，5%）
        newVel = newVel.subtract(tangential.scale(0.05));

        // 3) 径向速度上限：接近目标时限制，避免过冲
        double newRadial = newVel.dot(dir);
        double radialCap = (distance <= 3.0) ? Math.min(maxPullSpeed, distance * 0.25) : maxPullSpeed;
        if (newRadial > radialCap) {
            newVel = newVel.subtract(dir.scale(newRadial - radialCap));
        }

        // 4) 总速度上限
        double len = newVel.length();
        if (len > maxPullSpeed) {
            newVel = newVel.scale(maxPullSpeed / len);
        }

        applyMotionToPlayer(shooter, newVel);

        shooter.resetFallDistance();
        if (tickCount % 6 == 0) {
            level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                    ModSounds.HOOK_PULL.get(), SoundSource.PLAYERS, 0.4f, 1.2f);
        }
    }

    /**
     * 生物牵引。
     * <p>
     * - 抓取模块·稳固：玩家不动，把目标拉向玩家；
     * - 抓取模块·拉向：小碰撞箱目标被拉向玩家，其余把玩家拉向目标；
     * - 默认：大体型/高血量把玩家拉向目标，中小型把目标拉向玩家。
     */
    private void pullTowardsLiving(Player shooter, LivingEntity target) {
        if (!target.isAlive() || target.isRemoved()) {
            setRetracting();
            return;
        }

        // 快速模式：玩家被拉向目标（蜘蛛侠式），不受体型/抓取模块影响
        if (quickMode) {
            Vec3 dir = target.position().add(0, target.getBbHeight() / 2.0, 0).subtract(shooter.position());
            double dist = dir.length();
            if (dist <= QUICK_ARRIVE_RADIUS) {
                quickHold = true;
                setHookState(State.HOOKED);
                applyMotionToPlayer(shooter, Vec3.ZERO);
                shooter.resetFallDistance();
                return;
            }
            float maxSpeed = pressurized ? 1.2f : 0.9f;
            float speed = (float) Math.max(0.12, Math.min(maxSpeed, (dist - QUICK_ARRIVE_RADIUS) * 0.5));
            applyMotionToPlayer(shooter, dir.normalize().scale(speed));
            shooter.resetFallDistance();
            return;
        }

        // 到达目标附近：停止牵引并切入 HOOKED，避免近距离方向翻转导致来回抖动
        if (shooter.position().distanceTo(target.position()) <= 2.0) {
            setHookState(State.HOOKED);
            applyMotionToPlayer(shooter, Vec3.ZERO);
            target.setDeltaMovement(Vec3.ZERO);
            target.hurtMarked = true;
            shooter.resetFallDistance();
            return;
        }

        double playerVol = shooter.getBbWidth() * shooter.getBbWidth() * shooter.getBbHeight();
        double targetVol = target.getBbWidth() * target.getBbWidth() * target.getBbHeight();
        boolean isHuge = targetVol >= 1.5 * playerVol;
        boolean isHighHealth = target.getMaxHealth() >= 150.0f;
        boolean isHeavyBoss = isHuge || isHighHealth;

        // 双钩模块激活时抓取模块不生效，按基础牵引处理
        boolean hasGrapple = GrapplingHookGunItem.isGrappleModuleEffective(gunStack);
        GrapplingHookGunItem.GrappleMode gMode = GrapplingHookGunItem.getGrappleMode(gunStack);

        float pullSpeed = pressurized ? 0.95f : 0.65f;
        Vec3 toTarget = target.position().subtract(shooter.position()).normalize();
        Vec3 toPlayer = shooter.position().subtract(target.position()).normalize();

        if (hasGrapple && gMode == GrapplingHookGunItem.GrappleMode.STABLE) {
            // 1. 抓取模块 - 稳固模式：自身绝对固定不动，极大增强将目标拉向玩家的能力
            applyMotionToPlayer(shooter, Vec3.ZERO);
            target.setDeltaMovement(toPlayer.scale(pullSpeed * 1.2).add(0, 0.16, 0));
            target.hurtMarked = true;
        } else if (hasGrapple && gMode == GrapplingHookGunItem.GrappleMode.PULL_TOWARDS) {
            // 2. 抓取模块 - 拉向模式：除碰撞箱极小生物外，其余一律将玩家拉向目标
            boolean isTiny = targetVol < 0.5 * playerVol;
            if (isTiny) {
                target.setDeltaMovement(toPlayer.scale(pullSpeed * 1.0).add(0, 0.12, 0));
                target.hurtMarked = true;
            } else {
                applyMotionToPlayer(shooter, toTarget.scale(pullSpeed));
            }
        } else {
            // 3. 基础默认牵引逻辑
            if (isHeavyBoss) {
                // 大怪 (>= 1.5倍玩家体型) 或 高血 (>= 150血)：稳定把白板玩家拉向怪物
                applyMotionToPlayer(shooter, toTarget.scale(pullSpeed));
                target.setDeltaMovement(target.getDeltaMovement().scale(0.8));
            } else {
                // 普通中小型怪：大幅提高把怪拉向玩家的能力，施加轻微上抛防卡地面
                target.setDeltaMovement(toPlayer.scale(pullSpeed * 1.15).add(0, 0.18, 0));
                target.hurtMarked = true;
                applyMotionToPlayer(shooter, toTarget.scale(pullSpeed * 0.05));
            }
        }
        shooter.resetFallDistance();
        if (tickCount % 6 == 0) {
            level().playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                    ModSounds.HOOK_PULL.get(), SoundSource.PLAYERS, 0.4f, 1.2f);
        }
    }

    /**
     * 掉落物牵引：拉向玩家，接近后自动拾取；掉落物保护模块额外防止被销毁。
     */
    private void pullTowardsItem(Player shooter, ItemEntity item) {
        if (!item.isAlive() || item.isRemoved()) {
            setRetracting();
            return;
        }
        Vec3 toPlayer = shooter.position().add(0, 0.6, 0).subtract(item.position());
        double dist = toPlayer.length();

        if (dist < 1.8) {
            // 接近玩家自动拾取
            item.setPickUpDelay(0);
            item.playerTouch(shooter);
            if (itemProtection) {
                item.setInvulnerable(false);
            }
            item.setNoGravity(false);
            setRetracting();
        } else {
            item.setNoGravity(true);
            Vec3 motion = toPlayer.normalize().scale(1.1).add(0, 0.18, 0);
            item.setDeltaMovement(motion);
            item.hasImpulse = true;
            item.hurtMarked = true;
            if (itemProtection) {
                item.clearFire();
                if (item.getY() < level().getMinBuildHeight() + 1) {
                    item.setDeltaMovement(motion.x, 0.5, motion.z);
                }
            }
        }
    }

    private void applyMotionToPlayer(Player player, Vec3 motion) {
        player.setDeltaMovement(motion);
        player.hurtMarked = true;
        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new SyncPlayerMotionPacket(motion), serverPlayer);
        }
    }

    // ==================== 4. 攀岩阶段 ====================

    /**
     * 攀岩阶段：以「最大活动半径」为核心的球面约束。
     *
     * 规则：
     *  1. 绳长内完全自由（保留重力，可摆荡、可自由移动）；
     *  2. 越界时只消除「向外（远离锚点）的径向速度分量」，不额外回拉，避免持续阻力感；
     *  3. 自动上升/手动收放绳只改绳长，位移由球面约束自然产生；
     *  4. 每 tick 都会把修正后的速度同步给客户端（保持客户端预测一致）。
     */
    private void tickClimbing(Player shooter) {
        if (getTargetType() != TargetType.BLOCK) {
            setRetracting();
            return;
        }

        // 附着方块消失 -> 钩爪自由下落
        if (hookedBlockPos != null && level().getBlockState(hookedBlockPos).isAir()) {
            startDropping();
            return;
        }

        Vec3 anchor = getAnchorPos();
        Vec3 playerPos = shooter.position();
        double dist = playerPos.distanceTo(anchor);
        if (dist < 0.001) {
            shooter.resetFallDistance();
            return;
        }

        Vec3 radialDir = anchor.subtract(playerPos).scale(1.0 / dist); // 指向锚点的单位向量
        Vec3 vel = shooter.getDeltaMovement();
        double radialSpeed = vel.dot(radialDir); // >0 朝锚点

        // 攀岩自动上升速度（已按需求减半）
        double maxSpeed = pressurized ? 0.45 : 0.30;
        double nearFactor = Math.min(1.0, Math.max(0.0, (dist - 1.6) / 5.0));
        double targetSpeed = 0.08 + (maxSpeed - 0.08) * nearFactor;

        // 攀岩 = 绳长约束：玩家在绳长内自由活动（可左右摆荡、可攀爬），
        // 只有超出绳长时才被拉住。攀爬由 Z+左/右键（改绳长）驱动，不自动拉向锚点。
        double ropeLen = getRopeLength();

        // 自动上升：仅此模式下持续缩短绳长
        // 停止条件：钩爪已到达玩家手臂高度（约等于眼高），此时继续上升只会把人顶住
        double stopDist = Math.max(0.8, shooter.getEyeHeight() - 0.1);
        if (entityData.get(DATA_AUTO_ASCEND)) {
            if (dist <= stopDist) {
                entityData.set(DATA_AUTO_ASCEND, false);
                // 到位后清掉径向速度，避免残留推力把人顶离
                Vec3 v = shooter.getDeltaMovement();
                double rs = v.dot(radialDir);
                if (rs > 0) {
                    applyMotionToPlayer(shooter, v.subtract(radialDir.scale(rs)));
                }
            } else {
                setRopeLength((float) Math.max(1.0, dist - 0.45));
                ropeLen = getRopeLength();
            }
        }

        Vec3 newVel = vel;

        // 安全网：锚定状态下若周围没有任何实心方块，说明锚点已失效 -> 掉落
        if (!HookGeometry.hasSolidBlockNear(level(), getAnchorPos())) {
            startDropping();
            return;
        }

        // 攀岩 = 「最大活动半径」球面约束：
        // 绳长内完全自由（保留重力、可摆荡、可自由移动）；
        // 只有越界时才消除「向外」的径向速度，不额外回拉（避免持续阻力感）。
        if (dist > ropeLen && radialSpeed < 0) {
            newVel = newVel.subtract(radialDir.scale(radialSpeed));
        }

        // 自动上升：逐 tick 平滑抬升，且只在未到位时施加
        if (entityData.get(DATA_AUTO_ASCEND) && dist > stopDist) {
            // 取「当前径向速度」与「目标上升速度」的较大者，避免速度叠加导致上下抖动
            double curRadial = newVel.dot(radialDir);
            double wantRadial = targetSpeed;
            if (curRadial < wantRadial) {
                newVel = newVel.add(radialDir.scale(wantRadial - curRadial));
            }
        }

        // 仅在速度被修正时才覆盖玩家速度并同步客户端：
        // 每 tick 无条件覆盖会抹掉玩家本地预测的输入加速，产生“阻力感”。
        if (newVel.distanceToSqr(vel) > 1.0E-8) {
            applyMotionToPlayer(shooter, newVel);
        }

        shooter.resetFallDistance();
    }

    public void handleClimbAction(int action) {
        switch (action) {
            case HookClimbPacket.ACTION_CLIMB_UP -> {
                entityData.set(DATA_AUTO_ASCEND, false);
                Player shooter = getShooter();
                if (shooter != null) {
                    Vec3 anchor = getAnchorPos();
                    Vec3 playerPos = shooter.position();
                    double dist = playerPos.distanceTo(anchor);
                    // 到达手臂高度即停止上升（与自动上升一致）
                    double stopDist = Math.max(0.8, shooter.getEyeHeight() - 0.1);
                    if (dist <= stopDist) {
                        return;
                    }
                    setRopeLength(getRopeLength() - 0.25f);
                    // 文档 9.5：手动上升速度 0.20。只改绳长不会产生位移，
                    // 必须额外施加径向朝锚点的速度，否则玩家感觉“没反应”。
                    Vec3 dir = anchor.subtract(playerPos).scale(1.0 / dist);
                    Vec3 cur = shooter.getDeltaMovement();
                    double radial = cur.dot(dir);   // >0 朝锚点
                    if (radial < 0.20) {
                        applyMotionToPlayer(shooter, cur.add(dir.scale(0.20 - radial)));
                    }
                    shooter.resetFallDistance();
                }
            }
            case HookClimbPacket.ACTION_CLIMB_DOWN -> {
                entityData.set(DATA_AUTO_ASCEND, false);
                // 只延长绳长（扩大活动半径），不施加任何推力：
                // 玩家靠自身重力/输入即可远离钩爪，避免被"推开"。
                setRopeLength(getRopeLength() + 0.25f);
            }
            case HookClimbPacket.ACTION_AUTO_ASCEND_TOGGLE -> toggleAutoAscend();
            case HookClimbPacket.ACTION_STOP -> entityData.set(DATA_AUTO_ASCEND, false);
        }
    }

    // ==================== 5. 挖掘阶段 ====================

    private void tickMining(Player shooter) {
        // 钻头模式被切到 OFF（发射后再按 G 关闭）或双钩模块激活 -> 结束挖掘，转入普通锚定
        if (GrapplingHookGunItem.getMiningMode(gunStack) == GrapplingHookGunItem.MiningMode.OFF
                || !GrapplingHookGunItem.isMiningModuleEffective(gunStack)) {
            setHookState(State.HOOKED);
            return;
        }

        // ④ 先判定当前是否「值得挖掘」（无掉落物且为“破坏有掉落物的”模式时不耗能）
        if (getTargetType() == TargetType.BLOCK) {
            BlockState pre = level().getBlockState(miningBlockPos);
            if (pre.isAir()) {
                // 方块已被破坏 -> 钩爪脱落自由下落（与 HOOKED/CLIMBING 一致）
                startDropping();
                return;
            }
            ItemStack heldPre = getHeldGun(shooter);
            if (heldPre == null) {
                setRetracting();
                return;
            }
            GrapplingHookGunItem.MiningMode preMode = GrapplingHookGunItem.getMiningMode(heldPre);
            if (preMode == GrapplingHookGunItem.MiningMode.MINE_ALL) {
                List<ItemStack> preDrops = Block.getDrops(pre, (ServerLevel) level(), miningBlockPos, null, shooter, heldPre);
                if (preDrops.isEmpty()) {
                    // 无掉落物：不破坏、不耗能、无音效，原地等待
                    miningTicks = 0;
                    return;
                }
            }
        }

        // 钻头工作时持续耗能：0.1 能量/秒（= 0.005/tick，加压模块 ×2）
        float drillCost = pressurized ? 0.010f : 0.005f;
        ItemStack energyGun = getHeldGun(shooter);
        if (energyGun == null || !GrapplingHookGunItem.consumeEnergyWithBacktank(shooter, energyGun, drillCost)) {
            setHookState(State.HOOKED);
            return;
        }

        // A. 挖掘方块
        if (getTargetType() == TargetType.BLOCK) {
            BlockState state = level().getBlockState(miningBlockPos);
            if (state.isAir()) {
                startDropping();
                return;
            }

            miningTicks++;

            // 先取掉落物（用于模式判定）
            ItemStack heldNow = getHeldGun(shooter);
            if (heldNow == null) {
                setRetracting();
                return;
            }
            List<ItemStack> dropsNow = Block.getDrops(state, (ServerLevel) level(), miningBlockPos, null, shooter, heldNow);
            GrapplingHookGunItem.MiningMode mModeNow = GrapplingHookGunItem.getMiningMode(heldNow);

            // 「破坏 + 可掉落」模式：只挖有掉落物的方块。
            // 无掉落物 -> 不破坏、无音效、不回收，原地待命。
            if (mModeNow == GrapplingHookGunItem.MiningMode.MINE_ALL && dropsNow.isEmpty()) {
                miningTicks = 0;
                return;
            }

            // 钻头运转音效与粒子
            if (miningTicks % 4 == 0) {
                level().playSound(null, miningBlockPos.getX(), miningBlockPos.getY(), miningBlockPos.getZ(),
                        ModSounds.HOOK_DRILL.get(), SoundSource.BLOCKS, 0.7f, 1.2f);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, state),
                            miningBlockPos.getX() + 0.5, miningBlockPos.getY() + 0.5, miningBlockPos.getZ() + 0.5,
                            6, 0.2, 0.2, 0.2, 0.05
                    );
                }
            }

            if (miningTicks >= maxMiningTicks) {
                // 挖掘完成
                if (level() instanceof ServerLevel serverLevel) {
                    ItemStack held = getHeldGun(shooter);
                    if (held == null) {
                        setRetracting();
                        return;
                    }
                    List<ItemStack> drops = Block.getDrops(state, serverLevel, miningBlockPos, null, shooter, held);
                    serverLevel.destroyBlock(miningBlockPos, false);

                    // 将掉落物存入钩爪存储仓（8 格）
                    ItemStackHandler storage = GrapplingHookGunItem.getStorageHandler(held);
                    for (ItemStack drop : drops) {
                        ItemStack remainder = ItemHandlerHelper.insertItem(storage, drop, false);
                        if (!remainder.isEmpty()) {
                            // 溢出掉落物散落于世界中
                            Block.popResource(serverLevel, miningBlockPos, remainder);
                        }
                    }
                    GrapplingHookGunItem.saveStorageHandler(held, storage);
                }
                startDropping();
            }
        }

        // B. 钻头持续磨血
        else if (getTargetType() == TargetType.LIVING && hookedEntity instanceof LivingEntity living) {
            if (!living.isAlive() || living.isRemoved()) {
                setRetracting();
                return;
            }

            continuousDamageTimer++;
            if (continuousDamageTimer % 10 == 0) {
                living.hurt(level().damageSources().playerAttack(shooter), 2.0f);
                level().playSound(null, living.getX(), living.getY(), living.getZ(),
                        ModSounds.HOOK_DRILL.get(), SoundSource.PLAYERS, 0.6f, 1.4f);
            }
        } else {
            setRetracting();
        }
    }

    /** 附着方块消失 -> 钩爪脱落，自由下落（不掉进玩家背包，也不瞬移消失）。 */
    public void startDropping() {
        setHookState(State.DROPPING);
        noPhysics = false;
        hookedEntity = null;
        this.hookedBlockPos = null;
        this.dropLanded = false;
        setTargetType(TargetType.NONE);
        setDeltaMovement(0.0, -0.05, 0.0);
        // 断开双钩连接，避免 partner 继续画绳/施加约束
        GrapplingHookEntity partner = getPartnerHook();
        if (partner != null) {
            partner.clearPartnerHook();
        }
        clearPartnerHook();
    }

    /** 是否已落地（落地后永久静止，不消失）。 */
    private boolean dropLanded = false;

    /** 自由下落：受重力下落，落地后静止保留（不消失，可右键回收）。 */
    private void tickDropping() {
        // 已落地：检查脚下方块是否还在，被破坏则重新下落。
        // 用碰撞形状而非 isAir：水/草/火把等无碰撞方块不应阻止下落。
        if (dropLanded) {
            BlockPos below = BlockPos.containing(getX(), getY() - 0.1, getZ());
            BlockState belowState = level().getBlockState(below);
            if (belowState.isAir() || belowState.getCollisionShape(level(), below).isEmpty()) {
                dropLanded = false;   // 悬空 -> 继续下落
            } else {
                // 落地后若装有挖掘模块且模式开启：继续挖掘脚下这块方块（钻头向下连挖）
                Player shooter = getShooter();
                ItemStack held = shooter != null ? getHeldGun(shooter) : null;
                if (shooter != null && held != null
                        && GrapplingHookGunItem.isMiningModuleEffective(held)
                        && GrapplingHookGunItem.getMiningMode(held) != GrapplingHookGunItem.MiningMode.OFF) {
                    float hd = belowState.getDestroySpeed(level(), below);
                    if (hd >= 0.0f) {
                        this.miningBlockPos = below;
                        this.miningTicks = 0;
                        this.maxMiningTicks = Math.max(10, (int) (hd * 12));
                        this.hookedBlockPos = below;
                        setTargetType(TargetType.BLOCK);
                        setAnchorPos(new Vec3(below.getX() + 0.5, below.getY() + 1.0, below.getZ() + 0.5));
                        setHookState(State.MINING);
                        return;
                    }
                }
                setDeltaMovement(Vec3.ZERO);
                return;
            }
        }

        Vec3 motion = getDeltaMovement().add(0.0, -0.04, 0.0);
        if (motion.y < -1.2) {
            motion = new Vec3(motion.x, -1.2, motion.z);
        }
        setDeltaMovement(motion);

        double beforeY = getY();
        move(MoverType.SELF, motion);

        // 竖直方向下不去 => 落地，永久静止
        if (Math.abs(getY() - beforeY) < 0.02 && motion.y < 0) {
            dropLanded = true;
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        // 已进入实心方块内部
        if (level().getBlockState(BlockPos.containing(position())).isSolid()) {
            dropLanded = true;
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        // 掉出世界底部：这种情况下才移除
        if (getY() < level().getMinBuildHeight() - 16) {
            discard();
        }
    }

    // ==================== 6. 收回阶段 ====================

    private void tickRetracting(Player shooter) {
        Vec3 targetPos = shooter.getEyePosition().subtract(0, 0.2, 0);
        Vec3 currentPos = position();
        Vec3 toShooter = targetPos.subtract(currentPos);
        double remain = toShooter.length();

        // 足够近，或本 tick 会越过玩家（剩余距离小于步长）-> 直接结束，避免过冲后在两点间振荡
        double step = 2.0;
        if (remain < 1.2 || remain <= step) {
            discard();
            return;
        }

        Vec3 returnMotion = toShooter.normalize().scale(step);
        setDeltaMovement(returnMotion);
        move(net.minecraft.world.entity.MoverType.SELF, returnMotion);
    }

    /**
     * 双钩拴绳约束（单向刚性）：
     * - 连接建立时记录长度 leashLength（上限 20）；
     * - 两钩距离不超过 leashLength，超出即把两端“被钩住的东西”拉近；
     * - 距离小于 leashLength 时不做任何事（允许靠近）。
     */
    private static void constrainPair(GrapplingHookEntity a, GrapplingHookEntity b, double dist) {
        double limit = Math.min(20.0, Math.min(a.leashLength, b.leashLength));
        if (dist <= limit) {
            return;
        }
        double overshoot = dist - limit;
        double pull = Math.min(0.5, 0.12 + overshoot * 0.08);
        a.pullEntityOnly(b.position(), pull);
        b.pullEntityOnly(a.position(), pull);
    }

    /** 仅拉动“被钩住的实体”，不作用于发射者玩家（避免两个方向互相拉扯）。 */
    private void pullEntityOnly(Vec3 target, double speed) {
        if (hookedEntity instanceof LivingEntity living && living.isAlive()) {
            Vec3 dir = target.subtract(living.position());
            if (dir.lengthSqr() > 0.01) {
                living.setDeltaMovement(living.getDeltaMovement().add(dir.normalize().scale(speed)));
                living.hurtMarked = true;
            }
        }
    }

    // ==================== 网络同步与持久化 ====================

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        Entity owner = getOwner();
        buffer.writeInt(owner != null ? owner.getId() : entityData.get(DATA_OWNER_ID));
        buffer.writeItem(gunStack);
        buffer.writeInt(getHookState().ordinal());
        Vec3 anchor = getAnchorPos();
        buffer.writeFloat((float) anchor.x);
        buffer.writeFloat((float) anchor.y);
        buffer.writeFloat((float) anchor.z);
        buffer.writeInt(getHookMaterialId());
        GrapplingHookEntity partner = getPartnerHook();
        buffer.writeInt(partner != null ? partner.getId() : -1);
        // 快速模式标记：客户端渲染器 isDualHookMode() 需要它来区分「快速摆荡绳」与「双钩绳」
        buffer.writeBoolean(quickMode);
        buffer.writeBoolean(quickSecond);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        int ownerId = additionalData.readInt();
        if (ownerId != -1) {
            entityData.set(DATA_OWNER_ID, ownerId);
            Entity ent = level().getEntity(ownerId);
            if (ent instanceof Player player) {
                setOwner(player);
            }
        }
        this.gunStack = additionalData.readItem();
        int stateIdx = additionalData.readInt();
        if (stateIdx >= 0 && stateIdx < State.values().length) {
            setHookState(State.values()[stateIdx]);
        }
        float ax = additionalData.readFloat();
        float ay = additionalData.readFloat();
        float az = additionalData.readFloat();
        setAnchorPos(new Vec3(ax, ay, az));
        int matId = additionalData.readInt();
        entityData.set(DATA_HOOK_MATERIAL_ID, matId);
        int partnerId = additionalData.readInt();
        if (partnerId != -1) {
            entityData.set(DATA_PARTNER_HOOK_ID, partnerId);
        }
        this.quickMode = additionalData.readBoolean();
        this.quickSecond = additionalData.readBoolean();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(DATA_STATE, tag.getInt("HookState"));
        entityData.set(DATA_TARGET_TYPE, tag.getInt("TargetType"));
        entityData.set(DATA_ROPE_LENGTH, tag.getFloat("RopeLength"));
        entityData.set(DATA_HOOK_MATERIAL_ID, tag.getInt("MaterialId"));
        if (tag.contains("GunStack")) {
            gunStack = ItemStack.of(tag.getCompound("GunStack"));
        }
        if (tag.contains("SourceGun")) {
            sourceGun = ItemStack.of(tag.getCompound("SourceGun"));
        }
        if (tag.contains("HookedBlockPos")) {
            this.hookedBlockPos = BlockPos.of(tag.getLong("HookedBlockPos"));
        }
        this.dropLanded = tag.getBoolean("DropLanded");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("HookState", entityData.get(DATA_STATE));
        tag.putInt("TargetType", entityData.get(DATA_TARGET_TYPE));
        tag.putFloat("RopeLength", entityData.get(DATA_ROPE_LENGTH));
        tag.putInt("MaterialId", entityData.get(DATA_HOOK_MATERIAL_ID));
        if (!gunStack.isEmpty()) {
            tag.put("GunStack", gunStack.save(new CompoundTag()));
        }
        if (!sourceGun.isEmpty()) {
            tag.put("SourceGun", sourceGun.save(new CompoundTag()));
        }
        // 锚定方块坐标：重载后仍需保留，否则会误判“方块消失”
        if (hookedBlockPos != null) {
            tag.putLong("HookedBlockPos", hookedBlockPos.asLong());
        }
        tag.putBoolean("DropLanded", dropLanded);
    }
}
