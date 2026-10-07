# Kaleidoscope Tavern Compatibility

目标：Minecraft 26.3，Tavern `1.2.0.11-fabric+mc26.3`，Modrinth `FOhx6x47`；源码 `26.3-fabric` / `68af326b9e7a055c087e98ba30f523a17af70a2c`。发布 JAR 与源码方法签名、真实消费路径均已核对。可选依赖；深度 Mixin 仅此版本启用，未知版本只加载 Profile 并记录警告。

## 独立饮用与恢复

- 50 个原生 Food / Consumable 全覆盖：4 葡萄、25 瓶装饮品、14 鸡尾酒、6 果汁桶、1 空杯。空杯中性，无补水、恢复或饮食收益。
- DrinkBlockItem、CocktailBlockItem（含 Signature 子类）、JuiceBucketItem 的独立 finishUsingItem 结算一次；TWT2 仍在 ItemStack 入口结算一次。原生葡萄沿标准 FoodProperties 路径。
- 方块饮品交互只是取回物品，不算饮用。原 Brew Level、鸡尾酒效果组件、颜色和容器返还保留。
- Buildup Quality 与 Brew Level 独立。发酵等级为 0～6；陈酿不是 Feast。
- 具有治疗成分的酒（当前 Wine / Sakura Wine）按七级增加 `0 / 0 / 1 / 2 / 3 / 4 / 5 HP` Recovery Reserve。难以下咽的反胃、其余品质的微醺保留。
- 治疗型鸡尾酒将 Regeneration / Instant Health 成分合为固定 **3 HP** 储备，不随成分数量、时间或 amplifier 增长。
- 这些是 Foreign Cuisine Effect 的恢复预算，与 Profile 的直接 recovery.health 相加，之后只经过一次份额 / Variety 计算，使用原 Food Recovery 队列（12 tick/HP；Restorative 10 tick/HP）。全局上限 20 HP，Overfull 阻止新增；不瞬间 heal，不产生第二套再生时钟。
- Profile 控制直接食物恢复；原生酒效果数据控制是否具有治疗语义。高优先级数据包可分别覆盖这两层。普通原版药水不受影响。
- Tavern 破瓶生成的效果列表也移除治疗效果，避免绕过恢复预算；破瓶不提供饮用储备。
- 原生独立饮用若被数据包加入 FOOD 组件，会执行一次 FoodProperties 及相应 Overeating。无营养饮品不虚构 Hunger 成本；已有 Overfull 仍阻止恢复收益。

原酒再生的理论满额治疗（忽略满血浪费）为普通 32 HP、优良 96 HP、精酿 192 HP、陈酿 432 HP；现对应一次 2 / 3 / 4 / 5 HP 储备。低劣为 1 HP，未发酵/难以下咽为 0。

## Effect Budget

| 来源效果 | 上限 |
|---|---|
| Resistance | I，300 s |
| Strength | I 300 s / II 60 s |
| Haste | I 600 s / II 120 s |
| Health Boost | I～II，300 s |
| Fire Resistance | I，480 s |
| Night Vision / Water Breathing / Grass Stealth / XP Drain / Vision | I，900 s |
| Long Reach / Bloody Mary / Ardent Heat / Tomb Raider | I，300 s |
| 其他有益状态 | 最高 II，600 s；瞬时玩法保留 |

不修改磁盘配置或 JAR。对 Tavern 自身数据读取和效果施加进行只读裁剪，因此实际效果与原生 tooltip 一致；不改写普通 Potion。负面成本保留。

Cocktail merge：`最长持续时间 + 0.5 × 其余时间之和`，再应用预算；等级取最高后封顶。单一成分不再凭空增加 20%。运算用 long 防溢出，输出仍是原生 Entry 格式；Signature 原始组件不被覆盖。

Bloody Mary：仅实际 AFTER_DEATH、击杀者持有效效果时，`reserve += min(victim.maxHealth × 0.10, 2)`。取消原 ALLOW_DEATH 中的瞬间治疗与“阻止死亡”行为。非致命攻击无收益，自杀无收益，Overfull 无收益，储备上限 20。

审查保留：High Heels 跨步、Grass Stealth、Vision、Long Reach、XP Drain、Upside Down、Tomb Raider；Ardent Heat 冲刺破坏及原耗竭/护甲/空腹代价；Zenith 一次地表传送及饥饿代价；Shriek Attack 一次声波玩法。它们不占 Main Meal Benefit 槽位。无酒精 HUD。

## Profile

所有 Profile 均显式定义 Quality、Recovery、Hydration、Consumption Speed、Diet / Variety。酒与鸡尾酒采用 Prepared；葡萄 Basic/1 HP/轻快；水分优先级始终为 **TWT2 blacklist > Buildup explicit > TWT2 default > fallback**，显式 0/0 也生效。

