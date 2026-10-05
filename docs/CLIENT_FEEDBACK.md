# 客户端反馈（Stage 6）

食物 Tooltip 展示服务端 Food Profile 的品质、基础恢复、已实现料理增益和饮食类别，不需要其他 HUD Mod。提供简体中文与英文翻译；没有新增常驻状态条。

## 普通提示

蘑菇煲示例（物品名称由原版提供）：

```text
正式料理
逐渐恢复 3 点生命值（基础）
提供「调养」饮食增益
蔬菜
```

- Quality 始终显示，Basic / Prepared / Meal / Feast 分别使用灰、绿、金、浅紫色。
- Recovery 以生命值 HP 为单位，2 HP 等于一颗心。显示基础储备量，不表示瞬间治疗，不预测当前玩家实际收到的治疗量。
- 普通提示中的 Recovery 不超过既有 20 HP 储备上限；小于 `0.000001 HP` 的无效基础量不显示。有效数值最多六位小数，去掉末尾零。
- 当前储备余量、多样饮食奖励、创造/旁观模式和恢复条件会影响实际结果。具体机制见 [RECOVERY.md](RECOVERY.md)；本阶段不读取或同步玩家的这些状态。
- 只有已实现的 Restorative / Invigorated 显示普通增益行。未知引用和实验 Steady 不会被描述为可获得的效果。
- 类别合并为一行，以 ` · ` 分隔；无类别时不虚构类别。零 Recovery、无 Benefit 均省略对应行。
- 未匹配 Profile 的食物显示“基础食物”，继续保留原版营养。物品栈必须同时具有 Food 和 Consumable 组件才添加食物提示；为非食物配置 Profile 不会把它变成食物。

提示按物品 ID 查询，与服务端消费查询一致，不依据单个物品栈组件重新推断 Profile。Hunger / Saturation 仍由原版组件及其他显示 Mod 负责；Stage 6 的 Hydration 图标复用 TWT2 + AppleSkin 自带的提示，具体开关、优先级和不安装 AppleSkin 时的显示边界见 [HYDRATION.md](HYDRATION.md)。

用户通过 Stage 5 验收后确定了后续优化方向：普通 Tooltip 优先使用 icon + 短文字，Recovery 探索爱心数量/半格表达；料理增益待专用图标准备后再统一调整。本阶段保留既有恢复和增益文本。

## 高级提示

按 **F3+H** 开启原版高级 Tooltip 后，额外显示：

- 最终 Profile ID，未匹配则为 `fallback`；
- 未裁剪的 `recovery.health` 原始数值；
- 解析后的 `variety_group`；
- `meal_benefit` 原始引用和是否已实现；
- 基础数值受饮食奖励与储备上限影响的说明。

“可用 / available”只指该增益类型已实现，不代表玩家当前拥有这个增益。没有泄露服务器数据包本地路径，也没有在普通 Tooltip 中堆放评分、重复系数或计时字段。

## 服务端同步与边界

`buildup_vitals:food_tooltips_v2` 是仅服务端到客户端的完整食物元数据快照，Stage 6 新增 Hydration 及服务端适配启用标记。服务端在玩家加入和整轮数据包成功重载后发送已解析的 Item/Tag 最终匹配结果；客户端无需复制服务器数据包。两端应同时更新，旧 v1 通道不混用。

客户端一次性替换不可变 Map。整体重载失败时双方继续使用上一次成功结果；成功删除定义会移除旧条目，必要时显示下层数据包或 fallback。与服务端本来的错误隔离语义一致：单个非法文件被跳过但整轮重载成功时，显示新的有效结果。

新连接初始化和断线均清空缓存。尚未收到快照、对方没有发送通道或服务器显示快照超限时，不添加 Buildup 食物行，避免使用本地默认数据冒充服务器规则。收到合法的空快照则意味着全部食物使用已知 fallback。

Fabric large-payload API 负责分片，应用层最多接受 65,536 个条目、16 MiB 编码数据；类别、枚举、非有限/负恢复值和重复条目均校验。服务器发送前检查大小，超限时记录警告并发送“显示不可用”的空状态，不中断现有服务端食物机制。没有客户端上报治疗、评分或资源消耗的通道。

渲染时只查询内存中的显示数据，不读磁盘、不发网络请求。只同步食物静态元数据，不修改恢复、增益、饮食附件的持久化格式，也不改变数值或食物数据包。

## AppleSkin 与心形预览调研

验证基准为作者发布的 [AppleSkin Fabric mc26.3-3.0.10](https://maven.ryanliptak.com/squeek/appleskin/appleskin-fabric/mc26.3-3.0.10/)，代码来源为 [AppleSkin](https://github.com/squeek502/AppleSkin)。可选测试命令见 [README](../README.md)。AppleSkin 仅以本地 JAR 加入 GameTest 运行时；发布包不包含它，也没有硬依赖或反射集成。

Buildup 通过 Fabric `ItemTooltipCallback` 添加自己的说明；AppleSkin 的原版 Hunger / Saturation 图示由其自身绘制。Buildup 不重复添加这些图示，不取消其他 Mod 的 Tooltip 回调。

**AppleSkin 的生命恢复预测尚未适配 Buildup 规则。** 该版本 `FoodHelper.getEstimatedHealthIncrement` 按原版营养与自然恢复计算，不认识本模组的 Recovery Reserve、不同恢复门槛和饮食奖励，因此心形预测不应视为本模组治疗承诺。需要避免该预测时，可由玩家在 AppleSkin 配置中关闭 `showFoodHealthHudOverlay`；本模组不擅自改写其配置。Hunger / Saturation 图示与这一限制是不同的功能。

本阶段按 PLAN 允许的调研范围，**不发布 Recovery Reserve 半透明心形预览**。26.3 的 `net.minecraft.client.gui.Hud.extractPlayerHealth/extractHearts` 使用 `GuiGraphicsExtractor`，并处理多排生命、吸收心、状态纹理、受击闪烁与抖动。可靠的储备预览还需要服务端玩家储备同步、最大生命/吸收适配、与 AppleSkin 预测的明确优先级及多 GUI 比例验证。当前仅有食物静态显示数据，不能据此画出玩家剩余储备。此结论避免引入未经验证的心形覆盖或新进度条。

本轮基础兼容范围是未安装 AppleSkin、安装上述版本后的普通/高级食物提示及客户端/服务端连接；其他 HUD/食物查询 Mod 和用户整合包仍需分别验证。
