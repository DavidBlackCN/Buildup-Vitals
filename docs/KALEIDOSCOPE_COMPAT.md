# Kaleidoscope Series Compatibility

Minecraft 26.3 / Fabric，Buildup Vitals 0.1.0-alpha.1。按 Cookery → Tavern → Nether → End 顺序实现、分别验证后联合测试。所有模块均为可选依赖，发布包不嵌入第三方 JAR，不改其配置、配方、世界生成或酿造机器。

## 安装与版本

| 模块 | 精确支持版本 / 发布 | Profile | 说明与完整表 |
|---|---|---:|---|
| Cookery | [1.6.0.3-fabric+mc26.3](https://modrinth.com/mod/kaleidoscope-cookery-refabricated/version/Yf5VytNq) | 120 | [Cookery](KALEIDOSCOPE_COOKERY_COMPAT.md) |
| Tavern | [1.2.0.11-fabric+mc26.3](https://modrinth.com/mod/kaleidoscope-tavern-refabricated/version/FOhx6x47) | 50 | [Tavern](KALEIDOSCOPE_TAVERN_COMPAT.md) |
| Nether | [1.1.13-fabric+mc26.3](https://modrinth.com/mod/kaleidoscope-nether-refabricated/version/r0AAEyh5) | 85 | [Nether](KALEIDOSCOPE_NETHER_COMPAT.md) |
| End | [1.0.16-fabric+mc26.3](https://modrinth.com/mod/kaleidoscope-end-refabricated/version/V836vh70) | 44 | [End](KALEIDOSCOPE_END_COMPAT.md) |

299份包括297个食品/饮品条目和餐袋、空杯两个中性条目；清单以目标发布注册表为准，包含独立 Drink、多口方块和可重复食品。四个内置数据包独立加载，可被服务器数据包覆盖。

Nether / End 同时需要 Cookery，以及 **Forge Config API Port 的 Fabric 26.3.1**（[发布 JpKvrr9J](https://modrinth.com/mod/forge-config-api-port/version/JpKvrr9J)）。这两个目标包的元数据遗漏了实际配置库依赖；不要误装同名 Forge 构建。开发测试已显式加入正确 Fabric 包，生产安装需自行满足这些上游依赖。

深度 Mixin 只对精确验证版本启用。升级第三方后先重新审核，未知版本仅保留数据包并明确警告，**不代表新的效果预算仍然生效**。插件启动阶段只读取Loader元数据；不提前加载Minecraft类。已合并上游Mixin的钩子按完整来源类和handler名定位，避免随机方法哈希。

## 当前机制

- Vigor以原图标/ID进入Foreign Main，只提供Invigorated；Nourishment仍提供Restorative + Invigorated。与Buildup原生主增益互斥，不重复图标，Overfull阻止授予/刷新。
- Foreign Cuisine Effect中其他特殊效果不占主槽。保留Ghost / Warped / Tropical Strider / Dream / Mint的独特玩法；强恢复、防御和穿甲按各模块预算约束。
- Cookery/End的料理再生已包含在Profile总恢复量；Tavern治疗酒0～5 HP、治疗鸡尾酒总共3 HP、Bloody Mary每次真实击杀至多2 HP，均进入一个Recovery Controller、受储备20 HP与Overfull限制。
- 高饱和自然10 tick、Food12、Restorative10、Stable80和自然最快10 tick不变；Warmth只改变Stable环境周期64/72，不开第二套治疗器。
- Shield为20%、单次最多4 HP并支付Saturation；Star为20%减伤、一次净化、+0.5击退抗性；Crimson只削弱25%有效护甲；Void只削弱15%有效护甲及Resistance/Protection减伤；Mint无80%通用减伤。
- 七道Dragon Cuisine按Mythic Budget提供5～6 HP整份恢复与至多两项主要效果，没有无限Strength/Resistance/Regeneration。
- TWT2始终为blacklist > explicit Profile（含0）> default > fallback。Block Food按实际口数分摊整份预算；物品Drink沿TWT2真实消费路径一次结算。摆放、取回、空杯不产生收益。
- 不包装全局heal/hurt，不接管普通Potion、Beacon、Golden Apple，不恢复Comfort，不进入Oxygen/Mana。

## 自动测试与复现

在26.3目录使用Java25和Wrapper；EULA参数仅在已经同意协议后使用：

```powershell
$series = @('-PwithCookery=true', '-PwithTavern=true', '-PwithNether=true', '-PwithEnd=true')
.\gradlew.bat build runClientGameTest @series -PacceptMinecraftEula=true
# 按需添加下面任一参数，或全部添加：
# -PwithThirst=true
# '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar'
# -PwithFarmersDelight=true
# -PwithMoreDelight=true  # 自动加入FD与Delight Lib
# 无第三方回归：省略@series和上述参数
```

测试包括114项JVM、75项注册服务端GameTest、10个客户端入口。可选模块缺席时相应条件测试只检查缺省路径，不把未运行的模块行为当作通过。客户端入口覆盖集成服务端与同JVM DedicatedServer真实TCP连接；服务端GameTest另在无图形测试服务端运行。完整联合矩阵结果见下表。

2026-10-07，Windows / Java25；每组执行 `build runClientGameTest`，JVM 114/114（未变输入由Gradle复用）。

| 组合 | Wrapper构建 | 注册服务端测试 | 客户端/专服TCP | 耗时 |
|---|---|---|---|---|
| 四模块 | PASS | 75 / 75 | PASS | 1m 47s |
| 四模块 + TWT2 | PASS | 75 / 75 | PASS | 1m 51s |
| 四模块 + AppleSkin | PASS | 75 / 75 | PASS | 1m 40s |
| 四模块 + FD | PASS | 75 / 75 | PASS | 1m 58s |
| 四模块 + More Delight / FD / Delight Lib | PASS | 75 / 75 | PASS | 2m 10s |
| 四模块 + TWT2 + AppleSkin + FD + More Delight | PASS | 75 / 75 | PASS | 2m 30s |
| 无上述可选模组 | PASS | 75 / 75 | PASS | 1m 19s |

服务端测试验证注册表完整性、实际消费、Overfull、主增益互斥、逐口总量、TWT2优先级、死亡与有限叠加计算；客户端验证同步、tooltip预算、原图标语义与重连。多人测试为模拟玩家，同JVM专服连接不是独立操作系统进程的长期真人联机压测。原始日志在忽略的run目录；一次性报告归档工作区根目录docs/26.3，不进入发布仓库。

## 完整人工验收清单

建议使用复制的生存测试存档；创造模式不会授予或兑现Buildup恢复储备。用 `/buildupvitals food profile <item>`、`/buildupvitals recovery`、`/buildupvitals diet`、`/thirst query <player>` 交叉核对，详细物品ID和预算见各模块表。

### 启动、可选性与显示

- [ ] 无森罗、Cookery、各附属以及四模块联合均正常进世界/专服；安装表中精确版本及上游前置。
- [ ] 再分别加入TWT2、AppleSkin、FD、More Delight，并测试全组合；无Mixin启动错误。
- [ ] 中文/英文普通和高级tooltip简洁可读；原生制作品质、酒等级、容器说明保留，预算后的效果等级与时长一致。
- [ ] 多人专服的两个玩家分别进食、死亡、换维度、退出重连，储备/效果/饮食互不串扰。

### Cookery

- [ ] 吃Vigor料理只见Vigor；依次吃Nourishment、调养、振奋料理，主槽替换，特殊效果仍可共存。
- [ ] Overfull时Vigor/Nourishment不新增、不刷新，不增加恢复储备；营养/补水仍正常。
- [ ] Shield在Hunger≥18且有Saturation时减伤20%、每击最多4 HP；低Saturation只支付现有量，无Hunger透支、递归或无敌。
- [ ] Warmth在普通主世界/下界/热源旁Stable周期分别80/72/64，高饱和仍10；无独立跳血。
- [ ] 茶杯、奶茶、花茶真实饮用恢复/补水一次，摆放取回不结算，餐袋按内部食物结算。
- [ ] 1×2、3×3及普通多口料理完整吃完才获得整份恢复/补水；部分食用、换玩家续吃和容器行为正常。

### Tavern

- [ ] 葡萄、瓶装酒、预设/自调鸡尾酒、果汁桶实际饮用正常；颜色、Brew Level、容器和原生微醺/反胃保留。
- [ ] Wine/Sakura Wine等级0～6的恢复预算分别0/0/1/2/3/4/5 HP；治疗鸡尾酒合计3 HP，不随治疗成分叠加。
- [ ] 酒不再授予Regeneration/Instant Health；破瓶也不绕过预算，普通治疗药水仍有效。
- [ ] 单成分鸡尾酒不增加20%时长，多成分为最长+其余一半，再受等级/时长上限约束；Resistance最高I、Strength II至多60秒。
- [ ] Bloody Mary只在目标真实死亡后入储备，非致命攻击无收益；每次最多2 HP，Overfull和20 HP上限生效。
- [ ] High Heels、Grass Stealth、Long Reach、XP Drain、Tomb Raider、Zenith等玩法与原代价仍可用，不占主槽。

### Nether

- [ ] Star Blessing普通无甲10 HP伤害为8 HP，无周期治疗；首次净化后能再次受到负面效果，仍可被有限击退。
- [ ] Crimson不额外放大无甲伤害，只减少25%有效护甲；高等级不成长，不跳过韧性/附魔。
- [ ] Ghost攀墙、Warped安抚、Tropical Strider环境能力仍正常。
- [ ] 永恒牛排保留冷却/物品，每次1 HP；反复满饱食进食可触发Overfull并停止新增储备。
- [ ] 四道可摆放料理按实际口数消费，原生营养、容器及水分总量正常。

### End

- [ ] Mint能安抚末影人，但普通攻击没有80%减伤；Dream保留移动/落地能力，普通攻击仍造成伤害。
- [ ] Void Erosion没有全穿甲或致命伤锁血；护甲、Resistance、Protection仍有用，高等级不成长。
- [ ] 逐道验证模块文档七道神话料理：完整5～6 HP，无无限效果/再生/Resistance II，龙蛋羹为Health Boost II 300秒。
- [ ] 龙料理tooltip显示当前等级、时长和属性数值；摆盘逐口不重复整份预算，第二项Void Erosion正确授予。
- [ ] 四种茶均8/6，九道方块食品逐口结算，配方/容器/独特外观保留。

### 联合、数据包与恢复回归

- [ ] Crimson + Void + Resistance + Star联用仍能正常受伤/死亡，没有递归或全穿透；特殊效果不挤掉Nourishment/Vigor。
- [ ] TWT2配置与显式Profile冲突时Profile获胜；blacklist仍最高，显式0/0不回退；有无AppleSkin均无重复水滴结算。
- [ ] 高优先级数据包修改Recovery/Hydration/速度/主增益后reload，服务端查询和客户端tooltip同步；删去覆盖后恢复内置值。
- [ ] 高饱和半血恢复约5秒，储备不拖慢自然恢复、受击不暂停；Warmth/Quenched不突破10 tick自然下限。
- [ ] FD Nourishment保留原生时长和单一图标，More Delight短再生沙拉仍保留自身效果；普通Potion、Beacon、Golden Apple正常。

本清单供发布前真人体验验收；自动测试已覆盖的机制可复用证据，不要求重复全部数学测试。未声明完成长期多人、全部美术表现或整合包全生态验证。
