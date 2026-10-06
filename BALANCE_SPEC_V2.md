# Buildup Vitals — Balance Spec v2

> 文档状态：Stage 7.5 正式平衡规范
> 目标平台：Fabric 26.3
> 适用范围：Recovery、Food Recovery、Hydration、Consumption、Overeating、Meal Benefit
> 说明：本文给出第二版明确的设计目标和原型数值。除特别标注为“原则锁定”的内容外，具体数值仍允许在 Stage 7.5 实机测试后进行小幅调整。

---

# 1. 本轮平衡目标

Stage 7 的系统架构已经成立，但第一版数值在实际游玩中存在明显问题：

- 高 Hunger / 高 Saturation 时自然恢复远慢于原版；
- 玩家处于半血甚至残血时，即使饱食状态很好也缺乏有效恢复能力；
- Food Recovery 的兑现速度过慢，正式料理在实机中存在感不足；
- Food Recovery 在 Hunger 满时受原版“不能继续进食”规则限制，无法稳定承担主动恢复职责；
- Hydration 的默认数值过于保守，大量普通食品为 `0 / 0`；
- TWT2 自身的 Quenched 治疗与 Buildup Recovery 存在并行治疗、未来叠加超模的风险；
- Meal Benefit 虽有机制，但由于不是正式状态效果，玩家感知过弱。

因此 v2 的总体目标调整为：

> **Buildup Vitals 的总恢复节奏应与原版处于相近量级，但将原版高度集中于 Saturation 快速回血中的价值，重新分配到 Saturation、Food Recovery、Meal Benefit、Hydration 和饮食多样性之间。**

目标不是削弱原版恢复，而是重新组织恢复来源，让玩家能明显感受到：

- 高 Saturation 仍然有价值；
- 正式料理确实能治疗；
- 饮食状态良好会更舒服；
- 水和含水食物有明确区别；
- 小零食、高质量料理和普通口粮拥有不同使用场景；
- 玩家不研究系统也不会突然变得很难生存。

---

# 2. 原版恢复作为数学基准

Minecraft 原版高饱和自然恢复应继续作为 Buildup 的数学锚点。

概念上，原版高饱和恢复具有两层逻辑：

```text
Hunger = 20
Saturation > 0
玩家受伤
↓
进入快速自然恢复
```

高 Saturation 时，恢复速度可接近：

```text
约每 10 tick 触发一次
```

且恢复量与当前 Saturation 有关。

当无法使用快速饱和恢复后，Hunger ≥ 18 时进入较慢自然恢复：

```text
约每 80 tick 恢复 1 HP
```

Buildup v2 不再把这两个档位整体大幅削弱。

---

# 3. Natural Recovery v2

## 3.1 高 Saturation 恢复

原则锁定：

> 高 Saturation 状态下，自然恢复应接近原版，但略慢于原版峰值。

Stage 7.5 Recovery Balance Hotfix 当前值：

```text
触发间隔：11 tick
```

即：

```text
约 0.55 秒一次
```

恢复量建议采用与 Saturation 连续关联的模型：

```text
heal = min(currentSaturation, 6) / 6
```

示例：

| Saturation | 单次恢复 |
|---:|---:|
| ≥ 6 | 1.0 HP |
| 4 | 0.667 HP |
| 3 | 0.5 HP |
| 2 | 0.333 HP |
| 1 | 0.167 HP |

这样可以避免旧版：

```text
Saturation >= 6
↓
突然进入 Well-fed

Saturation < 6
↓
突然掉回很慢的恢复
```

这种明显的二段式断层。

v2 应尽量让 Saturation 对恢复的贡献是连续的。

本轮仅将间隔从 12 调为 11 tick，恢复量曲线不变。以初始 10/20 HP、Hunger=20、Saturation=20、Exhaustion=0、无额外治疗和活动为对照：原版回满为 100 tick，当前普通状态为 110 tick（+10%）。单份食物储备优先支付统一治疗脉冲，并不另叠加一条治疗通道；在这个对照中提高到 1 HP 不会再缩短总时间。

