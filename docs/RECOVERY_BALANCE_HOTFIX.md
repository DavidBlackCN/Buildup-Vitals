# Stage 7.5 Recovery Balance Hotfix

> 历史报告：本文记录此前11 tick方案与当时验收目标。当前已由 [10 tick自然恢复基准](RECOVERY_BASELINE_10.md) 替代；不再以组合场景偏离±10%作为削弱自然恢复的理由。

日期：2026-10-06。仅处理 Recovery 数值和验证，不增加机制，不进入 Stage 8。

## 结论与范围

官方 `recovery.health` 允许0，非零最小值为 **1 HP（半颗心）**，优先整数1～6。扫描全部41份内置定义（40份原版 Item、1份示例 Tag）后，无正数小于1，也无非整数基础恢复。40种原版物品分布为：0 HP ×19，1 HP ×17，2 HP ×1，3 HP ×2，4 HP ×1。

唯一核心数值修改为 `CoreBalance.Recovery.WELL_FED_TICKS: 12 → 11`。Saturation 的 `min(saturation,6)/6` 恢复量曲线、Stable80、Food12、Restorative10、自然最快10 tick、每实际自然恢复HP消耗6 Exhaustion，以及 Variety / Quenched 倍率均保持不变；没有修改 Controller 算法、Mixin、存档格式或第三方 Profile Schema。

**满营养普通恢复达到原版+10%的目标；有限营养并带食物储备的情况并非全部在±10%内。** 后者的储备节省自然恢复成本，能够延迟饱和恢复降速；本轮如实保留并测量这一既有机制，不将其误报为全场景达标。

## 食物定位调整

| 旧值 | 新值 | 物品与理由 |
|---:|---:|---|
| 0.5 | 1 | 苹果、胡萝卜、甜菜根、马铃薯、面包、西瓜片、甜浆果、发光浆果、金胡萝卜；提供可感知的基础恢复 |
| 0.5 | 0 | 生牛肉、生猪排、生羊肉、生兔肉、生鳕鱼、生鲑鱼、热带鱼；保留原生营养与较高补水，让烹饪产生明确恢复收益 |
| 0.5 | 0 | 干燥曲奇、特殊用途紫颂果；分别保留快速零食和独立传送定位 |

熟制单品仍为1，南瓜派2，蘑菇煲/甜菜汤3，兔肉煲4。金苹果、附魔金苹果、蜂蜜瓶和可疑炖菜继续为0；不额外叠加其独立效果。Quality、Hydration、Variety 分组、消费档位和原版营养均未调整。完整清单见 [VANILLA_BALANCE](VANILLA_BALANCE.md)。

约束仅针对官方基础数据。第三方小数 Profile、已保存的小数储备、Variety 加成、缺血裁剪和自然饱和恢复的小数继续有效。旧 Well-fed 进度11加载到新周期时沿用既有 Codec 裁剪为10，无格式迁移。

## 对照条件与方法

原版基准来自本地已解析 Minecraft 26.3 `FoodData` 源码：饱和恢复10 tick，Stable80；按原版 `float` 运算顺序处理饱和消耗，Exhaustion严格大于4时每tick扣一次。独立 JVM 参考模型与实际 `RecoveryController` 对照；GameTest 通过原版消费完成、`FoodData.tick`、真实效果与 TWT2 玩家 tick 路径复核本模组结果。

主表统一从 **10/20 HP、Hunger20、Saturation20、Exhaustion0、恢复进度0** 开始。食物组从消费完成后开始计时，饱食与饱和均已满；不计进食动画时间，不再进食、不移动、无积食、无药水/信标/金苹果治疗。按每秒20tick换算。Quenched组初始Thirst20/Quenched20，运行中继续实际扣减。Variety上限列为倍率理论边界，另用真实十餐历史验证实际组合。

## 修改前后：半血回满

同条件原版为 **100 tick / 5.00秒**。

