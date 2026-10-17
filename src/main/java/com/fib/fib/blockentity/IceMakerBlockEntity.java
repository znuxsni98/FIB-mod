package com.fib.fib.blockentity;

import com.fib.fib.FIBMod;
import com.fib.fib.gui.container.menu.IceMakerMenu;
import com.fib.fib.init.ModBlockEntities;
import com.fib.fib.init.recipe.ModRecipes;
import com.fib.fib.init.recipe.custom.IceMakerRecipe;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 制冰机方块实体：流体 + 物品 + FE → 物品。
 *
 * <p>槽位分工（与 GUI 贴图一致）：
 * <pre>
 *   槽 0  物品输入      槽 2  物品输出
 *   槽 1  流体容器输入  槽 3  空容器输出
 * </pre>
 * 槽 1 → 槽 3 是<b>单向</b>的：满桶从上面进去，倒空后空桶从下面出来。槽 3 不会反向从罐里
 * 灌满容器 —— 否则刚倒空的桶会被立刻灌回去，看起来像"什么都没发生"。
 * 想把流体取出来用潜行右键（见 {@code Ice_Maker#use}）或接管道。
 */
public class IceMakerBlockEntity extends BlockEntity implements MenuProvider, IHaveGoggleInformation {

    // ==================== 槽位 ====================
    public static final int INPUT_SLOT = 0;
    public static final int INPUT_SLOT_FLUID = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int OUTPUT_SLOT_FLUID = 3;
    public static final int SLOT_COUNT = 4;

    // ==================== 容量 ====================
    public static final int TANK_CAPACITY = 8000;
    public static final int ENERGY_CAPACITY = 20000;
    public static final int MAX_RECEIVE = 1000;

    /** 客户端同步间隔（tick）。GUI 里的流体颜色、护目镜读的是方块实体 NBT，走这条通道。 */
    private static final int SYNC_INTERVAL = 10;

    /**
     * 默认的回流名单：在里面的物品，机器空闲时会把输出槽的它们搬回输入槽。
     * 对应数据包文件 {@code data/fib_mod/tags/items/ice_maker_recyclable.json}，
     * 整合包作者可以直接往里加物品，不用碰配方。
     *
     * <p>只有配方<b>没写</b> {@code recycle} 字段时才轮到它。
     */
    public static final TagKey<Item> RECYCLABLE_TAG =
            ItemTags.create(new ResourceLocation(FIBMod.MOD_ID, "ice_maker_recyclable"));

    // ==================== NBT 键 ====================
    private static final String NBT_INVENTORY = "inventory";
    private static final String NBT_TANK = "Tank";
    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_PROGRESS = "Progress";
    private static final String NBT_ACTIVE_RECIPE = "ActiveRecipe";
    private static final String NBT_LAST_CRAFT = "LastCraft";

    // ==================== ContainerData 下标 ====================
    public static final int DATA_COUNT = 6;
    private static final int DATA_PROGRESS = 0;
    private static final int DATA_MAX_PROGRESS = 1;
    private static final int DATA_ENERGY = 2;
    private static final int DATA_ENERGY_CAP = 3;
    private static final int DATA_FLUID = 4;
    private static final int DATA_FLUID_CAP = 5;

    // ==================== 状态 ====================
    private int progress = 0;
    private int syncCooldown = 0;

    /**
     * 进行中的配方。<b>存 id 而不是存对象引用</b>：{@code RecipeManager} 会在 {@code /reload}
     * 或数据包重载时整批换掉 Recipe 实例，抓着旧引用不放就会一直用一个已经不在管理器里的配方。
     */
    @Nullable
    private ResourceLocation activeRecipeId;

    /**
     * 产出输出槽里那批东西的配方。<b>回流要读它的 {@code recycle} / {@code recycle_delay}</b>，
     * 所以必须记住"是谁产出的"——只看物品本身是查不到那条配方的。
     *
     * <p>要落盘：关掉游戏再进来，回流还得能按同一条配方走。
     */
    @Nullable
    private ResourceLocation lastCraftId;

    /**
     * 输入槽已经空了多久（tick）。
     *
     * <p><b>故意不存盘</b>：它只是个倒计时，重新载入后从 0 重新数没有任何副作用，
     * 存盘反而会让这个字段每 tick 都变、把区块一直标成脏的。
     */
    private int idleTicks;

    // ==================== 物品 ====================
    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // 输入槽来者不拒（具体要什么由配方决定）；流体槽只收装了水的容器，
            // 空桶放进去没有意义，而且会让"槽 3 是纯输出"的语义变得含糊。
            if (slot == INPUT_SLOT) {
                return true;
            }
            if (slot == INPUT_SLOT_FLUID) {
                return isAcceptedFluid(FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY));
            }
            return false;
        }
    };

    // ==================== 流体 ====================

    /**
     * 制冰机认不认这种流体 —— <b>只认水</b>。
     *
     * <p>用 {@link FluidTags#WATER} 标签而不是直接比 {@code Fluids.WATER}，
     * 这样整合包里那些自己打了 water 标签的模组水也能用。
     *
     * <p>槽位和储罐都要过这一关：只卡槽位的话，管道和潜行右键照样能灌一罐岩浆进来，
     * 而岩浆在这台机器上没有任何用途，只会白占着罐子。
     */
    public static boolean isAcceptedFluid(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid().is(FluidTags.WATER);
    }

    private final FluidTank fluidTank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            // 管道 fill / 潜行右键倒桶都走这里（FluidTank.fill 第一件事就是问它）。
            return isAcceptedFluid(stack);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final LazyOptional<IFluidHandler> fluidCap = LazyOptional.of(() -> fluidTank);

    // ==================== 能量 ====================
    private final InternalEnergyStorage energyStorage = new InternalEnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE);
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energyStorage);

    // ==================== ContainerData ====================
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            IceMakerRecipe recipe = getActiveRecipe();
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX_PROGRESS -> recipe == null ? 0 : recipe.getProcessingTime();
                case DATA_ENERGY -> energyStorage.getEnergyStored();
                case DATA_ENERGY_CAP -> ENERGY_CAPACITY;
                case DATA_FLUID -> fluidTank.getFluidAmount();
                case DATA_FLUID_CAP -> TANK_CAPACITY;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 客户端同步回写。只有 progress 需要真的落到字段上给 GUI 用，
            // 其余几项客户端读的是方块实体自己同步过来的值。
            if (index == DATA_PROGRESS) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public IceMakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ICE_MAKER_BE.get(), pos, state);
    }

    // ==================================================================================
    //  主循环
    // ==================================================================================

    /**
     * 每游戏刻调用一次。
     *
     * <p>第一件事就是挡掉客户端：GUI 里的进度靠 {@link #data} 从服务端推过去，
     * 客户端自己也跑一遍的话会在服务端数值到达之前先把 progress 加乱。
     */
    public void tick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        boolean dirty = drainContainerIntoTank();

        IceMakerRecipe recipe = getActiveRecipe();

        // 每 tick 复查输入还成不成立。开工之后就不再回头看输入槽是不对的：
        // 造雪途中把模具拿走、造冰途中往槽里塞个模具，机器都会抱着旧配方一条道跑到黑，
        // 表现得像输入槽根本不起作用。这里一旦发现对不上就当场中止，进度归零，
        // 下面会立刻按当前槽里的东西重新选一条配方（造冰途中放模具 → 改去造雪）。
        if (recipe != null
                && !recipe.inputsMatch(fluidTank.getFluid(), itemHandler.getStackInSlot(INPUT_SLOT))) {
            abortCraft();
            recipe = null;
            dirty = true;
        }

        if (recipe == null) {
            recipe = findRecipe();
            if (recipe != null) {
                startCraft(recipe);
                dirty = true;
            }
        }
        if (recipe != null && advanceCraft(recipe)) {
            dirty = true;
        }

        // 放在合成之后：这一 tick 刚开始的活儿会先让 activeRecipeId 变成非 null，
        // 回流一看到有活儿在跑就直接跳过，不会把料从输出槽抽走。
        if (handleRecycling()) {
            dirty = true;
        }

        if (dirty) {
            setChanged();
        }

        // 定期把罐里的流体种类推给客户端 —— ContainerData 只能传 int，
        // 传不了流体 ID，而 GUI 上色和护目镜都要知道是哪种流体。
        if (++syncCooldown >= SYNC_INTERVAL) {
            syncCooldown = 0;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * 槽 1 的容器 → 内部储罐。倒空后的空容器落到槽 3。
     *
     * @return 本 tick 是否真的动了东西
     */
    private boolean drainContainerIntoTank() {
        ItemStack inSlot = itemHandler.getStackInSlot(INPUT_SLOT_FLUID);
        if (inSlot.isEmpty()) {
            return false;
        }

        ItemStack single = inSlot.copyWithCount(1);
        FluidStack contained = FluidUtil.getFluidContained(single).orElse(FluidStack.EMPTY);
        if (contained.isEmpty()) {
            return false;
        }

        // 必须能装下【整罐】才动手。
        // 桶这类容器只能整桶进出：一旦只转移一部分，Forge 的 IFluidHandlerItem#getContainer()
        // 会照样返回一个"整桶"物品，等于凭空多出一份流体。
        if (fluidTank.getCapacity() - fluidTank.getFluidAmount() < contained.getAmount()) {
            return false;
        }

        // 先模拟：确认倒得成，且空容器在槽 3 放得下。
        // 顺序很重要 —— 先真倒再检查槽位的话，槽 3 满了就会把流体白吞掉。
        FluidActionResult simulated = FluidUtil.tryEmptyContainer(single, fluidTank, contained.getAmount(), null, false);
        if (!simulated.isSuccess()) {
            return false;
        }

        ItemStack emptied = simulated.getResult();
        if (!canInsertIntoOutput(OUTPUT_SLOT_FLUID, emptied)) {
            return false;
        }

        // 模拟通过，真执行。结果容器与模拟一致（同一份输入的确定性结果）。
        FluidActionResult drained = FluidUtil.tryEmptyContainer(single, fluidTank, contained.getAmount(), null, true);
        if (!drained.isSuccess()) {
            return false;
        }

        itemHandler.extractItem(INPUT_SLOT_FLUID, 1, false);
        insertIntoOutput(OUTPUT_SLOT_FLUID, emptied);
        return true;
    }

    // ==================================================================================
    //  往输出槽放东西
    // ==================================================================================

    /**
     * 模拟：输出槽能不能完整吃下 {@code stack}。
     *
     * <p><b>必须走这里，不能走 {@link ItemStackHandler#insertItem}</b> —— 见
     * {@link #insertIntoOutput} 的长注释。
     */
    private boolean canInsertIntoOutput(int slot, ItemStack stack) {
        ItemStack existing = itemHandler.getStackInSlot(slot);
        if (existing.isEmpty()) {
            return stack.getCount() <= itemHandler.getSlotLimit(slot);
        }
        return ItemStack.isSameItemSameTags(existing, stack)
                && existing.getCount() + stack.getCount()
                   <= Math.min(itemHandler.getSlotLimit(slot), existing.getMaxStackSize());
    }

    /**
     * 真正往输出槽里塞，返回没塞下的余量（塞得下就是空栈）。同类物品会合并。
     *
     * <p><b>为什么不能用 {@link ItemStackHandler#insertItem}：</b>那个方法在写入前会先问
     * {@link ItemStackHandler#isItemValid}，而两个输出槽的 {@code isItemValid} 都是 false
     * （输出槽对玩家和管道只出不进）。也就是说，外部插不进去是设计如此，但机器自己也因此
     * 插不进去 —— 后果是连锁的：
     * <ul>
     *   <li>{@link #findRecipe} 里"输出槽放得下产物吗"永远返回 false，
     *       于是 <b>没有一条配方能匹配上，机器整个不工作</b>；</li>
     *   <li>{@link #finishCraft} 的产物永远落不进输出槽，全掉到地上；</li>
     *   <li>{@link #drainContainerIntoTank} 倒空后的空桶永远无处可放，流体槽跟着失效。</li>
     * </ul>
     * 所以机器自己的写入绕开校验，直接改内容；阀门仍然由 {@code isItemValid} 把着，
     * 玩家和管道照旧只能取不能放。
     */
    private ItemStack insertIntoOutput(int slot, ItemStack stack) {
        ItemStack existing = itemHandler.getStackInSlot(slot);

        if (existing.isEmpty()) {
            int moved = Math.min(stack.getCount(), itemHandler.getSlotLimit(slot));
            itemHandler.setStackInSlot(slot, stack.copyWithCount(moved));
            return stack.copyWithCount(stack.getCount() - moved);
        }

        if (!ItemStack.isSameItemSameTags(existing, stack)) {
            return stack;
        }

        int room = Math.min(itemHandler.getSlotLimit(slot), existing.getMaxStackSize())
                - existing.getCount();
        int moved = Math.min(room, stack.getCount());
        if (moved <= 0) {
            return stack;
        }
        ItemStack merged = existing.copy();
        merged.grow(moved);
        itemHandler.setStackInSlot(slot, merged);
        return stack.copyWithCount(stack.getCount() - moved);
    }

    /** 取当前进行中的配方；配方已被数据包重载移除时顺手把进度清掉。 */
    @Nullable
    private IceMakerRecipe getActiveRecipe() {
        if (activeRecipeId == null) {
            return null;
        }
        IceMakerRecipe recipe = getRecipeById(activeRecipeId);
        if (recipe == null) {
            activeRecipeId = null;
            progress = 0;
        }
        return recipe;
    }

    /**
     * 按 id 去 {@code RecipeManager} 里现查一条制冰机配方。
     *
     * <p>{@code /reload} 会把所有 Recipe 实例整批换掉，所以任何跨 tick 保存的东西都只能存 id、
     * 用的时候再查 —— 抓着对象引用会在重载后指向一个已经不在管理器里的旧配方。
     */
    @Nullable
    private IceMakerRecipe getRecipeById(@Nullable ResourceLocation id) {
        if (id == null || level == null) {
            return null;
        }
        return level.getRecipeManager().byKey(id)
                .filter(r -> r instanceof IceMakerRecipe)
                .map(r -> (IceMakerRecipe) r)
                .orElse(null);
    }

    /** 遍历所有制冰机配方，返回第一个三项条件都满足的。 */
    @Nullable
    private IceMakerRecipe findRecipe() {
        if (level == null) {
            return null;
        }
        ItemStack inputSlot = itemHandler.getStackInSlot(INPUT_SLOT);
        // 两个输入源都空的时候不可能匹配，早点退出，省掉每 tick 一次 getAllRecipesFor 的列表分配
        if (inputSlot.isEmpty() && fluidTank.isEmpty()) {
            return null;
        }

        FluidStack tank = fluidTank.getFluid();
        for (IceMakerRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.ICE_MAKER_TYPE.get())) {
            boolean canAccept = canInsertIntoOutput(OUTPUT_SLOT, recipe.getOutput());
            if (recipe.matchesInputs(tank, inputSlot, canAccept)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * 开工：记下配方，进度归零。
     *
     * <p><b>这里不扣料。</b>料是在产出那一刻（{@link #finishCraft}）才扣的。原因就是中止：
     * 输入槽一被改动这炉就要中止，如果开工时已经把水和物品吃掉，中止就变成了"白吞一份料"，
     * 玩家会觉得机器在偷东西。放到完成时扣，中止永远不亏。
     */
    private void startCraft(IceMakerRecipe recipe) {
        activeRecipeId = recipe.getId();
        progress = 0;
    }

    /**
     * 中止当前这炉：输入对不上了，进度归零。
     *
     * <p>因为料是完成时才扣的，这里不需要退还任何东西 —— 这也是把扣料挪到完成时的原因。
     */
    private void abortCraft() {
        activeRecipeId = null;
        progress = 0;
    }

    /**
     * 推进一 tick。
     *
     * <p>两种情况是<b>暂停</b>而不是中止，因为输入本身没出问题，等条件恢复了还能接着做：
     * <ul>
     *   <li>电量不够 —— 等来电；</li>
     *   <li>输出槽放不下这一份产物 —— 等玩家腾地方。</li>
     * </ul>
     * 输入不成立那种情况在 {@link #tick()} 里就中止掉了，走不到这儿。
     *
     * @return 状态是否发生变化
     */
    private boolean advanceCraft(IceMakerRecipe recipe) {
        // 配方里写的就直接是每 tick 的耗电，不用再按进度分摊。
        int thisTick = recipe.getEnergyPerTick();
        if (energyStorage.getEnergyStored() < thisTick) {
            return false;
        }
        // 输出槽满 = 暂停。等到进度条快走完才发现放不下的话，这一炉就白做了，
        // 所以每一 tick 都查一次，早发现早停。
        if (!canInsertIntoOutput(OUTPUT_SLOT, recipe.getOutput())) {
            return false;
        }

        energyStorage.consume(thisTick);

        progress++;
        if (progress >= recipe.getProcessingTime()) {
            finishCraft(recipe);
        }
        return true;
    }

    /** 完工：先扣料，扣得动才产出。 */
    private void finishCraft(IceMakerRecipe recipe) {
        // 输入本 tick 已经复查过，这里是最后一道保险。扣不动就宁可这炉白做，
        // 也绝不能凭空变出产物 —— 那就成刷物品了。
        if (!consumeInputs(recipe)) {
            abortCraft();
            return;
        }

        ItemStack leftover = insertIntoOutput(OUTPUT_SLOT, recipe.getOutput());
        if (!leftover.isEmpty() && level != null) {
            // 匹配阶段已经确认塞得下，走到这里说明中途被别的途径占满了。宁可掉在地上也别吞掉。
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), leftover);
        }
        // 记下"输出槽里这批东西是谁做的" —— 回流要靠它拿 recycle / recycle_delay
        lastCraftId = recipe.getId();
        idleTicks = 0;
        progress = 0;
        activeRecipeId = null;
    }

    /**
     * 扣掉这条配方要的流体和物品。
     *
     * <p>全部先模拟、再执行：中途任何一项对不上就整体放弃，不能扣了一半又退出。
     *
     * @return 是否扣成功
     */
    private boolean consumeInputs(IceMakerRecipe recipe) {
        FluidStack need = recipe.getFluidInput();
        if (!need.isEmpty()) {
            FluidStack drained = fluidTank.drain(need.getAmount(), IFluidHandler.FluidAction.SIMULATE);
            if (drained.getAmount() < need.getAmount()) {
                return false;
            }
        }

        ItemStack itemNeed = recipe.getItemInput();
        boolean eatItem = recipe.shouldConsumeItem() && !itemNeed.isEmpty();
        if (eatItem) {
            ItemStack taken = itemHandler.extractItem(INPUT_SLOT, itemNeed.getCount(), true);
            if (taken.getCount() < itemNeed.getCount()) {
                return false;
            }
        }

        // 模拟都过了，真扣。
        if (!need.isEmpty()) {
            fluidTank.drain(need.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        }
        if (eatItem) {
            itemHandler.extractItem(INPUT_SLOT, itemNeed.getCount(), false);
        }
        return true;
    }

    // ==================================================================================
    //  回流（输出槽 → 输入槽）
    // ==================================================================================

    /**
     * 回流：机器空闲、输入槽空够一段时间之后，把输出槽里的东西搬回输入槽。
     *
     * <p>这是给链式配方用的 —— 比如「水 + 雪球 → 冰」和「水 + 冰 → 压缩冰」两条配方，
     * 第一条的产物正好是第二条的原料，搬回去就能自己接着跑，不用玩家手动倒手。
     *
     * <p><b>开关怎么定</b>（按优先级从高到低）：
     * <ol>
     *   <li>产出这批东西的配方里写了 {@code recycle} → 用它的值，{@code false} 就是彻底不回；</li>
     *   <li>没写 → 查物品标签 {@link #RECYCLABLE_TAG}。</li>
     * </ol>
     * 延迟同理，配方没写 {@code recycle_delay} 就用 {@link IceMakerRecipe#DEFAULT_RECYCLE_DELAY}。
     *
     * <p><b>触发条件</b>是三件事同时成立：没有进行中的合成、输入槽为空、输出槽非空。
     * "没有进行中的合成"这条尤其重要 —— 不排除的话，耗时超过延迟的配方会在半途被塞进东西，
     * 而输入槽一变这炉就会中止（见 {@link #tick()}），等于自己把自己的活儿搅黄。
     *
     * @return 状态是否变化（需要存盘 + 同步）
     */
    private boolean handleRecycling() {
        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);

        boolean idle = activeRecipeId == null
                && itemHandler.getStackInSlot(INPUT_SLOT).isEmpty()
                && !output.isEmpty();

        if (!idle) {
            idleTicks = 0;
            // 输出槽空了，"谁产出的"这条记录也就失效了
            if (output.isEmpty() && lastCraftId != null) {
                lastCraftId = null;
                return true;
            }
            return false;
        }

        IceMakerRecipe producer = getRecipeById(lastCraftId);

        // 配方表态了就用配方的，没表态才看标签
        Boolean override = producer == null ? null : producer.getRecycle();
        boolean allow = override != null ? override : output.is(RECYCLABLE_TAG);
        if (!allow) {
            idleTicks = 0;
            return false;
        }

        int delay = producer == null
                ? IceMakerRecipe.DEFAULT_RECYCLE_DELAY
                : producer.getRecycleDelay();
        if (++idleTicks < delay) {
            return false;
        }

        idleTicks = 0;
        return moveOutputToInput(output);
    }

    /** 把输出槽整摞搬进输入槽；输入槽塞不下的部分留在输出槽。 */
    private boolean moveOutputToInput(ItemStack output) {
        // insertItem 只认"要塞进去的那份"的拷贝，返回没塞下的余量。
        // 先搬再写回，中间不能用 output 本身 —— 它是槽位里的活引用。
        ItemStack leftover = itemHandler.insertItem(INPUT_SLOT, output.copy(), false);
        if (leftover.getCount() == output.getCount()) {
            return false;
        }
        itemHandler.setStackInSlot(OUTPUT_SLOT, leftover);
        return true;
    }

    // ==================================================================================
    //  对外接口
    // ==================================================================================

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public IFluidHandler getFluidHandler() {
        return fluidTank;
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public ContainerData getData() {
        return data;
    }

    public int getProgress() {
        return progress;
    }

    /** 将当前机器内部的所有物品掉落到世界中。 */
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    // ==================================================================================
    //  能力
    // ==================================================================================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
        fluidCap.invalidate();
    }

    // ==================================================================================
    //  持久化与同步
    // ==================================================================================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(NBT_INVENTORY, itemHandler.serializeNBT());
        tag.put(NBT_TANK, fluidTank.writeToNBT(new CompoundTag()));
        tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored());
        tag.putInt(NBT_PROGRESS, progress);
        if (activeRecipeId != null) {
            tag.putString(NBT_ACTIVE_RECIPE, activeRecipeId.toString());
        }
        // idleTicks 不存：纯倒计时，重新数一遍没有任何副作用
        if (lastCraftId != null) {
            tag.putString(NBT_LAST_CRAFT, lastCraftId.toString());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound(NBT_INVENTORY));
        fluidTank.readFromNBT(tag.getCompound(NBT_TANK));
        energyStorage.setEnergyStored(tag.getInt(NBT_ENERGY));
        progress = tag.getInt(NBT_PROGRESS);

        activeRecipeId = readId(tag, NBT_ACTIVE_RECIPE);
        lastCraftId = readId(tag, NBT_LAST_CRAFT);
        idleTicks = 0;
    }

    /** 读一个 ResourceLocation 字段；存档里的 id 坏掉时当它不存在，不要因此炸掉整块区域。 */
    @Nullable
    private static ResourceLocation readId(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return null;
        }
        try {
            return new ResourceLocation(tag.getString(key));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 客户端同步用的 NBT。<b>直接复用 saveWithoutMetadata()</b>，
     * 让键名只有 saveAdditional 一处定义 —— 两边各写一份迟早会漏字段，
     * 而漏掉的字段在客户端表现为"GUI 上某个数是死的"。
     */
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ==================================================================================
    //  界面
    // ==================================================================================

    @Override
    public Component getDisplayName() {
        return Component.translatable("be.title.ice_maker");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new IceMakerMenu(id, inventory, this, data);
    }

    // ==================================================================================
    //  护目镜
    // ==================================================================================

    /** 本模组命名空间的语言构建器（LangBuilder 会把 key 拼成 "fib_mod.<key>"） */
    private static LangBuilder lang() {
        return new LangBuilder(FIBMod.MOD_ID);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        lang().translate("gui.goggles.ice_maker.stats").forGoggles(tooltip);

        // 进度： 40 / 200 tick（没在跑就不显示）
        IceMakerRecipe recipe = getActiveRecipe();
        if (recipe != null) {
            lang().translate("gui.goggles.ice_maker.progress").style(ChatFormatting.GRAY)
                    .space()
                    .add(CreateLang.number(progress).style(ChatFormatting.AQUA))
                    .text(ChatFormatting.DARK_GRAY, " / ")
                    .add(CreateLang.number(recipe.getProcessingTime()).style(ChatFormatting.DARK_GRAY))
                    .space()
                    .add(lang().translate("generic.unit.tick").style(ChatFormatting.GOLD))
                    .forGoggles(tooltip, 1);
        }

        // 流体： 3,000 / 8,000 mB  Water
        lang().translate("gui.goggles.ice_maker.fluid").style(ChatFormatting.GRAY)
                .space()
                .add(CreateLang.number(fluidTank.getFluidAmount()).style(ChatFormatting.AQUA))
                .text(ChatFormatting.DARK_GRAY, " / ")
                .add(CreateLang.number(TANK_CAPACITY).style(ChatFormatting.DARK_GRAY))
                .space()
                .add(lang().translate("generic.unit.mb").style(ChatFormatting.GOLD))
                .forGoggles(tooltip, 1);

        // 储能： 3,200 / 20,000 FE
        lang().translate("gui.goggles.energy").style(ChatFormatting.GRAY)
                .space()
                .add(CreateLang.number(energyStorage.getEnergyStored()).style(ChatFormatting.AQUA))
                .text(ChatFormatting.DARK_GRAY, " / ")
                .add(CreateLang.number(ENERGY_CAPACITY).style(ChatFormatting.DARK_GRAY))
                .space()
                .add(lang().translate("generic.unit.fe").style(ChatFormatting.GOLD))
                .forGoggles(tooltip, 1);

        return true;
    }

    // ==================================================================================
    //  内部能量存储
    // ==================================================================================

    /**
     * 只进不出的能量缓冲。
     *
     * <p>{@code maxExtract = 0} 让外部的 {@code extractEnergy} 恒返回 0（{@link EnergyStorage}
     * 的 {@code canExtract()} 就是 {@code maxExtract > 0}），机器自己的耗电走 {@link #consume}，
     * 绕开那条限制。
     */
    private static class InternalEnergyStorage extends EnergyStorage {
        public InternalEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0, 0);
        }

        /** 机器内部扣电，不受 maxExtract 限制。 */
        public void consume(int amount) {
            this.energy = Math.max(0, this.energy - amount);
        }

        public void setEnergyStored(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }
    }
}
