# Stage 7.5 Plan — Balance v2 & Consumption Rework

> 项目：Buildup Vitals
> 目标平台：Fabric 26.3
> 当前基线：Stage 7 已完成
> 本阶段名称：**Stage 7.5 — Balance v2 & Consumption Rework**
> 核心规范：`BALANCE_SPEC_V2.md`
> 长期设计总纲：`DESIGN_PRINCIPLES.md`

---

# 1. 阶段目标

Stage 7 已完成核心系统、原版 Alpha 食物 Profile、TWT2 可选适配和长链路验证，但当前实机体验暴露出第二版平衡需求。

本阶段不是推翻现有架构，而是：

> **在保留 Stage 1～7 数据驱动和模块结构的基础上，对 Recovery、Hydration、进食权限、过量进食、进食速度和 Meal Benefit 表现方式进行第二版重构。**

必须以：

```text
BALANCE_SPEC_V2.md
```

作为本阶段数值与机制的主要规范。

---

# 2. 本阶段必须解决的问题

本阶段至少完成：

1. Recovery v2；
2. Saturation 连续恢复模型；
3. Stable Recovery 回归接近原版；
4. Food Recovery 提速；
5. Food Recovery 不得拖慢 Natural Recovery；
6. 满 Hunger 继续进食；
7. Overeat Load；
8. `Overfull / 积食`；
9. 三档 Consumption Speed；
10. Meal Benefit MobEffect 化；
11. 原版 40 种食物 Recovery v2；
12. 原版 40 种食物 Hydration v2；
13. Pure Water `10 / 8` 平衡；
14. TWT2 Profile 优先级重构；
15. TWT2 Quenched Recovery 与 Buildup Recovery 协调；
16. Tooltip / Debug 信息同步；
17. GameTest / JVM Test / Client 验证；
18. 文档同步；
19. **更新 `DESIGN_PRINCIPLES.md` 到当前机制版本。**

---

# 3. 明确不进入的范围

Stage 7.5 不做：

- Farmer's Delight 完整官方兼容包；
- Kaleidoscope Cookery 完整官方兼容包；
- 其他大规模第三方食品 Mod 适配；
- Satiated Shield 深度魔改；
- Nourishment 深度魔改；
- Oxygen 深度重构；
- Mana；
- Stamina；
- 新常驻 HUD 条；
- 复杂胃容量 UI；
- 现实营养模拟；
- 新 Potion Recipe；
- Stage 8 Core Freeze。

第三方 Mod 只处理：

> 本阶段必须解决的 TWT2 Recovery / Hydration 协调。

---

# 4. 执行原则

Codex 在本阶段必须：

- 先读 `BALANCE_SPEC_V2.md`；
- 再读 `DESIGN_PRINCIPLES.md`；
- 再检查当前 Stage 7 实现；
- 不根据旧 Stage 文档中的 Alpha 数值继续实现；
- 不机械保留与 v2 冲突的旧逻辑；
- 不一次性做完后才测试；
- 每个子阶段都进行对应测试；
- 最终才进行全量矩阵和文档同步。

若实现方式与规范存在冲突，应优先：

```text
用户当前要求
>
BALANCE_SPEC_V2.md
>
更新后的 DESIGN_PRINCIPLES.md
>
旧 Stage 文档
>
旧 Alpha 数值
```

---

# 5. 子阶段 7.5-A — Recovery v2

## 目标

重构当前：

```text
FOOD_TICKS = 50
STABLE_TICKS = 120
WELL_FED_TICKS = 80
```

旧模型。

## A1. Natural Recovery

实现连续 Saturation 恢复模型。

目标：

```text
Hunger = 20
Saturation > 0
↓
12 tick 周期
```

恢复量：

```text
min(Saturation, 6) / 6
```

实际恢复值必须：

- 受缺失 Health 上限裁剪；
- 使用实际成功恢复值计算 Exhaustion；
- 不能因为浮点尾数进入异常状态。

## A2. Stable Recovery

目标：

```text
Hunger >= 18
且不满足 Saturation 快速恢复
↓
1 HP / 80 tick
```

