# 恢复机制（Stage 4）

普通食物继续使用自身 Food Component 提供 Hunger / Saturation。Food Profile 的 `recovery.health` 提供恢复储备；[Restorative](MEAL_BENEFITS.md) 可以加快其兑现，[饮食多样性](DIET_MEMORY.md) 提供少量额外储备及 Well-fed 速度奖励。未实现口渴、Tooltip 或新 HUD。战斗节奏和数值平衡待主要功能机制基本完成后统一评估。

## 恢复优先级与参数

所有时间均按服务端 20 TPS 计；卡顿时随游戏 tick 放慢，不在离线期间补发治疗。

| 路径 | 条件 | 速度 | 代价 |
|---|---|---|---|
| Food Recovery | 生存/冒险玩家受伤且储备 > 0 | 通常每 50 tick（2.5 秒）最多 1 HP；Restorative 时 40 tick（2 秒） | 仅扣实际获得的储备治疗量 |
| Well-fed | 无储备，Hunger ≥ 20、Saturation ≥ 6 | 基础每 80 tick（4 秒）最多 1 HP；多样性最高约 75 tick | 每实际恢复 1 HP 产生 6 Exhaustion |
| Stable | 无储备，Hunger ≥ 18、Saturation ≥ 1 | 每 120 tick（6 秒）最多 1 HP | 同上 |

Food Recovery 优先，期间不会并行积累自然恢复进度。储备耗尽后自然恢复从新周期开始；自然恢复档位切换也重新计时。受击不会重置进度，没有战斗暂停逻辑。

自然恢复遵循 `minecraft:natural_health_regeneration` 游戏规则；关闭该规则后，显式的食物 Recovery 仍有效，独立药水/再生/信标等治疗仍由原机制处理。

和平难度的额外快速回血也交给此控制器，避免叠加；原版和平难度自动补充 Hunger / Saturation 保留。Invigorated 仅降低指定活动的 Exhaustion，自然恢复成本、饥饿伤害与伤害计算不变。

## 储备与生命周期

- 吃完带 Food Component 的物品时，按当时服务端快照查询该 Item 的 Profile，记录本次饮食后加入 `recovery.health × foodMultiplier`。倍率在 1.0–1.15 之间，中途取消使用不会触发；单纯拥有 Profile 不会让非食物变为食物。
- 内置蘑菇煲基础提供 3 HP 储备与 Restorative，正常返还碗；南瓜派基础提供 1 HP 储备与 Invigorated。无 Recovery 的食物仍为零恢复，不因 Variety 凭空回血，原版营养保留。
- 上限固定为 **20 HP**，超出部分不进入储备。满血时保留储备、不消耗、不衰减，但清零恢复计时，不能预先积累瞬间治疗。
- 储备中不足 1 HP 的尾数允许在下一周期兑现。接近满血时只扣实际恢复的 HP；其他 Mod 若拒绝此次 `heal`，不扣未生效的储备，也不立即连续重试。
- 小于 `0.000001 HP` 的储备忽略/归零，避免低于实际生命精度的尾数永久挡住自然恢复。
- 新食物可追加储备，不重置正在进行的 Food 周期；从自然恢复切到 Food 时开始新周期。
- Variety 只加速 Well-fed，不改变 Stable 周期或自然恢复每 HP 成本。饮食改变导致间隔变化时保留进度，最多在下个 tick 兑现一次。已获得的储备不追溯加奖或扣减。
- 创造/旁观模式不获得或兑现储备，已有储备保留、计时归零。返回生存后可继续使用。
- 死亡立即清零，死亡重生不复制。维度切换、活着离开末地、断线重连和正常停服保存保留状态。没有跨玩家静态状态。
- 满血/不满足恢复条件会清零进度；正常保存则记录当时的储备、模式和周期进度。`/reload` 不回溯改变已吃下的储备，只影响之后的进食。

参数集中在 `food/recovery/RecoveryBalance.java`。本阶段不引入额外配置框架；这些是原型平衡值，后续按体验调整。

## 持久化与客户端边界

使用 Fabric Data Attachment `buildup_vitals:recovery`，通过 Codec 持久化 `reserve`、`progress` 和 `mode`。这是首次新增的状态格式；没有迁移或覆盖旧玩家字段。缺失状态为零；非法数值由 Codec 拒绝，日志遵循 Fabric 附件加载行为。周期进度在读取时限制到对应模式范围。

附件仅服务端保存，不同步给客户端。实际生命使用原版同步；本阶段调试查询在服务端执行，因此客户端无需拥有权威恢复数值。

## 命令

需要 Game Masters 权限（通常 OP 2 / 单人开启命令）：

```text
/buildupvitals food profile minecraft:mushroom_stew
/buildupvitals recovery
/buildupvitals recovery <player>
```

第二、三个命令只读，显示储备、当前模式、周期进度、主要增益 ID、剩余 tick、生命、Hunger 和 Saturation。Food / Well-fed 周期分母显示当前实际间隔。满血时模式显示 `NONE`，即使还有储备；刚进食时可短暂显示 `FOOD`，下一 tick 判定满血后归为 `NONE`。

## 注入范围

| Mixin | 26.3 目标 | 原因与边界 |
|---|---|---|
| `FoodRecoveryMixin` | `FoodProperties.onConsume(Level, LivingEntity, ItemStack, Consumable)` 尾部 | 当前 Fabric 无完成进食事件；原版已补营养、尚未缩减食物栈，仅处理 ServerPlayer |
| `NaturalRecoveryMixin` | `FoodData.tick(ServerPlayer)` 中唯一的 `Boolean.booleanValue()` 与方法尾部 | 只关闭本方法自然恢复分支；耗竭和饥饿分支照常执行，尾部运行控制器 |
| `PeacefulRecoveryMixin` | `ServerPlayer.tickRegeneration()` 内的 `ServerPlayer.heal(float)` 调用 | 只取消和平难度的额外生命恢复，保留补充食物行为 |

不改写 `LivingEntity.heal`、伤害方法或整段 `FoodData.tick`。注入要求命中，版本变化时显式失败。若其他 Mod 也重写这些自然恢复位置，仍需单独进行兼容验证。

## 人工体验

在测试世界中准备蘑菇煲、普通面包、治疗药水和金苹果；生存模式下受伤并消耗一些 Hunger。

1. 吃蘑菇煲后查询储备，基础增加 3 HP（多样饮食可再增加最多 15%）并获得 Restorative；观察约 2 秒恢复半颗心，而非瞬间恢复。
2. 储备兑现期间受到攻击，继续观察恢复，不应重新等待“脱战”。
3. 储备耗尽后，用高饱食/高饱和食物观察自然恢复，普通 Well-fed 约 4 秒半颗心，多样饮食可稍快。
4. 使用治疗药水和金苹果，确认原版治疗及效果；将储备吃到满血后检查其保留。
5. 退出重进、切维度，查询储备；死亡后应为零。
6. 本轮验收机制正确性；连续战斗节奏、料理收益感和参数微调按用户要求推迟到主要功能与机制基本完成后。
