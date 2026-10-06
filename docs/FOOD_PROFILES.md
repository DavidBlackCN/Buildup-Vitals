# Food Profile v1

Schema 仍为 v1，Stage 7.5 向后兼容增加可选消费速度。`recovery.health` 在未积食的服务端玩家完成进食后加入储备，按 [Recovery v2](RECOVERY.md) 兑现。显式 `meal_benefit` 授予正式 MobEffect，Quality 决定默认时长；已桥接的外来原生消费效果保留自身时长。[饮食记忆](DIET_MEMORY.md) 提供少量奖励。安装已验证 TWT2 时，匹配的 Profile Hydration 在 blacklist 之后、TWT2 配置/drinks 之前生效。Traits 和 overrides 仍只加载与查询，不改写原生营养。

官方平衡的 `recovery.health` 允许0；非零最小值为 **1 HP（半颗心）**，优先整数1～6。此为官方数据约束，不改变 Schema v1 对第三方非负有限小数的支持，也不取整 Variety 加成、储备余额或缺血裁剪。普通食物约1、Prepared约1～2、Meal约3～4、Feast约4～6；干燥、特殊或不适合恢复的食物可为0。

## 文件位置

将 JSON 放入数据包的 `data/<namespace>/buildup_vitals/food_profiles/<path>.json`。
Profile ID 为 `<namespace>:<path>`。例如：

```text
my_pack/
├── pack.mcmeta
└── data/my_pack/buildup_vitals/food_profiles/stew.json
```

Minecraft 26.3 数据包格式为 121.0。最小 `pack.mcmeta`：

```json
{
  "pack": {
    "description": "My Buildup food profiles",
    "min_format": 121,
    "max_format": 121
  }
}
```

示例 `stew.json`：

```json
{
  "schema_version": 1,
  "selector": { "item": "minecraft:mushroom_stew" },
  "priority": 0,
  "quality": "meal",
  "recovery": { "health": 3.0 },
  "hydration": { "thirst": 6, "quenched": 4 },
  "diet": {
    "categories": ["vegetable"],
    "variety_group": "my_pack:stew"
  },
  "traits": ["soup", "warm"],
  "meal_benefit": "buildup_vitals:restorative"
}
```

Tag 选择器使用 `"selector": { "tag": "my_pack:meals" }`，不带 `#`。Tag 文件遵循原版 `data/my_pack/tags/item/meals.json`，支持嵌套 Tag、`replace` 和可选条目。

## 字段与默认值

机器可读规范：[food-profile.schema.json](food-profile.schema.json)。不要把 `$schema` 字段加入 Profile 本身，未知字段会被拒绝。

| 字段 | 规则 / 默认值 |
|---|---|
| `schema_version` | 必填，整数 1 |
| `selector` | 必填，仅有 `item` 或 `tag` 其中之一；必须显式写 `namespace:path` |
| `priority` | 32 位有符号整数；默认 0，较大者优先 |
| `quality` | `basic`、`prepared`、`meal`、`feast`；默认 `basic`；增益基础持续 600 / 1200 / 2400 / 3600 tick，也提供轻量 Variety 权重 |
| `consumption.speed` | 可选 `normal` / `quick` / `fast`，显式档位分别为32 / 21 / 16 tick；未声明或空 consumption 对象保留物品原生时长，积食最后乘1.25并向上取整 |
| `recovery.health` | 有限非负数，单位 HP；默认 0；作为基础量接受最多 +15% Variety 奖励，储备总上限仍为 20 HP |
| `hydration.thirst` / `quenched` | 非负 32 位整数；默认 0；可选适配分别裁剪到 0–20 后交给 TWT2 结算，不乘 Variety |
| `diet.categories` | `protein`、`grain`、`vegetable`、`fruit`、`dairy`、`sweet` 的不重复数组；默认空 |
| `diet.variety_group` | 可选 namespaced ID；缺省时使用被查询物品自己的 ID，Tag 内物品不会自动被归为同组 |
| `traits` | 不重复的字符串数组，各项匹配 `[a-z0-9_]+`；默认空。`soup`、`drink`、`warm` 属于 Trait |
| `meal_benefit` | 可选 namespaced ID；原生 `buildup_vitals:restorative`、`buildup_vitals:invigorated`；受支持 FD 存在时还支持 `farmersdelight:nourishment`；`buildup_vitals:steady` 保留为实验类型 |
| `overrides.hunger` | 可选非负 32 位整数，绝对食物点数；缺省表示保留原版 |
| `overrides.saturation` | 可选有限非负数，绝对饱和点数，非原版 saturation modifier；缺省表示保留原版 |

