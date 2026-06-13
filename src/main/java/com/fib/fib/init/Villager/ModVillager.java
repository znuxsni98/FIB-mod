package com.fib.fib.init.Villager;

import com.fib.fib.FIBMod;
import com.google.common.collect.ImmutableSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;

public class ModVillager {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(ForgeRegistries.POI_TYPES, FIBMod.MOD_ID);

    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
            DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, FIBMod.MOD_ID);

//注册村民

//军火商
    public static final RegistryObject<PoiType> TACZ_POI = POI_TYPES.register("tacz_poi",
            () -> new PoiType(ImmutableSet.copyOf(getBlockById("tacz:gun_smith_table").orElse(Blocks.BARRIER).getStateDefinition().getPossibleStates()),1,2));

    public static final RegistryObject<VillagerProfession> TACZ_MAKER = VILLAGER_PROFESSIONS.register("tacz_merchantr",
            () -> new VillagerProfession("tacz_merchantr",
                    p -> p.get() == TACZ_POI.get(), p -> p.get() == TACZ_POI.get(),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_ARMORER));

//RS
    public static final RegistryObject<PoiType> RS_POI = POI_TYPES.register("rs_poi",
        () -> new PoiType(ImmutableSet.copyOf(getBlockById("refinedstorage:controller").orElse(Blocks.BARRIER).getStateDefinition().getPossibleStates()),1,2));

    public static final RegistryObject<VillagerProfession> RS_MAKER = VILLAGER_PROFESSIONS.register("rs_merchantr",
            () -> new VillagerProfession("rs_merchantr",
                    p -> p.get() == RS_POI.get(), p -> p.get() == RS_POI.get(),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_ARMORER));





    public static void register(IEventBus eventBus) {
        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register((eventBus));
    }

    public static Optional<Block> getBlockById(String blockId) {
        ResourceLocation location = ResourceLocation.tryParse(blockId);
        if (location == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(location));
    }
}
