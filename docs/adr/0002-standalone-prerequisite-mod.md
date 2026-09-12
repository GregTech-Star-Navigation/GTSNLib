# GTSNLib 以独立前置 mod 分发

GTSNLib 作为独立 mod 发布；依赖它的 mod 在 `mods.toml` 中声明依赖，玩家/整合包自行安装。不采用 jarJar 内嵌。

**Considered Options**: jarJar 内嵌（多个 mod 会重复装载同名库，注册表/全局状态冲突、体积膨胀、版本漂移）。

**Consequences**: 升级库无需重发全部依赖 mod；玩家需额外安装一个前置（生态惯例，如 Patchouli / Curios）。
