# Buildup Vitals — Codex Implementation Plan

> 目标平台：Minecraft 26.3 / Fabric  
> 项目状态：Bootstrap / Pre-Alpha  
> Mod ID：`buildup_vitals`  
> Maven Group：`com.davidblackcn`  
> Java Package：`com.davidblackcn.buildupvitals`  
> 当前设计基准：`DESIGN_PRINCIPLES.md`

---

## 1. 本计划的用途

这份文档不是一次性“把整个 Mod 全部写完”的任务单，而是一份分阶段实施路线。

Codex 每次只执行一个 Stage。每个 Stage 完成后必须停止，不得自动进入下一阶段，并向人工验收者提交：

1. 本阶段完成内容；
2. 主要文件变更；
3. 构建与测试结果；
4. 已知问题、未完成项与风险；
5. 一份可直接照着执行的人工验收清单。

只有人工明确确认通过后，才进入下一 Stage。

任何阶段如果发现设计原则、Minecraft 26.3 API 或第三方 Mod API 与计划存在冲突，应先记录事实、给出最小修改建议并停止，不要擅自扩大项目范围。

---

# 2. 当前工程基线

以 FabricMC 官方 `fabric-example-mod` 的 `26.3` 分支为基线。

当前计划使用：

- Minecraft：`26.3`
- Java：`25`
- Fabric Loader：`0.19.5`
- Fabric API：`0.161.0+26.3`
- Fabric Loom：`1.18-SNAPSHOT`
- Gradle Wrapper：以官方 26.3 示例工程当前版本为准

版本号不是项目设计的一部分。每次正式开工前应检查 Fabric 官方 26.3 示例工程是否有兼容性更新；如果只是补丁级工具链更新，可同步后记录，不需要重新设计项目。

当前仓库骨架已经预设：

```text
mod id      = buildup_vitals
archive     = buildup_vitals
maven group = com.davidblackcn
package     = com.davidblackcn.buildupvitals
```

---

# 3. 总体目标

第一条完整开发线只实现以下 Gameplay Loop：

```text
玩家进食
  ↓
原版 FoodComponent 继续处理 Hunger / Saturation
  ↓
Buildup Food Profile 提供附加信息
  ├─ Quality
  ├─ Recovery
  ├─ Hydration
  ├─ Diet Categories
  ├─ Variety Group
  ├─ Food Traits
  └─ Meal Benefit
  ↓
Recovery Reserve / Meal Benefit / Diet Memory
  ↓
形成更缓慢但更有层次的生命恢复与饮食正反馈
```

在安装 Thirst Was Taken 2 时，再通过可选 Adapter 将 Food Profile 中的 Hydration 接入口渴系统。

第一条完整开发线结束后，应达到：

- 原版普通食物仍然可靠；
- 正式料理明显值得制作；
- 食物可以渐进恢复生命；
- 高 Hunger / Saturation 能提供缓慢自然恢复；
- 多样饮食只有正向或极轻的额外收益差异；
- 重复饮食不会削弱基础 Hunger / Saturation；
- 没有专门适配的第三方食物仍可正常使用；
- 数据包可以覆盖或扩展重要食物行为；
- 安装 Thirst Was Taken 2 后，食物可自然接入口渴/Quenched；
- 未安装 Thirst Was Taken 2 时，Mod 仍可独立正常运行。

---

# 4. 当前明确不做的内容

在本计划的 Core 完成前，不做：

- Oxygen / Air 深度重构；
- Mana / Stamina 等未来 Vitals；
- Farmer's Delight Nourishment 深度魔改；
- Kaleidoscope Cookery Satiated Shield 深度魔改；
- 复杂营养素系统；
- 食物保质期、腐败、温度等大型生存系统；
- 新增大量常驻 HUD 条；
- 通过“战斗状态”暂停 Food Recovery；
- 用原版 Status Effect 简单复制 Meal Benefit；
- 为未知 Mod 食物硬编码大量 Item ID；
- 在 Core 尚不稳定时提前设计跨加载器架构。