| 场景 | 热修前 tick / 秒 | 热修后 tick / 秒 | 热修后相对原版时间 |
|---|---:|---:|---:|
| 仅自然恢复 | 120 / 6.00 | 110 / 5.50 | +10% |
| 普通苹果（储备0.5→1） | 120 / 6.00 | 110 / 5.50 | +10% |
| 4 HP 兔肉煲 | 120 / 6.00 | 110 / 5.50 | +10% |
| 3 HP 滋养料理 | 114 / 5.70 | 107 / 5.35 | +7% |
| 自然恢复 + Quenched | 105 / 5.25 | 100 / 5.00 | 0% |
| 苹果 + Quenched | 105 / 5.25 | 100 / 5.00 | 0% |
| 自然恢复 + 最高 Variety | 112 / 5.60 | 103 / 5.15 | +3% |
| 滋养 + 最高 Variety（3.45 HP储备） | 107 / 5.35 | 102 / 5.10 | +2% |
| 滋养 + 最高 Variety + Quenched | 100 / 5.00 | 100 / 5.00 | 0% |

先只修改 Profile、仍保留12 tick再计算，上表总时间均未改变。苹果提高恢复量只将实际自然费用由57降至54 Exhaustion；自然恢复无食物为60，4 HP Meal为36，3 HP滋养Meal为42。这证明满营养下新增储备主要替代自然恢复支出，并未并行增加治疗。

因此调整11 tick的原因是旧普通自然恢复比原版慢20%，不是预设“食物变强必须削弱 Saturation”。满营养普通组110 tick恰为+10%边界，好的 Vitals 受10 tick下限限制，仍不会出现两个治疗器叠加。

真实十餐历史的 Variety（非虚构满分）下：无食物103 tick、苹果104、兔肉煲103、滋养102；与Quenched组合后四组均100。自然速度实际约1.067～1.070，食物额外储备约13.5%～13.9%。

## Stable、独立食物与有限营养

- Stable的受控对照（Hunger19、Saturation20，仅自然恢复）：原版、本模组均800 tick / 40秒；Quenched持续有效时模型为696 tick / 34.8秒。Variety不加速Stable。
- 不满足自然恢复的实际消费测试：苹果1 HP需12 tick / 0.6秒；兔肉煲4 HP需48 tick / 2.4秒；蘑菇煲3 HP含滋养需30 tick / 1.5秒。每份储备耗尽即停，未增加持续治疗量。
- 受击不重置储备进度、积食暂停食物但保留自然恢复、食物不能拖慢自然恢复、治疗实际扣费和外部治疗独立性继续由现有测试覆盖。

以下初始Saturation不足以全程维持最高脉冲，不能用“缺10 HP ×固定周期”估算。仍为半血、满Hunger、无额外活动；食物列已含1 HP苹果储备：

| 初始 Saturation | 原版 | 热修后仅自然 | 热修后苹果 | 含苹果的时间差 |
|---:|---:|---:|---:|---:|
| 15 | 140 tick / 7.00秒 | 154 / 7.70秒 | 121 / 6.05秒 | -13.6% |
| 12.8 | 310 tick / 15.50秒 | 314 / 15.70秒 | 245 / 12.25秒 | -21.0% |
| 10 | 无法回满，停在约18.833 HP | 同左 | 停在约19.833 HP | 不应报告完整恢复时间 |
| 6 | 无法回满，停在约16.167 HP | 同左 | 停在约17.167 HP | 不应报告完整恢复时间 |

“无法回满”组跟踪6000tick，营养不足导致Hunger低于自然恢复阈值；未偷偷补回Saturation或持续补食。15与12.8组的本模组时间均经过GameTest复核。低饱和值的初步估算已由Java浮点逐tick结果替代，上表为最终结果。

单一全局间隔无法同时抵消有限营养下的储备节省、并保持满营养基线在±10%。为遵守小范围数值热修复，本轮不引入食物耗竭成本、按营养动态惩罚储备等新机制。此为明确的平衡边界；不能把测试通过解释为所有生存情境的主观手感已经验收。

