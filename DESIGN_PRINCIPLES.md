# Buildup Vitals — Design Principles

> 工作名称：**Buildup Vitals**  
> 首要目标平台：**Fabric 26.3**  
> 文档状态：**第一版核心设计原则**  
> 当前重点：**饥饿、饱和、食物恢复、饮食质量、饮食多样性、口渴兼容**  
> 后续方向：**氧气、第三方料理机制重构、法力等其他玩家状态资源**

---

## 1. 项目定位

Buildup Vitals 是 Buildup 系列中面向玩家基础状态机制的 Vanilla+ 模组。

它的目标不是向 Minecraft 中不断增加新的生存条，也不是将游戏改造成一套复杂的生存模拟器，而是重新整理和扩展原版已经存在、但彼此关联较弱的玩家状态系统。

首个版本主要围绕饥饿值、饱和度、生命恢复、食物、口渴与氧气展开。未来也可以接入法力、耐力、温度或其他 Mod 提供的玩家资源。

Buildup Vitals 更接近一层“玩家状态协调与增强框架”，而不是所有状态值本身的所有者。对于已经存在成熟实现的机制，应优先接入和重新协调，而不是重复造轮子。

例如：饥饿与饱和继续基于 Minecraft 原版；生命值继续使用原版 Health；氧气继续基于原版 Air；口渴优先接入 Thirst Was Taken 2；未来法力值优先兼容已有成熟法力系统。

## 2. 核心设计理念

Buildup Vitals 最核心的设计原则是：

> **奖励玩家把自己的状态照顾得更好，而不是惩罚玩家没有按照某一种“正确方式”游玩。**

系统应使普通玩法可靠、正常且可以长期游玩；更加讲究的玩法则获得额外正反馈。准备充分、饮食丰富、料理质量较高，应让玩家获得更好的状态与体验，而不是让没有维护好所有状态的玩家持续遭遇强烈负反馈。

Buildup Vitals 可以增加深度，但不应增加大量维护负担。

## 3. Vanilla+ 在本项目中的含义

Vanilla+ 不代表所有机制都必须极其简单，也不代表只能使用原版已有规则。它更强调三点：

1. **机制应该容易通过体验理解。** 玩家应能逐渐发现“吃一顿完整料理以后恢复得更舒服”，而不需要先查 Wiki 理解大量数值。
2. **新系统应围绕已有行为展开。** 玩家仍然只需要吃东西、做料理、喝水、游泳、战斗和探索。
3. **原版资源仍然可靠。** 牛排仍然应该是一种好用食物，面包仍然可以长期作为普通食物，重复食用某一种食物不应让它突然失去基础生存价值。

## 4. 玩家状态总体结构

```text
Player Vitals
├── Health
├── Food
│   ├── Hunger
│   └── Saturation
├── Hydration
│   ├── Thirst
│   └── Quenched
├── Respiration
│   └── Air
└── External
    ├── Mana
    ├── Stamina
    └── Future Resources
```

Buildup Vitals 不要求自己拥有这些数据。它主要负责读取状态、修改状态之间的关系、统一部分恢复逻辑、为食物等内容提供数据定义，并为第三方系统提供兼容接口。

## 5. Hunger 与 Saturation 的重新定位

### Hunger：基础生存储备

Hunger 回答的是“玩家还需要吃东西吗？”。它继续承担原版中最基础的食物储备职责，例如是否处于饥饿状态、是否能够继续疾跑、长时间缺少食物是否进入危险状态，以及食物最基本的“填饱肚子”能力。

因此 Hunger 是长期、基础、可靠的生存资源。

### Saturation：短期能量与恢复状态

Saturation 回答的是“玩家最近吃得怎么样？”。它代表玩家当前较短期的能量储备和恢复能力。

两个 Hunger 同样满值的玩家可以具有不同状态：一个只是吃饱但 Saturation 较低，另一个则 Hunger 与 Saturation 都较高，处在更好的恢复状态。

Saturation 对生命恢复和良好身体状态的重要性应显著提高。

## 6. Hunger 与 Saturation 的基础消耗关系

