from hotitem import HotItem


def test_roundtrip():
    it = HotItem(id="hn:1", title="t", url="u", source="hackernews",
                 score=50.0, published_at="", summary="s", key_points=["a"])
    assert HotItem.from_dict(it.to_dict()) == it
