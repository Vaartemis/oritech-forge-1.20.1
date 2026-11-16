#!/bin/bash
# Build without Gradle: javac against Mojang-mapped Minecraft/Forge jars, then reobfuscate to SRG with ForgeAutoRenamingTool.
# Environment:
#   KIT   folder with mc-moj-at.jar, forgeclient-moj-at.jar, forgeuniversal-moj.jar (Mojang-mapped, access transformers applied),
#         moj2srg.tsrg and extmoj/*.jar (architectury, geckolib, ... in Mojang names)
#   LIBS  a Minecraft/Forge libraries folder (as installed by the Forge installer), incl. ForgeAutoRenamingTool 0.1.22
#   OUT   output jar (default build/oritech-forge-1.20.1-1.2.13.jar)
set -e
ROOT=$(cd "$(dirname "$0")/.." && pwd)
: "${KIT:?set KIT}"; : "${LIBS:?set LIBS}"
OUTJAR=${OUT:-$ROOT/build/oritech-forge-1.20.1-1.2.13.jar}
EXT=$(ls $KIT/extmoj/*.jar | tr '\n' ':')
CPLIBS=$(find $LIBS -name '*.jar' ! -name 'ForgeAuto*' ! -name '*srg.jar' ! -path '*forge/1.20.1*' | tr '\n' ':')
CP="$KIT/forgeclient-moj-at.jar:$KIT/mc-moj-at.jar:$KIT/forgeuniversal-moj.jar:$CPLIBS$EXT"
W=$ROOT/build/work; rm -rf $W && mkdir -p $W/classes $W/stage $(dirname $OUTJAR)
javac --release 17 -proc:none -nowarn -encoding UTF-8 -cp "$CP" -d $W/classes $(find $ROOT/src/main/java -name '*.java')
(cd $W/classes && zip -q -r $W/moj.jar .)
ARTLIBS="--lib $KIT/mc-moj-at.jar --lib $KIT/forgeclient-moj-at.jar --lib $KIT/forgeuniversal-moj.jar"
for j in $KIT/extmoj/*.jar; do ARTLIBS="$ARTLIBS --lib $j"; done
java -jar $(find $LIBS -name 'ForgeAutoRenamingTool-*-all.jar' | head -1) --input $W/moj.jar --output $W/srg.jar --map $KIT/moj2srg.tsrg $ARTLIBS > $W/art.log
cd $W/stage && unzip -q $W/srg.jar && cp -r $ROOT/src/main/resources/. .
mkdir -p META-INF && printf 'Manifest-Version: 1.0\nSpecification-Title: oritech\nImplementation-Title: Oritech\nImplementation-Version: 1.2.13-forge1.20.1\nMixinConfigs: oritech.mixins.json\n' > META-INF/MANIFEST.MF
rm -f $OUTJAR && zip -q $OUTJAR META-INF/MANIFEST.MF && zip -q -r $OUTJAR . -x META-INF/MANIFEST.MF
echo "built $OUTJAR"
