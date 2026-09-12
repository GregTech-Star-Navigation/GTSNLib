package com.gtsn.lib.ui.demo;

import com.gtsn.lib.GTSNLib;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 演示菜单类型注册（公共侧）。
 *
 * <p>菜单类型必须在两端一致注册；客户端屏幕经 {@code MenuScreens.register} 在客户端初始化时绑定
 * （见 {@code com.gtsn.lib.ui.client.GtsnUiScreens}）。</p>
 */
public final class DemoMenus {

    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, GTSNLib.MOD_ID);

    /** 数据同步演示菜单类型。 */
    public static final RegistryObject<MenuType<DemoMenu>> SYNC_DEMO =
            MENU_TYPES.register("sync_demo", () -> IForgeMenuType.create(DemoMenu::fromNetwork));

    private DemoMenus() {
    }

    /** 在 mod 事件总线上注册（入口调用）。 */
    public static void register(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}
