package com.fib.fib.client.render;

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
import net.minecraft.world.entity.Entity;
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

import java.util.ArrayList;
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
        float energyCost = 1.0f * (pressurized ? 2.0f : 1.0f) * (dualHook ? 1.5f : 1.0f);
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

        double maxDist = pressurized ? 24.0 : 16.0;

        // 弹道模拟
        List<Vec3> points = new ArrayList<>();
        points.add(startPos);

        Vec3 currentPos = startPos;
        Vec3 currentVel = velocity;
        Vec3 hitPoint = null;
        Direction hitFace = null;
        BlockPos hitBlockPos = null;
        boolean canAnchor = true;

        for (int step = 0; step < 40; step++) {
            currentVel = currentVel.subtract(0, 0.03, 0);
            Vec3 nextPos = currentPos.add(currentVel);

            // 射程截断
            if (startPos.distanceTo(nextPos) > maxDist) {
                double distToNext = startPos.distanceTo(nextPos);
                double factor = (maxDist - startPos.distanceTo(currentPos)) / (distToNext - startPos.distanceTo(currentPos));
                if (factor > 0 && factor <= 1.0) {
                    points.add(currentPos.add(nextPos.subtract(currentPos).scale(factor)));
                }
                break;
            }

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
                points.add(hitPoint);
                canAnchor = true;
                break;
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                hitPoint = blockHit.getLocation();
                hitFace = blockHit.getDirection();
                hitBlockPos = blockHit.getBlockPos();
                points.add(hitPoint);
                break;
            }

            points.add(nextPos);
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

        // 开始在世界空间绘制
        PoseStack poseStack = event.getPoseStack();
        Vec3 camPos = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lines());

        // 颜色确定：有效锚定为青色，无效为警示红，未命中为柔和天蓝
        int r, g, b, a;
        if (hitPoint != null) {
            if (canAnchor) {
                r = 0; g = 235; b = 255; a = 200;
            } else {
                r = 255; g = 60; b = 60; a = 220;
            }
        } else {
            r = 140; g = 190; b = 255; a = 120;
        }

        // 1. 绘制弹道曲线段
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 p1 = points.get(i);
            Vec3 p2 = points.get(i + 1);

            consumer.vertex(pose, (float) p1.x, (float) p1.y, (float) p1.z)
                    .color(r, g, b, a)
                    .normal(0, 1, 0)
                    .endVertex();
            consumer.vertex(pose, (float) p2.x, (float) p2.y, (float) p2.z)
                    .color(r, g, b, a)
                    .normal(0, 1, 0)
                    .endVertex();
        }

        // 2. 绘制表面全息落点环与准星
        if (hitPoint != null && hitFace != null) {
            renderLandingMarker(consumer, pose, hitPoint, hitFace, canAnchor, r, g, b);
        }

        poseStack.popPose();
    }

    /** 绘制附着在方块表面上的全息圆环与十字刻度 */
    private static void renderLandingMarker(VertexConsumer consumer, Matrix4f pose, Vec3 hitPoint,
                                            Direction face, boolean canAnchor, int r, int g, int b) {
        // 微小偏置防止与方块表面 Z-Fighting
        Vec3 normalVec = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 center = hitPoint.add(normalVec.scale(0.015));

        // 微弱呼吸脉冲动画
        double time = System.currentTimeMillis() / 250.0;
        float pulse = (float) Math.sin(time) * 0.02f;
        float radius = 0.25f + pulse;

        // 绘制 16 边形圆环
        int segments = 16;
        for (int i = 0; i < segments; i++) {
            double a1 = (i * 2.0 * Math.PI) / segments;
            double a2 = ((i + 1) * 2.0 * Math.PI) / segments;

            Vec3 off1 = getTangentOffset(face, Math.cos(a1) * radius, Math.sin(a1) * radius);
            Vec3 off2 = getTangentOffset(face, Math.cos(a2) * radius, Math.sin(a2) * radius);

            Vec3 v1 = center.add(off1);
            Vec3 v2 = center.add(off2);

            consumer.vertex(pose, (float) v1.x, (float) v1.y, (float) v1.z)
                    .color(r, g, b, 240)
                    .normal(0, 1, 0)
                    .endVertex();
            consumer.vertex(pose, (float) v2.x, (float) v2.y, (float) v2.z)
                    .color(r, g, b, 240)
                    .normal(0, 1, 0)
                    .endVertex();
        }

        // 绘制内部十字瞄准刻度
        float innerR = 0.14f;
        Vec3 u = getTangentOffset(face, innerR, 0);
        Vec3 v = getTangentOffset(face, 0, innerR);

        // U 轴十字线
        consumer.vertex(pose, (float)(center.x - u.x), (float)(center.y - u.y), (float)(center.z - u.z))
                .color(r, g, b, 220).normal(0, 1, 0).endVertex();
        consumer.vertex(pose, (float)(center.x + u.x), (float)(center.y + u.y), (float)(center.z + u.z))
                .color(r, g, b, 220).normal(0, 1, 0).endVertex();

        // V 轴十字线
        consumer.vertex(pose, (float)(center.x - v.x), (float)(center.y - v.y), (float)(center.z - v.z))
                .color(r, g, b, 220).normal(0, 1, 0).endVertex();
        consumer.vertex(pose, (float)(center.x + v.x), (float)(center.y + v.y), (float)(center.z + v.z))
                .color(r, g, b, 220).normal(0, 1, 0).endVertex();

        // 表面微型锚爪投影线
        if (canAnchor) {
            float flukeR = 0.08f;
            for (int k = 0; k < 4; k++) {
                double ang = k * (Math.PI / 2.0) + (Math.PI / 4.0);
                Vec3 fTip = center.add(getTangentOffset(face, Math.cos(ang) * flukeR, Math.sin(ang) * flukeR));
                consumer.vertex(pose, (float) center.x, (float) center.y, (float) center.z)
                        .color(r, g, b, 255).normal(0, 1, 0).endVertex();
                consumer.vertex(pose, (float) fTip.x, (float) fTip.y, (float) fTip.z)
                        .color(r, g, b, 255).normal(0, 1, 0).endVertex();
            }
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
