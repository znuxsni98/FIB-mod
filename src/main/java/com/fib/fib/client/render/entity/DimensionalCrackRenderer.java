package com.fib.fib.client.render.entity;

import com.fib.fib.FIBMod;
import com.fib.fib.client.render.ModShaders;
import com.fib.fib.init.entity.custom.DimensionalCrackEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 维度裂缝渲染器。
 *
 * <p>几何只有一个四边形，撕裂轮廓、沟边、气焰、水波纹、闪电全部在片元着色器里逐像素算
 * （见 {@code assets/fib_mod/shaders/core/dimensional_crack.fsh}）。上一版用细分网格在顶点级
 * 近似这些效果，但顶点级做不出逐像素的遮罩轮廓，边缘的杂乱度有上限。
 *
 * <p>顶点侧的职责只有三件事：
 * <ol>
 *   <li>把四边形摆到裂缝的位置和朝向，按开启动画缩放；</li>
 *   <li>把<b>归一化局部坐标</b>写进 UV0 —— 撕裂边缘正好是单位圆，片元里直接拿来做极坐标；</li>
 *   <li>把视差、时间、淡入作为 uniform 传下去。</li>
 * </ol>
 */
public class DimensionalCrackRenderer extends EntityRenderer<DimensionalCrackEntity> {

    private static final ResourceLocation STARS_TEXTURE =
            new ResourceLocation(FIBMod.MOD_ID, "textures/entity/custom/dimensional_crack_stars.png");

    /** 水波纹贴图，绑到着色器的 Sampler1（见 {@link ModShaders}） */
    private static final ResourceLocation RIPPLE_TEXTURE =
            new ResourceLocation(FIBMod.MOD_ID, "textures/entity/custom/dimensional_crack_ripple.png");

    /** 闪电贴图，绑到着色器的 Sampler2 */
    private static final ResourceLocation LIGHTNING_TEXTURE =
            new ResourceLocation(FIBMod.MOD_ID, "textures/entity/custom/dimensional_crack_lightning.png");

    /** 半宽（格）—— 与实体碰撞箱 2×3 对应 */
    private static final float HALF_W = 1.0F;
    /** 半高（格）—— 明显大于半宽，得到竖直椭圆 */
    private static final float HALF_H = 1.5F;

    /** 星空贴图采样密度（每格多少 UV）。0.30 × 半高 1.5 ≈ 0.45，贴图刚好铺满且不被拉伸 */
    private static final float UV_PER_BLOCK = 0.30F;

    /**
     * 四边形半边长（归一化单位，1.0 = 撕裂边缘）。
     * 最外层的元素是气焰（到 1.45 左右）和水波纹环（到 1.66 左右），
     * 1.9 已经够用，片元里再 discard 掉 1.80 以外 —— 少画一圈没用的像素。
     */
    private static final float QUAD_R = 1.9F;

    private static final int FULL_LIGHT = 15728880;

    // ==================== 视差 ====================
    /** 视角（转头）引起的偏移系数 */
    private static final float VIEW_PARALLAX = 0.30F;
    /** 位移（走动）引起的偏移系数 */
    private static final float MOVE_PARALLAX = 0.08F;
    /** 星云的虚拟深度（格）：越大视差越平缓、越"远" */
    private static final float NEBULA_DEPTH = 8.5F;

    private float parallaxU;
    private float parallaxV;

    public DimensionalCrackRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DimensionalCrackEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        float openProgress = entity.getOpenProgress(partialTick);
        if (openProgress <= 0.0F) {
            return;
        }

        ShaderInstance shader = ModShaders.crackShader();
        if (shader == null) {
            return;
        }

        float time = entity.getAge() + partialTick;
        float ease = smoothstep(openProgress);

        poseStack.pushPose();

