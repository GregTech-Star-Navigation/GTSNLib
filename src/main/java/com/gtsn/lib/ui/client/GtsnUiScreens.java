package com.gtsn.lib.ui.client;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.ui.demo.DemoMenus;
import com.gtsn.lib.ui.screen.DemoMenuScreen;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;

/**
 * 客户端菜单屏幕注册（仅客户端加载，{@code bus = MOD} 订阅 {@link FMLClientSetupEvent}）。
 *
 * <p>把 {@link DemoMenus#SYNC_DEMO} 绑定到 {@link DemoMenuScreen}，使服务端打开菜单时客户端
 * 能创建对应界面。专职服务端不加载本类（类加载纪律，见 ADR-0003/0004）。</p>
 */
@Mod.EventBusSubscriber(modid = GTSNLib.MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
public final class GtsnUiScreens {

    private static final Logger LOGGER = LogUtils.getLogger();

    private GtsnUiScreens() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(DemoMenus.SYNC_DEMO.get(), DemoMenuScreen::new);
            LOGGER.info("[GTSNLib] sync demo menu screen registered");
        });
    }
}
