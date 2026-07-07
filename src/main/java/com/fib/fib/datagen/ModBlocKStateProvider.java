package com.fib.fib.datagen;

import com.fib.fib.FIBMod;
import com.fib.fib.init.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocKStateProvider extends BlockStateProvider {
    public ModBlocKStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, FIBMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        //生成方块状态文件

        //BB模型
        simpleBlockWithoutBlockModel(ModBlocks.BOWL_BLOCK);
        simpleBlockWithoutBlockModel(ModBlocks.CRATE);
        simpleBlockWithoutBlockModel(ModBlocks.TRASH_CAN);
        simpleBlockWithoutBlockModel(ModBlocks.OIL_DRUM);

        //有朝向
        customHorizontalBlock(ModBlocks.CORPSE_1);
        customHorizontalBlock(ModBlocks.CORPSE_2);
        customHorizontalBlock(ModBlocks.ICE_MAKER);
        customHorizontalBlock(ModBlocks.RADIO_STATION);
        customHorizontalBlock(ModBlocks.SCIENTISTS_EXPERIMENTAL_PLATFORM);
        customHorizontalBlock(ModBlocks.ENGINEER_WORKBENCH);
    }


    private <T extends Block> void customHorizontalBlock(RegistryObject<T> block) {
        ResourceLocation model = modLoc("block/" + block.getId().getPath());
        horizontalBlock(block.get(), models().getExistingFile(model));
        simpleBlockItem(block.get(), models().getExistingFile(model));
    }

    private <T extends Block> void simpleBlockWithoutBlockModel(RegistryObject<T> block) {
        ResourceLocation model = modLoc("block/" + block.getId().getPath());
        simpleBlock(block.get(), models().getExistingFile(model));
        simpleBlockItem(block.get(), models().getExistingFile(model));
    }
}
