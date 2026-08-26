# 数据伙伴提交指南 / Data partner PR guide

这份文档写给**数据伙伴方的 AI 助手（Claude）和数据同学**。仓库是公开的
（`Novawerk/berlin-chinese-food-map`），餐厅与折扣数据以 YAML 形式存放在
`data/restaurants/`，我们接受伙伴方以 **fork + Pull Request** 的方式提交改动。

PR 合并到 `main` 后，GitHub Actions 会在几分钟内把 YAML 同步进 Firestore，
App 与落地页随即读到新数据。所以**一条错误数据合并即上线**——下面的规则不是
风格偏好，是流水线的硬约束。

背景阅读（可选）：[`data/README.md`](README.md) 是完整的流水线参考，
[`data/restaurants/_schema.yaml`](restaurants/_schema.yaml) 是字段的权威定义。
本文与它们冲突时，以 `_schema.yaml` 为准。

---

## 0. 硬规则速查（提 PR 前逐条对照）

- [ ] 只修改 `data/restaurants/**` 下的文件，其它路径一律不碰
- [ ] 从 fork 的分支发 PR 到 `main`，不直接推 `main`
- [ ] **永远不要删除餐厅 YAML 文件**——关店用 `hidden: true`（见 §7）
- [ ] **永远不要重命名已存在的文件 / id**（见 §3）
- [ ] 不修改 `data/_tags.yaml`，`tags` 只能用已有的 22 个标签（见 §4）
- [ ] 不填写 `googleData` / `rating` / 营业时间 / 照片 / `visitCount` / `viewCount`——这些由流水线自动生成
- [ ] 不设置 `featured`（这是我们的编辑位）
- [ ] 一个 PR 一个主题；PR 描述里逐条写明**改动内容 + 依据 + 日期**
- [ ] CI 的 **Tag taxonomy** 检查必须是绿的
- [ ] 不提交图片、PDF、zip 等二进制文件

---

## 1. 流水线长什么样（解释上面的规则）

```
data/restaurants/*.yaml  --(合并进 main)-->  GitHub Actions  -->  Firestore  -->  App / 落地页
```

两个关键特性：

1. **同步是 upsert（只增改、不删）。** 删掉一个 YAML 文件**不会**删掉线上的
   Firestore 文档——只会让它变成永远不再更新的孤儿，继续显示在地图上。
   所以下线餐厅一律用 `hidden: true`。
2. **文件名就是数据库主键。** `charlottenburg/ni-hao.yaml` 对应 Firestore 文档
   `ni-hao`。重命名文件 = 新建一条数据 + 留下一个孤儿。

---

## 2. 环境准备

```bash
gh repo fork Novawerk/berlin-chinese-food-map --clone
```

本地校验只需要 Node 20，不需要任何密钥：

```bash
cd scripts/sync-to-firestore && npm ci && node check-tags.mjs
```

同步脚本本身（`node index.js`）需要 Firebase / Google Places 密钥，**伙伴方不需要
也无法运行**，请不要尝试。

---

## 3. 文件位置与命名

```
data/restaurants/{district}/{id}.yaml
```

- `{district}`：小写连字符的区名文件夹（`charlottenburg`、`prenzlauer-berg`…）。
  新增餐厅时选**已存在**的最接近的文件夹；需要新建区文件夹时，在 PR 描述里点名说明。
- `{id}`：小写连字符 slug，**全仓库唯一**，直接成为 Firestore 文档 id。
  惯例是英文名转写，同品牌多店加区名后缀：`wen-cheng-wedding`、`bang-bang-noodles-mitte`。
- id 一旦上线就**冻结**。名字写错、区划分错都不要改文件名——在 PR 描述里指出来，
  由我们处理迁移。

检查 id 是否重复：

```bash
ls data/restaurants/*/*.yaml | xargs -n1 basename | sort | uniq -d
```

---

## 4. 字段速查表

