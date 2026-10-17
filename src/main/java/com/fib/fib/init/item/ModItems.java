package com.fib.fib.init.item;

import com.fib.fib.FIBMod;
import com.fib.fib.init.item.custom.IceBallItem;
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

    public static final RegistryObject<Item> ICE_BALL =
            ITEMS.register("ice_ball", () -> new IceBallItem(new Item.Properties()));

    public static final RegistryObject<Item> GRAPPLING_HOOK_GUN =
            ITEMS.register("grappling_hook_gun", () -> new com.fib.fib.init.item.custom.GrapplingHookGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BASIC_HOOK =
            ITEMS.register("basic_hook", () -> new com.fib.fib.init.item.custom.hook.HookMaterialItem(
                    new Item.Properties().stacksTo(1), "fib_mod.hook_upgrade.basic_hook.desc", 4.0f, 0.0f, true, false, false));

    public static final RegistryObject<Item> PRISMARINE_HOOK =
            ITEMS.register("prismarine_hook", () -> new com.fib.fib.init.item.custom.hook.HookMaterialItem(
                    new Item.Properties().stacksTo(1), "fib_mod.hook_upgrade.prismarine_hook.desc", 6.0f, 4.0f, true, false, true));

    public static final RegistryObject<Item> WOOL_HOOK =
            ITEMS.register("wool_hook", () -> new com.fib.fib.init.item.custom.hook.HookMaterialItem(
                    new Item.Properties().stacksTo(1), "fib_mod.hook_upgrade.wool_hook.desc", 0.0f, 0.0f, false, false, false));

    public static final RegistryObject<Item> NETHERITE_HOOK =
            ITEMS.register("netherite_hook", () -> new com.fib.fib.init.item.custom.hook.HookMaterialItem(
                    new Item.Properties().stacksTo(1).fireResistant(), "fib_mod.hook_upgrade.netherite_hook.desc", 8.0f, 0.0f, true, true, false));

    public static final RegistryObject<Item> PRESSURIZED_MODULE =
            ITEMS.register("pressurized_module", () -> new com.fib.fib.init.item.custom.hook.PressurizedModuleItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SMART_UPGRADE_MODULE =
            ITEMS.register("smart_upgrade_module", () -> new com.fib.fib.init.item.custom.hook.SmartUpgradeModuleItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> DUAL_HOOK_MODULE =
            ITEMS.register("dual_hook_module", () -> new com.fib.fib.init.item.custom.hook.DualHookModuleItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ITEM_PROTECTION_MODULE =
            ITEMS.register("item_protection_module", () -> new com.fib.fib.init.item.custom.hook.ItemProtectionModuleItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MINING_MODULE =
            ITEMS.register("mining_module", () -> new com.fib.fib.init.item.custom.hook.MiningModuleItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
