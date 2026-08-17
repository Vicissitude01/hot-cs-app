from hotitem import HotItem


def merge(existing: list[HotItem], fresh: list[HotItem], limit=500) -> tuple[list[HotItem], list[HotItem]]:
    seen = {it.id for it in existing}
    out = list(existing)
    new_items = []
    for it in fresh:
        if it.id not in seen:
            seen.add(it.id)
            out.insert(0, it)
            new_items.append(it)
    return out[:limit], new_items
