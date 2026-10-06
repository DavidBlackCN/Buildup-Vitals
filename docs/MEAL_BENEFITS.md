# Meal Benefit v2（Stage 7.5）

主要饮食增益正式注册为原版 MobEffect，由原版负责保存、同步和倒计时。HUD、背包效果卡显示图标与时间；食物授予默认无粒子。不注册 Potion 或酿造配方。

| ID（命名空间 buildup_vitals） | 中文 | 类型 | 行为 |
|---|---|---|---|
| restorative | 滋养 | Beneficial | 食物储备周期 12→10 tick，不增加储备总量 |
| invigorated | 振奋 | Beneficial | 指定活动 Exhaustion ×0.9 |
| steady | 稳态 | Beneficial | 已注册的 UI/互斥占位，无实际加成；官方 Profile 不投放 |
| overfull | 积食 | Harmful | 暂停食物储备、阻止新储备/增益、进食时长 ×1.25 |

三种主要增益互斥，包括命令授予和食物授予；Overfull 独立共存。外部药水和第三方效果不占主槽。Steady 仍被 Profile 行为注册表标为 experimental：引用会警告并忽略该字段，其他 Profile 字段保留。可用 `/effect give` 查看其正式图标及互斥行为。

## 授予与时长

只有存活的生存/冒险玩家、未积食且 Profile 显式引用已实现增益时才授予。Quality 不自动创造增益，也不重写营养。

| Quality | 基础时长 |
|---|---:|
| Basic | 600 tick / 30 秒 |
| Prepared | 1200 tick / 60 秒 |
| Meal | 2400 tick / 120 秒 |
| Feast | 3600 tick / 180 秒 |

当次饮食记录后的 Variety 最多增加 10% 时长，向下取整，食物授予上限 3600 tick。同类取新时长和现有剩余时间的较大值，不无限累加；不会缩短管理员授予的更长或无限效果。异类立即替换。无增益/未知/实验引用不改变已有主增益。积食时既不替换，也不刷新。

原版实体 tick 推进时长，满血、创造/旁观仍正常倒计时；这些模式不获得 Buildup 实际加成。死亡立即清空，不复制到死亡重生玩家；活着的维度转移、保存、重连保留。离线不计时。重载只影响未来进食。

Restorative 只改变储备兑现效率，保留已有进度，不创造额外治疗。Invigorated 只作用于 `ServerPlayer.checkMovementStatistics(DDD)V` 的六个和 `jumpFromGround()V` 的两个原版 Exhaustion 调用：疾跑、跳跃、游泳及水中移动。原版行走/潜行的零成本、攀爬仅记距离的规则保持；自然恢复成本与第三方直接耗竭不打折。

## 旧存档迁移

保留 `buildup_vitals:meal_benefit` 附件注册及原 Codec 作为兼容入口。首次登录或服务端恢复 tick 遇到有效旧类型/时间，且没有现代主要效果时，转换为同类型、同剩余 tick、无粒子的 MobEffect，然后删除旧附件。已有现代效果优先；未知/实验/失效旧状态安全清除。之后只使用原版 MobEffect 保存和计时，不存在附件与效果双重倒计时。

`/buildupvitals recovery [player]` 显示当前主要增益与恢复信息；超过 3600 tick 或无限的管理员效果在此食物状态摘要中裁剪到 3600，原版效果自身时间不变。普通 Tooltip 使用效果图标与短名称；背包效果卡悬停显示简短说明。详见 [CLIENT_FEEDBACK](CLIENT_FEEDBACK.md)。