基础循环继续遵循 Minecraft 原有思路：玩家活动产生 Exhaustion，优先消耗 Saturation；Saturation 耗尽后继续活动，才开始消耗 Hunger。

Buildup Vitals 不需要完全推翻这一模型，主要变化发生在 Saturation 对恢复和整体状态的意义上。

## 7. 自然生命恢复

自然回血保留，但不再承担“吃两块食物以后迅速从残血回到满血”的主要治疗职责，而被重新定位为玩家长期保持良好状态后得到的背景恢复。

首轮原型测试可从以下范围开始：

- **Stable Recovery**：Hunger 状态良好、Saturation 达到基本要求时，约每 6 秒恢复 1 HP。
- **Well-fed Recovery**：Hunger 较高、Saturation 较高时，约每 4 秒恢复 1 HP。

这些数字只作为原型起点。设计目标是确保即使在最佳普通状态下，自然恢复也明显低于原版高饱食状态的爆发恢复能力。

## 8. 食物直接恢复生命

部分食物可以拥有独立的生命恢复能力，但不应简单采用“吃下料理后瞬间 +N HP”的方式。

Buildup Vitals 使用 **Food Recovery Reserve（食物恢复储备）**。

例如一道料理具有 `Recovery = 4 HP`，并不代表玩家立刻获得 4 HP，而是吃下后向 Recovery Reserve 中加入 4 HP，再在随后一段时间逐渐转换为真正的 Health。

首轮测试可采用约每 2～3 秒转换 1 HP 的速度。

这样既能让料理真正成为治疗体系的一部分，又不会让食物退化为廉价瞬间治疗药水，同时也为其他机制提供稳定接口。

## 9. Food Recovery 不因受到攻击而暂停

这一原则明确固定：

> **受到攻击、进入战斗或刚刚损失生命，都不会暂停已经获得的 Food Recovery Reserve。**

食物既然已经吃下，其治疗价值就应持续兑现。Buildup Vitals 不使用“受击后暂停食物恢复”“战斗状态下食物恢复失效”等规则。

系统主要依靠食物使用时间、Recovery Reserve 的渐进恢复与总体恢复速度来控制战斗中的食物治疗强度。

## 10. Recovery Controller

Food Recovery 与 Natural Recovery 不应毫无控制地直接叠加，因此需要统一的 Recovery Controller 协调自然类型的生命恢复。

推荐基础逻辑是：存在可执行的 Food Recovery Reserve 时优先按 Food Recovery 逻辑恢复；没有可执行的 Food Recovery 时再进入 Natural Recovery。

治疗药水、再生药水、信标、金苹果以及其他明确独立的治疗系统不应被强行接管。

## 11. Food Profile

每种食物都可以拥有一份附加的 **Food Profile**，用于描述原有 Food Component 之外的属性。概念字段包括：

- Quality
- Recovery
- Hydration
- Diet Categories
- Variety Group
- Food Traits
- Meal Benefit
- 可选 Hunger / Saturation override

默认情况下应继续尊重 Minecraft 或第三方 Mod 自己的 Food Component；只有兼容包或整合包明确需要重新平衡时才覆盖 Hunger 与 Saturation。

没有 Buildup 专门适配的食物也必须正常可用。

## 12. Food Quality

食物质量采用四个基础等级：

```text
Basic
Prepared
Meal
Feast
```

Quality 描述的不是物品稀有度，而是这份食物作为一顿饮食有多完整、多精心。

- **Basic**：基础食物，例如苹果、胡萝卜、面包、普通烤肉。
- **Prepared**：经过一定加工的食品，例如简单组合食品、派、三明治等。
- **Meal**：完整正式料理，例如炖菜、米饭料理与烹饪 Mod 中的大部分正式菜品。
- **Feast**：复杂、丰盛或高完成度料理。

Quality 不直接决定 Hunger。牛排可以 Hunger 高、Saturation 高但仍是 Basic；复杂炖菜可以有相近基础饱食价值，但因为属于 Meal 而在 Recovery、Hydration、Variety 与 Meal Benefit 上更完整。

高级料理的价值来自更完整，而不是“更大号的鸡腿条”。

Quality 也不应单纯通过配方材料数量自动推断。官方兼容数据中的重要料理应人工调整，自动推断只作为 fallback。

