package com.fib.fib.init.recipe;

import com.fib.fib.FIBMod;
import com.fib.fib.init.recipe.custom.IceMakerRecipe;
import com.fib.fib.init.recipe.custom.IceMakerRecipeSerializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 自定义配方类型与序列化器的注册入口。
 *
 * <p>配方类型和序列化器是两个独立的注册表：
 * <ul>
 *   <li>{@code RecipeType} 只用来分类/查找（{@code RecipeManager#getAllRecipesFor}），
 *       本身不解析任何东西，所以 {@link RecipeType#simple} 给个 id 就够了；</li>
 *   <li>{@code RecipeSerializer} 才是真正读 JSON 的那个，JSON 里的 {@code "type"} 字段
 *       匹配的是<b>序列化器</b>的注册名。</li>
 * </ul>
 *
 * <p>两者的注册名都取 {@code ice_maker}，于是 JSON 里写 {@code "type": "fib_mod:ice_maker"}。
 */
public class ModRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, FIBMod.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, FIBMod.MOD_ID);

    public static final RegistryObject<RecipeType<IceMakerRecipe>> ICE_MAKER_TYPE =
            RECIPE_TYPES.register("ice_maker", () ->
                    RecipeType.simple(new ResourceLocation(FIBMod.MOD_ID, "ice_maker")));

    public static final RegistryObject<RecipeSerializer<IceMakerRecipe>> ICE_MAKER_SERIALIZER =
            RECIPE_SERIALIZERS.register("ice_maker", IceMakerRecipeSerializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
