# Kaleidoscope Nether Compatibility

目标：Minecraft 26.3，Nether `1.1.13-fabric+mc26.3` / Modrinth `r0AAEyh5`；源码 `26.3-fabric` / `e8a226a7f30979433d32f76d58be468f26b52459`，Cookery `1.6.0.3-fabric+mc26.3`。发布 JAR 方法已核对，Mixin 精确版本门控。

**安装依赖：** Nether 该发布包实际使用 Forge Config API Port，但 fabric.mod.json 漏报了它。测试环境显式安装 Fabric **26.3.1**（Modrinth `JpKvrr9J`）；同名 Maven `26.3.1` 会解析到另一 Loader，因此固定发布 ID。Buildup 不强制安装 Nether 或配置库，也不修改其配置。用户装 Nether 时需同时安装此库及 Cookery。

## 语义重平衡

| 效果 | 原行为 | 当前行为 |
|---|---|---|
| Star Blessing | 减伤 80%；每 10 tick 治疗最大生命 2%；后续负面免疫；完全免击退 | 护甲/抗性结算后减伤 20%；无独立治疗；获得时一次净化；固定 +0.5 击退抗性，与等级无关 |
| Crimson | ×1.5 起步且随等级增加；30% + 10 点穿甲；自定义线性护甲公式 | 移除伤害倍率；有效护甲 ×0.75；保留原版韧性、附魔、护甲磨损与公式；不随等级增强 |
| Ghost | 攀墙与灵魂粒子 | 保留 |
| Warped | 配置内下界生物安抚 | 保留 |
| Tropical Strider | 熔岩/岩浆环境通行 | 保留 |
| Mysterious Poison | 独特毒素玩法 | 保留 |

Star Blessing 尊重 BYPASSES_EFFECTS / BYPASSES_RESISTANCE；击退抗性与装备按原版属性组合，移除状态时正常移除属性。一次净化沿原生 onEffectAdded，新获得的负面效果可正常生效。

20 HP 玩家旧 Star Blessing 在 30 秒理论额外治疗 24 HP；当前为 0。普通无甲 10 HP 攻击，旧减伤后 2 HP，当前 8 HP。Crimson 无甲 8 HP 攻击不再放大；穿甲只改变 armor 输入，不将攻击改成真实伤害。

深度兼容通过 Mixin 动态选择器读取 `MixinMerged` 的**完整来源类名 + 原 handler 名**定位目标，不依赖运行时随机方法哈希、不修改 JAR、不包装全局 heal/hurt。目标缺失会明确失败；未知版本不启用深度适配。

## 消费路径与 Profile

85 个实际 Food / Consumable 条目均有独立 Profile。原生 FoodProperties 结算一次，不改 Hunger / Saturation。4 道可放置料理沿 Cookery FoodBiteBlock，整份 Profile 的 Recovery 与 Hydration 分摊到实际口数；整数补水余数累积分配，不会每口向上取整。

永恒牛排保留物品、冷却与原动画，每次 **1 HP**；正常计入 Overeating，Overfull 阻止新增 Recovery。原生 Warmth / Satiated Shield 使用已重构 Cookery 行为。含 Vigor 的料理显式占 Main Meal Benefit 槽位，其他特殊效果可共存。

汤炖菜 6/4，汤面 5/3，水果沙拉 4/2；干燥肉食可为 0/0。TWT2 优先级仍为 blacklist > explicit Profile > TWT2 default > fallback。

