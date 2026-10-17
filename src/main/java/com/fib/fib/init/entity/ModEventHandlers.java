package com.fib.fib.init.entity;

import com.fib.fib.FIBMod;
import com.fib.fib.init.entity.boss.HajjiAuthorEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventHandlers {
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.HAJJI_AUTHOR.get(), HajjiAuthorEntity.createAttributes().build());
    }

}