| 字段 | 必填 | 谁负责 | 说明 |
|------|------|--------|------|
| `name.zh` / `name.en` | ✅ | 伙伴 | 至少给一个；`name.de` 可选 |
| `tags` | ✅ | 伙伴 | 1–3 个，**第一个是主标签**；只能取自 `data/_tags.yaml` |
| `address.addressLine1` | ✅ | 伙伴 | 街名 + 门牌号 |
| `address.postalCode` | ✅ | 伙伴 | **必须加引号**（`"10707"`），否则 YAML 会当成数字 |
| `address.district` | ✅ | 伙伴 | 展示用区名，首字母大写：`Wilmersdorf` |
| `address.city` / `country` | — | 默认 | 省略即 Berlin / Germany |
| `latitude` / `longitude` | ✅ | 伙伴 | 十进制度，建议 6–7 位小数 |
| `placeId` | 建议 | 伙伴/我们 | 不确定就留空，见 §8 |
| `hasDiscount` | — | **伙伴** | Pinwo 折扣合作标记，见 §6 |
| `discountInfo` | — | **伙伴** | 优惠一句话文案，见 §6 |
| `hidden` | — | 伙伴 | `true` = 下线，见 §7 |
| `chain.brand` / `chain.branch` | — | 伙伴 | 连锁分店；同品牌 `brand` 必须逐字一致 |
| `phone` / `priceRange` | — | 伙伴 | `priceRange` 只能是 `€` / `€€` / `€€€` |
| `description` | — | 伙伴 | 双语可选；必须是原创或有授权的文字 |
| `logoUrl` / `galleries` | — | 伙伴 | 只放**自有或已授权**的图片直链 |
| `featured` / `editorialNote` | ❌ | 我们 | 编辑精选位，伙伴 PR 不要动 |
| `googleData`、评分、营业时间、照片 | ❌ | 流水线 | 由 Google Places 自动填充，写了也会被覆盖 |
| `visitCount` / `viewCount` | ❌ | 应用 | 运行时计数器，同步时会被忽略 |

**标签**：22 个（10 个菜系 + 12 个业态），权威列表在
[`data/_tags.yaml`](_tags.yaml)。写法是全大写枚举值（`SICHUAN`、`HOTPOT`…）。
惯例是先写菜系标签，再写 1–2 个业态标签。

> 需要一个现有列表里没有的标签？**不要自己加。** 新增标签要同步改 Kotlin 枚举、
> 两份 `strings.xml` 和 admin 类型，只能由我们来做。在 PR 描述或单独 issue 里
> 提出诉求即可，那家餐厅先用最接近的现有标签。

---

## 5. 新增餐厅模板

最小可用（推荐的起点）：

```yaml
name:
  en: Sichuan Folk
  zh: 川渝人家
tags:
  - SICHUAN
  - NOODLES
address:
  addressLine1: Beispielstr. 12
  postalCode: "10115"
  district: Mitte
latitude: 52.5200080
longitude: 13.4049540
```

带折扣与连锁信息的完整示例：

```yaml
name:
  en: Lychee
  zh: 荔枝
  de: Lychee
tags:
  - CANTONESE
  - DIM_SUM
address:
  addressLine1: Bayerische Str. 9
  postalCode: "10707"
  district: Wilmersdorf
latitude: 52.4969271
longitude: 13.3135486
phone: "+49 30 12345678"
priceRange: "€€"
placeId: ChIJM3DeBu9QqEcRInP3c7Ey6TE
chain:
  brand: Lychee
  branch: Wilmersdorf

hasDiscount: true
discountInfo:
  zh: "周周半价"
  en: "Weekly 50% off"
```

---

## 6. 折扣数据（伙伴方的主战场）

```yaml
hasDiscount: true
discountInfo:
  zh: "会员到店赠送一道招牌小菜"
  en: ""        # 可选，没译好就留空字符串
```

- `hasDiscount: true` 会让这家店在地图上使用折扣专属的 marker 样式，
  并在详情页显示优惠卡片。
- `discountInfo.zh` 是优惠卡片上的一行文案。**写常驻优惠，一句话，建议 ≤ 20 个汉字。**
  具体规则、有效期、核销方式放 pinwo.de——详情页的按钮会跳过去。
- `discountInfo` 留空时卡片会退化成通用文案，不会报错，但体验更差。
- **优惠下线**：把 `hasDiscount` 改成 `false`，并**整块删除 `discountInfo`**。
  同步脚本会据此清掉 Firestore 里的旧文案；只改 `hasDiscount` 而留着
  `discountInfo` 会让旧文案继续留在数据库里。

文案示例：

| ✅ 好 | ❌ 不好 |
|------|--------|
| `周周半价` | `每周三四五中午11:30-14:00 凭Pinwo会员码堂食8折，节假日除外，最终解释权归商家所有` |
| `会员赠送招牌小菜` | `优惠详情见小程序` |
| `午市套餐 9 折` | `折扣` |

---

## 7. 关店 / 搬迁 / 改名

**绝不 `git rm`。** 下线的写法是加一行带日期和依据的注释 + `hidden: true`：

```yaml
# Hidden 2026-08-26: confirmed permanently closed (Instagram @xxx marked
# "Permanently closed", verified on site 2026-08-24).
hidden: true
```

