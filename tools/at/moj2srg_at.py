import sys
# usage: moj2srg_at.py moj2srg.tsrg in.cfg out.cfg
tsrg, src, out = sys.argv[1:]
F = {}; M = {}; cur = None
for l in open(tsrg):
    if not l.startswith('\t'): cur = l.split()[0]; continue
    p = l.split()
    if len(p) == 2: F[(cur, p[0])] = p[1]
    else: M[(cur, p[0], p[1])] = p[2]
res = []
for l in open(src):
    l = l.strip()
    if not l or l.startswith('#'): continue
    p = l.split()
    if len(p) == 2: res.append(l); continue
    cls = p[1].replace('.', '/'); mem = p[2]
    if '(' in mem:
        name, desc = mem[:mem.index('(')], mem[mem.index('('):]
        if name == '<init>': res.append(l); continue
        s = M.get((cls, name, desc))
        if not s: print('NO METHOD', l); continue
        res.append(f'{p[0]} {p[1]} {s}{desc} # {name}')
    else:
        s = F.get((cls, mem))
        if not s: print('NO FIELD', l); continue
        res.append(f'{p[0]} {p[1]} {s} # {mem}')
open(out, 'w').write('\n'.join(res) + '\n')
print(len(res), 'lines')
