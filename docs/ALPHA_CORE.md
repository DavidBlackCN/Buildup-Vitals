# Alpha Core Freeze

冻结版本：**0.1.0-alpha.1**，Minecraft26.3 / Fabric。Stage 7.5与10 tick热修复已经人工验收；Stage 8冻结Core。之后经独立授权加入 [FD Compatibility](FARMERS_DELIGHT_COMPAT.md)，沿用本页的Core参数和数据契约。

## 对外契约

| 接口 | 冻结范围 |
|---|---|
| Food Profile | [Schema v1](food-profile.schema.json)、[字段和优先级](FOOD_PROFILES.md)；可选consumption字段保持向后兼容 |
| 数据包入口 | `data/<namespace>/buildup_vitals/food_profiles/*.json`；Item优先Tag，同路径由原版包栈完整覆盖 |
| 默认食用 | 未知食物保留原生营养与消费，不虚构恢复、类别或效果；官方恢复基础值为0或至少1 HP |
| 恢复 | [Recovery](RECOVERY.md)：自然10/80、食物12/10 tick，连续Saturation公式、一个时钟、有限储备、独立治疗不受接管 |
| 玩家数据 | `buildup_vitals:recovery`、`diet_memory`、`overeat`附件；料理与积食效果使用原版MobEffect存储与同步；旧meal_benefit附件继续可迁移 |
| 生命周期 | 死亡清短期状态，存档/重连/存活换维度保留；成功reload替换完整Profile快照，失败保留旧快照，不追溯重算已有储备或效果 |
| 管理查询 | `/buildupvitals food profile <item>`、`recovery [player]`、`diet [player]`；保持管理员权限与服务端权威 |
| 可选兼容 | TWT2 Fabric1.6.2+26.3、FD Refabricated26.3-3.6.27，无硬前置；未知版本关闭相应行为适配；AppleSkin不作为运行依赖 |

Schema是兼容包的首选稳定入口。当前没有独立发布、承诺二进制兼容的Java扩展API；内部public类、Mixin注入点和网络 `food_profiles_v3` 不是给其他Mod直接调用的通用API。客户端/服务端应使用同一Buildup版本，不承诺跨版本联机协议兼容。未来必要的Schema或持久化变更需明确版本化和迁移，不能悄悄改变旧字段含义。

Traits、营养overrides及实验Steady仅保留数据语义，不视为已实现效果。当前FD本体兼容新增Foreign主增益桥接和80份Profile；FD Addon、Kaleidoscope Cookery、Oxygen、Mana或Stamina未加入。

## 工程边界

- common源码不引用Minecraft客户端类；Tooltip与客户端Mixin独立source set，服务端只发送不可变显示快照，不接收客户端治疗请求。
- Profile索引在reload期间建立，运行查询为Map查找；玩家饮食窗口固定十次，不在每tick遍历所有Profile或读取文件。
- 每位玩家独立存储恢复、饮食、积食及效果；服务器线程串行处理同tick进食，不创建跨玩家共享的可变身体状态。
- Profile解析错误日志包含文件、pack和字段/语法位置；单文件失效不丢弃其他有效文件。`Prepared food profile snapshot`仅表示准备阶段完成；整个reload成功才发布并同步。
- Linux Wrapper使用Git执行位100755与LF。CI运行服务端GameTest所需的EULA接受参数；工作流文件修改也触发构建。测试代码与第三方JAR不打包入发布产物。

## 验证范围与复用规则

下表记录 Stage 8 冻结时的基线。随后 FD 兼容已执行五组组合的完整构建，当前为108项JVM、47项服务端GameTest（40项Core+7项FD条件测试）和5个客户端入口；新增Profile全覆盖、补水优先级、Foreign互斥/生命周期、重载与成就验证，详见 [FD兼容矩阵](FARMERS_DELIGHT_COMPAT.md)。

| 检查 | 证据范围 |
|---|---|
| JVM | 108项：解析、优先级、恢复数学、消费速度、网络编解码、饮食和状态Codec |
| 服务端GameTest | 40项：在原有39项上增加多玩家同tick/交错进食、独立恢复时钟、负荷、存档及死亡隔离；有/无TWT2构建 |
| 客户端连接 | 4个入口；本轮扩展负恢复值/截断JSON隔离与修复重载，检查服务端与客户端一致；复用已有集成/同JVM专服TCP、重连、死亡和换维度场景 |
| HUD兼容 | 复用已通过的TWT2+AppleSkin3.0.10组合；本轮未改渲染、消费、补水或恢复生产逻辑，仅推进版本号 |
| 产物与文档 | 核对正式JAR元数据、资源和测试排除，校验当前文档本地链接和官方Profile最小单位 |

这是自动化机制验证，**多人GameTest使用模拟连接，单客户端专服连接测试使用同JVM DedicatedServer**；不是两台真人客户端或长时间独立进程联机压测。GitHub托管runner最终结果需下次push确认，本地检查不冒充远端CI成功。

对未改变的代码允许复用已有通过证据；修复或新增断言后执行受影响的检查。运行命令见 [README](../README.md)。每轮原始日志保存在忽略的run/build目录，完成报告存放工作区根目录 `docs/26.3/`，不纳入版本仓库。发布docs仅维护当前行为、Schema和契约，不积累逐阶段报告。

## 人工验收

- 使用Alpha JAR进入原有单人存档和专服；确认主要效果、Tooltip、储备和积食行为与已验收版本一致。
- 两名真人玩家分别吃苹果与调养料理，一人死亡/重连，确认另一人的恢复、饮食和效果不受影响。
- 按数据包指南修改、故意写坏、修复并删除同一Profile；确认日志可定位问题，重载和客户端展示一致。
- 下一次push检查Linux工作流不再出现 `./gradlew: Permission denied`，且build与产物上传成功。

完成Stage 8后停止，等待本阶段验收；冻结不等于已经发布Release或批准下一阶段兼容开发。
