# Stage 6 Completion Report

日期：2026-10-05（Asia/Shanghai）。目标目录：`26.3`。阶段：Thirst Was Taken 2 Optional Integration。

Stage 5 已由用户验收。本轮固化“后续 Tooltip 采用简洁 icon + 短文字”的方向，只推进口渴适配；没有重做 Recovery / Buff 图标，也没有开始全面数值平衡。

## 完成内容

- 对接 **Thirst Was Taken 2 Fabric 1.6.2+26.3**，真实 ID `thirstwastaken2`。可选编译/开发依赖与 `suggests` 元数据就绪，未安装时不加载其 API，其他版本自动关闭本适配。
- 独立 HydrationAdapter 从服务端最终 Profile 构建不可变索引，通过 TWT2 的物品值解析入口提供 Hydration，保留其消费、Drink Event、玩家状态、Quenched、纯度和 HUD。
- 优先级明确为 **TWT2 黑名单/配置 → TWT2 drinks 数据包 → Buildup Profile → TWT2 通用规则**。不重复叠加；显式零值有效。
- 原始非负整数分别裁剪到 0–20 后交给 TWT2，遵循其最终 Quenched 上限及 Thirst 溢出转入 Quenched 的规则，避免极大整数溢出。
- 服务端启动/成功重载刷新，Profile 重载失败保留原结果，移除及关服清理；元数据 v2 同步 Hydration 与服务端能力，纯远程客户端在更新/断线时清理 TWT2 缓存。
- 本地服务器存在时拒绝客户端显示数据覆盖游戏索引，适应 TWT2 在同进程客户端/服务器间共享 API 缓存的结构。
- 补水提示复用 TWT2 与 AppleSkin 的现有水滴图标，不追加重复的文字或新状态条。
- 已将用户的后续 UI 方向写入版本目录的 PLAN、DESIGN_PRINCIPLES 及客户端说明。

行为、示例、优先级、外部恢复限制和开发命令见 [HYDRATION.md](HYDRATION.md)。

## 主要文件

| 文件 | 用途 |
|---|---|
| `hydration/HydrationAdapter.java` | 不可变索引、数值边界、成功重载、服务端权威和连接清理 |
| `compat/thirst/ThirstCompatibility.java`、`ThirstMixinPlugin.java` | 无第三方 API 类型的版本检测及可选注入门控 |
| `compat/thirst/ThirstBridge.java` | 只在支持版本已安装后调用公开缓存清理 API |
| `compat/thirst/mixin/ThirstValuesMixin.java`、`buildup_vitals.thirst.mixins.json` | 唯一值解析调用点的后备值桥接，不接管结算 |
| `network/TooltipProfile.java`、`FoodProfilesPayload.java`、`FoodProfileSync.java` | v2 协议、Hydration 校验及能力同步 |
| `src/client/.../ClientFoodProfiles.java`、`BuildupVitals.java` | 客户端接收/断线与 common 注册入口 |
| `build.gradle`、`gradle.properties`、`fabric.mod.json` | 固定可选依赖、受限仓库及 suggests 元数据 |
| `src/test/.../HydrationAdapterTest.java`、`FoodProfilesPayloadTest.java` | 有界数值及协议回归 |
| `src/gametest/.../HydrationGameTests.java`、`HydrationClientGameTest.java`、`ThirstTestSupport.java`、`ThirstClientScenario.java`、测试元数据 | 已安装/未安装场景、实际消费、原生优先级、水纯度及重载/连接测试 |
| `src/gametest/.../RecoveryClientGameTest.java` | 用非满口渴、零 Quenched 的测试状态隔离外部独立治疗，不改变生产行为或原断言 |
| 当前机制文档、README、版本 PLAN / DESIGN_PRINCIPLES、本报告、UPDATE_NOTES | 安装、显示限制、验收及后续 UI 约束 |

短生产 Java 路径相对 `src/main/java/com/davidblackcn/buildupvitals/`。修改仅在 `26.3`，开工时版本 Git 工作树干净；根目录共享文件及历史阶段报告未修改。

## 依赖与源码调查

