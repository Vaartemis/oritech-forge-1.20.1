#!/usr/bin/env python3
"""Oritech 1.21 resources -> Forge 1.20.1 resource tree. usage: convert.py <src_root> <out_dir>"""
import json, os, re, shutil, sys
SRC, OUT = sys.argv[1], sys.argv[2]
shutil.rmtree(OUT, ignore_errors=True)
os.makedirs(OUT)
stats = {}
def bump(k): stats[k] = stats.get(k, 0) + 1

# c: tag -> forge: tag (names that differ on Forge 1.20.1)
C2F = {
 'glass_blocks': 'glass', 'glass_panes': 'glass_panes', 'obsidians': 'obsidian', 'stones': 'stone',
 'cobblestones': 'cobblestone', 'slime_balls': 'slimeballs', 'strings': 'string', 'leathers': 'leather',
 'gunpowders': 'gunpowder', 'sands': 'sand', 'gravels': 'gravel', 'netherracks': 'netherrack',
 'end_stones': 'end_stones', 'bones': 'bones', 'eggs': 'eggs', 'feathers': 'feathers', 'ender_pearls': 'ender_pearls',
 'chests': 'chests', 'barrels': 'barrels', 'bookshelves': 'bookshelves', 'mushrooms': 'mushrooms',
 'player_workstations/crafting_tables': 'workbench', 'player_workstations/furnaces': 'furnaces',
 'tools/shears': 'shears', 'tools/bow': 'tools/bows', 'tools/shield': 'tools/shields', 'tools/fishing_rod': 'tools/fishing_rods',
 'foods': 'foods', 'buckets/water': 'buckets/water', 'buckets/lava': 'buckets/lava', 'buckets/empty': 'buckets/empty',
 'crops': 'crops', 'seeds': 'seeds', 'dyes': 'dyes', 'rods/wooden': 'rods/wooden', 'rods/blaze': 'rods/blaze',
 'is_desert': 'is_desert', 'is_hot': 'is_hot', 'is_cold': 'is_cold', 'is_snowy': 'is_snowy', 'is_dry': 'is_dry',
 'is_wet': 'is_wet', 'is_sandy': 'is_sandy', 'is_mountain': 'is_mountain', 'is_plains': 'is_plains',
 'is_overworld': None, 'is_nether': None, 'is_end': None,
}
VANILLA_BIOME = {'is_overworld': 'minecraft:is_overworld', 'is_nether': 'minecraft:is_nether', 'is_end': 'minecraft:is_end'}
def ctag(path):
    if path in VANILLA_BIOME: return VANILLA_BIOME[path]
    return 'forge:' + (C2F.get(path) or path)
def fix_tag_ref(s):
    if not isinstance(s, str): return s
    if s.startswith('#c:'): return '#' + ctag(s[3:])
    if s.startswith('c:'): return ctag(s[2:])
    return s
def walk(o, key=None):
    if isinstance(o, dict):
        return {k: walk(v, k) for k, v in o.items()}
    if isinstance(o, list):
        return [walk(v, key) for v in o]
    if isinstance(o, str) and (key in ('tag', 'values', 'biomes', 'fluid', 'items', 'blocks', None) or o.startswith('#c:')):
        return fix_tag_ref(o)
    return o
def conv_conditions(o):
    if isinstance(o, dict):
        n = {}
        for k, v in o.items():
            if k == 'neoforge:conditions': k = 'conditions'
            if k == 'type' and isinstance(v, str) and v.startswith('neoforge:'): v = 'forge:' + v[9:]
            n[k] = conv_conditions(v)
        return n
    if isinstance(o, list): return [conv_conditions(v) for v in o]
    return o
def conv_result(r):
    if isinstance(r, dict) and 'id' in r and 'item' not in r:
        r = dict(r); r['item'] = r.pop('id')
        if 'components' in r: r.pop('components'); bump('result_components_dropped')
    return r
KEEP_NS = {'minecraft', 'oritech'}
def conv_recipe(j):
    t = j.get('type', '')
    if t.split(':')[0] not in KEEP_NS: bump('recipe_foreign_dropped'); return None
    j = conv_conditions(walk(j))
    if t.startswith('minecraft:'):
        if 'result' in j:
            r = conv_result(j['result'])
            if t in ('minecraft:smelting', 'minecraft:blasting', 'minecraft:smoking', 'minecraft:campfire_cooking') and isinstance(r, dict) and r.get('count', 1) == 1 and len(r) <= 2:
                r = r['item']
            j['result'] = r
    return j
