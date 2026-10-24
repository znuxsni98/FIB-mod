package com.fib.fib.client.render.item.GrapplingHook;

import com.fib.fib.FIBMod;
import com.fib.fib.init.entity.custom.GrapplingHookEntity;
import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import com.fib.fib.init.item.custom.hook.IHookMaterial;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.List;

/**
 * 钩爪枪射击前落点与弹道预测渲染器。
 * 当玩家手持钩爪枪且处于可发射状态时，在世界中实时演算抛物线轨迹，
 * 并在目标表面渲染全息落点环与锚定有效性提示。
 */
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GrapplingHookLandingRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        // 检测主手或副手是否持有钩爪枪
        ItemStack gunStack = player.getMainHandItem();
        if (!(gunStack.getItem() instanceof GrapplingHookGunItem)) {
            gunStack = player.getOffhandItem();
            if (!(gunStack.getItem() instanceof GrapplingHookGunItem)) {
                return;
            }
        }

        // 能量检查
        boolean pressurized = GrapplingHookGunItem.hasPressurizedModule(gunStack);
        boolean dualHook = GrapplingHookGunItem.hasDualHookModule(gunStack);
        float energyCost = 0.10f * (pressurized ? 2.0f : 1.0f) * (dualHook ? 1.5f : 1.0f);
        if (GrapplingHookGunItem.getEnergy(gunStack) < energyCost && !player.getAbilities().instabuild) {
            return;
        }

        // 活动钩爪数量检查：单钩模式已有钩爪或双钩模式已有2钩时不显示预测
        List<GrapplingHookEntity> activeHooks = mc.level.getEntitiesOfClass(
                GrapplingHookEntity.class,
                player.getBoundingBox().inflate(64.0),
                h -> h.getShooter() == player && !h.isRemoved()
        );
        if (!dualHook && !activeHooks.isEmpty()) return;
        if (dualHook && activeHooks.size() >= 2) return;

        // 计算弹道初速度与发射点
        float partialTick = event.getPartialTick();
        Vec3 startPos = player.getEyePosition(partialTick).subtract(0, 0.1, 0);

        float speed = pressurized ? 1.8f : 1.2f;
        float xRot = player.getXRot();
        float yRot = player.getYRot();
        float f = -Mth.sin(yRot * ((float) Math.PI / 180F)) * Mth.cos(xRot * ((float) Math.PI / 180F));
        float f1 = -Mth.sin(xRot * ((float) Math.PI / 180F));
        float f2 = Mth.cos(yRot * ((float) Math.PI / 180F)) * Mth.cos(xRot * ((float) Math.PI / 180F));
        Vec3 velocity = new Vec3(f, f1, f2).normalize().scale(speed);

        // 射程无距离限制（设计文档 §8），此处不做距离截断。

        // 弹道模拟
        Vec3 currentPos = startPos;
        Vec3 currentVel = velocity;
        Vec3 hitPoint = null;
        Direction hitFace = null;
        BlockPos hitBlockPos = null;
        boolean canAnchor = true;

        // 无射程上限：模拟步数提高，直到命中方块/实体为止
        for (int step = 0; step < 600; step++) {
            currentVel = currentVel.subtract(0, 0.03, 0);
            Vec3 nextPos = currentPos.add(currentVel);

            // 方块碰撞检测
            ClipContext clip = new ClipContext(
                    currentPos, nextPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            );
            BlockHitResult blockHit = mc.level.clip(clip);

            // 实体碰撞检测
            AABB stepBox = new AABB(currentPos, nextPos).inflate(0.5);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    mc.level, player, currentPos, nextPos, stepBox,
                    e -> e != player && !(e instanceof GrapplingHookEntity) && e.isAlive()
            );

            if (entityHit != null && (blockHit.getType() == HitResult.Type.MISS ||
                    currentPos.distanceTo(entityHit.getLocation()) < currentPos.distanceTo(blockHit.getLocation()))) {
                hitPoint = entityHit.getLocation();
                hitFace = Direction.UP;
                break;
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                hitPoint = blockHit.getLocation();
                hitFace = blockHit.getDirection();
                hitBlockPos = blockHit.getBlockPos();
                break;
            }

            currentPos = nextPos;
        }

        // 检查命中方块是否可锚定
        if (hitBlockPos != null) {
            IHookMaterial material = GrapplingHookGunItem.getHookMaterial(gunStack);
            BlockState state = mc.level.getBlockState(hitBlockPos);
            if (!material.canHookBlocks()) {
                canAnchor = false;
            } else if (state.getDestroySpeed(mc.level, hitBlockPos) > 25.0f && !material.canHookHardBlocks()) {
                canAnchor = false;
            }
        }

        // 若未命中任何目标，不渲染任何多余线条
        // （hitPoint 与 hitFace 总是成对赋值，判 hitPoint 即可）
        if (hitPoint == null) {
            return;
        }

        // 仅在目标命中表面绘制全息落点准星
        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        int r, g, b;
        if (canAnchor) {
            r = 0; g = 235; b = 255;
        } else {
            r = 255; g = 60; b = 60;
        }

        // 准星线条用 RenderType.lines() 默认带深度测试：
        // 即使法线偏移 0.05，相机近距离或倾斜角度下仍可能被方块表面遮挡。
        //
        // 关键：MultiBufferSource 会延迟到帧末才 flush，单纯 disableDepthTest 作用域太窄，
        // flush 时状态已恢复。必须在禁用深度测试期间强制 endBatch 立即绘制。
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        renderLandingMarker(consumer, pose, hitPoint, hitFace, r, g, b);
        mc.renderBuffers().bufferSource().endBatch(RenderType.lines());
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        poseStack.popPose();
    }

    /** 绘制附着在目标表面上的全息准星 */
    private static void renderLandingMarker(VertexConsumer consumer, Matrix4f pose, Vec3 hitPoint,
                                            Direction face, int r, int g, int b) {
        Vec3 normalVec = Vec3.atLowerCornerOf(face.getNormal());
        // 偏移 0.05 格（原 0.015 太小）：
        // 准星线条与方块表面几乎共面时，深度缓冲精度不足会导致线条被方块遮挡，表现为时隐时现。
        // 0.05 格足以拉开深度差，视觉上仍贴住表面。
        Vec3 center = hitPoint.add(normalVec.scale(0.05));

        float innerR = 0.04f;
        float outerR = 0.15f;

        Vec3 uIn = getTangentOffset(face, innerR, 0);
        Vec3 uOut = getTangentOffset(face, outerR, 0);
        Vec3 vIn = getTangentOffset(face, 0, innerR);
        Vec3 vOut = getTangentOffset(face, 0, outerR);

        // 计算 U / V 轴单位向量。
        // RenderType.lines() 着色器用 side = cross(viewDir, normal) 决定线宽扩展方向。
        // 要让 side 垂直于线段方向，normal 必须【平行于线段方向】—— 这与直觉相反：
        //   - side = cross(N, L)，若 normal = L（线段方向），则 side = cross(N, L) ⊥ L ✓
        // 原版 LevelRenderer.renderLineBox 画 Y 方向的线就传 normal(0,1,0)，正是这个原理。
        Vec3 uDir = getTangentOffset(face, 1.0, 0.0);  // U 轴单位向量
        Vec3 vDir = getTangentOffset(face, 0.0, 1.0);  // V 轴单位向量
        float ux = (float) uDir.x, uy = (float) uDir.y, uz = (float) uDir.z;
        float vx = (float) vDir.x, vy = (float) vDir.y, vz = (float) vDir.z;

        // 四向十字准星准星线：法线传线段自己的方向
        // +U（方向沿 U 轴，法线也传 U 轴）
        consumer.vertex(pose, (float)(center.x + uIn.x), (float)(center.y + uIn.y), (float)(center.z + uIn.z)).color(r, g, b, 240).normal(ux, uy, uz).endVertex();
        consumer.vertex(pose, (float)(center.x + uOut.x), (float)(center.y + uOut.y), (float)(center.z + uOut.z)).color(r, g, b, 240).normal(ux, uy, uz).endVertex();
        // -U
        consumer.vertex(pose, (float)(center.x - uIn.x), (float)(center.y - uIn.y), (float)(center.z - uIn.z)).color(r, g, b, 240).normal(ux, uy, uz).endVertex();
        consumer.vertex(pose, (float)(center.x - uOut.x), (float)(center.y - uOut.y), (float)(center.z - uOut.z)).color(r, g, b, 240).normal(ux, uy, uz).endVertex();
        // +V（方向沿 V 轴，法线也传 V 轴）
        consumer.vertex(pose, (float)(center.x + vIn.x), (float)(center.y + vIn.y), (float)(center.z + vIn.z)).color(r, g, b, 240).normal(vx, vy, vz).endVertex();
        consumer.vertex(pose, (float)(center.x + vOut.x), (float)(center.y + vOut.y), (float)(center.z + vOut.z)).color(r, g, b, 240).normal(vx, vy, vz).endVertex();
        // -V
        consumer.vertex(pose, (float)(center.x - vIn.x), (float)(center.y - vIn.y), (float)(center.z - vIn.z)).color(r, g, b, 240).normal(vx, vy, vz).endVertex();
        consumer.vertex(pose, (float)(center.x - vOut.x), (float)(center.y - vOut.y), (float)(center.z - vOut.z)).color(r, g, b, 240).normal(vx, vy, vz).endVertex();

        // 准星外环 (轻量 12 边形圆环)：每段的法线传该段的切线方向
        float ringR = 0.11f;
        int segments = 12;
        for (int i = 0; i < segments; i++) {
            double a1 = (i * 2.0 * Math.PI) / segments;
            double a2 = ((i + 1) * 2.0 * Math.PI) / segments;
            Vec3 off1 = getTangentOffset(face, Math.cos(a1) * ringR, Math.sin(a1) * ringR);
            Vec3 off2 = getTangentOffset(face, Math.cos(a2) * ringR, Math.sin(a2) * ringR);
            // 该段切线方向 = off2 - off1（归一化）
            Vec3 tangent = off2.subtract(off1).normalize();
            float tx = (float) tangent.x, ty = (float) tangent.y, tz = (float) tangent.z;
            consumer.vertex(pose, (float)(center.x + off1.x), (float)(center.y + off1.y), (float)(center.z + off1.z)).color(r, g, b, 200).normal(tx, ty, tz).endVertex();
            consumer.vertex(pose, (float)(center.x + off2.x), (float)(center.y + off2.y), (float)(center.z + off2.z)).color(r, g, b, 200).normal(tx, ty, tz).endVertex();
        }
    }

    /** 根据面朝向获得切平面坐标偏移 */
    private static Vec3 getTangentOffset(Direction normal, double u, double v) {
        return switch (normal.getAxis()) {
            case Y -> new Vec3(u, 0, v);
            case Z -> new Vec3(u, v, 0);
            case X -> new Vec3(0, u, v);
        };
    }
}
