# Buildup Vitals

Buildup 系列的 Vanilla+ 玩家状态机制模组。第一目标平台为 Fabric / Minecraft 26.3。

当前为 **0.1.0-alpha.1 / Alpha Core + Cuisine Compatibility**：连续Saturation恢复、12/10 tick食物恢复、满饱食进食与积食、三档进食速度、可见饮食效果，以及40种原版、可选FD的80种、More Delight的31种和Kaleidoscope四模块299份Profile。Core契约见 [Alpha Core](docs/ALPHA_CORE.md)，兼容及验收见 [Farmer's Delight](docs/FARMERS_DELIGHT_COMPAT.md)、[More Delight](docs/MORE_DELIGHT_COMPAT.md)、[Kaleidoscope Series](docs/KALEIDOSCOPE_COMPAT.md)；平衡以 [BALANCE_SPEC_V2](BALANCE_SPEC_V2.md) 和 [设计原则](DESIGN_PRINCIPLES.md) 为准。

官方食物恢复基础值为0或至少1 HP（优先整数）；高饱和自然恢复直接采用原版10 tick基准，Food12、调养/Nourishment10、Stable80及自然最快10 tick保持不变。主动或良好状态收益允许有限正反馈，不要求所有组合处于原版±10%内；[恢复说明](docs/RECOVERY.md) 包含典型恢复时间对照。

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
.\gradlew.bat build -PacceptMinecraftEula=true
.\gradlew.bat runClient
.\gradlew.bat runServer
```

上面的build参数表示已阅读并同意 [Minecraft EULA](https://aka.ms/MinecraftEULA)，用于自动服务端GameTest。普通开发服务端首次运行会生成 `run/eula.txt`，同意协议后手动设置 `eula=true`，再次运行 `runServer`；控制台输入 `stop` 可正常停服。Linux/macOS使用 `./gradlew`，仓库已保存其执行位和LF换行。

构建产物：`build/libs/buildup_vitals-0.1.0-alpha.1.jar`。`run/` 中的测试世界、日志、协议选择和配置均不提交到Git；测试Mod和可选第三方Mod不捆绑进产物。

开发客户端使用Fabric开发账号，Realms认证失败不代表本地单人世界无法启动。`docs/`仅保留当前机制、数据包Schema及冻结契约；阶段报告和一次性修复报告保存在工作区根目录 `docs/26.3/`，不纳入版本仓库提交。版本变化见 [CHANGELOG](CHANGELOG.md) 和 [UPDATE_NOTES](UPDATE_NOTES.md)。

## Food Profile 数据与查询

数据包格式和优先级见 [Food Profile v1](docs/FOOD_PROFILES.md)。修改数据包后执行 `/reload`，再查询：

```text
/buildupvitals food profile minecraft:apple
/buildupvitals food profile minecraft:melon_slice
/buildupvitals food profile minecraft:baked_potato
```

命令需要 Game Masters 权限（通常 OP 2 / 开启作弊）。当前内置 40 份 Item Profile，并保留旧示例水果 Tag Profile。完整分组、原型数值和调整入口见 [原版平衡包](docs/VANILLA_BALANCE.md)。牛排、面包、烤马铃薯等保持 Basic 和原版营养；蘑菇煲/甜菜汤、兔肉煲、南瓜派提供不同的恢复或料理增益。Hydration 按可选适配生效；Traits 和 overrides 仍是预留数据。

## 可选口渴适配

支持 **Thirst Was Taken 2 Fabric 1.6.2+26.3**。优先级为 blacklist > Buildup 显式 Profile（含0）> TWT2 配置/drinks > 通用 fallback，只由 TWT2 结算一次。Pure Water Bottle 为10/8，其余水质与疾病语义保留；无需移除原有食物配置。详见 [HYDRATION](docs/HYDRATION.md)。

开发环境加入 `-PwithThirst=true` 才加载 TWT2，例如 `.\gradlew.bat runClient -PwithThirst=true`；普通构建仅有编译依赖，不捆绑该 Mod。口渴水滴复用 TWT2，有无 AppleSkin 均有提示。Quenched 独立治疗改为自然恢复速度 ×1.15，受10 tick最短周期约束；不改写用户配置。

## 恢复与验证

[恢复机制说明](docs/RECOVERY.md) 与 [料理增益说明](docs/MEAL_BENEFITS.md) 列出参数、刷新替换、死亡及重连规则。用 `/buildupvitals recovery` 查询自身恢复与增益状态；控制台或管理员可用 `/buildupvitals recovery <player>`。

[饮食记忆说明](docs/DIET_MEMORY.md) 解释十次窗口、奖励公式和重复饮食边界。用 `/buildupvitals diet [player]` 查询历史及倍率。缺少类别的 fallback 食物也记录进食，保留基础营养，不虚构类别。

[客户端反馈说明](docs/CLIENT_FEEDBACK.md) 解释普通与高级 Tooltip、基础数值、服务端同步及 AppleSkin 的兼容边界。无需安装其他 HUD Mod；本阶段不增加常驻状态条。

`.\gradlew.bat build` 运行 JVM 测试和服务端 GameTest。前者报告位于 `build/reports/tests/test/index.html`，后者位于 `build/run/gameTest/`。测试 Mod 与测试类不会打包进发布 JAR。

真实连接玩家的单人/同 JVM DedicatedServer TCP 测试单独运行：

```powershell
# 已阅读并同意 Minecraft EULA 后使用该参数；仅供自动测试环境。
.\gradlew.bat runClientGameTest -PacceptMinecraftEula=true
```

客户端测试自动创建测试世界并验证恢复、料理增益、饮食记忆、切维度、死亡、保存重进、Tooltip 同步及重连，并生成中英文普通/高级提示截图。运行目录为 `build/run/clientGameTest/`，不会操作日常开发世界。

安装 TWT2 的测试场景另加 `-PwithThirst=true`，覆盖补水、优先级、事件取消、纯水/盐水、重载、移除与重连；未安装时仅验证适配关闭，不把未执行的口渴场景计为通过。完整三组合命令见 [HYDRATION.md](docs/HYDRATION.md)。

可选 AppleSkin 兼容测试：从 [作者 Maven](https://maven.ryanliptak.com/squeek/appleskin/appleskin-fabric/mc26.3-3.0.10/) 取得对应 JAR 放到忽略目录 `run/compat/` 后执行：

```powershell
.\gradlew.bat runClientGameTest -PacceptMinecraftEula=true '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar'
```

该参数仅向 GameTest 运行时加入本地测试包，不下载、捆绑或添加生产依赖。

## 许可证

Mod 使用 [MIT License](LICENSE)。Gradle Wrapper 保留上游 Apache-2.0 声明。

## Farmer's Delight

可选支持 FD Refabricated **26.3-3.6.27**：开发或测试运行时加入 `-PwithFarmersDelight=true`，发布包不捆绑 FD。详见 [兼容说明、全部80份 Profile 与人工验收清单](docs/FARMERS_DELIGHT_COMPAT.md)。Nourishment 保留原生 ID、图标和持续时间，在统一主增益槽中提供调养 + 振奋语义。

可选附属 **More Delight 26.09.16-26.3-fabric** 的31种食品已提供完整 Profile，复用上述 Nourishment 桥接。使用时还需安装 More Delight 自身前置 **Delight Lib 26.09.16-26.3-fabric**。开发/测试加 `-PwithMoreDelight=true` 自动加载三个模组；生产包均不捆绑。完整平衡表、测试组合及人工验收见 [More Delight](docs/MORE_DELIGHT_COMPAT.md)。

## Kaleidoscope Series

可选支持Cookery **1.6.0.3**、Tavern **1.2.0.11**、Nether **1.1.13**、End **1.0.16** 的 `fabric+mc26.3` 发布。Vigor接入互斥主增益，其他特色料理效果保留玩法身份；重平衡护盾、Warmth、酒类再生/鸡尾酒、Star Blessing、Crimson、Mint、Void Erosion及龙料理预算。整盘食品按实际口数分配，独立饮用只结算一次；不修改第三方配置或JAR。

Nether / End还需安装Cookery和 **Forge Config API Port Fabric26.3.1**（Modrinth发布`JpKvrr9J`，上游元数据漏报）。深度桥接仅精确支持版本启用，未知版本会警告，不能假定已受预算约束。

开发/测试按需加入 `-PwithCookery=true`、`-PwithTavern=true`、`-PwithNether=true`、`-PwithEnd=true`；Nether/End参数自动加入Cookery和正确配置库，均仅作测试运行依赖。完整版本链接、四份Profile表、联合矩阵及人工验收清单见 [Kaleidoscope Compatibility](docs/KALEIDOSCOPE_COMPAT.md)。
