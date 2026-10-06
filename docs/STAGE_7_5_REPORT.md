# Stage 7.5 — Balance v2 & Consumption Rework

目标目录：`26.3`。基线提交：`e1690a7`。本阶段依据 `BALANCE_SPEC_V2.md` 和 `Stage 7.5 Plan.md`；Stage 7 Alpha 数值被 v2 替代，旧报告保持历史内容。Stage 8 未启动。

## 实现摘要与主要文件

- `food/recovery/`、`config/CoreBalance.java`：连续 Saturation 数学、80 tick Stable、12/10 tick Food、单时钟协调、实际治疗结算与小数周期。
- `food/overeating/`、`mixin/FullHungerEatingMixin.java`：满饱食进食、消费前 Nutrition 溢出、负荷持久化、积食与一次性提示。
- `food/consumption/`、`food/profile/ConsumptionSpeed.java`、Parser/schema、`network/`：schema v1 可选三档速度、服务端权威时长和 v3 客户端同步。
- `effect/BuildupEffects.java`、`food/benefit/PlayerMealBenefits.java`：正式 MobEffect、主效果互斥、旧附件迁移，原版保存/同步/倒计时。
- `compat/thirst/`：显式 Profile 优先级、Pure Water 10/8、Quenched 统一恢复，以及无 AppleSkin 时的上游水滴复用。
- `src/main/resources/data/.../food_profiles/`：40种食物 v2 Recovery/Hydration/速度；保留类别分组、原版营养、容器和特殊效果。
- `src/client/`、`assets/buildup_vitals/`：爱心/效果图标短提示、中英文本地化、4枚状态图标、效果卡悬停说明。
- `command/`、`src/test/`、`src/gametest/`：实时调试信息、纯数学与实际服务端/客户端回归。
- 根目录及版本内 `DESIGN_PRINCIPLES.md`、正式 docs、README、PLAN：同步当前机制。根目录共享文档按本阶段明确要求更新；版本 Git 中保存相同副本及两份 v2 规范。

## 关键数值与决策

| 项目 | v2 |
|---|---|
| 满 Hunger、Saturation>0 | 每12 tick 恢复 min(Saturation,6)/6 HP |
| 其他 Hunger≥18 | 1 HP /80 tick，无 Saturation 下限 |
| 食物储备 | 普通12、Restorative10 tick/HP；上限20 HP |
| 自然恢复加速 | Well-fed Variety≤7.5%；满 Thirst + Quenched>0 ×1.15；最短10 tick |
| Overeat | 警告48、积食64、上限80、严格低于32解除、1/40 tick消化 |
| Consumption | normal32 / quick21 / fast16；未声明保留原生；积食最后×1.25向上取整 |
| Pure Water Bottle | 10 Thirst /8 Quenched；空状态两瓶20/16 |
| 原版常见恢复 | 熟肉/熟鱼1、面包/蔬果/曲奇0.5、派2、蔬菜汤3、兔肉煲4 |

Food 与 Natural 共用时钟，周期取较快者，食物先支付，小于1 HP的食物尾数允许自然补足；自然部分实际每HP付6 Exhaustion。受击不中断，满血不预充时钟。积食只限制食物额外恢复/增益，营养、水分、自然恢复和外部治疗保留。

速度档位以普通32 tick食物为基准的固定时长，不对干海带原有16 tick再乘2倍；Quick 使用21 tick。旧schema v1无需批量升级；未声明速度保留 Mod 物品自身特殊时长。

Steady 完成正式注册、图标与主槽互斥，但没有投放到官方 Profile，也没有虚构稳定资源的行为；旧的 experimental 引用仍警告并忽略。其命令效果仅供查看占位 UI。

## 数据迁移

旧 `meal_benefit` 附件注册和 Codec 保留。首次登录/tick 将有效旧类型和剩余时间转为原版效果；已有现代主效果优先，然后删除旧附件。以后不再双重保存或计时。

Recovery 保留 reserve/progress/mode，progress可保存小数；旧整数可读，超出新周期的旧进度裁剪，合法新小数原样保留。Overeat 新增负荷/消化进度及必要的滞回锁存。死亡清空；维度转移、活着替换玩家、保存重进与重连保留。

