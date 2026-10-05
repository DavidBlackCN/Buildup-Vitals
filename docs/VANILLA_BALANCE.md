# 原版食物 Alpha 平衡包

Stage 7 为 26.3 注册表中同时拥有 FOOD 与 CONSUMABLE 的全部 **40 种原版物品**提供具体 Item Profile。文件位于 `src/main/resources/data/buildup_vitals/buildup_vitals/food_profiles/`；旧的 `example_fruit.json` 和标签保留，共 41 份定义。具体 Item 优先于 Tag，西瓜片和甜浆果现在命中各自官方定义。

这是一套可玩的起始数据，不是最终平衡结论。所有食物保留原版 Hunger、Saturation、进食时间、堆叠、容器和特殊效果；不增加负面状态。下面的 Recovery 单位为 HP（2 HP = 1 颗心），表示逐渐兑现的储备，非瞬间治疗。没有列出的额外数值均为零，不表示原版营养为零。

## 食物定位

| 食物 | Quality | Recovery | Thirst / Quenched | 类别 | 主要增益 |
| --- | --- | ---: | --- | --- | --- |
| 面包 | Basic | 0 | 0 / 0 | grain | 无 |
| 生/熟牛肉、猪肉、羊肉 | Basic | 0 | 0 / 0 | protein | 无 |
| 生/熟鸡肉、兔肉 | Basic | 0 | 0 / 0 | protein | 无 |
| 生/熟鳕鱼、鲑鱼，热带鱼、河豚 | Basic | 0 | 0 / 0 | protein | 无 |
| 马铃薯、烤马铃薯、毒马铃薯、胡萝卜、金胡萝卜、甜菜根、干海带 | Basic | 0 | 0 / 0 | vegetable | 无 |
| 苹果 | Basic | 0 | 2 / 0 | fruit | 无 |
| 西瓜片 | Basic | 0 | 3 / 1 | fruit | 无 |
| 甜浆果、发光浆果、紫颂果 | Basic | 0 | 1 / 0 | fruit | 无 |
| 金苹果、附魔金苹果 | Basic | 0 | 0 / 0 | fruit | 保留原版效果 |
| 蜂蜜瓶 | Basic | 0 | 0 / 0 | sweet | 保留原版清毒 |
| 腐肉、蜘蛛眼 | Basic | 0 | 0 / 0 | 无 | 保留原版风险 |
| 曲奇 | Prepared | 0 | 0 / 0 | grain, sweet | 无 |
| 南瓜派 | Prepared | 1 | 0 / 0 | grain, sweet | Invigorated |
| 蘑菇煲、甜菜汤 | Meal | 3 | 4 / 2 | vegetable | Restorative |
| 兔肉煲 | Meal | 4 | 4 / 2 | protein, vegetable | Invigorated |
| 可疑炖菜 | Meal | 1 | 4 / 2 | vegetable | 保留原版花朵效果，无额外主要增益 |

牛排仍是高营养、便于携带的口粮；料理通过恢复、补水和有限增益体现价值。兔肉煲为探索提供活动消耗减免，蔬菜汤偏向加快现有恢复储备的兑现。曲奇不因为 Prepared 自动获得治疗；镀金食物也不因为稀有就自动成为 Feast。Steady 尚未实现，本包不发放。当前原版没有专门安排 Feast。

## Variety Group

分组只控制多样性额外奖励，不影响任何原版营养。牛/猪/羊的生熟变体共用 `minecraft:cooked_beef`；鸡/兔共用 `minecraft:cooked_chicken`；鱼类共用 `minecraft:fish`。如此替换口粮不会凭相近肉类无限刷分。

马铃薯三种变体共用 `minecraft:potato`，胡萝卜与金胡萝卜共用 `minecraft:carrot`；苹果及两种金苹果共用 `minecraft:apple`；两种浆果共用 `minecraft:sweet_berries`；可疑炖菜与蘑菇煲共用 `minecraft:mushroom_stew`。其它食物用本物品 ID。保持旧牛排、苹果、蘑菇煲等分组 ID，不修改已保存的饮食快照。

没有为凑齐六类而虚构 dairy 食物。牛奶没有 FOOD 组件，蛋糕是方块逐口食用，均不经过当前消费入口，不新增它们的恢复、增益和饮食记忆。药水也保持独立；未知 Mod 食物继续正常进食并使用 fallback。

## 调整入口与单位

- 单种食物：覆盖相同 Profile 路径的 JSON，执行 `/reload`。完整格式和优先级见 [FOOD_PROFILES.md](FOOD_PROFILES.md)。例如覆盖 `data/buildup_vitals/buildup_vitals/food_profiles/rabbit_stew.json`；不编辑 JAR。
- 核心默认参数：`src/main/java/com/davidblackcn/buildupvitals/config/CoreBalance.java` 集中 Recovery、Benefits、Variety 的起始值，原有三个 Balance 类保留为引用入口。它是编译时预设，**不是**可热重载的用户配置文件；改动后必须重建。
- 核心时序维持此前已验收值：普通食物储备 50 ticks/HP、Restorative 40、Stable 120、Well-fed 80；20 ticks = 1 秒。储备上限 20 HP，主要增益上限 3600 ticks，饮食窗口 10 次。
- 存档边界与参数有关：调低储备/增益上限、改变窗口或计时范围前必须审查旧存档兼容，不应只改数字后发布。本轮没有改变这些值或持久化格式。
- Tooltip 暂显示基础数值，多样性奖励另查 `/buildupvitals diet`；恢复与增益另查 `/buildupvitals recovery`。后续仍按“icon + 短文字”方向改进。

## 可选口渴与验收边界

上述补水列是 Buildup 定义。安装受支持 TWT2 时，其 blacklist、配置和原生 drinks 数据包仍优先；默认配置覆盖苹果、汤等许多项目，不保证最终数值等于本表。移除对应上游配置条目后才测试 Buildup 数值，显式 `0/0` 仍是有效覆盖。详见 [HYDRATION.md](HYDRATION.md)。不安装 TWT2 时不启用任何口渴机制。

TWT2 的独立 Quenched 治疗及 AppleSkin 的原版治疗预测仍是已知整合边界。本包没有擅自更改第三方配置。是否需要协调，应由后续实玩记录决定。

人工长时间试玩场景与记录方式见 [Stage 7 报告](STAGE_7_REPORT.md)。自动检查证明数据接通、规则符合预期，不能代替洞穴探索、连续夜战或整段生存流程的体验判断。
