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
 * 支持 4 种材质钩爪头部的立体绘制，以及与玩家枪口/次钩之间的悬链线动态重力绳索渲染。
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

        // 1. 渲染钩爪头部
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()) - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot())));

        renderHookHead(poseStack, buffer.getBuffer(RenderType.entityCutout(getTextureLocation(entity))), packedLight);
        poseStack.popPose();

        // 2. 渲染动态绳索
        renderRope(entity, partialTicks, poseStack, buffer);
    }

    /** 绘制立体钩爪头部（十字交叉锚爪与锚环） */
    private void renderHookHead(PoseStack poseStack, VertexConsumer consumer, int light) {
        poseStack.pushPose();
        poseStack.scale(0.35f, 0.35f, 0.35f);

        PoseStack.Pose last = poseStack.last();
        Matrix4f pose = last.pose();
        Matrix3f normal = last.normal();

        // 绘制正反交叉钩爪翼片
        for (int i = 0; i < 4; i++) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            drawQuad(consumer, pose, normal, light, -0.4f, -0.4f, 0.4f, 0.4f);
        }

        poseStack.popPose();
    }

    private void drawQuad(VertexConsumer consumer, Matrix4f pose, Matrix3f normal, int light,
                          float minX, float minY, float maxX, float maxY) {
        consumer.vertex(pose, minX, minY, 0.0f).color(255, 255, 255, 255).uv(0.0f, 1.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0f, 0.0f, 1.0f).endVertex();
        consumer.vertex(pose, maxX, minY, 0.0f).color(255, 255, 255, 255).uv(1.0f, 1.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0f, 0.0f, 1.0f).endVertex();
        consumer.vertex(pose, maxX, maxY, 0.0f).color(255, 255, 255, 255).uv(1.0f, 0.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0f, 0.0f, 1.0f).endVertex();
        consumer.vertex(pose, minX, maxY, 0.0f).color(255, 255, 255, 255).uv(0.0f, 0.0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0.0f, 0.0f, 1.0f).endVertex();
    }

    /** 绘制连接至玩家手部或双钩搭档的物理绳索 */
    private void renderRope(GrapplingHookEntity entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffer) {
        Player shooter = entity.getShooter();
        if (shooter == null) return;

        // 计算绳索起始端坐标
        Vec3 startPos;
        GrapplingHookEntity partner = entity.getPartnerHook();
        if (partner != null && !partner.isRemoved() && entity.getId() > partner.getId()) {
            // 双钩模式：由 ID 较小的钩爪负责单向绘制两钩之间的连接绳
            startPos = partner.getPosition(partialTicks);
        } else {
            // 单钩或主钩：连接至玩家手部
            startPos = getPlayerHandPos(shooter, partialTicks);
        }

        Vec3 hookPos = entity.getPosition(partialTicks);
        Vec3 delta = startPos.subtract(hookPos);

        poseStack.pushPose();
        VertexConsumer lineConsumer = buffer.getBuffer(RenderType.lines());
        Matrix4f pose = poseStack.last().pose();

        int segments = 16;
        double sagAmount = 0.15; // 悬链线重力下垂量
        if (entity.getHookState() == GrapplingHookEntity.State.PULLING) {
            sagAmount = 0.02; // 紧绷状态下几乎无下垂
        }

        double prevX = 0;
        double prevY = 0;
        double prevZ = 0;

        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;
            double x = delta.x * t;
            double z = delta.z * t;
            double y = delta.y * t - Math.sin(t * Math.PI) * sagAmount * Math.min(10.0, delta.length());

            lineConsumer.vertex(pose, (float) prevX, (float) prevY, (float) prevZ)
                    .color(70, 50, 35, 255)
                    .normal(0, 1, 0)
                    .endVertex();
            lineConsumer.vertex(pose, (float) x, (float) y, (float) z)
                    .color(70, 50, 35, 255)
                    .normal(0, 1, 0)
                    .endVertex();

            prevX = x;
            prevY = y;
            prevZ = z;
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
