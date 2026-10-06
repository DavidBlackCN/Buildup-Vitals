# 原版食物平衡包 v2

覆盖 Minecraft 26.3 中同时具有 Food 与 Consumable 的全部40种原版物品。目录 `src/main/resources/data/buildup_vitals/buildup_vitals/food_profiles/`，另保留旧 example_fruit Tag Profile，共41份定义。Item 优先于 Tag；未知 Mod 食物保持可靠 fallback。

Recovery 单位 HP（2 HP=一颗心），官方最小非零基础值为 **1 HP（半颗心）**；0 表示无直接恢复，优先采用整数 1～6 HP。它表示基础储备而非瞬间治疗，实际还受 Variety、20 HP上限和积食影响，加成和裁剪后的尾数仍可为小数。营养、容器、堆叠及特殊效果仍由原版控制；消费档位是本阶段明确增加的覆盖。native 表示未声明速度，保留物品原始时长；quick=21、fast=16 tick，积食最后 ×1.25。

本轮按定位调整，而非统一向上取整：普通水果、蔬菜、面包与熟肉/熟鱼为1；生肉和生鱼为0，保留其营养和较高补水，让烹饪带来明确恢复收益。南瓜派2，正式汤料理3～4；干燥曲奇和干海带为0，曲奇保留 Prepared 品质和快速使用。紫颂果保留独立传送用途，直接恢复为0；金苹果、附魔金苹果、蜂蜜瓶、可疑炖菜继续为0。原版没有硬塞 Feast，Steady 未投放。

Hydration 仅在受支持 TWT2 中结算：blacklist > Buildup Profile > 上游配置/drinks > fallback。Pure Water Bottle 不属于40种 Food，单独按纯度匹配10/8；水质/疾病语义见 [HYDRATION](HYDRATION.md)。

## 多样性分组

分组沿用 Stage 7，保持旧饮食快照语义：牛/猪/羊生熟共用 cooked_beef；鸡/兔共用 cooked_chicken；鱼类共用 fish；马铃薯变体共组，胡萝卜/金胡萝卜共组，苹果/金苹果共组，两种浆果共组，可疑炖菜/蘑菇煲共组。其余使用各自物品ID。只约束额外多样性奖励，不削减基础营养。

## 完整数据表

下表由本次40份已提交 Profile 核对生成，ID 省略 minecraft 命名空间。数值为 v2 原型，长期战斗和生存体验仍待人工验收。

| 食物 ID | Quality | Recovery HP | Thirst / Quenched | 速度 | 主增益 | 类别 |
|---|---|---:|---|---|---|---|
| apple | basic | 1 | 4 / 2 | quick | - | fruit |
| baked_potato | basic | 1 | 1 / 0 | native | - | vegetable |
| beef | basic | 0 | 2 / 0 | native | - | protein |
| beetroot | basic | 1 | 3 / 1 | quick | - | vegetable |
| beetroot_soup | meal | 3 | 6 / 4 | native | restorative | vegetable |
| bread | basic | 1 | 1 / 0 | native | - | grain |
| carrot | basic | 1 | 3 / 1 | quick | - | vegetable |
| chicken | basic | 0 | 2 / 0 | native | - | protein |
| chorus_fruit | basic | 0 | 3 / 1 | native | - | fruit |
| cod | basic | 0 | 2 / 1 | native | - | protein |
| cooked_beef | basic | 1 | 1 / 0 | native | - | protein |
| cooked_chicken | basic | 1 | 1 / 0 | native | - | protein |
| cooked_cod | basic | 1 | 1 / 0 | native | - | protein |
| cooked_mutton | basic | 1 | 1 / 0 | native | - | protein |
| cooked_porkchop | basic | 1 | 1 / 0 | native | - | protein |
| cooked_rabbit | basic | 1 | 1 / 0 | native | - | protein |
| cooked_salmon | basic | 1 | 1 / 0 | native | - | protein |
| cookie | prepared | 0 | 0 / 0 | fast | - | grain, sweet |
| dried_kelp | basic | 0 | 0 / 0 | fast | - | vegetable |
| enchanted_golden_apple | basic | 0 | 3 / 1 | native | - | fruit |
| glow_berries | basic | 1 | 3 / 1 | fast | - | fruit |
| golden_apple | basic | 0 | 3 / 1 | native | - | fruit |
| golden_carrot | basic | 1 | 2 / 1 | native | - | vegetable |
| honey_bottle | basic | 0 | 3 / 1 | native | - | sweet |
| melon_slice | basic | 1 | 5 / 3 | fast | - | fruit |
| mushroom_stew | meal | 3 | 6 / 4 | native | restorative | vegetable |
| mutton | basic | 0 | 2 / 0 | native | - | protein |
| poisonous_potato | basic | 0 | 2 / 1 | native | - | vegetable |
| porkchop | basic | 0 | 2 / 0 | native | - | protein |
| potato | basic | 1 | 2 / 1 | quick | - | vegetable |
| pufferfish | basic | 0 | 2 / 1 | native | - | protein |
| pumpkin_pie | prepared | 2 | 1 / 0 | native | invigorated | grain, sweet |
| rabbit | basic | 0 | 2 / 0 | native | - | protein |
| rabbit_stew | meal | 4 | 8 / 6 | native | invigorated | protein, vegetable |
| rotten_flesh | basic | 0 | 1 / 0 | native | - | - |
| salmon | basic | 0 | 2 / 1 | native | - | protein |
| spider_eye | basic | 0 | 1 / 0 | native | - | - |
| suspicious_stew | meal | 0 | 6 / 4 | native | - | vegetable |
| sweet_berries | basic | 1 | 3 / 1 | fast | - | fruit |
| tropical_fish | basic | 0 | 2 / 1 | native | - | protein |