## 修改与验证范围

生产改动：18份原版 Profile 与 `src/main/java/com/davidblackcn/buildupvitals/config/CoreBalance.java` 的一个常量。测试新增 `RecoveryBalanceComparisonTest`、`RecoveryBalanceGameTests`，更新现有消费、费用、饮食周期断言，并在原版40种物品遍历中检查官方最小值与整数单位。

当前正式规范同步至根目录和版本目录的 `BALANCE_SPEC_V2.md`、`DESIGN_PRINCIPLES.md`，以及 `docs/VANILLA_BALANCE.md`、`docs/FOOD_PROFILES.md`、`docs/RECOVERY.md`、`docs/DIET_MEMORY.md`、README。历史 Stage 报告保留当时结论，本报告记录本次替代关系。根目录不是Git工作树，根文档修改不属于版本目录提交；相同内容的版本目录副本纳入提交。

工具链保持 Minecraft26.3 / Java25.0.3 / Gradle9.7.1 / Loom1.18.2 / Loader0.19.5 / Fabric API0.161.0+26.3，原生命名；TWT2精确适配1.6.2+26.3，不改依赖或注入点。

## 最终验证与自检

以下命令均在 `26.3` 使用仓库 Wrapper 执行并通过：

```powershell
.\gradlew.bat build -PwithThirst=true -PacceptMinecraftEula=true --console=plain
.\gradlew.bat build -PacceptMinecraftEula=true --console=plain
.\gradlew.bat build runClientGameTest -PwithThirst=true -PacceptMinecraftEula=true '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar' --console=plain
.\gradlew.bat build runClientGameTest -PwithThirst=true -PacceptMinecraftEula=true --console=plain
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true --console=plain
```

- **108项完整JVM测试通过**，0失败/错误/跳过；新对照测试涵盖9组峰值组合、Stable和4组有限饱和条件。
- **39项完整服务端GameTest**分别在无TWT2、TWT2、TWT2＋AppleSkin环境通过。可选补水分支在未安装TWT2时不执行；官方数据审计和恢复用例始终执行。
- 三组环境的**4个客户端测试入口通过**，含集成服务器、DedicatedServer TCP连接、重载/回滚、重连、死亡、存档、界面与消费计时；可选补水连接场景仅在TWT2存在时运行。无TWT2与无AppleSkin的客户端已在最终生产代码上通过，之后新增有限饱和测试只扩充测试源码；最后再执行对应完整服务端构建。
- 所有41份内置Profile均经过JSON扫描，40种原版物品又经过真实注册表遍历审计：无正数小于1；18份数据差异仅限 `recovery.health`。
- 主表修改前、只改Profile和最终计算证据分别为本地 `run/recovery-hotfix-before.xml`、`run/recovery-hotfix-profiles-only.xml`、`run/recovery-hotfix-after.xml`。最终构建日志为 `run/recovery-hotfix-final-{thirst-build,standalone-build,appleskin}.log`；早期两组客户端通过日志为 `run/recovery-hotfix-{thirst,standalone}.log`。
- 无Mixin注入失败，发布JAR不含测试类或可选模组本体；已有WMI权限、Realms开发认证和各向异性过滤选项日志未影响测试。
- 根目录两份正式文档与版本目录副本内容一致；保留原有Markdown换行格式差异。`git diff --check`、JSON、文档路径和更新说明检查通过。

**Code Review: PASS WITH RISKS**，已按根目录 `CODE_REVIEW.md` 检查本轮全部差异。BLOCKER/MAJOR代码问题：无；没有改动其他版本，没有覆盖用户已有修改。`UPDATE_NOTES.md` 已追加两条已验证变化。

剩余风险是上文公开的有限营养恢复时间差，不能声明全场景±10%已实现；长期战斗主观手感、真人多人压力和未列出的模组组合未验收。相关自动测试没有以放宽断言掩盖问题，而是分别固定满营养目标与有限营养实际结果。完成本轮后停止，不进入 Stage 8。