移除旧的：

```text
120 tick / HP
```

默认。

## A3. Food Recovery

目标：

```text
普通：1 HP / 12 tick
Restorative：1 HP / 10 tick
```

保持：

- Recovery Reserve；
- 渐进兑现；
- 受击不中断；
- 满血保留；
- 死亡清零；
- 维度 / 重连持久化。

## A4. Food 与 Natural 协调

必须解决：

> Food Reserve 存在时不能让玩家恢复速度比没有 Food Reserve 时更慢。

需要新增测试：

- 高 Saturation + Food Reserve；
- 低 Saturation + Food Reserve；
- Restorative；
- Food Reserve 中途耗尽；
- 满血后重新受伤；
- 受击不中断；
- Variety / Quenched 加速边界。

## A5. Recovery 上限

Natural Recovery 的 Buildup 状态加速最终不得突破：

```text
10 tick 等效峰值
```

外部药水不受此限制。

## A6. A 阶段测试

至少新增 / 修改：

- `RecoveryControllerTest`
- `RecoveryGameTests`
- TWT2 相关恢复测试
- 原版高 Saturation 对照测试

建议新增数学测试表：

```text
Saturation 1
Saturation 2
Saturation 3
Saturation 6
Saturation > 6
```

验证单次恢复。

---

# 6. 子阶段 7.5-B — Full Hunger Eating & Overeating

## 目标

允许 Hunger 满值继续进食，并通过 Overeat Load 约束滥用。

## B1. 满 Hunger 继续进食

实现：

> 正常 Food Item 在 Hunger = 20 时仍然可使用。

要求：

- 不把普通 Potion 当 Food；
- 不破坏 Consumable 自己的特殊逻辑；
- 不破坏 Creative / Spectator；
- 不影响物品自己的 use duration；
- 兼容客户端 use animation；
- 服务端拥有最终判定。

需要检查 26.3 Food / Consumable 的实际 `canUse` / `canEat` 路径，选择最小 Mixin 注入面。

## B2. Overeat Attachment

新增玩家状态：

```text
OvereatState
```

至少保存：

```text
load
```

可以视需要保存：

```text
warningSent
```

但不要保存能通过 load 推导的冗余状态。

## B3. Overeat 计算

按 `BALANCE_SPEC_V2.md`：

```text
missingHunger = 20 - hungerBeforeEat
overflowNutrition = max(0, nutrition - missingHunger)
load += overflowNutrition
```

必须使用：

> 进食前 Hunger。

注意消费回调当前可能发生在原版已经补完 Hunger 之后，因此实现时需要找到可靠的“进食前数值”获取位置。

不要错误使用：

> 进食后的 Hunger

反推。

## B4. 阈值

原型默认：

```text
Warning = 48
Overfull = 64
Max = 80
Clear = 32
Decay = 1 / 40 tick
```

集中放入 Core Balance。

## B5. Overfull MobEffect

新增：

```text
buildup_vitals:overfull
```

中文：

```text
积食
```

英文：

```text
Overfull
```

行为：

- 阻止新 Food Recovery；
- 暂停已有 Food Recovery Reserve 兑现；
- 阻止新 Meal Benefit；
- 阻止刷新主 Meal Benefit；
- Natural Recovery 正常；
- Potion / Beacon / Golden Apple 等外部治疗正常；
- Hunger / Saturation 正常；
- Hydration 正常；
- 继续吃东西允许；
- 继续吃东西约慢 25%。

## B6. Warning

第一次跨过：

```text
48
```

时显示一次轻提示。

不要：

- Spam；
- 每 tick 显示；
- 增加常驻 HUD。

## B7. B 阶段测试

覆盖：

- Hunger 12 吃牛排 → 0 overflow；
- Hunger 20 吃牛排 → +8；
- Hunger 20 吃 Cookie → +2；
- 连吃约 8 牛排 → Overfull；
- load 降到 32 以下解除；
- Overfull 时食物仍正常恢复 Hunger；
- Overfull 时 Food Recovery 不增加；
- 已有 Reserve 暂停；
- Potion 正常；
- 死亡清空；
- 重连保存；
- Creative / Spectator 不进入该机制。

