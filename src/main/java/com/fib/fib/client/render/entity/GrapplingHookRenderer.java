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
 * 钩爪实体及其绳索渲染器。
 * 钩爪头部为 3D 建模；绳索改用原版拴绳（leash）同款渲染算法。
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

    private static final int ROPE_STEPS = 24;
    private static final float ROPE_HALF_WIDTH = 0.025F;

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
    public boolean shouldRender(GrapplingHookEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(GrapplingHookEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));
        render3DHookHead(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity))), packedLight);
        poseStack.popPose();

        renderLeashStyleRope(entity, partialTicks, poseStack, buffer);
    }

    private void render3DHookHead(PoseStack poseStack, VertexConsumer consumer, int light) {
        poseStack.pushPose();
        poseStack.scale(0.42f, 0.42f, 0.42f);

        PoseStack.Pose currentPose = poseStack.last();
        Matrix4f pose = currentPose.pose();
        Matrix3f normal = currentPose.normal();

        drawBox(consumer, pose, normal, light,
                -0.32f, -0.04f, -0.04f,
                0.20f, 0.04f, 0.04f,
                0.25f, 0.0f, 0.5f, 0.75f);

        drawConeTip(consumer, pose, normal, light, 0.20f, 0.38f, 0.04f);

        drawBox(consumer, pose, normal, light,
                -0.42f, -0.07f, -0.07f,
                -0.32f, 0.07f, 0.07f,
                0.5f, 0.0f, 0.75f, 0.25f);

        for (int i = 0; i < 4; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(i * 90.0f));
            PoseStack.Pose flukePose = poseStack.last();
            Matrix4f fMat = flukePose.pose();
            Matrix3f fNorm = flukePose.normal();

            drawBox(consumer, fMat, fNorm, light,
                    0.10f, 0.04f, -0.02f,
                    0.18f, 0.24f, 0.02f,
                    0.0f, 0.25f, 0.25f, 0.75f);

            drawBox(consumer, fMat, fNorm, light,
                    -0.04f, 0.20f, -0.02f,
                    0.12f, 0.26f, 0.02f,
                    0.0f, 0.0f, 0.25f, 0.25f);

            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void drawConeTip(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int light,
                            float startX, float endX, float r) {
        drawQuad(consumer, pose, normal, light,
                startX, r, -r, 0.25f, 0.75f,
                startX, r, r, 0.5f, 0.75f,
                endX, 0, 0, 0.5f, 1.0f,
                endX, 0, 0, 0.25f, 1.0f,
                0.5f, 1.0f, 0.0f);
        drawQuad(consumer, pose, normal, light,
                startX, -r, r, 0.25f, 0.75f,
                startX, -r, -r, 0.5f, 0.75f,
                endX, 0, 0, 0.5f, 1.0f,
                endX, 0, 0, 0.25f, 1.0f,
                0.5f, -1.0f, 0.0f);
        drawQuad(consumer, pose, normal, light,
                startX, -r, r, 0.25f, 0.75f,
                startX, r, r, 0.5f, 0.75f,
                endX, 0, 0, 0.5f, 1.0f,
                endX, 0, 0, 0.25f, 1.0f,
                0.5f, 0.0f, 1.0f);
        drawQuad(consumer, pose, normal, light,
                startX, r, -r, 0.25f, 0.75f,
                startX, -r, -r, 0.5f, 0.75f,
                endX, 0, 0, 0.5f, 1.0f,
                endX, 0, 0, 0.25f, 1.0f,
                0.5f, 0.0f, -1.0f);
    }

    private void drawBox(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int light,
                         float minX, float minY, float minZ,
                         float maxX, float maxY, float maxZ,
                         float uMin, float vMin, float uMax, float vMax) {
        drawQuad(consumer, pose, normal, light,
                minX, maxY, minZ, uMin, vMin,
                minX, maxY, maxZ, uMin, vMax,
                maxX, maxY, maxZ, uMax, vMax,
                maxX, maxY, minZ, uMax, vMin,
                0, 1, 0);
        drawQuad(consumer, pose, normal, light,
                maxX, minY, minZ, uMin, vMin,
                maxX, minY, maxZ, uMin, vMax,
                minX, minY, maxZ, uMax, vMax,
                minX, minY, minZ, uMax, vMin,
                0, -1, 0);
        drawQuad(consumer, pose, normal, light,
                maxX, minY, minZ, uMin, vMin,
                maxX, maxY, minZ, uMin, vMax,
                maxX, maxY, maxZ, uMax, vMax,
                maxX, minY, maxZ, uMax, vMin,
                1, 0, 0);
        drawQuad(consumer, pose, normal, light,
                minX, minY, maxZ, uMin, vMin,
                minX, maxY, maxZ, uMin, vMax,
                minX, maxY, minZ, uMax, vMax,
                minX, minY, minZ, uMax, vMin,
                -1, 0, 0);
        drawQuad(consumer, pose, normal, light,
                minX, minY, maxZ, uMin, vMin,
                maxX, minY, maxZ, uMin, vMax,
                maxX, maxY, maxZ, uMax, vMax,
                minX, maxY, maxZ, uMax, vMin,
                0, 0, 1);
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

    /**
     * 原版拴绳（leash）风格绳索渲染。
     * 与 MobRenderer#renderLeash 相同：RenderType.leash() + 两趟顶点对（正向/反向各 24 段）。
     */
    private void renderLeashStyleRope(GrapplingHookEntity entity, float partialTicks,
                                      PoseStack poseStack, MultiBufferSource buffer) {
        // ===== 双钩模式：两枚钩爪已建立连接后，绳子只连接两枚钩爪，与钩爪枪无关 =====
        GrapplingHookEntity partner = entity.getPartnerHook();
        if (partner != null && !partner.isRemoved()) {
            // 只由 id 较小的一方绘制，避免同一条绳画两遍
            if (entity.getId() > partner.getId()) return;

            double ax = Mth.lerp(partialTicks, entity.xo, entity.getX());
            double ay = Mth.lerp(partialTicks, entity.yo, entity.getY()) + 0.15D;
            double az = Mth.lerp(partialTicks, entity.zo, entity.getZ());
            double bx = Mth.lerp(partialTicks, partner.xo, partner.getX());
            double by = Mth.lerp(partialTicks, partner.yo, partner.getY()) + 0.15D;
            double bz = Mth.lerp(partialTicks, partner.zo, partner.getZ());

            renderRopeBetween(entity, poseStack, buffer, ax, ay, az, bx, by, bz);
            return;
        }

        // 保护：以下两种情况都不能退回到"玩家手 → 钩爪"的渲染，
        // 否则会画出"玩家与钩爪连接"的假象：
        //  1. 该钩爪本应已配对（partner id 有效），但对方实体当前未加载/未解析；
        //  2. 它本身就是双钩模块发射的钩爪 —— 双钩的绳只连两钩之间，永不连玩家手。
        if (entity.getPartnerHookId() != -1 || entity.isDualHookMode()) {
            return;
        }

        Player shooter = entity.getShooter();
        if (shooter == null) return;

        int handSide = shooter.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        if (shooter.getOffhandItem().getItem() instanceof GrapplingHookGunItem
                && !(shooter.getMainHandItem().getItem() instanceof GrapplingHookGunItem)) {
            handSide = -handSide;
        }

        double handX, handY, handZ;
        if ((this.entityRenderDispatcher.options == null
                || this.entityRenderDispatcher.options.getCameraType().isFirstPerson())
                && shooter == Minecraft.getInstance().player) {
            double fovFactor = 960.0D / this.entityRenderDispatcher.options.fov().get();
            Vec3 nearPt = this.entityRenderDispatcher.camera.getNearPlane()
                    .getPointOnPlane((float) handSide * 0.48F, -0.22F);
            nearPt = nearPt.scale(fovFactor);
            float attackAnim = shooter.getAttackAnim(partialTicks);
            float swingSin = Mth.sin(Mth.sqrt(attackAnim) * (float) Math.PI);
            nearPt = nearPt.yRot(swingSin * 0.5F);
            nearPt = nearPt.xRot(-swingSin * 0.7F);
            handX = Mth.lerp(partialTicks, shooter.xo, shooter.getX()) + nearPt.x;
            handY = Mth.lerp(partialTicks, shooter.yo, shooter.getY()) + nearPt.y + (double) shooter.getEyeHeight();
            handZ = Mth.lerp(partialTicks, shooter.zo, shooter.getZ()) + nearPt.z;
        } else {
            float bodyYawRad = Mth.lerp(partialTicks, shooter.yBodyRotO, shooter.yBodyRot) * ((float) Math.PI / 180F);
            double sinYaw = Math.sin(bodyYawRad);
            double cosYaw = Math.cos(bodyYawRad);
            double sideOffset = (double) handSide * 0.38D;
            handX = Mth.lerp(partialTicks, shooter.xo, shooter.getX()) - cosYaw * sideOffset - sinYaw * 0.7D;
            handY = shooter.yo + (double) shooter.getEyeHeight()
                    + (shooter.getY() - shooter.yo) * (double) partialTicks - 0.40D;
            handZ = Mth.lerp(partialTicks, shooter.zo, shooter.getZ()) - sinYaw * sideOffset + cosYaw * 0.7D;
            if (shooter.isCrouching()) {
                handY -= 0.1875D;
            }
        }

        double hookX = Mth.lerp(partialTicks, entity.xo, entity.getX());
        double hookY = Mth.lerp(partialTicks, entity.yo, entity.getY()) + 0.15D;
        double hookZ = Mth.lerp(partialTicks, entity.zo, entity.getZ());

        poseStack.pushPose();
        PoseStack.Pose poseEntry = poseStack.last();
        Matrix4f poseMat = poseEntry.pose();

        float dx = (float) (handX - hookX);
        float dy = (float) (handY - hookY);
        float dz = (float) (handZ - hookZ);

        BlockPos hookPos = BlockPos.containing(hookX, hookY, hookZ);
        BlockPos handPos = BlockPos.containing(handX, handY, handZ);
        int hookBlockLight = entity.level().getBrightness(LightLayer.BLOCK, hookPos);
        int handBlockLight = entity.level().getBrightness(LightLayer.BLOCK, handPos);
        int hookSkyLight = entity.level().getBrightness(LightLayer.SKY, hookPos);
        int handSkyLight = entity.level().getBrightness(LightLayer.SKY, handPos);

        VertexConsumer consumer = buffer.getBuffer(RenderType.leash());

        float horizInv = Mth.invSqrt(dx * dx + dz * dz) * ROPE_HALF_WIDTH / 2.0F;
        float nx = dz * horizInv;
        float nz = dx * horizInv;

        for (int i = 0; i <= ROPE_STEPS; i++) {
            addRopeVertexPair(consumer, poseMat, dx, dy, dz,
                    hookBlockLight, handBlockLight, hookSkyLight, handSkyLight,
                    ROPE_HALF_WIDTH, 0.0F, nx, nz, i, false);
        }
        for (int i = ROPE_STEPS; i >= 0; i--) {
            addRopeVertexPair(consumer, poseMat, dx, dy, dz,
                    hookBlockLight, handBlockLight, hookSkyLight, handSkyLight,
                    ROPE_HALF_WIDTH, 0.0F, nx, nz, i, true);
        }

        poseStack.popPose();
    }

    /** 在两点之间绘制一段原版拴绳风格的绳索。 */
    private void renderRopeBetween(GrapplingHookEntity entity, PoseStack poseStack, MultiBufferSource buffer,
                                   double ax, double ay, double az, double bx, double by, double bz) {
        poseStack.pushPose();
        Matrix4f poseMat = poseStack.last().pose();

        float dx = (float) (bx - ax);
        float dy = (float) (by - ay);
        float dz = (float) (bz - az);

        BlockPos pa = BlockPos.containing(ax, ay, az);
        BlockPos pb = BlockPos.containing(bx, by, bz);
        int aBlock = entity.level().getBrightness(LightLayer.BLOCK, pa);
        int bBlock = entity.level().getBrightness(LightLayer.BLOCK, pb);
        int aSky = entity.level().getBrightness(LightLayer.SKY, pa);
        int bSky = entity.level().getBrightness(LightLayer.SKY, pb);

        VertexConsumer consumer = buffer.getBuffer(RenderType.leash());
        float horizInv = Mth.invSqrt(dx * dx + dz * dz) * ROPE_HALF_WIDTH / 2.0F;
        float nx = dz * horizInv;
        float nz = dx * horizInv;

        for (int i = 0; i <= ROPE_STEPS; i++) {
            addRopeVertexPair(consumer, poseMat, dx, dy, dz,
                    aBlock, bBlock, aSky, bSky, ROPE_HALF_WIDTH, 0.0F, nx, nz, i, false);
        }
        for (int i = ROPE_STEPS; i >= 0; i--) {
            addRopeVertexPair(consumer, poseMat, dx, dy, dz,
                    aBlock, bBlock, aSky, bSky, ROPE_HALF_WIDTH, 0.0F, nx, nz, i, true);
        }
        poseStack.popPose();
    }

    private static void addRopeVertexPair(VertexConsumer consumer, Matrix4f pose,
                                          float dx, float dy, float dz,
                                          int blStart, int blEnd, int slStart, int slEnd,
                                          float halfWidthY, float dyOffset,
                                          float nx, float nz, int index, boolean reverse) {
        float f = (float) index / (float) ROPE_STEPS;
        int bl = (int) Mth.lerp(f, (float) blStart, (float) blEnd);
        int sl = (int) Mth.lerp(f, (float) slStart, (float) slEnd);
        int packedLight = LightTexture.pack(bl, sl);

        float shade = (index % 2 == (reverse ? 1 : 0)) ? 0.7F : 1.0F;
        float r = 0.5F * shade;
        float g = 0.4F * shade;
        float b = 0.3F * shade;

        float xOff = dx * f;
        float yOff = (dy > 0.0F) ? (dy * f * f) : (dy - dy * (1.0F - f) * (1.0F - f));
        float zOff = dz * f;

        consumer.vertex(pose, xOff - nx, yOff + halfWidthY, zOff + nz)
                .color(r, g, b, 1.0F).uv2(packedLight).endVertex();
        consumer.vertex(pose, xOff + nx, yOff + dyOffset - halfWidthY, zOff - nz)
                .color(r, g, b, 1.0F).uv2(packedLight).endVertex();
    }
}
