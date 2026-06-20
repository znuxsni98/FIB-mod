package com.fib.fib.init;

import com.fib.fib.FIBMod;
import com.fib.fib.blockentity.TrashCanBlockEntity;
import com.fib.fib.init.block.ModBlocks;
import com.fib.fib.blockentity.CrateBlockEntity;
import com.fib.fib.blockentity.IceMakerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, FIBMod.MOD_ID);



    public static final RegistryObject<BlockEntityType<IceMakerBlockEntity>>
            ICE_MAKER_BE =
            BLOCK_ENTITIES.register("ice_maker_be", () ->

                    BlockEntityType.Builder.of(
                            IceMakerBlockEntity::new,
                            ModBlocks.ICE_MAKER.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<CrateBlockEntity>>
            CRATE_BE =
            BLOCK_ENTITIES.register("crate_be", () ->

                    BlockEntityType.Builder.of(
                            CrateBlockEntity::new,
                            ModBlocks.CRATE.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<TrashCanBlockEntity>>
            TRASH_CAN_BE =
            BLOCK_ENTITIES.register("trash_can_be", () ->

                    BlockEntityType.Builder.of(
                            TrashCanBlockEntity::new,
                            ModBlocks.TRASH_CAN.get()
                    ).build(null)
            );



    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}