第一目标只做 Fabric 26.3。未来是否加入 NeoForge，应在 Fabric 版核心稳定后再判断。

---

# 5. 架构原则

## 5.1 服务器权威

凡是影响玩家真实数值的逻辑，默认由服务端权威处理，包括：

- Recovery Reserve；
- Natural Recovery；
- Diet Memory；
- Variety；
- Meal Benefit 的实际 Gameplay 效果；
- Hydration Adapter 的实际数值修改。

客户端主要负责：

- Tooltip；
- HUD 反馈；
- 可视化；
- 必要状态同步。

不要让客户端直接决定治疗、饮食评分或资源消耗。

## 5.2 优先事件与公共 API，Mixin 只做必要拦截

能使用 Fabric API、公开 Minecraft API 或稳定事件完成的工作，不要先用 Mixin。

需要改变原版 Hunger / Natural Regeneration 等无法通过现有事件干净完成的逻辑时，可以使用 Mixin，但每个 Mixin 必须目的单一，并尽量避免覆盖整段原版方法。

## 5.3 数据驱动优先

重要食物平衡数据放入 Data Pack。代码负责解释规则，不负责维护一长串具体食物 ID。

## 5.4 未适配内容必须可靠降级

任何未知食物首先应保留原 FoodComponent 行为，再使用 Buildup fallback。不能因为没有 Profile 就破坏食用行为。

## 5.5 不做“隐藏惩罚式平衡”

尤其固定以下规则：

- Food Recovery 不因受击暂停；
- 重复食物不降低基础 Hunger；
- 重复食物不降低基础 Saturation；
- Variety 的基础数学基准是 `1.0`，良好饮食向上奖励，而不是普通饮食先被压低；
- Meal Benefit 不要求玩家维护大量新资源条。

---

# 6. 建议模块结构

最终包结构可根据实现适度调整，但建议保持语义分层：

```text
com.davidblackcn.buildupvitals
├── BuildupVitals.java
│
├── api/
│   ├── food/
│   └── vitals/
│
├── food/
│   ├── profile/
│   ├── quality/
│   ├── recovery/
│   └── benefit/
│
├── diet/
│   ├── memory/
│   └── variety/
│
├── data/
│   ├── loader/
│   └── selector/
│
├── integration/
│   └── thirst/
│
├── player/
│   └── state/
│
├── network/
├── command/
├── mixin/
└── util/
```

客户端独立放置于 `src/client`：

```text
com.davidblackcn.buildupvitals.client
├── BuildupVitalsClient.java
├── hud/
├── tooltip/
├── network/
└── mixin/
```

不要在 Stage 0 就创建几十个空类。结构随实际实现逐步落地。

---

# 7. 数据模型初稿

## 7.1 FoodProfile

Food Profile 第一版至少应能表达：

```text
selector
quality
recovery
hydration
categories
variety_group
traits
meal_benefit
optional overrides
```

建议模型语义类似：

```json
{
  "quality": "meal",
  "recovery": {
    "health": 3.0
  },
  "hydration": {
    "thirst": 4,
    "quenched": 2
  },
  "diet": {
    "categories": ["protein", "vegetable", "grain"],
    "variety_group": "beef_stew"
  },
  "traits": ["soup", "warm"],
  "meal_benefit": "buildup_vitals:restorative"
}
```

这只是数据语义示例，不锁死最终 JSON Schema。

## 7.2 Selector

需要支持至少两类选择方式：

- 单 Item；
- Item Tag。

第一版应明确冲突优先级，例如：

```text
具体 Item Profile
>
更具体的内置兼容规则
>
Tag Profile
>
Fallback
```

同级冲突应有确定性行为并输出可诊断信息。

## 7.3 Food Quality

固定四档：

```text
BASIC
PREPARED
MEAL
FEAST
```

