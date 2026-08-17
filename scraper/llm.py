import json
import urllib.request


def summarize(items, api_key, base_url, model, max_items=10):
    """为未解读的新条目生成 summary + key_points。失败返回空解读，不重试。"""
    if not api_key or not items:
        return
    prompt = ("为以下计算机热点各写一句\"为什么值得看\"的解读和 3 个要点，"
              "用中文，输出 JSON 对象，格式："
              '{"items": [{"id": "...", "summary": "...", "key_points": ["..."]}]}。输入：\n')
    for it in items[:max_items]:
        prompt += f"- [{it.id}] {it.title}\n"
    body = {
        "model": model,
        "messages": [{"role": "user", "content": prompt}],
        "response_format": {"type": "json_object"},
        "temperature": 0.3,
    }
    req = urllib.request.Request(
        base_url.rstrip("/") + "/chat/completions",
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
