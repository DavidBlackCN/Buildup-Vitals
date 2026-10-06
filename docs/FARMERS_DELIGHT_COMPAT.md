# Farmer's Delight Compatibility — Minecraft 26.3

支持 **Farmer's Delight Refabricated 26.3-3.6.27**，运行时完整版本字符串为 `26.3-3.6.27+refabricated`。FD 是可选依赖，Buildup 自己的 Restorative / Invigorated / Steady 注册始终存在。此兼容不包含 FD Addon、Kaleidoscope Cookery、Oxygen、Mana 或 Comfort 玩法。

## 版本与源码证据

- [官方发布](https://modrinth.com/mod/farmers-delight-refabricated/version/26.3-3.6.27)，Modrinth version ID `hTNvMewX`；JAR SHA-1：`8c00b5f004fafe157df7ddb241d32f28f458a4d6`。
- [实际源码提交](https://github.com/MehVahdJukaar/FarmersDelightRefabricated/tree/615259dd453adec97b045765566a8f982b79cd71)，对应 `fabric/latest/26.3`、3.6.27。发布 JAR 的注册组件和 Nourishment 方法字节码也已核对。
- 本项目目标：MC 26.3 / Java 25 / Gradle 9.7.1 / Loom 1.18.2 / Loader 0.19.5 / Fabric API 0.161.0+26.3；使用目标版本非混淆名称，无 Yarn。
- FD 为 MIT。本项目不复制其实现、贴图或捆绑 JAR。Maven 固定 `compileOnly`；开发/测试加 `-PwithFarmersDelight=true` 才加入运行时，发布元数据仅 `suggests`。

缺少 FD 时不注册其资源包或 Foreign 语义，所有 FD Mixin 经插件关闭。非已验证版本仍可读取内置 Profile，但关闭行为桥接并输出版本警告；这不代表该版本已兼容，需重新核对注册表和方法再测试。

Mixin 准备阶段只允许调用 `FarmersDelightVersion` 的 Loader 元数据检查，不能调用持有 `Identifier` 或其他 Minecraft 类型的运行时兼容类。否则会过早加载游戏类，令 Iris 等其他模组的 Mixin 发生 `MixinTargetAlreadyLoadedException`。JVM 回归测试用隔离 ClassLoader 禁止游戏/可选模组类加载，覆盖 FD 与 TWT2 两个 Mixin 插件。

本地可用 `-PtestClientModsDir=run/compat/bootstrap` 为 GameTest 加入启动回归 JAR；目录内仅放要验证的客户端兼容组件及其依赖，不放第二份 Buildup。此参数不增加发布依赖，也不捆绑测试模组。

## Nourishment 语义桥接

`farmersdelight:nourishment` 是真实的 Foreign Main Meal Benefit：原 ID、图标、计时、保存、同步、移除语义及 FD 成就判断均保留。只展示 Nourishment，不同时添加 Buildup 调养和振奋。其行为为：

- Food Recovery 周期 10 tick/HP；只加速储备兑现，不创造储备或额外回血。
- 与振奋相同的疾跑、跳跃、游泳及水中移动耗竭 ×0.9；自然恢复成本和通用耗竭不打折。
- 放大器不进一步放大上述收益。与原生 Restorative / Invigorated / Steady 共用一个互斥主槽，新异类替换旧效果；独立药水和未桥接的第三方效果不占槽。

目标版本 `NourishmentEffect.applyEffectTick` **没有直接 heal**，其原生特殊经济是玩家不满足高饱食受伤恢复条件时每 tick 返还最多 4 点耗竭。兼容仅对服务端玩家中和此方法的返还，保持效果存活；不包装 `LivingEntity.heal`、不拦截全部效果、不改用户 FD/TWT2 配置。FD 满 Hunger 可食用判断与 Buildup 已有规则兼容，保留。

原生 Nourishment 料理的 600 / 1200 / 3600 / 6000 tick 和授予概率原样保留，本轮**不乘 Variety 时长**。同类走原版刷新，不能累加时长。原生 6000 tick 是保留外来语义的例外，不受 Buildup 默认授予 3600 tick 上限截短。没有原生 Nourishment 消费效果但数据包显式引用该 ID 的食品，使用 Buildup Quality 时长和原有 Variety 上限一次。

Overfull 阻止首次授予和刷新，包含直接命令或其他模组调用；已经存在的效果继续计时。超过负荷阈值的当次食物也不能追加储备或刷新效果。营养、补水和饮食记录保留。死亡、牛奶、到期、命令清除后不残留一份 Buildup 镜像状态。管理员查询显示真实剩余 tick。

FD 默认给原版三种炖菜追加 Nourishment；兼容后以最终 Food Profile 为准，保留蘑菇煲/甜菜汤的调养及兔肉煲的振奋。Profile 改为无主增益也会抑制该食品后续 Nourishment 授予，但不清除玩家之前已经拥有的增益。其他效果，如兔肉煲 Jump Boost、沙拉 Regeneration、果汁独立治疗、苹果酒 Absorption，保持原生语义。

## 数据包与消费路径

运行时 Item Registry 扫描确认 **80 个 Food / Consumable 物品**，全部有显式 Profile。牛奶瓶、热可可、西瓜汁只有 Consumable、没有 Food；它们经受限的 FD 饮料路径执行一次 Profile/Diet，保留原本容器及独立效果，不把普通药水或其他非食品纳入 Food Recovery。

数据放在随模组发布的内置包：

```text
src/main/resources/resourcepacks/farmers_delight/
  pack.mcmeta
  data/buildup_vitals/buildup_vitals/food_profiles/farmersdelight/<item>.json
```

FD 存在时自动启用，数据格式仍为 Schema v1。Profile ID 是 `buildup_vitals:farmersdelight/<item>`。整合包可在高优先级数据包覆盖相同逻辑路径，或另加显式 Item selector。完整替换、不合并字段；删除 `meal_benefit` 表示无新主增益，不能填写字符串 `none`。成功 `/reload` 同步到客户端，失败保留上次成功快照。

原生 Nourishment 授予发生在 FoodProperties 回调之后。Buildup 检查 Consumable 中的真实来源，延后到原生消费效果授予一次，保留其时长和概率；同时检查最终 Profile，避免 FD 回调覆盖数据包选择。没有硬编码“某道菜永远授予 Nourishment”的 Java 物品表。

放置式整派、宴席方块、Rice Roll Medley、Horse Feed 等没有 Food/Consumable 组件，不为它们虚构可食用 Profile；实际切片/盛出的份额已有 Profile。FD PieBlock 直接食用使用对应切片的 FoodProperties 回调。其他可选配方输入不在运行时反推：饺子可用蘑菇、卷心菜卷可用蔬菜，因此不强制加 protein；Bone Broth 的骨头固定、辅料可变，只给稳定的 protein 类别。

所有恢复为 0 或整数 1–6 HP。金色/独立治疗不是抬高储备的理由：西瓜汁保留原生 2 HP 独立治疗，Profile 恢复为 0；苹果酒保留 Absorption，恢复为 0。普通熟肉切片 1 HP，Prepared 1–2，Meal 3–4，Feast 4–5；当前没有为填满范围而强设 6 HP 的菜。

Diet 依据目标 JAR 配方与主要食材设置。生熟、切片及原版同类肉沿用现有组；鱼卷共组、整海带卷与切片共组、两种肉意面共组。真正不同料理保留各自组。Variety 恢复奖励仍只有一次，Hydration 不乘 Variety。

## TWT2、AppleSkin 与恢复边界

补水依次为 `TWT2 blacklist > Buildup explicit Profile（含 0）> TWT2 默认/配置 > fallback`。全部 80 个物品均测试了 API 解析、故意设为 19/19 的 TWT2 配置覆盖、实际消费一次补水，以及 blacklist。过饱仍能补水。果汁可以有更高 Quenched，但不在两个维度同时超过 Pure Water 10/8。

无 TWT2 时补水数据不产生新状态条。有 TWT2 无 AppleSkin 时复用上游水滴；有 AppleSkin 时由上游显示。FD 原生 Nourishment tooltip 行在有效 Profile 已同步后由 Buildup 单行图标 + 原名替代，原版炖菜也不会显示失效的 Nourishment。其余 FD 独立效果说明保留。调养与 FD 滋养名称区分，安适仍仅占位。

Recovery Controller 数值不变：Natural 10/80、Food 12、调养/Nourishment Food 10 tick；连续 `min(Saturation,6)/6`。高饱和、5 HP 宴席、Variety 与有/无 Quenched 的组合从 10 HP 回满均为 100 tick，单 tick 不会叠出第二次治疗。无自然恢复的 3 HP FD 牛肉炖菜为 30 tick，受击不暂停。保留主动收益，不重新削弱被动基线。

## 自动验证与复现

在目标目录、Java 25，已经同意 Minecraft 测试 EULA 后运行。AppleSkin fixture 为官方 `mc26.3-3.0.10` 发布 JAR，仅放在忽略的 `run/compat/` 中。

```powershell
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true -PwithFarmersDelight=true
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true -PwithFarmersDelight=true -PwithThirst=true
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true -PwithFarmersDelight=true '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar'
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true -PwithFarmersDelight=true -PwithThirst=true '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar'
```

首轮五组均已通过。当时JVM为108项，服务端GameTest为47项（40项Core与7项FD条件测试；无FD时跳过FD专属断言并验证缺省门控），客户端为5个入口，FD场景有集成服与DedicatedServer TCP连接、重载、重连和真实成就判断。中英文FD+TWT2+AppleSkin tooltip截图已检查，无重复Nourishment行或缺失图标。

后续启动热修复新增第109项JVM回归测试。另以 Iris `1.11.6+mc26.3`、Sodium `0.9.3-alpha.1+mc26.3`、AsyncLogger `2.2.2+26.1.2-fabric` 复现旧版的Identifier提前加载崩溃；修复后该组合加入FD+TWT2+AppleSkin，以及不装FD/TWT2/AppleSkin的两条 `build runClientGameTest` 路径均通过。此结果不等于用户完整123模组整合包或所有光影包都已验证。

| 组合 | Wrapper build / JVM | 服务端 GameTest | 客户端与 DedicatedServer TCP |
|---|---|---|---|
| 无 FD | PASS | PASS | PASS，FD缺省门控 |
| FD | PASS | PASS | PASS |
| FD + TWT2 | PASS | PASS | PASS |
| FD + AppleSkin | PASS | PASS | PASS |
| FD + TWT2 + AppleSkin | PASS | PASS | PASS |

服务端GameTest是独立启动的无客户端服务器进程；客户端连接测试涵盖集成服和同JVM DedicatedServer + TCP，不冒充两台真人客户端或长时间多人压力测试。其他FD版本、其他HUD组合及下列人工游玩清单仍需按实际整合包验收。工作日志和截图保留在工作区根目录 `docs/26.3/`，不进入版本提交。

## 人工验收清单

建议使用独立生存测试世界、开启命令，在正常 20 TPS 下验收；用 `/buildupvitals food profile <item>`、`recovery`、`diet` 查看服务器实际状态。

- [ ] 分别启动无 FD、FD、FD+TWT2、FD+AppleSkin、FD+TWT2+AppleSkin；无 FD 无缺类/启动失败，有 FD 能加载内置兼容数据。
- [ ] 背包查看番茄、牛排切片、曲奇、鸡汤、宴席、牛奶瓶/可可/西瓜汁；核对下表的恢复、速度、类别和补水，零恢复不显示虚假心形值。
- [ ] 生存与创造对比番茄 quick（21 tick）、小切片/曲奇 fast（16 tick）、鸡汤 normal（32 tick）；创造不获得储备或饮食增益。
- [ ] 吃鸡汤：只显示 FD Nourishment / 滋养，约 5 分钟；不出现调养+振奋两张额外卡片，悬停说明为食物恢复 10 tick/HP、活动耗竭 -10%。
- [ ] 无自然恢复条件下吃牛肉炖菜，3 HP 在 30 tick 兑现；中途受击继续恢复，满血不浪费储备。高饱和半血约 5 秒回满，不出现独立 FD 恢复脉冲。
- [ ] Nourishment 后吃蘑菇煲/南瓜派，改为调养/振奋；反向再吃 FD Nourishment 菜，恢复唯一 FD 图标。用命令分别授予三个原生效果和 Nourishment，检查后授予异类替换。
- [ ] 满 Hunger 连续进食到负荷 48 有一次轻提示、64 触发积食；当次与后续食品不加储备、不刷新 Nourishment，现有效果继续计时，营养/补水/饮食记录仍变化。积食下命令授予也不能刷新 Nourishment。
- [ ] 重复切片/生熟同类肉不会刷多个组；轮换不同完整料理能获得有限 Variety 奖励，但 Nourishment 原生 6000 tick 不变。
- [ ] 数据包覆盖鸡汤主增益为调养、振奋、删除字段，并改变恢复/补水/速度；`/reload` 后服务端实际结果和 tooltip 同步，没有 FD 回调覆盖或两条 Nourishment 提示。移除覆盖恢复默认。
- [ ] TWT2 下喝牛奶瓶 8/8、可可 7/9、西瓜汁 9/9、清汤 8/6；检查实际 API/状态，考虑满条裁剪。把 TWT2 配置故意设成 19/19，显式 Profile 仍胜出；加入 blacklist 后不补水。
- [ ] 有/无 AppleSkin 均只有一套水滴和主增益说明；中英文、普通与 F3+H tooltip 可读。AppleSkin 原版回血预测不当成 Buildup 储备预测。
- [ ] FD 料理保留容器、原生营养、原有独立效果和 Nourishment 成就；放置整派后直接吃/切片进食、宴席盛出后食用能正常工作。
- [ ] 用牛奶、等待到期、命令清除、死亡移除 Nourishment，语义随之消失；保存重进、专服断线重连和存活换维度保留原生效果时间。
- [ ] 药水、信标、金苹果独立治疗照常；未引入 Comfort、额外常驻 HUD、Addon 适配或其他 Vitals 玩法。

## 全量 Profile 表

下表由实际 80 个运行时物品与发布数据对照；ID 省略 `farmersdelight:`。H/S 为 FD 原生 Food 组件的 Hunger / Saturation 点数；`—` 表示仅 Consumable。水分为 Thirst/Quenched。N 为原生 Nourishment tick，0 表示没有主增益；所有原生 Nourishment 食品均保留该主增益。组仅省略 `farmersdelight:`，跨原版组保留 `minecraft:`。

| 物品 | 原生 H/S | Quality | HP | 水分 | 速度 | Diet | Variety Group | N tick |
|---|---|---|---:|---|---|---|---|---:|
| cabbage | 2/1.6 | basic | 1 | 3/2 | quick | vegetable | cabbage | 0 |
| tomato | 1/0.6 | basic | 1 | 4/2 | quick | vegetable | tomato | 0 |
| onion | 2/1.6 | basic | 1 | 2/1 | quick | vegetable | onion | 0 |
| fried_egg | 4/3.2 | basic | 1 | 1/0 | normal | protein | fried_egg | 0 |
| milk_bottle | — | basic | 0 | 8/8 | normal | dairy | milk | 0 |
| hot_cocoa | — | prepared | 1 | 7/9 | normal | dairy, sweet | hot_cocoa | 0 |
| apple_cider | 0/0 | prepared | 0 | 8/10 | normal | fruit, sweet | apple_cider | 0 |
| melon_juice | — | prepared | 0 | 9/9 | normal | fruit | melon_juice | 0 |
| tomato_sauce | 4/3.2 | prepared | 1 | 3/2 | normal | vegetable | tomato | 0 |
| wheat_dough | 2/1.2 | basic | 0 | 1/0 | normal | grain | wheat_dough | 0 |
| raw_pasta | 2/1.2 | basic | 0 | 1/0 | normal | grain | wheat_dough | 0 |
| pumpkin_slice | 3/1.8 | basic | 1 | 2/1 | quick | vegetable | minecraft:pumpkin | 0 |
| cabbage_leaf | 1/0.8 | basic | 1 | 2/1 | fast | vegetable | cabbage | 0 |
| minced_beef | 2/1.2 | basic | 0 | 1/0 | fast | protein | minecraft:cooked_beef | 0 |
| beef_patty | 4/6.4 | basic | 1 | 1/0 | fast | protein | minecraft:cooked_beef | 0 |
| chicken_cuts | 1/0.6 | basic | 0 | 1/0 | fast | protein | minecraft:cooked_chicken | 0 |
| cooked_chicken_cuts | 3/3.6 | basic | 1 | 1/0 | fast | protein | minecraft:cooked_chicken | 0 |
| bacon | 2/1.2 | basic | 0 | 1/0 | fast | protein | minecraft:cooked_beef | 0 |
| cooked_bacon | 4/6.4 | basic | 1 | 1/0 | fast | protein | minecraft:cooked_beef | 0 |
| cod_slice | 1/0.2 | basic | 0 | 1/0 | fast | protein | minecraft:fish | 0 |
| cooked_cod_slice | 3/3 | basic | 1 | 1/0 | fast | protein | minecraft:fish | 0 |
| salmon_slice | 1/0.2 | basic | 0 | 1/0 | fast | protein | minecraft:fish | 0 |
| cooked_salmon_slice | 3/4.8 | basic | 1 | 1/0 | fast | protein | minecraft:fish | 0 |
| mutton_chops | 1/0.6 | basic | 0 | 1/0 | fast | protein | minecraft:cooked_beef | 0 |
| cooked_mutton_chops | 3/4.8 | basic | 1 | 1/0 | fast | protein | minecraft:cooked_beef | 0 |
| ham | 5/3 | basic | 0 | 2/0 | normal | protein | minecraft:cooked_beef | 0 |
| smoked_ham | 10/16 | basic | 1 | 1/0 | normal | protein | minecraft:cooked_beef | 0 |
| pie_crust | 2/0.8 | basic | 0 | 0/0 | normal | grain, dairy | pie_crust | 0 |
| cake_slice | 2/0.4 | prepared | 1 | 1/0 | quick | grain, dairy, sweet | minecraft:cake | 0 |
| apple_pie_slice | 3/1.8 | prepared | 1 | 1/0 | quick | grain, fruit, dairy, sweet | apple_pie | 0 |
| sweet_berry_cheesecake_slice | 3/1.8 | prepared | 1 | 1/0 | quick | grain, fruit, dairy | sweet_berry_cheesecake | 0 |
| chocolate_pie_slice | 3/1.8 | prepared | 1 | 1/0 | quick | grain, dairy, sweet | chocolate_pie | 0 |
| pumpkin_pie_slice | 3/1.8 | prepared | 1 | 1/0 | quick | vegetable, protein, sweet | minecraft:pumpkin_pie | 0 |
| sweet_berry_cookie | 2/0.4 | prepared | 0 | 0/0 | fast | grain, fruit, sweet | minecraft:cookie | 0 |
| honey_cookie | 2/0.4 | prepared | 0 | 0/0 | fast | grain, sweet | minecraft:cookie | 0 |
| melon_popsicle | 3/1.2 | prepared | 1 | 4/3 | fast | fruit | melon_popsicle | 0 |
| glow_berry_custard | 7/8.4 | prepared | 2 | 3/2 | normal | fruit, dairy, protein, sweet | glow_berry_custard | 0 |
| fruit_salad | 6/7.2 | prepared | 2 | 5/4 | normal | fruit, vegetable | fruit_salad | 0 |
| mixed_salad | 6/7.2 | prepared | 2 | 4/3 | normal | vegetable | mixed_salad | 0 |
| nether_salad | 5/4 | prepared | 0 | 2/1 | normal | vegetable | nether_salad | 0 |
| barbecue_stick | 8/14.4 | prepared | 2 | 2/1 | normal | protein, vegetable | barbecue_stick | 0 |
| egg_sandwich | 8/12.8 | prepared | 2 | 1/0 | normal | protein, grain | egg_sandwich | 0 |
| chicken_sandwich | 10/16 | meal | 3 | 2/1 | normal | protein, grain, vegetable | chicken_sandwich | 0 |
| hamburger | 11/17.6 | meal | 3 | 2/1 | normal | protein, grain, vegetable | hamburger | 0 |
| bacon_sandwich | 10/16 | meal | 3 | 2/1 | normal | protein, grain, vegetable | bacon_sandwich | 0 |
| mutton_wrap | 10/16 | meal | 3 | 2/1 | normal | protein, grain, vegetable | mutton_wrap | 0 |
| dumplings | 8/12.8 | prepared | 2 | 2/1 | normal | grain, vegetable | dumplings | 0 |
| stuffed_potato | 10/14 | prepared | 2 | 2/1 | normal | protein, vegetable, dairy | stuffed_potato | 0 |
| cabbage_rolls | 5/5 | prepared | 2 | 3/2 | normal | vegetable | cabbage_rolls | 0 |
| salmon_roll | 7/8.4 | prepared | 2 | 2/1 | normal | protein, grain | fish_roll | 0 |
| cod_roll | 7/8.4 | prepared | 2 | 2/1 | normal | protein, grain | fish_roll | 0 |
| kelp_roll | 12/12 | prepared | 2 | 2/1 | normal | grain, vegetable | kelp_roll | 0 |
| kelp_roll_slice | 6/6 | prepared | 1 | 1/0 | fast | grain, vegetable | kelp_roll | 0 |
| cooked_rice | 6/4.8 | basic | 1 | 2/1 | normal | grain | cooked_rice | 600 |
| bone_broth | 8/11.2 | prepared | 2 | 8/6 | normal | protein | bone_broth | 1200 |
| beef_stew | 12/19.2 | meal | 3 | 6/4 | normal | protein, vegetable | beef_stew | 3600 |
| chicken_soup | 14/21 | meal | 3 | 6/4 | normal | protein, vegetable | chicken_soup | 6000 |
| vegetable_soup | 12/19.2 | meal | 3 | 6/4 | normal | vegetable | vegetable_soup | 3600 |
| fish_stew | 12/19.2 | meal | 3 | 6/4 | normal | protein, vegetable | fish_stew | 3600 |
| fried_rice | 14/21 | meal | 3 | 2/1 | normal | protein, grain, vegetable | fried_rice | 6000 |
| pumpkin_soup | 14/21 | meal | 4 | 6/5 | normal | protein, vegetable, dairy | pumpkin_soup | 6000 |
| baked_cod_stew | 14/21 | meal | 4 | 6/5 | normal | protein, vegetable | baked_cod_stew | 6000 |
| noodle_soup | 14/21 | meal | 4 | 6/5 | normal | protein, grain, vegetable | noodle_soup | 6000 |
| onion_soup | 14/21 | meal | 3 | 6/4 | normal | grain, vegetable, dairy | onion_soup | 6000 |
| bacon_and_eggs | 10/12 | meal | 3 | 1/0 | normal | protein | bacon_and_eggs | 1200 |
| pasta_with_meatballs | 12/19.2 | meal | 4 | 2/1 | normal | protein, grain, vegetable | meat_pasta | 3600 |
| pasta_with_mutton_chop | 12/19.2 | meal | 4 | 2/1 | normal | protein, grain, vegetable | meat_pasta | 3600 |
| mushroom_rice | 12/19.2 | meal | 3 | 3/2 | normal | grain, vegetable | mushroom_rice | 3600 |
| roasted_mutton_chops | 14/21 | meal | 4 | 3/2 | normal | protein, grain, vegetable | roasted_mutton_chops | 6000 |
| vegetable_noodles | 14/21 | meal | 3 | 3/2 | normal | grain, vegetable | vegetable_noodles | 6000 |
| steak_and_potatoes | 12/19.2 | meal | 4 | 2/1 | normal | protein, vegetable | steak_and_potatoes | 3600 |
| ratatouille | 10/12 | meal | 3 | 4/3 | normal | vegetable | ratatouille | 1200 |
| squid_ink_pasta | 14/21 | meal | 4 | 2/1 | normal | protein, grain, vegetable | squid_ink_pasta | 6000 |
| grilled_salmon | 14/21 | meal | 4 | 3/2 | normal | protein, vegetable, fruit | grilled_salmon | 6000 |
| roast_chicken | 14/21 | feast | 5 | 3/2 | normal | protein, grain, vegetable | roast_chicken | 6000 |
| stuffed_pumpkin | 14/21 | feast | 4 | 4/3 | normal | grain, vegetable, fruit | stuffed_pumpkin | 6000 |
| honey_glazed_ham | 14/21 | feast | 5 | 2/1 | normal | protein, grain, fruit, sweet | honey_glazed_ham | 6000 |
| shepherds_pie | 14/21 | feast | 5 | 3/2 | normal | protein, vegetable, dairy | shepherds_pie | 6000 |
| gleaming_salad | 14/21 | feast | 4 | 5/4 | normal | vegetable, fruit, sweet | gleaming_salad | 6000 |
| dog_food | 4/1.6 | basic | 0 | 2/0 | normal | protein, grain | dog_food | 0 |
