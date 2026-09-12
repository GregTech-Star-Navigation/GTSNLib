package com.gtsn.lib.ui.client;

import com.gtsn.lib.ui.theme.ThemeContext;
import com.gtsn.lib.ui.theme.ThemeDefinition;
import com.gtsn.lib.ui.theme.ThemeId;
import com.gtsn.lib.ui.theme.ThemeParseException;
import com.gtsn.lib.ui.theme.ThemeParser;
import com.gtsn.lib.ui.theme.ThemeRegistry;
import com.gtsn.lib.ui.theme.ThemeResolver;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 主题资源加载（客户端专用）：经 {@link ResourceManager} 枚举 {@code assets/<namespace>/ui/themes/*.json}，
 * 用无 MC 的 {@link ThemeParser} 解析、{@link ThemeResolver} 解析继承后挂载到 {@link ThemeContext}。
 *
 * <p>无任何文件系统路径：资源定位全部经由 {@code ResourceLocation} / {@code ResourceManager}；
 * 单个主题文件解析失败仅记录日志并跳过，注册表保底默认主题。</p>
 *
 * <p>客户端专用类：专职服务端不得加载（类加载纪律，见 ADR-0003/0004）。</p>
 */
public final class ThemeResources implements PreparableReloadListener {

    /** 主题资源目录（相对 {@code assets/<namespace>/}）。 */
    public static final String THEME_DIRECTORY = "ui/themes";

    private static final String JSON_SUFFIX = ".json";
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> loadDefinitions(manager), backgroundExecutor)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(ThemeResources::apply, gameExecutor);
    }

    /** 同步读取并应用主题（供测试 / 手动刷新；资源管理器需处于可用状态）。 */
    public static void loadAndApply(ResourceManager manager) {
        apply(loadDefinitions(manager));
    }

    /** 枚举并解析全部主题定义；单个文件失败不影响其它文件。 */
    static List<ThemeDefinition> loadDefinitions(ResourceManager manager) {
        List<ThemeDefinition> definitions = new ArrayList<>();
        Map<ResourceLocation, Resource> resources = manager.listResources(
                THEME_DIRECTORY, location -> location.getPath().endsWith(JSON_SUFFIX));
        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation location = entry.getKey();
            ThemeId id = themeIdOf(location);
            try (Reader reader = entry.getValue().openAsReader()) {
                definitions.add(ThemeParser.parse(id, reader));
            } catch (ThemeParseException error) {
                LOGGER.warn("[GTSNLib] 主题 {} 解析失败，已跳过: {}", id, error.getMessage());
            } catch (Exception error) {
                LOGGER.warn("[GTSNLib] 主题 {} 读取失败，已跳过: {}", id, error.toString());
            }
        }
        return definitions;
    }

    private static void apply(List<ThemeDefinition> definitions) {
        ThemeRegistry registry = ThemeResolver.resolve(definitions);
        ThemeContext.setRegistry(registry);
        LOGGER.info("[GTSNLib] UI 主题已加载: {}（激活: {}）", registry.ids(), ThemeContext.activeId());
    }

    /** {@code ui/themes/<name>.json} → {@code <namespace>:<name>}。 */
    static ThemeId themeIdOf(ResourceLocation location) {
        String path = location.getPath();
        String name = path.substring(THEME_DIRECTORY.length() + 1);
        if (name.endsWith(JSON_SUFFIX)) {
            name = name.substring(0, name.length() - JSON_SUFFIX.length());
        }
        return ThemeId.of(location.getNamespace(), name);
    }
}
