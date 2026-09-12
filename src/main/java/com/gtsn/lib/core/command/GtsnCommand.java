package com.gtsn.lib.core.command;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.api.IntegrationRegistry;
import com.gtsn.lib.core.GtsnBuildInfo;
import com.gtsn.lib.core.GtsnIntegrations;
import com.gtsn.lib.core.IntegrationSummary;
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
 *
 * <p>按 ADR-0003，事件订阅类不得引用任何可选联动 mod 的类型；此处只依赖纯库类型。</p>
 */
@Mod.EventBusSubscriber(modid = GTSNLib.MOD_ID, bus = Bus.FORGE)
public final class GtsnCommand {
    private GtsnCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("gtsnlib").executes(GtsnCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        IntegrationRegistry registry = GtsnIntegrations.registry();
        for (String line : IntegrationSummary.commandLines(
                GtsnBuildInfo.VERSION, registry.registeredCount(), GtsnIntegrations.targetStates(registry))) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }
}
