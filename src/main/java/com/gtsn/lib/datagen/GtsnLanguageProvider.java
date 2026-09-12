package com.gtsn.lib.datagen;

import com.gtsn.lib.GTSNLib;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/**
 * 生成 {@code assets/gtsnlib/lang/<locale>.json}。T1 只包含命令相关的占位词条。
 */
public class GtsnLanguageProvider extends LanguageProvider {

    public GtsnLanguageProvider(PackOutput output, String locale) {
        super(output, GTSNLib.MOD_ID, locale);
    }

    @Override
    protected void addTranslations() {
        add("gtsnlib.command.status", "GTSNLib status");
    }
}
