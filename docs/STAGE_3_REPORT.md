# Stage 3 Completion Report

日期：2026-10-05（Asia/Shanghai）。目标目录：`26.3`。阶段：Food Quality & Meal Benefit Framework。

## 本轮验收范围

用户已通过 Stage 2 的功能与机制验收，明确要求将战斗节奏及数值评估、微调推迟到 Mod 主要功能和机制基本完成后。因此本轮验证机制正确性和兼容边界，不对料理收益感或战斗平衡作定稿结论。此要求也适用于前一阶段报告中的数值体验待办。

## 完成内容

- 四档 Quality 首次参与 Gameplay：显式有效增益分别持续 600 / 1200 / 2400 / 3600 tick，不改写 Hunger / Saturation 或 Recovery 总量。
- 新增内置类型注册表、不可变单槽位状态和服务器权威的玩家适配。相同增益刷新到新旧时长较大值，不累加；不同已实现增益替换。
- Restorative 将储备兑现周期从 50 tick 缩短到 40 tick，总治疗量不变，保留现有进度及受击不中断规则。
- Invigorated 将原版指定移动与跳跃耗竭乘以 0.9，自然恢复仍支付每实际恢复 1 HP 的完整 6 Exhaustion。
- 新增独立持久化附件，保存类型和剩余时间；死亡清除，跨维度、保存重进和断线重连保留。Stage 2 恢复附件格式不变。
- 蘑菇煲现有 Restorative 引用生效；新增南瓜派 Prepared / Recovery 1 / Invigorated 示例，原版营养不变。
- 查询命令显示增益可用性、主要增益 ID、剩余时间及实际恢复周期；未知/实验引用有明确数据警告并保留其他有效字段。
- Steady 按计划保留实验类型、框架与禁用行为测试，没有擅自定义饱和锁定或药水替代机制。Feast secondary modifier 未启用。

详细参数与数据语义见 [MEAL_BENEFITS.md](MEAL_BENEFITS.md)、[FOOD_PROFILES.md](FOOD_PROFILES.md) 和 [RECOVERY.md](RECOVERY.md)。

## 主要文件与原因

| 文件 | 变更用途 |
|---|---|
| `food/benefit/` | 类型、注册表、原型参数、单槽位状态、授予与倒计时 |
| `player/MealBenefitAttachments.java` | 新增独立持久化，不改旧 Recovery 格式 |
| `food/recovery/PlayerRecovery.java`、`RecoveryController.java` | 消费完成时授予增益，动态食物恢复周期，保持自然恢复成本 |
| `mixin/ActivityExhaustionMixin.java`、`src/main/resources/buildup_vitals.mixins.json` | 仅在原版活动调用点减少耗竭 |
| `BuildupVitals.java`、`data/loader/FoodProfileLoader.java` | 生命周期注册、无效增益引用诊断 |
| `command/FoodProfileCommand.java`、`RecoveryCommand.java`、`food/profile/FoodProfile.java` | 查询与当前字段语义 |
| `src/main/resources/data/buildup_vitals/buildup_vitals/food_profiles/pumpkin_pie.json` | 最小 Invigorated 示例 |
| `src/test/`、`src/gametest/` | 单元、服务端、真实连接客户端及错误引用夹具 |
| [README.md](../README.md)、当前机制文档、本报告、[UPDATE_NOTES.md](../UPDATE_NOTES.md) | 当前行为、验证结果和验收清单 |

表中短 Java 路径相对 `src/main/java/com/davidblackcn/buildupvitals/`。未修改根目录或其他版本；开工时工作树干净。

## 兼容性决策与证据