## 13. Meal Benefit

高质量食物除了 Recovery 与 Variety 之外，可以提供 **Meal Benefit**，用于表达“我刚刚吃了一顿不错的饭”的中期状态。

Meal Benefit 不要求实现为 Minecraft 原版 Status Effect。更适合由 Buildup 自己维护一层轻量饮食状态，并通过 Recovery Controller、Exhaustion、Vitals 状态与 Hydration 等机制兑现。

第一批适合探索的 Meal Benefit 包括：

- **Restorative / 调养**：提高 Recovery Reserve 的兑现效率，但不增加最终治疗总量。
- **Invigorated / 精力充沛**：轻微降低疾跑、跳跃、游泳、攀爬等特定活动造成的 Exhaustion，首轮可从约 5%～10% 测试。
- **Steady / 安适**：使部分 Vitals 的短时间波动更加平稳，重点是削弱短时间资源消耗峰值，而不是简单锁住 Saturation。

Meal Benefit 不应只是“吃饭后获得 Speed I / Regeneration I”的药水效果合集。

## 14. Meal Benefit 与第三方料理 Buff 的边界

Buildup Vitals 不应重复实现已有烹饪 Mod 的主要 Buff 语义。尤其应避免直接重复长时间维持 Saturation、自动持续回血、用 Hunger / Saturation 抵消大量伤害等方向。

Buildup 自己更适合关注 Recovery Reserve、Vitals 稳定性、活动消耗、饮食多样性以及 Quality 与中期状态的联动。

对 Farmer's Delight、Kaleidoscope Cookery 等 Mod 的深度兼容，应在 Core 完成以后进行，并以语义协调和重新平衡为主，而不是简单叠加所有 Buff。

## 15. Meal Benefit 的堆叠原则

Buildup 自己的主要 Meal Benefit 同一时间原则上只保留一个：

- 同类型 Benefit：刷新或延长；
- 不同类型 Benefit：新的主要 Benefit 替换旧的；
- Feast：可允许一个主要 Benefit 加一个很轻的 Secondary Modifier，但 Secondary Modifier 不形成第二个完整状态。

第三方 Mod 自己提供的 Buff 不强制占用 Buildup 的 Meal Benefit 槽位。

## 16. 负面 Meal Benefit

Buildup Vitals 可以存在轻量负面饮食状态，例如 Heavy / 过饱、Greasy / 油腻、Questionable / 可疑、Spoiled / 变质。

但负面状态应主要来自食物本身的特殊性质，而不是来自“玩家没有按照系统规定去均衡饮食”。连续吃同一种正常食物不应因此获得严重营养不良类 Debuff。

## 17. Dietary Variety

Buildup Vitals 不使用复杂营养条，不要求玩家维护蛋白质、脂肪、碳水、维生素、矿物质等多个资源。

系统使用 **Dietary Variety（饮食多样性）**：吃得丰富应获得额外奖励，吃得单一最多失去部分额外收益，而不会破坏基础生存能力。

## 18. Diet Memory

玩家拥有一份隐藏的 Diet Memory，用于记录最近的实际饮食历史。每次有效进食可以记录：

- Food ID
- Quality
- Diet Categories
- Variety Group
- Timestamp

首轮建议从最近约 10 次有效进食开始测试，合理范围约为 8～12 次。

## 19. Diet Categories、Food Traits 与 Variety Group

第一版 Diet Categories 建议保持较少：

```text
protein
grain
vegetable
fruit
dairy
sweet
```

玩家不需要把所有类别都“打卡”才能获得良好状态。

`soup`、`drink`、`snack`、`warm`、`cold`、`meal` 等不属于营养类别，应放在独立的 Food Traits 中。

每种食物还可以拥有 `variety_group`，用于判断多个物品是否本质上属于相似食物。Variety Group 应允许由 Data Pack 作者调整。

## 20. Variety Score

最终公式不在设计原则阶段锁死，但优先级大致应为：

```text
Category Diversity
    >
Variety Group Diversity
    >
Food Quality
```

概念上：

```text
Variety Score
= 类别覆盖
+ 食物组多样性
+ Quality 权重
- 重复衰减
```

