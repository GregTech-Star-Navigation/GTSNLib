package com.gtsn.lib.ui.demo;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.core.DemoContentRuntime;
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
 * （见 {@code com.gtsn.lib.ui.client.GtsnUiScreens}）。与其他演示内容一致，注册受
 * {@link DemoContentRuntime} 门控：生产安装默认不登记本菜单类型（{@link #SYNC_DEMO} 保持未注册态，
 * {@code isPresent()} 为 {@code false}），开发运行恒登记。</p>
 */
public final class DemoMenus {

    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, GTSNLib.MOD_ID);

    /** 数据同步演示菜单类型。 */
    public static final RegistryObject<MenuType<DemoMenu>> SYNC_DEMO =
            MENU_TYPES.register("sync_demo", () -> IForgeMenuType.create(DemoMenu::fromNetwork));

    private DemoMenus() {
    }

    /**
     * 在 mod 事件总线上注册（入口调用）。
     *
     * <p>演示门控关闭时不挂接 {@link DeferredRegister}，注册表不会写入 {@code gtsnlib:sync_demo}
     * 条目；{@link #SYNC_DEMO} 字段本身保持 {@code isPresent() == false}，引用方需容忍缺席。</p>
     */
    public static void register(IEventBus modBus) {
        if (!DemoContentRuntime.enabled()) {
            return;
        }
        MENU_TYPES.register(modBus);
    }
}
