package com.gtsn.lib.datagen;

import com.gtsn.lib.GTSNLib;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Data generation 入口（{@code runData}）：T1 仅产出语言文件骨架。
 */
@Mod.EventBusSubscriber(modid = GTSNLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GtsnDataGenerator {
    private GtsnDataGenerator() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();

        generator.addProvider(event.includeClient(), new GtsnLanguageProvider(output, "en_us"));
        generator.addProvider(event.includeClient(), new GtsnLanguageProvider(output, "zh_cn"));
    }
}
