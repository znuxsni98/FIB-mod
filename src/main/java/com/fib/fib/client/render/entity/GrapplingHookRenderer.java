package com.fib.fib.client.render.entity;

import com.fib.fib.FIBMod;
import com.fib.fib.init.entity.custom.GrapplingHookEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * 钩爪实体及其物理绳索渲染器。
 * 具备全 3D 实体钩爪头部（实心中心轴、锥形穿透鼻锥、四向折角倒钩爪、后部拴绳锚环）
 * 以及双向十字带厚度 3D 物理编织绳索渲染。
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

        // 2. 渲染全 3D 编织立体绳索
        render3DRope(entity, partialTicks, poseStack, buffer, packedLight);
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

    /** 绘制连接至玩家手部的 3D 十字编织立体绳索 */
    private void render3DRope(GrapplingHookEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Player shooter = entity.getShooter();
        if (shooter == null) return;

        Vec3 startPos = getPlayerHandPos(shooter, partialTicks);
        GrapplingHookEntity partner = entity.getPartnerHook();
        if (partner != null && !partner.isRemoved()) {
            // 双钩激活时在枪口端做左右微弱偏置，使双绳清晰并列
            Vec3 look = shooter.getViewVector(partialTicks);
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            double offset = (entity.getId() < partner.getId()) ? -0.06 : 0.06;
            startPos = startPos.add(right.scale(offset));
        }

        Vec3 hookPos = entity.getPosition(partialTicks);
        Vec3 delta = startPos.subtract(hookPos);

        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();

        // 绳索材质颜色匹配
        int cr = 75, cg = 55, cb = 42;
        switch (entity.getHookMaterialId()) {
            case 1 -> { cr = 35; cg = 135; cb = 135; } // 海晶
            case 2 -> { cr = 220; cg = 215; cb = 205; } // 羊毛
            case 3 -> { cr = 42; cg = 38; cb = 46; } // 下界合金
        }

        int segments = 16;
        double sagAmount = 0.15; // 悬链线重力下垂量
        if (entity.getHookState() == GrapplingHookEntity.State.PULLING) {
            sagAmount = 0.02; // 紧绷状态下几乎无下垂
        }

        Vec3[] points = new Vec3[segments + 1];
        points[0] = Vec3.ZERO;

        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            double x = delta.x * t;
            double z = delta.z * t;
            double y = delta.y * t - Math.sin(t * Math.PI) * sagAmount * Math.min(10.0, delta.length());
            points[i] = new Vec3(x, y, z);
        }

        // 1. 绘制 3D 双交叉立体带 (Leash 渲染器，具备空间宽度与实体光照)
        VertexConsumer leashConsumer = buffer.getBuffer(RenderType.leash());
        float ropeRadius = 0.020f; // 4cm 直径

        for (int i = 1; i <= segments; i++) {
            Vec3 p0 = points[i - 1];
            Vec3 p1 = points[i];

            Vec3 d = p1.subtract(p0);
            double len = d.length();
            if (len < 1e-4) continue;

            // 水平法向量
            double hx = -d.z;
            double hz = d.x;
            double hlen = Math.sqrt(hx * hx + hz * hz);
            if (hlen < 1e-3) {
                hx = 1; hz = 0; hlen = 1;
            }
            double ux = (hx / hlen) * ropeRadius;
            double uz = (hz / hlen) * ropeRadius;

            // 垂直第二法向量
            double nx = d.x / len, ny = d.y / len, nz = d.z / len;
            double vx = ny * uz;
            double vy = nz * ux - nx * uz;
            double vz = -ny * ux;
            double vlen = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (vlen > 1e-4) {
                vx = (vx / vlen) * ropeRadius;
                vy = (vy / vlen) * ropeRadius;
                vz = (vz / vlen) * ropeRadius;
            }

            // 十字交叉带 1 (U 方向)
            leashConsumer.vertex(pose, (float)(p0.x - ux), (float)(p0.y), (float)(p0.z - uz)).color(cr, cg, cb, 255).uv2(packedLight).endVertex();
            leashConsumer.vertex(pose, (float)(p0.x + ux), (float)(p0.y), (float)(p0.z + uz)).color(cr, cg, cb, 255).uv2(packedLight).endVertex();
            leashConsumer.vertex(pose, (float)(p1.x + ux), (float)(p1.y), (float)(p1.z + uz)).color(cr, cg, cb, 255).uv2(packedLight).endVertex();
            leashConsumer.vertex(pose, (float)(p1.x - ux), (float)(p1.y), (float)(p1.z - uz)).color(cr, cg, cb, 255).uv2(packedLight).endVertex();

            // 十字交叉带 2 (V 方向)
            leashConsumer.vertex(pose, (float)(p0.x - vx), (float)(p0.y - vy), (float)(p0.z - vz)).color(cr, cg, cb, 255).uv2(packedLight).endVertex();
            leashConsumer.vertex(pose, (float)(p0.x + vx), (float)(p0.y + vy), (float)(p0.z + vz)).color(cr, cg, cb, 255).uv2(packedLight).endVertex();
            leashConsumer.vertex(pose, (float)(p1.x + vx), (float)(p1.y + vy), (float)(p1.z + vz)).color(cr, cg, cb, 255).uv2(packedLight).endVertex();
            leashConsumer.vertex(pose, (float)(p1.x - vx), (float)(p1.y - vy), (float)(p1.z - vz)).color(cr, cg, cb, 255).uv2(packedLight).endVertex();
        }

        // 2. 绘制中心微线（保障远景下的防走样连续度）
        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lines());
        for (int i = 1; i <= segments; i++) {
            Vec3 p0 = points[i - 1];
            Vec3 p1 = points[i];
            lineConsumer.vertex(pose, (float) p0.x, (float) p0.y, (float) p0.z).color(cr, cg, cb, 255).normal(0, 1, 0).endVertex();
            lineConsumer.vertex(pose, (float) p1.x, (float) p1.y, (float) p1.z).color(cr, cg, cb, 255).normal(0, 1, 0).endVertex();
        }

        poseStack.popPose();
    }

    private Vec3 getPlayerHandPos(Player player, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        boolean isFirstPerson = mc.options.getCameraType().isFirstPerson() && player == mc.player;

        if (isFirstPerson) {
            Vec3 eye = player.getEyePosition(partialTicks);
            Vec3 look = player.getViewVector(partialTicks);
            Vec3 up = new Vec3(0, 1, 0);
            Vec3 right = look.cross(up).normalize();

            // 第一人称右手枪口偏置
            int handSide = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
            return eye.add(right.scale(0.35 * handSide)).add(look.scale(0.4)).subtract(0, 0.25, 0);
        } else {
            // 第三人称根据玩家朝向偏置
            double bodyYaw = Math.toRadians(Mth.lerp(partialTicks, player.yBodyRotO, player.yBodyRot));
            double armOffset = player.getMainArm() == HumanoidArm.RIGHT ? 0.35 : -0.35;
            double x = player.getX() - Math.sin(bodyYaw) * 0.2 + Math.cos(bodyYaw) * armOffset;
            double y = player.getY() + player.getBbHeight() * 0.65;
            double z = player.getZ() + Math.cos(bodyYaw) * 0.2 + Math.sin(bodyYaw) * armOffset;
            return new Vec3(x, y, z);
        }
    }
}