代码层使用稳定 ID / enum 或等价表示，数据层使用小写字符串。

## 7.4 Diet Categories

第一版内置语义：

```text
protein
grain
vegetable
fruit
dairy
sweet
```

不要把 `soup`、`drink`、`warm` 等混入 Category，这些属于 Trait。

---

# 8. Stage 0 — Bootstrap & Build Baseline

## 目标

建立一个真正可构建、可启动、无 Gameplay 修改的 Fabric 26.3 工程。

## 任务

1. 对照 FabricMC 官方 `fabric-example-mod` 26.3 分支同步完整 Gradle Wrapper，包括当前官方 wrapper jar；
2. 检查并更新当前 skeleton 的工具链版本；
3. 确认 Java 25；
4. 确认 `fabric.mod.json`、主入口和 client 入口；
5. 确认 Mod ID / Maven Group / Package；
6. 删除所有官方 Example Mod 示例 Mixin 和示例逻辑；
7. 保留空的主/客户端 Mixin 配置，仅在确实需要时添加具体类；
8. 确认 MIT License；
9. 保留 `DESIGN_PRINCIPLES.md` 与本 `PLAN.md`；
10. 执行构建；
11. 启动开发客户端；
12. 启动开发服务端，确认 Mod 可在 Dedicated Server 环境加载。

## 本阶段禁止

- 不实现 Food Profile；
- 不修改 Hunger；
- 不实现 Recovery；
- 不添加 Thirst Was Taken 2；
- 不提前创建大量空 API。

## 自动验证

至少完成：

```text
./gradlew clean build
./gradlew runClient
./gradlew runServer
```

Windows 环境使用等价 `gradlew.bat`。

## 人工验收

- [ ] Gradle 导入正常；
- [ ] Java 25 正确；
- [ ] 客户端能进入主菜单并创建/进入世界；
- [ ] Dedicated Server 能启动并识别 Buildup Vitals；
- [ ] 日志无 Example Mod 残留；
- [ ] 当前没有任何 Gameplay 行为变化；
- [ ] `DESIGN_PRINCIPLES.md` 和 `PLAN.md` 已入仓库。

## Stage Gate

完成后停止，提交报告，等待人工批准进入 Stage 1。

---

# 9. Stage 1 — Food Profile Data Foundation

## 目标

建立数据驱动的 Food Profile 读取、解析、匹配和调试基础，但暂时不改变真实 Gameplay。

## 任务

1. 设计第一版 JSON Schema；
2. 设计 Item / Item Tag selector；
3. 实现 datapack reload；
4. 实现 Profile registry / snapshot；
5. 实现冲突优先级；
6. 实现 fallback；
7. 允许 Reload 后完整替换旧快照，避免部分更新状态；
8. 为错误 JSON 提供清晰日志：文件、字段、原因；
9. 加入少量 Vanilla 示例 Profile 作为测试数据；
10. 实现开发调试命令，例如：

```text
/buildupvitals food profile <item>
```

能够显示最终命中的 Profile、来源和 fallback 信息。

## 推荐测试

为纯数据逻辑编写 JVM 单元测试，至少覆盖：

- enum / ID 解析；
- Item selector；
- Tag selector；
- Item 优先于 Tag；
- fallback；
- 无效字段；
- reload 后旧快照不残留。

## 本阶段禁止

- 不改变玩家 Hunger；
- 不回血；
- 不保存玩家 Diet Memory；
- 不实现 Meal Benefit；
- 不接入 Thirst Was Taken 2。

## 人工验收

- [ ] `/reload` 后 Profile 能更新；
- [ ] 修改测试 JSON 后无需重启游戏即可生效；
- [ ] Item 与 Tag 冲突结果符合设计；
- [ ] 删除 Profile 后 fallback 正常；
- [ ] 错误 JSON 不导致世界无法进入，并有明确日志；
- [ ] 未适配食物不受任何 Gameplay 影响。