- **重新开业**：删掉那行注释，把 `hidden` 改回 `false`（或直接删掉该字段）。
- **搬迁**：保留同一个文件 / id，更新 `address` 和经纬度，**清空 `placeId`**
  （Google 那边通常是新的地点），并在 PR 描述里说明。
- **改名**：只改 `name`，**不要改文件名**。
- **换东家 / 变成另一家店**：不要复用旧文件。旧文件 `hidden: true`，
  新店建新文件，并在 PR 描述里把两者关联起来。

---

## 8. placeId

`placeId` 会让流水线自动补齐评分、营业时间、电话、封面照片，价值很高，但
**填错的代价也很高**（会把别家店的照片和营业时间挂到这家店上）。

- 有把握（在
  [Google 的 Place ID 查找器](https://developers.google.com/maps/documentation/places/web-service/place-id)
  上按名称 + 地址确认过）就填。
- 没把握就**留空**，在 PR 描述里写一句「placeId 待解析」，我们会用带密钥的
  resolver 脚本补上。
- 绝对不要猜、不要从同名的其它分店复制。

---

## 9. YAML 格式规范

- 2 空格缩进，不用 tab；UTF-8 无 BOM；LF 换行；文件末尾留一个换行
- `postalCode` 必须加引号；`phone` 建议加引号（`"+49 …"`）
- 经纬度是裸数字，不加引号
- 行尾不要有多余空格；不要用中文全角标点写字段名
- 字段顺序沿用示例：`name` → `tags` → `address` → 经纬度 → 其它可选字段 →
  `chain` → `placeId` → 折扣块
- 参照同区已有文件的写法，保持一致

---

## 10. 提交前自检

```bash
cd scripts/sync-to-firestore && npm ci && node check-tags.mjs
```

```bash
git status --porcelain -- . ':!data/restaurants'
```

第二条命令**必须没有输出**——有输出说明动到了 `data/restaurants/` 之外的文件
（包括新建的临时文件、脚本、笔记，这些都不要提交）。

---

## 11. PR 规范

- 分支名：`partner/YYYY-MM-DD-<主题>`，例如 `partner/2026-08-26-coupons`
- 标题：`data(partner): <一句话>`，例如
  `data(partner): 8 月折扣更新（新增 5 家，下线 2 家）`
- 规模：一个 PR 建议不超过 ~30 个文件；新增餐厅、折扣更新、关店下线尽量分开提
- 描述模板：

```markdown
## 改动内容
| 文件 | 类型 | 说明 |
|------|------|------|
| `data/restaurants/mitte/xxx.yaml` | 新增 | 2026-08 新店，实地确认 |
| `data/restaurants/wilmersdorf/yyy.yaml` | 折扣 | 合作生效 2026-09-01 |
| `data/restaurants/kreuzberg/zzz.yaml` | 下线 | 2026-08 关店，Google 标注 permanently closed |

## 数据来源
（实地走访 / 商家确认 / Google Maps 核对，注明日期）

## 待我们处理
- [ ] `xxx` 的 placeId 待解析

## 自检
- [ ] 只改了 `data/restaurants/**`
- [ ] 没有删除或重命名任何文件
- [ ] 本地 `node check-tags.mjs` 通过
```

---

## 12. 合并之后

1. CI 的 **Tag taxonomy** 会在 PR 上跑（不需要密钥，fork PR 也能跑）。
2. 我们 review：字段合法性、id 唯一性、坐标是否落在柏林、折扣文案、来源是否可信。
3. 合并进 `main` 后 **Sync Restaurants to Firestore** 自动触发，几分钟内上线。
4. 如果我们在 review 中直接改了你的分支或另开了 commit，请先 `git pull` 再继续。

---

## 13. 会被直接关掉的 PR

- 改动了 `data/restaurants/**` 之外的任何文件（app 代码、CI workflow、
  `_tags.yaml`、脚本、依赖）
- 删除或重命名了餐厅 YAML
- 自己新增了标签枚举值
- 带二进制文件（图片、PDF、zip）
- 批量机器生成、无来源说明的数据

---

## 14. 数据合规

只提交你们**有权分享**的数据。不要照搬 Google Maps、大众点评、小红书等平台的
描述文字或照片；`galleries` / `logoUrl` 只能放自有或已获商家授权的图片链接。
这是一个非营利的开源社区项目，数据会以开源形式公开。

---

有疑问先开 issue（`Novawerk/berlin-chinese-food-map` → Issues），
不要在 PR 里试探性地改流水线。
