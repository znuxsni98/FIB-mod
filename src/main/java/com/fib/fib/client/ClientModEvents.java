
package com.fib.fib.client;

import com.fib.fib.FIBMod;
import com.fib.fib.init.entity.ModEntities;
import com.fib.fib.client.render.entity.DimensionalCrackRenderer;
import com.fib.fib.client.render.entity.HajjiAuthorRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HAJJI_AUTHOR.get(), HajjiAuthorRenderer::new);
        event.registerEntityRenderer(ModEntities.DIMENSIONAL_CRACK.get(), DimensionalCrackRenderer::new);
    }
}