显式 `null`、未知字段、重复 JSON 键、重复数组项、错误大小写、数字字符串、小数整数、越界整数、非有限数及负数均会被拒绝。不接受注释、尾随逗号或同文件多个 JSON 文档。解析深度上限为 32。数值以 Java `double` 保存，极小非零数可能舍入为零。

## 覆盖和冲突规则

先使用原版资源包栈解析同路径文件：高优先级数据包的文件完整替换低优先级文件，不做字段合并。资源过滤规则也由原版处理。

不同 Profile 同时命中物品时，按以下次序选择：

1. 具体 Item selector 优先于 Tag selector。
2. 同类 selector 中，原版数据包栈位置较高者优先。
3. 数据包栈位置相同，`priority` 较大者优先。
4. 仍相同，完整 Profile ID 按字符串字典序较小者优先。

若要确保覆盖内置 Item Profile，应覆盖其同路径文件或提供自己的 Item Profile。高优先级 Tag 不会盖过具体 Item。

同类 selector 的重叠会输出冲突警告，包含首个重叠物品、赢家和被覆盖文件及数据包。同一对 Profile 在一次重载只警告一次。Tag 被明确 Item 覆盖是正常行为，不警告。

没有命中时使用 fallback：`basic`、Recovery/Hydration 为 0、类别和 Trait 为空、无 Benefit/override、Variety Group 为物品 ID。不自动推断公共 Tag 类别；可通过 Tag Profile 显式赋予类别。非食物也可查询或声明 Profile，但不会因此变成可食用物品。受支持 FD 的现有 Consumable 饮料也结算 Profile/Diet；其他普通药水或无消费组件物品不因 Profile 自动进入 Food Recovery。

FD 存在时内置兼容包提供 80 份显式数据，路径为 `data/buildup_vitals/buildup_vitals/food_profiles/farmersdelight/<item>.json`，Profile ID 为 `buildup_vitals:farmersdelight/<item>`。不需更改 Schema；最终 Profile 决定主增益，FD 后续原生 Nourishment 授予不能覆盖数据包改为原生增益/无增益的选择。原生 Nourishment 时长不乘 Variety；无原生消费来源时才回退 Quality 默认时长。全表与例外见 [FARMERS_DELIGHT_COMPAT](FARMERS_DELIGHT_COMPAT.md)。

More Delight 存在时另启用 `buildup_vitals:more_delight` 内置包，为目标 `26.09.16-26.3-fabric` 的31种食品提供数据。路径为 `data/buildup_vitals/buildup_vitals/food_profiles/moredelight/<item>.json`，Profile ID 为 `buildup_vitals:moredelight/<item>`。沿用相同 Schema、覆盖与 Nourishment 语义，无需添加新机制；全表和例外见 [MORE_DELIGHT_COMPAT](MORE_DELIGHT_COMPAT.md)。

## 重载与错误处理

- `/reload` 重新读取资源并构建完整不可变快照，预先展开 Tag 为物品索引；查询为 Map 查找，不做文件 IO。
- 使用 26.3 原版 `TagLoader` 解析本轮数据包栈，避免在 pending Tag 尚未绑定时访问成员，也避免沿用旧 Tag 内容。额外的 Tag 解析仅发生在重载期间。
- `DataResourceStore` 将快照绑定到本轮服务端资源；没有跨世界的可变静态 Profile 缓存。整个原版重载成功时才切换到新资源。
- 一个 Profile 文件出错时，只跳过该文件；记录资源路径、数据包名、字段路径/JSON 语法位置及原因，其余有效 Profile 继续加载。
- 高优先级同路径文件无效时，不恢复被覆盖的低优先级版本；其余有效规则或 fallback 可接管。下一次修复并重载即可生效。
- 缺失的 Item/Tag selector 输出警告并跳过；已存在但为空的 Tag 合法，匹配零物品。
- 未知或实验 `meal_benefit` 输出包含文件、数据包、字段和 ID 的警告；只忽略增益行为，保留该 Profile 的 Recovery 等字段。查询仍显示原引用与 `benefit_available=false`。不会清除或刷新玩家已有增益。
- 成功重载只影响之后进食的增益授予；不追溯移除或重算已获得增益的剩余时间。
- 饮食记忆保存吃下当时的 Profile 字段快照，重载不改写旧记录；之后进食使用新字段，旧记录按十次窗口自然移出。
- 删除文件后，下次成功重载不保留旧定义；若下层数据包仍有同路径文件，会按原版规则重新显示下层版本。
- 原版重载整体失败时，服务端保留旧的整套资源和 Profile 快照。不要把 `Prepared food profile snapshot` 日志当作整轮重载已经成功。

