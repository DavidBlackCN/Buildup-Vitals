# Hydration 与 Thirst Was Taken 2（Stage 6）

Buildup 可选适配 **Thirst Was Taken 2 Fabric 1.6.2+26.3**（真实 Mod ID：`thirstwastaken2`）。未安装时其余机制照常运行；安装其他版本时记录提示并关闭本适配，不尝试未知注入。TWT2 本身要求服务器和客户端都安装。

## 谁负责什么

Food Profile 提供 `hydration.thirst` 和 `hydration.quenched`；TWT2 负责完成饮用时的结算、玩家状态、同步、溢出、口渴 HUD、饮水行为及水纯度。本适配只参与食物值查询，不自己增加玩家口渴值，也不增加一套饮用完成监听。

因此一次消费仍只有 TWT2 的一次结算；其 Drink Event 的改值/取消、纯度处理与其他集成入口继续生效。Buildup 不修改水组件、净化、盐水、疾病或原有饮水动画，不让非食物自动变成可消费物品。

## 数据优先级

同一物品按以下顺序选择，命中后不累加低优先级值：

1. TWT2 `itemBlacklist`：不补水。
2. TWT2 配置中的 `drinks`，然后 `foods`。
3. TWT2 标准 `data/<namespace>/thirstwastaken2/drinks/*.json`。
4. Buildup 最终匹配 Food Profile 的 Hydration。
5. TWT2 自身的通用 Tag 与关键词规则。

**TWT2 默认配置已经包含苹果、蘑菇煲等许多原版食物。** 它们同样属于第 2 项，优先于 Buildup；安装适配不代表现有默认配置被悄悄覆盖。要让某个物品由 Buildup Profile 管理，服务器管理员需从 TWT2 的 `drinks` / `foods` 配置中移除该物品条目，并按 TWT2 配置加载方式重启服务器。不要把配置值设为零来表示“移除”。客户端 TWT2 的本地配置也会影响其提示，应保持相应配置一致。

显式零值具有意义：TWT2 drinks 中的 `{ "thirst": 0, "quenched": 0 }` 会阻止 Buildup 和后续通用匹配；一个已匹配 Buildup Profile 的 Hydration 为零（含省略字段的默认零）会阻止更低优先级通用匹配。没有匹配 Buildup Profile 时完全交还 TWT2，不把 Buildup 的 fallback 零值当作显式覆盖。

## 参数与 Quenched

Profile 格式与 Item/Tag 匹配规则见 [FOOD_PROFILES.md](FOOD_PROFILES.md)。字段仍接受非负 32 位整数，保持原数据格式；适配传给 TWT2 前分别裁剪到该验证版本的 0–20，避免极大值相加溢出。

Quenched 是 TWT2 的独立隐藏储备，不是 Hunger / Saturation，也不使用 Variety 倍率。本模组不预先把食物的 Quenched 限制到该食物的 Thirst：最终上限由玩家结算后的 Thirst 决定。

例如玩家 `Thirst=5 / Quenched=0`，食用 `6 / 8` 的食物后为 `11 / 8`。该版本还会将超过 20 的 Thirst 增量转入 Quenched：从 `19 / 0` 摄入 `4 / 2` 后为 `20 / 5`。这些是沿用 TWT2 的语义，不是 Buildup 新增的转换规则。

现有 Buildup 示例：苹果 `2 / 0`、蘑菇煲 `4 / 2`，面包与牛排为零；它们是否最终生效取决于上述优先级。原生水瓶、牛奶和蜂蜜瓶继续使用 TWT2 原有规则。本阶段未修改这些平衡值，也未提前建立全面食物平衡包。

## 重载、同步与生命周期

- 服务器启动及 Buildup 整轮成功重载后，从新的 Profile 快照生成不可变 Hydration 索引，并清理 TWT2 的物品值缓存。
- Buildup Profile 重载失败不发布新索引；成功删除 Profile 后移除对应值，重新交由 TWT2 剩余规则解析。TWT2 自己的 drinks 文件重载仍由其实现负责。
- 食物元数据协议升级为 `buildup_vitals:food_tooltips_v2`，携带 Hydration 与服务端是否启用适配；登录和成功重载更新客户端。客户端不读取本地服务器数据包，不提交玩家数值。
- TWT2 的 API 缓存在 integrated server 与客户端间共享，因此本适配在同一进程存在服务器时只使用服务器索引，收到的客户端显示包不会覆盖它。纯远程客户端才安装收到的显示索引。
- 客户端断线清理远程索引，服务器停止清理本地索引；不同存档不复用旧 Profile。
- v1/v2 不混用，建议两端同时更新 Buildup。显示包超过既有 65,536 条/16 MiB 限制时，远程显示会降级，服务器食物机制仍正常。

