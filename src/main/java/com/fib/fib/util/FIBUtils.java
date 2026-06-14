package com.fib.fib.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public class FIBUtils {

    public static Optional<Block> getBlockById(String blockId) {
        if ("minecraft:air".equals(blockId)) return Optional.of(Blocks.AIR);
        ResourceLocation location = ResourceLocation.tryParse(blockId);
        if (location == null) return Optional.empty();
        Block value = ForgeRegistries.BLOCKS.getValue(location);
        if (value == null || value == Blocks.AIR) return Optional.empty();
        return Optional.of(value);
    }

    public static Optional<Item> getItemById(String itemId) {
        if ("minecraft:air".equals(itemId)) return Optional.of(Items.AIR);
        ResourceLocation location = ResourceLocation.tryParse(itemId);
        if (location == null) return Optional.empty();
        Item value = ForgeRegistries.ITEMS.getValue(location);
        if (value == null || value == Items.AIR) return Optional.empty();
        return Optional.of(value);
    }
}