## Stage Gate

完成后停止。

---

# 10. Stage 2 — Recovery Core

## 目标

第一次真正改变核心 Gameplay：建立 Food Recovery Reserve、Natural Recovery 重构与统一 Recovery Controller。

## 任务

### 10.1 玩家恢复状态

为玩家保存至少：

```text
recovery_reserve
recovery_tick_progress / cooldown
```

选择 Minecraft 26.3 / Fabric 当前合适的持久化方案。优先使用稳定公开 API；如果需要 Data Attachment、Component 或 Mixin NBT，请先检查 26.3 当前推荐方式再实现。

必须确保：

- 死亡后的行为有明确规则；
- 维度切换不丢失；
- 断线重连不异常；
- Integrated Server 和 Dedicated Server 一致。

首轮建议：Recovery Reserve 属于玩家短期身体状态，死亡时清零。

### 10.2 Food Recovery Reserve

Food Profile 中的 `recovery.health` 在玩家成功吃完食物时进入 Reserve。

原型目标：约每 2～3 秒兑现 1 HP。

明确禁止：

```text
玩家受击 -> 暂停 Food Recovery
```

受击、战斗状态或刚受到伤害均不得停止已获得的 Food Recovery。

### 10.3 Natural Recovery

重构原版高饱食自然恢复，使其变成较慢的背景恢复。

原型目标：

- Stable：约 1 HP / 6 s；
- Well-fed：约 1 HP / 4 s。

具体 Hunger / Saturation 阈值先做成集中常量或配置对象，便于后续平衡，不要散落在 Mixin 中。

### 10.4 Recovery Controller

避免 Food Recovery 与 Natural Recovery 形成无控制双重爆发。

至少保证：

- Reserve 有可兑现治疗时，Food Recovery 拥有清晰优先级；
- 无 Reserve 时自然恢复工作；
- Potion / Beacon / Golden Apple / 外部明确治疗效果不被拦截；
- 不改变伤害计算本身。

## 自动测试

尽量把 Recovery Controller 做成可独立测试的纯逻辑层。

覆盖：

- 满血时 Reserve 不错误丢失或无限堆积；
- 受击后仍继续恢复；
- Reserve 耗尽后自然恢复重新工作；
- Hunger / Saturation 不达阈值时自然恢复正确停止；
- 第三方直接 heal 调用不被错误吞掉。

## 人工验收重点

- [ ] 吃测试料理后不是瞬间回血；
- [ ] Reserve 按预期逐渐兑现；
- [ ] 被怪攻击后仍持续兑现；
- [ ] 原版满饱食不再高速拉满生命；
- [ ] 药水治疗仍正常；
- [ ] 金苹果仍正常；
- [ ] 玩家断线重连与切维度无异常；
- [ ] Dedicated Server 行为与单人一致。

## Stage Gate

完成后停止，人工重点体验“战斗节奏是否被破坏”。

---

# 11. Stage 3 — Food Quality & Meal Benefit Framework

## 目标

让 Basic / Prepared / Meal / Feast 第一次产生实际 Gameplay 区别，同时建立 Buildup 自己的 Meal Benefit 状态层。

## 任务

1. 完成四档 Quality 数据模型；
2. Quality 本身不直接重写 Hunger；
3. 建立 Meal Benefit registry / type；
4. 玩家同一时间只保留一个主要 Buildup Meal Benefit；
5. 同类刷新/延长，不同类替换；
6. Feast 可预留轻量 secondary modifier 接口，但本阶段可不启用；
7. Meal Benefit 不实现为简单原版 Status Effect 包装；
8. 首批实现：
   - Restorative；
   - Invigorated；
   - Steady（如果机制尚未足够明确，可先只完成框架与测试实现，不强行定稿）。

## 原型方向

### Restorative

提高 Recovery Reserve 的兑现速度，不增加最终总治疗量。

### Invigorated

