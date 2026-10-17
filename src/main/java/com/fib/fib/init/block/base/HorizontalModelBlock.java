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
 * --------------------------------------------------------------------------
 * 🧱 【新人专用基类】自带东南西北四向水平旋转的装饰模型方块
 * --------------------------------------------------------------------------
 * 【初中生大白话讲解】：
 * 以前每次加一个桌子、椅子、箱子，都要从头写 60 行代码（定义 FACING、覆写 rotate、
 * mirror、getStateForPlacement、createBlockStateDefinition）。只要有一行抄漏了，
 * 方块放下去不是方向不对就是游戏直接崩溃！
 * 
 * 现在只要继承本类（或者直接 new HorizontalModelBlock），传一个碰撞箱即可：
 * 一行代码就能拥有完整的四向旋转与状态注册！
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
