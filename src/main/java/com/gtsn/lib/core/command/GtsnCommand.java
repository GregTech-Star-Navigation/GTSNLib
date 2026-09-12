package com.gtsn.lib.core.command;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.api.IntegrationRegistry;
import com.gtsn.lib.compat.mekanism.ChemicalReport;
import com.gtsn.lib.compat.mekanism.MekanismChemicals;
import com.gtsn.lib.core.GtsnBuildInfo;
import com.gtsn.lib.core.GtsnIntegrations;
import com.gtsn.lib.core.IntegrationSummary;
import com.gtsn.lib.gt.adapter.GtAdapter;
import com.gtsn.lib.gt.adapter.GtAdapterReport;
import com.gtsn.lib.gt.adapter.GtContentStatus;
import com.gtsn.lib.gt.adapter.GtFluidReport;
import com.gtsn.lib.gt.adapter.GtFluidStatus;
import com.gtsn.lib.gt.adapter.GtPartStatus;
import com.gtsn.lib.gt.adapter.GtQueryResult;
import com.gtsn.lib.gt.adapter.GtRegistrationReport;
import com.gtsn.lib.gt.registration.BlockRegistration;
import com.gtsn.lib.gt.registration.ItemRegistration;
import com.gtsn.lib.gt.registration.MachineRegistration;
import com.gtsn.lib.gt.registration.MaterialPart;
import com.gtsn.lib.gt.registration.MaterialRegistration;
import com.gtsn.lib.gt.registration.RegistrationKind;
import com.gtsn.lib.ui.demo.DemoMenu;
import com.gtsn.lib.ui.demo.DemoMenus;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@code /gtsnlib} 诊断命令：打印库版本、已登记联动数与各目标 mod 的在场/缺席状态。
 * 子命令 {@code /gtsnlib gt} 经 GTCEu 适配层查询材料与 tag prefix，作为适配层运行时证据；
 * 子命令 {@code /gtsnlib gt material <id>} 额外打印经注册简化层登记的材料的衍生件与矿词。
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
                .then(Commands.literal("gt")
                        .executes(GtsnCommand::executeGt)
                        .then(Commands.literal("material")
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .executes(GtsnCommand::executeGtMaterial)))
                        .then(Commands.literal("fluid")
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .executes(GtsnCommand::executeGtFluid))))
                .then(Commands.literal("reg")
                        .executes(GtsnCommand::executeReg))
                .then(Commands.literal("mek")
                        .executes(GtsnCommand::executeMek)
                        .then(Commands.literal("chemical")
                                .then(Commands.argument("id", StringArgumentType.greedyString())
                                        .executes(GtsnCommand::executeMekChemical))))
                .then(Commands.literal("ui")
                        .executes(GtsnCommand::executeUi)));
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

    /**
     * {@code /gtsnlib gt material <id>}：查询材料在 GTCEu 中的存在性，并在该材料经注册简化层登记时，
     * 打印其衍生件、矿词与实时生成状态（#12 的游戏内证据）。
     */
    private static int executeGtMaterial(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        GtAdapter adapter = GtAdapter.get();
        List<String> lines = new ArrayList<>(GtAdapterReport.commandLines(adapter.query(id)));

        Optional<MaterialRegistration> registration = adapter.registeredMaterial(id);
        if (registration.isPresent()) {
            MaterialRegistration material = registration.get();
            lines.add("registration " + material.resourceLocation() + " | derived: " + material.derivedItems());
            lines.add("ore tags: " + material.oreTags());
            Set<MaterialPart> parts = new LinkedHashSet<>();
            for (String key : material.derivedItems().keySet()) {
                parts.add(MaterialPart.fromKey(key));
            }
            for (GtPartStatus status : adapter.partStatus(id, parts)) {
                lines.add("part " + status.part() + " -> " + status.itemId()
                        + " [" + status.oreTag() + "] generated=" + status.itemGenerated());
            }
        } else {
            lines.add("registration " + id + ": absent");
        }

        for (GtFluidStatus fluid : adapter.materialFluids(id)) {
            lines.add(GtFluidReport.materialFluidLine(fluid));
        }

        for (String line : lines) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    /**
     * {@code /gtsnlib gt fluid <id>}：查询流体在 GT 流体注册表中的存在性与物态，
     * 并在命中材料流体形态或一次性流体登记时打印材料关联（#13 的游戏内证据）。
     */
    private static int executeGtFluid(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        GtFluidStatus status = GtAdapter.get().fluidStatus(id);
        for (String line : GtFluidReport.commandLines(status)) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    /**
     * {@code /gtsnlib reg}：列出经通用注册简化层登记的方块 / 物品 / 机器，并逐一查询其在真实注册表中的
     * 存在性（#15 的游戏内证据）。
     */
    private static int executeReg(CommandContext<CommandSourceStack> context) {
        GtAdapter adapter = GtAdapter.get();
        List<GtContentStatus> statuses = new ArrayList<>();
        for (BlockRegistration registration : adapter.blocks()) {
            statuses.add(adapter.contentStatus(RegistrationKind.BLOCK, registration.resourceLocation()));
        }
        for (ItemRegistration registration : adapter.items()) {
            statuses.add(adapter.contentStatus(RegistrationKind.ITEM, registration.resourceLocation()));
        }
        for (MachineRegistration registration : adapter.machines()) {
            statuses.add(adapter.contentStatus(RegistrationKind.MACHINE, registration.resourceLocation()));
        }
        for (String line : GtRegistrationReport.commandLines(statuses,
                adapter.blocks().size(), adapter.items().size(), adapter.machines().size())) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    /**
     * {@code /gtsnlib ui}：为执行者打开数据同步演示菜单（#19 的游戏内入口）。
     *
     * <p>菜单由服务端创建并经 {@code NetworkHooks.openScreen} 打开；需玩家执行。演示门控关闭时
     * 菜单类型未登记，此处返回可读失败信息，而不是让 {@code SYNC_DEMO.get()} 抛错。</p>
     */
    private static int executeUi(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!DemoMenus.SYNC_DEMO.isPresent()) {
            context.getSource().sendFailure(Component.literal(
                    "[GTSNLib] demo content is disabled; sync demo menu is not registered"
                            + " (set registerDemoContent=true to enable)."));
            return 0;
        }
        NetworkHooks.openScreen(player, DemoMenu.provider());
        return 1;
    }

    /**
     * {@code /gtsnlib mek}：列出 Mekanism 化学注册后端可用性与已声明化学物质数量（#14 的游戏内证据）。
     *
     * <p>本类只依赖纯门面 {@link MekanismChemicals}（GTSN 自有类型），不引用任何 Mekanism 类型。</p>
     */
    private static int executeMek(CommandContext<CommandSourceStack> context) {
        for (String line : ChemicalReport.summaryLines(
                MekanismChemicals.available(), MekanismChemicals.registrations().size())) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    /**
     * {@code /gtsnlib mek chemical <id>}：查询化学物质在 Mekanism 注册表中的存在性、种类与颜色
     * （#14 的游戏内证据）；Mekanism 缺席时报告后端不可用。
     */
    private static int executeMekChemical(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "id");
        for (String line : ChemicalReport.commandLines(MekanismChemicals.status(id))) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }
}
