# Buildup Vitals

Buildup 系列的 Vanilla+ 玩家状态机制模组。第一目标平台为 Fabric / Minecraft 26.3。

当前处于 Stage 5（Client Feedback & Tooltips）：食物提示显示品质、基础恢复、料理增益和饮食类别，中英文及 F3+H 高级信息均可用。显示数据随服务端登录和成功重载同步；既有恢复与饮食机制保持不变。设计总纲见 [DESIGN_PRINCIPLES.md](DESIGN_PRINCIPLES.md)，分阶段实施计划见 [PLAN.md](PLAN.md)。每个 Stage 验收通过后才能进入下一阶段；战斗节奏与数值平衡按用户要求留到主要功能和机制基本完成后评估。

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

开发客户端使用 Fabric 开发账号，Realms 认证失败不代表本地单人世界无法启动。历史验证见 [Stage 0 报告](docs/STAGE_0_REPORT.md)、[Stage 1 报告](docs/STAGE_1_REPORT.md)、[Stage 2 报告](docs/STAGE_2_REPORT.md)、[Stage 3 报告](docs/STAGE_3_REPORT.md)、[Stage 4 报告](docs/STAGE_4_REPORT.md)；当前验证和人工验收见 [Stage 5 报告](docs/STAGE_5_REPORT.md)。

## Food Profile 数据与查询

数据包格式和优先级见 [Food Profile v1](docs/FOOD_PROFILES.md)。修改数据包后执行 `/reload`，再查询：

```text
/buildupvitals food profile minecraft:apple
/buildupvitals food profile minecraft:melon_slice
/buildupvitals food profile minecraft:baked_potato
```

命令需要 Game Masters 权限（通常 OP 2 / 开启作弊）。当前内置六份示例 Profile，展示 Item、Tag、料理增益和饮食类别；未匹配时使用 fallback。蘑菇煲基础提供 3 HP 储备与 Restorative，南瓜派基础提供 1 HP 储备与 Invigorated。牛排、面包、苹果和水果 Tag 提供类别信息，原版食物营养保持不变。Hydration、Traits 和 overrides 仍是预留数据。

## 恢复与验证

[恢复机制说明](docs/RECOVERY.md) 与 [料理增益说明](docs/MEAL_BENEFITS.md) 列出参数、刷新替换、死亡及重连规则。用 `/buildupvitals recovery` 查询自身恢复与增益状态；控制台或管理员可用 `/buildupvitals recovery <player>`。

[饮食记忆说明](docs/DIET_MEMORY.md) 解释十次窗口、奖励公式和重复饮食边界。用 `/buildupvitals diet [player]` 查询历史及倍率。缺少类别的 fallback 食物也记录进食，保留基础营养，不虚构类别。

[客户端反馈说明](docs/CLIENT_FEEDBACK.md) 解释普通与高级 Tooltip、基础数值、服务端同步及 AppleSkin 的兼容边界。无需安装其他 HUD Mod；本阶段不增加常驻状态条。

`.\gradlew.bat build` 运行 JVM 测试和服务端 GameTest。前者报告位于 `build/reports/tests/test/index.html`，后者位于 `build/run/gameTest/`。测试 Mod 与测试类不会打包进发布 JAR。

真实连接玩家的单人/独立服务端测试单独运行：

```powershell
# 已阅读并同意 Minecraft EULA 后使用该参数；仅供自动测试环境。
.\gradlew.bat runClientGameTest -PacceptMinecraftEula=true
```

客户端测试自动创建测试世界并验证恢复、料理增益、饮食记忆、切维度、死亡、保存重进、Tooltip 同步及重连，并生成中英文普通/高级提示截图。运行目录为 `build/run/clientGameTest/`，不会操作日常开发世界。

可选 AppleSkin 兼容测试：从 [作者 Maven](https://maven.ryanliptak.com/squeek/appleskin/appleskin-fabric/mc26.3-3.0.10/) 取得对应 JAR 放到忽略目录 `run/compat/` 后执行：

```powershell
.\gradlew.bat runClientGameTest -PacceptMinecraftEula=true '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar'
```

该参数仅向 GameTest 运行时加入本地测试包，不下载、捆绑或添加生产依赖。

## 许可证

Mod 使用 [MIT License](LICENSE)。Gradle Wrapper 保留上游 Apache-2.0 声明。
