# Stage 4 Completion Report

日期：2026-10-05（Asia/Shanghai）。目标目录：`26.3`。阶段：Diet Memory & Dietary Variety。

用户已通过 Stage 3 验收；本轮继续遵循“主要功能和机制基本完成后再评价战斗节奏及微调数值”的要求。以下结果证明机制与边界，不代表平衡定稿。

## 完成内容

- 为服务端玩家记录最近十次完成的食物物品消费：Food ID、Quality、Categories、Variety Group 和主世界游戏 tick 时间戳。窗口即时滚动，不等待现实时间。
- 采用类别优先、食物组其次、Quality 最轻的奖励评分；多类别料理均分单餐贡献，不重复计餐；同组不同物品不会冒充多组。
- 单组饮食保持精确 1.0 基础倍率。重复系数最低 0.90，仅乘到额外奖励部分，不减少基础 Hunger、Saturation、Recovery 或增益时长。
- 多样性接入 Food Recovery 储备量（最高 +15%）、Meal Benefit 时长（最高 +10%，仍受原有 3600 tick 上限约束）、Well-fed 速度（目标最高 +7.5%，整数周期 75–80 tick）。Stable 与 Food 兑现周期不受 Variety 改写。
- 自然恢复每实际恢复 1 HP 仍消耗 6 Exhaustion，Restorative 仍只加速储备兑现，不增加储备总量。
- 新增独立持久化附件，死亡清空、活着替换和跨维度/读档/重连保留；不改变已有 Recovery、Meal Benefit 的保存格式或范围。
- 新增只读 `/buildupvitals diet [player]` 查询，显示历史、分项评分、重复系数与奖励倍率；恢复查询显示实际 Well-fed 周期。
- 增加牛排 Basic / protein、面包 Basic / grain 两份最小 Profile，以支持三组饮食验收，不赋予额外 Recovery 或 Benefit。

算法、完整参数、保存边界及说明见 [DIET_MEMORY.md](DIET_MEMORY.md)。

## 主要文件与原因

| 文件 | 用途 |
|---|---|
| `diet/DietEntry.java`、`DietMemory.java` | 有限、不可变饮食历史，保存进食时快照，缓存派生结果 |
| `diet/VarietyCalculator.java`、`VarietyResult.java`、`VarietyBalance.java` | 评分、重复系数、奖励与原型参数 |
| `diet/PlayerDiet.java`、`player/DietAttachments.java` | 服务器入口、死亡事件和有界 Codec 持久化 |
| `food/recovery/PlayerRecovery.java`、`RecoveryController.java` | 本次进食先记录再奖励，Well-fed 动态周期，防止极大 Recovery 值乘法溢出 |
| `food/benefit/MealBenefitState.java`、`PlayerMealBenefits.java` | 有界时长奖励，保留刷新/替换和现有总时长上限 |
| `command/DietCommand.java`、`RecoveryCommand.java`、`FoodProfileCommand.java` | 只读饮食历史与当前行为查询 |
| `BuildupVitals.java`、`food/profile/FoodProfile.java` | 注册入口和字段语义 |
| `src/main/resources/data/buildup_vitals/buildup_vitals/food_profiles/{bread,cooked_beef}.json` | 最小类别数据示例，保留原版营养 |
| `src/test/`、`src/gametest/` | 评分边界、消费路径、持久化与真实连接回归 |
| 当前机制文档、[README.md](../README.md)、本报告、[UPDATE_NOTES.md](../UPDATE_NOTES.md) | 行为、验收和更新说明 |

短 Java 路径相对 `src/main/java/com/davidblackcn/buildupvitals/`。修改仅在 `26.3`；开工时工作树干净。

## 兼容性与实现决策