肉 + 谷物 + 蔬菜通常应比苹果 + 西瓜 + 甜浆果 + 发光浆果具有更高的饮食多样性价值，但后者也不应完全没有收益。

## 21. 重复饮食的处理

连续大量食用相同或相近食物，可以降低额外收益，但明确不降低基础 Hunger、基础 Saturation 与最核心的生存价值。

首轮原型可考虑让大量连续重复后的额外收益最低约为正常值的 85%～90%，但 Hunger 与 Saturation 始终维持 100%。

数学基准应从 100% 开始：单一普通饮食约等于 baseline，丰富饮食可以逐渐达到约 105%、110%、115%。系统选择奖励型模型，而不是把普通饮食压低到 70% 再把“均衡饮食”当作恢复正常。

## 22. Variety Bonus

第一版可以考虑让 Variety 影响：

- Food Recovery：最高约 +10%～15%；
- Meal Benefit：轻微提高持续时间或效果，例如最高约 +10%；
- Well-fed Natural Recovery：只提供很轻的提升，例如最高约 +5%～10%。

最终数值需实机测试。

## 23. 一顿料理的完整价值

一份正式料理最终可以同时承担：

```text
Food
├── Hunger / Saturation   基础生存
├── Hydration             口渴与饮水
├── Recovery Reserve      短期生命恢复
└── Meal Benefit          中期状态
```

同时，本次进食写入 Diet Memory 并影响长期 Variety。

因此吃一顿饭可以具有即时、短期、中期与轻量长期价值，但玩家仍然只需要完成“吃东西”这一件事。

## 24. 普通食物与正式料理的定位

Buildup Vitals 不应淘汰普通食物。

牛排可以继续拥有高 Hunger、高 Saturation、低 Recovery、低或无 Hydration、Basic Quality、Protein Category，因此仍然非常适合旅行、探索和冒险。

正式料理则可以拥有类似的基础饱食能力，但同时提供更好的 Recovery、Hydration、Variety Contribution 与 Meal Benefit。

二者不是弱食物和强食物，而是不同使用场景的食物。

Feast 的主要优势同样应是“更全面”，而不是所有单项数值都达到最大。

## 25. Hydration

口渴系统第一阶段不从零实现，Buildup Vitals 优先将 Thirst Was Taken 2 作为可选兼容 Mod，并通过 Hydration Adapter 接入。

没有安装口渴 Mod 时，其他 Buildup Vitals 功能仍应正常工作。

绝大部分食物可以根据性质恢复不同程度 Hydration，以避免口渴系统变成“只能频繁喝水”的额外家务。水果可提供中等 Hydration，面包可能无或很低，汤类较高，饮品主要提供 Hydration 与 Quenched。

Buildup Vitals 原则上不鼓励频繁使用“吃咸肉直接扣口渴”等过度现实主义负值；大多数偏干或偏咸食品设为 Hydration = 0 已经足够表达差异。

## 26. 氧气系统

氧气是重要方向，但不属于第一阶段最优先内容。未来仍然基于原版 Air，而不是再创建第二套 Oxygen Bar。

可扩展方向包括 Air Capacity、Air Consumption、Air Recovery、水下氧气效率、临界缺氧反馈，以及装备或料理对氧气使用方式的影响。

## 27. 数据驱动

食物与兼容设计应尽可能数据驱动。优先允许 Data Pack 控制：

- Quality
- Recovery
- Hydration
- Diet Categories
- Variety Group
- Food Traits
- Meal Benefit
- 可选 Hunger / Saturation override
- 兼容行为

应避免大量针对具体物品 ID 的硬编码分支。

## 28. 未适配食物必须可靠降级

未知食物应继续使用原 Food Component 正常提供 Hunger / Saturation；Buildup 部分则可以使用 Basic/Fallback Quality、Recovery = 0、Hydration = 0，并尝试通过公共 Tag 推断 Diet 信息。

因此：**未适配不等于不可用。** 官方兼容包提供的是更完整体验，而不是把物品从“不能用”修到“能用”。

## 29. 官方兼容数据