轻度降低疾跑、跳跃、游泳、攀爬等特定活动产生的 Exhaustion，原型约 5%～10%。不要让其降低 Natural Recovery 自身产生的 Hunger 成本。

### Steady

目标是削弱短时间资源消耗峰值，而不是简单“降低所有 Saturation 消耗”。若无法在不重复 Farmer's Delight Nourishment 语义的前提下实现清晰机制，本 Stage 可以保留实验状态并记录，不要为了凑数强行完成。

## 人工验收

- [ ] Basic 食物仍然可靠；
- [ ] Prepared / Meal 能明显感受到附加价值；
- [ ] Benefit 不表现得像另一套药水；
- [ ] 同类 Benefit 正确刷新；
- [ ] 不同 Benefit 正确替换；
- [ ] 不出现右上角大量 Buildup Buff 堆叠；
- [ ] Restorative 不增加 Recovery 总量；
- [ ] Invigorated 幅度不至于让饱食系统失去意义。

## Stage Gate

完成后停止。

---

# 12. Stage 4 — Diet Memory & Dietary Variety

## 目标

实现“多吃不同类型有正反馈，长期只吃一种食物最多失去部分额外收益”的核心长期系统。

## 任务

### 12.1 Diet Memory

第一版记录最近 `10` 次有效进食，数据至少包含：

```text
food_id
quality
categories
variety_group
timestamp
```

### 12.2 Variety 算法

原则优先级：

```text
Category Diversity
>
Variety Group Diversity
>
Quality Weight
```

第一版数学基准必须满足：

```text
普通 / 单一饮食 ~= 1.00
丰富饮食       > 1.00
```

禁止把普通饮食默认压到 0.7～0.8 再称之为“恢复正常”。

### 12.3 重复衰减

重复食物只影响额外收益，例如：

- Recovery bonus；
- Variety contribution；
- Meal Benefit 持续时间或强度。

禁止修改基础 Hunger / Saturation。

原型最低额外收益倍率可从 `0.85～0.90` 区间测试。

### 12.4 Variety Bonus

首轮目标范围：

- Food Recovery：最高约 +10%～15%；
- Meal Benefit：最高约 +10%；
- Natural Recovery：最高约 +5%～10%。

不要在这一阶段增加攻击力、最大生命、护甲、移动速度等通用 RPG 奖励。

## 自动测试

至少覆盖：

- 十次窗口正确滚动；
- 同一 food id 重复；
- 不同 food id 但相同 variety_group；
- 多个 fruit 不会等价于跨多个 category；
- 多 Category Meal 的贡献；
- baseline 永不低于设计下限；
- Hunger / Saturation 与 Variety 完全解耦。

## 人工验收

准备至少三组测试饮食：

A. 连续牛排；  
B. 多种 Basic 水果；  
C. 肉 + 谷物 + 蔬菜 + 正式料理混合。

验收：

- [ ] A 仍能完全正常生存；
- [ ] B 有一定多样性收益但不能轻易达到最高档；
- [ ] C 明显拥有更好的额外收益；
- [ ] 三组之间没有夸张战斗力鸿沟；
- [ ] 切换饮食后旧记录自然被推出窗口；
- [ ] 不需要等待现实时间才能改变饮食状态。

## Stage Gate

完成后停止。

---

# 13. Stage 5 — Client Feedback & Tooltips

## 目标

让玩家能理解系统，但不增加新的“仪表盘式”管理负担。

## 任务

1. Food Tooltip 显示必要信息；
2. 显示 Quality；
3. 显示 Recovery；
4. 显示 Categories；
5. 显示 Meal Benefit；
6. Hydration 信息在 Thirst Adapter 完成后按可选兼容显示；
7. 为高级调试模式保留更详细数字；
8. 探索 Recovery Reserve 心形半透明预览，但如果实现复杂或与其他 HUD Mod 冲突，本 Stage 可只做调研/实验，不强制上线。

普通 Tooltip 目标是：

