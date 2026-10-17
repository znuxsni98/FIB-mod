package com.fib.fib.client.model;

import com.fib.fib.FIBMod;
import com.fib.fib.init.entity.boss.HajjiAuthorEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HajjiAuthorModel extends GeoModel<HajjiAuthorEntity> {
    @Override
    public ResourceLocation getModelResource(HajjiAuthorEntity object) {
        return new ResourceLocation(FIBMod.MOD_ID, "geo/hajji_author.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(HajjiAuthorEntity object) {
        return new ResourceLocation(FIBMod.MOD_ID, "textures/entity/boss/hajji_author/hajji_author.png");
    }

    @Override
    public ResourceLocation getAnimationResource(HajjiAuthorEntity object) {
        return new ResourceLocation(FIBMod.MOD_ID, "animations/boss/hajji_author/hajji_author.animation.json");
    }
}
