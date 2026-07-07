package com.fib.fib.init.block.custom.corpse;


import com.fib.fib.blockentity.corpse.Corpse2BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Corpse_2 extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape SHAPE_S =Shapes.join(Block.box(1, 0, 4, 15, 4, 14), Block.box(1, 0, -5, 15, 10, 4), BooleanOp.OR);
    public static final VoxelShape SHAPE_W =Shapes.join(Block.box(2, 0, 0.9375, 12, 4, 14.9375), Block.box(12, 0, 0.9375, 21, 10, 14.9375), BooleanOp.OR);
    public static final VoxelShape SHAPE_E =Shapes.join(Block.box(4, 0, 0.9375, 14, 4, 14.9375), Block.box(-5, 0, 0.9375, 4, 10, 14.9375), BooleanOp.OR);
    public static final VoxelShape SHAPE_N =Shapes.join(Block.box(1, 0, 2, 15, 4, 12), Block.box(1, 0, 12, 15, 10, 21), BooleanOp.OR);
    public Corpse_2(Properties pProperties) {
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





    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new Corpse2BlockEntity(blockPos, blockState);
    }


    @Override
    public InteractionResult use(BlockState state, Level level,
                                 BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {

        // 方块被玩家右键时打开 GUI 的入口
        if (!level.isClientSide()) {

            // 获取当前位置绑定的 BlockEntity
            BlockEntity entity = level.getBlockEntity(pos);

            // 确认该实体确实是正确方块
            if (entity instanceof Corpse2BlockEntity juicer) {

                player.openMenu(juicer);
            } else {
                // 如果当前位置没有正确的 BlockEntity，
                // 说明出现了逻辑错误，直接抛出异常。
                throw new IllegalStateException("Missing Container!");
            }
        }

        // 返回交互结果。
        // sidedSuccess 会在客户端和服务端分别返回正确的结果，
        // 保证交互逻辑在两端保持一致。
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof Corpse2BlockEntity crateEntity) {
                Containers.dropContents(level, pos, crateEntity.getInventory());
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

}

