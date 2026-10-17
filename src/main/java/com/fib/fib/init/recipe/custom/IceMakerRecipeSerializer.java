package com.fib.fib.init.recipe.custom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * 制冰机配方的解析器。JSON 格式：
 *
 * <pre>{@code
 * {
 *   "type": "fib_mod:ice_maker",
 *   "fluid_input": { "fluid": "minecraft:water", "amount": 500 },
 *   "item_input":  { "item": "minecraft:snowball" },
 *   "consume_item": true,
 *   "output":      { "item": "minecraft:ice", "count": 2 },
 *   "processing_time": 200,
 *   "energy_per_tick": 5,
 *   "recycle": true,
 *   "recycle_delay": 100
 * }
 * }</pre>
 *
 * <p><b>耗电只有 {@code energy_per_tick} 一个键，单位是每 tick 的 FE</b> ——
 * 200 tick、5 FE/t 的配方总共吃掉 1000 FE，但 JSON 里只写 {@code 5}。
 * 整段总耗电不写、也不做任何换算，想调贵调便宜就直接改这一个数。
 * 早期的 {@code energy_cost}（整段总耗电）键<b>已废弃</b>，写了会直接报错，详见
 * {@link #fromJson} 里的说明。
 *
 * <p><b>物品输入</b>里 {@code item} 写 {@code minecraft:air} 等同于不写这一整段 ——
 * 只想用流体的配方常用它占个位。可选字段 {@code nbt} 支持两种写法：SNBT 字符串
 * {@code "nbt": "{Mold:\"xxx:lines\"}"}（原版风格）或 JSON 对象 {@code "nbt": {"Mold": "xxx:lines"}}。
 * 带 {@code nbt} 的输入要求槽里的物品 NBT <b>完全一致</b>才算匹配。
 *
 * <p><b>回流</b>（{@code recycle} / {@code recycle_delay}，都可省略）是给链式配方用的：
 * 机器空闲且输入槽空够 {@code recycle_delay} tick 之后，会把输出槽里的东西搬回输入槽，
 * 好让"A 的产物正好是 B 的原料"能自己接下去。{@code recycle} 不写时改查物品标签
 * {@code fib_mod:ice_maker_recyclable}；写了就以配方为准，标签不再参与判断。
 *
 * <p><b>除 {@code output} 外全部可省略</b>，各有默认值；{@code fluid_input} 与 {@code item_input}
 * 至少要有一个，否则配方永远匹配不上任何东西。
 *
 * <p>关于报错：{@code RecipeManager} 加载数据包时只捕获
 * {@code IllegalArgumentException | JsonParseException}，其它异常会直接把加载打崩。
 * 而 {@code new ResourceLocation(...)} 抛的 {@code ResourceLocationException} 继承的是
 * {@code RuntimeException}，<b>不在捕获范围内</b> —— 所以这里所有 ID 解析都必须自己
 * 包一层转成 {@link JsonSyntaxException}，否则打错一个字符就不是"日志里一条报错"，
 * 而是整个存档进不去。
 */
public class IceMakerRecipeSerializer implements RecipeSerializer<IceMakerRecipe> {

    private static final int DEFAULT_PROCESSING_TIME = 200;
    private static final int DEFAULT_FLUID_AMOUNT = 1000;

    @Override
    public IceMakerRecipe fromJson(ResourceLocation id, JsonObject json) {
        // ---- 流体输入（可省略）----
        FluidStack fluidInput = FluidStack.EMPTY;
        if (GsonHelper.isValidNode(json, "fluid_input")) {
            JsonObject obj = GsonHelper.getAsJsonObject(json, "fluid_input");
            ResourceLocation fluidId = parseId(GsonHelper.getAsString(obj, "fluid"), "fluid_input.fluid");

            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null || fluid == Fluids.EMPTY) {
                throw new JsonSyntaxException("Unknown fluid '" + fluidId + "'");
            }

            int amount = GsonHelper.getAsInt(obj, "amount", DEFAULT_FLUID_AMOUNT);
            if (amount <= 0) {
                throw new JsonSyntaxException("fluid_input.amount must be > 0, got " + amount);
            }
            fluidInput = new FluidStack(fluid, amount);
        }

        // ---- 物品输入（可省略）----
        // 写 "minecraft:air" 等同于整段不写 —— 只想用流体的配方常这么占位，
        // 当错误拒掉的话整个文件会被 RecipeManager 跳过，得不偿失。
        ItemStack itemInput = ItemStack.EMPTY;
        if (GsonHelper.isValidNode(json, "item_input")) {
            itemInput = parseStack(GsonHelper.getAsJsonObject(json, "item_input"), "item_input", true);
        }

        // 带物品输入时默认消耗，纯催化剂才需要显式写 false
        boolean consumeItem = GsonHelper.getAsBoolean(json, "consume_item", true);

        // ---- 产物（必填）----
        JsonObject out = GsonHelper.getAsJsonObject(json, "output");
        ItemStack output = parseStack(out, "output", false);
        int count = GsonHelper.getAsInt(out, "count", 1);
        output.setCount(Math.max(1, Math.min(count, output.getItem().getMaxStackSize())));

        // ---- 耗时 / 耗电 ----
        // 下限 1：界面上要拿它当进度条分母，0 会除零
        int processingTime = Math.max(1, GsonHelper.getAsInt(json, "processing_time", DEFAULT_PROCESSING_TIME));

        // energy_per_tick 就是每 tick 的 FE，不做任何换算 ——
        // 整段总耗电 = energy_per_tick * processing_time，那是给 JEI 看的结果，不是要填的数。
        int energyPerTick = Math.max(0, GsonHelper.getAsInt(json, "energy_per_tick", 0));

        // 旧的 energy_cost 键已废弃，这里【报错】而不是忽略。两种语义差一个 processing_time
        // 的倍数：老写法的 6400 若被当成每 tick，就是 6400 FE/t，远超机器 1000 FE/t 的输入上限，
        // 配方会"看着像完全不工作"、且没有任何报错可查；静默按 0 处理又会让机器白跑。
        // 还不如让 RecipeManager 把文件名和这句话一起打进日志。
        if (json.has("energy_cost")) {
            throw new JsonSyntaxException("'energy_cost' is no longer supported; use 'energy_per_tick' "
                    + "(FE per tick) — divide the old total by processing_time, e.g. 6400 / 100 = 64");
        }

        // ---- 回流（可省略）----
        // 不写 recycle 时留 null，机器那边会退回到物品标签 fib_mod:ice_maker_recyclable。
        // 写了就完全按配方走：true 无条件回流，false 无条件不回流，标签一律不查。
        Boolean recycle = json.has("recycle") ? GsonHelper.getAsBoolean(json, "recycle") : null;
        int recycleDelay = Math.max(1, GsonHelper.getAsInt(
                json, "recycle_delay", IceMakerRecipe.DEFAULT_RECYCLE_DELAY));

        if (fluidInput.isEmpty() && itemInput.isEmpty()) {
            throw new JsonSyntaxException("Ice maker recipe needs at least one of fluid_input / item_input");
        }

        return new IceMakerRecipe(id, fluidInput, itemInput, consumeItem, output, processingTime,
                energyPerTick, recycle, recycleDelay);
    }

    /**
     * 从网络读回。字段必须与 {@link #toNetwork} 和 {@link #fromJson} 一一对应 ——
     * 服务端加载完配方后会通过 {@code ClientboundUpdateRecipesPacket} 同步给客户端，
     * 少读一个字段客户端拿到的就是错位的配方（JEI 里会显示空白或错乱）。
     */
    @Nullable
    @Override
    public IceMakerRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        FluidStack fluidInput = FluidStack.readFromPacket(buf);
        ItemStack itemInput = buf.readItem();
        boolean consumeItem = buf.readBoolean();
        ItemStack output = buf.readItem();
        int processingTime = buf.readVarInt();
        int energyPerTick = buf.readVarInt();
        // 先读一个"有没有写"的标记，再读值 —— Boolean 的 null 传不过去，
        // 少了这个标记客户端会把"没写"读成 false，回流就整条失效了。
        Boolean recycle = buf.readBoolean() ? buf.readBoolean() : null;
        int recycleDelay = buf.readVarInt();
        return new IceMakerRecipe(id, fluidInput, itemInput, consumeItem, output, processingTime,
                energyPerTick, recycle, recycleDelay);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, IceMakerRecipe recipe) {
        recipe.getFluidInput().writeToPacket(buf);
        buf.writeItem(recipe.getItemInput());
        buf.writeBoolean(recipe.shouldConsumeItem());
        buf.writeItem(recipe.getOutput());
        buf.writeVarInt(recipe.getProcessingTime());
        buf.writeVarInt(recipe.getEnergyPerTick());
        Boolean recycle = recipe.getRecycle();
        buf.writeBoolean(recycle != null);
        if (recycle != null) {
            buf.writeBoolean(recycle);
        }
        buf.writeVarInt(recipe.getRecycleDelay());
    }

    /** 读一个注册表 ID 字段，顺手把 {@code ResourceLocationException} 转成可被捕获的语法错误。 */
    private static ResourceLocation parseId(String raw, String where) {
        try {
            return new ResourceLocation(raw);
        } catch (RuntimeException e) {
            throw new JsonSyntaxException("Invalid resource location '" + raw + "' in " + where, e);
        }
    }

    /**
     * 读一个 {@code {"item": "...", "nbt": ...}} 形式的物品。count 不在这里处理，
     * 由调用方按各自语义套用。
     *
     * @param allowAir 允许写 {@code minecraft:air} 表示"没有这个输入"（返回空栈）。
     *                 输入槽可以，产物不行 —— 产物是空气的配方没有意义。
     */
    private static ItemStack parseStack(JsonObject obj, String where, boolean allowAir) {
        ResourceLocation itemId = parseId(GsonHelper.getAsString(obj, "item"), where + ".item");
        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item == null) {
            throw new JsonSyntaxException("Unknown item '" + itemId + "' in " + where);
        }
        if (item == Items.AIR) {
            if (!allowAir) {
                throw new JsonSyntaxException("'" + itemId + "' is not a valid " + where + ".item");
            }
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);
        CompoundTag nbt = parseNbt(obj, where);
        if (nbt != null) {
            stack.setTag(nbt);
        }
        return stack;
    }

    /**
     * 读可选的 {@code nbt} 字段。两种写法都认：
     * <ul>
     *   <li>SNBT 字符串 —— 原版物品 JSON 的写法：{@code "nbt": "{Mold:\"xxx:lines\"}"}</li>
     *   <li>JSON 对象 —— 直接照抄 NBT 结构：{@code "nbt": {"Mold": "xxx:lines"}}</li>
     * </ul>
     *
     * <p>没有这个字段时返回 {@code null}（而不是空 {@link CompoundTag}）—— 二者对
     * {@code ItemStack} 不等价，空 compound 会让 {@code isSameItemSameTags} 匹配不上
     * 真正的无 NBT 物品。
     */
    @Nullable
    private static CompoundTag parseNbt(JsonObject obj, String where) {
        if (!obj.has("nbt")) {
            return null;
        }

        JsonElement element = obj.get("nbt");
        Tag tag;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            try {
                tag = TagParser.parseTag(element.getAsString());
            } catch (CommandSyntaxException e) {
                throw new JsonSyntaxException("Malformed SNBT in " + where + ".nbt", e);
            }
        } else {
            tag = JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE, element);
        }

        if (!(tag instanceof CompoundTag compound)) {
            throw new JsonSyntaxException(where + ".nbt must be a compound tag");
        }
        // 空 compound 和"没有 NBT"在 ItemStack 里是两种状态，统一按没有处理。
        return compound.isEmpty() ? null : compound;
    }
}
