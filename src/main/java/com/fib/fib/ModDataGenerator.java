package com.fib.fib;

import com.fib.fib.datagen.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * --------------------------------------------------------------------------
 * 🛠️ 自动数据生成器总入口 (ModDataGenerator)
 * --------------------------------------------------------------------------
 * 【初中生大白话讲解】：
 * 你是不是觉得手写一个个方块的 JSON 模型文件（比如 assets/fib_mod/models/block/xxx.json）很麻烦？
 * Minecraft Forge 提供了一个偷懒神器，叫“数据生成器（Data Generator）”。
 * 
 * 你只要在代码里写一句规则（比如：“把所有模组方块的掉落物设置成掉落它自己”），
 * 然后在 IDEA 右侧 Gradle 任务里双击运行 `runData`，
 * 这个类就会自动帮你把几百个 JSON 模型、掉落物战利品表全部批量生成出来！
 * 
 * ⚠️【注意】：平时正常玩游戏时这个类是不运行的，只有运行 `runData` 任务时才会触发。
 */
@Mod.EventBusSubscriber(modid = FIBMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModDataGenerator {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // 1. 服务端数据：掉落物战利品表 (Loot Tables)
        generator.addProvider(event.includeServer(), new LootTableProvider(packOutput, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(ModBlockLootTablesProvider::new, LootContextParamSets.BLOCK)
        )));

        // 2. 服务端数据：方块与物品标签 (Tags)
        BlockTagsProvider blockTagsProvider = generator.addProvider(event.includeServer(),
                new ModBlockTagsProvider(packOutput, lookupProvider, existingFileHelper));
        generator.addProvider(event.includeServer(),
                new ModItemTagsProvider(packOutput, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));

        // 3. 客户端数据：自动生成方块状态 JSON 与物品模型 JSON
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));
        generator.addProvider(event.includeClient(), new ModItemModelsProvider(packOutput, existingFileHelper));
    }
}
