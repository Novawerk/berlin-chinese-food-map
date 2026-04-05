# 柏林中餐地图 | Berlin Chinese Food Map

[English Version](README.md)

一款开源的、社区驱动的柏林中餐馆指南。基于 Kotlin Multiplatform 和 Compose Multiplatform 构建，支持 Android 和 iOS。

**无需登录，纯展示体验。**

**由 [Novawerk](https://github.com/Novawerk) 打造** — 开源应用，用心制作。

## 功能特色

### 双视图模式

- **地图模式**（默认）— 交互式地图，餐厅标记按菜系类型着色。点击标记预览信息，再次点击查看完整详情。
- **列表模式** — 可滚动的餐厅卡片，支持按距离、评分或名称排序。地图与列表之间可无缝切换。

### 智能筛选

支持多条件组合筛选：

| 筛选条件 | 说明 |
|---------|------|
| 名称 | 按餐厅名搜索（支持中英文） |
| 菜系类型 | 川菜、粤菜、火锅、烧烤、点心、面食等 |
| 街区 | Mitte、Charlottenburg、Prenzlauer Berg、Neukölln 等 |
| 距离 | 按与当前位置的距离排序或筛选 |

### 餐厅详情

- **基本信息** — 地址、电话、营业时间、价格区间
- **菜系与标签** — 菜系风格、饮食选项、特色菜
- **菜品菜单** — 推荐菜品，含照片、描述和价格
- **照片展示** — 餐厅内外环境及菜品照片
- **社区评价** — 来自柏林华人社区的评分和评论

### 更多

- **收藏夹** — 保存常去的餐厅，快速访问（本地存储）
- **离线支持** — 无网络时也能浏览已缓存的餐厅
- **双语支持** — 完整的中文和英文支持
- **隐私优先** — 无需登录、无追踪、无广告、完全开源

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
│   ├── restaurant/                  # 餐厅与菜品模型、仓库
│   ├── review/                      # 评论模型
│   └── search/                      # 搜索/筛选逻辑
├── data/
│   ├── local/                       # 本地数据库、收藏
│   └── remote/                      # API 客户端
└── ui/
    ├── theme/                       # M3 主题
    ├── navigation/                  # 类型安全路由
    ├── components/                  # 共享组件（筛选栏、卡片）
    └── pages/
        ├── map/                     # 地图视图 + 餐厅标记
        ├── list/                    # 餐厅列表视图
        ├── detail/                  # 餐厅详情 + 菜品菜单
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
