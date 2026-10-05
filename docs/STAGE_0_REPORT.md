# Stage 0 Completion Report

日期：2026-10-05（Asia/Shanghai）。目标目录：`26.3/`。

状态：Stage 0 实现与必需自动验证通过；等待人工验收，未进入 Stage 1。

## 完成内容

- 初始化独立 Git 仓库，先以 `64d3435` 保存原始骨架，再提交本阶段修改。
- 核对并同步官方完整 Gradle Wrapper，确认 Java 25 工具链、公共入口和客户端入口。
- 固定 Loom 1.18.2，避免随 `1.18-SNAPSHOT` 变化；保留 Minecraft、Loader 和 Fabric API 的目标版本。
- 清除重复入口、空示例数据生成器、示例 Mixin 及其旧配置，保留两份空的正式 Mixin 配置。
- 图标归入 `assets/buildup_vitals/`，并在元数据中声明。
- 保留 MIT License 和设计原则，将根目录 `PLAN.md` 原样纳入此版本仓库。
- 无食物、回血、饥饿、HUD 或其他 Gameplay 修改。

## 主要文件

| 文件 | 原因 |
|---|---|
| `gradle.properties` | 固定 Loom 发布版本 |
| `build.gradle` | 明确 Java 25 toolchain；任务闭包预先捕获版本与归档名以兼容配置缓存 |
| `gradlew`、`gradlew.bat`、`gradle/wrapper/*` | 与官方固定提交同步校验；原始 Wrapper 已完整，批处理文件仅换行差异 |
| `src/main/java/com/davidblackcn/`、`src/client/java/com/davidblackcn/client/` 下的旧示例文件 | 删除模板重复入口和示例逻辑，保留 `com.davidblackcn.buildupvitals` 下正式入口 |
| 旧 `buildup-vitals*.mixins.json` | 删除未使用的示例配置 |
| `src/main/resources/fabric.mod.json`、`assets/buildup_vitals/icon.png` | 统一资源命名空间并声明图标 |
| `PLAN.md`、`README.md`、本报告 | 保存路线和复现方式，提供验收清单 |
| `BOOTSTRAP_NOTE.md` | 删除已经完成的临时 Wrapper 待办说明 |
| `UPDATE_NOTES.md` | 记录已验证的阶段成果 |

`DESIGN_PRINCIPLES.md`、MIT 正文和正式入口内容保持原样。全部修改仅位于 `26.3/`。

## 兼容性基线

| 项目 | 已验证值 |
|---|---|
| Minecraft | 26.3 |
| Java | JDK 25.0.3；编译目标 25；产物 class major 69 |
| Gradle | Wrapper 9.7.1 |
| Loom | 1.18.2 |
| 映射 | 26.3 原生非混淆名称；无额外 `mappings` 声明，非 Yarn |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.3 |
| 第三方 Gameplay 依赖 | 无 |

