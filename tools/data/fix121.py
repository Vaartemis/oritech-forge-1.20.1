"""1.21-only vanilla content -> 1.20.1 substitutes. Run after convert.py on res/."""
import json,os,sys
R=sys.argv[1]  # resources root
D=R+'/data/oritech'
SUB={'minecraft:crafter':'minecraft:crafting_table','minecraft:wind_charge':'minecraft:phantom_membrane',
     'minecraft:heavy_core':'minecraft:netherite_block'}
ATTR={'minecraft:generic.gravity':'forge:entity_gravity','minecraft:generic.step_height':'forge:step_height_addition',
      'minecraft:player.block_interaction_range':'forge:block_reach','minecraft:player.entity_interaction_range':'forge:entity_reach',
      'minecraft:player.block_break_speed':'attributeslib:mining_speed'}
DROP=['recipes/augment/dwarf.json','recipes/augment/giant.json','recipes/particle/heavy_core.json']
DROP_IDS={'oritech:augment/dwarf','oritech:augment/giant'}
def walk(o):
    if isinstance(o,dict): return {k:walk(v) for k,v in o.items()}
    if isinstance(o,list): return [walk(v) for v in o if not (isinstance(v,str) and v in DROP_IDS)]
    if isinstance(o,str): return SUB.get(o,ATTR.get(o,o))
    return o
for p in DROP:
    if os.path.exists(D+'/'+p): os.remove(D+'/'+p)
n=0
for d,_,fs in os.walk(D+'/recipes'):
    for f in fs:
        p=os.path.join(d,f); j=json.load(open(p)); k=walk(j)
        if 'attributeslib:mining_speed' in json.dumps(k): k.setdefault('conditions',[]).append({'type':'forge:mod_loaded','modid':'attributeslib'})
        if k!=j: json.dump(k,open(p,'w'),indent=2); n+=1
# tags: drop entries that do not exist in 1.20.1, short_grass -> grass
ids=set(l.split()[1] for l in open(os.environ.get('REGISTRY_IDS', 'ids.txt')))
for d,_,fs in os.walk(D+'/tags'):
    for f in fs:
        p=os.path.join(d,f); j=json.load(open(p)); vals=[]
        for v in j.get('values',[]):
            s=v if isinstance(v,str) else v.get('id','')
            if s=='minecraft:short_grass': v='minecraft:grass'; s=v
            if s.startswith('minecraft:') and s not in ids: continue
            vals.append(v)
        if vals!=j.get('values'): j['values']=vals; json.dump(j,open(p,'w'),indent=2); n+=1
print('fix121 changed',n)
