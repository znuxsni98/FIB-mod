package com.fib.fib.init;

import com.fib.fib.FIBMod;
import com.fib.fib.gui.container.menu.IceMakerMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class ModMenuTypes {


    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, FIBMod.MOD_ID);

    //region 注册区

    public static final RegistryObject<MenuType<IceMakerMenu>>
            ICE_MAKER_MENU =
            registerMenuType(
                    "ice_maker_menu",
                    IceMakerMenu::new
            );

    //endregion

    /**
     * 通用的 MenuType 注册方法。
     *
     * name：注册名称
     * factory：Menu 的构造器引用
     *
     * IForgeMenuType.create(factory) 会创建一个支持网络同步的 MenuType，
     * Forge 会利用这个 factory 在客户端和服务端分别构造 Menu。
     */
    private static <T extends AbstractContainerMenu>
    RegistryObject<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {

        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }

    /**
     * 将注册器挂载到 Forge 事件总线。
     *
     * 在模组初始化时调用此方法，
     * 这样 MENUS 中声明的所有 MenuType 才会真正被注册到游戏中。
     */
    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
