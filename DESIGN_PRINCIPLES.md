# Buildup Vitals — Design Principles

> 工作名称：**Buildup Vitals**  
> 首要目标平台：**Fabric 26.3**  
> 文档状态：**v2 核心设计原则（Stage 7.5）**
> 当前重点：**饥饿、饱和、食物恢复、饮食质量、饮食多样性、口渴兼容**  
> 当前兼容：**Farmer's Delight Refabricated 26.3-3.6.27，以及 More Delight 26.09.16-26.3-fabric 的 Food Profile 与料理增益语义桥接**
> 后续方向：**其他料理模组、氧气、法力等其他玩家状态资源**

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

v2 总恢复节奏以接近原版的量级为目标，将恢复价值分配到 Saturation、食物储备、料理增益和可选 Hydration。

- **Well-fed Recovery**：Hunger=20 且 Saturation>0，每 10 tick 恢复 `min(Saturation,6)/6 HP`，随 Saturation 连续变化，与原版峰值节奏对齐。
- **Stable Recovery**：Hunger≥18 且不满足上一条件，每 80 tick 恢复 1 HP，不要求最低 Saturation。
- 自然恢复按实际成功恢复的 HP 支付每 HP 6 Exhaustion；Buildup 速度奖励组合后的自然周期不得小于 10 tick。药水等外部治疗不受此上限限制。

具体原型值与行为以 `BALANCE_SPEC_V2.md` 为准，人工验收后可以小幅微调。

Passive Natural Recovery直接以原版作为baseline，当前高饱和基准为10 tick，撤销此前12→11 tick方案。初始半血、满Hunger、Saturation20且无额外活动时，普通回满与原版同为100 tick。Food Recovery、Meal Benefit、Hydration、Variety等主动或良好状态收益允许提供有限正反馈，不再要求所有组合严格落在原版±10%内，也不因主动进食快约10%～20%而再次削弱自然恢复。通过统一时钟、有限储备和10 tick自然下限防止明显失控的叠加治疗。

## 8. 食物直接恢复生命

部分食物可以拥有独立的生命恢复能力，但不应简单采用“吃下料理后瞬间 +N HP”的方式。

Buildup Vitals 使用 **Food Recovery Reserve（食物恢复储备）**。

例如一道料理具有 `Recovery = 4 HP`，并不代表玩家立刻获得 4 HP，而是吃下后向 Recovery Reserve 中加入 4 HP，再在随后一段时间逐渐转换为真正的 Health。

v2 普通食物储备每 12 tick 最多转换 1 HP，Restorative 为 10 tick；总储备上限 20 HP。满血保留、死亡清空，跨维度与重连保留。

**官方 Food Profile 的 `recovery.health` 最小非零单位为 1 HP（半颗心）**；0 表示不提供直接恢复。官方平衡优先采用 1 / 2 / 3 / 4 / 5 / 6 整数 HP，不再推荐 0.5 HP。普通水果、蔬菜、面包和熟制单品约 1，Prepared 约 1～2，Meal 约 3～4，Feast 约 4～6；普通生肉生鱼、干燥曲奇、特殊用途或独立强治疗食品可为 0，不能机械向上取整或给金苹果堆高 Recovery。

该规则约束官方基础数据，不收紧第三方数据包 Schema，也不取整 Variety 加成、自然饱和恢复或实际治疗尾数。

这样既能让料理真正成为治疗体系的一部分，又不会让食物退化为廉价瞬间治疗药水，同时也为其他机制提供稳定接口。

## 9. Food Recovery 不因受到攻击而暂停

这一原则明确固定：

> **受到攻击、进入战斗或刚刚损失生命，都不会暂停已经获得的 Food Recovery Reserve。**

食物既然已经吃下，其治疗价值就应持续兑现。Buildup Vitals 不使用“受击后暂停食物恢复”“战斗状态下食物恢复失效”等规则。

系统主要依靠食物使用时间、Recovery Reserve 的渐进恢复与总体恢复速度来控制战斗中的食物治疗强度。

## 10. Recovery Controller

Food Recovery 与 Natural Recovery 不应毫无控制地直接叠加，因此需要统一的 Recovery Controller 协调自然类型的生命恢复。

有可执行储备时优先由食物支付治疗，周期取食物与当前自然周期的较小值，少量储备尾数可以由自然恢复补足。**Food Reserve 不得拖慢玩家原本应有的自然恢复**，也不使用两个并行恢复时钟。受击不重置进度。

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

