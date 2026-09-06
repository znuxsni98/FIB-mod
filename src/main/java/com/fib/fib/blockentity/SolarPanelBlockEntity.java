package com.fib.fib.blockentity;

import com.fib.fib.init.ModBlockEntities;
import com.fib.fib.init.block.custom.Solar_Panel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

public class SolarPanelBlockEntity extends BlockEntity {

    // ==================== 发电参数常量 ====================
    private static final int MAX_ENERGY = 10000;
    private static final int BASE_GENERATION = 128;

    // 时间节点 (Minecraft dayTime % 24000)
    private static final long DAWN_START = 600L;        // 清晨开始
    private static final long MORNING_START = 1000L;    // 清晨结束 / 早晨开始
    private static final long NOON_START = 5000L;       // 早晨结束 / 正午开始
    private static final long NOON_END = 7000L;         // 正午结束 / 黄昏开始
    private static final long EVENING_START = 11000L;   // 黄昏结束 / 傍晚开始
    private static final long DAY_END = 12000L;         // 傍晚结束 / 完全停止
    private static final long NIGHT_START = 18000L;     // 深夜安全阈值

    // ==================== 能量存储 ====================
    private final CustomEnergyStorage energyStorage = new CustomEnergyStorage(MAX_ENERGY);
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);

    // ==================== 天空可见性缓存 ====================
    private boolean cachedCanSeeSky = false;
    private boolean needsSkyCheck = true;

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
        if (!getBlockState().getValue(Solar_Panel.HAS_CONTROLLER)) return;

        if (needsSkyCheck) {
            cachedCanSeeSky = level.canSeeSky(getBlockPos().above());
            needsSkyCheck = false;
        }

        int generation = calculateGeneration();
        if (generation > 0) {
            int received = energyStorage.receiveEnergy(generation, false);
            if (received > 0) {
                setChanged(); // 标记存档脏，但不立即发包
                // 每20tick(1秒)同步一次到客户端，而非每tick
                if (++syncCooldown >= 20) {
                    syncCooldown = 0;
                    level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                }
            }
        } else {
            // 不发电时也重置计数器，确保下次开始发电能及时同步
            syncCooldown = 0;
        }
    }

    // ==================== 核心发电计算 ====================
    private int calculateGeneration() {
        if (!(level instanceof ServerLevel serverLevel)) return 0;

        // 环境前置检查
        if (!serverLevel.dimension().equals(Level.OVERWORLD)) return 0;
        if (serverLevel.isRaining()) return 0;
        if (!cachedCanSeeSky) return 0;
        if (serverLevel.getBrightness(LightLayer.SKY, getBlockPos()) < 10) return 0;

        long dayTime = serverLevel.getDayTime() % 24000L;
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);

        // 夜间 & 日出前不发电
        if (dayTime >= NIGHT_START || dayTime < DAWN_START) return 0;
        // 日落后不发电
        if (dayTime >= DAY_END) return 0;

        double multiplier = getTimeDirectionMultiplier(dayTime, facing);
        if (multiplier <= 0.0) return 0;

        return (int) (BASE_GENERATION * multiplier);
    }

    // ==================== 平滑过渡乘数计算 ====================
    /**
     * 使用线性插值实现时间段之间的平滑过渡
     *
     * 更新后的规则:
     * ┌──────────────────────┬──────┬──────┬──────┬──────┐
     * │ 时间段                │ EAST │ WEST │ N/S  │ 说明  │
     * ├──────────────────────┼──────┼──────┼──────┼──────┤
     * │ 清晨 600~1000        │ 0.5  │ 0.2  │ 0.2  │ 线性升│
     * │ 早晨 1000~5000       │ 1.0  │  0   │ 0.4  │ 稳定  │
     * │ 正午 5000~7000       │ 1.3  │ 1.3  │ 1.3  │ 全向峰│
     * │ 黄昏 7000~11000      │  0   │ 1.0  │ 0.4  │ 稳定  │
     * │ 傍晚 11000~12000     │ 0.2  │ 0.5  │ 0.2  │ 线性降│
     * └──────────────────────┴──────┴──────┴──────┴──────┘
     */
    private double getTimeDirectionMultiplier(long dayTime, Direction facing) {
        boolean isEast = facing == Direction.EAST;
        boolean isWest = facing == Direction.WEST;
        boolean isNS = facing == Direction.NORTH || facing == Direction.SOUTH;

        // ---------- 清晨过渡期: 600 → 1000 ----------
        if (dayTime < MORNING_START) {
            // t: 0.0 → 1.0
            double t = (double) (dayTime - DAWN_START) / (MORNING_START - DAWN_START);
            if (isEast) return lerp(0.0, 0.5, t);   // 从0平滑升到0.5
            return lerp(0.0, 0.2, t);               // 其他方向从0平滑升到0.2
        }

        // ---------- 早晨: 1000 → 5000 ----------
        if (dayTime < NOON_START) {
            if (isEast) return 1.0;
            if (isNS) return 0.4;
            return 0.0;
        }

        // ---------- 正午: 5000 → 7000 ----------
        if (dayTime <= NOON_END) {
            return 1.3;
        }

        // ---------- 黄昏: 7000 → 11000 ----------
        if (dayTime < EVENING_START) {
            if (isWest) return 1.0;
            if (isNS) return 0.4;
            return 0.0;
        }

        // ---------- 傍晚过渡期: 11000 → 12000 ----------
        // t: 0.0 → 1.0
        double t = (double) (dayTime - EVENING_START) / (DAY_END - EVENING_START);
        if (isWest) return lerp(0.5, 0.0, t);       // 西向从0.5平滑降到0
        return lerp(0.2, 0.0, t);                   // 其他方向从0.2平滑降到0
    }

    /**
     * 线性插值: 当 t=0 返回 a, t=1 返回 b
     */
    private static double lerp(double a, double b, double t) {
        return a + (b - a) * Math.max(0.0, Math.min(1.0, t));
    }

    // ==================== 缓存刷新 ====================
    @Override
    public void setChanged() {
        super.setChanged();
        needsSkyCheck = true;
    }

    public void onNeighborChanged() {
        needsSkyCheck = true;
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
        needsSkyCheck = true; // ★ 修复1: load时必须刷新天空缓存
        syncCooldown = 0;     // 重置同步计数器
    }

    // ==================== Capability ====================
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
    }

    // ==================== 自定义 EnergyStorage ====================
    private static class CustomEnergyStorage extends EnergyStorage {
        public CustomEnergyStorage(int capacity) {
            super(capacity);
        }

        public void setEnergy(int energy) {
            this.energy = energy;
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

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        int energy = tag.getInt("Energy");
        energyStorage.setEnergy(Math.max(0, Math.min(energy, MAX_ENERGY)));
        needsSkyCheck = true; // 网络同步后刷新天空缓存
    }
}