---

# 7. 子阶段 7.5-C — Consumption Speed

## 目标

为 Food Profile 增加三档进食速度。

## C1. Schema

推荐：

```json
"consumption": {
  "speed": "fast"
}
```

允许：

```text
normal
quick
fast
```

对应：

```text
1.0x
1.5x
2.0x
```

## C2. 兼容旧 Profile

Schema 必须考虑：

> Stage 7 已经有大量 schema v1 Profile。

优先使用向后兼容扩展。

若必须升级 schema：

- 提供明确迁移；
- 更新测试；
- 更新文档；
- 不允许所有旧 Profile 静默失效。

如果可以在 schema v1 增加可选字段而保持兼容，则优先采用。

## C3. Override 原则

未写：

```text
consumption.speed
```

时：

> 保留物品原本 Consumable duration。

不是默认强制 32 tick。

## C4. 原版配置

Fast：

- dried_kelp
- cookie
- sweet_berries
- glow_berries
- melon_slice

Quick：

- apple
- carrot
- beetroot
- potato
- 其他合理轻食

其余默认保留 / normal。

Codex 可以基于原版已有 Consumable duration 修正最终列表，但不能违背：

> Snack 更快、正式 Meal 正常速度

这一设计方向。

## C5. Overfull 进食减速

Overfull：

```text
最终 use duration × 1.25
```

必须与：

```text
normal / quick / fast
```

正确组合。

例如：

```text
Fast 16 tick
Overfull
→ 20 tick
```

## C6. C 阶段测试

至少验证：

- 未声明 Profile 不改变速度；
- normal；
- quick；
- fast；
- Overfull + fast；
- 客户端动画时长与服务端完成时机一致；
- 中途取消不会错误结算；
- 多人 / dedicated server 不依赖客户端数据。

---

# 8. 子阶段 7.5-D — Meal Benefit MobEffect 化

## 目标

将 Stage 7 的 Attachment-only Meal Benefit 改成正式 MobEffect。

## D1. 注册效果

至少：

```text
restorative
invigorated
steady
overfull
```

类型：

```text
Restorative = Beneficial
Invigorated = Beneficial
Steady = Beneficial
Overfull = Harmful
```

## D2. 可见性

Meal Benefit：

- HUD Icon 可见；
- Inventory Effect 可见；
- 默认无粒子；
- 不注册默认 Potion；
- 不注册酿造配方。

## D3. 状态迁移

当前存档可能存在旧：

```text
MealBenefit Attachment
```

必须考虑迁移。

Stage 7.5 至少要决定并实现一种安全策略。

推荐：

```text
如果存在有效旧 Benefit
↓
首次 tick / 登录转换为对应 MobEffect
↓
删除旧 Attachment
```

不能简单删除旧状态导致玩家存档报错或附件 Codec 失配。

完成迁移后可以保留旧 Attachment 注册一段兼容周期，或按 Fabric Data Attachment 实际行为选择安全方案。

必须记录到文档。

## D4. 主 Meal Benefit 互斥

仍保留：

```text
Restorative
Invigorated
Steady
```

只允许一个主 Meal Benefit。

新食物授予时：

- 同类刷新 / 延长；
- 异类替换；
- 不影响 Overfull；
- Overfull 时禁止授予 / 刷新。

## D5. 图标

为至少：

- Restorative
- Invigorated
- Overfull

提供正式图标。

Steady 若暂未投放，也建议提供占位正式图标，避免注册后缺资源。

风格：

> 简洁、Vanilla-like、易识别。

本阶段不要求艺术精修，但不得缺失资源。

## D6. Steady

如果没有足够清晰、低风险的正式行为：

> 可以完成注册、互斥和 UI，但不加入任何原版 Food Profile。

不得为了“本阶段必须实现”而随意复制 Nourishment 或创建新复杂机制。

---

# 9. 子阶段 7.5-E — Hydration v2 & TWT2 Coordination

## 目标

