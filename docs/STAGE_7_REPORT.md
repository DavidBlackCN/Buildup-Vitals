# Stage 7 Completion Report

## 完成内容

本轮仅执行 **Stage 7 — Vanilla Balance Pack & First Integrated Prototype**，承接已验收的 Stage 6。形成原版食物 Alpha 数据与集成原型；核心计时参数维持已验收值。数值尚未定稿，长时间实玩与细调留给本阶段人工验收，不将自动测试当作战斗体验结论。

- 新增 35 份具体 Item Profile，加上已有 5 份，覆盖 26.3 全部 40 种 FOOD + CONSUMABLE 原版物品。旧示例水果 Tag 保留，共 41 份生产定义。
- 牛排、面包、烤马铃薯及其他基础口粮继续保持 Basic、完整原版营养，不因重复食用而受罚。
- 补充曲奇 Prepared、甜菜汤/兔肉煲/可疑炖菜 Meal。甜菜汤提供 3 HP + Restorative，兔肉煲提供 4 HP + Invigorated，可疑炖菜提供 1 HP 并保留独立原生效果；旧蘑菇煲与南瓜派数据不变。
- 补充水果和汤的 Hydration，保留 TWT2 配置与原生数据包优先级；新增数据仍通过原有消费路径结算一次。
- 为相近肉类、鱼类、根茎、浆果和镀金变体划分共享 Variety Group；不通过配方复杂度或稀有度自动升级质量，不为原版强造 Feast 或尚未实现的 Steady。
- 将 Recovery、Benefits、Variety 数值统一到 `CoreBalance`，原有 Balance 类保留引用入口。它是编译时默认预设，不是热重载用户配置；未改变玩家持久化格式或既有参数值。
- 将未适配/实验/未知增益测试移至三个仅测试用物品，避免测试资源覆盖官方胡萝卜和甜菜根数据，或继续误把烤马铃薯视为 fallback。

## 主要文件

- `src/main/resources/data/buildup_vitals/buildup_vitals/food_profiles/`：35 份新增原版数据，既有六份定义保留。
- `src/main/java/com/davidblackcn/buildupvitals/config/CoreBalance.java` 与三个原有 Balance 类：集中调参入口且保持当前行为。
- `src/gametest/java/com/davidblackcn/buildupvitals/VanillaPackGameTests.java`：完整覆盖、口粮重复、农场饮食、连续受击和难度边界的 5 项测试。
- `TestFoods.java`、测试入口和资源，以及既有 Diet/Recovery/Benefit/Tooltip/Thirst 测试：隔离测试数据、验证新食物接入客户端及可选补水。
- [VANILLA_BALANCE.md](VANILLA_BALANCE.md)：完整食物定位、分组、原型数值和调整边界；README 与 Food Profile 文档同步更新。

## 自动测试

工具链保持 Minecraft **26.3** / Java **25** / Gradle **9.7.1** / Loom **1.18.2** / Loader **0.19.5** / Fabric API **0.161.0+26.3**，使用原生非混淆名称，没有依赖升级或 Mixin 修改。

在 `26.3` 下执行，使用本机 Java 25，EULA 参数沿用用户对测试环境的授权：

```powershell
.\gradlew.bat build -PacceptMinecraftEula=true --console=plain
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true --console=plain
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true -PwithThirst=true --console=plain
.\gradlew.bat build runClientGameTest -PacceptMinecraftEula=true -PwithThirst=true '-PtestAppleSkinJar=run/compat/appleskin-fabric-mc26.3-3.0.10.jar' --console=plain
```

- 基础 Wrapper build：PASS，**95 项 JVM 测试、26 项服务端 GameTest**。
- Buildup 单独运行的 build + client：PASS。未安装 TWT2 的测试只确认适配关闭，口渴场景标记 NOT RUN。
- Buildup + TWT2 `1.6.2+26.3` 的 build + client：PASS，新增水果/汤补水、上游优先级、单次消费及重载回归通过。
- Buildup + TWT2 + AppleSkin `mc26.3-3.0.10` 的 build + client：PASS，26 项服务端测试与连接玩家回归通过，营养/补水提示不重复。

JVM 报告在 `build/reports/tests/test/index.html`。本轮日志保存在忽略目录 `run/stage7-build.log`、`run/stage7-standalone.log`、`run/stage7-twt.log`、`run/stage7-twt-appleskin.log`。最终统一新增文件为仓库 LF 换行后再次执行基础 build，结果 PASS，日志为 `run/stage7-final-build.log`。

初次新增测试曾因 26.3 字段访问和模拟时钟夹具失败：重复受击测试只推进 FoodData，需单独清除 LivingEntity 的 `damageCooldownTime`；已核对目标源码并修复，保留伤害确实生效的断言，没有放宽生产规则。修正后的基础 26 项全部通过。

## 实机/运行验证

