package com.fib.fib.client.render.entity;

import com.fib.fib.init.entity.custom.IceBall;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

/**
 * 冰球投射物渲染器，复用原版投掷物渲染（显示物品贴图）。
 */
public class IceBallRenderer extends ThrownItemRenderer<IceBall> {
    public IceBallRenderer(EntityRendererProvider.Context context) {
        super(context, 1.0F, true);
    }
}