重做原版 Hydration 和 TWT2 兼容优先级。

## E1. Pure Water

目标：

```text
10 Thirst / 8 Quenched
```

需要覆盖：

- Pure Water Bottle；
- TWT2 对水瓶的实际解析路径；
- Tooltip。

不要把 Dirty / Murky / Salty 水全部直接当 Pure 处理。

Water Purity 自己的 Quenched 修正和 sickness 语义必须继续保留。

## E2. 原版 Hydration 重做

当前大量：

```text
0 / 0
```

需要按 `BALANCE_SPEC_V2.md` 重做。

原则：

- 真正干燥食品才 0；
- 大部分普通食物至少 1 Thirst；
- 生肉 / 生鱼约 2；
- 对应熟食约 1；
- 水果 3～5；
- 汤 5～8；
- 饮品 6～10。

## E3. 数据优先级

实现目标：

```text
TWT2 blacklist
↓
Buildup explicit profile
↓
TWT2 explicit/default
↓
TWT2 generic fallback
```

必须重新审查当前：

```text
ThirstApi.resolve()
DataPackDrinks.get()
config drinks / foods
```

实际调用顺序。

不要仅修改文档，必须保证运行时真实生效。

## E4. Quenched Healing

协调目标：

> 不再让 TWT2 独立治疗器与 Buildup Recovery 完全平行叠加。

推荐：

```text
Thirst Full + Quenched > 0
→ Buildup Natural Recovery speed +15%
```

并受：

```text
10 tick minimum interval
```

约束。

实现前需要检查 TWT2 1.6.2 的：

```text
HealthRegen
ThirstManager
ThirstConfig
```

选择最小兼容注入。

不要写用户配置文件永久关闭：

```text
quenchedHealthRegen
```

除非没有其他安全实现；如果必须改变策略，需要先停止并报告，不允许自行改变用户配置。

## E5. TWT2 版本门控

继续保持：

> 只对已验证版本启用具体 Mixin。

不要把 Stage 7 的版本安全措施删除。

若 Stage 7.5 修改 TWT2 Mixin：

- 重新核对方法描述符；
- 重新运行有 / 无 TWT2 两套矩阵；
- 检查 dedicated server。

---

# 10. 子阶段 7.5-F — Vanilla Balance Pack v2

## 目标

重写当前 40 种原版食物的官方 Buildup Profile。

## F1. Recovery

全部食物按 `BALANCE_SPEC_V2.md` 重新评估。

不再让：

> 熟肉、面包等大量普通食物全是 Recovery 0。

## F2. Hydration

全部食物重新评估。

不再默认大量 `0 / 0`。

## F3. Consumption Speed

为适合的 Snack 和 Light Food 加入：

```text
quick / fast
```

## F4. Meal Benefit

当前：

```text
mushroom_stew → Restorative
pumpkin_pie → Invigorated
rabbit_stew → Invigorated
```

可继续作为起点。

允许根据 v2 重新评估，但：

- 不要让大量 Basic Food 都带 Buff；
- Meal Benefit 应主要出现在 Prepared / Meal / Feast；
- 不为了覆盖率硬塞。

## F5. 特殊物品保护

重新测试：

- golden_apple
- enchanted_golden_apple
- honey_bottle
- suspicious_stew
- rotten_flesh
- chicken
- pufferfish
- chorus_fruit

---

# 11. 子阶段 7.5-G — Client Feedback

## 目标

让玩家能够发现 v2 机制。

## G1. Tooltip

至少支持：

- Recovery；
- Hydration；
- Meal Benefit；
- Quick / Fast eating。

不要把 Overeat Load 数字写进普通 Food Tooltip。

## G2. Effect UI

验证所有 Meal Benefit 和 Overfull：

- 图标；
- 翻译；
- 倒计时；
- Inventory 显示；
- HUD；
- 无粒子。

## G3. Warning

Overeat Warning：

- 不 Spam；
- 本地化；
- multiplayer 正常；
- 服务器控制。

---

# 12. 子阶段 7.5-H — Debug & Tests

## 目标

