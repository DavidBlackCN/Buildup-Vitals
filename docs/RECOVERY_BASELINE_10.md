# Stage 7.5：10 tick Passive Natural Recovery 基准

2026-10-06。替代此前 [11 tick热修复方案](RECOVERY_BALANCE_HOTFIX.md)，不进入Stage 8。

## 本轮调整

生产代码仅修改 `CoreBalance.Recovery.WELL_FED_TICKS` 为10。连续公式仍为 `min(Saturation,6)/6 HP`；Food12、Restorative Food10、Stable80、自然最快10 tick、实际自然治疗每HP支付6 Exhaustion及20 HP储备上限均保留。未改Food Profile、Controller算法、Mixin、依赖、存档格式或独立治疗。

新目标：**Passive Natural Recovery直接以原版为baseline；主动进食、料理、Hydration和Variety允许有限正反馈，不再将所有组合严格限制在原版±10%。** 不因部分主动进食场景快约10%～20%而再次削弱自然恢复。约束来自单一时钟、有限储备、单次最多1 HP和自然周期至少10 tick，而非将所有恢复来源压成相同总时间。

当前高饱和基准已达10 tick下限，因此Variety与Quenched速度倍率不会再提高这个档位的频率。Variety的储备/增益时长奖励保留；Quenched仍可加速Stable；滋养仍能缩短无自然恢复或较慢自然恢复条件下的食物兑现周期。满营养时食物收益主要体现为替代自然治疗支出、保留更多营养。

## 实际恢复时间

从10/20 HP、Hunger20、Exhaustion0、进度0开始；无额外活动、药水、信标或金苹果治疗。食物列从消费完成后开始计时，并统一消费后的Saturation，不计进食动画或途中额外进食。20 tick=1秒。

原版列为按已解析Minecraft26.3 `FoodData` 源码实现的独立JVM参考模型；本模组列使用真实Controller逐tick数学与服务端 `FoodData` / 效果 / TWT2运行测试交叉核对。

| 条件 | 原版被动恢复 | 当前仅被动恢复 | 当前含主动/良好状态收益 |
|---|---:|---:|---:|
| Saturation20，无食物 | 100 tick / 5秒 | 100 / 5秒 | — |
| Saturation20，1 HP苹果 | 100 / 5秒 | 100 / 5秒 | 100 / 5秒 |
| Saturation20，4 HP兔肉煲 | 100 / 5秒 | 100 / 5秒 | 100 / 5秒 |
| Saturation20，3 HP滋养料理 | 100 / 5秒 | 100 / 5秒 | 100 / 5秒 |
| Saturation20，Variety / Quenched / 滋养组合 | 100 / 5秒 | 100 / 5秒 | 各组均100 / 5秒 |
| Saturation15，1 HP苹果 | 140 / 7秒 | 140 / 7秒 | 110 / 5.5秒 |
| Saturation12.8，1 HP苹果 | 310 / 15.5秒 | 300 / 15秒 | 230 / 11.5秒 |

食物是额外来源，不支付自然恢复耗竭，因而有限营养下能够延后降速。这些有限正反馈不触发新一轮自然恢复削弱。基准对齐指周期与连续公式；有限营养尾段仍保留本模组原有实际治疗扣费和浮点结算，不能宣称所有逐tick结果与原版完全一致，例如12.8组仅被动相差一个10 tick周期。

满营养时回满的自然费用：无储备60 Exhaustion，苹果54，兔肉煲36，滋养料理42，最高Variety滋养料理39.3。耗费减少而未增加并行治疗。真实十餐Variety历史、TWT2开启/关闭和组合场景均保持100 tick回满。

- Stable受控对照（Hunger19、Saturation20、无食物）：原版与本模组均800 tick / 40秒；Quenched持续有效时数学结果696 tick / 34.8秒。
- 无自然恢复时：1 HP苹果12 tick；4 HP兔肉煲48 tick；3 HP滋养料理30 tick。没有修改这些周期或总量。
- 初始Saturation6或10、无后续进食时，原版和本模组均可能因营养不足无法回满；本轮不虚构有限恢复时间。

## 测试与兼容性

更新原有Recovery对照、首次脉冲、Variety下限及实际耗竭时点断言。保留食物不拖慢自然恢复、受击不中断、积食只暂停食物、储备耗尽和独立治疗测试。旧Well-fed周期保存的进度10或11经既有Codec裁剪至9，原储备保留；新增对应回归断言，不引入新存档格式。

工具链不变：Minecraft26.3、Java25.0.3、Gradle9.7.1、Loom1.18.2、Loader0.19.5、Fabric API0.161.0+26.3，使用原生命名；可选TWT2精确门控1.6.2+26.3。

根目录及版本目录的 `BALANCE_SPEC_V2.md`、`DESIGN_PRINCIPLES.md` 同步当前目标；更新 `docs/RECOVERY.md`、`DIET_MEMORY.md`、`HYDRATION.md`、`MEAL_BENEFITS.md` 和README，明确哪些收益会被10 tick下限裁剪。根目录不是Git工作树，同内容的版本目录副本纳入提交。旧热修复报告仅加历史提示，不重写旧结果。

实际验证（在26.3目录，Java25.0.3，清空环境变量DEBUG）：

- `./gradlew.bat build runClientGameTest -PwithThirst=true -PacceptMinecraftEula=true --console=plain`：PASS。108项JVM测试零失败、39项服务端GameTest通过，4个客户端测试入口通过，覆盖集成/独立服务端连接、存档重开及重连。日志：`run/recovery-baseline10-thirst.log`。
- `./gradlew.bat build -PacceptMinecraftEula=true --console=plain`：PASS。无TWT2的39项服务端GameTest通过，JVM任务复用同一源码的成功结果。日志：`run/recovery-baseline10-standalone.log`。
- 全部41份内置Profile（含直接饮水Profile）扫描：没有 `0 < recovery.health < 1`；本轮未修改任何Profile。
- 根目录与版本目录两份正式规范内容一致；`git diff --check`通过。

初次验证发现一处测试夹具误将关闭自然恢复时的滋养等待时间改为9 tick，已恢复10 tick并重新跑完上述验证；生产逻辑未因此改动。

## 自检

Code Review: PASS。审查26.3全部改动及两份根目录规范；无BLOCKER、MAJOR或MINOR。生产变更仅一个常量，旧进度可安全读取，相关测试与当前规范已同步，UPDATE_NOTES追加本轮记录。无已知阻塞或新增兼容性风险；主观战斗手感仍由实玩验收，本报告不将自动测试等同于人工体验。完成后停止，不进入Stage 8。
