# Recovery v2（Stage 7.5）

当前规范为 [BALANCE_SPEC_V2](../BALANCE_SPEC_V2.md)。生命值单位 HP，2 HP = 一颗心；20 tick = 1 秒。Stage 2～7 的 50/80/120 tick Alpha 数值已被替代。

## 一套恢复时钟

| 条件 | 基础周期 | 单次恢复 |
|---|---:|---:|
| Hunger = 20，Saturation > 0 | 12 tick | `min(Saturation, 6) / 6 HP` |
| Hunger ≥18，且不满足上行 | 80 tick | 1 HP |
| 存在可用食物储备 | 12 tick | 最多 1 HP |
| 储备 + Restorative | 10 tick | 最多 1 HP |

Saturation 1、2、3、6、>6 对应单次 1/6、1/3、1/2、1、1 HP。Stable 没有额外 Saturation 门槛；Hunger <18 无自然恢复。自然恢复遵守 `naturalHealthRegeneration`，食物储备及外部治疗不受该游戏规则关闭影响。和平模式的原版背景治疗同样交由此时钟协调；饥饿伤害和原版耗竭扣减保留。

Variety 仅给满 Hunger 且 Saturation >0 的恢复最多 +7.5% 速度；已验证 TWT2 中启用口渴且 Thirst=20、Quenched>0 给自然恢复 ×1.15 速度。两者相乘，最终自然周期至少 10 tick。小数周期保留小数进度，不用整数向上取整丢掉小额奖励。

有储备时取 `min(foodInterval, naturalInterval)`。单次治疗取食物可支付量与本次自然恢复量的较大值，并裁剪到实际缺血量；不足 1 HP 的储备尾数可以由自然恢复补足。食物优先支付，剩余实际自然治疗才产生 `实际 HP ×6` Exhaustion。没有两套并行治疗器，不会因为吃下料理而拖慢自然恢复。

## 食物储备

服务端仅在真实 Food + Consumable 消费完成后读取 Profile，记录饮食，按当次 Variety 加入储备。总上限 20 HP；小于 `0.000001 HP` 的尾数归零。治疗后按实际成功治疗扣储备，不扣被其他机制阻止的治疗。满血保留储备并归零时钟，不预充治疗；再次受伤重新开始正常周期。受击不清零、不暂停、不延迟已有进度。创造/旁观不授予或兑现储备。

药水、信标、金苹果和第三方独立治疗照常运行；没有包装 `LivingEntity.heal` 或全局伤害入口。Invigorated 不减免自然恢复耗竭。AppleSkin 的原版回血预测不是 Buildup 储备预测。

## 满饱食与积食

普通 Food + Consumable 在 Hunger=20 仍允许使用，非食物药水不会因此变成食物。进食完成前读取 Hunger，计算：

```text
overflow = max(0, nutrition - (20 - hungerBefore))
load = min(80, load + overflow)
```

正常补足缺失 Hunger 不增加负荷。48 首次轻提示；64 进入 Overfull / 积食；80 封顶；每 40 个有效生存/冒险 tick 降 1；严格低于 32 解除并允许下一轮提示。64 起约 66 秒解除，80 起约 98 秒；离线不消化。持久化保存负荷、部分消化进度以及 32～63 区间无法仅由负荷推导的积食/提示锁存。

积食阻止新增储备、暂停已有储备、阻止主要饮食增益授予及刷新；自然恢复、外部治疗、营养、补水和饮食记录保留。仍可继续进食，最终使用时长乘 1.25 向上取整。没有负荷常驻 HUD 或普通 Tooltip 数字。手动 `/effect give` 的 Overfull 同样应用这些限制；负荷触发的积食在负荷尚未解除时会补回被清除的效果。

## 使用时间

Schema v1 可选 `consumption.speed`：normal=32、quick=21、fast=16 tick。它们是以原版普通食物 32 tick 为基准的明确档位，避免把原本 16 tick 的干海带再次缩到 8 tick。未声明时完整保留物品自身时长。积食后的 fast=20、quick=27、normal=40 tick。实际服务端使用计时和客户端动画使用同一服务端 Profile 元数据；中途取消不增加储备或负荷。

## 保存、迁移与查询

`buildup_vitals:recovery` 保持原有 reserve/progress/mode 字段；progress 扩展为可保存小数，旧整数仍可读。旧周期过长的已保存进度裁剪到新模式的最大合法整数进度，合法新小数原样保留。`buildup_vitals:overeat` 是新增附件。死亡立即清空，死亡重生不复制；正常保存重进、维度切换、活着替换玩家与重连保留。效果迁移见 [MEAL_BENEFITS](MEAL_BENEFITS.md)。

管理员 `/buildupvitals recovery [player]` 查看 Health、Hunger、Saturation、Reserve、模式、实际周期/进度、主要效果、Overeat、Overfull、Variety、自然速度和 TWT2 Thirst/Quenched。`Infinity` 表示当前不满足自然恢复条件。`/buildupvitals diet [player]` 查询饮食明细。

验证与人工体验清单见 [Stage 7.5 报告](STAGE_7_5_REPORT.md)。自动测试验证数学和生命周期，战斗手感仍需实玩验收。
