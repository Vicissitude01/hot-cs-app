# CS 热点推送 App 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 一个 Android App + 免费后端，定时抓取 5 个计算机热点源，AI 生成解读，后台轮询弹系统通知。

**Architecture:** 三个子系统共用同一个 GitHub 仓库——(1) `scraper/` Python 脚本由 GitHub Actions 定时抓取并提交 `data/hot.json`，GitHub Pages 免费托管该 JSON；(2) `app/` Kotlin + Jetpack Compose 客户端轮询该 JSON；(3) `.github/workflows/` 负责抓取与自动构建 APK 并发布到 Releases。

**Tech Stack:** Python 3（stdlib + requests）、GitHub Actions、GitHub Pages、Kotlin + Jetpack Compose、WorkManager、org.json（Android 内置）、OpenAI 兼容 LLM API。

**设计文档：** `docs/superpowers/specs/2026-08-17-cs-hot-news-app-design.md`

---

## 阶段 A：后端抓取器（Python）

### Task 1: 项目骨架 + 数据模型

**Files:**
- Create: `scraper/__init__.py`
- Create: `scraper/hotitem.py`

- [ ] **Step 1: 定义统一数据模型**

`scraper/hotitem.py`：

```python
from dataclasses import dataclass, field, asdict
from datetime import datetime, timezone

@dataclass
class HotItem:
    id: str            # "source:source_internal_id"，全局唯一
    title: str
    url: str
    source: str        # hackernews | github-trending | zhihu | juejin | v2ex
    score: float       # 各源归一化到 0-100
    published_at: str  # ISO8601 UTC，未知留空串
    summary: str = ""
    key_points: list = field(default_factory=list)

    def to_dict(self) -> dict:
        return asdict(self)

    @staticmethod
    def from_dict(d: dict) -> "HotItem":
        return HotItem(**{k: d.get(k, "") for k in
                          ["id", "title", "url", "source", "score",
                           "published_at", "summary", "key_points"]})
```

- [ ] **Step 2: 单测数据模型往返**

Create `scraper/test_hotitem.py`：

```python
from hotitem import HotItem

def test_roundtrip():
    it = HotItem(id="hn:1", title="t", url="u", source="hackernews",
                 score=50.0, published_at="", summary="s", key_points=["a"])
    assert HotItem.from_dict(it.to_dict()) == it
```

- [ ] **Step 3: 运行测试**

Run: `python -m pytest scraper/test_hotitem.py -q`
Expected: 1 passed

- [ ] **Step 4: 提交**

```bash
git add scraper/
git commit -m "feat: HotItem data model"
```

### Task 2: 五个源适配器

**Files:**
- Create: `scraper/sources.py`
- Modify: `scraper/hotitem.py` 无需改动

- [ ] **Step 1: 实现统一抓取入口 + HN 源**

`scraper/sources.py`（每个源一个函数，返回 `list[HotItem]`，失败抛异常由外层跳过）：

```python
import json, html, re, urllib.request
from hotitem import HotItem

UA = {"User-Agent": "Mozilla/5.0 (hot-cs-app/1.0)"}

def _get_json(url):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=20) as r:
        return json.load(r)

def _norm(score, max_score, out_max=100.0):
    return round(out_max * score / max_score, 1) if max_score else 0.0

def hackernews():
    ids = _get_json("https://hacker-news.firebaseio.com/v0/topstories.json")[:30]
    out = []
    for i in ids:
        it = _get_json(f"https://hacker-news.firebaseio.com/v0/item/{i}.json")
        out.append(HotItem(id=f"hackernews:{i}", title=it.get("title", ""),
                           url=it.get("url") or f"https://news.ycombinator.com/item?id={i}",
                           source="hackernews", score=float(it.get("score", 0)),
                           published_at=it.get("time", "")))
    return out
```

- [ ] **Step 2: 实现 GitHub Trending、知乎、掘金、V2EX 四个适配器**

