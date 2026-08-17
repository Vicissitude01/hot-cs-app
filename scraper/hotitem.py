from dataclasses import dataclass, field, asdict


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
