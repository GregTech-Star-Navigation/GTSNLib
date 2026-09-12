package com.gtsn.lib.ui.client;

import com.gtsn.lib.GTSNLib;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 主题资源加载的客户端接线：把 {@link ThemeResources} 注册进客户端资源重载管线
 * （初次加载与 F3+T 资源包重载均会触发）。
 *
 * <p>客户端专用类：{@code @EventBusSubscriber(value = Dist.CLIENT)} 保证专职服务端不加载本类
 * （类加载纪律，见 ADR-0003/0004）。</p>
 */
@Mod.EventBusSubscriber(modid = GTSNLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GtsnUiThemeEvents {

    private GtsnUiThemeEvents() {
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new ThemeResources());
    }
}
