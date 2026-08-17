# CS 热点

一个给自己用的 Android 应用：定时推送计算机领域的实时热点，每条热点附带 AI 生成的一句话解读和要点。

**数据源（5 个）**：Hacker News · GitHub Trending · 掘金 · V2EX · Lobsters
**费用**：0 元（GitHub 免费额度，无需云服务器）

## 它怎么工作

```
这个仓库（全部免费）
 ├─ GitHub Actions 每 30 分钟自动抓 5 个源 → AI 解读 → 生成 data/hot.json
 ├─ GitHub Pages 免费托管 hot.json（相当于一个接口）
 └─ GitHub Actions 自动把 App 编译成 APK 发到 Release
手机 App：每 15~60 分钟后台检查一次 → 有新热点弹通知 → 点开看详情
```

## 目录结构

```
scraper/          Python 抓取脚本（自动跑，不用管）
data/hot.json     抓取结果（自动生成，不用管）
app/              Android 应用源码（自动编译成 APK）
.github/workflows/ 自动化任务（自动跑，不用管）
docs/             设计文档和实现计划
```

## 小白上手（总共 5 步）

### 第 1 步：注册 GitHub

去 <https://github.com> 注册一个账号（免费，网页全中文）。

### 第 2 步：创建仓库并上传本代码

1. GitHub 首页点 **New repository**，仓库名填 `hot-cs-app`，选 **Public** 或 **Private** 都行，点 Create。
2. 打开本机终端（Windows 下按 `Win+R` 输入 `cmd` 回车），进入本项目目录，逐行执行：

```bash
git remote add origin https://github.com/你的用户名/hot-cs-app.git
git branch -M main
git push -u origin main
```

3. 刷新 GitHub 页面，能看到代码就成功了。

### 第 3 步：开启 GitHub Pages（App 靠它读数据）

1. 仓库页面 → **Settings**（齿轮）→ 左侧 **Pages**
2. **Source** 选 **Deploy from a branch** → 分支选 `main`，路径 `/root` → 点 **Save**
3. 等 1~2 分钟后访问：
   `https://你的用户名.github.io/hot-cs-app/data/hot.json`
   能看到一堆热点 JSON 就成功了。**把这个网址复制好，第 5 步要用。**

### 第 4 步：（可选）配置 AI 解读 API Key

不配置也能收到热点，只是没有 AI 解读。想有解读就：

1. 注册 [DeepSeek 开放平台](https://platform.deepseek.com)（便宜，注册送额度）
2. 创建 API Key（形如 `sk-xxxx`）
3. 仓库页面 → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**
4. 名称填 `LLM_API_KEY`，值粘贴你的 Key，保存

### 第 5 步：下载并安装 App

1. 仓库页面 → 右侧 **Releases** → 最新版 → 下载 `app-release.apk`
2. 手机打开文件，允许「安装未知来源应用」，安装
3. 打开 App → 右上角**设置** → 粘贴第 3 步的后端地址 → **保存并测试连接**（显示"连接成功"即可）
4. 允许通知权限，第一次打开会弹窗；如果没弹，去 系统设置 → 应用 → CS热点 → 通知 → 允许

之后每 30 分钟后台会自动检查新热点并弹通知。首次打开看到热点列表可能需要等抓取任务跑过一轮（手动跑一次：仓库 → **Actions** → 左侧 **scrape** → **Run workflow**）。

## 常见问题

| 现象 | 原因与解决 |
|---|---|
| App 里"还没有数据" | 后端地址没填或填错 → 设置里粘贴 Pages 网址并测试连接 |
| 测试连接失败 | Pages 没开或网址多了空格 → 确认能直接打开那个 hot.json |
| 没有 AI 解读 | 没配 `LLM_API_KEY`，或当天新热点超过 10 条（解读上限） |
| 通知不弹 | 系统设置里允许通知；部分国产 ROM 需在「自启动管理」放行 |
| 通知很久才来 | 间隔是 15~60 分钟（可改），且手机省电策略可能延长后台运行 |
| 某个源没内容 | 该源接口偶尔限流，下一次抓取会自动恢复，不影响其他源 |

## 开发者说明

- 抓取脚本：`python scraper/scrape.py`（本机跑通后再传）；自检 `python scraper/scrape.py --selftest`
- 本地跑 App：用 Android Studio 打开仓库根目录（首次会下载依赖，较慢）
- 单元测试在 CI 自动执行（`gradle :app:testDebugUnitTest`）
- 本项目按 superpowers 流程开发，设计/计划文档在 `docs/superpowers/`