±10% 是上述满营养对照的结果，不代表所有初始营养。Saturation=15 时，原版需140 tick，本模组无食物需154 tick，已有1 HP食物储备则需121 tick：储备免于自然恢复耗竭，节省的营养会延后饱和恢复减速，因此较原版快13.6%。Saturation=12.8 时同三组为310、314、245 tick，含食物约快21%。本轮保留这一既有来源差异，不通过新机制消除它；完整计算、限制和运行验证见版本目录的 `docs/RECOVERY_BALANCE_HOTFIX.md`。

## 3.2 Stable Recovery

当玩家：

```text
Hunger >= 18
```

但已经不满足高 Saturation 快速恢复时：

```text
1 HP / 80 tick
```

即：

```text
半颗心 / 4 秒
```

该档位与原版慢速自然恢复保持接近。

Stable Recovery 的职责只是：

> 提供可靠 Vanilla baseline。

它不负责表现 Buildup 的主要特色。

## 3.3 Natural Recovery 上限

Buildup 自己的自然类 Vitals 增益可以缩短恢复间隔，但原则上：

```text
自然恢复最快不低于 10 tick / HP 等效速度
```

也就是：

> 良好状态可以让玩家恢复到接近原版峰值，但不应靠 Vitals 普通正反馈突破原版峰值并无限叠乘。

特殊药水、信标、金苹果等独立外部治疗不受此限制。

---

# 4. Natural Recovery 与 Exhaustion

自然恢复继续消耗 Hunger / Saturation 资源。

推荐继续以原版恢复代价作为基准。

当前 Buildup 的：

```text
NATURAL_EXHAUSTION_PER_HP = 6
```

可以继续作为第二版原型起点。

如果采用：

```text
heal = min(Saturation, 6) / 6
```

则 Exhaustion 应按实际成功恢复的 Health 计算，而不是固定按一次完整 1 HP 计算。

原则：

> 恢复多少，支付多少对应恢复成本。

---

# 5. Food Recovery v2

Food Recovery Reserve 保留。

核心原则继续为：

```text
Food consumed
↓
Recovery Reserve
↓
逐渐转化为实际 Health
```

不改回高额瞬间回血。

## 5.1 普通 Food Recovery 速度

旧版：

```text
1 HP / 50 tick
```

过慢。

v2 第一版目标：

```text
1 HP / 12 tick
```

即：

```text
约 0.6 秒恢复 1 HP
```

## 5.2 Restorative

`buildup_vitals:restorative` 的核心效果改为：

```text
Food Recovery Interval:
12 tick → 10 tick
```

即：

```text
普通：1 HP / 0.6 秒
Restorative：1 HP / 0.5 秒
```

Restorative：

- 不增加 Recovery Reserve 总量；
- 不直接创造 Health；
- 不作用于药水等外部治疗；
- 只优化 Food Recovery 的兑现效率。

## 5.3 Food Recovery 不得拖慢 Natural Recovery

原则锁定：

> **拥有 Recovery Reserve 不得让玩家比原本应该拥有的自然恢复更慢。**

因此当前 Food 模式不应简单无条件覆盖 Natural 模式。

建议：

```text
effectiveFoodInterval =
min(
    foodRecoveryInterval,
    currentNaturalRecoveryEquivalentInterval
)
```

或者使用等价调度机制实现。

例：

```text
玩家高 Saturation：
Natural = 11 tick
Food = 12 tick

→ Food Reserve 随自然恢复按 11 tick 兑现
```

若未来：

```text
Natural = 10 tick
Food = 12 tick
```

则：

```text
Food Reserve 应至少以 10 tick 等效速度兑现
```

不能出现：

> “因为吃了一份料理，反而从更快的自然恢复被切到更慢的 Food Recovery。”

---

# 6. Food Recovery 的战斗规则