v2 Meal Benefit 正式注册为 Minecraft MobEffect，使用原版 HUD、背包图标、同步和倒计时，食物授予默认无粒子。不制作默认 Potion 或酿造配方；效果行为仍由 Recovery Controller 和特定活动消耗入口兑现。

第一批适合探索的 Meal Benefit 包括：

- **Restorative / 调养**：食物储备周期 12→10 tick，不增加最终储备总量。
- **Invigorated / 振奋**：原版疾跑、跳跃、游泳及水中移动耗竭降低 10%，不减免自然恢复或全局耗竭。
- **Steady / 安适**：完成注册、图标与互斥，占位保留；没有正式行为前不加入官方食物 Profile。

Meal Benefit 不应只是“吃饭后获得 Speed I / Regeneration I”的药水效果合集。

## 14. Meal Benefit 与第三方料理 Buff 的边界

Buildup Vitals 不应重复实现已有烹饪 Mod 的主要 Buff 语义。尤其应避免直接重复长时间维持 Saturation、自动持续回血、用 Hunger / Saturation 抵消大量伤害等方向。

Buildup 自己更适合关注 Recovery Reserve、Vitals 稳定性、活动消耗、饮食多样性以及 Quality 与中期状态的联动。

第三方料理 Buff 可以经明确、经过目标版本验证的适配器注册为 **Foreign Main Meal Benefit**。保留第三方真实效果的 ID、图标、原生持续时间及存储同步，只桥接其行为语义，不注册替身效果、不另建玩家状态或第二份倒计时。Buildup 自身 Restorative、Invigorated、Steady 注册始终保留。

当前适配 `farmersdelight:nourishment`：内部同时满足 Restorative 的 10 tick/HP 食物恢复和 Invigorated 的活动耗竭 ×0.9；界面仅保留 Nourishment。其目标版本没有独立 heal，实际冲突是每 tick 耗竭返还，适配中和该返还，避免特殊 Hunger/Saturation 经济绕开统一 Recovery。自然恢复、药水、信标、金苹果和其他独立治疗不受全局拦截。

原生 Nourishment 食用效果的 600/1200/3600/6000 tick 时长与授予概率原样保留，本轮不附加 Variety 时长倍率。Variety 的恢复量奖励仍只结算一次。数据包为没有原生 Nourishment 消费效果的食品显式选择该 ID 时，才使用 Buildup Quality 时长及最多 3600 tick 的默认授予规则。

适配器保持可选依赖、服务端权威和精确版本边界；缺少 FD 时不加载 FD 类或注册 Foreign 语义，未知版本关闭行为桥接并警告。官方兼容包使用现有 Food Profile Schema，整合包仍可覆盖恢复、补水、类别、速度与主增益，最终 Profile 决定食物授予，不能被第三方后续消费回调覆盖。

## 14.1 Foreign Cuisine Effect 与 Effect Review Gate

Foreign Cuisine Effect 是第三方料理效果的语义层，保留原 ID、图标和原版效果存储；只有明确标为 Main Meal Benefit 的效果才进入互斥主槽。当前 Cookery Vigor 只提供 Invigorated 的活动耗竭 ×0.9，不提供 Restorative，不额外显示振奋图标。既有 FD Nourishment 桥接保持不变。

特殊料理效果可以与主增益共存。Ghost 攀墙、Warped 安抚、Tropical Strider 环境通行、Dream 移动/落地保护、Mint 末影人安抚等应保留身份；不因为它们来自食物就强行塞入主槽。可共存不等于可以无限叠加恢复或战斗收益。

所有官方强料理适配必须先经过 **Effect Review Gate**：核对目标发布 JAR / 源码的真实消费、周期、属性和事件入口；逐项分类为保留、语义桥接或预算重平衡；计算整次治疗、减伤、穿甲和持续时间；验证组合、死亡、积食及客户端显示。深度 Mixin 精确版本门控，未知版本只保留数据包并警告，不猜测新签名。不修改第三方配置，不 patch JAR，不包装全局 heal / hurt。

长时间料理 Regeneration 优先转换为有限 Food Recovery；转换量和直接食物恢复共同进入一个 Controller、一次 Variety 奖励和 20 HP 储备上限，不能重复治疗。Cookery / End 的转换已经包含在 Profile 内；Tavern 治疗酒按 Brew Level 提供 0～5 HP、治疗鸡尾酒合计 3 HP，作为受限效果预算加入同一储备。Bloody Mary 仅真实击杀后加入目标最大生命 10%、至多 2 HP，受积食和储备上限限制。普通 Potion、Beacon、Golden Apple，以及已审核保留的 FD / More Delight 独立短治疗不受全局接管。

