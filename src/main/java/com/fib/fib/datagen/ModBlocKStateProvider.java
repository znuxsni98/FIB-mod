package com.fib.fib.datagen;

import com.fib.fib.FIBMod;
import com.fib.fib.init.block.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder;
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
        simpleBlockWithoutBlockModel(ModBlocks.VAULT_WALL);
        simpleBlockWithoutBlockModel(ModBlocks.VAULT_GATE);
        simpleBlockWithoutBlockModel(ModBlocks.RU_MENG_DOLL);

        //有朝向
        customHorizontalBlock(ModBlocks.CORPSE_1);
        customHorizontalBlock(ModBlocks.CORPSE_2);
        customHorizontalBlock(ModBlocks.ICE_MAKER);
        customHorizontalBlock(ModBlocks.RADIO_STATION);
        customHorizontalBlock(ModBlocks.SCIENTISTS_EXPERIMENTAL_PLATFORM);
        customHorizontalBlock(ModBlocks.ENGINEER_WORKBENCH);

        //围栏
        customFence(ModBlocks.CHAIN_LINK_FENCE, "chain_link_fence");
    }


    private <T extends Block> void customFence(RegistryObject<T> block, String name) {
        ModelFile modelPost = models().getExistingFile(modLoc("block/" + name + "_post"));
        ModelFile modelSide = models().getExistingFile(modLoc("block/" + name +  "_side"));

        MultiPartBlockStateBuilder builder = getMultipartBuilder(block.get())
                .part().modelFile(modelPost).addModel().end();
        PipeBlock.PROPERTY_BY_DIRECTION.entrySet().forEach(e -> {
            Direction direction = e.getKey();
            if (direction.getAxis().isHorizontal()) {
                builder.part().modelFile(modelSide).rotationY(((int) direction.toYRot() + 180) % 360).addModel()
                        .condition(e.getValue(), true);
            }
        });

        simpleBlockItem(block.get(), models().getExistingFile(modLoc("block/" + name)));
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
