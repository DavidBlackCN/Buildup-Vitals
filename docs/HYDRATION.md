# Hydration v2 与 TWT2

可选适配 **Thirst Was Taken 2 Fabric 1.6.2+26.3**，Mod ID `thirstwastaken2`。未安装时 Buildup 独立运行；其他版本关闭适配并记录提示，不尝试未知方法注入。两端需使用相同 Buildup/TWT2 版本。

## 数据优先级

1. TWT2 `itemBlacklist`：不补水。
2. Buildup 最终匹配的显式 Item/Tag Profile，包含显式零以及已匹配 Profile 省略 Hydration 的默认零。
3. TWT2 配置 `drinks`，然后 `foods`。
4. TWT2 原生 `data/<namespace>/thirstwastaken2/drinks/*.json`。
5. TWT2 通用 Tag 和关键词 fallback。

没有匹配 Buildup Profile 才交还 TWT2，Buildup 的代码 fallback 零值不覆盖原生规则。Stage 7 的“必须移除上游配置才生效”已废止；整合包应通过 Buildup Profile 管理已适配食物，blacklist 仍可禁用。

`ThirstApi.resolve(Item)` 在黑名单返回之后、首次 `Map.get`（配置查询）之前注入 Profile。显式 `0/0` 使用上游 NONE 标记，避免落入通用匹配。最终只有 TWT2 完成一次消费结算；Drink Event 的改值/取消、疾病和纯度处理仍生效，不额外调用 drink 发放第二份补水。

Profile 的非负整数分别裁剪到 0～20，防止溢出；Quenched 不乘 Variety。上游将超出满 Thirst 的增量转入 Quenched，并以玩家结算后的 Thirst 限制储备，例如 19/0 吃 6/4 后为 20/9。

## 纯水与食物

Pure 直接饮用水的栈级查询为每份 **10 Thirst / 8 Quenched**，空状态两份为 20/16。覆盖水瓶、装水陶碗，以及非空水袋、铜水壶、铁水壶；后面三种容器每次仍只消耗一份水。使用 TWT2 的 `isPlainWaterDrink` 判定，空容器、不可直接饮用的水桶、普通药水与果汁不套用该基准；脏水、浑水、Clean 或盐水也不会变成 Pure。没有对应 Buildup Profile 时使用该纯水基准；blacklist 最高，显式 Profile 仍可覆盖该基准。TWT2 自己的纯度比例、疾病、盐水不补水及额外消耗逻辑保留。

默认 Dirty/Murky/Clean 水仍使用上游水瓶基数 6/8，再按纯度比例 0%/50%/100% 调整 Quenched，疾病还可能降低结果。Pure 默认 100%，用户改过 TWT2 纯度比例时继续尊重该比例。满 Thirst 的原生饮水限制保留，满 Hunger 食物权限不改变它。

40 种原版食物完整值见 [VANILLA_BALANCE](VANILLA_BALANCE.md)：熟肉/熟鱼 1/0、生肉 2/0、生鱼 2/1、苹果 4/2、西瓜 5/3、蘑菇煲/甜菜汤 6/4、兔肉煲 8/6，曲奇/干海带 0/0。金苹果、蜂蜜等保留独立效果并给予适度水分。

FD Refabricated 26.3-3.6.27 的全部80个 Food/Consumable 也经同一显式覆盖入口：TWT2 blacklist > Buildup explicit Profile（含0）> TWT2 默认/配置 > fallback。包括没有 Food 组件的牛奶瓶、热可可和西瓜汁，不增加第二套结算。默认牛奶8/8、可可7/9、西瓜汁9/9、苹果酒8/10、Bone Broth8/6；普通汤5～6档、干曲奇0/0，完整值见 [FD 兼容](FARMERS_DELIGHT_COMPAT.md)。Overfull 不阻止补水，Variety 不放大补水，删除或覆盖 Profile 继续服从正常 reload 语义。

[More Delight](MORE_DELIGHT_COMPAT.md) 的31份显式 Profile 也使用同一入口，胡萝卜汤6/4、普通饭菜2/1～3/2、干吐司0/0；无额外饮料结算器。黑名单、配置冲突、实际一次消费及积食下补水均纳入附属兼容验证。

## Kaleidoscope 独立饮用与方块食品

四个独立Profile包覆盖Cookery120、Tavern50、Nether85、End44项（含两个中性容器），统一沿上述优先级。茶8/6、汤通常6/4、葡萄酒3/3、烈酒2/1、鸡尾酒5/4、果汁桶8/8、醋及空杯0/0；完整表见 [Kaleidoscope Series](KALEIDOSCOPE_COMPAT.md)。

