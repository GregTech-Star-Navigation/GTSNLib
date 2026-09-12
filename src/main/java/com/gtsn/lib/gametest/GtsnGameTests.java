package com.gtsn.lib.gametest;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.core.config.GtsnCommonConfig;
import com.gtsn.lib.gt.adapter.GtAdapter;
import com.gtsn.lib.gt.adapter.GtPartStatus;
import com.gtsn.lib.gt.adapter.GtQueryResult;
import com.gtsn.lib.gt.registration.MaterialPart;
import com.gtsn.lib.gt.registration.MaterialRegistration;
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

        if (output.result() != 1) {
            helper.fail("/gtsnlib returned " + output.result());
            return;
        }
        if (!output.messages().contains("GTSNLib 0.1.0 | integrations: 0 | targets present: 0/6")) {
            helper.fail("/gtsnlib status line was " + output.messages());
            return;
        }
        for (String modId : List.of("mekanism", "immersiveengineering", "create", "ae2", "enderio", "ad_astra")) {
            if (!output.messages().contains(modId + ": absent")) {
                helper.fail("/gtsnlib did not report target " + modId + ": " + output.messages());
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
