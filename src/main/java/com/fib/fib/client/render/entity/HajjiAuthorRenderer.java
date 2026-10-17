package com.fib.fib.client.render.entity;

import com.fib.fib.client.model.HajjiAuthorModel;
import com.fib.fib.init.entity.boss.HajjiAuthorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HajjiAuthorRenderer extends GeoEntityRenderer<HajjiAuthorEntity> {
    public HajjiAuthorRenderer(EntityRendererProvider.Context context) {
        super(context, new HajjiAuthorModel());
        this.shadowRadius = 1.0F;
    }
}