Tavern DrinkBlockItem / CocktailBlockItem / JuiceBucketItem与Cookery茶杯的实际ItemStack消费已经进入TWT2，因此兼容只补接Recovery/Diet，不再次补水。摆放饮品后取回不构成饮用，空杯与餐袋不发放额外收益。

Cookery及附属FoodBiteBlock没有ItemStack消费回调，由版本门控适配在真实吃下一口时查询同一个ThirstApi，并使用 `floor(total*(i+1)/N)-floor(total*i/N)` 分配Thirst/Quenched。完整吃完等于整盘Profile量，部分食用只得已吃份额；blacklist优先、0/0不回退、TWT2事件及状态上限继续生效。Overfull不阻止补水，Variety不放大水分。

## Quenched 恢复协调

已启用口渴、Thirst=20 且 Quenched>0 时，Buildup 自然恢复速度 ×1.15，与 Well-fed Variety 相乘，最终周期至少 10 tick。它不自行产生治疗，也不绕过 Hunger 或自然恢复游戏规则。储备存在时仍按 [统一恢复时钟](RECOVERY.md) 调度。

当前Well-fed基准已为10 tick，所以该倍率在高饱和档位不再带来额外频率；Stable仍可由80缩至约69.565 tick。Hydration的有限收益允许优于原版被动基线，但不重新启用TWT2独立治疗器，也不突破自然周期下限。

对已验证版本的 `HealthRegen.healWithQuenched(ServerPlayer,ThirstData,ExhaustionTracker)F` 使用一个 HEAD 返回 0 的窄注入，关闭其独立治疗及独立治疗费用。未改写 `quenchedHealthRegen` 配置或其他用户配置，没有全局 `LivingEntity.heal` 钩子。TWT2 其他活动耗水、气候、疾病及 FoodData 耗竭镜像继续存在，因此自然恢复的原版营养成本仍可能经 TWT2 镜像耗水。

TWT2 针对原版 FoodData 的脱水回血限制不额外接管 Buildup Controller：v2 自然恢复基线由 Hunger/Saturation 决定，Quenched 只奖励良好水分状态。第三方独立治疗仍需各自兼容，不能由本次有限协调代表所有模组组合。

## 同步与显示

成功启动/重载发布不可变 Hydration 索引并清理上游物品缓存；失败保留上次成功结果，删除 Profile 后恢复原生解析。`food_profiles_v3` 携带 Hydration、消费速度和服务端适配标记。远程客户端使用服务端快照，同 JVM 有服务器时只保留权威服务器索引；断线或服务器停止清理对应缓存。

安装 AppleSkin 时沿用 TWT2 自带水滴提示及其开关；未安装 AppleSkin 时，Buildup 调用 TWT2 已有水滴组件绘制 Thirst/Quenched 两行，不复制上游图标。水质名称沿用 TWT2，水滴数量经过实际水质修正，盐水不显示补水行。不新增口渴 HUD。

`/buildupvitals recovery [player]` 查询适配开关、Thirst/Quenched 和加速条件；`/thirst query <player>` 可查看上游状态。

## 版本证据与依赖

方法核对使用本地目标版本源码和发布 JAR，来源为 [官方 1.6.2 发布](https://modrinth.com/mod/thirst-was-taken-2/version/ZlAbvVVI) 与 [源码提交 2b3598f](https://github.com/n1ght3r/ThirstWasTaken2/tree/2b3598fbf6c543fda85a8a03acf4f698309ad104)。Mixin 插件精确检查版本；修改版本前必须重新验证描述符、查询顺序和运行矩阵。

固定依赖 `maven.modrinth:thirst-was-taken-2:1.6.2+26.3` 默认 compileOnly；`-PwithThirst=true` 才加入开发及 GameTest 运行时，不进入发布 POM。元数据为 suggests，无硬依赖，无嵌入 JAR。

上游使用 GPL-3.0-only；本项目没有复制上游实现/图标或捆绑上游 Mod。本次只通过其接口和窄 Mixin 适配，Buildup 原有许可证不变；若将来分发上游本体、修改版或合并产物，仍需按上游许可证处理相应分发义务。

## 运行矩阵

Java 25、目标目录，已同意测试 EULA 后运行：

```powershell
.\gradlew.bat clean build runClientGameTest -PacceptMinecraftEula=true
.\gradlew.bat build runClientGameTest -PwithThirst=true -PacceptMinecraftEula=true
.\gradlew.bat build runClientGameTest -PwithThirst=true -PacceptMinecraftEula=true '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar'
```

当前验证范围见 [Alpha Core Freeze](ALPHA_CORE.md)。连接测试的专服为同JVM DedicatedServer + TCP，并非独立操作系统进程或多人长期联机压测。已有组合证据在相关代码未变时复用，不要求每次文档维护重跑完整矩阵。
