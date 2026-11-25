"""Rewrite Oritech recipe result stacks from 1.21 style {"id": ...} to 1.20.1 style {"item": ...}.
Almost Unified (1.20.1) only unifies result objects keyed "item". Usage: id2item.py <root> [--check]"""
import json, sys, pathlib, re
root = pathlib.Path(sys.argv[1]); check = '--check' in sys.argv
STACK_KEYS = ('results', 'result', 'output', 'outputs')
changed = left = 0
for f in sorted(root.rglob('*.json')):
    if '/recipes/' not in f.as_posix(): continue
    txt = f.read_text(encoding='utf-8')
    try: j = json.loads(txt)
    except Exception: continue
    if not isinstance(j, dict) or not str(j.get('type', '')).startswith('oritech:'): continue
    n = 0
    for k in STACK_KEYS:
        v = j.get(k)
        for o in (v if isinstance(v, list) else [v]):
            if isinstance(o, dict) and 'id' in o and 'item' not in o:
                n += 1
                if not check:
                    # keep key order: rebuild dict with item in place of id
                    items = [('item' if kk == 'id' else kk, vv) for kk, vv in o.items()]
                    o.clear(); o.update(items)
    if n:
        left += n
        if not check:
            f.write_text(json.dumps(j, indent=2) + '\n', encoding='utf-8'); changed += 1
print(('LEFT' if check else 'FIXED'), left, 'result stacks', ('' if check else f'in {changed} files'))