| Item | Quality | HP | Thirst / Quenched | Variety Group |
|---|---|---:|---|---|
| `allium_garden` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `bloody_mary` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `brandy` | prepared | 0 | 2 / 1 | `buildup_vitals:spirits` |
| `brass_heart` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `carignan` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `champagne` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `depth_charge` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `emerald` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `empty_glassware` | basic | 0 | 0 / 0 | `buildup_vitals:wine` |
| `glow_berries_bucket` | prepared | 0 | 8 / 8 | `buildup_vitals:berry_juice` |
| `glowflower_brew` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `godfather` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `gold_grape` | basic | 1 | 4 / 2 | `buildup_vitals:grape` |
| `gold_grape_bucket` | prepared | 0 | 8 / 8 | `buildup_vitals:grape` |
| `grape` | basic | 1 | 4 / 2 | `buildup_vitals:grape` |
| `grape_bucket` | prepared | 0 | 8 / 8 | `buildup_vitals:grape` |
| `grasshopper` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `green_grape` | basic | 1 | 4 / 2 | `buildup_vitals:grape` |
| `green_grape_bucket` | prepared | 0 | 8 / 8 | `buildup_vitals:grape` |
| `honey_wine` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `ice_grape` | basic | 1 | 4 / 2 | `buildup_vitals:grape` |
| `ice_grape_bucket` | prepared | 0 | 8 / 8 | `buildup_vitals:grape` |
| `ice_wine` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `luminous_bride` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `madame_shexiang` | prepared | 0 | 2 / 1 | `buildup_vitals:spirits` |
| `miners_star` | prepared | 0 | 2 / 1 | `buildup_vitals:spirits` |
| `mojito` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `mother_snow` | prepared | 0 | 2 / 1 | `buildup_vitals:spirits` |
| `mystery_cocktail` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `nether_special` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `plum_wine` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `polaris_sweet_white` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `red_queen` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `riesling_dry_white` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `rum` | prepared | 0 | 2 / 1 | `buildup_vitals:spirits` |
| `sakura_wine` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `sauvignon_blanc_dry_white` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `screwdriver` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `sculk_special` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `sherry` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `signature_cocktail` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `sunset_glow` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `sweet_berries_bucket` | prepared | 0 | 8 / 8 | `buildup_vitals:berry_juice` |
| `sweet_berry_wine` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |
| `vinegar` | basic | 0 | 0 / 0 | `buildup_vitals:wine` |
| `vodka` | prepared | 0 | 2 / 1 | `buildup_vitals:spirits` |
| `watermelon_juice` | prepared | 0 | 8 / 8 | `buildup_vitals:berry_juice` |
| `whiskey` | prepared | 0 | 2 / 1 | `buildup_vitals:spirits` |
| `white_lady` | prepared | 0 | 5 / 4 | `buildup_vitals:cocktail` |
| `wine` | prepared | 0 | 3 / 3 | `buildup_vitals:wine` |

## 验证与人工验收

自动测试覆盖全部注册条目与消费入口、7 级发酵、反胃/微醺、单次恢复/Diet、容器、创模式、Overfull、鸡尾酒合并/等级上限、原版 Potion 隔离、真实击杀、放置取回与 TWT2 显式覆盖/黑名单；客户端检查同步及七级 tooltip，包含 integrated 和 dedicated TCP。

人工验收：

- [ ] 七级 Wine / Sakura Wine 的品质标签不变，恢复依次 0/0/1/2/3/4/5，无再生图标；微醺与反胃保留。
- [ ] 陈酿仍显示 Prepared，不显示 Feast；容器正确返回，创造模式不扣物品。
- [ ] 混合多个相同效果时，时间递减叠加且不突破上表；普通药水原效果不变。
- [ ] Bloody Mary 击杀真正死亡，HP 不瞬间跳涨；小动物更少储备，Boss 最多 2 HP。
- [ ] Overfull 时酒与击杀均不增加储备，饮水仍可解渴。
- [ ] 放置酒瓶/鸡尾酒、取回、再饮用保留品质、颜色、效果数据；只结算一次。
- [ ] TWT2 果汁 8/8、鸡尾酒 5/4、葡萄酒 3/3、烈酒 2/1、醋 0/0；黑名单最高优先。
- [ ] Juice Bucket 原一次清除状态、各特殊玩法能力及原有代价保留。

2026-10-07 自动验证：`build runClientGameTest -PwithTavern=true`，以及增加 `-PwithThirst=true -PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar`，均 PASS（113 JVM、61 server GameTest、8 client entrypoints）。最终强化 Overfull 击杀断言后 `build -PwithTavern=true -PwithThirst=true` 再次 PASS。以上命令均使用 Wrapper、Java 25 与 `-PacceptMinecraftEula=true`。
