package com.fib.fib.blockentity;

import com.fib.fib.FIBMod;
import com.fib.fib.init.ModBlockEntities;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.CreateLang;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.List;

public class RTGBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float DEFAULT_TEMP = 20.0f;
    private static final int MAX_STORAGE = 20000; // 储能20KFE

    // === 发电量参数（FE/tick）===
    private static final int GEN_AT_ZERO_TEMP = 3000;   // 0°C 时的发电量
    private static final int GEN_PER_DEGREE = 60;       // 每偏离 0°C 1 度的增减量

    // === 自定义能量存储 ===
    private final InternalEnergyStorage energyStorage = new InternalEnergyStorage(MAX_STORAGE);
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energyStorage);

    // === 温度 API 反射 ===
    private static volatile Method getTempMethod;
    private static volatile boolean tempApiResolved = false;

    private float cachedTemperature = DEFAULT_TEMP;
    private int temperatureUpdateTimer = 0; // 新增：温度更新计时器
    private int syncCooldown = 0;           // 护目镜显示用的节流同步计数器

    public RTGBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTG_BE.get(), pos, state);
    }

    // ==================== 自定义 EnergyStorage ====================
    private static class InternalEnergyStorage extends EnergyStorage {
        public InternalEnergyStorage(int capacity) {
            // 参数顺序: (capacity, maxReceive, maxExtract, energy)
            // ★ maxExtract 必须 > 0，否则 canExtract()=false、extractEnergy() 恒返回 0，
            //   导致 pushEnergy() 把电送出去后自己扣不掉 —— 这正是"输出不扣电"的根因。
            super(capacity, 0, capacity, 0);
        }

        public void setEnergyStored(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }

        @Override
        public boolean canReceive() {
            return false; // 本机只发电，不接受外部输入
        }
    }

    // ==================== 每 Tick 发电 ====================
    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        // 1. 每 10 tick 更新一次温度
        temperatureUpdateTimer++;
        if (temperatureUpdateTimer >= 10) {
            cachedTemperature = getEnvironmentTemperature();
            temperatureUpdateTimer = 0;
        }

        // 2. 每 tick 计算发电量
        int generation = calculateGeneration(cachedTemperature);

        if (generation > 0) {
            // 注意：这里直接设置能量，如果超过上限会被 setEnergyStored 截断
            // 如果需要更平滑的溢出处理，可以改为 receiveEnergy
            int newEnergy = Math.min(energyStorage.getEnergyStored() + generation, MAX_STORAGE);
            energyStorage.setEnergyStored(newEnergy);
        }

        // 3. 输出能量
        pushEnergy();

        // 4. 每 20 tick 同步一次温度/储能给客户端 —— 客户端不跑 serverTick，
        //    不同步的话护目镜上的温度和发电量会一直是旧值。
        if (++syncCooldown >= 20) {
            syncCooldown = 0;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        setChanged();
    }

    /**
     * 按温度计算每 tick 发电量（FE/t）：
     *  - 0°C        → 3000
     *  - 温度 > 0   → 每升高 1 度，发电量 -60
     *  - 温度 < 0   → 每降低 1 度，发电量 +60
     * 统一为： gen = 3000 - 60 × 温度，并对下限取 0（不产生负发电）。
     */
    private int calculateGeneration(float temperature) {
        int gen = GEN_AT_ZERO_TEMP - Math.round(temperature * GEN_PER_DEGREE);
        return Math.max(gen, 0);
    }

    // ==================== 能量输出 ====================
    private void pushEnergy() {
        if (energyStorage.getEnergyStored() <= 0) return;

        Direction outputDir = getOutputDirection();
        BlockPos targetPos = worldPosition.relative(outputDir);
        BlockEntity neighbor = level.getBlockEntity(targetPos);

        if (neighbor == null) return;

        neighbor.getCapability(ForgeCapabilities.ENERGY, outputDir.getOpposite()).ifPresent(handler -> {
            if (handler.canReceive()) {
                // 获取当前所有存储的能量，不再限制每次发送量
                int toSend = energyStorage.getEnergyStored();

                // 尝试发送所有能量
                int accepted = handler.receiveEnergy(toSend, false);

                if (accepted > 0) {
                    energyStorage.extractEnergy(accepted, false);
                }
            }
        });
    }

    /**
     * 获取输出方向：FACING 的顺时针方向（即方块左侧）
     * 保持原有逻辑不变
     */
    private Direction getOutputDirection() {
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        return facing.getClockWise();
    }

    // ==================== 温度 API 反射 ====================
    private float getEnvironmentTemperature() {
        if (!tempApiResolved) {
            try {
                Class<?> clazz = Class.forName("sfiomn.legendarysurvivaloverhaul.api.temperature.TemperatureUtil");
                getTempMethod = clazz.getMethod("getWorldTemperature", net.minecraft.world.level.Level.class, BlockPos.class);
                LOGGER.info("[RTG] Resolved TemperatureUtil.getWorldTemperature(Level, BlockPos)");
            } catch (ClassNotFoundException e) {
                LOGGER.error("[RTG] SFIOMN TemperatureUtil not found! Default {}°C", DEFAULT_TEMP);
            } catch (NoSuchMethodException e) {
                LOGGER.error("[RTG] getWorldTemperature not found! Default {}°C", DEFAULT_TEMP);
            }
            tempApiResolved = true;
        }
        if (getTempMethod != null && level != null) {
            try {
                Object result = getTempMethod.invoke(null, level, worldPosition);
                if (result instanceof Number num) {
                    return num.floatValue();
                }
            } catch (Exception e) {
                LOGGER.error("[RTG] Failed to invoke getWorldTemperature at {}", worldPosition, e);
            }
        }
        return DEFAULT_TEMP;
    }

    // ==================== NBT 持久化 ====================
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putFloat("CachedTemp", cachedTemperature);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int stored = tag.contains("Energy") ? tag.getInt("Energy") : 0;
        energyStorage.setEnergyStored(stored);
        cachedTemperature = tag.contains("CachedTemp") ? tag.getFloat("CachedTemp") : DEFAULT_TEMP;
    }

    // ==================== Capability ====================
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    // ==================== 客户端同步 ====================
    /**
     * 同步给客户端的轻量数据。默认实现返回空 tag，客户端拿不到温度，
     * 护目镜就会一直显示兜底温度，故这里显式带上温度与储能。
     * 客户端由默认的 handleUpdateTag → load(tag) 反序列化。
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putFloat("CachedTemp", cachedTemperature);
        return tag;
    }

    /**
     * ★ 关键：服务端把方块实体的 NBT 推给客户端靠的就是这个包。
     * BlockEntity.getUpdatePacket() 默认返回 null，而 ChunkHolder.broadcastBlockEntity 只在该包
     * 非 null 时才发送 —— 所以只调 level.sendBlockUpdated(...) 只会广播方块状态、BE 数据不会跟着走，
     * 客户端温度就永远是兜底的 20°C（显示成 1800 FE/t）、储能永远是 0。
     */
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public float getCachedTemperature() {
        return cachedTemperature;
    }

    public int getEnergyStored() {
        return energyStorage.getEnergyStored();
    }

    /** 当前发电量 FE/t（客户端用同步过来的温度即可算出同样的值） */
    public int getCurrentGeneration() {
        return calculateGeneration(cachedTemperature);
    }

    // ==================== 护目镜显示 ====================
    /** 本模组命名空间的语言构建器（LangBuilder 会把 key 拼成 "fib_mod.<key>"） */
    private static LangBuilder lang() {
        return new LangBuilder(FIBMod.MOD_ID);
    }

    /**
     * 机械动力护目镜显示：发电量 / 温度 / 储能。
     * 纯客户端调用；温度与储能由 serverTick 每 20 tick 同步一次。
     *
     * @return true 表示本次构建了内容，返回 false 则护目镜不显示
     */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        lang().translate("gui.goggles.rtg.stats").forGoggles(tooltip);

        // 发电量： 3,000 FE/t
        lang().translate("gui.goggles.generation").style(ChatFormatting.GRAY)
                .space()
                .add(CreateLang.number(getCurrentGeneration()).style(ChatFormatting.AQUA))
                .space()
                .add(lang().translate("generic.unit.fe_per_tick").style(ChatFormatting.GOLD))
                .forGoggles(tooltip, 1);

        // 温度： 20.0 °C
        lang().translate("gui.goggles.temperature").style(ChatFormatting.GRAY)
                .space()
                .add(lang().text(String.format("%.1f", cachedTemperature)).style(ChatFormatting.AQUA))
                .space()
                .add(lang().translate("generic.unit.celsius").style(ChatFormatting.GOLD))
                .forGoggles(tooltip, 1);

        // 储能： 3,200 / 20,000 FE
        lang().translate("gui.goggles.energy").style(ChatFormatting.GRAY)
                .space()
                .add(CreateLang.number(energyStorage.getEnergyStored()).style(ChatFormatting.AQUA))
                .text(ChatFormatting.DARK_GRAY, " / ")
                .add(CreateLang.number(MAX_STORAGE).style(ChatFormatting.DARK_GRAY))
                .space()
                .add(lang().translate("generic.unit.fe").style(ChatFormatting.GOLD))
                .forGoggles(tooltip, 1);

        return true;
    }
}