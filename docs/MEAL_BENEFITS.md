# Meal Benefit v2 与 Foreign Cuisine Effect

主要饮食增益正式注册为原版 MobEffect，由原版负责保存、同步和倒计时。HUD、背包效果卡显示图标与时间；食物授予默认无粒子。不注册 Potion 或酿造配方。

| ID（命名空间 buildup_vitals） | 中文 | 类型 | 行为 |
|---|---|---|---|
| restorative | 调养 | Beneficial | 食物储备周期 12→10 tick，不增加储备总量 |
| invigorated | 振奋 | Beneficial | 指定活动 Exhaustion ×0.9 |
| steady | 安适 | Beneficial | 已注册的 UI/互斥占位，无实际加成；官方 Profile 不投放 |
| overfull | 积食 | Harmful | 暂停食物储备、阻止新储备/增益、进食时长 ×1.25 |

三种原生主增益与已注册 Foreign Main Meal Benefit 互斥，包括命令授予和食物授予；Overfull 独立共存。未桥接的第三方效果和独立药水效果不占主槽。Steady 仍被 Profile 行为注册表标为 experimental：引用会警告并忽略该字段，其他 Profile 字段保留。可用 `/effect give` 查看其正式图标及互斥行为。

当前高饱和自然恢复基准为10 tick，储备本已按统一时钟取更快周期，因此调养或 Nourishment 不会在该档位再额外提速或另加治疗。其12→10 tick收益仍作用于无自然恢复或自然恢复更慢的食物兑现阶段。

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

`/buildupvitals recovery [player]` 显示当前主增益的真实 MobEffect 剩余 tick，包括超过 3600 的外来效果；无限效果为原版 `-1`。内部旧 `MealBenefitState` 仅用于兼容摘要和有上限的默认授予，不负责外来效果的存储或计时。普通 Tooltip 使用效果图标与短名称；背包效果卡悬停显示简短说明。详见 [CLIENT_FEEDBACK](CLIENT_FEEDBACK.md)。

## Foreign 主增益：Nourishment

FD Refabricated `26.3-3.6.27+refabricated` 的 `farmersdelight:nourishment` 注册为 Foreign 主增益，保留 FD ID、图标和原生 MobEffect，行为等效调养 + 振奋，不添加两个原生图标或额外治疗器。原生 600 / 1200 / 3600 / 6000 tick 和消费效果概率保留，不乘 Variety 时长；上面的 3600 上限只约束 Buildup 默认时长授予。没有原生效果来源但显式引用该 ID 的数据包食品使用 Quality 默认授予。

新异类立即替换；同类走原版刷新语义，不累加，不因更短料理缩短现有效果。Overfull 阻止首次授予/刷新，包括直接命令或其他模组调用；已有效果照常倒计时。食物触发过饱的当次也不能绕过限制。死亡、牛奶、自然到期和命令移除后没有残留语义状态。

FD 给原版蘑菇煲、甜菜汤和兔肉煲追加的 Nourishment 服从它们最终的 Buildup Profile，保留原有调养/振奋选择。FD 食物也可通过数据包改成原生主增益或无主增益。只拦截已桥接的 Nourishment 消费授予；其他独立效果保留。详见 [FD 兼容与验收](FARMERS_DELIGHT_COMPAT.md)。

[More Delight 26.09.16-26.3-fabric](MORE_DELIGHT_COMPAT.md) 的六种 Nourishment 料理复用该桥接，原生3600 tick不乘Variety。两种沙拉的Regeneration I / 100 tick保留为独立效果，额外Recovery为0，不占主增益槽。没有为附属重复注册效果或新增Mixin。

## Foreign Cuisine Effect 与 Vigor

Foreign Cuisine Effect 是独立于主槽的语义登记层，不创建替身MobEffect或第二份计时状态。标为Main的效果复用既有Foreign Main注册；Special Cuisine Effect可与主增益共存。Nourishment原桥接不变。

Cookery `kaleidoscope_cookery:vigor` 是Foreign Main，仅提供Invigorated的指定活动耗竭×0.9，不提供Restorative。保留原ID、图标、消费概率与持续时间，不额外显示振奋；与Nourishment及三个Buildup主增益双向互斥。同类刷新不累加，Overfull阻止首次授予及刷新，最终Profile可改成原生增益或无增益。无原生来源的显式Vigor引用才走Quality默认授予。

Warmth、Satiated Shield、Star Blessing、Crimson、Ghost、Warped、Tropical Strider、Mint、Dream、Void Erosion以及Tavern特殊效果不占主槽。它们按已审核预算生效；积食不统一删除这些能力，但阻止其新增食物储备，Shield另有积食禁用条件。

Effect Review Gate 是适配时的审核要求：核对目标版本的真实效果来源、总治疗量、防御倍率、持续时间及互相叠加；明确保留/桥接/替换结论，再运行模块与联合测试。并非允许玩家通过未经验证的通用配置任意转换第三方效果。深度行为只在精确支持版本启用；全部预算、版本和验收见 [Kaleidoscope Series](KALEIDOSCOPE_COMPAT.md)。
