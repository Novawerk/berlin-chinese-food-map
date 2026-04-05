# 柏林中餐地图 | Berlin Chinese Food Map

[English Version](README.md)

一款开源的、社区驱动的柏林中餐馆指南。基于 Kotlin Multiplatform 和 Compose Multiplatform 构建，支持 Android 和 iOS。

**由 [Novawerk](https://github.com/Novawerk) 打造** — 开源应用，用心制作。

## 功能特色

- **餐厅地图** — 交互式地图，展示柏林各区的中餐馆
- **餐厅详情** — 详细信息：菜系类型、价格区间、营业时间、照片和评价
- **社区评价** — 来自柏林华人社区的评分和评论
- **搜索与筛选** — 按菜系（川菜、粤菜、火锅、烧烤等）、区域、价格筛选
- **收藏夹** — 保存常去的餐厅，快速访问
- **离线支持** — 无网络时也能浏览已保存的餐厅
- **双语支持** — 完整的中文和英文支持
- **隐私优先** — 无追踪、无广告、完全开源

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin 2.2 |
| UI | Compose Multiplatform (Material Design 3) |
| 导航 | Compose Navigation (类型安全路由) |
| 存储 | Jetpack DataStore / Room |
| 地图 | Google Maps SDK / MapKit |
| 依赖注入 | kotlin-inject (KSP) |
| 网络 | Ktor Client |
| 目标平台 | Android / iOS |
| 构建 | Gradle + 版本目录 |

## 架构

单模块 (`:composeApp`)，清晰分层：

```
composeApp/src/commonMain/kotlin/com/novawerk/berlinfoodmap/
├── App.kt                          # NavHost + 启动逻辑
├── domain/
│   ├── restaurant/                  # 餐厅模型、仓库
│   ├── review/                      # 评论模型
│   └── search/                      # 搜索/筛选逻辑
├── data/
│   ├── local/                       # 本地数据库、收藏
│   └── remote/                      # API 客户端
└── ui/
    ├── theme/                       # M3 主题
    ├── navigation/                  # 类型安全路由
    ├── components/                  # 共享组件
    └── pages/
        ├── map/                     # 地图视图 + 餐厅标记
        ├── list/                    # 餐厅列表视图
        ├── detail/                  # 餐厅详情页
        ├── search/                  # 搜索与筛选
        ├── favorites/               # 收藏的餐厅
        └── settings/                # 语言、关于
```

## 快速开始

```bash
# 前置条件：JDK 17+、Android SDK

# 构建 Android
./gradlew :composeApp:assembleDebug

# 安装到设备/模拟器
./gradlew :composeApp:installDebug
```

iOS 端请在 Xcode 中打开 `iosApp/` 并正常构建。

## 数据来源

餐厅数据由社区贡献。如果你知道柏林有好吃的中餐馆，欢迎提交 Issue 或 PR！

## 参与贡献

欢迎各种形式的贡献！无论是添加餐厅、修复 Bug 还是改进 UI，每一份贡献都很重要。

1. Fork 本仓库
2. 创建你的功能分支 (`git checkout -b feature/amazing-restaurant`)
3. 提交你的更改
4. 推送到分支
5. 提交 Pull Request

iOS 开发请将 `iosApp/Configuration/Config.xcconfig.template` 复制为 `Config.xcconfig` 并填入你的 Team ID。

## 许可协议

本项目基于 [MIT 许可协议](LICENSE) 开源。

Copyright (c) 2025-2026 [Novawerk](https://github.com/Novawerk)。你可以自由使用、修改和分发本软件，只需保留原始版权声明。
