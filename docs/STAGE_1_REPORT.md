# Stage 1 Completion Report

日期：2026-10-05（Asia/Shanghai）。目标：`26.3`，Food Profile Data Foundation。

## 完成内容

- 实现 schema v1 的严格 JSON 解析、Item / Item Tag selector、确定性冲突优先级和中性 fallback。
- 使用 Fabric `DataResourceLoader` / `DataResourceStore` 构建并发布不可变快照，预编译物品索引，查询不访问磁盘。
- 重载读取本轮 Tag 内容，支持原版嵌套 Tag、`replace` 和可选条目；删除 Profile 不残留，整轮重载失败保留旧快照。
- 无效 Profile 单独跳过，日志提供文件、数据包、字段/语法位置和原因。
- 添加苹果、蘑菇煲和水果 Tag 三份示例，以及 `/buildupvitals food profile <item>` 开发查询命令。
- 提供 JSON Schema、数据包作者文档和 56 项 JVM 测试。所有 Profile 值当前只用于读取和查询。

## 主要文件

| 文件 | 用途 |
|---|---|
| `src/main/java/com/davidblackcn/buildupvitals/food/profile/` | 数据模型、来源、优先级、fallback 和不可变索引 |
| `src/main/java/com/davidblackcn/buildupvitals/data/loader/` | 严格解析和服务端资源重载 |
| `src/main/java/com/davidblackcn/buildupvitals/command/FoodProfileCommand.java` | 有权限限制的查询命令 |
| `src/main/java/com/davidblackcn/buildupvitals/BuildupVitals.java` | 注册入口 |
| `src/main/resources/data/buildup_vitals/` | 示例 Profile 和 Item Tag |
| `src/main/resources/assets/buildup_vitals/lang/` | 示例 Tag 中英文名称 |
| `src/test/java/com/davidblackcn/buildupvitals/food/profile/` | 解析、匹配、快照和错误路径测试 |
| `build.gradle` | 固定 JUnit BOM 5.11.4，仅测试依赖 |
| [FOOD_PROFILES.md](FOOD_PROFILES.md)、[food-profile.schema.json](food-profile.schema.json) | 数据包格式、默认值、覆盖及失败行为 |
| [README.md](../README.md)、[UPDATE_NOTES.md](../UPDATE_NOTES.md) | 使用入口和本次更新说明 |

## 兼容性决策

沿用 Stage 0 工具链：Minecraft 26.3、Java 25（本机 25.0.3）、Gradle 9.7.1、Loom 1.18.2、Loader 0.19.5、Fabric API 0.161.0+26.3。使用 26.3 原生非混淆名称；未添加 Yarn、Mixin、Access Widener 或生产依赖。

同路径文件由原版资源包栈处理；不同 Profile 按 Item 优先于 Tag、实际数据包栈优先级、数值 `priority`、Profile ID 字典序依次裁决。没有额外的数据包排序配置。

目标源码与首次运行证明：26.3 的 pending registry lookup 能查到 Tag 键，但成员尚未绑定。最初读取该 HolderSet 的实现导致服务端加载失败；已改用原版公开 `TagLoader.loadTagsForRegistry` 解析本轮资源，随后通过冷启动及多轮 Tag 替换验证。额外 Tag 解析只发生在重载阶段，不修改共享 Holder。

## 自动测试

在 `26.3` 目录、JDK 25 环境实际执行：

- `.\gradlew.bat genSources --console=plain`：PASS。首次沙箱执行因临时目录权限失败，提升执行权限后成功，供核对目标 API 使用。
- `.\gradlew.bat build --console=plain`：PASS，56 项测试，0 失败、0 错误。解析测试 49 项、快照测试 7 项。
- 资源及 Schema JSON 语法检查：PASS；Schema 与解析规则人工对照，未运行第三方 JSON Schema 验证器。
- `git diff --check`：PASS。

测试覆盖枚举、ID、默认值、全部数据字段、错误类型/范围、重复键和数组项、未知字段、异常嵌套、内置样例、Item/Tag 优先级、数据包优先级、确定性裁决、警告去重、缺失/空 Tag、fallback、删除/Tag 更新后的新快照及旧快照不可变性。

本地测试报告：`build/reports/tests/test/index.html`。产物：`build/libs/buildup_vitals-0.1.0-dev.jar`。

## 实机/运行验证

`.\gradlew.bat runServer --console=plain`：修复后启动、正常 `stop`、退出码 0；含错误 Profile 的再次冷启动也成功。服务端验证使用临时高低两个数据包，并按实际 `/datapack` 顺序调整优先级。

