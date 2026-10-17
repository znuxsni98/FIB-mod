package com.fib.fib.init.block.custom;

import com.fib.fib.blockentity.IceMakerBlockEntity;
import com.fib.fib.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraftforge.fluids.FluidUtil;
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

        // 潜行 + 手里拿着装了流体的容器（桶、储罐…）时，走 Forge 的流体交互：
        // 往机器里灌，或者把机器里的抽出来（具体方向由 FluidUtil 按容器和储罐的状态自己定）。
        // 不加这一条的话，手里拿着水桶右键只会开界面，玩家没法快速喂水。
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && FluidUtil.getFluidContained(held).isPresent()) {
            // 客户端必须返回成功，否则手臂不会摆、音效不会响；
            // 真正的转移只在服务端发生（interactWithFluidHandler 内部自己区分两侧）。
            return FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())
                    ? InteractionResult.sidedSuccess(level.isClientSide())
                    : InteractionResult.PASS;
        }

        // 方块被玩家右键时打开 GUI 的入口
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

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {

        // 旧方块与新方块不是同一个方块，时说明当前方块被替换/破坏了
        if (pState.getBlock() != pNewState.getBlock()) {

            // 取出当前位置的 BlockEntity
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);

            // 如果它确实是该方块的 BE，就执行掉落逻辑
            if (blockEntity instanceof IceMakerBlockEntity IceMaker) {
                IceMaker.drops();
            }
        }

        // 保留父类逻辑
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }
}

