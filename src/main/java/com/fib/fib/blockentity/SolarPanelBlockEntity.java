package com.fib.fib.blockentity;

import com.fib.fib.FIBMod;
import com.fib.fib.init.ModBlockEntities;
import com.fib.fib.init.block.custom.Solar_Panel;
import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class SolarPanelBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    // ==================== 发电参数常量 ====================
    private static final int MAX_ENERGY = 10000;
    private static final int BASE_GENERATION = 128;

    // 时间节点 (Minecraft dayTime % 24000)
    private static final long DAWN_START = 600L;        // 发电开始
    private static final long DAY_END = 12000L;         // 发电结束
    private static final long NIGHT_START = 18000L;     // 深夜安全阈值（18000~24000 视为夜间）

    // ==================== 能量存储 ====================
    private final CustomEnergyStorage energyStorage = new CustomEnergyStorage(MAX_ENERGY);
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    /** 只读视图：供 Jade 等 UI 从任意面读取电量显示；不允许任何方向传输/抽取 */
    private final LazyOptional<IEnergyStorage> readOnlyCapability =
            LazyOptional.of(() -> new ReadOnlyEnergyStorage(energyStorage));

    // ==================== ContainerData ====================
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored();
                case 1 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 仅客户端反序列化时由 addDataSlots 自动调用
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    // ==================== 构造器 ====================
    public SolarPanelBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.SOLAR_PANEL_BE.get(), pPos, pBlockState);
    }

    // ==================== Tick 主循环 ====================