```text
正式料理
逐渐恢复 3 点生命值
提供「调养」饮食增益
蛋白质 · 谷物 · 蔬菜
```

而不是公开大量内部系数。

## 兼容注意

检查 AppleSkin 等常见 HUD / Food Tooltip Mod 的基本兼容，避免重复绘制相同信息或崩溃。

## 人工验收

- [ ] 普通玩家不看 Wiki 也能理解食物大致特点；
- [ ] Tooltip 不过度拥挤；
- [ ] Debug 信息与普通信息分离；
- [ ] 不新增无必要的常驻状态条；
- [ ] 关闭/不安装可选 HUD Mod 时仍正常。

## Stage Gate

完成后停止。

---

# 14. Stage 6 — Thirst Was Taken 2 Optional Integration

## 目标

在不把 Thirst Was Taken 2 变成硬前置的前提下，将 Food Profile 的 Hydration 接入真实口渴与 Quenched。

## 开工前调查

Codex 必须先阅读 Thirst Was Taken 2 当前 26.3：

- 官方源码；
- `fabric.mod.json` 中真实 mod id；
- Java API；
- datapack drinks API；
- Maven / Modrinth Maven / 其他推荐开发依赖方式；
- 许可证对编译依赖和源码引用的要求。

当前已知其公开支持：

```text
data/<namespace>/thirstwastaken2/drinks/
```

以及 Java API 与 Drink Event，但实现前仍必须以当前 26.3 源码为准。

## 任务

1. 添加 optional development dependency；
2. `fabric.mod.json` 使用合适的 optional/suggests 语义，不加入硬 `depends`；
3. 使用独立 `HydrationAdapter`；
4. 未安装 TWT2 时不加载任何其类；
5. Food Profile 中 Hydration 数据在安装 TWT2 时兑现；
6. 避免重复应用：如果同一个食物已经通过 TWT2 datapack 获得 thirst 值，不允许 Buildup 再无条件叠加一次；
7. 明确 Buildup 与 TWT2 datapack 的数据优先级；
8. 兼容 TWT2 的 Quenched 语义；
9. 不修改 TWT2 的水纯度系统。

## 数据策略建议

优先评估两种方式：

A. Buildup 在加载期将 Food Profile Hydration 转译到 TWT2 数据/API；  
B. 官方兼容包直接生成/内置 TWT2 标准 drinks JSON，Buildup 只负责更高层关联。

优先选择耦合更小、重复应用风险更低的一种。

## 必测矩阵

```text
Buildup only
Buildup + TWT2
Buildup + TWT2 + AppleSkin
Dedicated Server + clients
/reload 后 Hydration 数据变更
```

## 人工验收

- [ ] 不装 TWT2 可正常启动；
- [ ] 安装后 Hydration 正常生效；
- [ ] 水果、汤、饮品体现不同补水定位；
- [ ] 不发生双倍补水；
- [ ] Quenched 正常；
- [ ] Buildup 不接管水纯度；
- [ ] TWT2 原有饮水玩法不被破坏。

## Stage Gate

完成后停止。

---

# 15. Stage 7 — Vanilla Balance Pack & First Integrated Prototype

## 目标

用 Minecraft 原版食物建立第一套完整可玩的官方平衡数据，并进行系统级实机测试。

## 任务

1. 为主要 Vanilla 食物建立官方 Profile；
2. 保留原版基础食物的可靠定位；
3. 为炖菜、派等更完整食品建立 Prepared / Meal 差异；
4. 加入适度 Recovery；
5. 加入 Diet Categories / Variety Group；
6. 安装 TWT2 时为水果、汤等提供合理 Hydration；
7. 使用少量食物测试各类 Meal Benefit；
8. 完成基础平衡配置集中化，方便后续调整。

## 实机测试场景

至少测试：