原则锁定：

> **受到攻击不会暂停、清零或延迟已经获得的 Food Recovery Reserve。**

不实现：

- Combat Lock；
- 受击暂停；
- 脱战计时；
- 受击重置 Food Recovery Progress。

原因：

> Food Recovery 本身已经通过进食动作和渐进式兑现承担平衡成本，再因受击暂停只会破坏战斗节奏。

---

# 7. Food Recovery 数值规范

后续所有原版、Farmer's Delight、Kaleidoscope Cookery 及其他食物兼容，优先依据本节确定 Recovery。

单位：

```text
2 HP = 1 颗心
```

官方平衡数据允许 `recovery.health = 0`，表示不提供直接 Food Recovery；只要提供，**最小非零单位必须为 1 HP（半颗心）**。

优先使用清晰的整数档位：`1 / 2 / 3 / 4 / 5 / 6 HP`。不使用 0.5 HP 官方基础值，也不把所有旧小数机械向上取整：干燥零食、普通生肉生鱼和特殊用途物品可保留或调整为 0，熟制单品和普通水果蔬菜面包以 1 为主。

这是官方数据的设计约束，Schema v1 仍接受第三方非负有限小数，保持旧数据包兼容；Variety 加成、缺血裁剪、储备尾数以及自然饱和恢复仍可产生小数，不在运行时强制取整。

不鼓励出现没有实际玩法意义的：

```text
1.37 HP
2.83 HP
```

## 7.1 通用分级

| 食物定位 | Recovery 建议值 |
|---|---:|
| 真正干燥 / 极轻零食 | 0 HP；明确值得恢复时至少 1 HP |
| 普通生肉 / 生鱼、风险明显的生食 | 0 HP |
| 新鲜水果 / 蔬菜 / 面包 | 1 HP |
| 普通熟制单品 | 1 HP |
| 简单加工食品 | 1～2 HP |
| Prepared | 1～2 HP；干燥曲奇例外为 0 |
| 普通 Meal | 3 HP |
| 丰盛 Meal | 4 HP |
| Feast | 4～6 HP |
| 特殊高价值料理 | 原则上不超过 6 HP |
| 已有强治疗语义的特殊物品 | 可不给额外 Recovery |

## 7.2 原版参考

推荐第二版原型：

| 食物 | Recovery |
|---|---:|
| 生牛肉 | 0 |
| 牛排 | 1 |
| 生猪排 | 0 |
| 熟猪排 | 1 |
| 生羊肉 | 0 |
| 熟羊肉 | 1 |
| 生鸡肉 | 0 |
| 熟鸡肉 | 1 |
| 生兔肉 | 0 |
| 熟兔肉 | 1 |
| 生鳕鱼 / 生鲑鱼 / 热带鱼 | 0 |
| 熟鳕鱼 / 熟鲑鱼 | 1 |
| 苹果 | 1 |
| 胡萝卜 / 金胡萝卜 | 1 |
| 甜菜根 / 马铃薯 | 1 |
| 西瓜片 / 甜浆果 / 发光浆果 | 1 |
| 面包 | 1 |
| 曲奇 | 0 |
| 紫颂果 | 0（保留独立传送用途） |
| 干海带 | 0 |
| 南瓜派 | 2 |
| 蘑菇煲 | 3 |
| 甜菜汤 | 3 |
| 兔肉煲 | 4 |

金苹果、附魔金苹果等已有完整治疗和战斗语义的食物：

> 不应仅因为稀有而自动附加高额 Food Recovery。

---

# 8. Hydration v2

Hydration 的设计目标从：

> “只有少数明显含水食物恢复口渴”

调整为：

> **绝大多数普通食物都可以有不同程度的 Hydration；真正干燥食品才应是 0。**

因此：

```text
0 Hydration
```

应被视为一种明确特征，而不是默认答案。

---

# 9. Pure Water 基准

TWT2 当前水瓶标准为：

