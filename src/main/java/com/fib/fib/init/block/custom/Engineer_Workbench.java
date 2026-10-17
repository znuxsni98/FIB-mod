package com.fib.fib.init.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class Engineer_Workbench extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // 单方块工作台碰撞箱限制在 [0, 16] 网格内，防止隐形超界碰撞体卡死村民认领工作台的寻路
    public static final VoxelShape SHAPE_S = Shapes.join(Block.box(0, 0, 0, 16, 16, 16), Block.box(0, 16, 0, 16, 24, 2), BooleanOp.OR);
    public static final VoxelShape SHAPE_W = Shapes.join(Block.box(0, 0, 0, 16, 16, 16), Block.box(14, 16, 0, 16, 24, 16), BooleanOp.OR);
    public static final VoxelShape SHAPE_E = Shapes.join(Block.box(0, 0, 0, 16, 16, 16), Block.box(0, 16, 0, 2, 24, 16), BooleanOp.OR);
    public static final VoxelShape SHAPE_N = Shapes.join(Block.box(0, 0, 0, 16, 16, 16), Block.box(0, 16, 14, 16, 24, 16), BooleanOp.OR);
    public Engineer_Workbench(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return switch (pState.getValue(FACING)) {
            case SOUTH -> SHAPE_S;
            case WEST -> SHAPE_W;
            case EAST -> SHAPE_E;
            default -> SHAPE_N;
        };
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING);
    }
}