Buildup Vitals 可以内置或附带常用 Mod 的官方兼容数据。首要目标包括 Minecraft 原版、Farmer's Delight 系列、Kaleidoscope Cookery 系列以及整合包实际使用的主要食物 Mod。

第三方整合包作者应始终能够使用自己的 Data Pack 覆盖 Buildup 默认平衡。

## 30. UI 与信息表达

Buildup Vitals 不应因为增加内部机制，就不断增加新的常驻 HUD 条。优先复用原版 Hunger、Health、Air 与 Thirst Was Taken 2 已有 HUD，并通过 Tooltip、小型状态图标或必要的半透明预览表达附加信息。

Recovery Reserve 可以未来尝试在心形 HUD 上以半透明方式预览即将恢复的生命，但不需要新增一条 Recovery Reserve 进度条。

普通 Tooltip 应表达食物大致特点，而不是堆满内部系数。详细数字可以放在高级 Tooltip、调试模式、配置文件或 Wiki 中。

## 31. 首轮平衡原型目标

以下数值仅作为第一轮实现和实机测试的起点，不代表最终平衡已经锁死：

| 项目 | 原型目标 |
|---|---:|
| Stable Natural Recovery | 约 1 HP / 6 秒 |
| Well-fed Natural Recovery | 约 1 HP / 4 秒 |
| Basic Food Recovery | 0～1 HP |
| Prepared Recovery | 1～2 HP |
| Meal Recovery | 2～4 HP |
| Feast Recovery | 3～5 HP |
| Recovery Reserve 转化速度 | 约 1 HP / 2～3 秒 |
| Variety 最大 Recovery Bonus | 约 +10%～15% |
| Variety 对 Natural Recovery | 约 +5%～10% 上限 |
| Variety 对 Meal Benefit | 约 +10% 上限 |
| 重复食物额外收益最低倍率 | 约 85%～90% |
| 重复食物 Hunger 倍率 | 100% |
| 重复食物 Saturation 倍率 | 100% |

这些数值主要用于确认设计体验是否正确，而不是在设计阶段追求最终数学平衡。

## 32. 第一阶段开发重点

第一阶段优先完成：Food Profile 数据系统；Hunger / Saturation 与自然恢复的重新协调；Recovery Controller；Food Recovery Reserve；四档 Food Quality；Meal Benefit 基础框架；Diet Memory；Dietary Variety；重复饮食的轻量额外收益衰减；Thirst Was Taken 2 Adapter；Food Hydration；基础 Tooltip / UI 信息表达；Data Pack 覆盖与兼容框架。

完成这些内容以后，饥饿、料理、恢复与口渴应已经形成完整 Gameplay Loop。

## 33. 后续阶段

Core 稳定之后再逐步推进 Oxygen / Air 深度优化、Farmer's Delight 与 Kaleidoscope Cookery 的深度平衡、第三方料理 Buff 语义协调，以及 Mana / Stamina 等外部 Vitals Adapter。

## 34. Non-Goals

当前明确不以以下方向为目标：

- 复杂营养模拟；
- 把饮食变成必须完成的日常任务；
- 严重惩罚单一饮食；
- 为了现实而牺牲游玩体验；
- 创造大量新的状态条；
- 让高级料理成为普通食物的纯数值上位替代；
- 让 Meal Benefit 退化为药水效果合集；
- 用大量硬编码完成兼容。

## 35. 最终设计准则

> **基础食物永远可靠。**  
> **认真准备料理值得。**  
> **吃得丰富会更舒服。**  
> **吃得单一不会被系统惩罚到无法正常游戏。**  
> **食物不仅仅是给饥饿条充值。**  
> **高级料理的优势来自更加完整，而不是单纯拥有更大的数字。**  
> **状态系统应该带来新的选择，而不是新的家务。**

理想体验是：玩家可以觉得“牛排很好用，我出去探险时会带”“做一顿正式料理更划算，受伤以后恢复也舒服很多”“最近换着吃各种东西，好像状态一直不错”“喝汤顺便能补水”。即使玩家不主动研究这些机制，也仍能正常生存；愿意研究以后，则确实能获得一些好处。

这就是 Buildup Vitals 所追求的：**原版优化 / 原版增强，而不是复杂化原版。**
