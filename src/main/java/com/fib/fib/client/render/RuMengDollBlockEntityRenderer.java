package com.fib.fib.client.render;

import com.fib.fib.blockentity.RuMengDollBlockEntity;
import com.fib.fib.init.block.custom.Ru_Meng_Doll;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 如梦玩偶的方块实体渲染器。
 *
 * 方块状态只能表达 0/90/180/270 四向旋转，而玩偶需要 16 向（每 22.5° 一个）。
 * 因此把模型渲染交给这里：直接复用现成的方块模型（不修改模型本身），
 * 在渲染时用 PoseStack 绕 Y 轴旋转 22.5°×rotation。
 */
public class RuMengDollBlockEntityRenderer implements BlockEntityRenderer<RuMengDollBlockEntity> {

    public RuMengDollBlockEntityRenderer(BlockEntityRendererProvider.Context pContext) {
    }

    @Override
    public void render(RuMengDollBlockEntity blockEntity, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        int rotation = state.getValue(Ru_Meng_Doll.ROTATION);
        BlockPos pos = blockEntity.getBlockPos();

        if (blockEntity.getLevel() == null) {
            return;
        }

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(state);
        RandomSource random = blockEntity.getLevel().getRandom();

        pose.pushPose();
        // 旋转中心对准方块中心
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(22.5F * rotation));
        pose.translate(-0.5, 0.0, -0.5);

        VertexConsumer consumer = buffer.getBuffer(net.minecraft.client.renderer.RenderType.cutout());
        dispatcher.getModelRenderer().tesselateBlock(
                blockEntity.getLevel(), model, state, pos, pose,
                consumer, false, random, state.getSeed(pos), packedOverlay);

        pose.popPose();
    }
}
