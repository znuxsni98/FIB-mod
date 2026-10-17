package com.fib.fib.client.render;

import com.fib.fib.FIBMod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

/**
 * 维度裂缝用的自定义 core shader 与 RenderType。
 *
 * <p>资源在 {@code assets/fib_mod/shaders/core/dimensional_crack.{json,vsh,fsh}}。
 * 注册走 {@link RegisterShadersEvent}（模组总线），每次资源重载都会重建 shader 实例，
 * 这里保存的引用会跟着更新；{@link RenderStateShard.ShaderStateShard} 拿的是 Supplier，
 * 所以 RenderType 不用跟着重建。这个模式照抄 Forge 自己的 rendertype_entity_unlit_translucent。
 *
 * <p>注意 {@code NO_CULL} / {@code TRANSLUCENT_TRANSPARENCY} / {@code COLOR_WRITE} 这几个现成常量
 * 在 {@link RenderStateShard} 里是 protected，模组包外拿不到，所以这里用它们对应的公开类自己 new 一份。
 * 另外 shader JSON 里的 {@code blend} 块会在每次着色器生效时被 {@code ShaderInstance#apply} 执行，
 * 是它最终决定混合方式。
 */
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModShaders {

    private static final ResourceLocation SHADER_ID =
            new ResourceLocation(FIBMod.MOD_ID, "dimensional_crack");

    private static final ResourceLocation STARS_TEXTURE =
            new ResourceLocation(FIBMod.MOD_ID, "textures/entity/custom/dimensional_crack_stars.png");

    /** shader 未加载完成时为 null，渲染器会直接跳过 */
    private static ShaderInstance crackShader;

    /** 标准 alpha 混合，且不写深度（裂缝是透明物体，写入深度会把后面的东西切掉） */
    private static final RenderStateShard.TransparencyStateShard CRACK_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "fib_crack_translucent",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFuncSeparate(
                                GlStateManager.SourceFactor.SRC_ALPHA,
                                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                                GlStateManager.SourceFactor.ONE,
                                GlStateManager.DestFactor.ZERO);
                        RenderSystem.depthMask(false);
                    },
                    () -> {
                        RenderSystem.depthMask(true);
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    });

    private static final RenderStateShard.ShaderStateShard CRACK_SHADER_STATE =
            new RenderStateShard.ShaderStateShard(() -> crackShader);

    /**
     * 裂缝分两遍画，两遍只差一个深度写入开关。
     *
     * <p>{@code CORE} 是轮廓内部的实心部分（片元里 alpha 恒为 1），<b>写深度</b>：
     * 不写的话，裂缝后面的实体虽然更远，但因为裂缝没在深度缓冲里留下记录，
     * 它们照样能通过深度测试、盖到裂缝上 —— 表现就是"能透过裂缝看见别的实体"。
     *
     * <p>{@code GLOW} 是边缘、气焰、水波纹、闪电这些透明部分，<b>不写深度</b>：
     * 写了的话这一圈半透明的像素会把后面真正该看到的东西切掉。
     *
     * <p>两遍贴在同一平面上，深度完全相同，靠默认的 LEQUAL 通过，不会 z-fighting。
     * 双面可见：裂缝是一张平面，从背后也要能看见，所以关掉背面剔除。
     */
    private static final RenderType CRACK_CORE_RENDER_TYPE =
            createCrackRenderType("fib_dimensional_crack_core", true);

    private static final RenderType CRACK_GLOW_RENDER_TYPE =
            createCrackRenderType("fib_dimensional_crack_glow", false);

    private static RenderType createCrackRenderType(String name, boolean writeDepth) {
        return RenderType.create(
                name,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(CRACK_SHADER_STATE)
                        .setTextureState(new RenderStateShard.TextureStateShard(STARS_TEXTURE, false, false))
                        .setTransparencyState(CRACK_TRANSPARENCY)
                        .setCullState(new RenderStateShard.CullStateShard(false))
                        .setWriteMaskState(new RenderStateShard.WriteMaskStateShard(true, writeDepth))
                        .createCompositeState(false));
    }

    private ModShaders() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), SHADER_ID, DefaultVertexFormat.NEW_ENTITY),
                shader -> crackShader = shader);
    }

    public static ShaderInstance crackShader() {
        return crackShader;
    }

    /** 轮廓内部的实心层，先画、写深度 */
    public static RenderType crackCoreRenderType() {
        return CRACK_CORE_RENDER_TYPE;
    }

    /** 边缘/气焰/水波纹/闪电层，后画、不写深度 */
    public static RenderType crackGlowRenderType() {
        return CRACK_GLOW_RENDER_TYPE;
    }
}
