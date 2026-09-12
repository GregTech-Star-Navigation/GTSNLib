package com.gtsn.lib.core.command;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.core.GtsnBuildInfo;
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
 * {@code /gtsnlib} 诊断命令：打印库版本与当前联动数。
 *
 * <p>按 ADR-0003，事件订阅类不得引用任何可选联动 mod 的类型；仅使用纯静态 {@link GtsnBuildInfo}。</p>
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
        // T1: 尚无联动模块，固定为 0；T2 起由 IntegrationRegistry 提供真实计数。
        String status = GtsnBuildInfo.formatStatus(GtsnBuildInfo.VERSION, 0);
        context.getSource().sendSuccess(() -> Component.literal(status), false);
        return 1;
    }
}
