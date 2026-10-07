# Kaleidoscope Cookery Compatibility

## 版本与边界

- Minecraft 26.3 Fabric；Cookery `1.6.0.3-fabric+mc26.3`。
- 发布：[Modrinth Yf5VytNq](https://modrinth.com/mod/kaleidoscope-cookery-refabricated/version/Yf5VytNq)。源码：`NightEpiphany/KaleidoscopeCookery` 的 `26.3-fabric`，提交 `88a8b1f03a2438a96ff0d2d742fdfeecedd2c91b`。
- 可选依赖；无 Cookery 时不启用数据包或注入。未知版本保留显式 Profile 数据，关闭深度行为桥接并警告。插件选取阶段只读取 Fabric 元数据。
- 不修改第三方配置或 JAR；不包装全局 heal/hurt。

## 效果语义

Vigor 是 Foreign Main Meal Benefit，仅等效 Invigorated（指定活动 exhaustion ×0.9），与 Buildup 原生主增益及 FD Nourishment 互斥。保留自己的 ID、图标、原生时长；不另加 Invigorated 图标，不加速 Food Recovery。最终 Profile 决定食物是否授予 Vigor；Overfull 阻止授予和刷新。上游方块路径只执行每组效果的第一项，兼容层补发位于后续项的主增益，仍保留该项原生概率和时长；其它次要效果不额外补发。

Warmth 与 Satiated Shield 属于 Foreign Cuisine Effect，可与主增益共存，不占主槽。

- **Satiated Shield**：Hunger≥18、有 Saturation、未 Overfull；护甲/抗性/附魔减伤后、吸收生命前，抵消 `min(伤害×20%, 4 HP, Saturation)`。抵消 1 HP 直接花费 1 Saturation，绝不借用 Hunger。跳过不可格挡、绕过效果/抗性/无敌的伤害；不递归调用 hurt。上游 ALLOW_DAMAGE 护盾回调停用。
- **Warmth**：取消玩家独立 heal；延续周围 5×3×5 热源标签或点燃方块检测，最多每25 tick刷新。Stable Recovery 热源旁64 tick，下界72 tick，二者同时存在取64；无热源主世界仍80。高 Saturation 仍10 tick基准。TWT2 Quenched 可继续按既定规则作用，但不会突破10 tick下限。
- **料理 Regeneration**：粽子、翻糖派、黄金沙拉、辣子鸡、花茶的独立再生归入表中 Food Recovery 总预算，不再额外授予再生。花茶2 HP；黄金料理不额外堆高恢复。普通药水、信标与金苹果的独立治疗不受影响。
- **保留** Preservation、Projectile Dodge、Sulfur、Flatulence、Hinder、Instant Smelting、Vitality、Tundra Strider、Mustard；审查确认它们不是本文替换的通用恢复来源。Preservation 的食物相关负面效果处理保留，并非全面持续负面免疫；Vitality 是击杀后幼体生成能力。

## 消费与数据契约

独立内置包 `resourcepacks/kaleidoscope_cookery`。发布 JAR 的实际注册表清单冻结在测试资源 `cookery-inventory.json`，共120项：119种食品/饮品，外加1个代吃容器。官方完整物品 Recovery 为0或整数1–6 HP；摆放后每口可自然分摊为小数。

- 普通 Food/Consumable 走现有消费回调；碗和盆等容器继续由上游返还。
- 茶杯/瓦罐奶茶独立 `finishUsingItem` 只补接 Recovery/Diet，不重复补水；TWT2 自己的 ItemStack 回调已覆盖它们。
- 所有 FoodBiteBlock（包括1×2和3×3盘）按真实 `maxBites` 分摊整盘 Recovery；每个实际吃下的口记录一次 Diet，重复分组不创造新的食材多样性。Overeat 使用该口实际上游 nutrition，包含上游品质倍率。
- 方块每口 Hydration 采用 `floor(total*(i+1)/N)-floor(total*i/N)`；吃完整盘恰好等于整盘 Profile，总量不会因逐口向上取整而膨胀。部分食用只获得已消费份额，拿取/摆放不结算。
- 茶杯摆放后右键取回物品，实际饮用时结算；不是凭拿取免费授予补水。
- 炼金餐袋本身0恢复/0补水；内部实际消费物品逐个走原入口，不给整个袋子额外恢复。
- TWT2：blacklist > Buildup explicit（包括0/0）> TWT2 default > fallback。饱腹不阻止补水。
- 原生制作品质、Hunger/Saturation、配方、概率效果和容器保留。Buildup Quality 表示料理定位，与上游制作品质独立。
- Tooltip显示实际 Buildup预算；去除过期的料理再生和重复Vigor行，保留其它效果与原生制作品质。纯茶、方块专用食品同样显示Profile。

## Profile 清单

Hydration列为 Thirst/Quenched，Recovery为完整物品HP。份数0表示普通物品消费；火腿片只支持摆放进食。主增益只有表中标记Vigor的食品，其他特殊效果不占主槽。

| Item ID（省略命名空间） | Quality | HP | Hydration | 份数 | Diet | 主增益 |
|---|---|---:|---|---:|---|---|
| `clay_pot_milk_tea` | prepared | 1 | 8/6 | 0 | dairy | — |
| `cold_cut_ham_slices` | prepared | 2 | 1/0 | 8 | protein | — |
| `transmutation_lunch_bag` | basic | 0 | 0/0 | 0 | — | — |
| `tomato` | basic | 1 | 4/2 | 0 | vegetable | — |
| `red_chili` | basic | 0 | 0/0 | 0 | vegetable | — |
| `green_chili` | basic | 0 | 0/0 | 0 | vegetable | — |
| `lettuce` | basic | 1 | 4/2 | 0 | vegetable | — |
| `caterpillar` | basic | 0 | 0/0 | 0 | protein | — |
| `fried_egg` | basic | 1 | 0/0 | 0 | protein | — |
| `tea_egg` | prepared | 2 | 1/0 | 0 | protein | — |
| `donkey_burger` | meal | 3 | 1/0 | 0 | protein, grain, vegetable | — |
| `mantou` | basic | 1 | 0/0 | 0 | grain | — |
| `baozi` | prepared | 2 | 1/0 | 0 | protein, grain | — |
| `shengjian_mantou` | prepared | 2 | 1/0 | 0 | protein, grain | — |
| `qingtuan` | prepared | 2 | 0/0 | 0 | — | — |
| `sticky_candy` | basic | 0 | 0/0 | 0 | sweet | — |
| `sticky_rice_cake` | prepared | 2 | 0/0 | 0 | grain | — |
| `zongzi` | prepared | 2 | 1/0 | 0 | protein, grain | — |
| `bamboo_tube_rice` | meal | 3 | 1/0 | 0 | grain | — |
| `samsa` | prepared | 2 | 1/0 | 0 | protein, grain | — |
| `meat_pie` | prepared | 2 | 1/0 | 0 | protein, grain | — |
| `dumpling` | prepared | 2 | 1/0 | 0 | protein, grain | — |
| `cooked_rice` | basic | 1 | 0/0 | 0 | grain | — |
| `scramble_egg_with_tomatoes` | prepared | 2 | 1/0 | 0 | protein, vegetable | Vigor |
| `scramble_egg_with_tomatoes_rice_bowl` | meal | 3 | 1/0 | 0 | protein, vegetable, grain | Vigor |
| `stir_fried_beef_offal` | prepared | 2 | 1/0 | 0 | protein, vegetable | — |
| `stir_fried_beef_offal_rice_bowl` | meal | 3 | 1/0 | 0 | protein, vegetable, grain | — |
| `braised_beef` | prepared | 2 | 1/0 | 0 | protein, vegetable | — |
| `braised_beef_rice_bowl` | meal | 3 | 1/0 | 0 | protein, vegetable, grain | — |
| `stir_fried_pork_with_peppers` | prepared | 2 | 1/0 | 0 | protein, vegetable | — |
| `stir_fried_pork_with_peppers_rice_bowl` | meal | 3 | 1/0 | 0 | protein, vegetable, grain | — |
| `sweet_and_sour_pork` | prepared | 2 | 1/0 | 0 | protein, sweet | — |
| `sweet_and_sour_pork_rice_bowl` | meal | 3 | 1/0 | 0 | protein, sweet, grain | — |
| `country_style_mixed_vegetables` | prepared | 2 | 4/2 | 0 | vegetable | Vigor |
| `fish_flavored_shredded_pork` | prepared | 2 | 1/0 | 0 | protein, vegetable | — |
| `fish_flavored_shredded_pork_rice_bowl` | meal | 3 | 1/0 | 0 | protein, vegetable, grain | — |
| `braised_fish_rice_bowl` | meal | 3 | 1/0 | 0 | protein, grain | — |
| `spicy_chicken_rice_bowl` | meal | 3 | 1/0 | 0 | protein, vegetable, grain | — |
| `suspicious_stir_fry_rice_bowl` | meal | 3 | 1/0 | 0 | grain | — |
| `egg_fried_rice` | meal | 3 | 1/0 | 0 | protein, grain | — |
| `delicious_egg_fried_rice` | meal | 4 | 1/0 | 0 | protein, grain, vegetable | — |
| `pork_bone_soup` | meal | 3 | 6/4 | 0 | protein | Vigor |
| `seafood_miso_soup` | meal | 3 | 6/4 | 0 | protein, vegetable | — |
| `fearsome_thick_soup` | meal | 3 | 6/4 | 0 | protein | — |
| `lamb_and_radish_soup` | meal | 3 | 6/4 | 0 | protein, vegetable | — |
| `braised_beef_with_potatoes` | meal | 3 | 6/4 | 0 | protein, vegetable | — |
| `wild_mushroom_rabbit_soup` | meal | 3 | 6/4 | 0 | protein, vegetable | — |
| `tomato_beef_brisket_soup` | meal | 3 | 6/4 | 0 | protein, vegetable | — |
| `pufferfish_soup` | meal | 3 | 6/4 | 0 | protein, vegetable | — |
| `borscht` | meal | 3 | 6/4 | 0 | protein, vegetable | — |
| `beef_meatball_soup` | meal | 3 | 6/4 | 0 | protein, vegetable | — |
| `chicken_and_mushroom_stew` | meal | 3 | 6/4 | 0 | protein, vegetable | — |
| `donkey_soup` | meal | 3 | 6/4 | 0 | protein | — |
| `laba_congee` | meal | 3 | 6/4 | 0 | grain, sweet | — |
| `beef_noodle` | meal | 3 | 5/3 | 0 | protein, grain | — |
| `hui_noodle` | meal | 3 | 5/3 | 0 | protein, grain | — |
| `udon_noodle` | meal | 3 | 5/3 | 0 | protein, grain, vegetable | — |
| `hot_dry_noodles` | meal | 3 | 1/0 | 0 | grain, vegetable | — |
| `sashimi` | basic | 0 | 0/0 | 0 | protein | — |
| `raw_lamb_chops` | basic | 0 | 0/0 | 0 | protein | — |
| `raw_cow_offal` | basic | 0 | 0/0 | 0 | protein | — |
| `raw_pork_belly` | basic | 0 | 0/0 | 0 | protein | — |
| `raw_donkey_meat` | basic | 0 | 0/0 | 0 | protein | — |
| `raw_cut_small_meats` | basic | 0 | 0/0 | 0 | protein | — |
| `raw_meatball` | basic | 0 | 0/0 | 0 | protein, vegetable | — |
| `cooked_lamb_chops` | basic | 1 | 1/0 | 0 | protein | — |
| `cooked_cow_offal` | basic | 1 | 1/0 | 0 | protein | — |
| `cooked_pork_belly` | basic | 1 | 1/0 | 0 | protein | — |
| `cooked_donkey_meat` | basic | 1 | 1/0 | 0 | protein | — |
| `cooked_cut_small_meats` | basic | 1 | 1/0 | 0 | protein | — |
| `cooked_meatball` | basic | 1 | 1/0 | 0 | protein, vegetable | — |
| `mystery_tea` | basic | 0 | 8/6 | 0 | — | — |
| `butter_tea` | basic | 0 | 8/6 | 0 | dairy | — |
| `barley_tea` | basic | 0 | 8/6 | 0 | grain | — |
| `tieguanyin` | basic | 0 | 8/6 | 0 | — | — |
| `biluochun` | basic | 0 | 8/6 | 0 | — | — |
| `oolong` | basic | 0 | 8/6 | 0 | — | — |
| `sakura_fubuki` | basic | 0 | 8/6 | 0 | — | — |
| `flower_tea` | prepared | 2 | 8/6 | 0 | — | — |
| `dark_cuisine` | prepared | 0 | 0/0 | 3 | — | — |
| `suspicious_stir_fry` | prepared | 2 | 1/0 | 1 | — | — |
| `slime_ball_meal` | prepared | 0 | 0/0 | 3 | — | — |
| `fondant_pie` | prepared | 2 | 1/0 | 4 | grain, vegetable, sweet | — |
| `dongpo_pork` | meal | 4 | 1/0 | 3 | protein | — |
| `fondant_spider_eye` | prepared | 2 | 0/0 | 4 | sweet | — |
| `chorus_fried_egg` | meal | 3 | 1/0 | 3 | protein, fruit | — |
| `braised_fish` | meal | 3 | 1/0 | 4 | protein | — |
| `spicy_chicken` | meal | 3 | 1/0 | 4 | protein, vegetable | — |
| `yakitori` | meal | 3 | 1/0 | 4 | protein, vegetable | — |
| `pan_seared_knight_steak` | meal | 3 | 1/0 | 4 | protein, fruit | — |
| `stargazy_pie` | meal | 3 | 1/0 | 4 | protein, grain, vegetable, sweet | — |
| `sweet_and_sour_ender_pearls` | prepared | 0 | 0/0 | 3 | — | — |
| `crystal_lamb_chop` | meal | 3 | 1/0 | 3 | protein | — |
| `blaze_lamb_chop` | meal | 3 | 1/0 | 3 | protein | — |
| `frost_lamb_chop` | meal | 3 | 1/0 | 3 | protein | — |
| `nether_style_sashimi` | meal | 3 | 1/0 | 4 | protein, vegetable | — |
| `end_style_sashimi` | meal | 3 | 1/0 | 4 | protein, fruit | — |
| `desert_style_sashimi` | meal | 3 | 1/0 | 4 | protein, vegetable | — |
| `tundra_style_sashimi` | meal | 3 | 1/0 | 4 | protein | Vigor |
| `cold_style_sashimi` | meal | 3 | 1/0 | 4 | protein | Vigor |
| `candied_potato` | prepared | 2 | 1/0 | 3 | vegetable, sweet | — |
| `stuffed_tiger_skin_pepper` | meal | 3 | 1/0 | 5 | protein, vegetable | — |
| `spicy_rabbit_head` | meal | 3 | 1/0 | 3 | protein, vegetable | — |
| `fried_caterpillar` | prepared | 2 | 1/0 | 3 | protein | — |
| `fried_spring_roll` | meal | 3 | 1/0 | 3 | protein, vegetable | — |
| `fruit_platter` | prepared | 2 | 5/3 | 4 | fruit | — |
| `braised_pork_ribs` | meal | 4 | 1/0 | 4 | protein | — |
| `cold_roasted_meat` | meal | 4 | 1/0 | 3 | protein | — |
| `oil_splashed_fish` | meal | 3 | 1/0 | 5 | protein, vegetable | — |
| `dough_drop_soup` | meal | 3 | 6/4 | 3 | grain, vegetable | — |
| `four_joy_meatball_soup` | meal | 4 | 6/4 | 4 | protein, vegetable | — |
| `numbing_spicy_chicken` | meal | 3 | 1/0 | 3 | protein, vegetable | — |
| `spicy_blood_stew` | meal | 4 | 6/4 | 3 | protein, vegetable | — |
| `brown_mushroom_pot_soup` | meal | 3 | 6/4 | 2 | vegetable | — |
| `red_mushroom_pot_soup` | meal | 3 | 6/4 | 2 | vegetable | — |
| `warped_fungus_pot_soup` | meal | 3 | 6/4 | 2 | vegetable | — |
| `crimson_fungus_pot_soup` | meal | 3 | 6/4 | 2 | vegetable | — |
| `buddha_jumps_over_the_wall` | feast | 5 | 6/4 | 2 | protein, vegetable | — |
| `shelf_mushroom_pot_soup` | meal | 3 | 6/4 | 2 | vegetable | — |
| `golden_salad` | meal | 2 | 5/3 | 6 | fruit, vegetable | — |

## 验证与人工验收

已执行 `build runClientGameTest -PacceptMinecraftEula=true` 的五种组合：无 Cookery、Cookery、Cookery+TWT2、Cookery+AppleSkin、Cookery+TWT2+AppleSkin，全部通过。Cookery使用 `-PwithCookery=true`，TWT2使用 `-PwithThirst=true`，AppleSkin通过固定本地测试JAR传入。112项JVM测试通过；新增方块后续项Vigor和护甲/不可格挡回归后，最终Cookery+TWT2+AppleSkin重跑通过56项服务端GameTest及7个客户端入口。客户端覆盖集成服、dedicated TCP、120项Profile同步、119项食物/饮品tooltip、双语截图与主增益/0补水/速度的数据包重载及删除恢复。

工作日志、截图和自检记录保存在工作区根目录 `docs/26.3/`，不打入模组或纳入26.3提交。测试明确保留原版药水路径，官方Profile无非预期正值小于1HP。人工视觉/战斗体感仍按下列清单验收。

- 生存模式吃大骨汤：只显示Vigor，替换已有Restorative/Nourishment；跑跳游泳消耗减少10%。
- Overfull后继续吃同料理：不增加Recovery，不刷新Vigor；饮水仍有效。
- 护盾在高/低Saturation、18/17 Hunger与Overfull状态下受近战/投射物攻击，观察Saturation扣费和每击4HP上限；虚空不可被抵消。
- Warmth在主世界远离/靠近热源、下界有/无热源时，分别确认80/64/72/64 tick Stable节奏；高Saturation不快于10tick。
- 分别手持食用、摆放后逐口食用1×2料理和3×3火腿盘；Recovery/Hydration不按整盘重复发放，容器正常返回。
- 花茶、瓦罐奶茶、粽子和辣子鸡实际效果与tooltip一致；其它独特效果正常。
- 使用餐袋内部食物：只计内部食品，普通药水仍能独立治疗。
- TWT2显式0/0、黑名单与Data Pack覆盖按优先级生效；服务端重载、重连后客户端显示同步。
