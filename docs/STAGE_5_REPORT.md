# Stage 5 Completion Report

日期：2026-10-05（Asia/Shanghai）。目标目录：`26.3`。阶段：Client Feedback & Tooltips。

用户已通过 Stage 4 验收；本轮只实现客户端反馈，不调整战斗节奏或平衡参数。

## 完成内容

- 普通食物 Tooltip 增加四档品质、基础逐渐恢复量、可用料理增益名称及单行饮食类别；缺失信息省略，fallback 显示基础食物。
- F3+H 高级模式追加 Profile、原始 Recovery、Variety Group、Benefit 引用及可用性；普通模式不显示内部调试字段。
- 新增 19 个中英文翻译键，保留既有水果 Tag 翻译。恢复文本标明“基础”，不把它描述为当前玩家确定获得的治疗。
- 服务端登录和成功 `/reload` 后发送完整、不可变显示快照；重载整体失败保留旧快照，删除定义不残留旧条目，断线/新连接清空缓存。
- 未收到或无法同步显示数据时不推断本地规则；已同步但未匹配的食物采用服务端已知的 fallback。
- 验证未安装 AppleSkin，以及安装 Fabric mc26.3-3.0.10 后的食物提示和真实连接；其营养图示保留一份，Buildup 信息也只添加一次。
- 完成 Recovery Reserve 心形预览调研。本阶段按 PLAN 允许的范围不发布预览，不新增 HUD 条。

完整行为及兼容限制见 [CLIENT_FEEDBACK.md](CLIENT_FEEDBACK.md)。

## 主要文件

| 文件 | 修改原因 |
|---|---|
| `network/TooltipProfile.java` | 定义显示所需数据，不传玩家状态或服务器文件路径 |
| `network/FoodProfilesPayload.java` | 版本化、单向、有界的完整快照协议和 Codec |
| `network/FoodProfileSync.java` | 登录与成功重载发送，支持超限时明确不可用状态 |
| `food/profile/ProfileSnapshot.java`、`BuildupVitals.java` | 只读暴露已解析物品索引及注册同步入口 |
| `src/client/.../ClientFoodProfiles.java` | 渲染线程缓存与连接生命周期清理 |
| `src/client/.../FoodTooltips.java`、`BuildupVitalsClient.java` | Fabric Tooltip 回调、普通/高级格式与客户端注册 |
| `assets/buildup_vitals/lang/{en_us,zh_cn}.json` | 中英文玩家提示与调试文本 |
| `src/test/.../FoodProfilesPayloadTest.java` | 协议往返、非法数据与长度边界测试 |
| `src/gametest/.../TooltipClientGameTest.java`、`TooltipReloadFailure.java`、测试 Mod 元数据 | 单人/独立服连接、重载回滚、断线清理和截图验证；失败注入仅存在于测试 Mod |
| `build.gradle`、`.gitignore` | 可选本地 AppleSkin 测试运行时和 JVM 初始化产生的日志忽略规则 |
| README、当前机制文档、本报告与 UPDATE_NOTES | 同步当前能力、复现命令、兼容边界和验收入口 |

短生产 Java 路径相对 `src/main/java/com/davidblackcn/buildupvitals/`。修改全部位于 `26.3`，开工时工作树干净。未改写历史阶段报告或根目录共享文档。

## 实现与兼容性决策

保持 Minecraft 26.3、Java 25（本机 25.0.3）、Gradle 9.7.1、Loom 1.18.2、Loader 0.19.5、Fabric API 0.161.0+26.3。映射仍为原生非混淆名称，没有依赖升级或兼容范围放宽。

