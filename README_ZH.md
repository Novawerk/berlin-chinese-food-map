# 柏林中餐地图 | Berlin Chinese Food Map

[English Version](README.md) | [项目协作提案](PROPOSAL.md)

一款社区驱动的非营利性质柏林中餐馆数字指南。基于 Kotlin Multiplatform 和 Compose Multiplatform 构建，支持 Android 和 iOS。

**无需登录。隐私优先。完全开源。**

**由 [Novawerk](https://github.com/Novawerk) 打造** — 开源应用，用心制作。

## 项目状态

**POC 已完成** — 原型验证已在四个核心方向全部通过：

| 组件 | 状态 | 验证内容 |
|------|------|---------|
| 移动端应用 (iOS + Android) | POC 就绪 | 跨平台地图与搜索逻辑、自定义 Marker、谷歌地图合规性检查 |
| 数据流水线 (GitHub 同步) | 自动化 | YAML → Firestore 自动同步与部署 |
| 项目落地页 | 已上线 | 双语 UI 框架及功能展示 |
| 管理后台（控制中心） | 正常运行 | 餐厅数据增删改查 (CRUD) 全套管理功能 |

## 实施路线图

| 阶段 | 描述 | 状态 |
|------|------|------|
| 第一阶段：Kick-off | 数据交接、视觉方向对齐、Schema 与交互逻辑敲定 | 即将开始 |
| 第二阶段：MVP 核心开发（第 1 周） | 精致地图 UI、餐厅画像、三语搜索、收藏与足迹、内部测试 | 即将开始 |
| 第三阶段：内测与发布筹备（第 2 周） | 社区内测（微信、小红书）、ASO 准备、GTM 协同 | 即将开始 |
| 第四阶段：正式发布与增长 | App Store + Play Store 上架、反馈迭代、UGC 机制、精品策划 | 即将开始 |

**Kick-off 目标：2026年4月14日当周**

## 核心交付产物

- **移动端原生应用** (iOS + Android) — 基于 Kotlin Multiplatform 开发的高性能跨平台原生应用
- **落地页** (https://berlinfoodmap.novawerk.io/) — 经过 SEO 优化的双语落地页，用于产品展示及应用分发
- **控制中心** — 为非技术背景团队成员设计的用户友好型管理后台
- **数据流水线** — 基于 GitHub 的工作流，支持社区贡献内容的自动校验与同步

## 功能特色

### 双视图模式

- **地图模式**（默认）— 以柏林为中心的交互式 Google 地图，显示按菜系类型着色的餐厅标记。点击标记预览信息，再次点击查看完整详情。
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

这是一个多平台项目，包含四个组件：

| 组件 | 技术 | 目录 |
|------|------|------|
| 移动应用 | Kotlin Multiplatform + Compose | `composeApp/` |
| 落地页 | Next.js + Tailwind CSS | `web-apps/landing-page/` |
| 管理面板 | React + Vite | `web-apps/admin/` |
| 数据流水线 | YAML + GitHub CI → Firestore | `data/` |

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

### 数据流水线

餐厅数据由社区通过 YAML 文件贡献：

```
data/restaurants/{district}/{restaurant-id}.yaml
```

| 字段 | 属性 | 备注 |
|------|------|------|
| 名称 (中/英) | 必填 | 例如：川味坊 / Sichuan Folk |
| 菜系分类 | 必填 | 细化分类（如：火锅、烧烤、粤菜等） |
| 街道地址 | 必填 | 包含街名和门牌号 |
| 地理坐标 | 建议提供 | GPS 经纬度（确保地图标记精准） |
| 视觉素材 | 能有最好 | 1-3 张高质量照片（环境或招牌菜） |

数据可通过 CSV、Excel 或 Google Sheets 形式交付。开发团队负责转换为 YAML 格式。CI 自动校验 Schema 并在合并时同步到 Firestore。

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

落地页：

```bash
cd web-apps/landing-page && npm install && npm run dev
```

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
