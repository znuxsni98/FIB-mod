package com.fib.fib.init.potion;

import com.fib.fib.FIBMod;
import com.fib.fib.init.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 冻结药水（移植自 26.4-snapshot-3 的 Potions.FREEZING / LONG_FREEZING）。
 * 数值：普通 900 tick（45s），长版 1800 tick（90s，相当于红石延长）。
 */
public class ModPotions {

    public static final DeferredRegister<Potion> POTIONS =
            DeferredRegister.create(ForgeRegistries.POTIONS, FIBMod.MOD_ID);

    public static final RegistryObject<Potion> FREEZING =
            POTIONS.register("freezing", () -> new Potion(
                    "freezing",
                    new MobEffectInstance(ModEffects.FREEZING.get(), 900)));

    public static final RegistryObject<Potion> LONG_FREEZING =
            POTIONS.register("long_freezing", () -> new Potion(
                    "long_freezing",
                    new MobEffectInstance(ModEffects.FREEZING.get(), 1800)));

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
    }
}