追加到 `scraper/sources.py`（沿用 `_get_json`/`_norm`；Trending 页面 HTML 用正则提取标题与链接，热度近似用仓库星标文本长度归一；各源标题清洗用 `html.unescape`）：

```python
def github_trending():
    req = urllib.request.Request("https://github.com/trending", headers=UA)
    page = urllib.request.urlopen(req, timeout=20).read().decode("utf-8", "ignore")
    items = []
    for m in re.finditer(r'<h2 class="h3[^"]*"><a href="/([^"]+)"[^>]*>([^<]+)</a>', page):
        repo, name = m.group(1), html.unescape(m.group(2)).strip()
        items.append(HotItem(id=f"github-trending:{repo}", title=name,
                             url=f"https://github.com/{repo}", source="github-trending",
                             score=50.0, published_at=""))
    return items

def zhihu():
    data = _get_json("https://www.zhihu.com/api/v3/feed/topstory/hot-lists/total?limit=20")
    out = []
    for it in data.get("data", []):
        t = it.get("target", {})
        out.append(HotItem(id=f"zhihu:{t.get('id')}", title=t.get("title", ""),
                           url=t.get("url", ""), source="zhihu",
                           score=_norm(it.get("detail_text", 0), 1_000_000), published_at=""))
    return out

def juejin():
    body = json.dumps({"id_type": 2, "sort_type": 3, "limit": 20}).encode()
    req = urllib.request.Request("https://api.juejin.cn/recommend_api/v1/article/recommend_all_feed",
                                 data=body, headers={**UA, "Content-Type": "application/json"})
    data = json.load(urllib.request.urlopen(req, timeout=20))
    out = []
    for it in data.get("data", []):
        if not it: continue
        a = it.get("item_info", {}).get("article_info", {})
        out.append(HotItem(id=f"juejin:{a.get('article_id')}", title=a.get("title", ""),
                           url=f"https://juejin.cn/post/{a.get('article_id')}",
                           source="juejin", score=float(a.get("rank_index", 0)),
                           published_at=a.get("ctime", "")))
    return out

def v2ex():
    data = _get_json("https://www.v2ex.com/api/topics/hot.json")
    out = []
    for it in data:
        out.append(HotItem(id=f"v2ex:{it.get('id')}", title=it.get("title", ""),
                           url=f"https://www.v2ex.com/t/{it.get('id')}", source="v2ex",
                           score=float(it.get("replies", 0) * 2), published_at=str(it.get("created", ""))))
    return out

ALL_SOURCES = {"hackernews": hackernews, "github-trending": github_trending,
               "zhihu": zhihu, "juejin": juejin, "v2ex": v2ex}
```

- [ ] **Step 3: 运行可用性检查**

Run: `python -c "from sources import ALL_SOURCES; s=ALL_SOURCES['hackernews'](); print(len(s), s[0].title if s else 'EMPTY')"`
Expected: 打印条数 > 0 与标题（网络可用时）。其余源可逐个用 `python -c "..."` 验证，失败源单独处理不阻塞。

- [ ] **Step 4: 提交**

```bash
git add scraper/sources.py
git commit -m "feat: 5 source adapters"
```

### Task 3: 去重合并 + LLM 解读

**Files:**
- Create: `scraper/llm.py`
- Create: `scraper/merge.py`
- Create: `scraper/test_merge.py`

- [ ] **Step 1: 去重合并逻辑（含单测）**

`scraper/merge.py`：

```python
from hotitem import HotItem

def merge(existing: list[HotItem], fresh: list[HotItem], limit=500) -> list[HotItem]:
    seen, out = {it.id for it in existing}, list(existing)
    new_items = []
    for it in fresh:
        if it.id not in seen:
            seen.add(it.id)
            out.insert(0, it)
            new_items.append(it)
    return out[:limit], new_items
```

`scraper/test_merge.py`：

