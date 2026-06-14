package com.fib.fib.init.block.custom;

import com.fib.fib.blockentity.IceMakerBlockEntity;
import com.fib.fib.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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
import org.jetbrains.annotations.Nullable;

public class Ice_Maker extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final VoxelShape SHAPE_S =Block.box(0, 0, 0, 16, 16, 16);
    public static final VoxelShape SHAPE_W =Block.box(0, 0, 0, 16, 16, 16);
    public static final VoxelShape SHAPE_E =Block.box(0, 0, 0, 16, 16, 16);
    public static final VoxelShape SHAPE_N =Block.box(0, 0, 0, 16, 16, 16);
    public Ice_Maker(Properties pProperties) {
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
        return new IceMakerBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level pLevel,
            BlockState pState,
            BlockEntityType<T> pBlockEntityType) {


        return pBlockEntityType == ModBlockEntities.ICE_MAKER_BE.get()
                ? (lvl, pos, state, be) ->
                ((IceMakerBlockEntity) be).tick()
                : null;
    }



    @Override
    public InteractionResult use(BlockState state, Level level,
                                 BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {

        // 方块被玩家右键时调用。
        // 这里我们用它作为打开 GUI 的入口。

        // GUI 必须由服务端发起，因此只在服务端执行打开逻辑。
        // 客户端只负责渲染界面，不负责创建 Menu。
        if (!level.isClientSide()) {

            // 获取当前位置绑定的 BlockEntity
            BlockEntity entity = level.getBlockEntity(pos);

            // 确认该实体确实是正确方块
            if (entity instanceof IceMakerBlockEntity juicer) {

                // 打开界面。
                // NetworkHooks.openScreen 会：
                // 1. 在服务端创建 Menu
                // 2. 通过网络把打开界面的信息发送给客户端
                // 3. 客户端根据 MenuType 创建对应的 Screen
                //
                // 这里传入 pos，是为了让客户端能够找到对应位置的 BlockEntity。
                NetworkHooks.openScreen((ServerPlayer) player, juicer, pos);

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
}