```text
6 Thirst / 8 Quenched
```

Buildup v2 官方平衡目标改为：

```text
Pure Water Bottle:
10 Thirst / 8 Quenched
```

设计理由：

```text
0 / 20 Thirst
↓
喝两瓶纯水
↓
基本回满
```

纯水应成为：

> 游戏中最强的直接补充 Thirst 的常规手段之一。

但不必同时拥有最高 Quenched。

---

# 10. Hydration 分级规范

| 类型 | Thirst | Quenched |
|---|---:|---:|
| Pure Water 基准 | **10** | **8** |
| 顶级补水饮品 | 8～10 | 8～12 |
| 普通饮品 | 6～8 | 5～9 |
| 高含水汤类 | 6～8 | 4～7 |
| 普通汤 / 炖菜 | 5～6 | 3～5 |
| 高含水果 | 4～5 | 2～4 |
| 普通水果 | 3～4 | 1～3 |
| 生鲜蔬菜 | 2～3 | 1～2 |
| 生肉 / 生鱼 | **2** | 0～1 |
| 对应熟肉 / 熟鱼 | **1** | 0～1 |
| 普通偏干食品 | 1 | 0 |
| 真正干燥食品 | **0** | **0** |

---

# 11. 生食与熟食的 Hydration 差异

原则锁定：

> **对应的普通生食应比熟食略微更补水。**

例如：

```text
生牛肉：2 / 0
牛排：1 / 0
```

```text
生鳕鱼：2 / 1
熟鳕鱼：1 / 0
```

```text
生兔肉：2 / 0
熟兔肉：1 / 0
```

这不是为了让生食整体优于熟食。

熟食仍然通常拥有：

- 更好的 Hunger；
- 更好的 Saturation；
- 更好的 Recovery；
- 更低风险。

生食只体现：

> 保留更多水分。

---

# 12. 水与其他饮料的定位

纯水应优先承担：

> 快速填充当前 Thirst。

高级饮料可以在：

> Quenched、Meal Benefit、Flavor、其他功能

上更有优势，但不应全面碾压纯水。

例如：

```text
Pure Water:
10 / 8

Fruit Juice:
8 / 10

Tea:
6 / 8

Soup:
6 / 4
```

这样：

```text
水
→ 最强直接补水之一

果汁
→ 更强持续性

汤
→ 同时覆盖 Hunger + Recovery + Hydration
```

---

# 13. TWT2 数据优先级 v2

当前 Stage 7 优先级：

```text
TWT2 Config
→ TWT2 Drinks Datapack
→ Buildup Food Profile
```

会导致大量 Buildup 官方 Hydration 值无法真正生效。

Stage 7.5 应重新设计为：

```text
TWT2 blacklist
↓
Buildup explicit profile
↓
TWT2 explicit / default item values
↓
TWT2 generic tag / keyword fallback
```

目标：

> 安装 Buildup 后，由 Buildup 的官方平衡 Profile 真正控制已适配食物的数值。

同时：

> 整合包作者仍然可以通过更高优先级 Buildup Data Pack 覆盖官方 Profile。

---

# 14. TWT2 Quenched Healing 协调

TWT2 1.6.2 默认：

```text
quenchedHealthRegen = 0.5
```

其 Quenched 可以独立产生治疗。

Stage 7.5 不应继续允许：

```text
Buildup Natural Recovery
+
Buildup Food Recovery
+
TWT2 independent Quenched Healing
```

完全平行叠加。

目标：

> Buildup 安装后，由 Buildup Recovery Controller 统一协调 Quenched 对恢复的贡献。

推荐语义：

```text
Thirst = Full
Quenched > 0
↓
Natural Recovery Speed +15%
```

该奖励：

- 不创建独立 Health；
- 不独立运行一套治疗定时器；
- 不作用于药水；
- 受 Natural Recovery 最快 10 tick 等效上限约束。

实现时应优先通过兼容层协调，不应直接永久修改用户配置文件。

