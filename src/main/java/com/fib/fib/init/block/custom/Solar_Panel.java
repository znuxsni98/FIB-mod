package com.fib.fib.init.block.custom;

import com.fib.fib.blockentity.SolarPanelBlockEntity;
import com.fib.fib.init.ModBlockEntities;
import com.fib.fib.init.item.ModItems;
import com.simibubi.create.AllItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class Solar_Panel extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HAS_CONTROLLER = BooleanProperty.create("has_controller");

    // ==================== 太阳能板本体碰撞箱 ====================
    public static final VoxelShape SHAPE_S = Stream.of(Block.box(0, -2.30761, 8.16548, 16, 14.69239, 10.16548), Block.box(1, 0.34216, 2.05718, 3, 11.34216, 3.30718), Block.box(0, 0, 14, 16, 2, 16), Block.box(13, 0, 0, 15, 1, 14.5), Block.box(13, 0.34216, 2.05718, 15, 11.34216, 3.30718), Block.box(1, 0, 0, 3, 1, 14.5), Block.box(3, 0, 0, 13, 1, 1.5)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape SHAPE_W = Stream.of(Block.box(8.16548, -2.30761, 0, 10.16548, 14.69239, 16), Block.box(2.05718, 0.34216, 13, 3.30718, 11.34216, 15), Block.box(14, 0, 0, 16, 2, 16), Block.box(0, 0, 1, 14.5, 1, 3), Block.box(2.05718, 0.34216, 1, 3.30718, 11.34216, 3), Block.box(0, 0, 13, 14.5, 1, 15), Block.box(0, 0, 3, 1.5, 1, 13)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape SHAPE_E = Stream.of(Block.box(5.83452, -2.30761, 0, 7.83452, 14.69239, 16), Block.box(12.69282, 0.34216, 13, 13.94282, 11.34216, 15), Block.box(0, 0, 0, 2, 2, 16), Block.box(1.5, 0, 13, 16, 1, 15), Block.box(12.69282, 0.34216, 1, 13.94282, 11.34216, 3), Block.box(1.5, 0, 1, 16, 1, 3), Block.box(14.5, 0, 3, 16, 1, 13)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape SHAPE_N = Stream.of(Block.box(0, -2.30761, 5.83452, 16, 14.69239, 7.83452), Block.box(1, 0.34216, 12.69282, 3, 11.34216, 13.94282), Block.box(0, 0, 0, 16, 2, 2), Block.box(1, 0, 1.5, 3, 1, 16), Block.box(13, 0.34216, 12.69282, 15, 11.34216, 13.94282), Block.box(13, 0, 1.5, 15, 1, 16), Block.box(3, 0, 14.5, 13, 1, 16)).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    // ==================== ★ 控制器附加碰撞箱（按朝向） ====================
    // 请根据实际控制器模型尺寸修改以下坐标
    // 格式: Block.box(x1, y1, z1, x2, y2, z2)  单位: 像素(0-16)
    public static final VoxelShape CTRL_SHAPE_N = Block.box(4, 2, 4, 12, 6, 12);
    public static final VoxelShape CTRL_SHAPE_S = Block.box(4, 2, 4, 12, 6, 12);
    public static final VoxelShape CTRL_SHAPE_W = Block.box(4, 2, 4, 12, 6, 12);
    public static final VoxelShape CTRL_SHAPE_E = Block.box(4, 2, 4, 12, 6, 12);

    // ==================== 组合碰撞箱缓存（避免每帧 join） ====================
    private static final VoxelShape FULL_SHAPE_N = Shapes.join(SHAPE_N, CTRL_SHAPE_N, BooleanOp.OR);
    private static final VoxelShape FULL_SHAPE_S = Shapes.join(SHAPE_S, CTRL_SHAPE_S, BooleanOp.OR);
    private static final VoxelShape FULL_SHAPE_W = Shapes.join(SHAPE_W, CTRL_SHAPE_W, BooleanOp.OR);
    private static final VoxelShape FULL_SHAPE_E = Shapes.join(SHAPE_E, CTRL_SHAPE_E, BooleanOp.OR);

    public Solar_Panel(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(HAS_CONTROLLER, false));
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        boolean hasCtrl = pState.getValue(HAS_CONTROLLER);
        return switch (pState.getValue(FACING)) {
            case SOUTH -> hasCtrl ? FULL_SHAPE_S : SHAPE_S;
            case WEST  -> hasCtrl ? FULL_SHAPE_W : SHAPE_W;
            case EAST  -> hasCtrl ? FULL_SHAPE_E : SHAPE_E;
            default    -> hasCtrl ? FULL_SHAPE_N : SHAPE_N;
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
        pBuilder.add(FACING, HAS_CONTROLLER);
    }

    // ==================== 右键交互：安装 / 拆除控制器 ====================
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);
        boolean hasCtrl = state.getValue(HAS_CONTROLLER);

        // 1. 手持控制器 → 安装
        if (held.is(ModItems.SOLAR_PANEL_CONTROLLER.get())) {
            if (!hasCtrl) {
                level.setBlock(pos, state.setValue(HAS_CONTROLLER, true), 3);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                return InteractionResult.CONSUME;
            }
            return InteractionResult.FAIL;
        }

        // 2. 手持 Create Wrench → 拆除控制器（返还，不消耗扳手）
        if (AllItems.WRENCH.isIn(held)) {
            if (hasCtrl) {
                level.setBlock(pos, state.setValue(HAS_CONTROLLER, false), 3);
                ItemStack controller = new ItemStack(ModItems.SOLAR_PANEL_CONTROLLER.get());
                if (!player.addItem(controller)) {
                    player.drop(controller, false);
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    // ==================== BE 创建与 Ticker ====================
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SolarPanelBlockEntity(blockPos, blockState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pBlockEntityType == ModBlockEntities.SOLAR_PANEL_BE.get()
                ? (lvl, pos, state, be) -> ((SolarPanelBlockEntity) be).tick()
                : null;
    }
}