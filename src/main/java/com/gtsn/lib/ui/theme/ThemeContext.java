package com.gtsn.lib.ui.theme;

import java.util.List;
import java.util.Objects;

/**
 * 全局主题上下文：控件渲染读取的当前主题入口。
 *
 * <p>客户端资源加载完成时以 {@link #setRegistry(ThemeRegistry)} 挂载从资源解析出的注册表；
 * 界面通过 {@link #setActive(ThemeId)} / {@link #cycle()} 切换主题，渲染下一帧即生效。</p>
 *
 * <p>纯 Java 静态状态：服务端 / 无 MC 环境同样可加载与测试（GameTest 使用）。</p>
 */
public final class ThemeContext {

    private static volatile ThemeRegistry registry = ThemeRegistry.builtin();
    private static volatile ThemeId activeId = ThemeRegistry.DEFAULT_ID;

    private ThemeContext() {
    }

    /** 当前注册表（默认内置注册表）。 */
    public static ThemeRegistry registry() {
        return registry;
    }

    /**
     * 挂载注册表；若当前激活主题在新注册表中不存在，回退到默认主题。
     */
    public static synchronized void setRegistry(ThemeRegistry newRegistry) {
        Objects.requireNonNull(newRegistry, "registry");
        registry = newRegistry;
        if (newRegistry.find(activeId).isEmpty()) {
            activeId = ThemeRegistry.DEFAULT_ID;
        }
    }

    /** 当前激活主题 id。 */
    public static ThemeId activeId() {
        return activeId;
    }

    /** 当前激活主题（已解析继承链）。 */
    public static Theme active() {
        return registry.get(activeId);
    }

    /**
     * 切换激活主题；未知 id 返回 {@code false} 并回退到默认主题。
     */
    public static synchronized boolean setActive(ThemeId id) {
        if (id != null && registry.find(id).isPresent()) {
            activeId = id;
            return true;
        }
        activeId = ThemeRegistry.DEFAULT_ID;
        return false;
    }

    /** 切换到注册表中的下一个主题（注册顺序，环形）；返回新的激活 id。 */
    public static synchronized ThemeId cycle() {
        List<ThemeId> ids = registry.ids();
        int index = ids.indexOf(activeId);
        activeId = ids.get((index + 1) % ids.size());
        return activeId;
    }

    /** 恢复内置默认注册表与默认主题（资源重载 / 测试隔离）。 */
    public static synchronized void reset() {
        registry = ThemeRegistry.builtin();
        activeId = ThemeRegistry.DEFAULT_ID;
    }
}