基线来源：[FabricMC 官方示例固定提交](https://github.com/FabricMC/fabric-example-mod/tree/44465cb0eb83932c72ece5934d32ddfc758802ed)。模板仍使用 `1.18-SNAPSHOT`；本项目使用 Fabric Maven 已发布的 [Loom 1.18.2](https://maven.fabricmc.net/net/fabricmc/fabric-loom/1.18.2/)，兼容性通过实际构建与两端启动确认。

Wrapper JAR SHA-256：`7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d`。

## 自动测试

以下命令均在 `26.3/` 执行，使用本机 JDK 25.0.3：

- `./gradlew.bat clean build --console=plain`：PASS，最终构建 8 个任务执行成功。
- `./gradlew.bat build --console=plain`：PASS。
- `./gradlew.bat runClient --console=plain`：PASS，正常退出后 Gradle 返回 0。
- `./gradlew.bat runServer --args=nogui --console=plain`：PASS，控制台 `stop` 后保存世界、正常退出，Gradle 返回 0；配置缓存复用成功。
- JAR 结构检查：PASS，两处正式入口、Java 25 字节码、空 Mixin 列表、图标和许可证齐全，无示例类或旧命名空间。
- 设计文档一致性：PASS，版本内 `PLAN.md` 和 `DESIGN_PRINCIPLES.md` 与根目录逐字节一致。
- Git 差异与空白检查：PASS；`PLAN.md` 原样保留 8 处 Markdown 双空格硬换行，普通 `diff --check` 会提示这些既有格式。逐项确认后，对其余改动执行 `diff --cached --check` 通过。
- 单元测试：`test NO-SOURCE`；本阶段没有业务逻辑，未新增测试框架。

产物：`build/libs/buildup_vitals-0.1.0-dev.jar`。

## 实机/运行验证

- 客户端：通过 computer-use 检查主菜单，创建默认生存世界 `New World`，进入世界并看到原版生命、饥饿和快捷栏；保存退出主菜单，再正常关闭客户端。
- 集成服务端：日志记录 26.3 世界创建、玩家加入、保存及停服。
- Dedicated Server：识别 `buildup_vitals 0.1.0-dev`，公共入口成功执行；生成世界后输出 `Done (1.249s)`，重启同一世界输出 `Done (0.232s)`，执行 `stop` 保存退出。
- 用户明确同意 EULA 后，才将本地 `run/eula.txt` 改为 `true`。测试服务器仅绑定 `127.0.0.1:25565`，保留默认在线认证。
- 本地日志：`run/stage0-client.log`、`run/stage0-server.log`；测试世界、配置、日志和 EULA 均受 `.gitignore` 排除。
- 首次服务端运行生成配置后因 EULA 未接受而退出，不计为启动成功；之后完成重新验证。第一次非交互服务端验证在就绪后被中断，随后改用可交互控制台验证正常停服。

## 已知问题

- 默认终端 Java 为 21，必须按 README 选择 JDK 25；未修改用户全局 Java 环境。
- 沙箱 WMI 硬件信息查询出现权限警告；可见桌面的客户端重跑无此警告，沙箱服务端仍有警告但成功就绪。
- Fabric 开发账号出现用户属性/Realms 联网认证错误，未阻止主菜单和本地世界启动；没有执行在线账号或 Realms 验证。
- 世界加载时出现一次 `Requested post effect does not exist: minecraft:end_of_frame` 警告，未进一步定位上游来源；未观察到阻断启动或世界渲染的问题。Mod 无渲染注册或注入逻辑。
- IDE Gradle 导入、独立服务端的真实多人连接和长期游玩尚未验证。没有未解决的构建或 Mod 加载阻断项。

## 自检结论

Code Review: PASS WITH RISKS

- Scope：依据根目录 `CODE_REVIEW.md` 检查目标目录完整差异、元数据、源码边界、版本和运行证据。
- BLOCKER：无。
- MAJOR：无。
- MINOR：上文运行警告及人工验收项保留。
- 自检修复：发现旧资源空目录仍进入 JAR，删除空目录后重新 `clean build`，再次检查产物通过。
- Update Notes：PASS；仅记录实际验证的 Stage 0 基线。
- 剩余风险：见已知问题；未将 `test NO-SOURCE` 或 IDE 未验证项目计为测试通过。

## 未进入的下一阶段内容

未实现 Food Profile、数据包加载器、调试命令、Recovery、Diet Memory、Meal Benefit、Tooltip 或 Thirst Was Taken 2 适配。

## 人工验收清单

- [ ] IDE 打开 `26.3/`，将 Gradle JVM 设为 JDK 25，确认导入无错误。
- [ ] 执行 `./gradlew.bat clean build`，确认 `build/libs/` 生成 Mod JAR。
- [ ] 执行 `./gradlew.bat runClient`，创建或进入测试世界，确认生命、饥饿与食用行为仍为原版。
- [ ] 执行 `./gradlew.bat runServer --args=nogui`，确认日志包含 `buildup_vitals 0.1.0-dev` 与 `Done`；输入 `stop`，确认保存退出。
- [ ] 检查日志无 Example Mixin、Hello Fabric world 残留，确认上述已披露警告不影响本机运行。
- [ ] 确认 `PLAN.md`、`DESIGN_PRINCIPLES.md`、`UPDATE_NOTES.md` 已在此 Git 仓库中。

## 建议下一步

按 `PLAN.md` 的 Stage Gate 停止，等待人工确认通过后才开始 Stage 1。
