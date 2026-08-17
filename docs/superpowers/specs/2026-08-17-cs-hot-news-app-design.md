# CS 热点推送 App — 设计文档

日期：2026-08-17
目标用户：个人自用（小白向，用户全程只做 GitHub 账号 + API Key + 下载安装）

## 目标

一个 Android App，定时推送计算机领域实时热点，每条热点附带 AI 生成的一句话解读和要点。

## 架构

三个部件，全部基于一个 GitHub 仓库：

```
┌──────────────────────────────────────────────────────────────┐
│ GitHub 仓库（所有者在 GitHub 上）                              │
│                                                              │
│  GitHub Actions workflow "scrape"（每 30 分钟）              │
│    ├─ Python 脚本抓取 5 个预置源                              │
│    ├─ 清洗、去重（按全局 item id）                            │
│    ├─ 对新条目调 LLM API 生成 summary + key_points           │
│    └─ 提交最新热点到 data/hot.json                            │
│                                                              │
│  GitHub Actions workflow "build-apk"（代码更新时触发）        │
│    └─ 用 Gradle 构建 release APK，发布到 Releases             │
│                                                              │
│  GitHub Pages：托管 data/hot.json → https://USER.github.io/REPO/hot.json │
└──────────────────────────────────────────────────────────────┘
                            ▲ 轮询(15~30min)
                            │
┌───────────────────────────┴──────────────────────────────────┐
│ Android App（Kotlin + Jetpack Compose）                       │
│  ├─ 热点列表页：5 源合并，按热度/时间排序                      │
│  ├─ 详情页：标题、原文链接、AI 解读、要点                      │
│  └─ 设置页：后端地址、通知开关                                 │
│  └─ WorkManager 定时轮询 hot.json，新 id 弹系统通知            │
└──────────────────────────────────────────────────────────────┘
```

## 数据模型

每个热点条目（data/hot.json 中数组元素）：

```json
{
  "id": "hackernews:41234567",
  "title": "标题",
  "url": "原文链接",
  "source": "hackernews",
  "score": 82,
  "published_at": "2026-08-17T10:00:00Z",
  "summary": "AI 一句话解读",
  "key_points": ["要点1", "要点2", "要点3"]
}
```

## 预置源（5 个适配器，统一输出 Item）

| source | 数据来源 |
|---|---|
| hackernews | HN 官方 topstories API |
| github-trending | GitHub Trending 页面解析 |
| zhihu | 知乎热榜 API |
| juejin | 掘金热榜 API |
| v2ex | V2EX API |

## 后端流程（scrape workflow）

1. `python scrape.py` 依次抓 5 个源，单个源失败不影响其余。
2. 读上次提交的 `data/hot.json`，用 `id` 去重。
3. 仅对"新出现的条目"调 LLM（API Key 存 GitHub Actions secret），生成 summary + key_points。
4. 合并写入 `data/hot.json`（保留最近 500 条），commit + push，Pages 自动更新。

LLM 接口：OpenAI 兼容格式，base_url + api_key + model 均为可配置输入（默认 DeepSeek）。

## 客户端设计

- 语言/框架：Kotlin + Jetpack Compose，单模块、单 Activity，最少依赖。
- 网络：直接用 `java.net.HttpURLConnection` 或最小 HTTP 封装（避免引 OkHttp/Retrofit 全家桶，若引则只引一个）。拉取 `hot.json` 解析 JSON。
- 缓存：本地 `SharedPreferences` 存 JSON 快照 + 上次已通知的 id 集合。
- 轮询：`WorkManager` 周期任务（默认 20 分钟，设置页可调 15/30/60）。
- 通知：`NotificationChannel` + 点击通知跳详情（scheme 深链或直接定位条目）。
- 页面：列表（LazyColumn）、详情（可滚动，按钮"在浏览器打开原文"）、设置（后端 URL、刷新间隔、通知开关）。

## 错误处理

| 故障 | 处理 |
|---|---|
| 单个源抓取失败 | 跳过该源，其余正常；脚本输出该源失败日志 |
| LLM 调用失败/超时 | 该条不带解读（summary 置空），下次抓取不重试 |
| 手机断网 | 展示本地缓存；WorkManager 网络恢复自动补拉 |
| Pages 未及时更新 | 轮询失败静默重试，不打扰用户 |

## 测试（最小可运行检查）

- 后端：`scrape.py` 内一个 `assert` 版 `demo()` 自检（去重逻辑），可用 `python scrape.py --selftest` 运行。
- 客户端：核心解析逻辑一个 `test`（纯 JVM 单元测试），UI 不做自动化测试。
- 验收：构建 APK 手动安装 → 首次打开看到热点 → 出现新条目时通知栏弹出。

## 明确的裁剪（YAGNI）

- 自定义内容源 → 砍掉，5 个预置源覆盖主要需求，真需要时后端加一个适配器即可。
- 收藏/点赞/评论、多分类订阅、搜索、多语言 → 全部砍掉，不是核心诉求。
- 多用户/鉴权 → 个人自用，不需要。
- 真正的实时推送（FCM/第三方 SDK）→ 国内不可靠或要注册，用 15~30 分钟轮询足够。

## 上线物料清单（用户侧）

1. GitHub 账号
2. LLM API Key（如 DeepSeek，放仓库 secret：`LLM_API_KEY`）
3. 手机开启"允许安装未知来源应用"
4. 从 GitHub Releases 下载 APK
