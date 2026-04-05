# 柏林中餐地图 | Berlin Chinese Food Map

[English Version](README.md)

一款开源的、社区驱动的柏林中餐馆指南。基于 Kotlin Multiplatform 和 Compose Multiplatform 构建，支持 Android 和 iOS。

**无需登录，纯展示体验。**

**由 [Novawerk](https://github.com/Novawerk) 打造** — 开源应用，用心制作。

## 功能特色

### 双视图模式

- **地图模式**（默认）— 以柏林为中心的交互式 Google 地图，显示餐厅标记。点击标记预览信息，再次点击查看完整详情。
- **列表模式** — 可滚动的餐厅卡片，支持按到访次数或名称排序，带有菜系类型筛选标签。

### 智能搜索与筛选

- **全文搜索** — 按中文、英文或德文餐厅名搜索，300ms 防抖输入
- **菜系类型** — 川菜、粤菜、火锅、烧烤、点心、面食、综合、其他
- **街区** — 按柏林城区筛选（Mitte、Charlottenburg、Neukölln 等）
- **组合筛选** — 叠加多个筛选条件精准定位

### 餐厅详情

- **基本信息** — 地址、电话、价格区间
- **图片画廊** — 可滑动的图片画廊，带页面指示器
- **到访与浏览统计** — 社区驱动的到访次数和浏览量追踪
- **菜系标签** — 菜系类型分类

### 收藏与到访记录

- **收藏夹** — 通过 DataStore 本地保存餐厅，快速访问
- **到访标记** — 标记已去过的餐厅，同步至 Firebase
- **个人美食日记** — 记录你在柏林的中餐探索之旅

### 更多

- **离线支持** — 无网络时也能浏览已缓存的餐厅
- **双语支持** — 完整的中英文 UI，支持德文餐厅名
- **深色模式** — 跟随系统、浅色、深色三种主题可选
- **隐私优先** — 仅匿名认证，无追踪、无广告、完全开源

## 项目结构

这是一个多平台项目，包含三个组件：

| 组件 | 技术 | 目录 |
|------|------|------|
| 移动应用 | Kotlin Multiplatform + Compose | `composeApp/` |
| 官网着陆页 | Next.js 16 + Tailwind CSS 4 | `web/` |
| 管理面板 | React + Vite | `admin/` |

### 应用架构

单模块 (`:composeApp`)，清晰分层：

```
composeApp/src/commonMain/kotlin/com/novawerk/berlinfoodmap/
├── App.kt                          # NavHost + 启动逻辑
├── domain/
│   ├── restaurant/                  # 餐厅模型、仓库接口
│   ├── favorites/                   # 收藏仓库
│   └── search/                      # 搜索/筛选逻辑
├── data/
│   ├── local/                       # DataStore（收藏、设置）
│   └── remote/                      # Firebase Auth + Firestore
├── di/                              # kotlin-inject AppComponent
└── ui/
    ├── theme/                       # Material Design 3（Expressive）
    ├── navigation/                  # 类型安全 @Serializable 路由
    ├── components/                  # RestaurantCard、CuisineChips、EmptyState
    └── pages/
        ├── map/                     # 地图视图 + 餐厅标记
        ├── list/                    # 餐厅列表视图
        ├── detail/                  # 餐厅详情 + 图片画廊
        ├── search/                  # 搜索与多条件筛选
        ├── favorites/               # 收藏的餐厅
        └── settings/                # 主题、语言、关于
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin 2.2 |
| UI | Compose Multiplatform (Material Design 3 Expressive) |
| 导航 | Compose Navigation (类型安全 `@Serializable` 路由) |
| 后端 | Firebase（匿名认证 + Firestore） |
| 本地存储 | Jetpack DataStore |
| 地图 | Google Maps SDK (Android) / MapKit (iOS) |
| 依赖注入 | kotlin-inject (KSP) |
| 网络 | Ktor Client |
| 日期/时间 | kotlinx-datetime |
| 目标平台 | Android (SDK 24+) / iOS |
| 构建 | Gradle + 版本目录 |

## 快速开始

```bash
# 前置条件：JDK 17+、Android SDK

# 构建 Android
./gradlew :composeApp:assembleDebug

# 安装到设备/模拟器
./gradlew :composeApp:installDebug
```

iOS 端请在 Xcode 中打开 `iosApp/` 并正常构建。

着陆页：

```bash
cd web && npm install && npm run dev
```

## 数据来源

餐厅数据由社区贡献，通过 `data/restaurants/` 中的 JSON 文件管理，并同步到 Firebase Firestore。如果你知道柏林有好吃的中餐馆，欢迎提交 Issue 或 PR！

## 参与贡献

欢迎各种形式的贡献！无论是添加餐厅、修复 Bug 还是改进 UI，每一份贡献都很重要。

1. Fork 本仓库
2. 创建你的功能分支 (`git checkout -b feature/amazing-restaurant`)
3. 提交你的更改（我们使用[约定式提交](https://www.conventionalcommits.org/zh-hans/)）
4. 推送到分支
5. 提交 Pull Request

iOS 开发请将 `iosApp/Configuration/Config.xcconfig.template` 复制为 `Config.xcconfig` 并填入你的 Team ID。

## 许可协议

本项目基于 [MIT 许可协议](LICENSE) 开源。

Copyright (c) 2025-2026 [Novawerk](https://github.com/Novawerk)。你可以自由使用、修改和分发本软件，只需保留原始版权声明。
