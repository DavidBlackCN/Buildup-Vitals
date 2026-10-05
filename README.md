# Buildup Vitals

Buildup 系列的 Vanilla+ 玩家状态机制模组。第一目标平台为 Fabric / Minecraft 26.3。

当前处于 Stage 0（Bootstrap），尚未实现任何 Gameplay 机制。设计总纲见 [DESIGN_PRINCIPLES.md](DESIGN_PRINCIPLES.md)，分阶段实施计划见 [PLAN.md](PLAN.md)。每个 Stage 验收通过后才能进入下一阶段。

## 当前工程基线

- Minecraft 26.3
- Fabric Loader 0.19.5
- Fabric API 0.161.0+26.3
- Fabric Loom 1.18.2（固定发布版）
- Gradle Wrapper 9.7.1
- Java 25
- 映射：使用 26.3 原生非混淆名称，不声明 Yarn 或额外 `mappings` 依赖
- Maven group: `com.davidblackcn`
- Mod ID: `buildup_vitals`

完整 Wrapper（包括 JAR）已从 [FabricMC 官方 26.3 示例工程](https://github.com/FabricMC/fabric-example-mod/tree/44465cb0eb83932c72ece5934d32ddfc758802ed) 同步。Loom 从模板的 `1.18-SNAPSHOT` 固定到同系列已发布补丁版 `1.18.2`，以便复现构建。

## 构建与启动

在此目录运行，先将 `JAVA_HOME` 指向本机 JDK 25；IDE 的 Gradle JVM 也应选择 JDK 25。不要将本机绝对路径写进共享 Gradle 配置。

```powershell
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-25.0.3' # 按本机安装位置调整
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
.\gradlew.bat clean build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

开发服务端首次运行会生成 `run/eula.txt`。阅读并同意 [Minecraft EULA](https://aka.ms/MinecraftEULA) 后，手动设置 `eula=true`，再次运行 `runServer`；控制台输入 `stop` 可正常停服。

构建产物：`build/libs/buildup_vitals-0.1.0-dev.jar`。`run/` 中的测试世界、日志、协议选择和配置均不提交到 Git。

开发客户端使用 Fabric 开发账号，Realms 认证失败不代表本地单人世界无法启动。运行时验证结果和人工验收清单见 [Stage 0 报告](docs/STAGE_0_REPORT.md)。

## 许可证

Mod 使用 [MIT License](LICENSE)。Gradle Wrapper 保留上游 Apache-2.0 声明。