保持 Minecraft 26.3、Java 25（本机 25.0.3）、Gradle 9.7.1、Loom 1.18.2、Loader 0.19.5、Fabric API 0.161.0+26.3，使用原生非混淆名称。开工核对 [Fabric 官方 26.3 属性](https://raw.githubusercontent.com/FabricMC/fabric-example-mod/26.3/gradle.properties)，本轮未升级依赖或更改构建配置。

- 沿用已经过运行时验证的 `FoodProperties.onConsume` 服务端完成消费入口和恢复 tick 入口，没有新增或修改 Mixin / Access Widener。
- 目标源码确认 `MinecraftServer.overworld()` 与 `LevelAccessor.getGameTime()`，跨维度不混用各维度时钟。时间戳不参与过期逻辑。
- 使用现有 Fabric Attachment API；目标 DataFixerUpper 10.0.21 源码确认有界 `Codec.listOf(min,max)` 和验证 API。新附件最多十条，重复类别和未知枚举由 Codec 拒绝。
- Score 不存盘，读档从条目重算；不可变历史缓存评分，正常 tick 只读取已算好的结果。重载 Profile 不重写历史。
- 多样性只提高奖励，基础倍率不低于 1.0。Well-fed 周期向上取整防止超出加速上限，实际最高约 +6.67%；每 HP 成本不变。
- Meal Benefit 沿用 3600 tick 总上限，Feast 基础已到上限时不继续延长。没有为了 Stage 4 改动旧附件格式。
- 沿用物品消费范围：原版蛋糕方块在 `CakeBlock.eat` 直接调用 `FoodData.eat(2,0.1F)`，不经过该入口，本阶段没有为蛋糕或第三方自定义方块进食新增记录入口。

## 验证命令与结果

在 `26.3` 设置 `JAVA_HOME=C:/Program Files/Java/jdk-25.0.3`、将 JDK bin 加入当前进程 PATH、清空本机 `DEBUG` 后执行：

```powershell
.\gradlew.bat compileJava --console=plain
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true --console=plain
```

初步编译成功；最终完整命令 **BUILD SUCCESSFUL**，包含必需的 Wrapper `build`。EULA 使用用户已授权的测试环境选择。

| 检查 | 实际结果 |
|---|---|
| JVM 测试 | **90 通过，0 失败/错误**：Parser 49、Snapshot 7、Recovery 17、Meal Benefit 9、Diet 8 |
| 服务端 GameTest | **20 通过**：Recovery 7、Meal Benefit 6、Diet 6、框架 sanity 1 |
| 十次窗口 | 正确移出最旧记录、同 tick 按进食顺序保存、不可变快照通过 |
| 重复规则 | 同 Food ID、不同 ID 同组、相同 ID 改组均有覆盖；长期单组精确回到基础 |
| 类别与质量 | 多种水果有小幅组奖励，低于跨类别组合；多类别 Meal 贡献完整且只占一餐；类别优先于 Quality |
| 下限与上限 | 1000 个固定种子的随机窗口覆盖最终倍率、重复系数和 Well-fed 周期范围；无基础惩罚 |
| 原版营养解耦 | 不同历史下同食物 Hunger / Saturation 相等；连续牛排仍提供原营养；创造/旁观不记录 |
| Gameplay 奖励 | 本次进食更新窗口后仅授予一次额外储备与增益时长；无瞬时治疗，时长不突破旧上限 |
| 自然恢复 | 仅 Well-fed 加速，Stable 与进度保留有单元覆盖；实际恢复 1 HP 后仍产生 6 Exhaustion |
| 保存与非法状态 | 全部条目及时间戳经玩家 NBT 精确往返；派生评分一致；超长、重复类别、未知 Quality 的保存数据被拒绝 |
| 生命周期 | 活着替换保留、死亡不复制、玩家间隔离通过；真实玩家死亡立即清空 |
| 真实单人连接 | 恢复/增益回归、饮食记录、下界往返、死亡重生、空/满历史查询、存档关闭重开通过 |
| 真实 DedicatedServer 连接 | localhost 连接及相同场景通过，断线重连后保留十条历史及奖励 |
| 资源与发布包 | 全部源码 JSON 可解析；发布 JAR 包含 6 份生产 Profile，没有测试类/测试 Mod/错误引用夹具 |
| 文档与 Git | 本地文档链接、更新说明格式、最终差异空白检查通过 |

真实连接查询记录的混合示例（牛排、面包、蘑菇煲、苹果、南瓜派各两次）：score `0.92`，Food 倍率约 `1.13647`，Benefit 倍率约 `1.09098`，Well-fed 周期 `75` tick。死亡后为空窗口、倍率全为 `1.0`、Well-fed 回到 `80` tick。这是功能观测，未据此宣称战斗平衡已完成。

首轮服务端失败为新增测试错误地期望从 Hunger 0 吃牛排获得 12.8 饱和点；目标源码实际将 Saturation 限制到当前 Hunger，此时只能达到 8。已将测试起点改为 Hunger 10，验证正常增加到 Hunger 18、Saturation 12.8，保留营养断言，不修改原版或生产逻辑来迎合测试。修正后完整构建和客户端测试重新通过。

报告位置：`build/reports/tests/test/index.html`。本轮日志保留在忽略目录 `run/stage4-build.log`、`run/stage4-server-gametest.log`、`run/stage4-client-gametest.log`，原始日志在 `build/run/gameTest/` 与 `build/run/clientGameTest/`。自动测试使用独立测试世界，不操作日常开发世界。

## 自检

```text
Code Review: PASS WITH RISKS

Scope:
- Reviewed files: 上述生产代码、资源、测试及当前文档的全部差异
- Target version/directory: Minecraft 26.3 / 26.3

Findings:
- BLOCKER: None
- MAJOR: None
- MINOR: None

Fixes Applied During Review:
- 食物基础恢复先按储备上限裁剪再乘奖励，避免有限极大值溢出。
- 保留旧增益上限，更新当前文档的基础值与奖励含义。
- 面包成为明确 Profile 后，将 fallback 回归示例改为未匹配的烤马铃薯。
- 修正营养测试的原版饱和上限前提，并验证空/满饮食历史查询。

Verification:
- Wrapper build / 90 JVM tests / 20 server GameTests: PASS
- Connected integrated + dedicated lifecycle tests: PASS
- Runtime Mixin / server-client boundary / existing persistence unchanged: PASS
- JSON / release JAR / documentation links / diff checks: PASS
- Update Notes: PASS

Residual Risks:
- 第三方自定义进食入口、同时修改恢复的 Mod 和多客户端并发未验证。
- 原版蛋糕与第三方方块进食不在当前物品完成消费入口范围内。
- 手动长按进食与三组饮食切换待人工机制验收；数值平衡按用户要求延期。
```

测试仍出现既有的 WMI 权限、开发账号/Realms 认证、框架默认各向异性过滤及 `minecraft:end_of_frame` 告警。未知/Steady 引用警告来自隔离测试数据，属于预期覆盖。最终无 Mod Mixin 注入错误或未解决的测试失败。阻断项：无。

## 人工验收清单

使用测试世界、生存模式并开启命令。每次有 Hunger 空间后正常吃完；可用 `/buildupvitals diet` 与 `/buildupvitals recovery` 观察状态。

- [ ] A：连续吃 10 次牛排。Hunger / Saturation 不因重复下降；历史只有牛排，最终倍率为 1.0。
- [ ] B：轮换苹果、西瓜片、甜浆果共 10 次。记录都是 fruit，但组不同；有少量奖励，达不到跨类别高分。
- [ ] C：牛排、面包、蘑菇煲、苹果、南瓜派各两次。跨类别得分高于 B，食物恢复和增益时长获得少量额外收益。
- [ ] 从 C 连续改吃牛排，观察最旧记录逐条移出，第十次回到 A；不需要等待现实时间。
- [ ] 中途取消进食不记录；一次吃完只增加一条；原版营养及药水效果照常工作。
- [ ] 蘑菇煲的储备逐渐兑现而非瞬时治疗；自然恢复没有原版高速爆发，仍消耗饱食资源。
- [ ] 满血不清空饮食历史；退出重进、跨维度和服务端重连保留，死亡后清空。
- [ ] 本阶段验收上述机制，三组间的收益感和战斗节奏不作为本轮数值定稿要求。

Stage 4 到此停止，等待机制验收。没有提前进入 Stage 5 的 Tooltip / 客户端反馈，也未新增 Hydration 或通用 RPG 奖励。
