package com.fib.fib.init.recipe.custom;

import com.fib.fib.init.recipe.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 制冰机配方：流体 +（可选）物品 + FE → 物品。
 *
 * <p>匹配逻辑不在 {@link #matches} 里 —— 那个方法的参数是原版 {@link Container}，
 * 装不下流体罐和能量缓冲。机器每 tick 直接调 {@link #matchesInputs} 自己判，
 * {@code matches} 留个返回 false 的实现只是因为接口要求。
 */
public class IceMakerRecipe implements Recipe<Container> {

    /** 回流延迟的默认值：输入槽空满 5 秒就把产物搬回去。 */
    public static final int DEFAULT_RECYCLE_DELAY = 100;

    private final ResourceLocation id;
    private final FluidStack fluidInput;
    private final ItemStack itemInput;
    private final boolean consumeItem;
    private final ItemStack output;
    private final int processingTime;
    private final int energyPerTick;

    /**
     * 回流开关。<b>三段值</b>：
     * <ul>
     *   <li>{@code null} —— 配方没写 {@code recycle}，交给物品标签
     *       {@code fib_mod:ice_maker_recyclable} 决定</li>
     *   <li>{@code TRUE} / {@code FALSE} —— 配方写死了，优先级高于标签</li>
     * </ul>
     * 用包装类型而不是 {@code boolean}，就是为了区分"没写"和"写了 false"。
     */
    @Nullable
    private final Boolean recycle;

    /** 输入槽空多少 tick 之后才回流。 */
    private final int recycleDelay;

    public IceMakerRecipe(ResourceLocation id, FluidStack fluidInput, ItemStack itemInput,
                          boolean consumeItem, ItemStack output, int processingTime, int energyPerTick) {
        this(id, fluidInput, itemInput, consumeItem, output, processingTime, energyPerTick,
                null, DEFAULT_RECYCLE_DELAY);
    }

    public IceMakerRecipe(ResourceLocation id, FluidStack fluidInput, ItemStack itemInput,
                          boolean consumeItem, ItemStack output, int processingTime, int energyPerTick,
                          @Nullable Boolean recycle, int recycleDelay) {
        this.id = id;
        this.fluidInput = fluidInput;
        this.itemInput = itemInput;
        this.consumeItem = consumeItem;
        this.output = output;
        this.processingTime = Math.max(1, processingTime);
        this.energyPerTick = Math.max(0, energyPerTick);
        this.recycle = recycle;
        // 下限 1：0 会让机器每 tick 都把产物往回搬
        this.recycleDelay = Math.max(1, recycleDelay);
    }

    // === 核心匹配逻辑 ===
    @Override
    public boolean matches(Container container, Level level) {
        // 注意：对于自定义机器，通常不在这里做完整匹配
        // 而是在 TileEntity 中手动检查流体+物品+能量
        // 此方法主要用于工作台等原版容器，自定义机器可返回 false
        return false;
    }

    /**
     * 机器用的匹配判定：罐里的流体、输入槽的物品、输出槽放不放得下，三者都满足才算命中。
     *
     * @param tank        机器内部流体罐的当前内容（不会为空，空时传 {@link FluidStack#EMPTY}）
     * @param inputSlot   物品输入槽的内容
     * @param canAccept   把 {@link #getOutput()} 塞进输出槽的<b>模拟</b>结果
     */
    public boolean matchesInputs(FluidStack tank, ItemStack inputSlot, boolean canAccept) {
        return canAccept && inputsMatch(tank, inputSlot);
    }

    /**
     * 只看输入成不成立，<b>不管输出槽放不放得下</b>。
     *
     * <p>分成两个方法的用途不一样：
     * <ul>
     *   <li>选配方时用 {@link #matchesInputs}（输出放不下就没必要开工）；</li>
     *   <li>开工之后每 tick 用这个复查。<b>输出槽满 = 暂停，输入不成立 = 中止</b>，
     *       两者要分开处理，所以这里必须把输出槽那一条剔出去。</li>
     * </ul>
     *
     * <p><b>关于"输入槽必须正好是这条配方要的东西"</b>：如果配方不需要物品，
     * 槽里却出现了东西，这里判为不成立。这条不是洁癖 —— 没有它，一条"只吃水"的配方
     * 在水还没用完时会一直压着"水 + 模具"的配方，玩家把模具放进去机器照旧造冰，
     * 相当于输入槽被无视了。
     */
    public boolean inputsMatch(FluidStack tank, ItemStack inputSlot) {
        if (!fluidInput.isEmpty()) {
            // 必须是同一种流体且量够。FluidStack#isFluidEqual 比的是流体本身（不含 NBT 之外的量），
            // 但 NBT 不同的同种流体（比如带标签的药剂）会被判为不等，这里按机器常规语义只比流体种类。
            if (tank.isEmpty() || !tank.getFluid().isSame(fluidInput.getFluid())) {
                return false;
            }
            if (tank.getAmount() < fluidInput.getAmount()) {
                return false;
            }
        }
        if (!itemInput.isEmpty()) {
            if (inputSlot.isEmpty() || !inputSlot.is(itemInput.getItem())) {
                return false;
            }
            if (!tagMatches(inputSlot.getTag(), itemInput.getTag())) {
                return false;
            }
            if (inputSlot.getCount() < itemInput.getCount()) {
                return false;
            }
        } else if (!inputSlot.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * 槽里物品的 NBT 是否<b>包含</b>配方里写的那几项。
     *
     * <p>这里刻意不用 {@link ItemStack#isSameItemSameTags} —— 那是严格相等：只要槽里的物品
     * 多带一个无关的 NBT 键（耐久、缓存的显示名、模组自己加的标签），整条配方就永远匹配不上，
     * 表现出来就是"配方不工作"，而且没有任何报错可查。
     *
     * <p>改成子集判定：配方写了哪些键，槽里就得有哪些键、值相等；配方没写的键不参与比较。
     * 顶层逐键比较，嵌套 compound 按整体相等处理。配方不写 {@code nbt} 时视为不限 NBT。
     *
     * @param actual   槽里物品的 NBT，可能为 null
     * @param required 配方要求的 NBT，可能为 null（不限）
     */
    private static boolean tagMatches(@Nullable CompoundTag actual, @Nullable CompoundTag required) {
        if (required == null || required.isEmpty()) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        for (String key : required.getAllKeys()) {
            if (!Objects.equals(actual.get(key), required.get(key))) {
                return false;
            }
        }
        return true;
    }

    // === Getter 方法（供 TileEntity 调用）===
    // 注意全部返回拷贝：FluidStack / ItemStack 都是可变对象，
    // 直接漏出去的话调用方一次 setAmount / shrink 就会改到配方本身的共享实例上 ——
    // 而配方是数据包加载出来的单例，改一次会污染所有同款机器。
    public FluidStack getFluidInput() { return fluidInput.copy(); }
    public ItemStack getItemInput() { return itemInput.copy(); }
    public boolean shouldConsumeItem() { return consumeItem; }
    public ItemStack getOutput() { return output.copy(); } // 必须 copy！
    public int getProcessingTime() { return processingTime; }

    /**
     * 每 tick 消耗的 FE。<b>不是整段流程的总耗电</b> ——
     * 总耗电 = {@code energyPerTick * processingTime}。
     */
    public int getEnergyPerTick() { return energyPerTick; }

    /**
     * 回流开关，见字段注释：{@code null} = 配方没表态，按物品标签来。
     * 机器那边把这个判断和标签合并成一句 {@code override != null ? override : output.is(tag)}。
     */
    @Nullable
    public Boolean getRecycle() { return recycle; }

    public int getRecycleDelay() { return recycleDelay; }

    // === 必须实现的接口方法 ===
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.ICE_MAKER_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.ICE_MAKER_TYPE.get(); }
    @Override public ItemStack assemble(Container c, RegistryAccess r) { return output.copy(); }
    @Override public boolean canCraftInDimensions(int w, int h) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess r) { return output.copy(); }

    /** 让配方书/JEI 能拿到"这个东西需要什么材料"，否则会被判成 isIncomplete。 */
    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        if (!itemInput.isEmpty()) {
            list.add(Ingredient.of(itemInput));
        }
        return list;
    }

    /** 不进配方书 —— 它不是 3x3 能摆出来的东西。 */
    @Override
    public boolean isSpecial() {
        return true;
    }
}