---

# 15. 满 Hunger 继续进食

原则锁定：

> **正常 Food Item 即使 Hunger = 20，也允许继续进食。**

原因：

- Food Recovery 需要主动使用入口；
- Meal Benefit 不应被 Hunger 满值锁死；
- 含水食物应能在 Hunger 满时继续用于 Hydration；
- Variety 不应要求玩家先强行消耗 Hunger。

这项能力是 Buildup Food Recovery 完整闭环的重要部分。

---

# 16. Overeating

允许满 Hunger 继续进食以后，需要防止：

> 玩家无限狂吃高 Recovery 食物。

Buildup v2 使用：

# Overeat Load

内部中文概念：

> **过量进食负荷**

它不是常驻 HUD 条。

---

# 17. Overeat Load 计算

每次完成进食：

```text
missingHunger =
20 - hungerBeforeEat

overflowNutrition =
max(
    0,
    foodNutrition - missingHunger
)

OvereatLoad += overflowNutrition
```

只计算：

> 本次无法被 Hunger 正常接受的 Nutrition。

因此：

### 满 Hunger 吃牛排

假设 Nutrition = 8：

```text
OvereatLoad += 8
```

### 满 Hunger 吃曲奇

假设 Nutrition = 2：

```text
OvereatLoad += 2
```

### Hunger = 12 吃牛排

缺少 8 点 Hunger：

```text
overflowNutrition = 0
```

正常补充 Hunger 不应受到过量进食惩罚。

---

# 18. Overeat Load 原型数值

第一版目标：

```text
Soft Warning Threshold = 48
Overfull Threshold = 64
Maximum Load = 80
Recovery Threshold = 32
Decay = 1 / 40 tick
```

即：

```text
每 2 秒消化 1 点
```

满 Hunger 连吃牛排：

```text
1 个 → 8
2 个 → 16
3 个 → 24
4 个 → 32
5 个 → 40
6 个 → 48
7 个 → 56
8 个 → 64
```

约第 8 个牛排正式进入严重过量进食。

这个尺度符合：

> 正常补吃、为了 Recovery 多吃一两份料理不会立即被惩罚；

但：

> 明明完全饱了还连续塞大量食物，会得到明显负反馈。

---

# 19. Overfull

正式负面状态：

```text
ID:
buildup_vitals:overfull

中文：
积食

英文：
Overfull
```

类型：

```text
Harmful MobEffect
```

---

# 20. Overfull 效果

原则目标：

> 对“继续靠吃东西获得额外价值”产生明显限制，但不要把玩家整体战斗能力废掉。

推荐：

| 行为 | Overfull |
|---|---|
| Hunger 正常恢复 | 保留 |
| Saturation 正常恢复 | 保留 |
| Hydration 正常恢复 | 保留 |
| 新增 Food Recovery Reserve | **禁止** |
| 已有 Recovery Reserve | **暂停兑现** |
| 新 Meal Benefit | **禁止** |
| 刷新 Meal Benefit | **禁止** |
| Natural Recovery | 保留 |
| 药水 / 信标 / 金苹果等外部治疗 | 保留 |
| 继续进食 | 允许 |
| 继续进食速度 | 约慢 25% |

不建议附加：

- Slowness；
- Weakness；
- Mining Fatigue；
- Nausea；

除非未来实机证明当前负反馈仍不足。

Overfull 应主要惩罚：

> **继续滥用食物**

而不是：

> 玩家整个人突然变得无法战斗。

---

# 21. Overfull 解除

当：

```text
OvereatLoad < 32
```

时移除 Overfull。

不要求额外道具、睡觉或现实时间冷却。

玩家只需要：

> 停止继续大量进食，让身体自然消化。

---

# 22. Overeat Warning

当：

```text
OvereatLoad >= 48
```

且尚未进入 Overfull 时：

建议首次给出一次轻量提示。

例如：

```text
你已经吃得很撑了……
```

可使用：

