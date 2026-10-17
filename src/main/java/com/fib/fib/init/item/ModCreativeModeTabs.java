package com.fib.fib.init.item;

import com.fib.fib.FIBMod;
import com.fib.fib.init.block.ModBlocks;
import com.fib.fib.init.potion.ModPotions;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
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

                        pOutput.accept(ModItems.CANNED_MUSHROOMS.get());
                        pOutput.accept(ModItems.SPRING_AND_AUTUMN_INTESTINES.get());
                        pOutput.accept(ModItems.WILD_DOG_MILK.get());
                        pOutput.accept(ModItems.SOLAR_PANEL_CONTROLLER.get());
                        pOutput.accept(ModItems.ICE_BALL.get());
                        pOutput.accept(ModItems.GRAPPLING_HOOK_GUN.get());
                        pOutput.accept(ModItems.BASIC_HOOK.get());
                        pOutput.accept(ModItems.PRISMARINE_HOOK.get());
                        pOutput.accept(ModItems.WOOL_HOOK.get());
                        pOutput.accept(ModItems.NETHERITE_HOOK.get());
                        pOutput.accept(ModItems.PRESSURIZED_MODULE.get());
                        pOutput.accept(ModItems.SMART_UPGRADE_MODULE.get());
                        pOutput.accept(ModItems.DUAL_HOOK_MODULE.get());
                        pOutput.accept(ModItems.ITEM_PROTECTION_MODULE.get());
                        pOutput.accept(ModItems.MINING_MODULE.get());
                        pOutput.accept(ModItems.GRAPPLE_MODULE.get());

                        pOutput.accept(PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotions.FREEZING.get()));
                        pOutput.accept(PotionUtils.setPotion(new ItemStack(Items.POTION), ModPotions.LONG_FREEZING.get()));
                        pOutput.accept(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), ModPotions.FREEZING.get()));
                        pOutput.accept(PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), ModPotions.FREEZING.get()));


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
                        pOutput.accept(ModBlocks.VAULT_WALL.get());
                        pOutput.accept(ModBlocks.VAULT_GATE.get());
                        pOutput.accept(ModBlocks.CHAIN_LINK_FENCE.get());
                        pOutput.accept(ModBlocks.RU_MENG_DOLL.get());
                        pOutput.accept(ModBlocks.ROTATING_WARNING_LIGHT.get());
                        pOutput.accept(ModBlocks.SOLAR_PANEL.get());
                        pOutput.accept(ModBlocks.RTG.get());
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
