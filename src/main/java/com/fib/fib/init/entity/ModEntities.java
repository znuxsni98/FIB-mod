package com.fib.fib.init.entity;

import com.fib.fib.FIBMod;
import com.fib.fib.init.entity.boss.*;
import com.fib.fib.init.entity.custom.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FIBMod.MOD_ID);

    public static final RegistryObject<EntityType<HajjiAuthorEntity>> HAJJI_AUTHOR =
            ENTITIES.register("hajji_author", () ->
                    EntityType.Builder.of(HajjiAuthorEntity::new, MobCategory.MONSTER)
                            .sized(1.2F, 2.0F)    //尺寸
                            .clientTrackingRange(64)       //更新距离-单位格
                            .fireImmune()                           //免疫火焰
                            .build("hajji_author")
            );

    public static final RegistryObject<EntityType<IceBall>> ICE_BALL =
            ENTITIES.register("ice_ball", () ->
                    EntityType.Builder.<IceBall>of(IceBall::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("ice_ball")
            );

    public static final RegistryObject<EntityType<DimensionalCrackEntity>> DIMENSIONAL_CRACK =
            ENTITIES.register("dimensional_crack", () ->
                    EntityType.Builder.of(DimensionalCrackEntity::new, MobCategory.MISC) // 杂项分类
                            .sized(2.0F, 3.0F)      // 宽 2、高 3；厚度 0.5 由 DimensionalCrackEntity#makeBoundingBox 定
                            .clientTrackingRange(32)
                            .build("dimensional_crack")
            );


    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