- Action Bar；
- 很轻的提示音；
- 不常驻的短提示。

不增加新的“胃容量条”。

---

# 23. Consumption Speed

Stage 7.5 正式加入食物进食速度差异。

目标：

> 让小零食和轻量食物拥有独立价值。

只使用三个基础速度档。

| Tier | 相对速度 | 32 tick 基准 |
|---|---:|---:|
| `normal` | 1.0× | 32 tick / 1.6s |
| `quick` | 1.5× | 约 21 tick / 1.05s |
| `fast` | 2.0× | 16 tick / 0.8s |

不鼓励给 Data Pack 作者暴露任意浮点倍率作为主接口。

---

# 24. 原版进食速度参考

## Fast / 2×

推荐：

- 干海带；
- 曲奇；
- 甜浆果；
- 发光浆果；
- 西瓜片。

## Quick / 1.5×

推荐：

- 苹果；
- 胡萝卜；
- 甜菜根；
- 生马铃薯；
- 部分轻量水果 / 蔬菜。

## Normal / 1×

推荐：

- 面包；
- 肉类；
- 鱼类；
- 南瓜派；
- 汤；
- 炖菜；
- 正式料理。

---

# 25. Food Profile 扩展

建议 schema v2 或向后兼容扩展：

```json
{
  "consumption": {
    "speed": "fast"
  }
}
```

允许：

```text
normal
quick
fast
```

未声明时：

> 完全保留物品原本的 Consumable duration。

Buildup 不应无条件统一第三方食物进食时间。

---

# 26. Snack Role

Stage 7.5 应正式承认：

> **Snack 是一种玩法定位，而不是“差一点的正餐”。**

典型小零食：

```text
低 Hunger
低 Recovery
低 Overeat Load
进食速度快
可能提供少量 Hydration
```

正式 Meal：

```text
进食较慢
高 Recovery
Meal Benefit
较好的 Variety
可能较高 Hydration
```

二者不应形成单纯上下位关系。

---

# 27. Meal Benefit 改为正式 MobEffect

Stage 7 当前 Meal Benefit 通过自定义 Attachment 管理，不是正式 MobEffect。

Stage 7.5 改为：

> **注册完整 Minecraft MobEffect。**

第一批：

```text
buildup_vitals:restorative
调养 / Restorative

buildup_vitals:invigorated
精力充沛 / Invigorated

buildup_vitals:steady
安适 / Steady

buildup_vitals:overfull
积食 / Overfull
```

---

# 28. Meal Benefit 的表现方式

Meal Benefit 应：

- 有正式状态图标；
- 在 HUD 中可见；
- 在 Inventory Effects 页面可见；
- 有名称；
- 有简短说明；
- 可被 `/effect` 查询和调试；
- 能正确保存、同步、死亡清除。

但默认：

> **不注册 Potion Item，不加入酿造配方。**

状态效果和药水是两个概念。

---

# 29. Meal Benefit 粒子

推荐：

```text
showIcon = true
showParticles = false
```

玩家应能：

> 看见状态存在。

但不应因为吃饭：

> 身边一直冒药水粒子。

---

# 30. Main Meal Benefit 槽位

原有原则保留：

> 同一时间原则上只有一个 Buildup 主 Meal Benefit。

主增益：

```text
Restorative
Invigorated
Steady
```

规则：

### 同类型

```text
刷新 / 延长
```

### 不同类型

```text
新 Benefit 替换旧 Benefit
```

`Overfull` 是 Harmful 独立状态：

> 不占主 Meal Benefit 槽。

未来可以考虑使用：

```text
#buildup_vitals:main_meal_benefits
```

Tag 统一管理互斥组。

---

# 31. Restorative v2

正式效果：

> 加快 Food Recovery Reserve 兑现。

目标：

```text
12 tick / HP
→
10 tick / HP
```

不增加总量。

---

# 32. Invigorated v2

继续保留：

> 降低特定活动产生的 Exhaustion。

