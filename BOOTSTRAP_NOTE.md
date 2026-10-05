# Bootstrap Note

此会话生成的工程骨架包含 Gradle 配置与 wrapper properties，但由于当前执行环境无法直接导出 GitHub 上的二进制 `gradle-wrapper.jar`，没有伪造或内嵌该二进制文件。

正式首次开工时，Codex 应将 FabricMC 官方 `fabric-example-mod` 的 `26.3` 分支作为唯一模板来源，同步：

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`

随后执行 `clean build`、`runClient` 与 `runServer`。完成后可以删除本说明文件。
