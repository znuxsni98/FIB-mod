package com.fib.fib.blockentity;

import com.fib.fib.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 如梦玩偶方块实体。
 *
 * 方块本身不保存逻辑，全部预设数据都存在这里：
 *   preset       预设 id
 *   skin         预设对应的皮肤贴图路径（资源包）
 *   name         预设的物品名称
 *   description  预设的物品描述
 *
 * 放置时，物品上的 BlockEntityTag 会被原版自动写入该方块实体
 * （BlockItem 放置流程会调用 BlockEntity.loadStatic）。
 */
public class RuMengDollBlockEntity extends BlockEntity {

    private String preset = "";
    private String skin = "";
    private String name = "";
    private String description = "";

    public RuMengDollBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.RU_MENG_DOLL_BE.get(), pPos, pBlockState);
    }

    public String getPreset() {
        return this.preset;
    }

    public String getSkin() {
        return this.skin;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setPreset(String preset) {
        this.preset = preset;
    }

    public void setSkin(String skin) {
        this.skin = skin;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("preset", this.preset);
        tag.putString("skin", this.skin);
        tag.putString("name", this.name);
        tag.putString("description", this.description);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.preset = tag.getString("preset");
        this.skin = tag.getString("skin");
        this.name = tag.getString("name");
        this.description = tag.getString("description");
    }
}
