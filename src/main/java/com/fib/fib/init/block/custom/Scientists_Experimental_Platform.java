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

import java.util.stream.Stream;

public class Scientists_Experimental_Platform extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape SHAPE_S =Stream.of(Block.box(-15, 17, 0, -3, 26, 10), Block.box(-16, 0, 0, 16, 17, 16), Block.box(-15, 16.464466094067262, 7.542893218813452, -3, 19.464466094067262, 11.917893218813452), Block.box(-1, 16, 0, 11, 20, 3)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape SHAPE_W =Stream.of(Block.box(6, 17, -15, 16, 26, -3), Block.box(0, 0, -16, 16, 17, 16), Block.box(4.082106781186548, 16.464466094067262, -15, 8.457106781186548, 19.464466094067262, -3), Block.box(13, 16, -1, 16, 20, 11)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape SHAPE_E =Stream.of(Block.box(0, 17, 19, 10, 26, 31), Block.box(0, 0, 0, 16, 17, 32), Block.box(7.542893218813452, 16.464466094067262, 19, 11.917893218813452, 19.464466094067262, 31), Block.box(0, 16, 5, 3, 20, 17)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape SHAPE_N =Stream.of(Block.box(19, 17, 6, 31, 26, 16), Block.box(0, 0, 0, 32, 17, 16), Block.box(19, 16.464466094067262, 4.082106781186548, 31, 19.464466094067262, 8.457106781186548), Block.box(5, 16, 13, 17, 20, 16)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public Scientists_Experimental_Platform(Properties pProperties) {
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