食物快照通道更新为 `food_profiles_v3`，两端需要同时更新。整体重载失败保留旧资源与客户端快照，成功删除后回落。超大元数据包仍可能按既有限制降级，届时不能承诺自定义消费动画同步。

## TWT2 兼容策略

严格限定 Fabric 1.6.2+26.3。优先级为 blacklist > Buildup explicit Profile（含0）> 上游配置/drinks > 通用fallback。resolve注入在黑名单之后、配置首次查询之前；零值保留上游 NONE 语义。不存在第二份补水结算，取消事件、纯度和疾病继续生效。

纯水覆盖仅匹配 Pure 水瓶栈，Dirty/Murky/Clean/Salt继续原有水质路径。Quenched独立治疗器用已核对完整描述符的 HEAD 返回0关闭，统一为Buildup自然恢复速度奖励；不改配置、不包装全局heal、不捆绑上游实现或资源。

## 自动测试与运行证据

工具链保持：Minecraft26.3、Java25、Gradle9.7.1、Loom1.18.2、Loader0.19.5、Fabric API0.161.0+26.3，原生非混淆映射；未升级依赖。

按A→F分别执行过Recovery JVM、Overeat JVM/服务端、消费档位/网络、效果迁移、TWT2专项、40种食物场景验证；G/H执行真实客户端、旧场景v2断言更新及完整回归。

2026-10-06 最终矩阵全部通过（包含自检修正后的源码）。

| 命令（目标目录、Java25） | 结果 | 日志 |
|---|---|---|
| `.\gradlew.bat clean build runClientGameTest -PacceptMinecraftEula=true` | PASS | `run/stage75-final-standalone.log` |
| `.\gradlew.bat build runClientGameTest -PwithThirst=true -PacceptMinecraftEula=true` | PASS | `run/stage75-final-thirst.log` |
| `.\gradlew.bat build runClientGameTest -PwithThirst=true -PacceptMinecraftEula=true '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar'` | PASS | `run/stage75-final-appleskin.log` |

JVM共107项，服务端34项GameTest。客户端4个测试入口包含实际进食、HUD/背包、恢复/持久化、Tooltip及可选补水场景；无TWT2时口渴专项明确 NOT RUN，其余测试仍运行。测试 Mod 不进入发布JAR。

覆盖要点：Saturation1/2/3/6/>6、Stable、恢复上限、储备小数尾数、不受击重置、实际治疗成本、消费前饥饿差、积食暂停/解除/刷新阻断、饮食效果互斥、旧附件迁移、消费16/20 tick与取消、40种原版覆盖、金苹果/蜂蜜清毒/可疑炖菜/危险食物/紫颂果原生消费操作、纯水两瓶补满、其他水质、Profile/黑名单优先级、单次补水、独立Quenched治疗消除，以及成功/失败重载和断线清理。

客户端通过真实TCP连接验证同JVM DedicatedServer，另有 integrated world、存档重新打开、维度切换、死亡重生与重连。**本轮没有宣称独立操作系统进程客户端+服务端、多人压力测试或长期手感测试完成。**

## 客户端视觉验证

已实际打开自动截图检查中文短Tooltip、爱心/主要效果图标及原版背包时间。截图保存在忽略目录 `run/stage75-evidence/<组合>/`；原始截图位于 `build/run/clientGameTest/screenshots/`，后续测试可覆盖。

客户端断言包括四枚效果sprite非missingno、名称可翻译、图标标志及无粒子、满饱食fast动画与服务端计时一致、积食后时长、完成后只结算一次、跨真实连接一次性actionbar提示。AppleSkin组合保留其一份原生食物叠加，不重复水滴行。

## 自检与剩余边界

Code Review：**PASS**。BLOCKER / MAJOR / MINOR：无未解决项。