建立 v2 完整验证矩阵。

## H1. 调试

命令至少能查看：

```text
Health
Hunger
Saturation
Recovery Reserve
Recovery interval / mode
Current Meal Benefit
Overeat Load
Overfull
Variety
TWT2 enabled
Thirst / Quenched integration state
```

## H2. JVM Tests

至少覆盖：

- Natural Recovery 数学；
- Food Recovery 调度；
- Overeat 计算；
- 阈值；
- Consumption Speed 解析；
- Profile Parser；
- Variety；
- Meal Benefit 互斥；
- 旧 Attachment 迁移逻辑。

## H3. GameTests

至少覆盖：

- Hunger 满值可以吃；
- 正常 Hunger 不产生 overflow；
- 满 Hunger 产生 overflow；
- Overfull；
- Food Recovery 暂停；
- Natural Recovery 继续；
- Potion 继续；
- 受击不中断；
- Consumption timing；
- Hydration；
- TWT2 priority；
- Quenched Recovery coordination。

## H4. Client Tests

至少验证：

- 进食动画时长；
- Tooltip；
- MobEffect HUD；
- Effect Inventory；
- Warning；
- 有 / 无 TWT2；
- 有 / 无 AppleSkin。

---

# 13. 子阶段 7.5-I — Documentation Sync

## 目标

完成代码后，不允许只更新 Stage Report。

必须同步所有仍被用户 / 后续 Codex 使用的正式文档。

## I1. 必须更新 DESIGN_PRINCIPLES.md

这是本阶段强制项。

至少修正：

### Recovery

旧：

```text
Stable ≈ 1 HP / 6s
Well-fed ≈ 1 HP / 4s
```

更新为 v2 连续 Saturation 模型和接近原版的恢复目标。

### Food Recovery

旧：

```text
约 1 HP / 2～3s
```

更新为：

```text
普通 12 tick
Restorative 10 tick
```

并说明：

> Food Recovery 不能拖慢 Natural Recovery。

### Meal Benefit

旧文档中：

> 不一定是 Minecraft Status Effect

更新为：

> v2 正式注册为 MobEffect，但默认不制作 Potion Recipe。

### Full Hunger Eating

新增：

> Hunger 满值仍允许正常 Food Item 进食。

### Overeating

新增：

- Overeat Load；
- Overfull；
- 负反馈边界；
- 不惩罚正常进食。

### Hydration

更新：

> 大部分普通食物都可以提供至少少量 Hydration，真正干燥食物才应为 0。

并加入：

```text
Pure Water 10 / 8
```

设计基准。

### Consumption Speed

新增：

```text
normal / quick / fast
```

及 Snack Role。

### TWT2

更新：

> Buildup 正式协调 Quenched 对 Recovery 的贡献，避免独立治疗器无限叠加。

## I2. 更新其他文档

至少检查并更新：

- `docs/RECOVERY.md`
- `docs/HYDRATION.md`
- `docs/FOOD_PROFILES.md`
- `docs/MEAL_BENEFITS.md`
- `docs/VANILLA_BALANCE.md`
- `docs/CLIENT_FEEDBACK.md`
- `README.md`
- `UPDATE_NOTES.md`
- `PLAN.md`

如果某旧 Stage Report 是历史记录：

> 不要重写历史结论。

只在新 Stage 7.5 Report 中明确：

> Stage 7 的 Alpha 数值已被 v2 替代。

---

# 14. Stage 7.5 Report

新增：

```text
docs/STAGE_7_5_REPORT.md
```

必须包含：

- 实现摘要；
- 关键数值；
- 数据迁移；
- TWT2 兼容策略；
- Tests；
- Client 验证；
- 与 Stage 7 的行为差异；
- 已知风险；
- 人工验收清单；
- 下一步建议。

---

# 15. CI / Build

完成后至少运行：

```text
./gradlew clean build
```

以及项目现有的：

- JVM tests；
- GameTests；
- client tests；
- standalone；
- TWT2 runtime；
- TWT2 + AppleSkin；
- dedicated server。

