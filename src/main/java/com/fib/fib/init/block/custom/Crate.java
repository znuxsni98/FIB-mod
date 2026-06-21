package com.fib.fib.init.block.custom;


import com.fib.fib.blockentity.CrateBlockEntity;
import com.fib.fib.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Crate extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape SHAPE_S =Block.box(0, 0, 0, 16, 16, 16);
    public static final VoxelShape SHAPE_W =Block.box(0, 0, 0, 16, 16, 16);
    public static final VoxelShape SHAPE_E =Block.box(0, 0, 0, 16, 16, 16);
    public static final VoxelShape SHAPE_N =Block.box(0, 0, 0, 16, 16, 16);
    public Crate(Properties pProperties) {
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
        return new CrateBlockEntity(blockPos, blockState);
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
            if (entity instanceof CrateBlockEntity juicer) {

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

            if (blockEntity instanceof CrateBlockEntity crateEntity) {
                Containers.dropContents(level, pos, crateEntity.getInventory());
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

}

