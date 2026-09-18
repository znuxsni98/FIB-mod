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
    public static final VoxelShape SHAPE_S = Stream.of(
            Block.box(3, 0, 0, 13, 1, 1.5),
            Block.box(1, 0, 0, 3, 1, 14.5),
            Block.box(13, 0, 0, 15, 1, 14.5),
            Block.box(0, 0, 14, 16, 2, 16),
            Block.box(0, 8.5, 6.75, 16, 9, 9),
            Block.box(0, 9, 6.25, 16, 9.5, 8.5),
            Block.box(0, 7.5, 7.75, 16, 8, 10),
            Block.box(0, 7, 8.25, 16, 7.5, 10.5),
            Block.box(0, 6.5, 8.75, 16, 7, 11),
            Block.box(0, 6, 9.25, 16, 6.5, 11.5),
            Block.box(0, 11.5, 3.75, 16, 12, 6),
            Block.box(0, 11, 4.25, 16, 11.5, 6.5),
            Block.box(0, 10.5, 4.75, 16, 11, 7),
            Block.box(0, 10, 5.25, 16, 10.5, 7.5),
            Block.box(0, 12, 3.25, 16, 12.5, 5.5),
            Block.box(0, 12.5, 3, 16, 13, 5),
            Block.box(0, 13, 3.5, 16, 13.5, 4.5),
            Block.box(0, 8, 7.25, 16, 8.5, 9.5),
            Block.box(0, 9.5, 5.75, 16, 10, 8),
            Block.box(0, 5.5, 9.75, 16, 6, 12),
            Block.box(0, 4, 11.25, 16, 4.5, 13.5),
            Block.box(0, 4.5, 10.75, 16, 5, 13),
            Block.box(0, 5, 10.25, 16, 5.5, 12.5),
            Block.box(0, 3.5, 11.75, 16, 4, 14),
            Block.box(0, 3, 12.25, 16, 3.5, 14.5),
            Block.box(0, 2.5, 12.75, 16, 3, 15),
            Block.box(0, 2, 13.25, 16, 2.5, 15.5),
            Block.box(13, 9, 3.525, 15, 9.5, 4.65),
            Block.box(13, 10.5, 3.9475, 15, 11, 4.8225),
            Block.box(13, 10, 3.9475, 15, 10.5, 5.0725),
            Block.box(13, 9.5, 3.7275, 15, 10, 4.8525),
            Block.box(13, 5, 1.875, 15, 5.5, 3),
            Block.box(13, 5.5, 2.075, 15, 6, 3.2),
            Block.box(13, 6, 2.295, 15, 6.5, 3.42),
            Block.box(13, 6.5, 2.495, 15, 7, 3.62),
            Block.box(13, 8.5, 3.315, 15, 9, 4.44),
            Block.box(13, 8, 3.115, 15, 8.5, 4.24),
            Block.box(13, 7.5, 2.925, 15, 8, 4.05),
            Block.box(13, 7, 2.695, 15, 7.5, 3.82),
            Block.box(13, 3, 1.035, 15, 3.5, 2.16),
            Block.box(13, 3.5, 1.245, 15, 4, 2.37),
            Block.box(13, 4, 1.455, 15, 4.5, 2.58),
            Block.box(13, 4.5, 1.675, 15, 5, 2.8),
            Block.box(13, 2.5, 0.8375, 15, 3, 1.9625),
            Block.box(13, 2, 0.635, 15, 2.5, 1.76),
            Block.box(13, 1.5, 0.415, 15, 2, 1.54),
            Block.box(13, 1, 0.2125, 15, 1.5, 1.3375),
            Block.box(1, 10.5, 3.9475, 3, 11, 4.8225),
            Block.box(1, 9, 3.525, 3, 9.5, 4.65),
            Block.box(1, 10, 3.9475, 3, 10.5, 5.0725),
            Block.box(1, 9.5, 3.7275, 3, 10, 4.8525),
            Block.box(1, 5, 1.875, 3, 5.5, 3),
            Block.box(1, 5.5, 2.075, 3, 6, 3.2),
            Block.box(1, 6, 2.295, 3, 6.5, 3.42),
            Block.box(1, 6.5, 2.495, 3, 7, 3.62),
            Block.box(1, 8.5, 3.315, 3, 9, 4.44),
            Block.box(1, 8, 3.115, 3, 8.5, 4.24),
            Block.box(1, 7.5, 2.925, 3, 8, 4.05),
            Block.box(1, 7, 2.695, 3, 7.5, 3.82),
            Block.box(1, 3, 1.035, 3, 3.5, 2.16),
            Block.box(1, 3.5, 1.245, 3, 4, 2.37),
            Block.box(1, 4, 1.455, 3, 4.5, 2.58),
            Block.box(1, 4.5, 1.675, 3, 5, 2.8),
            Block.box(1, 2.5, 0.8375, 3, 3, 1.9625),
            Block.box(1, 2, 0.635, 3, 2.5, 1.76),
            Block.box(1, 1.5, 0.415, 3, 2, 1.54),
            Block.box(1, 1, 0.2125, 3, 1.5, 1.3375)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape SHAPE_E = Stream.of(
            Block.box(0, 0, 3, 1.5, 1, 13),
            Block.box(0, 0, 13, 14.5, 1, 15),
            Block.box(0, 0, 1, 14.5, 1, 3),
            Block.box(14, 0, 0, 16, 2, 16),
            Block.box(6.75, 8.5, 0, 9, 9, 16),
            Block.box(6.25, 9, 0, 8.5, 9.5, 16),
            Block.box(7.75, 7.5, 0, 10, 8, 16),
            Block.box(8.25, 7, 0, 10.5, 7.5, 16),
            Block.box(8.75, 6.5, 0, 11, 7, 16),
            Block.box(9.25, 6, 0, 11.5, 6.5, 16),
            Block.box(3.75, 11.5, 0, 6, 12, 16),
            Block.box(4.25, 11, 0, 6.5, 11.5, 16),
            Block.box(4.75, 10.5, 0, 7, 11, 16),
            Block.box(5.25, 10, 0, 7.5, 10.5, 16),
            Block.box(3.25, 12, 0, 5.5, 12.5, 16),
            Block.box(3, 12.5, 0, 5, 13, 16),
            Block.box(3.5, 13, 0, 4.5, 13.5, 16),
            Block.box(7.25, 8, 0, 9.5, 8.5, 16),
            Block.box(5.75, 9.5, 0, 8, 10, 16),
            Block.box(9.75, 5.5, 0, 12, 6, 16),
            Block.box(11.25, 4, 0, 13.5, 4.5, 16),
            Block.box(10.75, 4.5, 0, 13, 5, 16),
            Block.box(10.25, 5, 0, 12.5, 5.5, 16),
            Block.box(11.75, 3.5, 0, 14, 4, 16),
            Block.box(12.25, 3, 0, 14.5, 3.5, 16),
            Block.box(12.75, 2.5, 0, 15, 3, 16),
            Block.box(13.25, 2, 0, 15.5, 2.5, 16),
            Block.box(3.525, 9, 1, 4.65, 9.5, 3),
            Block.box(3.9475, 10.5, 1, 4.8225, 11, 3),
            Block.box(3.9475, 10, 1, 5.0725, 10.5, 3),
            Block.box(3.7275, 9.5, 1, 4.8525, 10, 3),
            Block.box(1.875, 5, 1, 3, 5.5, 3),
            Block.box(2.075, 5.5, 1, 3.2, 6, 3),
            Block.box(2.295, 6, 1, 3.42, 6.5, 3),
            Block.box(2.495, 6.5, 1, 3.62, 7, 3),
            Block.box(3.315, 8.5, 1, 4.44, 9, 3),
            Block.box(3.115, 8, 1, 4.24, 8.5, 3),
            Block.box(2.925, 7.5, 1, 4.05, 8, 3),
            Block.box(2.695, 7, 1, 3.82, 7.5, 3),
            Block.box(1.035, 3, 1, 2.16, 3.5, 3),
            Block.box(1.245, 3.5, 1, 2.37, 4, 3),
            Block.box(1.455, 4, 1, 2.58, 4.5, 3),
            Block.box(1.675, 4.5, 1, 2.8, 5, 3),
            Block.box(0.8375, 2.5, 1, 1.9625, 3, 3),
            Block.box(0.635, 2, 1, 1.76, 2.5, 3),
            Block.box(0.415, 1.5, 1, 1.54, 2, 3),
            Block.box(0.2125, 1, 1, 1.3375, 1.5, 3),
            Block.box(3.9475, 10.5, 13, 4.8225, 11, 15),
            Block.box(3.525, 9, 13, 4.65, 9.5, 15),
            Block.box(3.9475, 10, 13, 5.0725, 10.5, 15),
            Block.box(3.7275, 9.5, 13, 4.8525, 10, 15),
            Block.box(1.875, 5, 13, 3, 5.5, 15),
            Block.box(2.075, 5.5, 13, 3.2, 6, 15),
            Block.box(2.295, 6, 13, 3.42, 6.5, 15),
            Block.box(2.495, 6.5, 13, 3.62, 7, 15),
            Block.box(3.315, 8.5, 13, 4.44, 9, 15),
            Block.box(3.115, 8, 13, 4.24, 8.5, 15),
            Block.box(2.925, 7.5, 13, 4.05, 8, 15),
            Block.box(2.695, 7, 13, 3.82, 7.5, 15),
            Block.box(1.035, 3, 13, 2.16, 3.5, 15),
            Block.box(1.245, 3.5, 13, 2.37, 4, 15),
            Block.box(1.455, 4, 13, 2.58, 4.5, 15),
            Block.box(1.675, 4.5, 13, 2.8, 5, 15),
            Block.box(0.8375, 2.5, 13, 1.9625, 3, 15),
            Block.box(0.635, 2, 13, 1.76, 2.5, 15),
            Block.box(0.415, 1.5, 13, 1.54, 2, 15),
            Block.box(0.2125, 1, 13, 1.3375, 1.5, 15)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape SHAPE_W = Stream.of(
            Block.box(14.5, 0, 3, 16, 1, 13),
            Block.box(1.5, 0, 1, 16, 1, 3),
            Block.box(1.5, 0, 13, 16, 1, 15),
            Block.box(0, 0, 0, 2, 2, 16),
            Block.box(7, 8.5, 0, 9.25, 9, 16),
            Block.box(7.5, 9, 0, 9.75, 9.5, 16),
            Block.box(6, 7.5, 0, 8.25, 8, 16),
            Block.box(5.5, 7, 0, 7.75, 7.5, 16),
            Block.box(5, 6.5, 0, 7.25, 7, 16),
            Block.box(4.5, 6, 0, 6.75, 6.5, 16),
            Block.box(10, 11.5, 0, 12.25, 12, 16),
            Block.box(9.5, 11, 0, 11.75, 11.5, 16),
            Block.box(9, 10.5, 0, 11.25, 11, 16),
            Block.box(8.5, 10, 0, 10.75, 10.5, 16),
            Block.box(10.5, 12, 0, 12.75, 12.5, 16),
            Block.box(11, 12.5, 0, 13, 13, 16),
            Block.box(11.5, 13, 0, 12.5, 13.5, 16),
            Block.box(6.5, 8, 0, 8.75, 8.5, 16),
            Block.box(8, 9.5, 0, 10.25, 10, 16),
            Block.box(4, 5.5, 0, 6.25, 6, 16),
            Block.box(2.5, 4, 0, 4.75, 4.5, 16),
            Block.box(3, 4.5, 0, 5.25, 5, 16),
            Block.box(3.5, 5, 0, 5.75, 5.5, 16),
            Block.box(2, 3.5, 0, 4.25, 4, 16),
            Block.box(1.5, 3, 0, 3.75, 3.5, 16),
            Block.box(1, 2.5, 0, 3.25, 3, 16),
            Block.box(0.5, 2, 0, 2.75, 2.5, 16),
            Block.box(11.35, 9, 13, 12.475, 9.5, 15),
            Block.box(11.1775, 10.5, 13, 12.0525, 11, 15),
            Block.box(10.9275, 10, 13, 12.0525, 10.5, 15),
            Block.box(11.1475, 9.5, 13, 12.2725, 10, 15),
            Block.box(13, 5, 13, 14.125, 5.5, 15),
            Block.box(12.8, 5.5, 13, 13.925, 6, 15),
            Block.box(12.58, 6, 13, 13.705, 6.5, 15),
            Block.box(12.38, 6.5, 13, 13.505, 7, 15),
            Block.box(11.56, 8.5, 13, 12.685, 9, 15),
            Block.box(11.76, 8, 13, 12.885, 8.5, 15),
            Block.box(11.95, 7.5, 13, 13.075, 8, 15),
            Block.box(12.18, 7, 13, 13.305, 7.5, 15),
            Block.box(13.84, 3, 13, 14.965, 3.5, 15),
            Block.box(13.63, 3.5, 13, 14.755, 4, 15),
            Block.box(13.42, 4, 13, 14.545, 4.5, 15),
            Block.box(13.2, 4.5, 13, 14.325, 5, 15),
            Block.box(14.0375, 2.5, 13, 15.1625, 3, 15),
            Block.box(14.24, 2, 13, 15.365, 2.5, 15),
            Block.box(14.46, 1.5, 13, 15.585, 2, 15),
            Block.box(14.6625, 1, 13, 15.7875, 1.5, 15),
            Block.box(11.1775, 10.5, 1, 12.0525, 11, 3),
            Block.box(11.35, 9, 1, 12.475, 9.5, 3),
            Block.box(10.9275, 10, 1, 12.0525, 10.5, 3),
            Block.box(11.1475, 9.5, 1, 12.2725, 10, 3),
            Block.box(13, 5, 1, 14.125, 5.5, 3),
            Block.box(12.8, 5.5, 1, 13.925, 6, 3),
            Block.box(12.58, 6, 1, 13.705, 6.5, 3),
            Block.box(12.38, 6.5, 1, 13.505, 7, 3),
            Block.box(11.56, 8.5, 1, 12.685, 9, 3),
            Block.box(11.76, 8, 1, 12.885, 8.5, 3),
            Block.box(11.95, 7.5, 1, 13.075, 8, 3),
            Block.box(12.18, 7, 1, 13.305, 7.5, 3),
            Block.box(13.84, 3, 1, 14.965, 3.5, 3),
            Block.box(13.63, 3.5, 1, 14.755, 4, 3),
            Block.box(13.42, 4, 1, 14.545, 4.5, 3),
            Block.box(13.2, 4.5, 1, 14.325, 5, 3),
            Block.box(14.0375, 2.5, 1, 15.1625, 3, 3),
            Block.box(14.24, 2, 1, 15.365, 2.5, 3),
            Block.box(14.46, 1.5, 1, 15.585, 2, 3),
            Block.box(14.6625, 1, 1, 15.7875, 1.5, 3)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape SHAPE_N = Stream.of(
            Block.box(3, 0, 14.5, 13, 1, 16),
            Block.box(13, 0, 1.5, 15, 1, 16),
            Block.box(1, 0, 1.5, 3, 1, 16),
            Block.box(0, 0, 0, 16, 2, 2),
            Block.box(0, 8.5, 7, 16, 9, 9.25),
            Block.box(0, 9, 7.5, 16, 9.5, 9.75),
            Block.box(0, 7.5, 6, 16, 8, 8.25),
            Block.box(0, 7, 5.5, 16, 7.5, 7.75),
            Block.box(0, 6.5, 5, 16, 7, 7.25),
            Block.box(0, 6, 4.5, 16, 6.5, 6.75),
            Block.box(0, 11.5, 10, 16, 12, 12.25),
            Block.box(0, 11, 9.5, 16, 11.5, 11.75),
            Block.box(0, 10.5, 9, 16, 11, 11.25),
            Block.box(0, 10, 8.5, 16, 10.5, 10.75),
            Block.box(0, 12, 10.5, 16, 12.5, 12.75),
            Block.box(0, 12.5, 11, 16, 13, 13),
            Block.box(0, 13, 11.5, 16, 13.5, 12.5),
            Block.box(0, 8, 6.5, 16, 8.5, 8.75),
            Block.box(0, 9.5, 8, 16, 10, 10.25),
            Block.box(0, 5.5, 4, 16, 6, 6.25),
            Block.box(0, 4, 2.5, 16, 4.5, 4.75),
            Block.box(0, 4.5, 3, 16, 5, 5.25),
            Block.box(0, 5, 3.5, 16, 5.5, 5.75),
            Block.box(0, 3.5, 2, 16, 4, 4.25),
            Block.box(0, 3, 1.5, 16, 3.5, 3.75),
            Block.box(0, 2.5, 1, 16, 3, 3.25),
            Block.box(0, 2, 0.5, 16, 2.5, 2.75),
            Block.box(1, 9, 11.35, 3, 9.5, 12.475),
            Block.box(1, 10.5, 11.1775, 3, 11, 12.0525),
            Block.box(1, 10, 10.9275, 3, 10.5, 12.0525),
            Block.box(1, 9.5, 11.1475, 3, 10, 12.2725),
            Block.box(1, 5, 13, 3, 5.5, 14.125),
            Block.box(1, 5.5, 12.8, 3, 6, 13.925),
            Block.box(1, 6, 12.58, 3, 6.5, 13.705),
            Block.box(1, 6.5, 12.38, 3, 7, 13.505),
            Block.box(1, 8.5, 11.56, 3, 9, 12.685),
            Block.box(1, 8, 11.76, 3, 8.5, 12.885),
            Block.box(1, 7.5, 11.95, 3, 8, 13.075),
            Block.box(1, 7, 12.18, 3, 7.5, 13.305),
            Block.box(1, 3, 13.84, 3, 3.5, 14.965),
            Block.box(1, 3.5, 13.63, 3, 4, 14.755),
            Block.box(1, 4, 13.42, 3, 4.5, 14.545),
            Block.box(1, 4.5, 13.2, 3, 5, 14.325),
            Block.box(1, 2.5, 14.0375, 3, 3, 15.1625),
            Block.box(1, 2, 14.24, 3, 2.5, 15.365),
            Block.box(1, 1.5, 14.46, 3, 2, 15.585),
            Block.box(1, 1, 14.6625, 3, 1.5, 15.7875),
            Block.box(13, 10.5, 11.1775, 15, 11, 12.0525),
            Block.box(13, 9, 11.35, 15, 9.5, 12.475),
            Block.box(13, 10, 10.9275, 15, 10.5, 12.0525),
            Block.box(13, 9.5, 11.1475, 15, 10, 12.2725),
            Block.box(13, 5, 13, 15, 5.5, 14.125),
            Block.box(13, 5.5, 12.8, 15, 6, 13.925),
            Block.box(13, 6, 12.58, 15, 6.5, 13.705),
            Block.box(13, 6.5, 12.38, 15, 7, 13.505),
            Block.box(13, 8.5, 11.56, 15, 9, 12.685),
            Block.box(13, 8, 11.76, 15, 8.5, 12.885),
            Block.box(13, 7.5, 11.95, 15, 8, 13.075),
            Block.box(13, 7, 12.18, 15, 7.5, 13.305),
            Block.box(13, 3, 13.84, 15, 3.5, 14.965),
            Block.box(13, 3.5, 13.63, 15, 4, 14.755),
            Block.box(13, 4, 13.42, 15, 4.5, 14.545),
            Block.box(13, 4.5, 13.2, 15, 5, 14.325),
            Block.box(13, 2.5, 14.0375, 15, 3, 15.1625),
            Block.box(13, 2, 14.24, 15, 2.5, 15.365),
            Block.box(13, 1.5, 14.46, 15, 2, 15.585),
            Block.box(13, 1, 14.6625, 15, 1.5, 15.7875)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    // ==================== ★ 控制器附加碰撞箱（按朝向） ====================
    // 请根据实际控制器模型尺寸修改以下坐标
    // 格式: Block.box(x1, y1, z1, x2, y2, z2)  单位: 像素(0-16)
    public static final VoxelShape CTRL_SHAPE_N = Stream.of(
            Block.box(1.25, 1, 5.5, 4, 4, 12.5),
            Block.box(4.25, 1, 7, 10.75, 6, 12),
            Block.box(4.25, 1, 12, 11, 6, 16),
            Block.box(3, 0, 5.5, 11, 1, 14.5),
            Block.box(5, 6, 13, 11, 11, 16)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape CTRL_SHAPE_S = Stream.of(
            Block.box(12, 1, 3.5, 14.75, 4, 10.5),
            Block.box(5.25, 1, 4, 11.75, 6, 9),
            Block.box(5, 1, 0, 11.75, 6, 4),
            Block.box(5, 0, 1.5, 13, 1, 10.5),
            Block.box(5, 6, 0, 11, 11, 3)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape CTRL_SHAPE_W = Stream.of(
            Block.box(5.5, 1, 12, 12.5, 4, 14.75),
            Block.box(7, 1, 5.25, 12, 6, 11.75),
            Block.box(12, 1, 5, 16, 6, 11.75),
            Block.box(5.5, 0, 5, 14.5, 1, 13),
            Block.box(13, 6, 5, 16, 11, 11)
            ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();
    public static final VoxelShape CTRL_SHAPE_E = Stream.of(
            Block.box(3.5, 1, 1.25, 10.5, 4, 4),
            Block.box(4, 1, 4.25, 9, 6, 10.75),
            Block.box(0, 1, 4.25, 4, 6, 11),
            Block.box(1.5, 0, 3, 10.5, 1, 11),
            Block.box(0, 6, 5, 3, 11, 11)
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

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