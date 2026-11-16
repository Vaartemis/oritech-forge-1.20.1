import json,os,re,sys,collections
ids=set(l.split()[1] for l in open(os.environ.get('REGISTRY_IDS', 'ids.txt')))|{'minecraft:crafting_shaped','minecraft:crafting_shapeless','minecraft:smelting','minecraft:blasting','minecraft:smithing_transform','minecraft:stonecutting','minecraft:smoking','minecraft:campfire_cooking','minecraft:binomial_with_bonus_count','minecraft:ore_drops','minecraft:uniform_bonus_count'}
root=sys.argv[1]  # data root
pat=re.compile(r'"(tag"\s*:\s*"|#?)minecraft:([a-z0-9_/.]+)"')
bad=collections.defaultdict(set)
for d,_,fs in os.walk(root):
  for f in fs:
    if not f.endswith('.json'): continue
    p=os.path.join(d,f); t=open(p).read()
    for tag,n in pat.findall(t):
      if tag: continue
      if 'minecraft:'+n not in ids: bad['minecraft:'+n].add(os.path.relpath(p,root))
for k in sorted(bad): print(k, len(bad[k]), sorted(bad[k])[:3])