        // 以碰撞箱中心为裂缝中心
        poseStack.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));

        // 开启动画：从中心张开，过程中略微拉长（撕裂感），最后收回标准竖直椭圆
        float grow = 0.18F + 0.82F * ease;
        float stretch = 1.0F + 0.30F * (1.0F - ease);
        float hw = HALF_W * grow;
        float hh = HALF_H * grow * stretch;

        updateParallax(entity);

        Matrix4f matrix = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        float openAlpha = Mth.clamp(ease * 1.45F, 0.0F, 1.0F);

        // Sampler0 由 RenderType 的 TextureStateShard 指派，另外两张贴图没有对应的 state shard
        // （overlay/lightmap 占的是 1/2 号槽），只能自己往纹理槽里塞。
        // VertexBuffer#_drawWithShader 每次立即绘制都会把 0..11 号槽读出来 setSampler，
        // 所以只要在 endBatch 之前写好，这一批就会用上。
        RenderSystem.setShaderTexture(1, RIPPLE_TEXTURE);
        RenderSystem.setShaderTexture(2, LIGHTNING_TEXTURE);

        shader.safeGetUniform("CrackTime").set(time);
        shader.safeGetUniform("CrackParU").set(parallaxU);
        shader.safeGetUniform("CrackParV").set(parallaxV);
        shader.safeGetUniform("CrackScaleU").set(HALF_W * UV_PER_BLOCK);
        shader.safeGetUniform("CrackScaleV").set(HALF_H * UV_PER_BLOCK);
        shader.safeGetUniform("CrackAlpha").set(openAlpha);

        // uniform 是整个 ShaderInstance 共享的，而批处理要到 flush 时才真正画。
        // 同时存在两条裂缝的话后者会把前者的 uniform 覆盖掉，所以每一遍都立刻结束这一批，
        // 保证每条裂缝用自己的时间/视差。endBatch(RenderType) 只冲这一种，不动别的。
        MultiBufferSource.BufferSource immediate =
                bufferSource instanceof MultiBufferSource.BufferSource bs ? bs : null;

        // ---- 第一遍：轮廓内部的实心层，写深度 ----
        // 这一遍必须每一帧都画：它是唯一会往深度缓冲里留下记录的遍，
        // 少了它，裂缝后面的实体就会盖到裂缝上（"透过裂缝能看见别的实体"）。
        // 开启动画期间 CrackAlpha 还小，片元里 alpha 也跟着小，提前 discard 掉，不会挡东西。
        shader.safeGetUniform("CrackMode").set(0.0F);
        emitQuad(bufferSource.getBuffer(ModShaders.crackCoreRenderType()), matrix, normal, hw, hh);
        if (immediate != null) {
            immediate.endBatch(ModShaders.crackCoreRenderType());
        }

        // ---- 第二遍：边缘层，不写深度 ----
        shader.safeGetUniform("CrackMode").set(1.0F);
        emitQuad(bufferSource.getBuffer(ModShaders.crackGlowRenderType()), matrix, normal, hw, hh);
        if (immediate != null) {
            immediate.endBatch(ModShaders.crackGlowRenderType());
        }

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /** 把一个覆盖整条裂缝的四边形写进 consumer */
    private void emitQuad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float hw, float hh) {
        quadVertex(consumer, matrix, normal, -QUAD_R, -QUAD_R, hw, hh);
        quadVertex(consumer, matrix, normal, QUAD_R, -QUAD_R, hw, hh);
        quadVertex(consumer, matrix, normal, QUAD_R, QUAD_R, hw, hh);
        quadVertex(consumer, matrix, normal, -QUAD_R, QUAD_R, hw, hh);
    }

    /**
     * 四边形顶点。位置 = 归一化坐标 × 实际半宽/半高；UV0 存归一化坐标本身，片元里当极坐标用。
     * 顶点色留白，片元只取它的 alpha。
     */
    private void quadVertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal,
                            float u, float v, float hw, float hh) {
        consumer.vertex(matrix, u * hw, v * hh, 0.0F)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_LIGHT)
                .normal(normal, 0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    /**
     * 视差伪 3D。
     *
     * <p><b>符号规则：星空永远和相机的"等效位移"同向。</b>
     * 往右走，星空在裂缝里也往右移；往右转，同样往右移。反直觉，但这是对的：
     * 近处的裂缝边框比远处的星空位移更大，所以"更远的东西相对边框看起来在往同方向偏"。
     * 举例（裂缝在 z=0，视线沿 +Z，星在 z=100）：
     * <pre>
     *   眼睛 x=0   → 裂缝中心 0°，   星 0°
     *   眼睛 x=+2  → 裂缝中心 -21.8°，星 -1.09°
     * </pre>
     * 裂缝往左挪了 21.8°，星只挪了 1.09°，所以相对裂缝，星是往<b>右</b>偏的。
     *
     * <p>纯旋转物理上不该产生位移（裂缝和星空都固定在世界上，转视线不改变二者的相对关系），
     * 这里按同一条规则风格化处理：把朝向投影当成"若朝那个方向走"的等效位移 ——
     * 于是视角向左上 → 星空向左上移。
     *
     * <p>两项叠加，都是<b>相机状态的纯函数</b>——相机不动，星云就完全静止（没有自漂移）。
     */
    private void updateParallax(DimensionalCrackEntity entity) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        // 裂缝自身的朝向（与渲染时 Axis.YP.rotationDegrees(-yRot) 保持一致）
        float yawRad = -entity.getYRot() * Mth.DEG_TO_RAD;
        float rightX = Mth.cos(yawRad);
        float rightZ = -Mth.sin(yawRad);

        // ---- 视角项：把相机朝向投影到裂缝的"右/上"轴上，当成等效位移 ----
        float camYaw = camera.getYRot() * Mth.DEG_TO_RAD;
        float camPitch = camera.getXRot() * Mth.DEG_TO_RAD;
        float cosPitch = Mth.cos(camPitch);
        float lookX = -Mth.sin(camYaw) * cosPitch;
        float lookY = -Mth.sin(camPitch);
        float lookZ = Mth.cos(camYaw) * cosPitch;

        float viewU = lookX * rightX + lookZ * rightZ;
        float viewV = lookY;

        // ---- 位移项：相机相对裂缝中心的实际偏移 ----
        Vector3f cam = camera.getPosition().toVector3f();
        float cx = (float) entity.getX();
        float cy = (float) (entity.getY() + entity.getBbHeight() * 0.5D);
        float cz = (float) entity.getZ();

        float dx = cam.x - cx;
        float dy = cam.y - cy;
        float dz = cam.z - cz;

        float moveU = dx * rightX + dz * rightZ;
        float dist = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        float moveFactor = MOVE_PARALLAX * (NEBULA_DEPTH / (NEBULA_DEPTH + dist));

        // 等效位移 → 内容位移同向。u 轴取正；v 轴因为贴图 v 朝下、顶点 y 朝上，整体反号。
        parallaxU = -(moveU * moveFactor + viewU * VIEW_PARALLAX);
        parallaxV = (dy * moveFactor + viewV * VIEW_PARALLAX);
    }

    private static float smoothstep(float x) {
        float t = Mth.clamp(x, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    @Override
    public ResourceLocation getTextureLocation(DimensionalCrackEntity entity) {
        return STARS_TEXTURE;
    }
}
