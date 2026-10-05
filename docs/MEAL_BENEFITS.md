# Food Quality 与 Meal Benefit（Stage 4）

这是 Buildup 独立维护的料理状态，不注册原版 MobEffect，也不添加右上角药水图标或新 HUD。所有实际效果由服务端决定；原版和第三方药水效果不占用这个槽位。

## 授予与四档 Quality

生存/冒险玩家完成食物消费时，服务端查找当时的 Food Profile。只有显式指定已实现的 `meal_benefit` 才授予增益；Quality 不自动赋予增益、不重写 Hunger/Saturation，也不乘算 Recovery 总量。

| Quality | 增益基础时长（20 TPS） |
|---|---|
| Basic | 600 tick / 30 秒 |
| Prepared | 1200 tick / 60 秒 |
| Meal | 2400 tick / 120 秒 |
| Feast | 3600 tick / 180 秒 |

Quality 为数据包明确指定的等级，不按材料数量推断。示例苹果 Basic 没有增益，蘑菇煲 Meal 提供 Restorative，南瓜派 Prepared 提供 Invigorated。数据包可为其他等级配置显式增益；未适配食物仍可靠地使用原始营养。

Stage 4 在本次进食写入 [Diet Memory](DIET_MEMORY.md) 后，用新的 Variety 为时长增加最多 10%，向下取整到 tick。最终仍受 Stage 3 的 **3600 tick 总上限**约束，因此 Feast 已到上限时不会再延长。仅奖励超出基础的部分，不因重复饮食缩短基础时长，不回溯重算已有增益。

这些参数只用于建立可验证机制，不代表完成平衡。按用户 2026-10-05 的要求，战斗节奏、增益收益感和数值微调留到主要功能和机制基本完成后统一进行。

## 单槽位与生命周期

- 同时最多一个主要增益，不叠等级。再次吃同类取 `max(当前剩余时间, 新食物含 Variety 奖励的时长)`：可以刷新或延长，不缩短，不无限累加。
- 不同已实现类型立即替换，采用新食物的完整时长。无增益、未知 ID、实验 Steady 均不清除或刷新已有增益。
- 每服务端食物 tick 倒计时，到零移除。满血仍倒计时；离线、暂停且不 tick 时不经过时间。
- 创造/旁观不获得增益，已有增益不产生实际效果，但在线 tick 仍倒计时。
- 死亡立即清除，死亡重生不复制；正常保存、重连、跨维度和活着替换玩家（如离开末地）保留。
- `/reload` 只改变未来进食结果，不重算已经授予的类型和剩余时间。
- Feast secondary modifier 本阶段未启用。

## 已实现与保留类型

### Restorative / 调养

`buildup_vitals:restorative` 将 Food Recovery 周期由 50 tick 缩短至 40 tick；每次仍最多兑现 1 HP，只扣实际治疗量。不增加储备、不增加总治疗量、不加速自然恢复。

增益获取、替换和到期均保留已有 Food 周期进度。若进度已经超过新的短周期，下个 tick 最多兑现一次；不会进食即治疗或一次补发多个周期。Stage 2 的恢复附件格式和 0–49 tick 的保存范围保持不变。

### Invigorated / 精力充沛

`buildup_vitals:invigorated` 将指定活动的 Exhaustion 乘以 `0.9`。覆盖原版疾跑、普通/疾跑跳跃、游泳及水中移动；行走和潜行的零耗竭仍为零。26.3 攀爬只记录距离，本身没有 Exhaustion，本模组不额外增加消耗。

Mixin 仅改变 `ServerPlayer.checkMovementStatistics(DDD)V` 中 6 个和 `jumpFromGround()V` 中 2 个 `ServerPlayer.causeFoodExhaustion(F)V` 调用参数，要求精确命中数量。不包装总耗竭入口；其他活动/外部直接施加的耗竭、自然恢复的每 HP 6 Exhaustion 均保留原成本。

### Steady / 安适（实验，未启用）

注册 `buildup_vitals:steady` 类型用于表达保留的机制方向，但不授予、不占槽、不产生效果。当前缺少与其他料理模组明确区分的“削峰”规则，按 Stage 3 计划保留实验状态。引用它会产生数据警告，其他 Profile 字段仍生效。

## 数据、保存与调试

Profile 格式见 [FOOD_PROFILES.md](FOOD_PROFILES.md)。未知/实验引用保留在查询结果中，并显示 `benefit_available=false`；加载时警告包含文件、数据包和 `$.meal_benefit`。类型注册表为内置固定表，不允许数据包定义任意 Java 行为。

新增持久化附件 `buildup_vitals:meal_benefit`，字段为可选 `type` 和 `remaining_ticks`（0–3600）。缺失附件视为空；缺少类型或时间为零归一为空，越界值由 Codec 拒绝。已保存但不可用的类型在下个 tick 清除。旧 `buildup_vitals:recovery` 格式不变。

不向客户端同步自定义状态；真实生命与饱食走原版同步。管理员可在服务端只读查询：

```text
/buildupvitals food profile minecraft:pumpkin_pie
/buildupvitals food profile minecraft:mushroom_stew
/buildupvitals recovery
/buildupvitals recovery <player>
```

后两项显示 `meal_benefit`、`remaining_ticks` 与恢复周期。没有增益时显示 `none` / `0`。用 `/buildupvitals diet [player]` 查询饮食倍率；Tooltip 与 HUD 属于后续阶段。
