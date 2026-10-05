# Stage 2 Completion Report

日期：2026-10-05（Asia/Shanghai）。目标目录：`26.3`。阶段：Recovery Core。

## 完成内容

- 食物成功完成消费后，将 Profile `recovery.health` 加入服务器权威的恢复储备。
- 储备每 50 tick 兑现最多 1 HP；受击不暂停、不重置。满血保留储备，上限 20 HP；只扣实际获得的治疗量。
- 统一控制器优先使用 Food Recovery，储备耗尽后才开始自然恢复周期。Stable 为 120 tick / HP，Well-fed 为 80 tick / HP。
- 原版两条高饱食回血分支与和平难度额外回血由控制器协调，保留食物营养、活动消耗、和平难度补充食物和饥饿伤害。
- 使用 Fabric Data Attachment 持久化储备、进度和模式；死亡清零，活着切维度、保存重进及断线重连保留。
- 新增只读 `/buildupvitals recovery [player]` 命令，延续 Game Masters 权限要求。
- 新增纯逻辑、服务端及连接客户端测试；CI 构建自动执行服务端测试并保留测试日志。

全部参数、生命周期和边界见 [RECOVERY.md](RECOVERY.md)。

## 主要文件

| 文件 | 原因 |
|---|---|
| `src/main/java/com/davidblackcn/buildupvitals/food/recovery/` | 集中平衡参数、不可变状态、纯控制器及玩家适配 |
| `src/main/java/com/davidblackcn/buildupvitals/player/RecoveryAttachments.java` | 首次新增恢复状态持久化 |
| `src/main/java/com/davidblackcn/buildupvitals/mixin/`、`src/main/resources/buildup_vitals.mixins.json` | 三处必要的进食/自然恢复注入 |
| `command/RecoveryCommand.java`、`command/FoodProfileCommand.java` | 查询恢复状态并更新 Profile 输出说明 |
| `BuildupVitals.java`、`food/profile/FoodProfile.java` | 注册入口与数据语义说明 |
| `src/test/.../RecoveryControllerTest.java` | 恢复边界和调度逻辑测试 |
| `src/gametest/`、`build.gradle` | 隔离测试 Mod、服务端测试及真实连接玩家测试 |
| `.github/workflows/build.yml` | 上传 JVM 报告及服务端 GameTest 日志 |
| [README.md](../README.md)、[FOOD_PROFILES.md](FOOD_PROFILES.md)、[RECOVERY.md](RECOVERY.md)、[UPDATE_NOTES.md](../UPDATE_NOTES.md) | 用户行为、数据包说明、验收及更新记录 |

表中的短 Java 路径均相对 `src/main/java/com/davidblackcn/buildupvitals/`。

## 兼容性决策

保持 Minecraft 26.3、Java 25（本机 25.0.3）、Gradle 9.7.1、Loom 1.18.2、Loader 0.19.5、Fabric API 0.161.0+26.3；使用原生非混淆名称。未升级工具链或新增生产依赖；MixinExtras 0.5.5 来自现有 Loader 环境。

