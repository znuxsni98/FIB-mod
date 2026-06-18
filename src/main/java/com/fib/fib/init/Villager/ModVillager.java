package com.fib.fib.init.Villager;

import com.fib.fib.FIBMod;
import com.fib.fib.util.FIBUtils;
import com.google.common.collect.ImmutableSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public class ModVillager {

    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, FIBMod.MOD_ID);
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS = DeferredRegister.create(ForgeRegistries.VILLAGER_PROFESSIONS, FIBMod.MOD_ID);


    public static final Map<String, RegEntry> VILLAGERS = new HashMap<>();
    public static final String TACZ = "tacz";
    public static final String RS = "rs";
    public static final String ENGINEER = "engineer";
    public static final String SCIENTIST = "scientists";
    public static final String BEGGAR = "beggar";
    public static final String DOCTOR = "doctor";
    public static final String FLESH_SCHOLAR = "flesh_scholar";
    public static final String TICKET_AGENT = "ticket_agent";

    // Key, 工作方块ID, 兜底工作方块
    private static final Map<String, ProfessionData> DEF_MAP = Map.of(
            TACZ,          new ProfessionData("tacz:gun_smith_table",                      Blocks.WHITE_WOOL),
            RS,            new ProfessionData("refinedstorage:controller",                 Blocks.ORANGE_WOOL),
            ENGINEER,      new ProfessionData("fib_mod:engineer_workbench",                Blocks.MAGENTA_WOOL),
            SCIENTIST,     new ProfessionData("fib_mod:scientists_experimental_platform",  Blocks.LIGHT_BLUE_WOOL),
            BEGGAR,        new ProfessionData("fib_mod:bowl_block",                        Blocks.YELLOW_WOOL),
            DOCTOR,        new ProfessionData("spore:surgery_table",                       Blocks.LIME_WOOL),
            FLESH_SCHOLAR, new ProfessionData("biomancy:primordial_cradle",                Blocks.PINK_WOOL),
            TICKET_AGENT,  new ProfessionData("lightmanscurrency:ticket_machine",          Blocks.GRAY_WOOL)
    );

    public static void register(IEventBus eventBus) {
        DEF_MAP.forEach((name, data) -> {
            RegistryObject<PoiType> poi = POI_TYPES.register(name + "_poi", () -> {
                Block block = FIBUtils.getBlockById(data.targetId).orElse(data.fallback);
                return new PoiType(ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates()), 1, 1);
            });

            RegistryObject<VillagerProfession> profession = VILLAGER_PROFESSIONS.register(name + "_merchantr", () ->
                    new VillagerProfession(name + "_merchantr",
                            h -> h.value() == poi.get(), h -> h.value() == poi.get(),
                            ImmutableSet.of(), ImmutableSet.of(), SoundEvents.VILLAGER_WORK_ARMORER)
            );

            VILLAGERS.put(name, new RegEntry(poi, profession));
        });

        POI_TYPES.register(eventBus);
        VILLAGER_PROFESSIONS.register(eventBus);
    }


    private record ProfessionData(String targetId, Block fallback) {}
    public record RegEntry(RegistryObject<PoiType> poi, RegistryObject<VillagerProfession> profession) {}
}