## 查询命令

Stage 5 的 [食物 Tooltip](CLIENT_FEEDBACK.md) 使用服务端的最终匹配结果。登录与成功 `/reload` 后更新客户端完整显示快照；重载整体失败保留旧显示。无需在客户端另装服务器的数据包，未同步时不使用本地默认数据推断服务器玩法。

需要原版 Game Masters 权限（通常为 OP 2 或开启作弊），控制台也可用：

```text
/buildupvitals food profile minecraft:apple
/buildupvitals food profile minecraft:melon_slice
/buildupvitals food profile minecraft:baked_potato
```

命令显示最终 Profile ID、文件路径、数据包、selector、优先级、fallback 状态、全部数据值和快照规模。参数使用原版 Item 参数解析和补全；数据组件参数不会改变按 Item ID 查询的语义。

Stage 7.5 更新全部40份原版 Item Profile，保留示例水果 Tag；完整表见 [VANILLA_BALANCE](VANILLA_BALANCE.md)。Recovery Hotfix 后蘑菇煲为 Meal / Recovery 3 / Restorative，南瓜派为 Prepared / Recovery 2 / Invigorated；牛排、面包、烤马铃薯保留 Basic，均为1 HP恢复，不附加主要增益。苹果 quick、西瓜片与曲奇 fast，曲奇因干燥不提供直接恢复。消费速度随服务端成功重载的 v3 快照同步；删除字段恢复原生时长，无需迁移旧 v1 JSON。

## 数据包制作与排错流程

1. 在测试世界的 `datapacks/my_pack/` 创建上述 `pack.mcmeta` 和一个Profile；ZIP包应让 `pack.mcmeta` 位于ZIP根目录。
2. 修改现有食物时使用Item selector；希望覆盖内置蘑菇煲可使用 `data/buildup_vitals/buildup_vitals/food_profiles/mushroom_stew.json`，也可使用自己的namespace和相同Item selector。完整覆盖不合并字段，遗漏字段恢复默认值。
3. 执行 `/reload`，用 `/datapack list enabled` 确认启用，再用 `/buildupvitals food profile minecraft:mushroom_stew` 查看最终来源；如果包未启用，先通过原版 `/datapack enable` 启用。
4. 受伤且未积食时完成一次进食，使用 `/buildupvitals recovery` 和 `/buildupvitals diet` 验证储备、效果和历史；Tooltip展示基础量，实际Variety奖励以服务端查询为准。
5. 修改数值、重载、再次进食；旧储备和已有料理效果不会被追溯重算。删除Profile并重载，确认回到下层定义或fallback，消费档位也不残留。

| 现象 | 检查 |
|---|---|
| 文件不生效 | 路径中的两层namespace/目录是否正确；包是否启用；selector物品是否存在；查询结果由谁覆盖 |
| 日志 `Skipping food profile` | 按日志的文件、pack、字段修正；负恢复量或截断JSON只隔离该文件，其余有效Profile继续工作 |
| 高优先级坏文件后恢复量变0 | 同路径低优先级文件已被遮盖；修好高优先级文件再reload，或删除它恢复下层版本 |
| `Prepared food profile snapshot` 后仍是旧值 | 该日志只表示准备完成；检查整轮reload是否被其他监听器拒绝，失败时两端都保留旧快照 |
| Tag没有覆盖原版食物 | 具体Item selector优先于Tag，与Tag所在包的优先级无关 |
| 无补水或与基础值不同 | 核对已验证TWT2版本、blacklist、水质和裁剪规则，见 [HYDRATION](HYDRATION.md) |

Alpha冻结范围见 [ALPHA_CORE](ALPHA_CORE.md)。第三方小数恢复仍合法，官方最小1 HP只是内置平衡约定。`traits`、`overrides`和实验Steady不构成已实现玩法；未知食物可直接fallback，不必为了安全食用而添加空兼容包。
