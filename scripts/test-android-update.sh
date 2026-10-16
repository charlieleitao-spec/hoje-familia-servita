#!/usr/bin/env bash
set -euo pipefail
package=br.org.servitas.familia
activity="$package/.MainActivity"
mkdir -p android-test
trap 'adb shell svc wifi enable >/dev/null 2>&1 || true; adb shell svc data enable >/dev/null 2>&1 || true; adb logcat -d > android-test/logcat.txt || true; adb shell dumpsys connectivity > android-test/connectivity.txt || true; adb shell dumpsys webviewupdate > android-test/webview.txt || true' EXIT

wait_for_text() {
  local wanted="$1" destination="$2"
  for attempt in $(seq 1 20); do
    adb shell uiautomator dump /sdcard/window.xml >/dev/null 2>&1 || true
    adb pull /sdcard/window.xml "$destination" >/dev/null 2>&1 || true
    if test -f "$destination" && python3 - "$destination" "$wanted" <<'PY'
import sys, xml.etree.ElementTree as ET
try:
    root=ET.parse(sys.argv[1])
    values=' '.join(value for node in root.iter() for key,value in node.attrib.items() if key in ('text','content-desc'))
    sys.exit(0 if sys.argv[2] in values else 1)
except (ET.ParseError, OSError):
    sys.exit(1)
PY
    then return 0; fi
    sleep 3
  done
  echo "Não foi encontrado na tela: $wanted"
  cat "$destination" || true
  return 1
}

adb install android-test/previous.apk | tee android-test/install-previous.txt
adb shell dumpsys package "$package" > android-test/package-before.txt
grep -q 'versionName=2.9.13' android-test/package-before.txt
adb shell am start -W -n "$activity" | tee android-test/launch-previous.txt
sleep 3
adb shell pidof "$package" > android-test/process-previous.txt

adb install -r android-test/current.apk | tee android-test/install-update.txt
adb shell dumpsys package "$package" > android-test/package-after.txt
grep -q 'versionName=2.9.15' android-test/package-after.txt
grep -q 'versionCode=296' android-test/package-after.txt
python3 - <<'PY'
import re
from pathlib import Path
before=Path('android-test/package-before.txt').read_text()
after=Path('android-test/package-after.txt').read_text()
for field in ['appId','firstInstallTime','ceDataInode']:
    a=re.search(r'\b'+field+r'=(.+)',before)
    b=re.search(r'\b'+field+r'=(.+)',after)
    assert a and b, f'Campo {field} não encontrado no relatório do Android'
    # ceDataInode may share a line with other per-user fields; compare its numeric value.
    left=a[1].split()[0] if field!='firstInstallTime' else a[1]
    right=b[1].split()[0] if field!='firstInstallTime' else b[1]
    assert left==right, f'{field} mudou durante a atualização: {left!r} / {right!r}'
print('Atualização instalada sem desinstalar; UID e data da primeira instalação preservados.')
PY
adb logcat -c
adb shell svc wifi enable
adb shell svc data enable
adb shell settings put global private_dns_mode off
adb shell am start -W -n "$activity" | tee android-test/launch-current.txt
if ! wait_for_text 'Hoje na Família Servita' android-test/current-online.xml; then
  # A transient network failure on first boot sends the wrapper to its retry screen.
  adb shell am force-stop "$package"
  adb shell am start -W -n "$activity"
  wait_for_text 'Hoje na Família Servita' android-test/current-online.xml
fi
# Give the first service worker installation time to finish caching all essentials.
sleep 10
adb shell pidof "$package" > android-test/process-online.txt

adb shell svc wifi disable
adb shell svc data disable
sleep 3
adb shell am force-stop "$package"
adb shell am start -W -n "$activity" | tee android-test/launch-offline.txt
wait_for_text 'Sem conexão.' android-test/current-offline.xml
adb shell pidof "$package" > android-test/process-offline.txt

cat > android-test/result.txt <<'TXT'
APROVADO — emulador Android 15 (API 35)
APK 2.9.13 instalado; atividade e processo iniciados.
APK 2.9.15 instalado com adb install -r, sem desinstalação.
versionCode 296 confirmado; UID e primeira instalação preservados.
Tela principal carregada online após atualização.
Aplicativo reaberto com Wi-Fi e dados desativados; aviso Sem conexão exibido.
Processo permaneceu ativo nas verificações online e offline.
TXT
cat android-test/result.txt
if test -n "${GITHUB_STEP_SUMMARY:-}"; then cat android-test/result.txt >> "$GITHUB_STEP_SUMMARY"; fi