def conv_match_tool(o):
    if isinstance(o, dict):
        if o.get('condition') == 'minecraft:match_tool' and isinstance(o.get('predicate'), dict):
            p = dict(o['predicate'])
            preds = p.pop('predicates', {})
            ench = preds.get('minecraft:enchantments')
            if ench:
                p['enchantments'] = [{'enchantment': e.get('enchantments'), 'levels': e.get('levels', {'min': 1})} for e in ench]
            if isinstance(p.get('items'), str):
                v = p.pop('items')
                if v.startswith('#'): p['tag'] = v[1:]
                else: p['items'] = [v]
            o = dict(o); o['predicate'] = p
        return {k: conv_match_tool(v) for k, v in o.items()}
    if isinstance(o, list): return [conv_match_tool(v) for v in o]
    return o
DIR_MAP = {'recipe': 'recipes', 'loot_table': 'loot_tables', 'advancement': 'advancements'}
TAG_MAP = {'item': 'items', 'block': 'blocks', 'fluid': 'fluids', 'entity_type': 'entity_types', 'game_event': 'game_events'}
def out_path(rel):
    parts = rel.split('/')
    # parts: data, ns, kind, ...
    if parts[0] != 'data' or len(parts) < 4: return rel
    ns, kind = parts[1], parts[2]
    if kind in DIR_MAP: parts[2] = DIR_MAP[kind]
    if kind == 'tags' and len(parts) > 4 and parts[3] in TAG_MAP: parts[3] = TAG_MAP[parts[3]]
    if kind == 'neoforge': parts[2] = 'forge'
    if ns == 'c' and kind == 'tags':
        sub = '/'.join(parts[4:])[:-5]
        mapped = ctag(sub)
        mns, mpath = mapped.split(':')
        parts = ['data', mns, 'tags', parts[3]] + (mpath + '.json').split('/')
    return '/'.join(parts)
def merge_tag(dst, j):
    if os.path.exists(dst):
        old = json.load(open(dst))
        vals = old.get('values', []) + [v for v in j.get('values', []) if v not in old.get('values', [])]
        old['values'] = vals; j = old
    return j
roots = ['common/src/main/generated', 'neoforge/src/main/generated', 'common/src/main/resources', 'neoforge/src/main/resources']  # hand-written assets win over datagen stubs
for root in roots:
    base = os.path.join(SRC, root)
    for dp, dn, fn in os.walk(base):
        for f in fn:
            full = os.path.join(dp, f); rel = os.path.relpath(full, base)
            if rel.startswith('META-INF') or f.endswith('.mixins.json') or f in ('interfaces.json',) or rel.startswith('assets/oritech/../'): continue
            if not (rel.startswith('assets/') or rel.startswith('data/')): continue
            if '/advancement/' in rel: bump('advancement_dropped'); continue
            if rel.startswith('assets/emi/') or rel.startswith('data/neoforge/'): bump('platform_dropped'); continue
            orel = out_path(rel)
            dst = os.path.join(OUT, orel)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            if rel.startswith('data/') and f.endswith('.json'):
                try: j = json.load(open(full))
                except Exception as e: print('BAD JSON', rel, e); continue
                kind = rel.split('/')[2]
                if kind == 'recipe':
                    j = conv_recipe(j)
                    if j is None: continue
                elif kind == 'loot_table':
                    j = conv_match_tool(walk(j))
                elif kind == 'tags':
                    j = walk(j); j = merge_tag(dst, j)
                else:
                    j = conv_conditions(walk(j))
                txt = json.dumps(j, indent=2)
                txt = txt.replace('"neoforge:add_features"', '"forge:add_features"').replace('"neoforge:remove_features"', '"forge:remove_features"')
                open(dst, 'w').write(txt); bump('data_json')
            else:
                shutil.copy(full, dst); bump('copied')
json.dump({'pack': {'pack_format': 15, 'description': 'Oritech resources'}}, open(os.path.join(OUT, 'pack.mcmeta'), 'w'), indent=2)
print(stats)
