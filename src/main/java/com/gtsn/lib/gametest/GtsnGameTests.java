package com.gtsn.lib.gametest;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.api.IntegrationModule;
import com.gtsn.lib.api.IntegrationRegistry;
import com.gtsn.lib.api.IntegrationTargets;
import com.gtsn.lib.compat.mekanism.ChemicalStatus;
import com.gtsn.lib.compat.mekanism.MekanismChemicals;
import com.gtsn.lib.core.ForgeModPresence;
import com.gtsn.lib.core.GtsnIntegrations;
import com.gtsn.lib.core.config.GtsnCommonConfig;
import com.gtsn.lib.gt.adapter.GtAdapter;
import com.gtsn.lib.gt.adapter.GtContentStatus;
import com.gtsn.lib.gt.adapter.GtFluidStatus;
import com.gtsn.lib.gt.adapter.GtPartStatus;
import com.gtsn.lib.gt.adapter.GtQueryResult;
import com.gtsn.lib.gt.registration.BlockRegistration;
import com.gtsn.lib.gt.registration.FluidRegistration;
import com.gtsn.lib.gt.registration.ItemRegistration;
import com.gtsn.lib.gt.registration.MachineRegistration;
import com.gtsn.lib.gt.registration.MaterialPart;
import com.gtsn.lib.gt.registration.MaterialRegistration;
import com.gtsn.lib.gt.registration.RegistrationKind;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 最小 GameTest 集：证明 {@code runGameTestServer} 可运行、库已被加载，且 {@code /gtsnlib}
 * 与 {@code /gtsnlib gt}（经 GTCEu 适配层）在真实加载环境下的输出符合预期。
 */