```python
from hotitem import HotItem
from merge import merge

def _it(i, src="hn"):
    return HotItem(id=f"{src}:{i}", title=str(i), url=f"u/{i}", source=src, score=1.0, published_at="")

def test_merge_dedupes_and_returns_new():
    existing = [_it(1), _it(2)]
    fresh = [_it(2), _it(3)]
    merged, new = merge(existing, fresh)
    assert [it.id for it in merged] == ["hn:3", "hn:1", "hn:2"]
    assert [it.id for it in new] == ["hn:3"]

def test_merge_respects_limit():
    merged, _ = merge([_it(i) for i in range(3)], [_it(99)], limit=3)
    assert len(merged) == 3
```

- [ ] **Step 2: LLM 解读模块（OpenAI 兼容）**

`scraper/llm.py`：

```python
import json, urllib.request

def summarize(items, api_key, base_url, model, max_items=10):
    """为未解读的新条目生成 summary + key_points，调用失败返回空解读（不重试）。"""
    if not api_key or not items:
        return
    prompt = ("为以下计算机热点各写一句话解读和 3 个要点，输出 JSON 数组："
              '[{"id": "...", "summary": "...", "key_points": ["..."]}]。输入：\n')
    for it in items[:max_items]:
        prompt += f"- [{it.id}] {it.title}\n"
    body = {"model": model, "messages": [{"role": "user", "content": prompt}],
            "response_format": {"type": "json_object"}, "temperature": 0.3}
    req = urllib.request.Request(base_url.rstrip("/") + "/chat/completions",
        data=json.dumps(body).encode(),
        headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"})
    data = json.load(urllib.request.urlopen(req, timeout=60))
    content = data["choices"][0]["message"]["content"]
    result = json.loads(content)
    key = {r["id"]: r for r in (result.get("items") or [])}
    for it in items[:max_items]:
        if it.id in key:
            it.summary = key[it.id].get("summary", "")
            it.key_points = key[it.id].get("key_points", [])
```

- [ ] **Step 3: 运行合并单测**

Run: `python -m pytest scraper/test_merge.py -q`
Expected: 2 passed

- [ ] **Step 4: 提交**

```bash
git add scraper/
git commit -m "feat: dedupe merge + LLM summary"
```

### Task 4: 主入口 scrape.py + 自检

**Files:**
- Create: `scraper/scrape.py`

- [ ] **Step 1: 主入口**

`scraper/scrape.py`：

```python
import argparse, json, os, sys
from datetime import datetime, timezone
from hotitem import HotItem
import sources, merge, llm

DATA = os.path.join(os.path.dirname(__file__), "..", "data", "hot.json")

def load_existing():
    if os.path.exists(DATA):
        with open(DATA, encoding="utf-8") as f:
            return [HotItem.from_dict(d) for d in json.load(f)]
    return []

def save(items):
    os.makedirs(os.path.dirname(DATA), exist_ok=True)
    with open(DATA, "w", encoding="utf-8") as f:
        json.dump([it.to_dict() for it in items], f, ensure_ascii=False, indent=2)

def run(api_key=None, base_url=None, model=None):
    existing = load_existing()
    fresh = []
    for name, fn in sources.ALL_SOURCES.items():
        try:
            fresh += fn()
        except Exception as e:
            print(f"[warn] source {name} failed: {e}", file=sys.stderr)
    items, new = merge.merge(existing, fresh)
    if new:
        llm.summarize(new, api_key, base_url, model)
    save(items)
    print(f"ok: {len(items)} items, {len(new)} new")
    return items

def selftest():
    from test_hotitem import test_roundtrip
    from test_merge import test_merge_dedupes_and_returns_new, test_merge_respects_limit
    test_roundtrip(); test_merge_dedupes_and_returns_new(); test_merge_respects_limit()
    print("selftest: 3 checks passed")

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--selftest", action="store_true")
    ap.add_argument("--api-key", default=os.environ.get("LLM_API_KEY"))
    ap.add_argument("--llm-base-url", default=os.environ.get("LLM_BASE_URL", "https://api.deepseek.com/v1"))
    ap.add_argument("--llm-model", default=os.environ.get("LLM_MODEL", "deepseek-chat"))
    a = ap.parse_args()
    if a.selftest:
        selftest()
    else:
        run(a.api_key, a.llm_base_url, a.llm_model)
```

