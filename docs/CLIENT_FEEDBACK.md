# 客户端反馈 v2（Stage 7.5）

普通食物 Tooltip 使用服务端最终 Profile，显示品质、爱心 + 基础 HP、主要效果图标 + 短名称、非 normal 消费速度及饮食类别。示例蘑菇煲为「正式料理 / 爱心 +3 HP / 滋养图标 + 滋养 / 蔬菜」，不再使用长篇恢复和增益介绍。

恢复图标复用 Minecraft GUI 心形 sprite，增益图标为本模组 18×18 透明像素资源。使用 26.3 `FontDescription.AtlasSprite`，不修改整套 tooltip 渲染器。HP 是基础储备，不表示即时回血，2 HP=一颗心；Variety、剩余容量和积食会改变实际到账量。显示量裁剪到储备上限20，极小无效量省略，最多六位小数。

Quick/Fast 显示简短本地化文字；normal 或未声明档位不额外占一行。仅真实 Food+Consumable 添加 Buildup 食物行；fallback 标为 Basic，不虚构恢复、增益或类别。零恢复、未知/实验 Benefit 省略对应行。

## 水滴与 AppleSkin

安装受支持 TWT2 后使用其真实解析结果和纯度修正。无 AppleSkin 时 Buildup 复用上游水滴组件；有 AppleSkin 时由 TWT2 自己绘制，避免重复。Pure Water 的提示为10/8基准，盐水不显示虚假的补水。没有 TWT2 时不出现无效口渴提示。

AppleSkin 仅用于显示原版 Hunger/Saturation 以及上游口渴图示，不是本模组硬依赖。已验证版本为 mc26.3-3.0.10，来源见 [README](../README.md)。**其生命恢复预测仍是原版估算，不认识 Buildup Reserve/Overfull/Variety**；本轮不更改用户配置，不发布储备半透明心形 HUD。

## 效果 UI 与提示

Restorative、Invigorated、Steady、Overfull 都有正式图标、中英文名称和短说明。食物授予默认无粒子；原版负责 HUD、背包卡片及剩余时间同步。背包效果卡悬停追加简短说明，窄屏与展开卡片都有效。Steady 仅注册占位，不在食物提示中承诺实际收益。

负荷首次达到48由服务器发送一次 actionbar 轻提示，低于32后重置提示锁存；不每 tick 提醒，不新增负荷条，也不在普通食物 Tooltip 显示负荷数。已有积食通过原版负面状态图标表达。

## 高级提示与同步

F3+H 额外显示最终 Profile ID、原始 Recovery、Variety Group、Benefit 引用/是否实现与基础值说明。需要实时数值时使用 `/buildupvitals recovery [player]`，不把调试系数塞进普通 tooltip。

`buildup_vitals:food_profiles_v3` 是服务端到客户端的完整元数据快照，增加可选消费档位，客户端用于消费动画与 tooltip；实际治疗、负荷和授予始终由服务端决定。两端应一起更新，不混用旧 v2 通道。

登录、成功整轮 `/reload` 更新不可变快照；重载失败保留旧值；删除定义回落；连接初始化和断线清空。未收到可用快照不以客户端本地数据猜测服务器玩法，消费时长保留原生值。超过 65,536 条或16 MiB 时显示数据降级并记录警告，服务端机制仍运行；这种超大数据包下不保证自定义消费动画同步。

使用 Fabric large-payload 分片，校验枚举、数量、重复条目及非法数值，没有客户端上报生命或储备的通道。渲染只查内存，不读文件或发网络请求。当前状态效果走原版同步，不自行维护第二份计时器。

截图、真实连接与兼容测试结果见 [Stage 7.5 报告](STAGE_7_5_REPORT.md)。其他 HUD Mod、多人长期游玩和极端 GUI 比例需另外验收。
