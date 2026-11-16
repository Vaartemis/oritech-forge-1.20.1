"""Create 6 (1.21 NeoForge) compat recipes -> Create 6.0.8 Forge 1.20.1 format, written into res/. Run after convert.py."""
import json, os, glob, re, sys
SRC = sys.argv[1]  # upstream neoforge/src/main/generated/data/oritech/recipe
OUT = sys.argv[2]  # src/main/resources/data/oritech/recipes
src_conv = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'convert.py')).read()
ns = {}
exec(src_conv[src_conv.index('# c: tag'):src_conv.index('def walk')], ns)
fix = ns['fix_tag_ref']
def ingr(i):
    if i.get('type') == 'fluid_stack' or ('fluid' in i and 'amount' in i):
        return {'fluid': i['fluid'], 'amount': i['amount']}
    if 'tag' in i: return {'tag': fix(i['tag'])}
    return i
def res(r):
    if 'amount' in r: return {'fluid': r['id'], 'amount': r['amount']}
    o = {'item': r['id']}
    for k in ('count', 'chance'):
        if k in r: o[k] = r[k]
    return o
n = 0
for p in glob.glob(SRC + '/*/compat/create/*.json'):
    j = json.load(open(p))
    if not j.get('type','').startswith('create:'): continue
    o = {'type': j['type'], 'conditions': [{'type': 'forge:mod_loaded', 'modid': 'create'}]}
    if 'heat_requirement' in j: o['heatRequirement'] = j['heat_requirement']
    o['ingredients'] = [ingr(i) for i in j['ingredients']]
    o['results'] = [res(r) for r in j['results']]
    if 'processing_time' in j: o['processingTime'] = j['processing_time']
    rel = os.path.relpath(p, SRC)
    dst = os.path.join(OUT, rel); os.makedirs(os.path.dirname(dst), exist_ok=True)
    json.dump(o, open(dst, 'w'), indent=2); n += 1
print('create compat recipes', n)
