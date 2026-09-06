//package com.fib.fib.entity;
//
//import com.fib.fib.FIBMod;
//import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.MobCategory;
//import net.minecraftforge.eventbus.api.IEventBus;
//import net.minecraftforge.registries.DeferredRegister;
//import net.minecraftforge.registries.ForgeRegistries;
//import net.minecraftforge.registries.RegistryObject;
//
//public class ModEntities {
//    public static final DeferredRegister<EntityType<?>> ENTITY_TYPE =
//            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FIBMod.MOD_ID);
//
//    public static final RegistryObject<EntityType<SeatEntity>> SEAT = ENTITY_TYPE.register("seat",
//            () -> EntityType.Builder.<SeatEntity>of(
//                    (p_20722_, p_20723_) -> new SeatEntity(p_20723_), MobCategory.MISC)
//                    .sized(0.0f,0.0f)
//                    .setCustomClientFactory(((spawnEntity, level) -> new SeatEntity(level)))
//                    .build("seat"));
//
//
//    public static void register(IEventBus eventBus) {
//        ENTITY_TYPE.register(eventBus);
//    }
//}
