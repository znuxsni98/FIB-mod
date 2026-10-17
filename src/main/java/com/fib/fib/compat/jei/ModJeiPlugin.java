package com.fib.fib.compat.jei;

import com.fib.fib.FIBMod;
import com.fib.fib.init.block.ModBlocks;
import com.fib.fib.init.recipe.ModRecipes;
import com.fib.fib.init.recipe.custom.IceMakerRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * JEI 插件入口。
 *
 * <p><b>整个类只能被 JEI 通过注解扫描发现，绝不能被 {@code ClientSetup} / {@code FIBMod}
 * 这类一定会加载的类引用</b> —— JEI 在 build.gradle 里是 {@code compileOnly}，
 * 开发环境以外没有它，被主动引用就是 {@code NoClassDefFoundError} 崩启动。
 * 靠 {@code @JeiPlugin} 注解发现正好满足这一点：JEI 用 ASM 读注解、不加载类。
 */
@JeiPlugin
public class ModJeiPlugin implements IModPlugin {

    /**
     * JEI 的配方类型。uid 特意和 {@link ModRecipes#ICE_MAKER_TYPE} 保持一致
     * （{@code fib_mod:ice_maker}），这样两边在日志和调试里对得上号。
     */
    public static final RecipeType<IceMakerRecipe> ICE_MAKER_TYPE =
            RecipeType.create(FIBMod.MOD_ID, "ice_maker", IceMakerRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(FIBMod.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new IceMakerRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ICE_MAKER_TYPE, getAllIceMakerRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 在 JEI 里点"用途/配方"时，制冰机本体作为催化剂出现在左侧
        registration.addRecipeCatalyst(ModBlocks.ICE_MAKER.get(), ICE_MAKER_TYPE);
    }

    /**
     * 直接从客户端的 {@code RecipeManager} 拿配方 —— 配方在进服时已经由
     * {@code ClientboundUpdateRecipesPacket} 同步过来，没必要再走 JEI 的
     * {@code IRecipeManager} 绕一圈。
     */
    private static List<IceMakerRecipe> getAllIceMakerRecipes() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            // 主菜单里没有 level，但那时 JEI 也不会来问
            return List.of();
        }
        return level.getRecipeManager().getAllRecipesFor(ModRecipes.ICE_MAKER_TYPE.get());
    }
}