继续使用 Minecraft 26.3、Java 25（本机 25.0.3）、Gradle 9.7.1、Loom 1.18.2、Loader 0.19.5、Fabric API 0.161.0+26.3；原生非混淆名称，无 Yarn / 额外 mappings。未增加生产依赖、修改 Wrapper 或放宽元数据约束。开工核对了 [Fabric 官方 26.3 示例属性](https://raw.githubusercontent.com/FabricMC/fabric-example-mod/26.3/gradle.properties)，保持现有固定发布版基线。

API 和注入证据来自目标版本本地 sources JAR 及已解析 Fabric 依赖，之后经编译和实际启动验证：

- `ServerPlayer.checkMovementStatistics(DDD)V` 内 6 个 `ServerPlayer.causeFoodExhaustion(F)V`，`jumpFromGround()V` 内 2 个同目标调用；`ModifyArg` 分别要求 `require/allow=6/6` 与 `2/2`。
- 原版攀爬分支只记录统计，没有耗竭，不新增消耗。步行/潜行的零耗竭保持为零。
- 不改全局 `causeFoodExhaustion`、`FoodData.addExhaustion` 或自然恢复成本；没有原版 MobEffect 注册或客户端权威计算。
- Fabric Attachment 负责持久化与活着替换玩家的复制；死亡事件立即删除，未启用 copy-on-death。
- 保留 Stage 2 Recovery 的保存范围，Restorative 只改变本轮调度间隔，不迁移或重写旧状态。

## 实际验证

在 `26.3` 中设置 `JAVA_HOME=C:/Program Files/Java/jdk-25.0.3`，将其 `bin` 加入当前进程 PATH，清空本机 `DEBUG` 环境变量后执行：

```powershell
.\gradlew.bat compileJava test --console=plain
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true --console=plain
```

第一项用于初步编译和已有测试，成功；最终第二项 **BUILD SUCCESSFUL**，包含必需的 Wrapper `build`。EULA 参数仅用于已获用户授权的自动测试环境。

| 检查 | 实际结果 |
|---|---|
| JVM 测试 | **80 通过**：Parser 49、Snapshot 7、Recovery 16、Meal Benefit 8；失败/错误 0 |
| 服务端 GameTest | **14 通过**：原 Recovery 7、Meal Benefit 6、框架 sanity 1 |
| 单槽位与时长 | 四档 Quality、显式授予、同类刷新不缩短/不累加、不同替换、到期移除通过 |
| Restorative | 实际食物消费后较早恢复；与无增益对照均仅兑现 3 HP；小数总量和周期切换有单元覆盖 |
| Invigorated | 普通/疾跑跳跃、疾跑和游泳累计耗竭从 1.35 变为 1.215；直接耗竭仍为原值；自然恢复仍扣 6 |
| 原版食物和效果 | 南瓜派营养及饱和保留，蘑菇煲返碗；Basic/fallback 正常；金苹果效果独立，没有 Buildup 药水堆叠 |
| 错误引用 | 测试数据中的未知 ID 与 Steady 产生包含资源/包/字段的预期警告；Recovery 和营养保留，已有增益不受影响 |
| 生命周期 | 精确 NBT 往返、活着复制、死亡不复制、满血倒计时、创造模式禁用及玩家状态隔离通过 |
| 真实单人玩家 | 原恢复回归、授予增益、下界往返、死亡重生、查询、同一存档关闭重开通过 |
| 真实客户端 + DedicatedServer | localhost 连接，执行恢复及增益生命周期场景；断开重连后保留状态，通过 |
| 发布产物与资源 | 源码 JSON 均可解析；发布 JAR 仅含 4 份生产 Profile，不含测试类、测试 Mod 或错误引用夹具 |
| Git 与文档 | 差异空白检查、文档本地链接与路径检查通过 |

JVM 报告：`build/reports/tests/test/index.html`。运行日志：`build/run/gameTest/logs/latest.log`、`build/run/clientGameTest/logs/latest.log`；本轮副本为 `run/stage3-server-gametest.log`、`run/stage3-client-gametest.log`，完整构建输出为 `run/stage3-build.log`。运行目录及日志不提交。

首轮服务端测试失败源于原版 `makeMockServerPlayer` 固定返回创建时的游戏模式，调用 `setGameMode` 无法改变模拟对象的 `gameMode()`。已使用对应模式创建模拟玩家，保留行为断言；期间补回 API 返回 `Player` 所需的 `ServerPlayer` 类型转换，修复测试编译错误。最终重新执行完整构建和客户端测试通过。

测试直接走完整 `finishUsingItem` 路径，真实连接场景运行正常游戏 tick；没有将这些结果表述为手动长按进食或战斗体验已完成。

## 自检

```text
Code Review: PASS WITH RISKS

Scope:
- Reviewed files: 本报告列出的全部生产代码、Mixin、资源、测试和文档差异
- Target version/directory: Minecraft 26.3 / 26.3

Findings:
- BLOCKER: None
- MAJOR: None
- MINOR: None

Fixes Applied During Review:
- 增益 ID 使用共享 Mod ID；清理当前文档遗留的字段只读说明。
- 查询显示有效食物周期及引用可用性；补充错误引用和原版耗竭成本回归。
- 修正模拟玩家的游戏模式夹具与返回类型，未降低断言要求。

Verification:
- Wrapper build / 80 JVM tests / 14 server GameTests: PASS
- Connected integrated + dedicated client lifecycle tests: PASS
- Mixin runtime / server-client boundary / unchanged recovery persistence: PASS
- JSON / release JAR / documentation links / git diff checks: PASS
- Update Notes: PASS

Residual Risks:
- 尚未验证同时修改移动耗竭、进食或自然恢复的第三方 Mod。
- 未执行多客户端并发和长期压力测试。
- 手动长按进食与界面体验待本阶段人工验收；数值平衡按用户要求延期。
```

现有开发环境仍出现 WMI 权限、开发账号属性/Realms 认证、框架默认各向异性过滤和 `minecraft:end_of_frame` 告警；最终测试通过，没有 Mod Mixin 注入失败。测试专用未知/实验引用警告是预期覆盖，不出现在发布数据中。阻断项：无。

## 人工验收清单（机制）

在测试世界开启命令、使用生存模式，准备苹果/面包、蘑菇煲、南瓜派、金苹果。可在消耗一些 Hunger 后依次测试：

- [ ] 查询蘑菇煲与南瓜派 Profile，分别为 Meal / Restorative 和 Prepared / Invigorated，`benefit_available=true`。
- [ ] 正常吃完蘑菇煲，查询 `/buildupvitals recovery`：储备增加 3 HP，主要增益为 `buildup_vitals:restorative`，剩余时间从约 2400 tick 倒数；中途取消不授予。
- [ ] 再吃同类只刷新、不累加时长；吃南瓜派替换为 `buildup_vitals:invigorated`、约 1200 tick，不保留两个主要增益。
- [ ] 吃苹果/面包仍正常补充原版营养，不移除已有增益；金苹果保留原版效果，右上角没有新增 Buildup 药水图标。
- [ ] Restorative 期间储备逐步兑现，受击不中断，总治疗不超过储备；Invigorated 到期后查询显示 `none`，原有饱食消耗仍工作。
- [ ] 满血时增益持续倒计时；退出重进、跨维度、独立服务端重连后保留剩余状态，死亡重生后为空。
- [ ] 本轮仅确认上述机制。战斗节奏、10% 活动减耗是否合适、四档持续时间及料理收益感留待主要功能机制完成后评估。

Stage 3 到此停止，等待机制验收；未提前进入 Stage 4 的 Diet Memory / Variety，也未实现 Hydration Adapter 或 HUD。
