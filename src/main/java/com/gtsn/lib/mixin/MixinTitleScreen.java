package com.gtsn.lib.mixin;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.TitleScreen;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 低风险客户端 Mixin：证明 Mixin 基建（refmap + config + AP）已生效。
 * 仅在标题界面初始化时打一条日志，不改动任何游戏行为。
 */
@Mixin(TitleScreen.class)
public class MixinTitleScreen {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Inject(method = "init()V", at = @At("HEAD"))
    private void gtsnlib$onInit(CallbackInfo ci) {
        LOGGER.info("[GTSNLib] MixinTitleScreen injected (mixin infrastructure online)");
    }
}
