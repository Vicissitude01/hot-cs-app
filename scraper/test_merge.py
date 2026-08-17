from hotitem import HotItem
from merge import merge


def _it(i, src="hn"):
    return HotItem(id=f"{src}:{i}", title=str(i), url=f"u/{i}",
                   source=src, score=1.0, published_at="")


def test_merge_dedupes_and_returns_new():
    existing = [_it(1), _it(2)]
    fresh = [_it(2), _it(3)]
    merged, new = merge(existing, fresh)
    assert [it.id for it in merged] == ["hn:3", "hn:1", "hn:2"]
    assert [it.id for it in new] == ["hn:3"]


def test_merge_respects_limit():
    merged, _ = merge([_it(i) for i in range(3)], [_it(99)], limit=3)
    assert len(merged) == 3