第一版仍建议：

```text
约 -10%
```

仅作用于明确活动：

- Sprint；
- Jump；
- Swim；
- Climb 等已验证入口。

不作用于：

- Natural Recovery 成本；
- Hunger Damage；
- 任意第三方 addExhaustion；
- 所有通用 exhaustion 来源。

---

# 33. Steady

Stage 7.5 可以正式实现 Steady，但如果实现风险较高，可以只完成 MobEffect 注册并暂不投放到原版 Profile。

推荐未来语义：

> 平滑短时间 Vitals 消耗波动。

但不应简单复制：

- Nourishment；
- Saturation 不掉；
- 直接减伤；
- 自动回血。

若本阶段没有足够清晰的实现，应：

> 保持已注册但默认无原版食物授予，留到后续阶段。

---

# 34. Variety v2

Stage 7 已建立的 Variety 算法总体保留。

当前原则继续有效：

```text
基础食物价值 = 100%
多样饮食 = 额外奖励
```

不因为 Overeating 的加入改变：

- Hunger 基础值；
- Saturation 基础值；
- 单一饮食 baseline。

---

# 35. Variety 与 Recovery v2

当前：

```text
MAX_FOOD_BONUS = +15%
MAX_BENEFIT_BONUS = +10%
MAX_WELL_FED_BONUS = +7.5%
```

可以继续作为 v2 第一轮测试值。

但：

> Well-fed Bonus 应适配新的连续 Saturation 恢复模型，而不是继续绑定旧的 `80 → 75 tick` 二段式逻辑。

建议将 Variety 作为：

> 对当前自然恢复间隔的小幅乘算奖励。

并继续受到：

```text
Natural Recovery 最快 10 tick 等效上限
```

约束。

---

# 36. 数据驱动原则

以下内容尽量继续通过 Food Profile / Data Pack 定义：

- Quality；
- Recovery；
- Hydration；
- Diet；
- Variety Group；
- Traits；
- Meal Benefit；
- Consumption Speed；
- 可选 Hunger / Saturation override。

Overeat 的核心阈值和全局 Recovery 数学模型可以继续先作为 Core Balance 参数。

---

# 37. 第二版推荐原版 Profile 方向

Stage 7.5 应重做当前 40 种原版食物数据。

总体目标：

### 大部分普通食物

至少拥有：

```text
Recovery = 0 或至少 1 HP（优先整数）
Hydration >= 1
```

除非它本身明显干燥或特殊。

### 生食

通常：

```text
Recovery 较低
Hydration 较高
```

### 熟食

通常：

```text
Recovery 较高
Hydration 较低
```

### 水果

通常：

```text
Recovery 通常为 1 HP（特殊用途水果可为 0）
Hydration 3～5
```

### 零食

通常：

```text
Recovery 0 或 1 HP（干燥曲奇、干海带为 0）
Hydration 0～1
Consumption = fast
```

### 汤 / 炖菜

通常：

```text
Recovery 3～4
Hydration 5～8
Meal Benefit
Consumption = normal
```

---

# 38. 需要特别保护的原版语义

以下内容不应因为 v2 重构而被破坏：

- 金苹果原有效果；
- 附魔金苹果原有效果；
- 蜂蜜瓶清除 Poison；
- 可疑炖菜花朵效果；
- 腐肉 Hunger 风险；
- 生鸡肉 Hunger 风险；
- 河豚毒性；
- Chorus Fruit teleport；
- 容器返还；
- 原有 Food Component Hunger / Saturation，除非明确 override。

---

# 39. UI 与 Tooltip

Stage 7.5 至少需要同步更新 Tooltip，使玩家能理解新的核心信息。

建议显示：

- Recovery；
- Hydration；
- Quality；
- Meal Benefit；
- Consumption Speed（仅非 normal 时）。

例如：

```text
恢复 ♥ 1.5
补水 💧 4
快速进食
提供：调养
```

不要显示：