- 已核对 common/client/可选依赖边界、注册顺序、所有新增Mixin的26.3或TWT2目标与描述符；实际客户端及服务端启动验证注入，无Access Widener新增。
- 审查修正：小数进度存档不应被整数裁剪；积食服务端使用时长直接读取负荷锁存，避免效果尚未重同步时漏减速；一次跨过警告及积食阈值仍发一次警告。
- 已更新正式规则/schema/数据表，历史Stage报告未改写；用户的其他版本目录未修改。
- AppleSkin生命预测仍按原版估算，不等于Buildup储备承诺。其他TWT2版本关闭适配；其他HUD/食物Mod组合未验证。
- 当前无新常驻HUD、无药水配方、无复杂胃容量UI；未进入Farmer's Delight/Kaleidoscope Cookery完整兼容、Oxygen、Mana、Stamina或Stage8。
- 生产JAR核对通过：4枚效果图标，无测试Mod、测试类或嵌入第三方JAR；JSON/schema、当前文档本地链接、Git差异检查通过。UPDATE_NOTES已追加本阶段Release说明。
- 测试日志保留已有环境噪音：开发账号Realms认证、WMI权限、原版Anisotropic Filtering=0、缺少end_of_frame效果，以及clean后首次生成server.properties。重载失败测试有意制造失败并验证回滚；没有未处理的构建、断言或Mixin错误。

## 人工验收方法

生存模式准备半血、满Hunger及不同Saturation；使用 `/buildupvitals recovery` 查看实时状态。分别比较普通口粮、汤、快食与纯水。积食可以在满Hunger连续吃约8份牛排触发；用调试值对照提示与效果，测试停止进食后的消退。建议分别体验无TWT2、有TWT2及已有整合包。

以下清单保留待用户实际验收，不把自动测试结果填成人工通过。

## Recovery

- [ ] 满 Hunger + 高 Saturation + 半血时，恢复速度明显接近原版，不再长期残血。
- [ ] Saturation 从 6 降到 1 时，恢复量平滑下降，没有突然断档。
- [ ] Hunger ≥18 的慢速恢复约 4 秒半颗心。
- [ ] Food Recovery 明显比 Stage 7 快。
- [ ] Food Reserve 不会让高 Saturation 玩家反而恢复更慢。
- [ ] 受击不会暂停 Food Recovery。

## Full Hunger Eating

- [ ] Hunger = 20 仍然可以吃普通 Food Item。
- [ ] 正常缺 Hunger 时进食不会无故积累 Overeat。
- [ ] Hunger 满值时只吃一两份料理不会马上受到严重惩罚。

## Overeating

- [ ] 连续大量吃高 Nutrition 食物最终会进入积食。
- [ ] Warning 在积食前出现且不 Spam。
- [ ] 积食后 Food Recovery 不再新增。
- [ ] 已有 Reserve 暂停兑现。
- [ ] Meal Benefit 不再新增或刷新。
- [ ] Natural Recovery、药水治疗正常。
- [ ] 停止进食后约一分钟级别能恢复。

## Consumption

- [ ] Cookie / Dried Kelp / Berries 等明显比 Meal 快。
- [ ] 普通 Meal 仍保持正常速度。
- [ ] Overfull 会让继续进食变慢。
- [ ] 客户端动画和服务端完成时机一致。

## Meal Benefit

- [ ] Restorative / Invigorated 在 HUD 中可见。
- [ ] 背包效果页可见。
- [ ] 没有药水粒子。
- [ ] 同一时间只有一个主要 Meal Benefit。
- [ ] 不会自动出现可酿造 Potion。

## Hydration

- [ ] Pure Water 两瓶基本可以从空口渴回满。
- [ ] 生肉比熟肉略微更补水。
- [ ] 大部分普通食物不再全是 0 Hydration。
- [ ] Cookie / Dried Kelp 等真正干燥食物保持 0 或接近 0。
- [ ] Soup 明显补水。
- [ ] 水仍然是最好的直接补水方式之一。

## TWT2

- [ ] Buildup Profile 对已适配食物真正生效。
- [ ] blacklist 仍然最高优先级。
- [ ] Quenched 不再形成失控的独立叠加治疗。
- [ ] Dirty / Murky / Pure / Salty 水语义正常。
- [ ] 没安装 TWT2 时 Buildup 正常运行。

## 下一步

Stage 7.5 implementation complete。等待人工体验验收；通过后再决定进入 Stage 8，或只对 tick、恢复、补水、负荷阈值与消费档位进行 Stage 7.5.x 小幅调整。
