package com.fib.fib.init.item;

import com.fib.fib.FIBMod;
import com.fib.fib.init.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab>CREATIVE_MODE_TABS =
           DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FIBMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> FIBMod_TAB =
            CREATIVE_MODE_TABS.register("fib_tab",() -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.FIB_LOGO_MOD.get()))
                    .title(Component.translatable("itemGroup.fib_tab"))
                    .displayItems((pParameters, pOutput) -> {

                        //pOutput.accept(ModItems.物品.get());
                        pOutput.accept(ModBlocks.ICE_MAKER.get());
                        pOutput.accept(ModBlocks.RADIO_STATION.get());
                        pOutput.accept(ModBlocks.BOWL_BLOCK.get());
                        pOutput.accept(ModBlocks.ENGINEER_WORKBENCH.get());
                        pOutput.accept(ModBlocks.SCIENTISTS_EXPERIMENTAL_PLATFORM.get());
                        pOutput.accept(ModBlocks.CRATE.get());
                        pOutput.accept(ModBlocks.OIL_DRUM.get());
                        pOutput.accept(ModBlocks.TRASH_CAN.get());
                        pOutput.accept(ModBlocks.CORPSE_1.get());
                        pOutput.accept(ModBlocks.CORPSE_2.get());
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
