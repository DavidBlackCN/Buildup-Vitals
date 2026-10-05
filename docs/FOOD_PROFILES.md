# Food Profile v1

Stage 2 起，`recovery.health` 在服务端玩家完成进食后加入恢复储备，按 [恢复规则](RECOVERY.md) 逐渐回血。其余字段仍只加载和查询，不改变食物 Hunger/Saturation、口渴或饮食增益；示例值不是已定稿的平衡数据。

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
  "hydration": { "thirst": 4, "quenched": 2 },
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
| `quality` | `basic`、`prepared`、`meal`、`feast`；默认 `basic` |
| `recovery.health` | 有限非负数，单位 HP；默认 0；进食后加入最多 20 HP 的储备 |
| `hydration.thirst` / `quenched` | 非负 32 位整数；默认 0 |
| `diet.categories` | `protein`、`grain`、`vegetable`、`fruit`、`dairy`、`sweet` 的不重复数组；默认空 |
| `diet.variety_group` | 可选 namespaced ID；缺省时使用被查询物品自己的 ID，Tag 内物品不会自动被归为同组 |
| `traits` | 不重复的字符串数组，各项匹配 `[a-z0-9_]+`；默认空。`soup`、`drink`、`warm` 属于 Trait |
| `meal_benefit` | 可选 namespaced ID；仅保存引用，不校验或实现尚未建立的 Benefit registry |
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

没有命中时使用 fallback：`basic`、Recovery/Hydration 为 0、类别和 Trait 为空、无 Benefit/override、Variety Group 为物品 ID。本阶段不自动推断公共 Tag 类别；可通过 Tag Profile 显式赋予类别。非食物也可查询或声明 Profile，但不会因此变成可食用物品。

## 重载与错误处理

- `/reload` 重新读取资源并构建完整不可变快照，预先展开 Tag 为物品索引；查询为 Map 查找，不做文件 IO。
- 使用 26.3 原版 `TagLoader` 解析本轮数据包栈，避免在 pending Tag 尚未绑定时访问成员，也避免沿用旧 Tag 内容。额外的 Tag 解析仅发生在重载期间。
- `DataResourceStore` 将快照绑定到本轮服务端资源；没有跨世界的可变静态 Profile 缓存。整个原版重载成功时才切换到新资源。
- 一个 Profile 文件出错时，只跳过该文件；记录资源路径、数据包名、字段路径/JSON 语法位置及原因，其余有效 Profile 继续加载。
- 高优先级同路径文件无效时，不恢复被覆盖的低优先级版本；其余有效规则或 fallback 可接管。下一次修复并重载即可生效。
- 缺失的 Item/Tag selector 输出警告并跳过；已存在但为空的 Tag 合法，匹配零物品。
- 删除文件后，下次成功重载不保留旧定义；若下层数据包仍有同路径文件，会按原版规则重新显示下层版本。
- 原版重载整体失败时，服务端保留旧的整套资源和 Profile 快照。不要把 `Prepared food profile snapshot` 日志当作整轮重载已经成功。

## 查询命令

需要原版 Game Masters 权限（通常为 OP 2 或开启作弊），控制台也可用：

```text
/buildupvitals food profile minecraft:apple
/buildupvitals food profile minecraft:melon_slice
/buildupvitals food profile minecraft:bread
```

命令显示最终 Profile ID、文件路径、数据包、selector、优先级、fallback 状态、全部数据值和快照规模。参数使用原版 Item 参数解析和补全；数据组件参数不会改变按 Item ID 查询的语义。

内置三份示例：苹果具体 Item、蘑菇煲具体 Item、示例水果 Tag。水果 Tag 包括苹果、西瓜片和甜浆果；苹果应命中具体规则，西瓜片应命中 Tag，面包应走 fallback。
