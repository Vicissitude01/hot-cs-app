import html
import json
import re
import urllib.request
from datetime import datetime, timezone

from hotitem import HotItem

UA = {"User-Agent": "Mozilla/5.0 (hot-cs-app/1.0)"}


def _get_json(url):
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=20) as r:
        return json.load(r)


def _post_json(url, body):
    req = urllib.request.Request(
        url, data=json.dumps(body).encode(),
        headers={**UA, "Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=20) as r:
        return json.load(r)


def _norm(score, max_score, out_max=100.0):
    try:
        score, max_score = float(score), float(max_score)
    except (TypeError, ValueError):
        return 0.0
    return round(out_max * score / max_score, 1) if max_score else 0.0


def _iso(epoch):
    try:
        return datetime.fromtimestamp(int(epoch), tz=timezone.utc).isoformat()
    except (TypeError, ValueError, OSError):
        return ""


def hackernews():
    ids = _get_json("https://hacker-news.firebaseio.com/v0/topstories.json")[:30]
    out = []
    for i in ids:
        it = _get_json(f"https://hacker-news.firebaseio.com/v0/item/{i}.json")
        out.append(HotItem(
            id=f"hackernews:{i}",
            title=it.get("title", ""),
            url=it.get("url") or f"https://news.ycombinator.com/item?id={i}",
            source="hackernews",
            score=float(it.get("score", 0) or 0),
            published_at=_iso(it.get("time", 0))))
    return out


def github_trending():
    req = urllib.request.Request("https://github.com/trending", headers=UA)
    page = urllib.request.urlopen(req, timeout=20).read().decode("utf-8", "ignore")
    out = []
    for art in re.findall(r'<article.*?</article>', page, re.S):
        m = re.search(r'<h2[^>]*>(.*?)</h2>', art, re.S)
        if not m:
            continue
        href = re.search(r'href="/([^"/]+/[^"/]+)"', m.group(1))
        if not href:
            continue
        title = re.sub(r'<[^>]+>', '', m.group(1))
        name = html.unescape(title).strip().split("/")[-1].strip()
        repo = href.group(1)
        out.append(HotItem(
            id=f"github-trending:{repo}",
            title=name,
            url=f"https://github.com/{repo}",
            source="github-trending",
            score=50.0,
            published_at=""))
    return out


def juejin():
    data = _post_json(
        "https://api.juejin.cn/recommend_api/v1/article/recommend_all_feed",
        {"id_type": 2, "sort_type": 3, "limit": 20})
    out = []
    for it in data.get("data", []):
        if not it:
            continue
        a = it.get("item_info", {}).get("article_info", {})
        if not a.get("article_id"):
            continue
        out.append(HotItem(
            id=f"juejin:{a.get('article_id')}",
            title=a.get("title", ""),
            url=f"https://juejin.cn/post/{a.get('article_id')}",
            source="juejin",
            score=float(a.get("rank_index", 0) or 0),
            published_at=_iso(a.get("ctime", 0))))
    return out


def v2ex():
    data = _get_json("https://www.v2ex.com/api/topics/hot.json")
    out = []
    for it in data:
        out.append(HotItem(
            id=f"v2ex:{it.get('id')}",
            title=it.get("title", ""),
            url=f"https://www.v2ex.com/t/{it.get('id')}",
            source="v2ex",
            score=float(it.get("replies", 0) or 0) * 2,
            published_at=_iso(it.get("created", 0))))
    return out


def lobsters():
    data = _get_json("https://lobste.rs/hottest.json")
    out = []
    for it in data:
        sid = it.get("short_id", "")
        out.append(HotItem(
            id=f"lobsters:{sid}",
            title=it.get("title", ""),
            url=it.get("url") or f"https://lobste.rs/s/{sid}",
            source="lobsters",
            score=float(it.get("score", 0) or 0),
            published_at=it.get("created_at", "")))
    return out


ALL_SOURCES = {"hackernews": hackernews, "github-trending": github_trending,
               "juejin": juejin, "v2ex": v2ex, "lobsters": lobsters}
