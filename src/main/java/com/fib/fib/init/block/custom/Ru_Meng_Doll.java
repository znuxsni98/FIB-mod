package com.fib.fib.init.block.custom;

import com.fib.fib.blockentity.RuMengDollBlockEntity;
import com.fib.fib.init.item.custom.DollBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Ru_Meng_Doll extends BaseEntityBlock {

    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;

    private static final int ROTATIONS = 16;

    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 15, 13);

    public Ru_Meng_Doll(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState().setValue(ROTATION, 0));
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    /**
     * 16 个转向（每 22.5° 一个）无法用方块状态变体表达（变体旋转只支持 90° 倍数），
     * 所以改为方块实体渲染：模型统一在 BlockEntityRenderer 里按 22.5°×rotation 旋转。
     */
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new RuMengDollBlockEntity(pPos, pState);
    }

    /**
     * 创造模式选中拾取时，把方块实体里的预设数据带回物品 NBT。
     */
    @Override
    public ItemStack getCloneItemStack(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        if (pLevel.getBlockEntity(pPos) instanceof RuMengDollBlockEntity be) {
            return DollBlockItem.createPreset(this, be.getPreset(), be.getSkin(), be.getName(), be.getDescription());
        }
        return super.getCloneItemStack(pLevel, pPos, pState);
    }

    /**
     * 生存模式敲掉时，掉落物也要带上方块实体的预设 NBT，
     * 否则放下去再敲回来数据就丢了。
     */
    @Override
    public List<ItemStack> getDrops(BlockState pState, LootParams.Builder pBuilder) {
        if (pBuilder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof RuMengDollBlockEntity be) {
            return List.of(DollBlockItem.createPreset(this, be.getPreset(), be.getSkin(), be.getName(), be.getDescription()));
        }
        return super.getDrops(pState, pBuilder);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        // 模型默认正面朝北（-Z，南北正放正常可证），而渲染器 Axis.YP 的旋转方向
        // 与 MC 偏航相反，所以必须取反（360-偏航），而不是加 180° 偏移。
        // 取反后：北/南不变，东/西与斜向都脸朝玩家。
        return this.defaultBlockState().setValue(ROTATION,
                Mth.floor(((360.0F - pContext.getRotation()) * 16.0F / 360.0F) + 0.5D) & 15);
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(ROTATION,
                pRotation.rotate(pState.getValue(ROTATION), ROTATIONS));
    }

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.setValue(ROTATION,
                pMirror.mirror(pState.getValue(ROTATION), ROTATIONS));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(ROTATION);
    }
}
