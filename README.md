# 生物指南针 Entity Compass

适用于 Minecraft `1.21.11` 的 Fabric MOD。它添加了一个“生物指南针”物品，可以指向已选择的生物类型或在线玩家，并提供 HUD 距离显示、近距离失效和全局设置。

当前版本：`1.0.6`

## 运行要求

- Minecraft `1.21.11`
- Fabric Loader `0.18.4` 或更高版本
- Fabric API
- Java 21

多人游戏需要服务端安装本 MOD；想使用目标菜单和 HUD 的客户端也需要安装本 MOD。

## 功能

- 右键生物指南针打开目标选择菜单。
- 可以选择一种生物，也可以选择在线玩家。
- 选择生物后，指南针会指向范围内最近的同类已加载实体。
- 选择玩家后，指南针会指向指定在线玩家。
- 按 `M` 打开全局设置菜单。
- 全局设置支持控制附魔光效、距离显示位置和近距离失效。
- 多人服务器中，只有管理员可以修改全局设置。

## 合成配方

```text
 E
ECE
 E
```

- `C`：指南针
- `E`：末影珍珠

## HUD 提示

- 距离文字会显示当前目标名称和距离。
- `目标在附近！` 表示最近目标小于近距离失效阈值。
- `找不到目标！` 表示当前目标不可用，或不在可追踪范围内。

指南针会优先判断最近的匹配目标。如果最近目标小于阈值，它会显示近距离提示，而不会跳过该目标去锁定更远的目标。

## 安装

1. 构建或下载 `entity-compass-1.0.6.jar`。
2. 将 jar 放入客户端或服务端的 `mods` 文件夹。
3. 确认 Fabric API 也在 `mods` 文件夹中。
4. 使用 Fabric 启动游戏。

## 构建

在 Windows PowerShell 中运行：

```powershell
.\gradlew.bat build
```

构建完成后，发布 jar 会生成在：

```text
build/libs/entity-compass-1.0.6.jar
```

项目版本由 [gradle.properties](gradle.properties) 中的 `mod_version` 控制。`build/` 和 `.gradle/` 是可再生成的构建目录，不需要提交。

## 项目文件

- [使用说明.md](使用说明.md)：更详细的中文使用说明。
- [build.gradle](build.gradle)：Gradle 和 Fabric Loom 构建配置。
- [gradle.properties](gradle.properties)：Minecraft、Fabric、Loom 和 MOD 版本配置。
- [src/main/resources/fabric.mod.json](src/main/resources/fabric.mod.json)：Fabric MOD 元数据。

## 许可证

本项目使用 MIT 许可证，详见 [LICENSE](LICENSE)。
