# More Delight Compatibility — Minecraft 26.3

可选支持 **More Delight 26.09.16-26.3-fabric**，配套 **Delight Lib 26.09.16-26.3-fabric** 和 **FD Refabricated 26.3-3.6.27**。31 种已注册 Food / Consumable 全部提供显式 Food Profile；木刀、石刀不属于食物。没有新增恢复机制、主增益、Mixin 或 Comfort 玩法。

## 版本证据与依赖

- [More Delight 官方发布](https://modrinth.com/mod/more-delight/version/pX0gsjqf)，SHA-1 `f0ec4121af9901895c4c32bbddd08d3930969ce6`。
- [对应 26.3 源码](https://github.com/axperty/moredelight/tree/360e61172756e8b22d4d647753321002c7452d35)。已逐项核对发布 JAR 的注册字节码、配方、组件及运行时注册表；不是按贴图文件名推断食品。芝士汉堡、芝士/花生酱吐司等残留资源没有实际物品注册，不为它们生成无效 Profile。
- [Delight Lib 官方发布](https://modrinth.com/mod/delight-lib/version/dABf1m0k)，SHA-1 `393b42752dd42463b8bfea99ebb615742b0ad00b`。核对 `FoodBuilder` 字节码：生成 FD `ConsumableItem`，用原版 `ApplyStatusEffectsConsumeEffect` 授予效果，fast 为 16 tick，碗料理保留碗容器。
- Fabric Loader 将版本中的 `09` 规范化为 `9`，运行时显示 `26.9.16-26.3-fabric`，与上述发布为同一版本。
- Buildup 不要求安装 More Delight 或 Delight Lib，不引用其类、不捆绑 JAR；More Delight 自己的必需依赖仍须安装。固定 Maven 版本仅在开发/测试参数 `-PwithMoreDelight=true` 时加入运行时，同时加载 FD 和 Delight Lib。未声明新的编译依赖。
- More Delight 存在时启用内置数据包；其他版本保留数据加载并提示未验证。FD 的行为桥接仍由既有精确版本门控控制。未验证版本不视为已兼容。

## 平衡与行为

沿用 [Balance v2](../BALANCE_SPEC_V2.md)，不改变 Core 参数：Natural 10/80、Food 12、Restorative/Nourishment Food 10 tick；连续 Saturation 公式与同一恢复时钟。

普通马铃薯丁、面包片和煎蛋卷 1 HP；干吐司 0；简单加工食品 1–2；米饭和土豆正餐 3；奶油肉意面与丰盛汉堡 4。没有仅因营养高就升级 Feast，也不为没有原生主增益的食品自动补上主增益。

六种 Nourishment 料理保留 `farmersdelight:nourishment` 的 ID、图标和原生 **3600 tick**，通过既有 [FD 桥接](FARMERS_DELIGHT_COMPAT.md) 提供调养 + 振奋语义。只显示一个主要增益；Variety 不乘原生时长；与 Buildup 原生主增益互斥。Overfull 阻止新储备和 Nourishment 首次授予/刷新，包含触发积食的当次消费。

土豆沙拉和鸡肉沙拉保留 **Regeneration I / 100 tick** 独立效果，额外 Recovery 为 0。它们不占主增益槽，积食不屏蔽独立再生。原生 Hunger、Saturation、配方、容器均保留；没有全局治疗拦截。

面包片/干吐司与原版面包共组，马铃薯丁与原版马铃薯共组，煎蛋卷与 FD 煎蛋共组。汉堡、鸡肉三明治、培根三明治变体复用 FD 对应组；三种肉饭、两种奶油肉意面、四种土豆拼盘、五种配料吐司分别共组。保留真实配方带来的类别差异，不因仅换肉类就增加多个 Variety Group。冰棍配方为可可和冰，不臆加 Dairy；米饭/奶油意面/胡萝卜汤含牛奶，列入 Dairy。

面包片、马铃薯丁、冰棍和吐司为 fast=16 tick；其余 normal=32 tick。创造模式同样使用速度档位，但不授予恢复储备和主要饮食增益。积食最终时长 ×1.25。

补水仍只有 TWT2 结算一次：blacklist > Buildup explicit Profile（含 0）> TWT2 配置/默认 > fallback。胡萝卜汤 6/4，干吐司 0/0；Variety 不放大补水，积食不阻止补水。无 TWT2 时不创建口渴机制；有/无 AppleSkin 复用既有水滴显示。

## 数据包覆盖

内置路径：`resourcepacks/more_delight/data/buildup_vitals/buildup_vitals/food_profiles/moredelight/<item>.json`，Profile ID 为 `buildup_vitals:moredelight/<item>`，包 ID 为 `buildup_vitals:more_delight`。只在安装 More Delight 时自动启用。

整合包用 `data/buildup_vitals/buildup_vitals/food_profiles/moredelight/<item>.json` 同路径完整覆盖，或添加自己的显式 Item Profile。遵循 [Schema v1 与优先级](FOOD_PROFILES.md)。省略 `meal_benefit` 可阻止之后的原生 Nourishment 授予，不清除之前已有效果；不能填写字符串 `none`。改变 Quality 不截短原生 Nourishment 时长。成功 `/reload` 更新服务端和客户端；删除覆盖后恢复内置值。

```text
/buildupvitals food profile moredelight:carrot_soup
/buildupvitals food profile moredelight:toast
/buildupvitals recovery
/buildupvitals diet
```

## 全量 Profile

ID 省略 `moredelight:`；水分为 Thirst/Quenched。N 为原生 Nourishment 3600 tick，R 为原生 Regeneration I 100 tick，`—` 无新主增益。Recovery 单位为 HP；1 HP=半颗心。所有分组均为数据字段，可由整合包覆盖。

| Item | H/S | Quality | HP | 水分 | 速度 | Diet | Variety Group | 效果 |
|---|---|---|---:|---|---|---|---|---|
| diced_potatoes | 2/1.6 | basic | 1 | 2/1 | fast | vegetable | minecraft:potato | — |
| chocolate_popsicle | 3/1.2 | prepared | 1 | 3/2 | fast | sweet | moredelight:chocolate_popsicle | — |
| omelette | 6/7.2 | basic | 1 | 1/0 | normal | protein | farmersdelight:fried_egg | — |
| cooked_rice_with_chicken_cuts | 14/22.4 | meal | 3 | 2/1 | normal | protein, grain, dairy | moredelight:meat_rice | N |
| cooked_rice_with_beef | 14/22.4 | meal | 3 | 2/1 | normal | protein, grain, dairy | moredelight:meat_rice | N |
| cooked_rice_with_porkchop | 14/22.4 | meal | 3 | 2/1 | normal | protein, grain, dairy | moredelight:meat_rice | N |
| creamy_pasta_with_ham | 12/19.2 | meal | 4 | 3/2 | normal | protein, grain, vegetable, dairy | moredelight:creamy_meat_pasta | N |
| creamy_pasta_with_chicken_cuts | 12/19.2 | meal | 4 | 3/2 | normal | protein, grain, vegetable, dairy | moredelight:creamy_meat_pasta | N |
| mashed_potatoes | 12/19.2 | prepared | 2 | 3/2 | normal | vegetable, dairy | moredelight:mashed_potatoes | — |
| diced_potatoes_with_chicken_cuts | 10/16.0 | meal | 3 | 2/1 | normal | protein, vegetable | moredelight:potato_plate | — |
| diced_potatoes_with_beef | 10/16.0 | meal | 3 | 2/1 | normal | protein, vegetable | moredelight:potato_plate | — |
| diced_potatoes_with_porkchop | 10/16.0 | meal | 3 | 2/1 | normal | protein, vegetable | moredelight:potato_plate | — |
| diced_potatoes_with_egg_and_tomato | 10/16.0 | meal | 3 | 3/2 | normal | protein, vegetable | moredelight:potato_plate | — |
| potato_salad | 6/7.2 | prepared | 0 | 3/2 | normal | protein, vegetable | moredelight:potato_salad | R |
| chicken_salad | 6/7.2 | prepared | 0 | 4/3 | normal | protein, vegetable | moredelight:chicken_salad | R |
| carrot_soup | 12/19.2 | meal | 3 | 6/4 | normal | vegetable, dairy | moredelight:carrot_soup | N |
| simple_hamburger | 8/12.8 | prepared | 2 | 1/0 | normal | protein, grain | farmersdelight:hamburger | — |
| hamburger_with_egg | 9/14.4 | prepared | 2 | 1/0 | normal | protein, grain | farmersdelight:hamburger | — |
| loaded_hamburger | 13/20.8 | meal | 4 | 2/1 | normal | protein, grain, vegetable | farmersdelight:hamburger | — |
| chicken_sandwich_with_egg_and_tomato | 11/17.6 | meal | 3 | 2/1 | normal | protein, grain, vegetable | farmersdelight:chicken_sandwich | — |
| steak_sandwich | 10/16.0 | meal | 3 | 2/1 | normal | protein, grain, vegetable | moredelight:meat_sandwich | — |
| porkchop_sandwich | 10/16.0 | meal | 3 | 2/1 | normal | protein, grain, vegetable | moredelight:meat_sandwich | — |
| egg_with_bacon_sandwich | 11/17.6 | meal | 3 | 1/0 | normal | protein, grain | farmersdelight:bacon_sandwich | — |
| tomato_sandwich | 7/11.2 | prepared | 2 | 3/2 | normal | grain, vegetable | moredelight:tomato_sandwich | — |
| bread_slice | 2/1.6 | basic | 1 | 1/0 | fast | grain | minecraft:bread | — |
| toast | 3/2.4 | basic | 0 | 0/0 | fast | grain | minecraft:bread | — |
| toast_with_egg | 5/6.0 | prepared | 1 | 1/0 | fast | grain, protein | moredelight:topped_toast | — |
| toast_with_honey | 5/6.0 | prepared | 1 | 1/0 | fast | grain, sweet | moredelight:topped_toast | — |
| toast_with_sweet_berries | 5/6.0 | prepared | 1 | 2/1 | fast | grain, fruit | moredelight:topped_toast | — |
| toast_with_glow_berries | 5/6.0 | prepared | 1 | 2/1 | fast | grain, fruit | moredelight:topped_toast | — |
| toast_with_chocolate | 5/6.0 | prepared | 1 | 1/0 | fast | grain, sweet | moredelight:topped_toast | — |

## 验证与人工验收

在 Java 25、已接受 Minecraft 测试 EULA 后运行：

```powershell
.\gradlew.bat build runClientGameTest -PwithMoreDelight=true -PacceptMinecraftEula=true
# 补水组合追加 -PwithThirst=true
# AppleSkin 组合追加 '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar'
# Iris 启动回归追加 '-PtestClientModsDir=run/compat/bootstrap'
```

新增测试覆盖目标发布的完整运行时注册表、原生营养/消费组件/效果/容器、31 种实际消费、积食、创造模式、Recovery、Variety 分组及 TWT2 显式补水/黑名单。客户端覆盖全部 31 种普通/高级 tooltip、两种语言、集成服、同 JVM DedicatedServer TCP、覆盖/删除/reload 和重连。独立服务端进程通过 Wrapper 的服务端 GameTest 验证；不把同 JVM TCP 当作独立多进程多人压力测试。

2026-10-07 本轮109项JVM、50项注册服务端GameTest通过；客户端共有6个测试入口。以下组合全部通过。More Delight 行均包含目标 FD 与 Delight Lib：

| 组合 | Wrapper build / JVM / 服务端GameTest | 客户端 / DedicatedServer TCP |
|---|---|---|
| 无 FD、无 More Delight | PASS；验证附属包不生效 | 复用已通过的Core证据，本轮未重跑客户端 |
| 仅 FD，无 More Delight | PASS；验证附属包不生效 | 复用已通过的FD证据，本轮未重跑客户端 |
| More Delight | PASS | PASS |
| More Delight + TWT2 | PASS | PASS |
| More Delight + AppleSkin | PASS | PASS |
| More Delight + TWT2 + AppleSkin + Iris/Sodium/AsyncLogger | PASS | PASS |

未安装对应模组时，条件测试仅验证缺省路径，不视为执行了其消费断言。最后一组使用 Iris `1.11.6+mc26.3`、Sodium `0.9.3-alpha.1+mc26.3`、AsyncLogger `2.2.2+26.1.2-fabric`、TWT2 `1.6.2+26.3` 和 AppleSkin `3.0.10+mc26.3`，确认未重现Identifier提前加载崩溃；没有另行声称验证所有光影包或完整整合包。中英文AppleSkin及完整组合截图均已检查，滋养只出现一次。

全部152份内置Profile（40原版Item + 1示例Tag + 80 FD + 31 More Delight）检查无非预期正数小于1 HP或小数基础Recovery。发布JAR包含31份附属Profile，不包含测试类、清单fixture或第三方JAR。日志及工作报告归档于工作区根目录 `docs/26.3/`，不进入版本提交。

- [ ] 安装目标 More Delight、Delight Lib 和 FD，进入单人及专服；移除 More Delight/Delight Lib 后 Buildup 和 FD 仍能启动。
- [ ] 查看胡萝卜汤、肉饭和奶油意面：只显示一条 Nourishment，食用后约 3 分钟，具有食物恢复加速与活动耗竭优惠。
- [ ] 先吃蘑菇煲再吃胡萝卜汤，以及反向进食，确认主要效果互斥。
- [ ] Hunger 不足 18 时吃胡萝卜汤，3 HP 储备约 30 tick 兑现；中途受击不暂停。半血、高饱和时仍以自然 10 tick 基准恢复。
- [ ] 沙拉显示并授予 5 秒再生，不显示额外恢复量；干吐司没有恢复与补水，配料吐司 1 HP。
- [ ] 生存/创造中面包片、冰棍和吐司约 16 tick，正式饭菜 32 tick；创造不累积储备。积食后变为 20/40 tick。
- [ ] 连续满饥饿进食触发积食后，不增加储备、不刷新 Nourishment，但营养、饮食记录、补水与独立沙拉再生保留。
- [ ] 轮换不同肉饭/汉堡/配料吐司，查询 Diet：同一家族共组；轮换不同料理种类可获得有限 Variety 奖励。
- [ ] TWT2 下胡萝卜汤实际 6/4，干吐司 0/0；配置改为 19/19 仍服从 Profile，blacklist 禁用补水。分别核对有/无 AppleSkin 的水滴和单行效果提示。
- [ ] 数据包改胡萝卜汤为 1 HP、quick、3/1 补水，主增益依次改调养/省略/Nourishment；reload 后行为和 tooltip 一致，删除覆盖恢复默认。
- [ ] 在实际整合包及所用光影下进行正常游玩验收；自动化结果不代表全部模组组合和长期多人存档均已验证。
