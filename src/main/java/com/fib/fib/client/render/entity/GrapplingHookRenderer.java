package com.fib.fib.client.render.entity;

import com.fib.fib.FIBMod;
import com.fib.fib.init.entity.custom.GrapplingHookEntity;
import com.fib.fib.init.item.custom.GrapplingHookGunItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 钩爪实体及其物理绳索渲染器。
 * 具备全 3D 实体钩爪头部（实心中轴、穿透锥头、四向倒钩爪、后部锚环）
 * 以及连接钩爪与发射枪口之间的真正 3D 建模物理绳索（具备真实管状截面体积、四向法线光照与编织纹理）。
 */
public class GrapplingHookRenderer extends EntityRenderer<GrapplingHookEntity> {
    private static final ResourceLocation TEXTURE_BASIC =
            new ResourceLocation(FIBMod.MOD_ID, "textures/entity/hook/basic_hook.png");
    private static final ResourceLocation TEXTURE_PRISMARINE =
            new ResourceLocation(FIBMod.MOD_ID, "textures/entity/hook/prismarine_hook.png");
    private static final ResourceLocation TEXTURE_WOOL =
            new ResourceLocation(FIBMod.MOD_ID, "textures/entity/hook/wool_hook.png");
    private static final ResourceLocation TEXTURE_NETHERITE =
            new ResourceLocation(FIBMod.MOD_ID, "textures/entity/hook/netherite_hook.png");

    public GrapplingHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(GrapplingHookEntity entity) {
        return switch (entity.getHookMaterialId()) {
            case 1 -> TEXTURE_PRISMARINE;
            case 2 -> TEXTURE_WOOL;
            case 3 -> TEXTURE_NETHERITE;
            default -> TEXTURE_BASIC;
        };
    }

    /** 绳索跨越整个视界，禁用视锥体剔除以防视角偏转时绳索消失 */
    @Override
    public boolean shouldRender(GrapplingHookEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(GrapplingHookEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        // 1. 渲染全 3D 钩爪头部实体
        poseStack.pushPose();
        // 沿飞行方向旋转，+X 为前进穿透朝向
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));