若项目现有命令名称不同，以当前 `build.gradle` / workflows 为准。

---

# 16. 人工验收清单

Codex 完成 Stage 7.5 后必须停止，输出以下人工验收清单。

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

---

# 17. Stage 7.5 完成条件

只有同时满足：

```text
代码实现完成
+
自动测试通过
+
客户端 / 服务端矩阵通过
+
BALANCE_SPEC_V2.md 行为全部对应
+
DESIGN_PRINCIPLES.md 已同步
+
正式文档已更新
+
STAGE_7_5_REPORT.md 已生成
```

才可宣布：

> Stage 7.5 implementation complete.

但此时：

> **不要自动进入 Stage 8。**

必须停止并等待人工体验验收。

---

# 18. Stage 7.5 后续决策

人工验收后可能出现：

### A. 数值舒服

进入：

> Stage 8 — Hardening & Alpha Core Freeze

### B. 数值仍有明显问题

进行：

> Stage 7.5.x Balance Hotfix

只调整：

- tick；
- Recovery；
- Hydration；
- Overeat threshold；
- Consumption tier；

尽量不再改架构。

---

# 19. 给 Codex 的开工提示词

请从这里开始执行：

```text
你现在负责 Buildup Vitals 的 Stage 7.5 — Balance v2 & Consumption Rework。

先完整阅读：

1. BALANCE_SPEC_V2.md
2. Stage 7.5 Plan.md
3. DESIGN_PRINCIPLES.md
4. PLAN.md
5. docs/STAGE_7_REPORT.md
6. docs/RECOVERY.md
7. docs/HYDRATION.md
8. docs/FOOD_PROFILES.md
9. docs/MEAL_BENEFITS.md
10. docs/VANILLA_BALANCE.md

随后审查当前 main 分支源码，确认 Stage 7 当前真实实现与文档是否一致。

本轮不要直接进入 Stage 8，也不要提前制作 Farmer's Delight / Kaleidoscope Cookery 完整兼容。

请严格按照 Stage 7.5 Plan 的 A → I 顺序推进。

重点目标包括：

- 重做 Recovery v2，使整体恢复速度回到接近原版的量级；
- 用连续 Saturation 模型取代旧 Well-fed 二段阈值；
- Stable 恢复回到约 80 tick / HP；
- Food Recovery 改为约 12 tick / HP，Restorative 约 10 tick / HP；
- Food Reserve 不得让玩家比原本 Natural Recovery 更慢；
- Hunger 满值仍允许进食；
- 新增 Overeat Load 与 Overfull / 积食；
- 加入 normal / quick / fast 三档进食速度；
- 将 Meal Benefit 正式注册为 MobEffect，但不制作默认 Potion / Brewing Recipe；
- 重做原版食物 Recovery / Hydration；
- Pure Water 调整为 10 Thirst / 8 Quenched；
- 调整 TWT2 数据优先级，让 Buildup explicit profile 真正控制已适配食物；
- 协调 TWT2 Quenched Healing，避免形成独立叠加治疗器；
- 同步 Tooltip、调试命令、测试和正式文档；
- 最后必须更新 DESIGN_PRINCIPLES.md，使其反映 v2 当前真实设计。

实现过程中优先最小侵入和数据驱动，不要扩大 Mixin 范围到 LivingEntity.heal 或全局伤害系统。

每个子阶段都先完成实现和对应测试再进入下一块；如果遇到必须改变用户配置文件、无法安全迁移旧 Attachment、或 TWT2 1.6.2 无法用最小注入协调恢复的情况，应停止并在输出中说明原因，不要自行采用高风险替代方案。

全部完成后：

1. 运行完整 build / test / GameTest / client / TWT2 矩阵；
2. 生成 docs/STAGE_7_5_REPORT.md；
3. 更新 UPDATE_NOTES.md；
4. 更新 DESIGN_PRINCIPLES.md 以及所有仍属于当前正式说明的 docs；
5. 给出 Code Review 总结；
6. 给出 Stage 7.5 人工验收清单；
7. 停止，不要自动进入 Stage 8。
```
