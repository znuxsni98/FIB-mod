package com.fib.fib.init.block.custom;

import com.fib.fib.init.ModBlockEntities;
import com.fib.fib.blockentity.Radio_StationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import org.jetbrains.annotations.Nullable;

public class Radio_Station extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape SHAPE_S =Block.box(0, 0, 0, 32, 13, 16);
    public static final VoxelShape SHAPE_W =Block.box(0, 0, 0, 16, 13, 32);
    public static final VoxelShape SHAPE_E =Block.box(0, 0, -16, 16, 13, 16);
    public static final VoxelShape SHAPE_N =Block.box(-16, 0, 0, 16, 13, 16);
    public Radio_Station(Properties pProperties) {
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

        /**
         * 当方块被放置到世界中时调用。
         *
         * 这个方法负责告诉游戏：
         * “这个方块在该位置应该创建哪一种 BlockEntity。”
         *
         * 每一个拥有 BlockEntity 的方块，都必须实现这个方法，
         * 否则即使注册了 BlockEntityType，世界中也不会真正生成实体。
         */

        return new Radio_Station(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level pLevel,
            BlockState pState,
            BlockEntityType<T> pBlockEntityType) {



        return pBlockEntityType == ModBlockEntities.RADIO_STATION_BE.get()
                // 类型匹配时，每 tick 调用我们的 BlockEntity.tick()
                ? (lvl, pos, state, be) ->
                ((Radio_StationBlockEntity) be).tick()
                : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level,
                                 BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {

        /**
         * 玩家右键方块时触发。
         *
         * 在本章中我们还没有 GUI，因此使用聊天信息作为
         * 最简单的“可视化调试方式”，用于观察 BlockEntity 内部数据。
         */

        // 只在服务端执行，避免客户端与服务端重复发送消息
        if (!level.isClientSide) {

            // 获取当前位置的 BlockEntity
            BlockEntity be = level.getBlockEntity(pos);

            // 判断是否为我们的工业处理单元实体
            if (be instanceof Radio_StationBlockEntity machine) {

                // 输出当前进度信息，用于验证 tick 与 NBT 是否正常工作
                player.sendSystemMessage(machine.getDebugMessages());
            }
        }

        return InteractionResult.SUCCESS;
    }
}
