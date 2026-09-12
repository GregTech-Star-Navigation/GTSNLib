package com.gtsn.lib.core.command;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.api.IntegrationRegistry;
import com.gtsn.lib.core.GtsnBuildInfo;
import com.gtsn.lib.core.GtsnIntegrations;
import com.gtsn.lib.core.IntegrationSummary;
import com.gtsn.lib.gt.adapter.GtAdapter;
import com.gtsn.lib.gt.adapter.GtAdapterReport;
import com.gtsn.lib.gt.adapter.GtQueryResult;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * {@code /gtsnlib} 诊断命令：打印库版本、已登记联动数与各目标 mod 的在场/缺席状态。
 * 子命令 {@code /gtsnlib gt} 经 GTCEu 适配层查询材料与 tag prefix，作为适配层运行时证据。
 *
 * <p>按 ADR-0003，事件订阅类不得引用任何可选联动 mod 的类型；此处只依赖纯库类型与适配层门面。</p>
 */
@Mod.EventBusSubscriber(modid = GTSNLib.MOD_ID, bus = Bus.FORGE)
public final class GtsnCommand {
    private GtsnCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("gtsnlib")
                .executes(GtsnCommand::execute)
                .then(Commands.literal("gt").executes(GtsnCommand::executeGt)));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        IntegrationRegistry registry = GtsnIntegrations.registry();
        for (String line : IntegrationSummary.commandLines(
                GtsnBuildInfo.VERSION, registry.registeredCount(), GtsnIntegrations.targetStates(registry))) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    /**
     * {@code /gtsnlib gt}：经 GTCEu 适配层查询已知材料与 tag prefix 总量，运行时验证适配层可用。
     *
     * <p>本类只依赖 {@link GtAdapter} 门面（GTSN 自有类型），不引用任何 GTCEu 类型。</p>
     */
    private static int executeGt(CommandContext<CommandSourceStack> context) {
        GtQueryResult result = GtAdapter.get().query(GtAdapter.DEFAULT_PROBE_MATERIAL);
        for (String line : GtAdapterReport.commandLines(result)) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }
}
