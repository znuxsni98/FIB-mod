package com.fib.fib.init.item;

import com.fib.fib.FIBMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FIBMod.MOD_ID);

    public static final RegistryObject<Item> FIB_LOGO_MOD =
            ITEMS.register("fib_logo_mod", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> WILD_DOG_MILK =
            ITEMS.register("wild_dog_milk", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SPRING_AND_AUTUMN_INTESTINES =
            ITEMS.register("spring_and_autumn_intestines", () -> new Item(new Item.Properties().food(ModFoods.SPRING_AND_AUTUMN_INTESTINES)));

    public static final RegistryObject<Item> CANNED_MUSHROOMS =
            ITEMS.register("canned_mushrooms", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> SOLAR_PANEL_CONTROLLER =
            ITEMS.register("solar_panel_controller", () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
