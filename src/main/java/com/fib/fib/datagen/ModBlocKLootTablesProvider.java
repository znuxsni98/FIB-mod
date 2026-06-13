package com.fib.fib.datagen;

import com.fib.fib.init.block.ModBlocks;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class ModBlocKLootTablesProvider extends BlockLootSubProvider {
    public ModBlocKLootTablesProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
      //方块掉落
      dropSelf(ModBlocks.ICE_MAKER.get());
      dropSelf(ModBlocks.RADIO_STATION.get());

    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(RegistryObject::get)::iterator;
    }
}