- 前期只吃简单食物；
- 矿洞长时间探索；
- 夜间连续战斗；
- 受伤后吃 Meal；
- 只带牛排远征；
- 建立农场后进行多样饮食；
- 安装/不安装 TWT2；
- Peaceful / Easy / Normal / Hard 的关键差异。

## 验收核心问题

- [ ] 牛排是否仍然值得带？
- [ ] 正式料理是否明显值得做？
- [ ] Food Recovery 是否帮助战斗但没有替代治疗药水？
- [ ] 自然恢复是否太慢导致烦躁？
- [ ] 自然恢复是否仍然太快？
- [ ] Variety 是否能被自然感知，但不会强迫换食物？
- [ ] TWT2 是否像系统自然组成部分，而不是另一套独立家务？

## Stage Gate

本阶段结束后形成第一个真正可玩的 Alpha。停止并进行较长时间人工体验，不立即进入第三方烹饪 Mod 兼容。

---

# 16. Stage 8 — Hardening, Tests & Alpha Freeze

## 目标

冻结 Core 第一版，为后续第三方料理兼容提供稳定基础。

## 任务

1. 清理技术债；
2. 检查服务器/客户端边界；
3. 检查数据 reload 生命周期；
4. 检查玩家状态持久化；
5. 检查死亡、复活、维度切换、重连；
6. 检查多人同时进食与状态同步；
7. 增加必要 GameTest / unit tests；
8. 确认错误 datapack 不造成灾难性失败；
9. 完善日志；
10. 更新 README；
11. 写第一版数据包开发说明；
12. 更新 CHANGELOG；
13. 标记 Alpha Core Freeze。

## 人工验收

- [ ] 客户端稳定；
- [ ] Dedicated Server 稳定；
- [ ] `/reload` 可反复执行；
- [ ] 玩家重连无数据异常；
- [ ] 多人情况下状态不串号；
- [ ] 不装 TWT2 稳定；
- [ ] 安装 TWT2 稳定；
- [ ] Vanilla 食物 fallback 稳定；
- [ ] 第三方未知食物至少仍能正常吃；
- [ ] Core API / Data Schema 已足够稳定，可以开始写官方兼容包。

## Stage Gate

完成后才允许进入 Farmer's Delight / Kaleidoscope Cookery 深度兼容。

---

# 17. Core Freeze 之后的后续路线

不属于当前 Codex 连续执行范围，仅作为路线记录。

## Phase B — Farmer's Delight Compatibility

重点不是给所有食物机械加 Tag，而是：

- 完整 Profile 数据；
- Nourishment 与 Buildup Recovery / Saturation 语义协调；
- 料理 Quality / Variety / Hydration 调整；
- 防止多个恢复机制叠加超模。

## Phase C — Kaleidoscope Cookery Compatibility

重点包括：

- 正式料理 Profile；
- 茶、汤等 Hydration；
- Satiated Shield / 饱腹代偿重新平衡；
- 防止“额外一条血”式超模玩法；
- 让其与 Buildup 的 Saturation 重新定位一致。

## Phase D — Oxygen / Air

基于原版 Air 扩展：

- Capacity；
- Consumption；
- Recovery；
- 水下状态反馈；
- 食物/装备/药水兼容。

不要创建第二条独立 Oxygen Bar。

## Phase E — External Vitals API

在真实需求出现后再设计：

- Mana Adapter；
- Stamina Adapter；
- 其他资源。

不要在没有实际接入对象时提前设计庞大抽象层。

---

# 18. 测试策略

## 18.1 Unit Tests

优先覆盖纯逻辑：

- Food Profile 解析；
- selector 优先级；
- Quality；
- Recovery Controller；
- Variety Score；
- Diet Memory 窗口；
- Repeat reduction；
- 数据冲突解析。

## 18.2 GameTest / Integration

适合覆盖：

- 玩家进食；
- Recovery Reserve；
- Hunger / Saturation 条件；
- reload；
- 数据持久化；
- 死亡 / 复活。

## 18.3 Manual Gameplay

