//package com.fib.fib.blockentity;
//
//import com.fib.fib.init.ModBlockEntities;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraftforge.common.capabilities.Capability;
//import net.minecraftforge.common.capabilities.ForgeCapabilities;
//import net.minecraftforge.common.util.LazyOptional;
//import net.minecraftforge.fluids.FluidStack;
//import net.minecraftforge.fluids.capability.IFluidHandler;
//import net.minecraftforge.fluids.capability.templates.FluidTank;
//
//import javax.annotation.Nonnull;
//import javax.annotation.Nullable;
//
//public class OilDrumBlockEntity extends BlockEntity {
//
//    // 定义流体容量（16000 mB = 16 桶）
//    private final FluidTank fluidTank = new FluidTank(16000) {
//        @Override
//        protected void onContentsChanged() {
//            setChanged(); // 标记数据变更，触发保存与同步
//            if (level != null && !level.isClientSide) {
//                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
//            }
//        }
//    };
//
//    // 2. 构造函数：调用父类并传入正确的注册对象
//    public OilDrumBlockEntity(BlockPos pPos, BlockState pState) {
//        super(ModBlockEntities.OIL_DRUM_BE.get(), pPos, pState); // 注意这里要用具体的 RegistryObject
//    }
//
//    // 3. 实现流体交互接口
//    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
//        return fluidTank.fill(resource, action);
//    }
//
//    public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
//        return fluidTank.drain(resource, action);
//    }
//
//    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
//        return fluidTank.drain(maxDrain, action);
//    }
//
//    // 4. 暴露流体能力（Capability）
//    @Nonnull
//    @Override
//    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
//        if (cap == ForgeCapabilities.FLUID_HANDLER) {
//            return LazyOptional.of(() -> fluidTank).cast();
//        }
//        return super.getCapability(cap, side);
//    }
//
//    // 5. 数据持久化（保存与读取 NBT）
//    @Override
//    protected void saveAdditional(CompoundTag tag) {
//        super.saveAdditional(tag);
//        tag.put("FluidTank", fluidTank.writeToNBT(new CompoundTag()));
//    }
//
//    @Override
//    public void load(CompoundTag tag) {
//        super.load(tag);
//        fluidTank.readFromNBT(tag.getCompound("FluidTank"));
//    }
//
//    // 6. 客户端数据同步
//    @Override
//    public CompoundTag getUpdateTag() {
//        return saveWithoutMetadata();
//    }
//
//    @Override
//    public void handleUpdateTag(CompoundTag tag) {
//        load(tag);
//    }
//
//    @Override
//    public ClientboundBlockEntityDataPacket getUpdatePacket() {
//        return ClientboundBlockEntityDataPacket.create(this);
//    }
//
//}