# Kaleidoscope End Compatibility

目标：Minecraft 26.3，End `1.0.16-fabric+mc26.3` / Modrinth `V836vh70`；源码 `26.3-fabric` / `edfb6c14f03ed26b1aedf19513d664ec899892fe`，Cookery `1.6.0.3-fabric+mc26.3`。发布 JAR 的深度钩子签名已核对。End 为可选依赖，深度适配仅指定版本启用。

安装时同时需要 Cookery 和 Forge Config API Port **Fabric 26.3.1**（`JpKvrr9J`）；End 的发布元数据遗漏了实际配置库依赖。Buildup 不修改第三方配置或 JAR。

## 特殊效果

- **Mint**：保留末影人安抚；删除递归伤害重定向及 80% 通用减伤。普通 10 HP 伤害仍为 10 HP。
- **Dream**：保留移动、外观和落地保护。仍正常受到普通攻击，不提供通用无敌。
- **Void Erosion**：攻击目标有效护甲 ×0.85，保留韧性、原版公式和护甲磨损；原版 Resistance 的减伤量 ×0.85，Protection 的有效减伤量 ×0.85（先尊重原版 20 EPF 上限）。原本穿甲的伤害类型保持原行为。无额外倍率，效果等级不再增强。
- 删除 Void Erosion 的全护甲/魔法减伤绕过和致命伤 1 HP 锁血；持有效果仍可正常死亡。
- 与 Crimson 联用时有效护甲为原值 ×0.75×0.85，仍经过原版公式；各自效果不取代 Main Meal Benefit。

例：仅 Resistance I 的 10 HP 来袭伤害，原减伤 2 HP，Void Erosion 下减伤 1.7 HP，最终 8.3 HP；不会变为 10 HP 完全穿透。Protection 的预算单独保留，未改写全局 heal/hurt。

## Mythic Cuisine Budget

| 料理 | Quality / Recovery | 主要效果 |
|---|---|---|
| Dragon Souffle | Feast / 6 HP | Strength II 60 s + Resistance I 120 s |
| Fried Dragon Egg | Feast / 6 HP | Strength I 180 s + Resistance I 120 s |
| Dark Dragon Steak | Feast / 5 HP | Strength I 180 s + Void Erosion 45 s |
| Dragon Egg Custard | Feast / 6 HP | Health Boost II 300 s |
| Dragon Egg Ice Cream | Feast / 5 HP | Strength I 180 s + Void Erosion 45 s |
| Dark Dragon Egg Stew | Feast / 6 HP | Strength I 180 s + Void Erosion 45 s |
| Dragon Head with Sauce | Feast / 6 HP | Strength I 180 s + Void Erosion 45 s |

每道最多两项主要战斗/特殊效果。来自这些料理的 Regeneration 被移除，恢复只来自显式 Food Profile；不再叠加额外恢复份额。熟龙肉按普通熟食为 Basic / 1 HP，原短再生也移除；生肉/龙蛋液是原料，不因稀有自动变 Feast。

旧舒芙蕾无限 Strength II + Resistance II + Regeneration II 不再保留。旧龙排/龙首/龙蛋羹等 300 s Regeneration II 理论可治疗 240 HP，现整份最多 5～6 HP；龙蛋煲旧 120～130 s 再生约 96～104 HP，现整份 6 HP。

预算作用于 End 的真实食物效果授予入口，独立 Potion 不受影响。Recipe / 物品 ID / 容器 / 原生品质组件保留；Cookery 品质延长不会绕过上述上限。客户端在原生格式化效果与属性之前应用同一预算，因此不会显示旧无限再生或错误的 Strength 属性加成。

## Food / Drink 与份额

44 个原生消费条目全部建立独立数据驱动 Profile，包括 4 种茶。9 道可放置料理沿 Cookery 实际每口入口按整份预算分摊 Recovery / Hydration，Diet 与 Overeat 随实际进食结算；不在摆盘/取回时增加收益。End 多效果方块路径补齐已审核的第二项效果（上游只授予列表首项），不重复第一项。

Overfull 阻止新增 Food Recovery / 主餐增益；独特特殊料理效果本身按预算约束，仍可与主餐状态共存。各料理的原生 Food / Hunger / Saturation 不被改写。茶 8/6，汤炖菜 6/4，湿水果 4/2，原料/固体食品按形态赋值；TWT2 blacklist > explicit Profile > TWT2 default > fallback。

