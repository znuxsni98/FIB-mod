package com.fib.fib.init.block;

import com.fib.fib.FIBMod;
import com.fib.fib.init.block.custom.*;
import com.fib.fib.init.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, FIBMod.MOD_ID);
    //方块注册


      public static final RegistryObject<Block> OIL_DRUM =
            registryBlock("oil_drum", () -> new Oil_Drum(BlockBehaviour.Properties.of()));

      public static final RegistryObject<Block> TRASH_CAN =
            registryBlock("trash_can", () -> new Trash_Can(BlockBehaviour.Properties.of()));

      public static final RegistryObject<Block> CRATE =
            registryBlock("crate", () -> new Crate(BlockBehaviour.Properties.of()));

      public static final RegistryObject<Block> ICE_MAKER =
              registryBlock("ice_maker", () -> new Ice_Maker(BlockBehaviour.Properties.of()));

      public static final RegistryObject<Block> RADIO_STATION =
              registryBlock("radio_station", () ->new Radio_Station(BlockBehaviour.Properties.of().noOcclusion()));
      
      public static final RegistryObject<Block> BOWL_BLOCK =
            registryBlock("bowl_block", () ->new Bowl_Block(BlockBehaviour.Properties.of()));

      public static final RegistryObject<Block> ENGINEER_WORKBENCH =
            registryBlock("engineer_workbench", () ->new Engineer_Workbench(BlockBehaviour.Properties.of().noOcclusion()));

      public static final RegistryObject<Block> SCIENTISTS_EXPERIMENTAL_PLATFORM =
            registryBlock("scientists_experimental_platform", () ->new Scientists_Experimental_Platform(BlockBehaviour.Properties.of().noOcclusion()));



    private static <T extends Block> void registerBlockItems(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static <T extends Block> RegistryObject<T> registryBlock(String name, Supplier<T> block) {
        RegistryObject<T> blocks = BLOCKS.register(name, block);
        registerBlockItems(name, blocks);
        return blocks;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