@GameTestHolder(GTSNLib.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GtsnGameTests {
    private GtsnGameTests() {
    }

    /** 捕获一次命令执行的返回码与消息。 */
    private record CommandOutput(int result, List<String> messages) {
    }

    private static CommandOutput runCommand(GameTestHelper helper, String command) {
        List<String> messages = new ArrayList<>();
        CommandSource source = new CommandSource() {
            @Override
            public void sendSystemMessage(Component component) {
                messages.add(component.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };

        MinecraftServer server = helper.getLevel().getServer();
        CommandSourceStack stack = server.createCommandSourceStack().withSource(source);
        int result = server.getCommands().performPrefixedCommand(stack, command);
        return new CommandOutput(result, messages);
    }

    @GameTest(template = "empty")
    public static void modIsLoaded(GameTestHelper helper) {
        if (!ModList.get().isLoaded(GTSNLib.MOD_ID)) {
            helper.fail("GTSNLib is not loaded");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gtsnlibCommandPrintsStatus(GameTestHelper helper) {
        CommandOutput output = runCommand(helper, "gtsnlib");

        long present = IntegrationTargets.modIds().stream().filter(id -> ModList.get().isLoaded(id)).count();
        String expectedStatus = "GTSNLib 0.1.0 | integrations: 6 | targets present: " + present + "/6";

        if (output.result() != 1) {
            helper.fail("/gtsnlib returned " + output.result());
            return;
        }
        if (!output.messages().contains(expectedStatus)) {
            helper.fail("/gtsnlib status line was " + output.messages() + ", expected " + expectedStatus);
            return;
        }
        for (String modId : IntegrationTargets.modIds()) {
            String expected = modId + ": " + (ModList.get().isLoaded(modId) ? "present" : "absent");
            if (!output.messages().contains(expected)) {
                helper.fail("/gtsnlib did not report target " + expected + ": " + output.messages());
                return;
            }
        }
        helper.succeed();
    }

    /** 真实 {@link ForgeModPresence} 冒烟：必须与运行时 {@link ModList} 判定一致。 */
    @GameTest(template = "empty")
    public static void forgeModPresenceMatchesRealModList(GameTestHelper helper) {
        ForgeModPresence presence = new ForgeModPresence();
        if (!presence.isLoaded(GTSNLib.MOD_ID)) {
            helper.fail("ForgeModPresence did not detect GTSNLib itself in the real ModList");
            return;
        }
        if (presence.isLoaded("gtsnlib_not_a_real_mod")) {
            helper.fail("ForgeModPresence reported a non-existent mod as loaded");
            return;
        }
        helper.succeed();
    }

    /** 六个联动模块均已登记；在场的模块已初始化，缺席的模块未被实例化（真实环境两态覆盖）。 */
    @GameTest(template = "empty")
    public static void integrationModulesMatchRealPresence(GameTestHelper helper) {
        IntegrationRegistry registry = GtsnIntegrations.registry();
        if (registry.registeredCount() != 6) {
            helper.fail("expected 6 registered integration modules, got " + registry.registeredCount());
            return;
        }
        for (String modId : IntegrationTargets.modIds()) {
            boolean present = ModList.get().isLoaded(modId);
            IntegrationModule module = registry.get(modId).orElse(null);
            if (present && module == null) {
                helper.fail("present target was not initialized: " + modId);
                return;
            }
            if (!present && module != null) {
                helper.fail("absent target was initialized: " + modId);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gtsnlibGtCommandQueriesAdapter(GameTestHelper helper) {
        CommandOutput output = runCommand(helper, "gtsnlib gt");

        if (output.result() != 1) {
            helper.fail("/gtsnlib gt returned " + output.result());
            return;
        }
        boolean available = output.messages().stream()
                .anyMatch(line -> line.startsWith("GT adapter | available: true"));
        boolean material = output.messages().stream()
                .anyMatch(line -> line.contains("material iron: present @ gtceu:iron"));
        if (!available || !material) {
            helper.fail("/gtsnlib gt output was " + output.messages());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gtAdapterQueriesMaterials(GameTestHelper helper) {
        GtAdapter adapter = GtAdapter.get();
        if (!adapter.available()) {
            helper.fail("GT material registry is unavailable through the adapter");
            return;
        }
        if (adapter.tagPrefixCount() <= 0) {
            helper.fail("GT tag prefix registry is empty through the adapter");
            return;
        }
        GtQueryResult result = adapter.query(GtAdapter.DEFAULT_PROBE_MATERIAL);
        if (!result.materialPresent()) {
            helper.fail("GT material " + GtAdapter.DEFAULT_PROBE_MATERIAL + " not found through the adapter");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialRegistrationGeneratesDerivedPartsAndTags(GameTestHelper helper) {
        GtAdapter adapter = GtAdapter.get();
        String id = GtAdapter.DEMO_MATERIAL;

        if (!adapter.query(id).materialPresent()) {
            helper.fail("DSL-registered material " + id + " is not present in GTCEu");
            return;
        }

        MaterialRegistration registration = adapter.registeredMaterial(id).orElse(null);
        if (registration == null) {
            helper.fail("material " + id + " was not registered through the GTSN registrar");
            return;
        }
        for (String part : List.of("ingot", "plate", "dust", "rod")) {
            if (!registration.derivedItems().containsKey(part)) {
                helper.fail("registration missing derived part " + part + ": " + registration.derivedItems());
                return;
            }
        }
        for (String tag : List.of(
                "forge:ingots/star_alloy", "forge:plates/star_alloy",
                "forge:dusts/star_alloy", "forge:rods/star_alloy")) {
            if (!registration.hasOreTag(tag)) {
                helper.fail("registration missing ore tag " + tag + ": " + registration.oreTags());
                return;
            }
        }

        Set<MaterialPart> parts = new LinkedHashSet<>();
        for (String key : registration.derivedItems().keySet()) {
            parts.add(MaterialPart.fromKey(key));
        }
        List<GtPartStatus> statuses = adapter.partStatus(id, parts);
        if (statuses.size() != 4) {
            helper.fail("expected 4 live part statuses, got " + statuses);
            return;
        }
        for (GtPartStatus status : statuses) {
            if (!status.itemGenerated()) {
                helper.fail("GTCEu did not generate item for part " + status.part() + ": " + status);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gtsnlibGtMaterialCommandReportsRegistration(GameTestHelper helper) {
        CommandOutput output = runCommand(helper, "gtsnlib gt material gtsnlib:star_alloy");

        if (output.result() != 1) {
            helper.fail("/gtsnlib gt material returned " + output.result());
            return;
        }
        boolean present = output.messages().stream()
                .anyMatch(line -> line.contains("material gtsnlib:star_alloy: present"));
        boolean derived = output.messages().stream()
                .anyMatch(line -> line.contains("registration gtsnlib:star_alloy") && line.contains("ingot"));
        boolean generated = output.messages().stream()
                .anyMatch(line -> line.contains("part ingot") && line.contains("generated=true"));
        if (!present || !derived || !generated) {
            helper.fail("/gtsnlib gt material output was " + output.messages());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void materialRegistersLiquidGasPlasmaFluidForms(GameTestHelper helper) {
        GtAdapter adapter = GtAdapter.get();
        MaterialRegistration registration = adapter.registeredMaterial(GtAdapter.DEMO_MATERIAL).orElse(null);
        if (registration == null) {
            helper.fail("material " + GtAdapter.DEMO_MATERIAL + " was not registered through the GTSN registrar");
            return;
        }
        List<GtFluidStatus> fluids = adapter.materialFluids(GtAdapter.DEMO_MATERIAL);
        if (fluids.size() != 3) {
            helper.fail("expected 3 material fluid forms, got " + fluids);
            return;
        }
        for (String state : List.of("liquid", "gas", "plasma")) {
            String expectedId = registration.fluid(state).orElse(null);
            if (expectedId == null) {
                helper.fail("material registration missing fluid state " + state + ": " + registration.fluids());
                return;
            }
            GtFluidStatus status = fluids.stream()
                    .filter(fluid -> state.equals(fluid.stateKey()))
                    .findFirst().orElse(null);
            if (status == null || !status.present()) {
                helper.fail("material fluid form " + state + " not present in GT registry: " + fluids);
                return;
            }
            if (!GtAdapter.DEMO_MATERIAL.equals(status.materialKey())) {
                helper.fail("fluid " + state + " not linked to material: " + status);
                return;
            }
            if (!expectedId.equals(status.fluidId())) {
                helper.fail("fluid " + state + " registered at " + status.fluidId() + ", expected " + expectedId);
                return;
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void standaloneFluidIsRegisteredAndQueryable(GameTestHelper helper) {
        GtAdapter adapter = GtAdapter.get();
        FluidRegistration registration = adapter.registeredFluid(GtAdapter.DEMO_FLUID).orElse(null);
        if (registration == null) {
            helper.fail("standalone fluid " + GtAdapter.DEMO_FLUID + " was not registered through the GTSN registrar");
            return;
        }
        if (!registration.standalone()) {
            helper.fail("fluid " + GtAdapter.DEMO_FLUID + " unexpectedly linked to material "
                    + registration.materialKey());
            return;
        }
        GtFluidStatus status = adapter.fluidStatus(GtAdapter.DEMO_FLUID);
        if (!status.present()) {
            helper.fail("standalone fluid " + GtAdapter.DEMO_FLUID + " absent from GT registry: " + status);
            return;
        }
        if (status.materialLinked()) {
            helper.fail("standalone fluid reported material link: " + status);
            return;
        }
        if (!"gas".equals(status.stateKey())) {
            helper.fail("standalone fluid state was " + status.stateKey());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void gtsnlibGtFluidCommandReportsFluid(GameTestHelper helper) {
        CommandOutput output = runCommand(helper, "gtsnlib gt fluid gtsnlib:star_alloy_plasma");

        if (output.result() != 1) {
            helper.fail("/gtsnlib gt fluid returned " + output.result());
            return;
        }
        boolean present = output.messages().stream()
                .anyMatch(line -> line.contains("fluid gtsnlib:star_alloy_plasma: present @ gtsnlib:star_alloy_plasma")
                        && line.contains("state=plasma")
                        && line.contains("[material gtsnlib:star_alloy]"));
        if (!present) {
            helper.fail("/gtsnlib gt fluid output was " + output.messages());
            return;
        }
        helper.succeed();
    }

    /** 通用注册 helper 的游戏内证据：演示方块（含物品）、物品与机器均进入真实注册表（#15）。 */
    @GameTest(template = "empty")
    public static void genericRegistrationHelperRegistersBlockItemAndMachine(GameTestHelper helper) {
        GtAdapter adapter = GtAdapter.get();

        BlockRegistration block = adapter.registeredBlock(GtAdapter.DEMO_BLOCK).orElse(null);
        if (block == null) {
            helper.fail("block " + GtAdapter.DEMO_BLOCK + " was not registered through the GTSN registrar");
            return;
        }
        if (!block.hasItem()) {
            helper.fail("block " + GtAdapter.DEMO_BLOCK + " did not generate a block item");
            return;
        }
        GtContentStatus blockStatus = adapter.contentStatus(RegistrationKind.BLOCK, GtAdapter.DEMO_BLOCK);
        if (!blockStatus.present()) {
            helper.fail("block " + GtAdapter.DEMO_BLOCK + " absent from Minecraft block registry: " + blockStatus);
            return;
        }
        GtContentStatus blockItemStatus =
                adapter.contentStatus(RegistrationKind.ITEM, block.itemResourceLocation());
        if (!blockItemStatus.present()) {
            helper.fail("block item " + block.itemResourceLocation()
                    + " absent from Minecraft item registry: " + blockItemStatus);
            return;
        }

        ItemRegistration item = adapter.registeredItem(GtAdapter.DEMO_ITEM).orElse(null);
        if (item == null) {
            helper.fail("item " + GtAdapter.DEMO_ITEM + " was not registered through the GTSN registrar");
            return;
        }
        GtContentStatus itemStatus = adapter.contentStatus(RegistrationKind.ITEM, GtAdapter.DEMO_ITEM);
        if (!itemStatus.present()) {
            helper.fail("item " + GtAdapter.DEMO_ITEM + " absent from Minecraft item registry: " + itemStatus);
            return;
        }

        MachineRegistration machine = adapter.registeredMachine(GtAdapter.DEMO_MACHINE).orElse(null);
        if (machine == null) {
            helper.fail("machine " + GtAdapter.DEMO_MACHINE + " was not registered through the GTSN registrar");
            return;
        }
        GtContentStatus machineStatus = adapter.contentStatus(RegistrationKind.MACHINE, GtAdapter.DEMO_MACHINE);
        if (!machineStatus.present()) {
            helper.fail("machine " + GtAdapter.DEMO_MACHINE + " absent from GT machine registry: " + machineStatus);
            return;
        }
        helper.succeed();
    }

    /** {@code /gtsnlib reg} 在真实环境下报告已登记内容及其存在性（#15 的游戏内证据）。 */
    @GameTest(template = "empty")
    public static void gtsnlibRegCommandReportsRegisteredContent(GameTestHelper helper) {
        CommandOutput output = runCommand(helper, "gtsnlib reg");

        if (output.result() != 1) {
            helper.fail("/gtsnlib reg returned " + output.result());
            return;
        }
        boolean header = output.messages().stream()
                .anyMatch(line -> line.equals("GTSNLib registrations | blocks: 1 | items: 1 | machines: 1"));
        boolean block = output.messages().stream()
                .anyMatch(line -> line.contains("block gtsnlib:test_block: present=true"));
        boolean item = output.messages().stream()
                .anyMatch(line -> line.contains("item gtsnlib:test_item: present=true"));
        boolean machine = output.messages().stream()
                .anyMatch(line -> line.contains("machine gtsnlib:test_machine: present=true"));
        if (!header || !block || !item || !machine) {
            helper.fail("/gtsnlib reg output was " + output.messages());
            return;
        }
        helper.succeed();
    }

    /** 未登记的资源位置在真实注册表中报告为缺席（负向覆盖，#15）。 */
    @GameTest(template = "empty")
    public static void contentStatusReportsAbsentForUnknownId(GameTestHelper helper) {
        GtContentStatus status = GtAdapter.get()
                .contentStatus(RegistrationKind.BLOCK, "gtsnlib:not_a_real_block");
        if (status.present()) {
            helper.fail("unknown block reported as present: " + status);
            return;
        }
        helper.succeed();
    }

    /**
     * Mekanism 化学注册随真实在场动态判定（#14）：Mekanism 在场时，经声明式 DSL 注册的演示化学物质必须
     * 存在于 Mekanism 注册表且可经命令查询；缺席时后端保持未安装且不触发任何类链接错误。
     */
    @GameTest(template = "empty")
    public static void mekanismChemicalRegistrationMatchesPresence(GameTestHelper helper) {
        boolean present = ModList.get().isLoaded(IntegrationTargets.MEKANISM.modId());
        if (!present) {
            if (MekanismChemicals.available()) {
                helper.fail("Mekanism chemical backend was installed while Mekanism is absent");
                return;
            }
            ChemicalStatus status = MekanismChemicals.status(MekanismChemicals.DEMO_CHEMICAL);
            if (status.present()) {
                helper.fail("chemical reported present while Mekanism is absent: " + status);
                return;
            }
            CommandOutput output = runCommand(helper, "gtsnlib mek chemical " + MekanismChemicals.DEMO_CHEMICAL);
            if (output.result() != 1) {
                helper.fail("/gtsnlib mek chemical returned " + output.result());
                return;
            }
            boolean unavailable = output.messages().stream()
                    .anyMatch(line -> line.contains("backend unavailable"));
            if (!unavailable) {
                helper.fail("/gtsnlib mek chemical output was " + output.messages());
                return;
            }
            helper.succeed();
            return;
        }

        if (!MekanismChemicals.available()) {
            helper.fail("Mekanism is present but the chemical backend was not installed");
            return;
        }
        if (!MekanismChemicals.isRegistered(MekanismChemicals.DEMO_CHEMICAL)) {
            helper.fail("demo chemical " + MekanismChemicals.DEMO_CHEMICAL
                    + " was not registered through the chemical DSL");
            return;
        }
        ChemicalStatus status = MekanismChemicals.status(MekanismChemicals.DEMO_CHEMICAL);
        if (!status.present()) {
            helper.fail("DSL-registered chemical " + MekanismChemicals.DEMO_CHEMICAL
                    + " is absent from Mekanism registry: " + status);
            return;
        }
        if (!"gas".equals(status.kindKey()) || !"mekanism:gas".equals(status.registryId())) {
            helper.fail("chemical kind/registry was " + status.kindKey() + " / " + status.registryId());
            return;
        }
        CommandOutput output = runCommand(helper, "gtsnlib mek chemical " + MekanismChemicals.DEMO_CHEMICAL);
        boolean presentLine = output.messages().stream()
                .anyMatch(line -> line.contains("chemical " + MekanismChemicals.DEMO_CHEMICAL + ": present @")
                        && line.contains("kind=gas")
                        && line.contains("registry=mekanism:gas"));
        if (!presentLine) {
            helper.fail("/gtsnlib mek chemical output was " + output.messages());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void commonConfigIsLoaded(GameTestHelper helper) {
        if (GtsnCommonConfig.INSTANCE == null) {
            helper.fail("common config was not registered");
            return;
        }
        if (!GtsnCommonConfig.INSTANCE.logIntegrationSummary) {
            helper.fail("common config example option has unexpected value");
            return;
        }
        helper.succeed();
    }
}
