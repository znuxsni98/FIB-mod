package com.fib.fib.datagen;

import com.fib.fib.init.block.ModBlocks;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class ModBlockLootTablesProvider extends BlockLootSubProvider {

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
        dropSelf(ModBlocks.RTG.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        // 核心修改：直接比对 RegistryObject 引用排除太阳能板，绝不在类加载期调用 .get() 导致过早触发注册表检查
        return ModBlocks.BLOCKS.getEntries().stream()
                .filter(regObj -> regObj != ModBlocks.SOLAR_PANEL)
                .map(RegistryObject::get)
                ::iterator;
    }
}