| Item | Quality | HP | Thirst / Quenched | Main Benefit |
|---|---|---:|---|---|
| `black_apple_salad` | prepared | 2 | 4/2 | — |
| `blaze_soup` | meal | 3 | 6/4 | — |
| `blazing_kabob` | prepared | 2 | 0/0 | — |
| `braised_lion_head` | meal | 4 | 0/0 | — |
| `braised_pork_rice` | meal | 4 | 0/0 | — |
| `braised_strider` | meal | 3 | 0/0 | kaleidoscope_cookery:vigor |
| `caramel_nether_caterpillar` | meal | 3 | 0/0 | kaleidoscope_cookery:vigor |
| `caramel_nether_caterpillar_rice` | meal | 3 | 0/0 | — |
| `chongqing_noodles` | meal | 3 | 5/3 | — |
| `cooked_piglin_meat` | basic | 1 | 0/0 | — |
| `cooked_strider_meat` | basic | 1 | 0/0 | — |
| `corn_carrot_pork_rib_soup` | feast | 5 | 6/4 | — |
| `couples_lung_slice` | meal | 3 | 0/0 | — |
| `crimson_fruit` | basic | 1 | 4/2 | — |
| `crimson_kabob` | prepared | 2 | 0/0 | — |
| `crimson_magma_stew` | meal | 3 | 6/4 | — |
| `crimson_salad` | prepared | 2 | 4/2 | — |
| `everlasting_flame_steak` | basic | 1 | 0/0 | — |
| `forgetfulness_soup` | meal | 3 | 6/4 | — |
| `fruit_platter` | meal | 3 | 4/2 | — |
| `garlic_oysters` | meal | 3 | 0/0 | — |
| `ghast_kabob` | prepared | 2 | 0/0 | — |
| `ghast_pasta` | meal | 3 | 0/0 | — |
| `ghast_pudding` | prepared | 2 | 0/0 | kaleidoscope_cookery:vigor |
| `ghast_tentacle` | basic | 0 | 0/0 | — |
| `giant_beast_croissant` | prepared | 2 | 0/0 | — |
| `gilded_barbaric_roast` | feast | 5 | 0/0 | — |
| `glowing_kabob` | prepared | 2 | 0/0 | — |
| `glowing_pudding` | prepared | 2 | 0/0 | — |
| `glowing_salad` | prepared | 2 | 4/2 | — |
| `glowing_soup` | meal | 3 | 6/4 | — |
| `golden_kabob` | prepared | 2 | 0/0 | kaleidoscope_cookery:vigor |
| `golden_roast` | meal | 3 | 0/0 | kaleidoscope_cookery:vigor |
| `ham` | basic | 0 | 0/0 | — |
| `ham_slice` | basic | 0 | 0/0 | — |
| `ham_yogurt` | meal | 3 | 0/0 | — |
| `hoglin_tusk_braised_meat` | feast | 5 | 0/0 | — |
| `lava_jelly` | prepared | 2 | 0/0 | — |
| `lava_roasted_chicken` | meal | 3 | 0/0 | — |
| `luosifen` | meal | 3 | 5/3 | — |
| `magma_cream_pudding` | prepared | 2 | 0/0 | — |
| `magma_cream_soup` | meal | 3 | 6/4 | — |
| `magma_cream_stir_fry` | meal | 3 | 0/0 | kaleidoscope_cookery:vigor |
| `magma_cream_stir_fry_rice` | meal | 3 | 0/0 | — |
| `magma_sweet_and_sour_pork` | meal | 3 | 0/0 | — |
| `mapo_tofu` | meal | 3 | 0/0 | kaleidoscope_cookery:vigor |
| `mapo_tofu_rice` | meal | 3 | 0/0 | — |
| `nether_caterpillar` | basic | 0 | 0/0 | — |
| `nether_caterpillar_sashimi` | meal | 3 | 0/0 | — |
| `nether_fries_platter` | feast | 5 | 0/0 | — |
| `nether_reed_stew` | meal | 3 | 6/4 | — |
| `pepper_pork_belly_chicken_soup` | feast | 5 | 6/4 | — |
| `poisonous_fruit` | basic | 0 | 4/2 | — |
| `poisonous_ghast_roast` | meal | 3 | 0/0 | — |
| `poisonous_soup` | meal | 3 | 6/4 | — |
| `raw_piglin_meat` | basic | 0 | 0/0 | — |
| `raw_strider_meat` | basic | 0 | 0/0 | — |
| `roasted_ghast_tentacle` | basic | 1 | 0/0 | — |
| `roasted_ham` | prepared | 2 | 0/0 | — |
| `roujiamo` | prepared | 2 | 0/0 | — |
| `ruby_steak` | meal | 3 | 0/0 | — |
| `sauerkraut_fish` | meal | 3 | 0/0 | kaleidoscope_cookery:vigor |
| `soul_glazed_roast` | meal | 3 | 0/0 | — |
| `soul_lamb_chop` | meal | 3 | 0/0 | — |
| `soul_pepper` | basic | 0 | 0/0 | — |
| `soul_pepper_stir_fry` | meal | 3 | 0/0 | — |
| `soul_return_rice` | meal | 4 | 0/0 | — |
| `soul_soup` | meal | 3 | 6/4 | — |
| `soul_stir_fry_meat` | meal | 3 | 0/0 | kaleidoscope_cookery:vigor |
| `soul_stir_fry_meat_rice` | meal | 3 | 0/0 | — |
| `soul_strider_kabob` | prepared | 2 | 0/0 | — |
| `spicy_hoglin_ramen` | meal | 3 | 5/3 | — |
| `spicy_pot` | meal | 3 | 0/0 | kaleidoscope_cookery:vigor |
| `spicy_pot_rice` | meal | 4 | 0/0 | — |
| `star_ghast_pasta` | meal | 3 | 0/0 | — |
| `star_stew` | meal | 4 | 6/4 | — |
| `star_stew_meat` | meal | 4 | 6/4 | — |
| `strider_nether_wart_stew` | meal | 3 | 6/4 | — |
| `strider_shell_stir_fry` | meal | 3 | 0/0 | — |
| `warped_cake` | prepared | 2 | 0/0 | — |
| `warped_fruit` | basic | 1 | 4/2 | — |
| `warped_hoglin_tenderloin_stew` | meal | 3 | 6/4 | — |
| `warped_kabob` | prepared | 2 | 0/0 | — |
| `warped_salad` | prepared | 2 | 4/2 | — |
| `wither_bone_soup` | meal | 3 | 6/4 | kaleidoscope_cookery:vigor |

