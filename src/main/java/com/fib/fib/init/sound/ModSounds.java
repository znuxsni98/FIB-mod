package com.fib.fib.init.sound;

import com.fib.fib.FIBMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.util.ForgeSoundType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENT =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FIBMod.MOD_ID);
//放置PLACE  破坏BREAK  挖掘HIT  行走STEP  跌落FALL   无声音"empty"(应该)


    public static final RegistryObject<SoundEvent> BLOCK_PLACE = registerSoundEvent("block_place");
    public static final RegistryObject<SoundEvent> BLOCK_BREAK = registerSoundEvent("block_break");
    public static final RegistryObject<SoundEvent> BLOCK_HIT = registerSoundEvent("block_hit");
    public static final RegistryObject<SoundEvent> BLOCK_STEP = registerSoundEvent("block_step");
    public static final RegistryObject<SoundEvent> BLOCK_FALL = registerSoundEvent("empty");

    public static final ForgeSoundType BLOCK_SOUND = new ForgeSoundType(1.0f,1.0f,
            BLOCK_BREAK, BLOCK_STEP, BLOCK_PLACE, BLOCK_HIT, BLOCK_FALL);



    public static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id =ResourceLocation.fromNamespaceAndPath(FIBMod.MOD_ID,name);
        return SOUND_EVENT.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }


    public static void register(IEventBus eventBus) {
        SOUND_EVENT.register(eventBus);
    }
}
