#!/bin/bash
# usage: KIT=<kit> verify_srg.sh <jar>  -> lists bytecode refs to Minecraft members still in Mojang names (must print MISSED 0)
export JAVA_TOOL_OPTIONS=
D=$(mktemp -d); cd $D && unzip -q "$1" 'rearth/*' 
find rearth -name '*.class' > list.txt
javap -c -p $(cat list.txt) 2>/dev/null | grep -oE "// (Method|Field|InterfaceMethod) (net/minecraft|com/mojang)/[A-Za-z0-9/\$_]+\.[A-Za-z0-9_\$<>]+:[^ ]+" | sort -u > refs.txt
python3 - "$D/refs.txt" <<'PY'
import sys,re
M={};cur=None
for l in open(''+__import__('os').environ['KIT']+'/moj2srg.tsrg'):
    if not l.startswith('\t'): cur=l.split()[0]; continue
    p=l.split()
    M.setdefault(cur,set()).add((p[0], p[1] if len(p)==3 else None))
bad=0
for l in open(sys.argv[1]):
    m=re.match(r'// \w+ (\S+)\.([^:.]+):(\S+)',l.strip())
    c,n,d=m.groups()
    s=M.get(c,set())
    if (n,d) in s or (n,None) in s:
        print(c,n,d); bad+=1
print('MISSED',bad)
PY
rm -rf $D
