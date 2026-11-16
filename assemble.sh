#!/usr/bin/env bash
# Baut aus dem offiziellen Fabric-Beispielprojekt (Branch 26.2) + den Addon-Dateien ein Gradle-Projekt in _proj/.
# Alle Addon-Dateien liegen flach im Repo; hier werden sie in die richtigen Ordner einsortiert.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/_proj"
rm -rf "$OUT"

JAR="$(find "$ROOT" -name '*namestyle*.jar' -not -path '*/_proj/*' -not -name '*plus*' | head -1)"
[ -n "$JAR" ] || { echo "FEHLER: Original-Jar (namestyle ... .jar) nicht im Repo gefunden"; exit 1; }
echo "Original-Jar: $JAR"

git clone --depth 1 --branch 26.2 https://github.com/FabricMC/fabric-example-mod "$OUT" \
  || git clone --depth 1 https://github.com/FabricMC/fabric-example-mod "$OUT"
rm -rf "$OUT/.git" "$OUT/.github" "$OUT/src"

if grep -q "splitEnvironmentSourceSets" "$OUT/build.gradle"; then
  CODE="$OUT/src/client"; CONF="clientCompileOnly"
else
  CODE="$OUT/src/main"; CONF="compileOnly"
fi
echo "Quellordner: $CODE ($CONF)"

mkdir -p "$CODE/java/dev/namestyleplus/mixin" "$CODE/resources" "$OUT/src/main/resources" "$OUT/libs"
for f in PlusClient PlusConfig PlusStyler PlusScreen; do cp "$ROOT/$f.java" "$CODE/java/dev/namestyleplus/"; done
for f in BuildMixin GuiMixin; do cp "$ROOT/$f.java" "$CODE/java/dev/namestyleplus/mixin/"; done
cp "$ROOT/namestyleplus.mixins.json" "$CODE/resources/"
cp "$ROOT/fabric.mod.json" "$OUT/src/main/resources/"
cp "$JAR" "$OUT/libs/namestyle.jar"

cat >> "$OUT/build.gradle" <<EOT

dependencies {
	$CONF files('libs/namestyle.jar')
}
EOT
sed -i -E 's/^archives_base_name[[:space:]]*=.*/archives_base_name=namestyle-plus/; s/^mod_version[[:space:]]*=.*/mod_version=1.0.0/' "$OUT/gradle.properties"
chmod +x "$OUT/gradlew"
