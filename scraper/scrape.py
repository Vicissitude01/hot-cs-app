import argparse
import json
import os
import sys

from hotitem import HotItem
import merge
import llm
import sources

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
        try:
            llm.summarize(new, api_key, base_url, model)
        except Exception as e:
            # LLM 失败不阻塞抓取（API Key 无效/超时等）
            print(f"[warn] LLM summarize failed: {e}", file=sys.stderr)
    save(items)
    print(f"ok: {len(items)} items, {len(new)} new")
    return items


def selftest():
    from test_hotitem import test_roundtrip
    from test_merge import test_merge_dedupes_and_returns_new, test_merge_respects_limit
    test_roundtrip()
    test_merge_dedupes_and_returns_new()
    test_merge_respects_limit()
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