强战斗料理按等级、时长、总收益和叠加路径共同预算。当前 Satiated Shield 在常规防御后抵消 20%、单次最多 4 HP，每抵消 1 HP 消耗 1 Saturation，不借用 Hunger；Star Blessing 为 20% 减伤、一次净化和固定 +0.5 击退抗性，无周期治疗或持续负面免疫。Crimson 只削减 25% 有效护甲；Void Erosion 只削减 15% 有效护甲及 Resistance / Protection 的减伤量，继续经过原版防御公式。Mint 不再提供 80% 通用减伤。

Tavern 的 Effect Budget 限制增益等级与时长，鸡尾酒合并采用最长时间加其余时间的一半，再裁剪到预算。Mythic Cuisine 每道最多两项主要战斗/特殊效果，完整 Recovery 为 5～6 HP；Strength II 至多 60 秒，通常 Strength I 180 秒、Resistance I 120 秒或 Void Erosion 45 秒，龙蛋羹为 Health Boost II 300 秒。禁止官方龙料理提供无限 Strength / Resistance / Regeneration。具体逐项预算见当前版本兼容文档。

Warmth 不独立治疗：只对 Stable Recovery 提供热源旁 64 tick、下界 72 tick 的环境周期，二者取较强者；普通 80 tick、高饱和 10 tick 和自然最快 10 tick 均保留。料理环境加速之间取最大值，不连乘；TWT2 Quenched 仍遵循既定规则。

方块、多口和可重复食物以实际消费为结算点。整盘 Recovery / Hydration 按真实口数分摊，整数补水余数累计分配；每口实际营养进入 Overeating。放置、取回和空容器不发放消费收益。Tavern 独立 Drink 路径同样只结算一次，原生品质、容器和独特玩法保留。

## 15. Meal Benefit 的堆叠原则

Buildup 原生主增益与已注册 Foreign Main Meal Benefit 共用一个主槽：

- 同类型 Benefit：刷新，不累加时长；
- 不同类型 Benefit：新的主要 Benefit 替换旧的；
- Feast 当前也只有一个主增益，没有正式 Secondary Modifier 槽位。

未注册桥接的第三方效果、Special Cuisine Effect 与独立药水效果不占主槽。Nourishment、Vigor 与 Restorative / Invigorated / Steady 双向互斥，后授予的异类生效，直接命令授予也遵守互斥。Overfull 独立共存，阻止 Foreign Main 首次授予与刷新，也阻止新增 Food Reserve；不移除已经生效的主增益，不抹掉营养、Hydration 或饮食记录。FD Comfort 不作为当前正式玩法恢复。

## 16. 负面 Meal Benefit

普通 Food Item 在满 Hunger 时仍可进食。以进食前 Hunger 计算超过缺失 Hunger 的 Nutrition，将溢出部分加入 Overeat Load；正常补饥饿不受惩罚。

默认负荷 48 一次轻提示、64 触发 Overfull / 积食、80 封顶、每 40 tick 消化 1 点，严格低于 32 解除。只多吃一两份料理不会立即受到严重惩罚，不新增胃容量常驻 HUD。

积食暂停储备兑现，阻止新储备和主增益授予/刷新，继续进食时长 ×1.25；Hunger、Saturation、Hydration、自然恢复和药水等外部治疗保留。积食独立于主增益槽位。负荷保存与重连保留、死亡清空；不因食物重复或类别不均衡而增加负荷。

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
- Well-fed Natural Recovery：保留最高+7.5%的Variety速度参数，但当前基准已达10 tick下限，不额外提高该档位实际频率；Variety的恢复储备与增益时长奖励仍有效。

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

牛排继续拥有高 Hunger、高 Saturation、1 HP 基础 Recovery、1/0 Hydration、Basic Quality、Protein Category，因此仍然适合旅行、探索和冒险。生肉可有略多水分（约 2/0），但恢复更少且保留原生风险。

正式料理则可以拥有类似的基础饱食能力，但同时提供更好的 Recovery、Hydration、Variety Contribution 与 Meal Benefit。

二者不是弱食物和强食物，而是不同使用场景的食物。

Feast 的主要优势同样应是“更全面”，而不是所有单项数值都达到最大。

## 25. Hydration