- 查阅目标已解析源码：Fabric Item API `14.7.0+cf6bc2db5d` 的 `ItemTooltipCallback`，Networking `6.3.8+fcdff87f5d` 的 large-payload、连接事件及客户端主线程接收语义，Lifecycle `4.1.9+ffef5f675d` 的成功重载事件。
- 服务端同步最终 Item/Tag 匹配结果，客户端不重新加载数据包；快照通道为 `buildup_vitals:food_tooltips_v1`。上限为 65,536 条及 16 MiB，类别/枚举/数值/重复条目有校验。
- 未新增 Mixin、Access Widener 或反射。common 不引用 client 类；专服无需加载客户端 Tooltip 实现。
- Tooltip 仅读取内存数据；Recovery 受既有 20 HP 上限裁剪，原始值保留在高级提示。没有修改食物 Profile、恢复参数、增益时长、Variety 算法或保存格式。
- AppleSkin 来自 [作者 Maven 的 mc26.3-3.0.10 发布包及源码](https://maven.ryanliptak.com/squeek/appleskin/appleskin-fabric/mc26.3-3.0.10/)。仅通过 `gametestRuntimeOnly` 本地文件参数参与兼容测试，不作为生产依赖，不打入发布 JAR。
- 心形调研基于 26.3 `Hud.extractPlayerHealth/extractHearts` 与 AppleSkin `FoodHelper.getEstimatedHealthIncrement`。储备预览需要额外玩家状态同步、多排/吸收心适配及预测优先级；本轮不具备这些实现，故不绘制推测结果。

## 自动测试

在目标目录设置当前进程 `JAVA_HOME=C:/Program Files/Java/jdk-25.0.3`、把 JDK bin 加入 PATH、清空本机 `DEBUG` 后执行：

```powershell
.\gradlew.bat compileJava compileClientJava --console=plain
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true --console=plain
.\gradlew.bat runClientGameTest -PacceptMinecraftEula=true '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar' --console=plain
.\gradlew.bat build --console=plain
```

上述最终命令均 **PASS / BUILD SUCCESSFUL**。最后一次 `build` 在恢复既有 Tag 翻译后执行；此前客户端功能与截图验证使用相同的 Tooltip 代码和新翻译。EULA 使用用户已授权的测试环境选择。

| 检查 | 实际结果 |
|---|---|
| JVM | **93 通过，0 失败/错误/跳过**：既有 90 项，加 3 项协议测试 |
| 服务端 GameTest | **20 通过**：Recovery 7、Meal Benefit 6、Diet 6、框架 sanity 1 |
| 协议边界 | 完整/空/不可用快照往返；未知 Benefit 与空类别保留；非法长度、截断、枚举、NaN/负恢复、重复类别及不可用非空状态被拒绝 |
| 普通/高级 Tooltip | Quality、Recovery、Benefit 各一份；调试字段只在高级模式；fallback 食物有基础品质；木棍没有食物行；实验 Steady 不显示普通增益 |
| 发布资源 | 源码 JSON 均可解析；中英文 20 键一致（含原有 1 键）；JAR 包含客户端/网络类、双语资源及原有 6 份 Profile |
| 发布隔离 | JAR 不含测试 Mod、失败注入、截图 Screen、AppleSkin 或测试数据包 |
| 文档/Git | 本地文档链接、Update Notes 格式与追加语义、最终差异空白检查通过 |

## 实机/运行验证

使用 Fabric Client GameTest 启动真实图形客户端，分别连接 integrated server 与 localhost DedicatedServer。不是以静态文字模拟网络和 Tooltip。

每种连接都验证：

1. 登录收到蘑菇煲基础 Recovery 3，烤马铃薯为 fallback。
2. 在测试世界安装数据包、成功重载后显示 Feast / Recovery 7，并新增烤马铃薯 Recovery 2。
3. 把文件改为 9 并注入整轮重载失败，客户端仍保留 7。
4. 删除测试定义并成功重载，蘑菇煲回到 3，烤马铃薯恢复 fallback。
5. 发送显示不可用状态，客户端停止添加 Buildup 食物行；再次成功重载恢复显示。
6. 断线清空、专服重连重新获得服务器快照。

未安装和安装 AppleSkin 均完成上述场景，并回归既有恢复、增益、饮食、跨维度、死亡、存档重开及重连测试。安装场景日志确认实际加载 `appleskin 3.0.10+mc26.3`；测试检查其 `FoodOverlayTextComponent` 恰好一份。

使用真实原版物品 Tooltip 渲染入口生成并检查 **8 张截图**：两种语言 × 普通/高级 × 无/有 AppleSkin。854×480 测试窗口内，新文字无缺字或截断；普通提示保持简短，高级信息分离；AppleSkin 的 Hunger / Saturation 图示保留且没有叠画。截图仅验证该窗口与 GUI 比例，没有泛化为所有分辨率和资源包均已验证。

首轮截图拍到资源加载过渡画面，已将测试改为等待 `client.gui.overlay() == null` 后截图并重跑。首次 AppleSkin 命令被 PowerShell 拆分了含点的参数，已给完整 `-PtestAppleSkinJar=...` 参数加引号并成功重跑。自检发现翻译文件替换漏留旧 Tag 键，已恢复两种语言并重新构建。

本地证据均位于忽略目录：

- `run/stage5-build.log`、`run/stage5-final-build.log`：完整构建；
- `run/stage5-server-gametest.log`：服务端测试；
- `run/stage5-standalone-client.log`、`run/stage5-appleskin-client.log`：两种客户端场景；
- `run/stage5-appleskin-build.log`：可选兼容任务；
- `run/stage5-visuals/`：八张最终截图；
- `build/reports/tests/test/index.html`：JVM 报告。

## 已知问题与自检

**AppleSkin 的生命恢复预测仍采用原版模型，不能准确代表 Buildup 的储备与自然恢复。** 本轮通过的是 Tooltip 营养图示与连接的基础兼容，不宣称其心形预测已适配。玩家可自行关闭 AppleSkin 的 `showFoodHealthHudOverlay`；本模组不改写对方配置。

AppleSkin 单独安装时记录找不到 JEI 可选 Mixin 目标的警告；JEI 未安装，本轮没有验证 JEI 集成。其余仍有既有 WMI 权限、开发账号/Realms 认证、测试框架各向异性过滤及 `minecraft:end_of_frame` 告警。未知/Steady 引用来自隔离测试数据。没有 Buildup Mixin 注入失败或未解决测试失败。

```text
Code Review: PASS WITH RISKS

Scope:
- Reviewed files: 本轮全部生产代码、资源、测试、构建配置及文档差异
- Target version/directory: Minecraft 26.3 / 26.3

Findings:
- BLOCKER: None
- MAJOR: None
- MINOR: None

Fixes Applied During Review:
- 恢复原有中英文水果 Tag 翻译，避免资源回归。
- 增益可用性使用 available 文案，避免误解为玩家当前已激活。
- 修正截图等待时机和 PowerShell 兼容测试参数引用。
- 忽略 JVM 网络类型初始化产生的本地日志目录。

Verification:
- Wrapper build / 93 JVM tests / 20 server GameTests: PASS
- Integrated + dedicated connected tests, with and without AppleSkin: PASS
- Eight real-renderer screenshots / JSON / release JAR isolation: PASS
- Client-server boundary / existing persistence and Mixin unchanged: PASS
- Documentation / final diff / Update Notes: PASS

Residual Risks:
- AppleSkin 生命恢复预测未适配；可关闭其预测选项，营养图示基本兼容已验证。
- 其他 HUD/Tooltip Mod、第三方资源包、多客户端并发和所有 GUI 比例未验证。
- 超大数据包的完整网络分片压力测试未运行，超限有明确显示降级边界。
- 可读性主观体验待人工验收；数值平衡按用户要求延期。
```

阻断项：无。

## 未进入的下一阶段内容

没有实现 Stage 6 Thirst Was Taken 2 Adapter、Hydration 玩法及其显示；没有新增饮水动作或状态条。没有提前进入食物全面适配或战斗数值微调。

## 人工验收清单

- [ ] 进入世界后查看蘑菇煲：正式料理、基础恢复 3、调养和蔬菜；南瓜派显示对应品质、基础恢复 1 与精力充沛。
- [ ] 查看牛排/面包/苹果：Basic 与对应类别；未匹配的烤马铃薯只有基础品质；木棍不添加食物信息。
- [ ] F3+H 关闭时没有 Profile/Group/内部数值；打开后出现调试信息，再关闭能恢复简短提示。
- [ ] 中英文分别阅读普通提示，确认名称和排版易懂；按日常使用的 GUI 比例检查完整显示。
- [ ] 修改测试数据包并 `/reload`，确认客户端显示服务器新值；移除定义后不残留旧值，退出重进仍正确。
- [ ] 分别不安装和安装对应 AppleSkin，确认 Buildup 提示正常、营养图示没有重复；不要把其原版心形预测当作本模组实际治疗量。
- [ ] 不出现新增常驻状态条或未生效的口渴信息；本轮只验收信息表达，数值收益留待机制基本完成后评估。

## 建议下一步

Stage 5 到此停止，等待人工验收。通过后再进入 Stage 6。