## Tooltip 与恢复行为边界

本阶段复用 TWT2 自己的水滴图标，不追加重复的 Buildup 补水长句。**TWT2 1.6.2 的食物水滴 Tooltip 需要 AppleSkin**，并受 TWT2 的显示选项控制；未安装 AppleSkin 时口渴机制仍正常，但没有这些食物水滴行。可以用 `/thirst query <player>` 查看真实玩家数值，或使用 Buildup 的 Profile 查询查看原始配置。

用户在 Stage 5 验收提出的简洁 icon + 短文字方向已记入版本目录的 PLAN 与 DESIGN_PRINCIPLES：恢复量以后改用爱心图标，Buff 提示等专用图标齐备后再优化。本阶段不重做原有 Recovery / Meal Benefit 提示。

**恢复协调尚有后续工作：** TWT2 1.6.2 默认 `quenchedHealthRegen=0.5`，可在满 Thirst 时提供独立治疗；其针对原版 FoodData 的脱水限制不会自动约束 Buildup 自定义 Recovery Controller。当前适配不接管这些恢复逻辑、不改写外部配置；因此不能把“每 4 秒恢复 1 HP”理解为安装 TWT2 后的所有来源合计速度。这些跨模组恢复行为需要在主要机制完成后的整体平衡阶段统一评估。

## 开发依赖、注入与许可证

开发依赖固定为 `maven.modrinth:thirst-was-taken-2:1.6.2+26.3`，仓库限定到 Modrinth Maven 的对应 group。默认仅 `compileOnly`；`-PwithThirst=true` 才加入 Loom `localRuntime` 和独立的 `gametestRuntimeOnly`，避免测试开关把可选依赖写入生产 Maven POM。`fabric.mod.json` 使用 `suggests`，没有硬 `depends`，不嵌入或重打包 TWT2。

官方 Java API 提供查询、玩家操作、Drink Event 和缓存清理，但没有动态物品值注册接口。纯静态 drinks 兼容包无法自动跟随任意 Buildup Profile/Tag 重载；在进食后额外调用 `drink` 又容易重复结算或绕过原有事件。因此使用一处经过版本门控的最小值解析注入：

```text
ThirstApi.resolve(Lnet/minecraft/world/item/Item;)[I
  DataPackDrinks.get(Lnet/minecraft/world/item/Item;)[I
```

在唯一调用的返回值为 `null` 时提供 Profile 值，非 null（包括零值）原样保留。`ModifyExpressionValue` 要求命中且只命中一次；没有改写整个解析方法。其他版本不应用这个 Mixin，也不调用 TWT2 API。新增版本支持前必须重新核对源码/字节码并运行矩阵。

调查基于 [官方发布版](https://modrinth.com/mod/thirst-was-taken-2/version/ZlAbvVVI) 与对应 [1.6.2 源码提交 2b3598f](https://github.com/n1ght3r/ThirstWasTaken2/tree/2b3598fbf6c543fda85a8a03acf4f698309ad104)，包括 `ThirstApi`、`ThirstEvents`、`DataPackDrinks`、消费 Mixin、Quenched 数据结构及开发者文档；发布 JAR 的方法描述符另外用 `javap` 核对。

上游采用 [GPL-3.0-only](https://github.com/n1ght3r/ThirstWasTaken2/blob/2b3598fbf6c543fda85a8a03acf4f698309ad104/LICENSE)，未发现额外 API 链接例外；其旧 MIT 归属声明见同提交的 CREDITS 和 licenses。此次没有复制上游实现或图标资源，依赖 JAR/源码仅在忽略目录用于调查和测试，不随 Buildup 发布包提交或捆绑。`compileOnly` 不构成许可证豁免；若今后分发 TWT2 本体、修改版或合并产物，需另行核对 GPL 的声明、对应源码及组合分发要求。本轮未变更 Buildup 原有许可证，也未进行这样的分发。

## 验证方式

在 Java 25 环境下，目标目录运行：

```powershell
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true
.\gradlew.bat build runClientGameTest -PwithThirst=true -PacceptMinecraftEula=true
.\gradlew.bat build runClientGameTest -PwithThirst=true -PacceptMinecraftEula=true '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar'
```

EULA 参数沿用用户对自动测试环境的授权。AppleSkin 本地测试包的获取方式见 [README](../README.md)。测试仅操作框架创建的世界，具体结果见 [Stage 6 报告](STAGE_6_REPORT.md)。
