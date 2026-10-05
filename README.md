# Buildup Vitals

Buildup 系列的 Vanilla+ 玩家状态机制模组。第一目标平台为 Fabric / Minecraft 26.3。

当前仓库处于工程初始化阶段。设计总纲见 `DESIGN_PRINCIPLES.md`，Codex 分阶段实施计划见 `PLAN.md`。

## 当前工程基线

- Minecraft 26.3
- Fabric Loader 0.19.5
- Fabric API 0.161.0+26.3
- Fabric Loom 1.18-SNAPSHOT
- Java 25
- Maven group: `com.davidblackcn`
- Mod ID: `buildup_vitals`

> 注意：本打包骨架未包含二进制 `gradle-wrapper.jar`。首次正式开工时请让 Codex 从 FabricMC 官方 `fabric-example-mod` 的 `26.3` 分支重新同步 Gradle Wrapper，并先完成 `./gradlew build` 与客户端/服务端启动验证，再进入功能开发。
