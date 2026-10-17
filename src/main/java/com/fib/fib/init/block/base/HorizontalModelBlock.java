package com.fib.fib.init.block.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 水平四向旋转装饰方块通用基类 (Horizontal Model Block)。
 * 封装水平朝向 (FACING) 的放置判断、旋转镜像与 BlockState 注册逻辑，提供统一的碰撞箱支持。
 */
public class HorizontalModelBlock extends HorizontalDirectionalBlock {
    private final VoxelShape shapeN, shapeS, shapeW, shapeE;

    /** 统一碰撞箱构造：四向使用同一个对称碰撞箱 */
    public HorizontalModelBlock(Properties properties, VoxelShape uniformShape) {
        this(properties, uniformShape, uniformShape, uniformShape, uniformShape);
    }

    /** 四向独立碰撞箱构造：四向各自有不同形状 */
    public HorizontalModelBlock(Properties properties, VoxelShape n, VoxelShape s, VoxelShape w, VoxelShape e) {
        super(properties);
        this.shapeN = n;
        this.shapeS = s;
        this.shapeW = w;
        this.shapeE = e;
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> shapeS;
            case WEST -> shapeW;
            case EAST -> shapeE;
            default -> shapeN;
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
