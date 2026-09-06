package com.fib.fib.datagen;

import com.fib.fib.init.block.ModBlocks;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class ModBlockLootTablesProvider extends BlockLootSubProvider {

    private static final Set<Block> EXCLUDED_BLOCKS = Set.of(
            //无需检查
            ModBlocks.SOLAR_PANEL.get()
    );

    public ModBlockLootTablesProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        //方块掉落
        dropSelf(ModBlocks.ICE_MAKER.get());
        dropSelf(ModBlocks.RADIO_STATION.get());
        dropSelf(ModBlocks.SCIENTISTS_EXPERIMENTAL_PLATFORM.get());
        dropSelf(ModBlocks.BOWL_BLOCK.get());
        dropSelf(ModBlocks.ENGINEER_WORKBENCH.get());
        dropSelf(ModBlocks.CRATE.get());
        dropSelf(ModBlocks.OIL_DRUM.get());
        dropSelf(ModBlocks.TRASH_CAN.get());
        dropSelf(ModBlocks.CORPSE_1.get());
        dropSelf(ModBlocks.CORPSE_2.get());
        dropSelf(ModBlocks.CHAIN_LINK_FENCE.get());
        dropSelf(ModBlocks.RU_MENG_DOLL.get());
        dropSelf(ModBlocks.ROTATING_WARNING_LIGHT.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream()
                .map(RegistryObject::get)
                .filter(block -> !EXCLUDED_BLOCKS.contains(block)) // <--- 核心修改：过滤掉不需要的方块
                ::iterator;
    }
}