Minecraft 26.3、Java 25（本机 25.0.3）、Gradle 9.7.1、Loom 1.18.2、Loader 0.19.5、Fabric API 0.161.0+26.3 保持不变，使用原生非混淆名称。

- 从 [作者 Modrinth 发布版 ZlAbvVVI](https://modrinth.com/mod/thirst-was-taken-2/version/ZlAbvVVI) 下载 `ThirstWasTaken2-1.6.2+26.3.jar`，与 API 返回的 SHA-512 校验一致；本地 SHA-256 为 `60f999c7828971a190e843f32487819d70115a77520911be9e6c70d029f79ee6`。
- 对应官方源码提交：[2b3598fbf6c543fda85a8a03acf4f698309ad104，release: 1.6.2](https://github.com/n1ght3r/ThirstWasTaken2/tree/2b3598fbf6c543fda85a8a03acf4f698309ad104)。核对发布 JAR 的 `fabric.mod.json`、`ThirstApi` API_VERSION 2、Drink Event、drinks 数据包、Quenched、消费和纯度路径。
- 按上游推荐使用 Modrinth Maven。26.3 当前 Loom 配置采用 `compileOnly`，非旧映射版本的 `modCompileOnly`。可选开发使用已核对 Loom 1.18.2 的 `localRuntime`，测试使用 `gametestRuntimeOnly`。
- 上游许可证为 GPL-3.0-only，未发现 API 链接例外。本次不复制上游实现/资源，不嵌入依赖；调查材料与原始 JAR 只在忽略目录。许可证及后续组合分发边界见 HYDRATION；本轮没有改变原有 MIT 文件或发布第三方组合产物。

公开 API 没有动态物品值注册方法。静态 drinks 包无法随任意 Profile/Tag 自动重载；进食后另行 `drink` 也不能可靠保留原生取消和纯度路径。因此本轮选择“向 TWT2 值查询提供 Profile 数据”，并将必须接触内部解析的位置收敛到一处、限定到已验证发布版。

Mixin 目标及 `javap` 确认证据：

```text
static ThirstApi.resolve(Lnet/minecraft/world/item/Item;)[I
  唯一 INVOKESTATIC DataPackDrinks.get(Lnet/minecraft/world/item/Item;)[I
```

`ModifyExpressionValue` 的静态处理函数接收原返回值与原方法 Item 参数；原值非 null 完整保留，仅 null 时提供 Profile 值。`require=1 / allow=1`；无 ordinal 猜测、局部变量捕获、反射或 Access Widener。版本不匹配时不应用此 Mixin。

## 自动测试

在 Java 25 环境、目标目录下，清空本机 `DEBUG` 后实际执行：

```powershell
.\gradlew.bat compileJava compileClientJava --console=plain
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true --console=plain
.\gradlew.bat build runClientGameTest -PwithThirst=true -PacceptMinecraftEula=true --console=plain
.\gradlew.bat build runClientGameTest -PwithThirst=true -PacceptMinecraftEula=true '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar' --console=plain
.\gradlew.bat build generatePomFileForMavenJavaPublication -PwithThirst=true --console=plain
```

最终结果均为 **PASS / BUILD SUCCESSFUL**。最后一条在开发运行时改为 `localRuntime` 后执行，并检查生成 POM 不含 TWT2；仅生成本地元数据，没有发布。EULA 沿用用户已授权的自动测试设置。

| 检查 | 实际结果 |
|---|---|
| JVM | **95 通过，0 失败/错误/跳过**：既有 93，加 Hydration 边界和 v2 能力/数值协议各 1 |
| 无 TWT2 服务端 | **21 通过**：既有 20，加适配关闭检查；已安装口渴消费场景明确 NOT RUN |
| 有 TWT2 服务端（含 AppleSkin 组合） | **21 通过**，新增检查进入真实 TWT2 消费场景 |
| 可选性 | 没有 TWT2 时 common/client/server 启动、旧机制及 Tooltip 回归通过；无其类缺失异常 |
| 单次消费 | 蘑菇煲、苹果的 Profile 恢复准确一次，原版营养、容器行为继续执行 |
| 配置优先级 | TWT2 显式配置值、黑名单、配置零值均阻止低优先级 Profile；测试结束恢复原配置对象，不落盘 |
| 数据包优先级 | 同物品 drinks `1/4` 覆盖 Profile `2/1`；drinks `0/0` 阻止补水；删除 drinks 后回到 Profile |
| 原生事件 | TWT2 Drink Event 取消后没有任何补发值 |
| Quenched | 验证高于食物 Thirst 的独立输入、20 上限及 Thirst 溢出转入 Quenched |
| 极大值 | Profile `Integer.MAX_VALUE/Integer.MAX_VALUE` 经网络保留原数据、进入 TWT2 前变为 `20/20`，实际消费无溢出 |
| 纯度 | Pure 水保持 Pure、恢复 `6/8`；盐水保留盐水身份且不补水 |
| 重载 | Profile `6/8 → 2/1` 生效并清缓存；故意失败保留 `2/1`；删除后无残留 |
| 客户端能力 | 无本地服务器时直接验证纯客户端索引接收、服务端关闭能力、断线清缓存；有服务器时客户端显示数据不能覆盖索引 |
| 发布包/元数据 | 6 份原生产 Profile 保持不变；无 TWT2/AppleSkin 类和资源、无测试 Mod/类；suggests 无硬前置，POM 无 TWT2 |
| 文档与 Git | JSON、版本门控一致性、本地链接、完整差异与 Update Notes 追加格式检查通过 |

## 实机/运行验证

三组合均使用真实图形客户端。安装 TWT2 的两组合都运行 integrated world 与 localhost DedicatedServer，覆盖进食、原生优先级、重载、失败回滚、删除、断线重连。原有 Recovery / Meal Benefit / Diet / Tooltip 的客户端测试也回归通过。

Fabric 的 DedicatedServer 测试通过 TCP 连接，但运行在测试客户端所在 JVM 中，不能等同于两个独立进程。为覆盖不同路径，另在没有服务器运行时测试了纯客户端 Hydration 索引接收/清理，并由网络 Codec 测试及连接测试验证元数据传输。不同进程及多客户端并发仍列为剩余验证项。

实际检查了有/无 AppleSkin 的补水示例截图，以及 TWT2 + AppleSkin 的中文普通/高级提示：临时 Profile 的 `6/8` 正确绘制为三枚实心水滴、四枚 Quenched 图标，Hunger/Saturation 图示与 Buildup 品质信息各自保留，没有叠画或截断。TWT2 单独安装时没有水滴 Tooltip，这是上游该版本需要 AppleSkin 的既定显示条件，口渴结算测试照常通过。

本轮修正的测试/构建问题：

- 初次纯水断言误写为 Quenched 11，源码与实际均为 8（Pure 按基础 100%），已修正为 `11/8` 后重跑。
- 原和平难度计时回归被 TWT2 的满口渴 Quenched 独立治疗干扰，等不到“恰好 11 HP”。在每个测量段准备非满口渴、零 Quenched 的玩家状态，保留原恢复断言，未关闭或改动生产治疗逻辑。
- 自检将开发运行时从普通 `runtimeOnly` 改为 `localRuntime`，避免启用开发开关时生成的生产 POM 携带 TWT2 运行依赖；构建和 POM 检查再次通过。

本地证据均位于忽略目录：`run/stage6-*-build.log`、`run/stage6-*-server.log`、`run/stage6-*-client.log`、`run/stage6-visuals/`；JVM 报告在 `build/reports/tests/test/index.html`。未将日志、第三方源码/JAR 或测试世界加入提交。

## 已知问题与自检

- 当前仅验证并启用 TWT2 `1.6.2+26.3`，更新该 Mod 需要重新核对注入和运行矩阵；其它版本保留 TWT2 自身行为但不接收 Buildup Hydration。
- TWT2 默认配置覆盖许多原版食物，因此看到其默认值是优先级的预期结果。客户端 TWT2 本地配置不一致也会影响其 Tooltip；Buildup 不伪称能够覆盖上游所有配置同步。
- TWT2 自带 Quenched 额外回血、脱水限制与 Buildup Recovery Controller 尚未统一协调；该外部行为保留，已明确记录到后续整合评估。不会把当前组合的恢复节奏声明为已平衡。
- 水滴 Tooltip 需要 TWT2 + AppleSkin；AppleSkin 本身的生命恢复预测仍未适配 Buildup。
- 未实际运行不支持的 TWT2 版本、独立双进程/多客户端并发、所有 GUI 比例及第三方自动饮用容器；徒手饮水、净化流程与完整水容器链路未逐项人工验证。

```text
Code Review: PASS WITH RISKS

Scope:
- Reviewed files: 本轮生产代码、资源、依赖/构建、测试及文档全部差异
- Target version/directory: Minecraft 26.3 / 26.3

Findings:
- BLOCKER: None
- MAJOR: None
- MINOR: None

Fixes Applied During Review:
- 使用版本门控、精确注入计数与独立 API bridge，避免可选类提前加载。
- Hydration 先裁剪再交给 TWT2，保留显式零与原生事件取消语义。
- 补测纯客户端索引与客户端不能覆盖本地服务器的边界。
- 修正纯水预期和外部独立治疗影响下的恢复测试前提。
- 开发依赖改为 localRuntime，并核对生产 POM/JAR 隔离。

Verification:
- Wrapper build / 95 JVM tests / 21 server GameTests: PASS
- Buildup only / + TWT2 / + TWT2 + AppleSkin: PASS
- Integrated + same-process DedicatedServer TCP tests: PASS
- Mixin runtime / hydration consumption / reload / cache lifecycle: PASS
- Real tooltip screenshots / JSON / JAR / POM / documentation / diff: PASS
- Update Notes: PASS

Residual Risks:
- 固定第三方版本；升级需重新验证。
- 外部恢复行为、Tooltip 配置差异和 AppleSkin 生命预测按上述边界保留。
- 独立进程、多客户端、其他自动饮用集成和完整饮水操作链未全面验证。
- 不进行数值平衡定稿；玩家可读性和常用整合环境仍需人工验收。
```

既有 WMI 权限、开发账号/Realms、各向异性过滤及 `minecraft:end_of_frame` 告警仍出现；AppleSkin 无 JEI 时的可选目标警告保留。隔离测试 Profile 的未知/实验增益警告为预期。最终没有 Buildup Mixin 注入错误或未解决的测试失败。阻断项：无。

## 未进入的下一阶段内容

未进入 Stage 7 的全面原版食物平衡包或数值微调；没有改变现有六份生产 Profile、Recovery 参数、增益时长、Variety 算法或玩家持久化格式。没有引入新的口渴条、水纯度系统或料理 Buff 图标。

## 人工验收清单

- [ ] 不安装 TWT2：进入单人世界和服务器，原有机制与 Tooltip 正常，无口渴依赖错误。
- [ ] 安装 TWT2 Fabric `1.6.2+26.3`：两端加载，查询实际 Thirst/Quenched；默认食物以其配置优先，不误判为 Profile 无效。
- [ ] 在测试配置中移除苹果/蘑菇煲相应 `foods` / `drinks` 条目后重启：从 `5/0` 吃苹果到 `7/0`，吃蘑菇煲到 `9/2`；每次只加一次。
- [ ] 为无高优先级配置的物品写入 Profile Hydration，再 `/reload` 改值；客户端和实际消费更新，删除定义不残留旧值。
- [ ] 为同物品加入 TWT2 drinks 值，确认覆盖而非累加；设为 `0/0` 后不补水。
- [ ] 从 `19/0` 摄入 `4/2` 后为 `20/5`；Quenched 不突破最终 Thirst，水果、汤与原生饮品保留不同补水定位。
- [ ] 纯水、盐水、徒手饮水和净化照常工作；重点补验未自动覆盖的原生日常饮水操作。
- [ ] 同装 AppleSkin 后查看水滴与营养图示无重复；关闭或不装 AppleSkin 时确认口渴机制继续正常。
- [ ] 检查常用 GUI 比例下的中英文提示；后续爱心与 Buff 图标优化已记录，本阶段不以它们作为未完成项。
- [ ] 本轮验收兼容机制；Quenched 与其他恢复来源的最终战斗节奏在后续统一评估。

## 建议下一步

Stage 6 到此停止，等待人工验收，再决定进入 Stage 7。