API 证据以本机目标 Minecraft sources JAR 和已解析 Fabric 模块 sources JAR 为准。开工时核对了 [Fabric 官方 26.3 示例配置](https://github.com/FabricMC/fabric-example-mod/blob/26.3/gradle.properties)；测试配置参考 [Fabric 自动测试文档](https://docs.fabricmc.net/develop/automatic-testing)，并以当前编译、启动结果校验。

Fabric 当前没有完成食物消费的公开事件，所以在 `FoodProperties.onConsume` 尾部注入，既能保留原版营养，也能在碗等余物替换前识别原食物。只对 `ServerPlayer` 生效。

原版自然回血仅在目标方法内拦截，未重写整个 `FoodData.tick`，未注入通用 `heal` 或伤害计算。和平难度保留其食物自动补充，而健康恢复使用统一周期。自然恢复仍产生每 HP 6 Exhaustion；食物储备无此额外消耗。

附件不启用 `copyOnDeath`，并在实际死亡事件立即移除。没有新增客户端状态同步，实际生命变化使用原版同步；没有变更已有玩家数据格式或旧 Food Profile schema。

## 自动测试

在目标目录、JDK 25 环境实际执行：

- `.\gradlew.bat compileJava test --console=plain`：PASS。
- `.\gradlew.bat build --console=plain`：PASS。
- `.\gradlew.bat runClientGameTest -PacceptMinecraftEula=true --console=plain`：PASS。
- 最终 `.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true --console=plain`：PASS，退出码 0。
- JVM 测试：**69 项，0 失败、0 错误**，其中既有数据层 56 项、恢复逻辑 13 项。
- 服务端 GameTest：**8 项必需测试通过**，其中本 Mod 7 项，框架自带 1 项。
- 资源 JSON、发布 JAR 内容、文档链接及 `git diff --check`：PASS。

JVM 报告：`build/reports/tests/test/index.html`。服务端日志：`build/run/gameTest/logs/latest.log`。连接玩家测试日志：`build/run/clientGameTest/logs/latest.log`。本轮日志另存到忽略目录 `run/stage2-server-gametest.log` 和 `run/stage2-client-gametest.log`，不进入提交。

发布 JAR `build/libs/buildup_vitals-0.1.0-dev.jar` 含三个必要 Mixin，不含测试 Mod、GameTest 类或 JUnit。

## 实机/运行验证

| 场景 | 结果 |
|---|---|
| 完整食物消费路径 | 蘑菇煲只添加一次 3 HP 储备，保留营养和碗；不瞬间回血 |
| 受击后的周期 | 50 tick 周期不中断，实际伤害正常，储备继续兑现 |
| 自然恢复 | 无原版 10 tick 爆发；80 / 120 tick 档位及低饱和停止正确 |
| 治疗独立性 | 直接 `heal`、治疗药水正常；金苹果再生及伤害吸收效果保留 |
| 原版资源行为 | 面包 fallback 正常提供营养；饥饿伤害及 Exhaustion 优先扣饱和保留 |
| 满血、上限、小数 | 储备不在满血时消耗或预充，累计上限正确；尾数及部分有效治疗有单元覆盖 |
| NBT 保存 | 储备与部分周期进度序列化/反序列化一致 |
| 玩家生命周期 | Fabric 活着重生事件保留状态，死亡重生不复制 |
| 真实单人玩家 | 正常 tick 恢复、受击、和平难度、下界往返、死亡重生、查询通过 |
| 单人世界关闭重开 | 保存后重新打开同一世界，储备保留 |
| 独立服务端 + 客户端 | 测试框架启动真正的 DedicatedServer，经 localhost 连接，执行相同恢复和生命周期场景通过 |
| 断线重连 | DedicatedServer 继续运行，客户端断开并重新连接后储备保留 |

客户端测试通过 Fabric 自动测试 API 运行真实游戏及连接；没有声称完成手动战斗体验。GameTest 中直接调用完整 `finishUsingItem` 消费路径；键鼠长按进食的体验仍列入人工验收。

首次测试失败已处理：模拟玩家缺少客户端加载确认造成无敌；附件复制需要 `AFTER_RESPAWN` 而非仅 `restoreFrom`；真实切维度之后须等待加载确认再测试死亡；查询需单目标选择器。均已修正测试夹具并重跑，未通过关闭断言掩盖失败。测试代码中 26.3 方法名及受检异常的编译错误也已修复；最终无编译或测试失败。

## 自检

```text
Code Review: PASS WITH RISKS

Scope:
- Reviewed files: 本报告列出的所有生产代码、Mixin、测试、构建、资源及文档改动
- Target version/directory: Minecraft 26.3 / 26.3

Findings:
- BLOCKER: None
- MAJOR: None
- MINOR: None

Fixes Applied During Review:
- 替换废弃的 WrapWithCondition 导入为当前 v2。
- 查询测试增加成功返回值断言，消除只运行命令但未检查结果的缺口。
- 更新 Stage 1 遗留的数据只读说明及发布内容检查。

Verification:
- Wrapper build / JVM tests / server GameTest: PASS
- Connected client + integrated/dedicated lifecycle tests: PASS
- Mixin runtime application and common/client boundaries: PASS
- Source JSON / release JAR / documentation / diff checks: PASS
- Update Notes: PASS

Residual Risks:
- 未验证同时接入其他修改自然恢复/食物消费的 Mod。
- 未进行多客户端并发、长时间压力测试及人工战斗体验。
- CI 配置已静态检查，未在远程 GitHub Actions 实际运行。
```

## 已知问题

- 测试环境存在 WMI 权限、开发账号属性/Realms 认证、框架默认各向异性过滤选项及既有 `minecraft:end_of_frame` post-effect 告警；最终游戏与断言通过，没有 Mod Mixin 注入失败。未将开发认证失败当作产品功能成功。
- 独立测试服务器使用 Fabric 测试框架默认的本地离线连接方式；生产服务器认证配置未改动。
- 平衡值仍是原型；尤其和平难度恢复节奏、Stable 阈值及 20 HP 储备上限需要人工体验确认。
- 阻断项：无。

## 未进入的下一阶段内容

未实现 Stage 3 的 Quality Gameplay、Meal Benefit 状态或其增益；未实现 Diet Memory、Variety、Hydration Adapter、Tooltip/HUD 或更多平衡食谱。

## 人工验收清单

先用测试世界和生存模式，开启命令，准备蘑菇煲、面包、高饱和食物、治疗药水与金苹果。详细机制见 [RECOVERY.md](RECOVERY.md)。

- [ ] `/buildupvitals food profile minecraft:mushroom_stew` 显示 Recovery 3；`/buildupvitals recovery` 显示自身状态。
- [ ] 消耗一些 Hunger 并受伤，正常吃完蘑菇煲；没有瞬间回血，约每 2.5 秒恢复半颗心，中途取消进食不增加储备。
- [ ] 储备兑现期间继续被怪攻击，恢复周期不因受击暂停。
- [ ] 无储备时吃饱，确认没有原版高速回血；Stable / Well-fed 的慢恢复能被自然感知。
- [ ] 使用治疗药水、金苹果及再生信标，确认各自原有效果。
- [ ] 满血后储备保留，反复进食累计不超过 20 HP；再次受伤后重新开始周期。
- [ ] 切维度、退出重进、独立服务端重连后查询储备；死亡后清零。
- [ ] 单人和独立服务端分别体验连续战斗，确认料理有价值且没有取代明确治疗手段。

## 建议下一步

等待人工验收 Stage 2，重点评价战斗节奏。根据 [PLAN.md](../PLAN.md) 的 Stage Gate，未自动进入 Stage 3。