        render3DHookHead(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))), packedLight);
        poseStack.popPose();

        // 2. 渲染真正的 3D 建模立体绳索
        render3DModeledRope(entity, partialTicks, poseStack, buffer);
    }

    /** 绘制具备实心轴、倒钩爪与锚环的立体钩爪头部 */
    private void render3DHookHead(PoseStack poseStack, VertexConsumer consumer, int light) {
        poseStack.pushPose();
        poseStack.scale(0.42f, 0.42f, 0.42f);

        PoseStack.Pose currentPose = poseStack.last();
        Matrix4f pose = currentPose.pose();
        Matrix3f normal = currentPose.normal();

        // 1. 实心中轴主杆 (X 轴沿向: -0.32 到 0.20)
        drawBox(consumer, pose, normal, light,
                -0.32f, -0.04f, -0.04f,
                0.20f, 0.04f, 0.04f,
                0.25f, 0.0f, 0.5f, 0.75f);

        // 2. 前端尖锐穿透锥头 (从 0.20 延伸至 0.38)
        drawConeTip(consumer, pose, normal, light, 0.20f, 0.38f, 0.04f);

        // 3. 后部拴绳固定锚环 (X: -0.42 到 -0.32)
        drawBox(consumer, pose, normal, light,
                -0.42f, -0.07f, -0.07f,
                -0.32f, 0.07f, 0.07f,
                0.5f, 0.0f, 0.75f, 0.25f);

        // 4. 四向折角倒钩爪（以 X 轴为中心旋转 4 次绘制实心爪枝）
        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(i * 90.0f));
            PoseStack.Pose flukePose = poseStack.last();
            Matrix4f fMat = flukePose.pose();
            Matrix3f fNorm = flukePose.normal();

            // 爪根向外延伸枝段 (+Y 方向展开)
            drawBox(consumer, fMat, fNorm, light,
                    0.10f, 0.04f, -0.02f,
                    0.18f, 0.24f, 0.02f,
                    0.0f, 0.25f, 0.25f, 0.75f);

            // 倒钩回折尖端 (反向弯曲至 -X 方向)
            drawBox(consumer, fMat, fNorm, light,
                    -0.04f, 0.20f, -0.02f,
                    0.12f, 0.26f, 0.02f,
                    0.0f, 0.0f, 0.25f, 0.25f);

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    /** 绘制穿透锥尖 */
    private void drawConeTip(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int light,
                            float startX, float endX, float r) {
        // +Y 斜面
        drawQuad(consumer, pose, normal, light,
                startX, r, -r, 0.25f, 0.75f,
                startX, r, r, 0.5f, 0.75f,
                endX, 0, 0, 0.5f, 1.0f,
                endX, 0, 0, 0.25f, 1.0f,
                0.5f, 1.0f, 0.0f);
        // -Y 斜面
        drawQuad(consumer, pose, normal, light,
                startX, -r, r, 0.25f, 0.75f,
                startX, -r, -r, 0.5f, 0.75f,
                endX, 0, 0, 0.5f, 1.0f,
                endX, 0, 0, 0.25f, 1.0f,
                0.5f, -1.0f, 0.0f);
        // +Z 斜面
        drawQuad(consumer, pose, normal, light,
                startX, -r, r, 0.25f, 0.75f,
                startX, r, r, 0.5f, 0.75f,
                endX, 0, 0, 0.5f, 1.0f,
                endX, 0, 0, 0.25f, 1.0f,
                0.5f, 0.0f, 1.0f);
        // -Z 斜面
        drawQuad(consumer, pose, normal, light,
                startX, r, -r, 0.25f, 0.75f,
                startX, -r, -r, 0.5f, 0.75f,
                endX, 0, 0, 0.5f, 1.0f,
                endX, 0, 0, 0.25f, 1.0f,
                0.5f, 0.0f, -1.0f);
    }

    /** 绘制实心长方体 6 个面 */
    private void drawBox(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int light,
                         float minX, float minY, float minZ,
                         float maxX, float maxY, float maxZ,
                         float uMin, float vMin, float uMax, float vMax) {
        // Top (+Y)
        drawQuad(consumer, pose, normal, light,
                minX, maxY, minZ, uMin, vMin,
                minX, maxY, maxZ, uMin, vMax,
                maxX, maxY, maxZ, uMax, vMax,
                maxX, maxY, minZ, uMax, vMin,
                0, 1, 0);
        // Bottom (-Y)
        drawQuad(consumer, pose, normal, light,
                maxX, minY, minZ, uMin, vMin,
                maxX, minY, maxZ, uMin, vMax,
                minX, minY, maxZ, uMax, vMax,
                minX, minY, minZ, uMax, vMin,
                0, -1, 0);
        // Front (+X)
        drawQuad(consumer, pose, normal, light,
                maxX, minY, minZ, uMin, vMin,
                maxX, maxY, minZ, uMin, vMax,
                maxX, maxY, maxZ, uMax, vMax,
                maxX, minY, maxZ, uMax, vMin,
                1, 0, 0);
        // Back (-X)
        drawQuad(consumer, pose, normal, light,
                minX, minY, maxZ, uMin, vMin,
                minX, maxY, maxZ, uMin, vMax,
                minX, maxY, minZ, uMax, vMax,
                minX, minY, minZ, uMax, vMin,
                -1, 0, 0);
        // Right (+Z)
        drawQuad(consumer, pose, normal, light,
                minX, minY, maxZ, uMin, vMin,
                maxX, minY, maxZ, uMin, vMax,
                maxX, maxY, maxZ, uMax, vMax,
                minX, maxY, maxZ, uMax, vMin,
                0, 0, 1);
        // Left (-Z)
        drawQuad(consumer, pose, normal, light,
                maxX, minY, minZ, uMin, vMin,
                minX, minY, minZ, uMin, vMax,
                minX, maxY, minZ, uMax, vMax,
                maxX, maxY, minZ, uMax, vMin,
                0, 0, -1);
    }

    private void drawQuad(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int light,
                          float x1, float y1, float z1, float u1, float v1,
                          float x2, float y2, float z2, float u2, float v2,
                          float x3, float y3, float z3, float u3, float v3,
                          float x4, float y4, float z4, float u4, float v4,
                          float nx, float ny, float nz) {
        consumer.vertex(pose, x1, y1, z1).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(pose, x2, y2, z2).color(255, 255, 255, 255).uv(u2, v2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(pose, x3, y3, z3).color(255, 255, 255, 255).uv(u3, v3).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
        consumer.vertex(pose, x4, y4, z4).color(255, 255, 255, 255).uv(u4, v4).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
    }

    /** 绘制真正的 3D 建模物理绳索（带实体方块截面、曲率悬链线、编织纹理与渐变光照） */
    private void render3DModeledRope(GrapplingHookEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        Player shooter = entity.getShooter();
        if (shooter == null) return;

        int handSide = shooter.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (shooter.getOffhandItem().getItem() instanceof GrapplingHookGunItem && !(shooter.getMainHandItem().getItem() instanceof GrapplingHookGunItem)) {
            handSide = -handSide;
        }

        double handX;
        double handY;
        double handZ;
        float crouchOffset;

        // 计算第一人称近平面枪口精确锚定或第三人称手部坐标
        if ((this.entityRenderDispatcher.options == null || this.entityRenderDispatcher.options.getCameraType().isFirstPerson()) && shooter == Minecraft.getInstance().player) {
            double fovFactor = 960.0D / (double)this.entityRenderDispatcher.options.fov().get().intValue();
            Vec3 nearPt = this.entityRenderDispatcher.camera.getNearPlane().getPointOnPlane((float)handSide * 0.48F, -0.22F);
            nearPt = nearPt.scale(fovFactor);
            float attackAnim = shooter.getAttackAnim(partialTicks);
            float swingSin = Mth.sin(Mth.sqrt(attackAnim) * (float)Math.PI);
            nearPt = nearPt.yRot(swingSin * 0.5F);
            nearPt = nearPt.xRot(-swingSin * 0.7F);
            handX = Mth.lerp(partialTicks, shooter.xo, shooter.getX()) + nearPt.x;
            handY = Mth.lerp(partialTicks, shooter.yo, shooter.getY()) + nearPt.y;
            handZ = Mth.lerp(partialTicks, shooter.zo, shooter.getZ()) + nearPt.z;
            crouchOffset = shooter.getEyeHeight();
        } else {
            float bodyYawRad = Mth.lerp(partialTicks, shooter.yBodyRotO, shooter.yBodyRot) * ((float)Math.PI / 180F);
            double sinYaw = Math.sin(bodyYawRad);
            double cosYaw = Math.cos(bodyYawRad);
            double sideOffset = (double)handSide * 0.35D;
            handX = Mth.lerp(partialTicks, shooter.xo, shooter.getX()) - cosYaw * sideOffset - sinYaw * 0.7D;
            handY = shooter.yo + (double)shooter.getEyeHeight() + (shooter.getY() - shooter.yo) * (double)partialTicks - 0.40D;
            handZ = Mth.lerp(partialTicks, shooter.zo, shooter.getZ()) - sinYaw * sideOffset + cosYaw * 0.7D;
            crouchOffset = shooter.isCrouching() ? -0.1875F : 0.0F;
        }

        // 双钩并在时在枪口端做左右分离微弱偏移
        GrapplingHookEntity partner = entity.getPartnerHook();
        if (partner != null && !partner.isRemoved()) {
            float viewYaw = shooter.getViewYRot(partialTicks) * ((float)Math.PI / 180F);
            double rightX = Math.cos(viewYaw);
            double rightZ = Math.sin(viewYaw);
            double sideSep = (entity.getId() < partner.getId()) ? -0.05 : 0.05;
            handX += rightX * sideSep;
            handZ += rightZ * sideSep;
        }

        // 钩爪端在世界空间的插值坐标
        double hookX = Mth.lerp(partialTicks, entity.xo, entity.getX());
        double hookY = Mth.lerp(partialTicks, entity.yo, entity.getY()) + 0.15D;
        double hookZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());

        // 相对向量（从钩爪指向手部）
        float relX = (float)(handX - hookX);
        float relY = (float)(handY - hookY) + crouchOffset;
        float relZ = (float)(handZ - hookZ);

        poseStack.pushPose();
        PoseStack.Pose poseEntry = poseStack.last();
        Matrix4f poseMat = poseEntry.pose();
        Matrix3f normMat = poseEntry.normal();

        // 绳索材质色彩
        int cr = 72, cg = 58, cb = 44;
        switch (entity.getHookMaterialId()) {
            case 1 -> { cr = 40; cg = 140; cb = 140; } // 海晶
            case 2 -> { cr = 225; cg = 220; cb = 210; } // 羊毛
            case 3 -> { cr = 44; cg = 40; cb = 48; } // 下界合金
        }

        int steps = 20;
        float sagFactor = (entity.getHookState() == GrapplingHookEntity.State.PULLING) ? 0.03f : 0.22f;
        float horizontalDist = (float)Math.sqrt(relX * relX + relZ * relZ);

        // 计算每段中心点及切线
        Vec3[] centerPoints = new Vec3[steps + 1];
        for (int k = 0; k <= steps; k++) {
            float t = (float)k / (float)steps;
            float px = relX * t;
            float py = relY * t - Mth.sin(t * (float)Math.PI) * sagFactor * Math.min(6.0f, horizontalDist);
            float pz = relZ * t;
            centerPoints[k] = new Vec3(px, py, pz);
        }

        // 光照插值准备
        BlockPos hookBlockPos = BlockPos.containing(hookX, hookY, hookZ);
        BlockPos handBlockPos = BlockPos.containing(handX, handY, handZ);
        int hookBlockLight = entity.level().getBrightness(LightLayer.BLOCK, hookBlockPos);
        int handBlockLight = entity.level().getBrightness(LightLayer.BLOCK, handBlockPos);
        int hookSkyLight = entity.level().getBrightness(LightLayer.SKY, hookBlockPos);
        int handSkyLight = entity.level().getBrightness(LightLayer.SKY, handBlockPos);

        // 1. 建模真正的 3D 棱柱实心管道绳索 (QUADS, entityCutoutNoCull, 保证所有着色器与光影下可见)
        VertexConsumer quadConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        float ropeR = 0.024f; // 4.8cm 直径真实工业绳索

        // 计算每个节点的 4 个横截面顶点
        Vec3[][] ringVertices = new Vec3[steps + 1][4];
        for (int k = 0; k <= steps; k++) {
            Vec3 p = centerPoints[k];
            Vec3 dir;
            if (k < steps) {
                dir = centerPoints[k + 1].subtract(p);
            } else {
                dir = p.subtract(centerPoints[k - 1]);
            }
            double dLen = dir.length();
            if (dLen < 1e-4) dir = new Vec3(0, 1, 0);
            else dir = dir.scale(1.0 / dLen);

            // 水平横截面法线
            double hx = -dir.z;
            double hz = dir.x;
            double hLen = Math.sqrt(hx * hx + hz * hz);
            if (hLen < 1e-4) {
                hx = 1; hz = 0; hLen = 1;
            }
            Vec3 u = new Vec3(hx / hLen * ropeR, 0, hz / hLen * ropeR);
            Vec3 v = dir.cross(u).normalize().scale(ropeR);

            ringVertices[k][0] = p.add(u);
            ringVertices[k][1] = p.add(v);
            ringVertices[k][2] = p.subtract(u);
            ringVertices[k][3] = p.subtract(v);
        }

        // 沿绳索长度封闭 4 个面生成 3D 管壁
        for (int k = 0; k < steps; k++) {
            float t = (float)k / (float)steps;
            int bl = (int)Mth.lerp(t, (float)hookBlockLight, (float)handBlockLight);
            int sl = (int)Mth.lerp(t, (float)hookSkyLight, (float)handSkyLight);
            int light = LightTexture.pack(bl, sl);

            float v0 = (k % 2 == 0) ? 0.0f : 0.5f;
            float v1 = (k % 2 == 0) ? 0.5f : 1.0f;

            for (int f = 0; f < 4; f++) {
                int nextF = (f + 1) % 4;
                Vec3 pA = ringVertices[k][f];
                Vec3 pB = ringVertices[k][nextF];
                Vec3 pC = ringVertices[k + 1][nextF];
                Vec3 pD = ringVertices[k + 1][f];

                Vec3 edge1 = pB.subtract(pA);
                Vec3 edge2 = pD.subtract(pA);
                Vec3 norm = edge1.cross(edge2).normalize();

                quadConsumer.vertex(poseMat, (float)pA.x, (float)pA.y, (float)pA.z).color(cr, cg, cb, 255).uv(0.25f, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normMat, (float)norm.x, (float)norm.y, (float)norm.z).endVertex();
                quadConsumer.vertex(poseMat, (float)pB.x, (float)pB.y, (float)pB.z).color(cr, cg, cb, 255).uv(0.50f, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normMat, (float)norm.x, (float)norm.y, (float)norm.z).endVertex();
                quadConsumer.vertex(poseMat, (float)pC.x, (float)pC.y, (float)pC.z).color(cr, cg, cb, 255).uv(0.50f, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normMat, (float)norm.x, (float)norm.y, (float)norm.z).endVertex();
                quadConsumer.vertex(poseMat, (float)pD.x, (float)pD.y, (float)pD.z).color(cr, cg, cb, 255).uv(0.25f, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normMat, (float)norm.x, (float)norm.y, (float)norm.z).endVertex();
            }
        }

        // 2. 绘制原版 lineStrip 强化远距离与强光影下的可视线
        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lineStrip());
        for (int k = 0; k <= steps; k++) {
            Vec3 p = centerPoints[k];
            float t = (float)k / (float)steps;
            float nextT = (float)(k + 1) / (float)steps;
            float px1 = relX * nextT;
            float py1 = relY * nextT - Mth.sin(nextT * (float)Math.PI) * sagFactor * Math.min(6.0f, horizontalDist);
            float pz1 = relZ * nextT;

            float dx = px1 - (float)p.x;
            float dy = py1 - (float)p.y;
            float dz = pz1 - (float)p.z;
            float dLen = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (dLen < 1e-4) dLen = 1.0f;
            dx /= dLen; dy /= dLen; dz /= dLen;

            lineConsumer.vertex(poseMat, (float)p.x, (float)p.y, (float)p.z)
                    .color(cr, cg, cb, 255)
                    .normal(normMat, dx, dy, dz)
                    .endVertex();
        }

        poseStack.popPose();
    }
}