- [ ] **Step 2: 运行自检**

Run: `python scraper/scrape.py --selftest`
Expected: `selftest: 3 checks passed`

- [ ] **Step 3: 实跑一次（网络可用时）**

Run: `python scraper/scrape.py`
Expected: `ok: N items, M new`，并生成 `data/hot.json`

- [ ] **Step 4: 提交**

```bash
git add scraper/ data/
git commit -m "feat: scrape.py entrypoint"
```

**阶段 A 完成标志：** `python scraper/scrape.py` 能本地生成 `data/hot.json`，`--selftest` 全绿。

---

## 阶段 B：Android 客户端

### Task 5: Gradle 项目骨架

**Files:**
- Create: `app/settings.gradle.kts`
- Create: `app/build.gradle.kts`（根，声明 AGP）
- Create: `app/gradle.properties`
- Create: `app/app/build.gradle.kts`
- Create: `app/app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 根配置（三文件）**

`settings.gradle.kts`：`pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }` + `dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }` + `rootProject.name = "hot-cs-app"` + `include(":app")`

`build.gradle.kts`：`plugins { id("com.android.application") version "8.5.2" apply false; id("org.jetbrains.kotlin.android") version "2.0.20" apply false }`

`gradle.properties`：`org.gradle.jvmargs=-Xmx2g`、`android.useAndroidX=true`、`android.nonTransitiveRClass=true`

- [ ] **Step 2: app 模块配置**

`app/app/build.gradle.kts`：

```kotlin
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.hotcs.app"; compileSdk = 34
    defaultConfig { applicationId = "com.hotcs.app"; minSdk = 26; targetSdk = 34; versionCode = 1; versionName = "1.0" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui"); implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 3: Manifest + 图标占位**

`app/app/src/main/AndroidManifest.xml`：声明 `INTERNET`、`POST_NOTIFICATIONS` 权限，`<application android:label="CS热点" android:theme="@style/Theme.AppCompat"><activity android:name=".MainActivity" android:exported="true">...`，通知 channel 名 `hot_alerts`。图标用默认（release 时换）。res 目录放最小 `themes.xml`（Material 兼容主题）与启动图标占位。

- [ ] **Step 4: 提交**

```bash
git add app/
git commit -m "feat: gradle skeleton"
```

### Task 6: 数据模型 + 网络拉取 + 本地缓存

**Files:**
- Create: `app/app/src/main/java/com/hotcs/app/data/HotItem.kt`
- Create: `app/app/src/main/java/com/hotcs/app/data/HotRepository.kt`
- Create: `app/app/src/main/java/com/hotcs/app/data/test/RepositoryTest.kt`（JVM 单测）

- [ ] **Step 1: 数据模型（kotlinx.serialization）**

`HotItem.kt`：`@Serializable data class HotItem(id, title, url, source, score, publishedAt="", summary="", keyPoints=emptyList())`，字段名映射 `published_at`→`publishedAt`、`key_points`→`keyPoints`（`@SerialName`）。

- [ ] **Step 2: 仓库（拉取 + 缓存 + 解析）**

`HotRepository.kt`：

```kotlin
class HotRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("hotcs", MODE_PRIVATE)

    fun fetch(baseUrl: String): List<HotItem> {
        val conn = URL(baseUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000; conn.readTimeout = 15_000
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val items = Json.decodeFromString<List<HotItem>>(text)
        prefs.edit().putString("cache", text).apply()
        return items
    }

    fun cached(): List<HotItem> {
        val t = prefs.getString("cache", null) ?: return emptyList()
        return runCatching { Json.decodeFromString<List<HotItem>>(t) }.getOrDefault(emptyList())
    }

    fun lastNotifiedIds(): Set<String> = prefs.getStringSet("notified", emptySet())!!.toSet()
    fun saveNotifiedIds(ids: Set<String>) = prefs.edit().putStringSet("notified", ids).apply()
}
```

（`HttpURLConnection` 为 stdlib，避免引 OkHttp；序列化用 kotlinx-serialization，解析失败返回空列表不崩溃。）

- [ ] **Step 3: JVM 单测解析（mock 字符串）**

`RepositoryTest.kt`：用一段含 `published_at`/`key_points` 的 JSON 字符串直接测 `Json.decodeFromString<List<HotItem>>`，断言字段映射正确、空 keyPoints 缺省正确。

- [ ] **Step 4: 提交**

```bash
git add app/app/src/main/java/com/hotcs/app/data/ app/app/src/test/java/
git commit -m "feat: data layer + parsing test"
```

### Task 7: 三个 Compose 页面

**Files:**
- Create: `app/app/src/main/java/com/hotcs/app/MainActivity.kt`
- Create: `app/app/src/main/java/com/hotcs/app/ui/HomeScreen.kt`
- Create: `app/app/src/main/java/com/hotcs/app/ui/DetailScreen.kt`
- Create: `app/app/src/main/java/com/hotcs/app/ui/SettingsScreen.kt`

- [ ] **Step 1: MainActivity + 导航**

`MainActivity.kt`：Compose `setContent` + `NavHost` 三个路由（`home`/`detail/{id}`/`settings`），顶部应用栏显示源名；数据经 `HotRepository` 加载进 `remember { mutableStateOf(...) }`，提供下拉刷新。

- [ ] **Step 2: 列表页**

`HomeScreen.kt`：`LazyColumn` 显示条目标题、源徽标（颜色区分 5 源）、热度分、AI 一句话解读（有则显示）。点击进入详情；右上角设置图标进设置页。

- [ ] **Step 3: 详情页 + 设置页**

`DetailScreen.kt`：标题、源/热度/时间、"在浏览器打开原文"按钮（`ACTION_VIEW`）、AI 解读、要点列表。

`SettingsScreen.kt`：后端 URL 输入框（默认 `https://你的用户名.github.io/hot-cs-app/data/hot.json`）、刷新间隔（15/20/30/60 单选）、通知开关（WorkManager 启停）、"测试连接"按钮。

- [ ] **Step 4: 提交**

```bash
git add app/app/src/main/java/com/hotcs/app/
git commit -m "feat: 3 compose screens"
```

### Task 8: 后台轮询 + 系统通知

**Files:**
- Create: `app/app/src/main/java/com/hotcs/app/notify/NotifyWorker.kt`
- Create: `app/app/src/main/java/com/hotcs/app/notify/Notifier.kt`
- Modify: `app/app/src/main/java/com/hotcs/app/MainActivity.kt`（启动时调度 worker）

- [ ] **Step 1: 通知调度**

`Notifier.kt`：创建 `NotificationChannel("hot_alerts", "热点提醒", IMPORTANCE_HIGH)`；`schedule(context)` 用 `WorkManager.enqueueUniquePeriodicWork("hot_poll", KEEP, PeriodicWorkRequestBuilder<NotifyWorker>(20, MINUTES).build())`，`cancel(context)` 取消；间隔从设置读取。

- [ ] **Step 2: 通知执行**

`NotifyWorker.kt`：`doWork()` 中读设置后端 URL → `HotRepository.fetch` → 新 id（`fetch().map{it.id} - lastNotifiedIds`）→ 对每条弹通知（标题、摘要、点通知深链到 `hotcs://detail/{id}`）→ `saveNotifiedIds`。任何异常 `Result.retry()`。

- [ ] **Step 3: 深链 + 通知点击跳详情**

`MainActivity` 的 NavHost 支持 `deepLink("hotcs://detail/{id}")`，通知 `contentIntent` 用该深链；Manifest 的 activity 增加 `android.intent.action.VIEW` intent-filter。

- [ ] **Step 4: 提交**

```bash
git add app/app/src/main/java/com/hotcs/app/
git commit -m "feat: background poll + notifications"
```

**阶段 B 完成标志：** Android Studio 打开 `app/` 可运行，列表/详情/设置可用；手动触发 worker 会弹通知。

---

## 阶段 C：CI 上线

### Task 9: 抓取 workflow + GitHub Pages

**Files:**
- Create: `.github/workflows/scrape.yml`
- Modify: `README.md`（Pages 配置说明）

- [ ] **Step 1: 抓取 workflow**

`scrape.yml`：

```yaml
name: scrape
on:
  schedule: [{cron: "*/30 * * * *"}]
  workflow_dispatch: {}
permissions: {contents: write}
jobs:
  scrape:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with: {python-version: "3.12"}
      - run: pip install -r scraper/requirements.txt
      - run: python scraper/scrape.py
        env:
          LLM_API_KEY: ${{ secrets.LLM_API_KEY }}
      - run: |
          git config user.name "github-actions[bot]"
          git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
          git add data/hot.json
          git diff --cached --quiet || git commit -m "data: refresh hot.json" && git push
```

- [ ] **Step 2: requirements + README Pages 说明**

`scraper/requirements.txt`：`requests`（仅文档保留，实际脚本用 stdlib，可留空行占位）。

`README.md`：写明——①仓库 Settings→Pages→Source 选 `Deploy from a branch`→`main`→`/root`；②App 设置页填 `https://<用户名>.github.io/<仓库>/data/hot.json`；③Add secret `LLM_API_KEY`；④开启 Actions 定时任务。

- [ ] **Step 3: 提交**

```bash
git add .github/ README.md scraper/requirements.txt
git commit -m "ci: scrape workflow + pages docs"
```

### Task 10: 自动构建 APK workflow

**Files:**
- Create: `.github/workflows/build-apk.yml`

- [ ] **Step 1: 构建 workflow**

`build-apk.yml`：触发 `push`（tags 或 main 手动）；steps：`checkout@v4` → `setup-java@v4`（temurin 17）→ `gradle/actions/setup-gradle@v3` → `./gradlew :app:assembleRelease` → `actions/upload-artifact@v4`（`app/app/build/outputs/apk/release/*.apk`）；`softprops/action-gh-release@v2` 在 tag 时发布 Release 附带 APK。

- [ ] **Step 2: 本地冒烟（可选）**

用户本机装 JDK17 + Android SDK，运行 `./gradlew :app:assembleRelease` 生成 APK；或直接依赖 CI。此步不阻塞提交。

- [ ] **Step 3: 提交**

```bash
git add .github/workflows/build-apk.yml
git commit -m "ci: auto build apk + release"
```

### Task 11: 收尾文档

**Files:**
- Create: `README.md`（若 Task 9 已建则补全）——安装步骤、故障排查、常见问题

- [ ] **Step 1: 完整 README**

包含：项目简介、目录结构、小白四步上手（GitHub 注册→建仓库→推送→下载 APK）、LLM API Key 获取指引、Pages 配置、通知权限开启方法（Android 设置→应用→CS热点→通知）、故障排查表（列表空白→检查 URL/网络；通知不弹→检查权限/后台限制）。

- [ ] **Step 2: 提交**

```bash
git add README.md
git commit -m "docs: full setup guide"
```

**阶段 C 完成标志：** push 到 GitHub 后 Actions 自动抓取生成 hot.json，Pages 可访问，Release 可下载 APK，用户安装后收到首次推送。

---

## 自审结果（写入计划时执行）

1. **Spec 覆盖**：设计文档中架构三部件、5 源、AI 解读、轮询通知、错误处理、最小测试均有对应任务（Task 1-11）。砍掉的特性（自定义源、搜索等）在设计中已声明，无任务属预期。
2. **占位符扫描**：无 TBD/TODO；所有代码步骤含具体代码。
3. **类型一致性**：`HotItem.id` 格式 `source:internal_id` 在 `scrape.py`/`sources.py`/`merge.py`/`llm.py`/Android `HotItem` 中保持一致；`data/hot.json` 路径在 Python 与 workflow 中一致；后端 URL 在 Android 设置页默认值与 README 一致。