- 客户端测试实际启动游戏，创建单人世界和同 JVM DedicatedServer，经 localhost TCP 连接；覆盖消费、背景恢复、和平难度协调、保存重进、切维度、死亡与重连。
- Tooltip 回归覆盖服务端默认值、数据包覆盖、故意失败的重载、删除定义、断线清空、中英文和 F3+H；新增断言检查烤马铃薯 Basic 与兔肉煲 Meal / Benefit。
- TWT2 测试分别移除西瓜片、甜菜汤、兔肉煲的高优先级配置后，从 `5/0` 消费到 `8/1`、`9/2`、`9/2`；测试结束恢复配置。原生默认配置仍优先，不把默认值不同误判成适配错误。
- 服务端功能场景验证 12 次连续基础口粮保持原版 Hunger/Saturation；混合农场食物获得正向奖励；胡萝卜与金胡萝卜切换不刷组；甜菜汤的三次定时回血不被连续受击取消。
- 四种难度测试验证统一 Well-fed 时序及原版饥饿边界：Easy 在 10 HP 停止，Normal 不致死，Hard 可致死；和平难度的完整玩家时钟另由既有客户端测试覆盖。
- 这些是可重复的受控机制场景，**没有声称实际完成长时间洞穴或连续整夜战斗**，也没有独立双进程和多客户端并发验证。
- 已查看本轮中文普通提示和 TWT2 + AppleSkin 补水提示截图，无缺字或重复图示；可选组合截图保存在 `run/stage7-visuals/`。原有长文字仍按既定后续优化处理。
- JSON、生产 JAR 和参数对照检查通过：打包 41 份 Profile，未混入测试物品、测试资源或 TWT2 类；既有六份 Profile 与所有旧 Balance 常量保持原值。

## 已知问题与自检

原版蛋糕通过方块逐口消费，牛奶没有 FOOD 组件，不进入当前食物入口；没有为凑齐 dairy 类而虚构元数据。它们仍按原版使用，本阶段未新增其恢复/增益/饮食快照。药水治疗继续独立。

TWT2 的 Quenched 独立回血、默认配置对 Profile 的覆盖、AppleSkin 治疗预测仍沿用 Stage 6 的已披露边界。图标与短文字 Tooltip 优化继续按用户意见保留在后续工作中。常用 GUI 比例、长时间实玩和最终数值体验仍需人工验收。

```text
Code Review: PASS WITH RISKS

Scope:
- Target: Minecraft 26.3 / 26.3
- Reviewed: 本轮生产默认参数、全部食物数据、测试隔离与集成用例、文档

Findings:
- BLOCKER: None
- MAJOR: None
- MINOR: None

Fixes Applied During Review:
- 隔离测试用食物与官方数据，保留 fallback / 未实现增益覆盖。
- 修正只推进 FoodData 的重复受击夹具，明确与完整玩家时钟的区别。
- 保持全部旧参数与 Profile 值，避免本轮顺带进行数值调整。

Verification:
- Wrapper build / 95 JVM / 26 server GameTests: PASS
- Standalone + TWT2 connected client matrix: PASS
- TWT2 + AppleSkin: PASS
- JSON / production JAR isolation: PASS
- Full diff / documentation / Update Notes: PASS

Residual Risks:
- 长时间体验与细调尚未进行；外部恢复与预测边界按正文保留。
- 未验证独立双进程、多客户端和全部 GUI 比例。
```

已按根目录 `CODE_REVIEW.md` 完整自检。运行日志中的 WMI 权限、开发账号 Realms、各向异性过滤及 `minecraft:end_of_frame` 提示，与 AppleSkin 无 JEI 时的可选目标警告仍存在；测试资源的未知/实验增益和故意重载失败为预期。没有 Buildup Mixin 注入失败或未解决的必需测试失败。阻断项：无。

`UPDATE_NOTES.md` 已追加 `✨ feat(balance)` 记录，概括原版食物适配、料理收益、分组、补水与原版营养保持；与本轮提交一起保存。

## 未进入的下一阶段内容

未进入 Stage 8 Core Freeze、第三方烹饪 Mod 适配、氧气或法力系统；未实现 Steady、负面料理状态、额外 HUD 或图标重绘。没有擅自微调此前恢复计时、奖励系数及持久化边界，也没有改变上游 TWT2 配置。

## 人工验收清单

建议复制测试世界，按正常 Survival 流程观察。每项记下难度、TWT2 是否启用及配置、食物消耗、开始/结束生命、明显不舒服的等待和是否被迫做额外维护；必要时用三个 `/buildupvitals` 查询命令记录 Profile、恢复和饮食状态。

- [ ] 开局只依赖面包、烤马铃薯、熟肉：能正常生存，不因饮食单一被惩罚。
- [ ] 长时间洞穴探索：记录口粮数量、恢复空档、药水使用和背包成本，比较携带汤/兔肉煲的价值。
- [ ] 连续夜间战斗：观察汤的渐进恢复能否缓解压力，同时保留受伤风险与药水的救急价值。
- [ ] 受伤后吃正式料理：无瞬间回血，受击不暂停已有储备，Recovery 耗尽后正常回到背景恢复。
- [ ] 仅带牛排远行：仍可靠，不需要为维持基础营养频繁换食物。
- [ ] 农场混合饮食：多样性是可感知的额外收益，不形成必须维护的家务。
- [ ] 无 TWT2 / 有 TWT2 分别实玩：水果、汤、饮水流程自然；记录 Quenched 额外治疗是否需要后续协调。
- [ ] Peaceful / Easy / Normal / Hard：观察自然恢复、饥饿后果和实际战斗容错，不以单一难度定稿数值。
- [ ] 综合回答：牛排是否值得带、料理是否值得做、恢复是否太快/太慢、药水是否仍有用、多样性是否成为负担。

## 建议下一步

Stage 7 在本轮实现与验证结束后停止，交付 Alpha 原型进行较长时间人工验收。根据记录集中微调，再由用户明确决定是否进入 Stage 8；本轮不自动推进。