// ★ 修复3: 增加发送节流，避免每tick都发包
    private int syncCooldown = 0;

    public void tick() {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(Solar_Panel.HAS_CONTROLLER)
                || !state.getValue(Solar_Panel.HAS_CONTROLLER)) return;

        // 1. 发电量取自快照（与护目镜显示同一套算法，见 computeSnapshot）
        int generation = computeSnapshot().totalGeneration();

        if (generation > 0) {
            // 储能"只出不进"，发电用内部 addEnergy 写入（不走 receiveEnergy，避免被外部注入影响）
            if (energyStorage.addEnergy(generation) > 0) {
                setChanged(); // 标记存档脏，但不立即发包
            }
        }

        // 每20tick(1秒)同步一次到客户端。放在发电分支外：满仓(addEnergy 返回 0)、夜里、雨天
        // 也要让护目镜看到最新储能，否则会停在"最后一次成功发电"的旧值上。
        if (++syncCooldown >= 20) {
            syncCooldown = 0;
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }

        // 2. 向后输出：移到所有发电早退分支之外 —— 夜里/雨天缓存电也要能送出去
        Direction back = state.getValue(Solar_Panel.FACING).getOpposite();
        BlockPos backPos = getBlockPos().relative(back);

        // 获取后方的能量接收能力
        BlockEntity backBE = level.getBlockEntity(backPos);
        if (backBE != null) {
            backBE.getCapability(ForgeCapabilities.ENERGY, back.getOpposite()).ifPresent(receiver -> {
                // 尝试从自身提取能量并注入到后方
                // extractEnergy 第二个参数 false 表示真实执行
                int extracted = energyStorage.extractEnergy(MAX_ENERGY, true); // 先模拟
                if (extracted > 0 && receiver.canReceive()) {
                    int received = receiver.receiveEnergy(extracted, false);
                    if (received > 0) {
                        energyStorage.extractEnergy(received, false); // 真实扣除
                        setChanged();
                    }
                }
            });
        }
    }

    /** 位置受光判定：白天能见天空 且 天空亮度 ≥ 10 */
    private boolean isLit(BlockPos pos, boolean canSeeSky) {
        return canSeeSky && level.getBrightness(LightLayer.SKY, pos) >= 10;
    }

    // ==================== 发电快照（服务端/客户端共用） ====================
    /**
     * 当前发电状态快照。
     * 计算用到的全部数据（世界时刻、天气、维度、天空可见性、天空亮度、方块状态）在客户端同样可得，
     * 因此护目镜可以直接调用本方法显示"实时"发电量，无需额外做网络同步。
     *
     * @param facing        朝向（护目镜显示方位）
     * @param hasController 是否装了控制器
     * @param perPanel      单块受光板发电量 FE/t
     * @param litPanels     阵列中受光的板数（含自己）
     * @param panelCount    阵列总板数（含自己）
     */
    public record GenerationSnapshot(Direction facing, boolean hasController,
                                     int perPanel, int litPanels, int panelCount) {
        /** 总发电量 FE/t = 单板发电 × 受光板数 */
        public int totalGeneration() {
            return perPanel * litPanels;
        }
    }

    public GenerationSnapshot computeSnapshot() {
        BlockState state = getBlockState();
        Direction facing = state.hasProperty(Solar_Panel.FACING)
                ? state.getValue(Solar_Panel.FACING) : Direction.NORTH;
        boolean hasController = state.hasProperty(Solar_Panel.HAS_CONTROLLER)
                && state.getValue(Solar_Panel.HAS_CONTROLLER);
        if (!hasController || level == null) {
            return new GenerationSnapshot(facing, false, 0, 0, 1);
        }

        // 阵列扫描两端都做（纯方块状态读取），失败时退化为"只有自己"
        List<BlockPos> linked = findLinkedPanels();
        int panelCount = 1 + linked.size();

        // 时间性环境（夜间/日出前/日落后）+ 维度 + 天气 不发电
        long dayTime = level.getDayTime() % 24000L;
        if (!level.dimension().equals(Level.OVERWORLD) || level.isRaining()
                || dayTime < DAWN_START || dayTime >= DAY_END) {
            return new GenerationSnapshot(facing, true, 0, 0, panelCount);
        }

        int perPanel = (int) (BASE_GENERATION * getTimeDirectionMultiplier(dayTime, facing));
        if (perPanel <= 0) {
            return new GenerationSnapshot(facing, true, 0, 0, panelCount);
        }

        // 天空/亮度实时查询：canSeeSky 是 O(1) 高度图查找，不再缓存，避免"遮挡拆除后不恢复发电"
        int litPanels = isLit(getBlockPos(), level.canSeeSky(getBlockPos().above())) ? 1 : 0;
        for (BlockPos slavePos : linked) {
            if (isLit(slavePos, level.canSeeSky(slavePos.above()))) {
                litPanels++;
            }
        }
        return new GenerationSnapshot(facing, true, perPanel, litPanels, panelCount);
    }

    // ==================== 阵列连接（控制器板 连接 无控制器板） ====================
    /**
     * 以控制器朝向为"前"，向左右两侧（垂直于朝向）连排扫描无控制器太阳能板。
     * 规则：
     *  1. 仅当 FACING 与控制器板一致 的板子可连；
     *  2. 中途遇到 非同朝向 / 非太阳能板 / 另一台控制器板 → 立即停止；
     *  3. 左右合计最多连接 MAX_LINKED_PANELS 块。
     */
    private static final int MAX_LINKED_PANELS = 4;

    private List<BlockPos> findLinkedPanels() {
        List<BlockPos> linked = new ArrayList<>();
        BlockState state = getBlockState();
        if (!state.hasProperty(Solar_Panel.HAS_CONTROLLER)
                || !state.getValue(Solar_Panel.HAS_CONTROLLER)) {
            return linked;
        }
        Direction facing = state.getValue(Solar_Panel.FACING);
        collectSide(facing.getCounterClockWise(), facing, linked); // 左
        collectSide(facing.getClockWise(), facing, linked);        // 右
        return linked;
    }

    private void collectSide(Direction dir, Direction requiredFacing, List<BlockPos> out) {
        BlockPos cursor = getBlockPos();
        for (int i = 0; i < MAX_LINKED_PANELS && out.size() < MAX_LINKED_PANELS; i++) {
            cursor = cursor.relative(dir);
            if (!isConnectableSlave(cursor, requiredFacing)) break;
            if (!isExclusiveSlave(cursor, dir, requiredFacing)) break; // 已被另一主机更优先占用 → 后面同侧只会更偏向对方
            out.add(cursor);
        }
    }

    /** 是"同朝向的无控制器太阳能板"才算可连接从板 */
    private boolean isConnectableSlave(BlockPos pos, Direction requiredFacing) {
        if (level == null) return false;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof Solar_Panel)) return false;
        if (state.getValue(Solar_Panel.HAS_CONTROLLER)) return false; // 另一台控制器：各自成组
        return state.getValue(Solar_Panel.FACING) == requiredFacing;
    }

    /**
     * 独占绑定判定：一块从板只能归属一台主机，避免两台主机重复计入同一从板。
     * 从该从板沿本主机方向继续向外找，若存在"同朝向"的另一台控制器：
     *  - 对方更近 → 不归本主机（return false）；
     *  - 等距 → 沿排坐标更小者（西/北）胜，保证确定性且全局不冲突。
     * 返回 true 表示本主机是这排里唯一(或最近/优先)能到达它的主机。
     */
    private boolean isExclusiveSlave(BlockPos slave, Direction dir, Direction facing) {
        if (level == null) return true;
        BlockPos p = slave;
        int otherDist = 0;
        BlockPos otherMaster = null;
        for (int i = 0; i < MAX_LINKED_PANELS; i++) {
            p = p.relative(dir);
            otherDist++;
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof Solar_Panel)) break;
            if (s.getValue(Solar_Panel.HAS_CONTROLLER)) {
                if (s.getValue(Solar_Panel.FACING) == facing) otherMaster = p;
                break; // 任何控制器都截断链条
            }
            if (s.getValue(Solar_Panel.FACING) != facing) break; // 朝向不同的板也截断
        }
        if (otherMaster == null) return true;

        int myDist = slave.distManhattan(getBlockPos());
        if (myDist != otherDist) return myDist < otherDist;

        Direction.Axis axis = facing.getClockWise().getAxis();
        return rowCoord(getBlockPos(), axis) < rowCoord(otherMaster, axis);
    }

    private static int rowCoord(BlockPos pos, Direction.Axis axis) {
        return axis == Direction.Axis.X ? pos.getX() : pos.getZ();
    }

    // ==================== 发电乘数（按时刻连续折线） ====================
    // 用一组关键点(时刻, 乘数)做线性插值 → 全段连续，时间段边界不再跳变。
    // 想调曲线只改下面三组乘数（时刻表共享，三组与时刻数量须一致）。
    // 形状：正午(6000)三向都到 1.3；早晨东向偏高、傍晚西向偏高、南北向居中；
    //       太阳在背面的时段保留约 0.08 的微量发电（沿用你之前的手调值），黎明/黄昏归 0。
    private static final long[] KNOT_TIMES = {600L, 1000L, 3000L, 5000L, 6000L, 7000L, 9000L, 11000L, 12000L};
    private static final double[] EAST_KNOTS = {0.00, 0.50, 1.10, 1.25, 1.30, 0.20, 0.08, 0.08, 0.00};
    private static final double[] WEST_KNOTS = {0.00, 0.08, 0.08, 0.35, 1.30, 1.30, 1.10, 0.45, 0.00};
    private static final double[] NS_KNOTS   = {0.00, 0.25, 0.65, 1.00, 1.30, 1.00, 0.60, 0.25, 0.00};

    private double getTimeDirectionMultiplier(long dayTime, Direction facing) {
        double[] knots;
        if (facing == Direction.EAST) {
            knots = EAST_KNOTS;
        } else if (facing == Direction.WEST) {
            knots = WEST_KNOTS;
        } else {
            knots = NS_KNOTS; // 南北同值
        }

        // 找所在时刻区间线性插值（dayTime 已被 tick 限定在 [600, 12000)）
        for (int i = 0; i < KNOT_TIMES.length - 1; i++) {
            if (dayTime <= KNOT_TIMES[i + 1]) {
                double t = (double) (dayTime - KNOT_TIMES[i]) / (KNOT_TIMES[i + 1] - KNOT_TIMES[i]);
                return lerp(knots[i], knots[i + 1], t);
            }
        }
        return knots[knots.length - 1];
    }

    /**
     * 线性插值: 当 t=0 返回 a, t=1 返回 b
     */
    private static double lerp(double a, double b, double t) {
        return a + (b - a) * Math.max(0.0, Math.min(1.0, t));
    }

    // ==================== NBT 持久化 ====================
    @Override
    protected void saveAdditional(@NotNull CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.putInt("Energy", energyStorage.getEnergyStored());
    }

    @Override
    public void load(@NotNull CompoundTag pTag) {
        super.load(pTag);
        int energy = pTag.getInt("Energy");
        energyStorage.setEnergy(Math.max(0, Math.min(energy, MAX_ENERGY)));
        syncCooldown = 0;     // 重置同步计数器
    }

    // ==================== Capability ====================
    /**
     * 能量端口规则：
     *  1. 未安装控制器 → 不暴露能量端口（线缆无法连接、残留电也无法被抽取）；
     *  2. 已安装控制器 → 只有"背面"（FACING 反方向）能真正传输/抽取（返回完整储能）；
     *     其余侧面/顶/底面返回"只读视图"——Jade 等 UI 仍能读到电量，但无法被抽走或注入。
     */
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            BlockState state = getBlockState();
            if (!state.hasProperty(Solar_Panel.HAS_CONTROLLER)
                    || !state.getValue(Solar_Panel.HAS_CONTROLLER)) {
                return LazyOptional.empty();
            }
            // 只有背面给完整储能；side == null 或其它面给只读视图（能读不能抽/注）
            if (side == state.getValue(Solar_Panel.FACING).getOpposite()) {
                return energyCapability.cast();
            }
            return readOnlyCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
        readOnlyCapability.invalidate();
    }

    // ==================== 自定义 EnergyStorage ====================
    /**
     * 只出不进的"发电机式"储能：
     *  super(capacity, 0, capacity) → 外部 maxReceive=0（无法向面板注电），maxExtract=容量；
     *  发电由内部 addEnergy() 写入，满则忽略本次发电。
     */
    private static class CustomEnergyStorage extends EnergyStorage {
        public CustomEnergyStorage(int capacity) {
            super(capacity, 0, capacity);
        }

        public void setEnergy(int energy) {
            this.energy = energy;
        }

        /** 内部发电写入，返回实际写入量（已满则返回 0） */
        public int addEnergy(int amount) {
            int added = Math.min(amount, Math.max(capacity - energy, 0));
            energy += added;
            return added;
        }
    }

    // ==================== 只读能量视图（UI 显示用） ====================
    /**
     * 所有传输/注入方法一律失效，仅透传读取电量——
     * 从任意面调用都不会真正抽走或灌入能量，只为 Jade 等工具显示能量条。
     */
    private static class ReadOnlyEnergyStorage implements IEnergyStorage {
        private final IEnergyStorage delegate;

        public ReadOnlyEnergyStorage(IEnergyStorage delegate) {
            this.delegate = delegate;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return delegate.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return delegate.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }


    // ==================== 客户端同步（仅保留 Energy） ====================
    // hasController 已移至 BlockState，由引擎自动同步，无需手动处理

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("Energy", energyStorage.getEnergyStored());
        return tag;
    }

    /**
     * ★ 关键：服务端把方块实体的 NBT 推给客户端靠的就是这个包。
     * BlockEntity.getUpdatePacket() 默认返回 null，而 ChunkHolder.broadcastBlockEntity 只在该包
     * 非 null 时才发送 —— 所以只调 level.sendBlockUpdated(...) 只广播了方块状态、BE 数据不会跟着走，
     * 客户端那边的储能就一直是 0（Jade 读服务端能力，所以它看着正常，护目镜会露馅）。
     */
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        int energy = tag.getInt("Energy");
        energyStorage.setEnergy(Math.max(0, Math.min(energy, MAX_ENERGY)));
    }
    //========================== 护目镜 =====================
    /** 本模组命名空间的语言构建器（LangBuilder 会把 key 拼成 "fib_mod.<key>"） */
    private static LangBuilder lang() {
        return new LangBuilder(FIBMod.MOD_ID);
    }

    /**
     * 机械动力护目镜显示：发电量 / 朝向 / 阵列 / 储能。
     * 由 GoggleOverlayRenderer 在玩家戴着护目镜看向本方块时调用（纯客户端），
     * 数据来自 computeSnapshot()，两端算法一致，所以显示的就是实时值。
     *
     * @return true 表示本次构建了内容，返回 false 则护目镜不显示
     */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        BlockState state = getBlockState();
        boolean hasController = state.hasProperty(Solar_Panel.HAS_CONTROLLER)
                && state.getValue(Solar_Panel.HAS_CONTROLLER);

        // 从板（无控制器）：不独立发电，只提示归属
        if (!hasController) {
            lang().translate("gui.goggles.solar_panel.slave")
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip);
            return true;
        }

        GenerationSnapshot snap = computeSnapshot();

        lang().translate("gui.goggles.solar_panel.stats").forGoggles(tooltip);

        // 发电量： 1,024 FE/t
        lang().translate("gui.goggles.generation").style(ChatFormatting.GRAY)
                .space()
                .add(CreateLang.number(snap.totalGeneration()).style(ChatFormatting.AQUA))
                .space()
                .add(lang().translate("generic.unit.fe_per_tick").style(ChatFormatting.GOLD))
                .forGoggles(tooltip, 1);

        // 朝向： 东
        lang().translate("gui.goggles.facing").style(ChatFormatting.GRAY)
                .space()
                .add(lang().translate("direction." + snap.facing().getName()).style(ChatFormatting.AQUA))
                .forGoggles(tooltip, 1);

        // 阵列： 受光 3 / 共 5 块
        lang().translate("gui.goggles.solar_panel.array",
                        snap.litPanels(), snap.panelCount())
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip, 1);

        // 储能： 3,200 / 10,000 FE
        lang().translate("gui.goggles.energy").style(ChatFormatting.GRAY)
                .space()
                .add(CreateLang.number(energyStorage.getEnergyStored()).style(ChatFormatting.AQUA))
                .text(ChatFormatting.DARK_GRAY, " / ")
                .add(CreateLang.number(energyStorage.getMaxEnergyStored()).style(ChatFormatting.DARK_GRAY))
                .space()
                .add(lang().translate("generic.unit.fe").style(ChatFormatting.GOLD))
                .forGoggles(tooltip, 1);

        return true;
    }
}