体验类问题无法完全自动化：

- 恢复节奏；
- 战斗节奏；
- 料理价值；
- Tooltip 易读性；
- Variety 是否自然；
- 口渴是否烦躁。

每个 Gameplay Stage 都必须给出人工测试方法。

---

# 19. Mixin 风险控制

Hunger / Natural Regeneration 很可能需要 Mixin，但应遵循：

1. 优先 `@Inject` / `@Modify*` 等局部方式；
2. 尽量避免 `@Overwrite`；
3. 每个 Mixin 只负责一个明确语义；
4. Mixin 类名明确指出目标行为；
5. 对目标方法签名变化建立失败即显式报错的策略；
6. 不在同一个 Mixin 中同时处理 Recovery、Variety、Hydration 与 Tooltip；
7. 关键 Mixin 旁写明为什么 Fabric API 无法替代。

---

# 20. 数据包兼容优先级

最终应让整合包作者拥有最高控制权。

建议最终语义：

```text
代码 fallback
<
Buildup 内置 Vanilla / Compat 数据
<
普通 Data Pack
<
更高优先级整合包 / 服务器 Data Pack
```

如果 Minecraft 实际 datapack pack priority 与上述概念存在差异，应以原版资源包栈机制实现等价效果，不自行创建难以理解的第二套优先级系统。

---

# 21. 性能要求

本 Mod 的核心逻辑运行频率较高，因此：

- 不允许每 tick 遍历所有 Food Profile；
- reload 时预编译 selector / lookup；
- Item 查询应尽量 O(1) 或接近 O(1)；
- Diet Memory 只有约 10 项，不需要复杂数据库结构；
- Variety 在进食事件时重算优先于每 tick 重算；
- Meal Benefit 与 Recovery 使用轻量玩家状态；
- 客户端 Tooltip 查询不得进行磁盘 IO；
- 服务器不得依赖客户端存在。

---

# 22. 每阶段 Codex 输出格式

每个 Stage 完成后统一输出：

```markdown
# Stage X Completion Report

## 完成内容
- ...

## 主要文件
- ...

## 自动测试
- `./gradlew ...`：PASS / FAIL

## 实机/运行验证
- ...

## 已知问题
- ...

## 未进入的下一阶段内容
- 明确列出，证明没有越界实现

## 人工验收清单
- [ ] ...
- [ ] ...

## 建议下一步
- 等待人工确认，不自动继续
```

---

# 23. 第一条 Codex 开工指令

首次交给 Codex 时，只执行 **Stage 0**。

推荐直接使用：

> 阅读仓库根目录 `DESIGN_PRINCIPLES.md` 与 `PLAN.md`。本轮只执行 `PLAN.md` 的 **Stage 0 — Bootstrap & Build Baseline**，不要进入 Stage 1，也不要实现任何 Gameplay 机制。先对照 FabricMC 官方 `fabric-example-mod` 的 `26.3` 分支同步完整 Gradle Wrapper 和当前兼容工具链，确认项目使用 `buildup_vitals` / `com.davidblackcn.buildupvitals` / Java 25。完成 clean build、开发客户端启动和 Dedicated Server 启动验证，清理所有 Example Mod 残留。完成后严格按 `PLAN.md` 的 Stage Completion Report 格式汇报并停止，等待人工验收。

---

# 24. 当前计划的成功标准

这份计划完成 Core Freeze 时，Buildup Vitals 不需要已经拥有很多内容。

它真正需要做到的是：

> 普通食物仍然可靠；正式料理值得准备；食物治疗不会破坏战斗；Saturation 有了清晰意义；饮食多样性提供奖励但不制造家务；口渴能够自然接入；未知 Mod 食物不会因为没有兼容而失效；所有这些机制都有稳定的数据驱动基础。

如果这个基础成立，再向 Farmer's Delight、Kaleidoscope Cookery、Oxygen、Mana 等方向扩展才有意义。