| Item | Quality | HP | Thirst / Quenched | Main Benefit |
|---|---|---:|---|---|
| `chorus_flower_cake` | prepared | 2 | 0/0 | — |
| `chorus_flower_soup` | meal | 3 | 6/4 | — |
| `chorus_flower_tea` | prepared | 0 | 8/6 | — |
| `chorus_pasta` | meal | 3 | 0/0 | — |
| `chorus_seed` | basic | 0 | 2/1 | — |
| `chorus_seed_cookie` | prepared | 2 | 0/0 | — |
| `cooked_ender_dragon_meat` | basic | 1 | 1/0 | — |
| `dark_dragon_egg_stew` | feast | 6 | 6/4 | — |
| `dark_dragon_steak` | feast | 5 | 1/0 | — |
| `dragon_breath_chorus_soup` | meal | 3 | 6/4 | kaleidoscope_cookery:vigor |
| `dragon_breath_mixed_stew` | meal | 4 | 6/4 | — |
| `dragon_breath_popping_candy` | basic | 0 | 0/0 | — |
| `dragon_egg_custard` | feast | 6 | 2/1 | — |
| `dragon_egg_ice_cream` | feast | 5 | 0/0 | — |
| `dragon_egg_liquid` | basic | 0 | 2/1 | — |
| `dragon_head_with_sauce` | feast | 6 | 0/0 | — |
| `dragon_souffle` | feast | 6 | 0/0 | — |
| `dream_berry` | basic | 1 | 4/2 | — |
| `end_caterpillar` | basic | 0 | 2/1 | — |
| `end_caterpillar_sashimi` | meal | 3 | 0/0 | — |
| `end_salad` | meal | 3 | 4/2 | — |
| `ender_dragon_tea` | prepared | 0 | 8/6 | — |
| `ender_mint_candy` | basic | 0 | 0/0 | — |
| `ender_mint_tea` | prepared | 0 | 8/6 | — |
| `fried_dragon_egg` | feast | 6 | 0/0 | — |
| `mint_chorus_mousse` | prepared | 2 | 0/0 | — |
| `mint_noodle_soup` | meal | 3 | 6/4 | — |
| `mint_sauce_shulker_meat` | meal | 3 | 1/0 | kaleidoscope_cookery:vigor |
| `mint_sauce_shulker_meat_rice_bowl` | meal | 4 | 1/0 | — |
| `optic_nerve` | basic | 0 | 2/1 | — |
| `optic_nerve_sweet_and_sour_pork` | meal | 3 | 0/0 | — |
| `raw_ender_dragon_meat` | basic | 0 | 2/1 | — |
| `raw_endermite_meat` | basic | 0 | 2/1 | — |
| `roasted_endermite_meat` | basic | 1 | 1/0 | — |
| `shulker_ice_cream` | prepared | 2 | 0/0 | — |
| `shulker_shell_meat` | basic | 0 | 2/1 | — |
| `shulker_shell_stew` | meal | 3 | 6/4 | — |
| `stir_fried_endermite_meat` | meal | 3 | 1/0 | kaleidoscope_cookery:vigor |
| `stir_fried_endermite_meat_rice_bowl` | meal | 4 | 1/0 | — |
| `stuffed_shulker` | meal | 3 | 0/0 | — |
| `stuffed_void_conch` | meal | 3 | 0/0 | — |
| `void_conch_noodle_soup` | meal | 3 | 6/4 | — |
| `void_mutton_steak` | meal | 3 | 1/0 | — |
| `void_tea` | prepared | 0 | 8/6 | — |

## 验证与人工验收

自动测试覆盖完整发布注册清单、44 次实际消费、4 茶、9 方块料理的逐口恢复/补水、Mint 安抚与普通伤害、Dream 落地/普通伤害、Void 致命伤与护甲/抗性/Protection IV 的精确计算、7 道神话料理的效果/时长/等级/储备/Overfull，客户端同步及实际 tooltip。

- [ ] Mint 可直视末影人，普通战斗无 80% 减伤。
- [ ] Dream 仍保留移动/落地能力，普通攻击可受伤。
- [ ] Void Erosion 只有限削弱防御，高等级不放大；持效果受到致命攻击仍会死亡。
- [ ] 逐道检查上表 7 道神话料理：无无限效果、无再生、无 Resistance II；龙蛋羹为 Health Boost II。
- [ ] 舒芙蕾的原版属性提示与当前 Strength II 对应，龙排为 Strength I，无旧无限时长提示。
- [ ] 各种方块料理逐口吃完只得到整份 5～6 HP 上限（非神话按各自 Profile）；第二项 Void Erosion 正常授予。
- [ ] 四种茶补水 8/6 且保留原图标/环境玩法；容器与放置/取回正常。
- [ ] Overfull 不增加食物储备；原版 Potion、Beacon、Golden Apple 仍按自身逻辑工作。

原版第三方美术/诗句 tooltip 保留；原发布资源警告不由本兼容修改。

2026-10-07：Wrapper `build runClientGameTest -PwithEnd=true`，以及添加 `-PwithThirst=true -PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar` 两组均 PASS（114 JVM、72 server GameTest、10 client entrypoints），均使用 Java 25 与 `-PacceptMinecraftEula=true`。
