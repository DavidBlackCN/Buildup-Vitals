# Changelog

## 0.1.0-alpha.1 — Alpha Core Freeze

Minecraft26.3 / Fabric，2026-10-06。首个Core冻结候选；版本产物可用于验收，未自动发布Release或创建Tag。

- 固化数据包Food Profile Schema v1、40种原版食物的官方平衡与未知食物fallback。
- 保留已验收的10 tick自然恢复基准、连续Saturation恢复量、食物12/滋养10/Stable80 tick、饮食奖励、积食与三档进食速度。
- 保留可见料理效果、图标Tooltip、可选TWT2补水及统一Quenched恢复调度。
- 补充多人状态隔离、独立存档与坏Profile重载恢复测试；保留既有死亡、换维度、重连及同步验证。
- 修复Linux Gradle Wrapper执行权限，完善CI触发与GameTest启动参数。
- 精简发布文档，完善数据包制作/排错流程和Core契约；阶段报告移到版本仓库外的本地工作区。

此前开发过程中的功能与修复明细见 [UPDATE_NOTES](UPDATE_NOTES.md)。当前参数与兼容边界以 [Alpha Core](docs/ALPHA_CORE.md) 及其链接的正式文档为准。