口渴系统第一阶段不从零实现，Buildup Vitals 优先将 Thirst Was Taken 2 作为可选兼容 Mod，并通过 Hydration Adapter 接入。

没有安装口渴 Mod 时，其他 Buildup Vitals 功能仍应正常工作。

绝大多数普通食物至少有少量 Hydration，真正干燥的曲奇、干海带才为 0。普通熟食约 1/0、生肉约 2/0、生鱼约 2/1、水果 3～5、汤 5～8 Thirst。Pure Water Bottle 为 10 Thirst / 8 Quenched，两瓶可以从空口渴补满；脏水、浑水和盐水继续保留上游纯度与疾病规则。

适配优先级为 TWT2 blacklist > Buildup 显式 Profile（包括 0）> TWT2 配置/drinks > 通用 fallback。TWT2 独立 Quenched 治疗由 Buildup 协调为满 Thirst 且 Quenched>0 时自然恢复速度 ×1.15，受 10 tick 上限约束，不写用户配置文件。具体注入仅启用于已验证的 1.6.2+26.3。

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

## 30.1 Consumption Speed 与 Snack

Food Profile 的可选 `consumption.speed` 分 normal / quick / fast 三档，以普通食物 32 tick 为基准分别使用 32 / 21 / 16 tick。未声明则保留物品原本时长；积食在最后乘 1.25。档位随服务端 Profile 同步，实际结算始终由服务端决定。

曲奇、干海带、浆果、西瓜片适合 fast；苹果、胡萝卜、甜菜根、马铃薯适合 quick；正式料理保留正常进食动作。Snack 以低 Nutrition、低过食负荷和短使用时间形成自己的用途，不应只看作缩水料理。

普通 Tooltip 使用图标与短文字：爱心 + 基础 HP、效果图标 + 名称、非 normal 速度提示。效果卡提供倒计时与简短悬停说明；复杂系数留给高级提示或调试。

## 31. v2 平衡原型目标

以下是 Stage 7.5 实现和人工验收的起点，不代表最终平衡已经锁死：

| 项目 | 原型目标 |
|---|---:|
| Stable Natural Recovery | Hunger≥18，1 HP / 80 tick |
| Well-fed Natural Recovery | Hunger=20、Saturation>0，每 10 tick 恢复 min(Saturation,6)/6 HP |
| Basic Food Recovery | 0～1 HP |
| Prepared Recovery | 1～2 HP |
| Meal Recovery | 2～4 HP |
| Feast Recovery | 3～5 HP |
| Recovery Reserve 转化速度 | 普通 12 tick / HP；Restorative 10 tick / HP |
| 官方 Food Recovery 基础单位 | 0 或至少 1 HP（半颗心），优先整数 1～6 HP |
| Variety 最大 Recovery Bonus | 约 +10%～15% |
| Variety 对 Natural Recovery | 保留+7.5%速度参数；当前Well-fed基准已达10 tick下限，不再提速 |
| Variety 对 Meal Benefit | 约 +10% 上限 |
| 重复食物额外收益最低倍率 | 约 85%～90% |
| 重复食物 Hunger 倍率 | 100% |
| 重复食物 Saturation 倍率 | 100% |

这些数值主要用于确认设计体验是否正确，而不是在设计阶段追求最终数学平衡。

## 32. 第一阶段开发重点

第一阶段优先完成：Food Profile 数据系统；Hunger / Saturation 与自然恢复的重新协调；Recovery Controller；Food Recovery Reserve；四档 Food Quality；Meal Benefit 基础框架；Diet Memory；Dietary Variety；重复饮食的轻量额外收益衰减；Thirst Was Taken 2 Adapter；Food Hydration；基础 Tooltip / UI 信息表达；Data Pack 覆盖与兼容框架。

完成这些内容以后，饥饿、料理、恢复与口渴应已经形成完整 Gameplay Loop。

## 33. 后续阶段

Core 已冻结，当前有 FD 本体的 80 份 Food / Consumable Profile 与 Nourishment 语义桥接，以及 More Delight 26.09.16-26.3-fabric 的31份Profile。其六种Nourishment料理保留3600 tick，两种独立再生沙拉不附加Recovery；同配方家族共用Variety Group。Kaleidoscope Cookery / Tavern / Nether / End 已按顺序加入 Foreign Cuisine Effect 和独立内置包，共299份Profile（含2个中性容器），目标版本与预算见 `docs/KALEIDOSCOPE_COMPAT.md`。其他Addon、Oxygen / Air、Mana / Stamina 仍需后续独立授权与版本验证。

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
