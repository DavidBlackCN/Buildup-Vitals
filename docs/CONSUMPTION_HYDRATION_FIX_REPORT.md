# Stage 7.5 补水与创造模式进食修复

日期：2026-10-06。目标：Minecraft 26.3；本轮只修改 `26.3`。

## 问题与修复

1. **纯水容器覆盖不完整。** 原实现仅在 `minecraft:potion` 且水质为 Pure 时覆盖 10/8，因此 Pure 水瓶本身已生效，Pure 装水陶碗、水袋、铜水壶和铁水壶遗漏。`ThirstValuesMixin` 改用已验证 TWT2 的 `WaterPurity.isPlainWaterDrink(ItemStack)`，对上述直接饮用的 Pure 水每份提供 10 Thirst / 8 Quenched。空容器、盐水、普通药水、果汁和不可直接饮用的水桶不套用基准；黑名单与 Buildup 显式 Profile 仍优先，水质比例和疾病继续由 TWT2 结算。
2. **创造模式进食档位被跳过。** `Consumption.duration` 原先对创造模式直接返回原版时长，导致显示“轻快进食”的苹果仍耗时 32 tick。现创造与生存模式共用 Profile 档位：normal 32、quick 21（约 1.5 倍速度）、fast 16 tick。创造模式仍不产生恢复储备与过食负荷，且两端都忽略其积食减速，防止切换模式后残留效果导致动画与服务器计时不一致。

`10/8` 仍仅适用于 **Pure**。Dirty/Murky/Clean 水保留上游基础值及水质规则，并未被重新定义成纯水。未声明消费档位的物品保留原生时长，例如蜂蜜瓶仍为 40 tick。

## 修改文件

- `src/main/java/com/davidblackcn/buildupvitals/compat/thirst/mixin/ThirstValuesMixin.java`：扩展栈级纯水判定和物品黑名单检查。
- `src/main/java/com/davidblackcn/buildupvitals/food/consumption/Consumption.java`：修复创造模式档位及客户端积食豁免。
- `src/gametest/java/com/davidblackcn/buildupvitals/V2GameTests.java`、`V2ClientGameTest.java`：增加创造模式各档位、原生时长、真实使用计时与完成结算回归。
- 同目录 `ThirstTestSupport.java`、`ThirstClientScenario.java`：增加五种容器的 API、实际饮用、份数、空容器、黑名单、低水质、盐水和客户端显示验证。
- `docs/HYDRATION.md`、`docs/CLIENT_FEEDBACK.md`：说明容器覆盖范围及创造模式消费规则。
- 本报告及 `UPDATE_NOTES.md`：记录证据与 Release 变化。

## 验证证据

先添加回归测试再修改实现，旧代码分别在“创造模式苹果应为 21 tick”和“Pure 陶碗应为 10/8”断言处失败，见本地 `run/fix-consumption-repro.log`。扩展客户端连续进食场景时清理其 Diet 测试状态，避免新增苹果消费改变原有曲奇的 Variety 恢复断言。

在 Java 25.0.3 下执行以下 Wrapper 命令，均为 **PASS**：

```powershell
.\gradlew.bat build runClientGameTest -PwithThirst=true -PacceptMinecraftEula=true --console=plain
.\gradlew.bat build runClientGameTest -PwithThirst=true -PacceptMinecraftEula=true '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar' --console=plain
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true --console=plain
```

- JVM 报告：107 项，0 失败、0 错误、0 跳过；未变化的测试任务允许 Gradle 复用结果。
- 三组环境各 36 项服务端 GameTest 通过；未安装 TWT2 的环境不执行可选补水分支。
- 三组环境均运行 4 个客户端测试入口；无 TWT2 时验证可选依赖缺失路径，补水消费场景明确不运行。
- 集成服务器与 DedicatedServer TCP 连接均验证创造模式牛排 32 / 苹果 21 / 曲奇 16，以及生存苹果 21 tick；客户端开始计时、服务端计时和消费统计一致，每次完成一次消费。
- 五种 Pure 容器每份实际补水 10/8；三种多份容器喝两份后从 0/0 到 20/16，空后无补水。盐水不补水，各物品黑名单生效。
- 有、无 AppleSkin 的 Pure 陶碗截图均检查通过：补水 5 个完整水滴、Quenched 4 个完整水滴，无重复行。证据保存在本地忽略目录 `run/fix-consumption-evidence/{thirst,appleskin}/screenshots/`。
- 完整日志：`run/fix-consumption-{thirst,appleskin,standalone}.log`。无 Mixin 注入错误；既有测试环境各向异性过滤选项 0、WMI 权限和 Realms 开发认证日志不影响上述断言。
- 发布 JAR 未包含测试类或嵌入的可选依赖。

## Code Review

**PASS**。按根目录 `CODE_REVIEW.md` 检查完整差异，无 BLOCKER / MAJOR / MINOR；`git diff --check` 通过，更新说明已追加。

版本配置保持 Minecraft 26.3、Java 25、Gradle 9.7.1、Loom 1.18.2、Loader 0.19.5、Fabric API 0.161.0+26.3；沿用目标版本原生命名，未引入 Yarn 或新依赖。TWT2 窄注入仍精确门控 1.6.2+26.3，栈级方法描述符不变。已核对本地 TWT2 源码及 Minecraft 使用计时链路；26.3 `FirstPersonHandsAndItems` 也读取 `ItemStack.getUseDuration`。

无存档格式或配置文件修改，无已知剩余问题。客户端专服连接测试使用同 JVM DedicatedServer + TCP；未覆盖长期多人压力测试或未列出的第三方模组组合。