```text
Recovery Interval = 10 ticks
Overeat coefficient = 0.125
```

这些属于调试层。

---

# 40. Debug / Command

Stage 7.5 应扩展调试命令，至少能够观察：

```text
Recovery Reserve
Natural Recovery mode / interval
Overeat Load
Overfull state
Current Meal Benefit
Diet / Variety
TWT2 hydration integration state
```

建议：

```text
/buildupvitals recovery
/buildupvitals diet
/buildupvitals vitals
```

具体命令结构可由实现决定，但调试能力必须覆盖新状态。

---

# 41. 第二版体验验收目标

完成 Stage 7.5 后，应达到以下体验：

### 场景 A：满 Hunger、高 Saturation、半血

玩家无需额外吃东西，也应该：

> 明显且快速地恢复。

体验接近原版。

### 场景 B：满 Hunger、高 Saturation、半血，吃正式料理

允许继续吃。

吃下以后：

- 获得 Food Recovery；
- 恢复不会被受击打断；
- 不会因为 Food Reserve 反而降低原本 Natural Recovery 速度；
- 获得可见 Meal Benefit。

### 场景 C：满 Hunger 连续吃 1～2 份料理

允许。

不应立刻进入严重负反馈。

### 场景 D：满 Hunger 连续狂吃约 8 个牛排

进入：

> `Overfull / 积食`

之后：

- 不能继续通过食物获得 Recovery；
- Meal Benefit 不再新增 / 刷新；
- Food Reserve 暂停兑现；
- Natural Recovery 仍正常；
- 药水仍正常；
- 继续吃东西更慢。

### 场景 E：严重口渴

两瓶 Pure Water 基本回满 Thirst。

### 场景 F：吃生牛肉与牛排

生牛肉：

> Hydration 略高。

牛排：

> Recovery / Hunger / Saturation 更优。

### 场景 G：Cookie / Dried Kelp 等 Snack

明显比正餐吃得快。

但不能凭此成为最高效治疗食品。

### 场景 H：安装 TWT2

Quenched 不再作为完全独立的第二治疗器与 Buildup Recovery 无限制叠加。

---

# 42. 最终设计边界

Stage 7.5 完成后应坚持：

> **恢复速度接近原版，而恢复来源比原版更丰富。**

> **高 Saturation 依旧重要，但不再是唯一核心。**

> **食物回血是主动恢复手段，但不是瞬间药水。**

> **满 Hunger 可以吃东西，但长期滥用会积食。**

> **积食惩罚“狂吃”，不惩罚正常生存。**

> **绝大多数食物多少都能补水，真正干燥的食物才是 0。**

> **纯水是最好的直接补水手段之一。**

> **小零食的价值可以来自速度，而不是大数值。**

> **Meal Benefit 应当被玩家看见、理解和发现。**

> **状态效果可以真实存在，但不等于必须做成药水。**

---

# 43. 本文与 DESIGN_PRINCIPLES.md 的关系

`DESIGN_PRINCIPLES.md` 是项目长期设计总纲。

本文是：

> 当前版本更具体的第二版数值与机制规范。

Stage 7.5 完成后必须同步修改 `DESIGN_PRINCIPLES.md`，至少修正以下旧结论：

- 自然恢复“明显慢于原版”的旧描述；
- Stable / Well-fed 的第一版 6 秒 / 4 秒数值；
- Food Recovery 2～3 秒 / HP 的旧目标；
- Meal Benefit“不一定注册为真实 Status Effect”的旧描述；
- Hydration 大量食物允许为 0 的旧倾向；
- 增加满 Hunger 继续进食；
- 增加 Overeating / Overfull；
- 增加 Consumption Speed 差异化；
- 增加 TWT2 Quenched Recovery 协调原则。

同步后：

> `DESIGN_PRINCIPLES.md` 负责“为什么这样设计”，
> `BALANCE_SPEC_V2.md` 负责“当前具体如何平衡”。
