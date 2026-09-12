package com.gtsn.lib.gametest;

import com.gtsn.lib.GTSNLib;
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
import java.util.List;

/**
 * 最小 GameTest 集：证明 {@code runGameTestServer} 可运行、库已被加载，且 {@code /gtsnlib}
 * 在真实加载环境下的输出符合预期。
 */
@GameTestHolder(GTSNLib.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GtsnGameTests {
    private GtsnGameTests() {
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
        int result = server.getCommands().performPrefixedCommand(stack, "gtsnlib");

        if (result != 1) {
            helper.fail("/gtsnlib returned " + result);
            return;
        }
        if (!messages.contains("GTSNLib 0.1.0 | integrations: 0")) {
            helper.fail("/gtsnlib output was " + messages);
            return;
        }
        helper.succeed();
    }
}
