package com.fib.fib.init;

import com.fib.fib.FIBMod;
//import com.fib.fib.blockentity.OilDrumBlockEntity;
import com.fib.fib.blockentity.*;
import com.fib.fib.blockentity.corpse.Corpse1BlockEntity;
import com.fib.fib.blockentity.corpse.Corpse2BlockEntity;
import com.fib.fib.init.block.ModBlocks;
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

    public static final RegistryObject<BlockEntityType<Corpse1BlockEntity>>
            CORPSE1_BE =
            BLOCK_ENTITIES.register("corpse1_be", () ->

                    BlockEntityType.Builder.of(
                            Corpse1BlockEntity::new,
                            ModBlocks.CORPSE_1.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<Corpse2BlockEntity>>
            CORPSE2_BE =
            BLOCK_ENTITIES.register("corpse2_be", () ->

                    BlockEntityType.Builder.of(
                            Corpse2BlockEntity::new,
                            ModBlocks.CORPSE_2.get()
                    ).build(null)
            );


    public static final RegistryObject<BlockEntityType<RuMengDollBlockEntity>>
            RU_MENG_DOLL_BE =
            BLOCK_ENTITIES.register("ru_meng_doll_be", () ->

                    BlockEntityType.Builder.of(
                            RuMengDollBlockEntity::new,
                            ModBlocks.RU_MENG_DOLL.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<SolarPanelBlockEntity>>
            SOLAR_PANEL_BE =
            BLOCK_ENTITIES.register("solar_panel_be", () ->

                    BlockEntityType.Builder.of(
                            SolarPanelBlockEntity::new,
                            ModBlocks.SOLAR_PANEL.get()
                    ).build(null)
            );

//    public static final RegistryObject<BlockEntityType<OilDrumBlockEntity>>
//            OIL_DRUM_BE =
//            BLOCK_ENTITIES.register("oil_drum_be", () ->
//
//                    BlockEntityType.Builder.of(
//                            OilDrumBlockEntity::new,
//                            ModBlocks.OIL_DRUM.get()
//                    ).build(null)
//            );


    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}