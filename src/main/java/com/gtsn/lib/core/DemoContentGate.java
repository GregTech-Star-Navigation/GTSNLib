package com.gtsn.lib.core;

/**
 * 演示内容登记门控谓词（纯函数，无 Forge / Minecraft 依赖，可无头单测）。
 *
 * <p>GTSNLib 是 GTSN 项目群的前置库：它不应把演示内容（材料 {@code gtsnlib:star_alloy} 及其流体、
 * 一次性流体 {@code gtsnlib:stellar_air}、{@code test_block}/{@code test_item}/{@code test_machine}
 * 与化学物质 {@code gtsnlib:test_chemical}）无条件写进每一个安装的注册表与世界。</p>
 *
 * <p>门控规则：开发运行（{@code production=false}，含 {@code runClient}/{@code runServer}/
 * {@code runGameTestServer}/单测）恒登记，保证既有 GameTest 与演示入口可用；生产安装默认不登记，
 * 仅当配置显式选择加入（{@code registerDemoContent=true}）时登记。</p>
 *
 * @see DemoContentRuntime 运行时解析（读取 {@code FMLEnvironment.production} 与配置）
 */
public final class DemoContentGate {

    private DemoContentGate() {
    }

    /**
     * 是否登记演示内容。
     *
     * @param production   是否生产运行（{@code FMLEnvironment.production}）
     * @param configOptIn  配置是否显式选择加入（{@code registerDemoContent}）
     * @return 开发态恒 {@code true}；生产态仅在显式选择加入时为 {@code true}
     */
    public static boolean shouldRegister(boolean production, boolean configOptIn) {
        return !production || configOptIn;
    }
}
