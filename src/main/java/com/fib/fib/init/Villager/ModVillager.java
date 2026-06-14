package com.fib.fib.init.Villager;

import com.fib.fib.FIBMod;
import com.fib.fib.init.block.ModBlocks;
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

//工程师
    public static final RegistryObject<PoiType> EW_POI = POI_TYPES.register("ew_poi",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.ENGINEER_WORKBENCH.get().getStateDefinition().getPossibleStates()),1,2));

    public static final RegistryObject<VillagerProfession> ENGINEER_WORKBENCH_MAKER = VILLAGER_PROFESSIONS.register("ew_merchantr",
            () -> new VillagerProfession("ew_merchantr",
                    p -> p.get() == EW_POI.get(), p -> p.get() == EW_POI.get(),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_ARMORER));

//科学家
    public static final RegistryObject<PoiType> SEP_POI = POI_TYPES.register("sep_poi",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.SCIENTISTS_EXPERIMENTAL_PLATFORM.get().getStateDefinition().getPossibleStates()),1,2));

    public static final RegistryObject<VillagerProfession> SCIENTISTS_EXPERIMENTAL_PLATFORM_MAKER = VILLAGER_PROFESSIONS.register("sep_merchantr",
            () -> new VillagerProfession("sep_merchantr",
                    p -> p.get() == SEP_POI.get(), p -> p.get() == SEP_POI.get(),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_ARMORER));

//乞丐
    public static final RegistryObject<PoiType> BB_POI = POI_TYPES.register("bb_poi",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.BOWL_BLOCK.get().getStateDefinition().getPossibleStates()),1,2));

    public static final RegistryObject<VillagerProfession> BEGGAR_MAKER = VILLAGER_PROFESSIONS.register("bb_merchantr",
            () -> new VillagerProfession("bb_merchantr",
                    p -> p.get() == BB_POI.get(), p -> p.get() == BB_POI.get(),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_ARMORER));

//医生
    public static final RegistryObject<PoiType> ST_POI = POI_TYPES.register("st_poi",
            () -> new PoiType(ImmutableSet.copyOf(getBlockById("spore:surgery_table").orElse(Blocks.BARRIER).getStateDefinition().getPossibleStates()),1,2));

    public static final RegistryObject<VillagerProfession> DOCTOR_MAKER = VILLAGER_PROFESSIONS.register("st_merchantr",
            () -> new VillagerProfession("st_merchantr",
                    p -> p.get() == ST_POI.get(), p -> p.get() == ST_POI.get(),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_ARMORER));

//血肉学者
    public static final RegistryObject<PoiType> PC_POI = POI_TYPES.register("pc_poi",
            () -> new PoiType(ImmutableSet.copyOf(getBlockById("biomancy:primordial_cradle").orElse(Blocks.BARRIER).getStateDefinition().getPossibleStates()),1,2));

    public static final RegistryObject<VillagerProfession> FLESHANDBLOODSCHOLAR_MAKER = VILLAGER_PROFESSIONS.register("pc_merchantr",
            () -> new VillagerProfession("pc_merchantr",
                    p -> p.get() == PC_POI.get(), p -> p.get() == PC_POI.get(),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_ARMORER));

//票务员
    public static final RegistryObject<PoiType> TM_POI = POI_TYPES.register("tm_poi",
            () -> new PoiType(ImmutableSet.copyOf(getBlockById("lightmanscurrency:ticket_machine").orElse(Blocks.BARRIER).getStateDefinition().getPossibleStates()),1,2));

    public static final RegistryObject<VillagerProfession> TICKETAGENT_MAKER = VILLAGER_PROFESSIONS.register("tm_merchantr",
            () -> new VillagerProfession("tm_merchantr",
                    p -> p.get() == TM_POI.get(), p -> p.get() == TM_POI.get(),
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