| 场景 | 实测结果 |
|---|---|
| 默认数据及查询 | 3 个有效 Profile、4 个索引物品；苹果命中 Item，西瓜片命中 Tag，面包 fallback |
| 同路径高低数据包 | 高包苹果 Recovery 7 覆盖低包的 5 |
| 不同路径竞争 | 高包 `priority=0` 的面包规则胜过低包 `priority=1000` |
| 同轮 Tag 替换 | `replace` 后胡萝卜命中 Tag，西瓜片退出匹配；苹果仍由 Item 规则决定 |
| 嵌套及可选条目 | 嵌套 Tag 正确展开，可选缺失物品不阻断 |
| 无效文件隔离 | 语法错误、负数和未知物品均输出诊断，其余 Profile 继续生效 |
| 修改和删除 | 苹果 Recovery 7→9 立即生效；删除土豆 Profile 后恢复 fallback |
| 原版整体重载失败 | 注入错误 loot table，同时将 Recovery 改为 99；重载失败仍查询到 9；修复后才变为 99 |
| 高包同路径文件无效 | 不复活低包旧文件，由仍有效的内置苹果规则接管 |
| 带错误 Profile 冷启动 | 成功进入 `Done`，错误文件被跳过 |
| 禁用测试数据包 | 完整恢复内置 3/4 快照 |

`.\gradlew.bat runClient --console=plain`：PASS，正常退出码 0。通过可见客户端创建并进入生存世界（允许命令），游戏内查询苹果成功，`/reload` 成功，保存退出成功。没有新增 Mod 加载或重载异常。

运行证据保存在本地忽略目录：`run/stage1-reload.log`、`run/stage1-cold-start.log`、`run/stage1-client.log`。测试数据包已停用并移至 `run/stage1-test-packs/`，不随发布包或提交分发。没有将首次失败启动误计为通过。

## 自检

```text
Code Review: PASS WITH RISKS

Scope:
- Reviewed files: 本报告列出的全部新增及修改文件、构建产物和资源元数据
- Target version/directory: Minecraft 26.3 / 26.3

Findings:
- BLOCKER: None
- MAJOR: None
- MINOR: None

Fixes Applied During Review:
- 核对有限浮点数上界和异常 JSON 嵌套限制，补充对应测试。
- 补充示例 Item Tag 的中英文翻译，消除默认资源的缺失翻译提示。

Verification:
- Wrapper build / 56 JVM tests: PASS
- Dedicated Server / reload / cold start: PASS
- Client world / query / reload / save and exit: PASS
- common/client 边界、无玩家数值写入、无新增 Mixin/AW: PASS
- JSON / 文档路径 / 最终差异 / 发布 JAR 内容: PASS
- Update Notes: PASS

Residual Risks:
- 未进行多客户端联机、第三方 Mod 矩阵或长时间压力测试。
- 未逐项进行实际进食数值对照；本阶段没有注册食用处理器或修改玩家数值的代码。
```

## 已知问题

- 开发账号出现用户属性/Realms 认证告警；不影响已验证的单人世界和独立服务端。
- 客户端仍有 Stage 0 已记录的 `minecraft:end_of_frame` post-effect 告警，当前未观察到进入世界或重载失败；未在本阶段修改原版渲染。
- 示例值是数据层测试值，尚不构成食物平衡或实际恢复效果。
- 当前阶段无阻断项；第三方/多人/长时间兼容性仍待后续阶段验证。

## 未进入的下一阶段内容

未实现 Stage 2 的 Recovery Reserve、食用事件、回血控制器、玩家状态存储或同步；也未实现 Hunger/Saturation 改写、Diet Memory、Meal Benefit 效果或 Thirst Was Taken 2 接入。

## 人工验收清单

1. 使用 JDK 25，在 `26.3` 执行 `.\gradlew.bat build`；确认成功并查看测试报告。
2. 启动 `.\gradlew.bat runClient`，进入允许命令的测试世界，执行以下命令：

   ```text
   /buildupvitals food profile minecraft:apple
   /buildupvitals food profile minecraft:melon_slice
   /buildupvitals food profile minecraft:bread
   ```

   - [ ] 苹果显示 `buildup_vitals:apple`，西瓜片显示 `buildup_vitals:example_fruit`，面包显示 `fallback=true`。
3. 按 [数据包文档](FOOD_PROFILES.md) 在测试世界添加自己的数据包，用 Item selector 指向苹果，设置 `recovery.health=7`。
   - [ ] `/reload` 后查询苹果显示自己的文件和数据包，以及数值 7。
   - [ ] 改为 9 并重载后立即更新；删除该文件并重载后恢复内置规则。
4. 将上述文件的 `recovery.health` 改为 -1。
   - [ ] 重载不中断其他有效 Profile，日志含文件路径、`$.recovery.health` 和非负约束原因。
5. 创建一个 Tag Profile；为同一物品再建具体 Item Profile。
   - [ ] 查询命中 Item；修改 Tag 成员后重载，原成员不残留。
6. 正常吃原版食物并观察游戏。
   - [ ] 没有本 Mod 新增的回血、口渴、饮食记忆或增益行为。

## 建议下一步

等待人工验收确认；按 [PLAN.md](../PLAN.md) Stage Gate 停在 Stage 1，不自动进入 Stage 2。