## 验收

自动测试覆盖完整发布清单、85 次实际消费、Star 一次净化/后续负面/无周期治疗/非完全免击退、Crimson 多等级有甲无甲对照、永恒牛排冷却/Overfull、4 道多口料理、TWT2 实际补水及黑名单、Ghost 攀墙与 Warped 安抚、客户端 Profile/Tooltip 同步。

- [ ] 获得 Star Blessing 清除已有负面，之后可再次中毒；不会每半秒自行回血。
- [ ] Star Blessing 普通伤害减少 20%，仍会击退；移除后击退抗性恢复。
- [ ] Crimson 无甲目标伤害不放大，有甲目标有限穿透，高等级不继续增强。
- [ ] Ghost 攀墙、Warped 安抚、Tropical Strider 环境能力、回魂饭/孟婆汤传送保留。
- [ ] 永恒牛排吃完保留并进入冷却，恢复 1 HP；连续进食导致 Overfull，之后不增加恢复储备。
- [ ] 灵魂羊排 3 口、卤肉饭 4 口、红烧狮子头 5 口、玉米胡萝卜排骨汤 3 口；全部吃完只获得整份 Recovery/Hydration。
- [ ] 汤仍提供补水，Vigor 只有一条主增益提示；普通 Potion 不受影响。

原发布客户端有部分模型 particle 纹理引用警告；兼容不更改第三方美术资源，人工验收留意外观。

2026-10-07：Wrapper `build runClientGameTest -PwithNether=true`，以及添加 `-PwithThirst=true -PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar` 两组均 PASS，114 JVM、67 server GameTest、9 client entrypoints。均使用 Java 25 与 `-PacceptMinecraftEula=true`，Cookery/配置库自动加入测试依赖。
