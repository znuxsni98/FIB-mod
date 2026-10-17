package com.fib.fib.compat.jei;

import com.fib.fib.blockentity.IceMakerBlockEntity;
import com.fib.fib.init.block.ModBlocks;
import com.fib.fib.init.recipe.custom.IceMakerRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * 制冰机的 JEI 配方页。
 *
 * <p>布局（槽位坐标固定，不随配方是否有流体输入而变 —— 变了的话 {@link #draw}
 * 那边就得重算一遍，两边很容易对不上）：
 * <pre>
 *   x:   0      18    26        48          74
 *        [流体]   +   [物品入]   →→→→   [产出]
 *   y:   3      4     4         5           4
 * </pre>
 */
public class IceMakerRecipeCategory implements IRecipeCategory<IceMakerRecipe> {

    private static final int WIDTH = 96;
    private static final int HEIGHT = 56;

    private static final int FLUID_X = 0;
    private static final int FLUID_Y = 3;

    private static final int PLUS_X = 18;
    private static final int PLUS_Y = 4;

    private static final int INPUT_X = 26;
    private static final int INPUT_Y = 4;

    private static final int ARROW_X = 48;
    private static final int ARROW_Y = 5;

    private static final int OUTPUT_X = 74;
    private static final int OUTPUT_Y = 4;

    private static final int TEXT_COLOR = 0xFF404040;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable plus;
    private final Component title;

    public IceMakerRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.ICE_MAKER.get());
        this.arrow = guiHelper.getRecipeArrow();
        this.plus = guiHelper.getRecipePlusSign();
        this.title = Component.translatable("fib_mod.jei.ice_maker.title");
    }

    @Override
    public RecipeType<IceMakerRecipe> getRecipeType() {
        return ModJeiPlugin.ICE_MAKER_TYPE;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IceMakerRecipe recipe, IFocusGroup focuses) {

        FluidStack fluidInput = recipe.getFluidInput();
        if (!fluidInput.isEmpty()) {
            builder.addInputSlot(FLUID_X, FLUID_Y)
                    // 按机器储罐的容量来画液面高度，和实际能装多少对得上
                    .setFluidRenderer(IceMakerBlockEntity.TANK_CAPACITY, true, 16, 16)
                    .addFluidStack(fluidInput.getFluid(), fluidInput.getAmount());
        }

        ItemStack itemInput = recipe.getItemInput();
        if (!itemInput.isEmpty()) {
            builder.addInputSlot(INPUT_X, INPUT_Y)
                    .setStandardSlotBackground()
                    .addItemStack(itemInput);
        }

        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
                .setOutputSlotBackground()
                .addItemStack(recipe.getOutput());
    }

    @Override
    public void draw(IceMakerRecipe recipe, IRecipeSlotsView slots,
                     GuiGraphics graphics, double mouseX, double mouseY) {

        arrow.draw(graphics, ARROW_X, ARROW_Y);
        if (!recipe.getFluidInput().isEmpty() && !recipe.getItemInput().isEmpty()) {
            plus.draw(graphics, PLUS_X, PLUS_Y);
        }

        Font font = Minecraft.getInstance().font;
        graphics.drawString(font,
                Component.translatable("fib_mod.jei.ice_maker.time", recipe.getProcessingTime()),
                0, 26, TEXT_COLOR, false);
        graphics.drawString(font,
                Component.translatable("fib_mod.jei.ice_maker.energy_per_tick", recipe.getEnergyPerTick()),
                0, 36, TEXT_COLOR, false);

        // 只有真的会回流的配方才提示，免得每一页都挂一行没用的字
        if (recycles(recipe)) {
            graphics.drawString(font,
                    Component.translatable("fib_mod.jei.ice_maker.recycle", recipe.getRecycleDelay())
                            .withStyle(ChatFormatting.DARK_AQUA),
                    0, 46, TEXT_COLOR, false);
        }
    }

    /**
     * 判断这条配方的产物会不会被搬回输入槽。
     *
     * <p>必须和 {@code IceMakerBlockEntity#handleRecycling} 用同一套规则，否则 JEI 显示的
     * 和机器实际做的会对不上：配方写了 {@code recycle} 就按配方的，没写才查物品标签。
     */
    private static boolean recycles(IceMakerRecipe recipe) {
        Boolean override = recipe.getRecycle();
        if (override != null) {
            return override;
        }
        return recipe.getOutput().is(IceMakerBlockEntity.RECYCLABLE_TAG);
    }
}
