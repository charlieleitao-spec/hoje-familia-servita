"""Exercise the published signed APK update independently of the proposed debug build."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

OUT = Path('update-evidence')
OUT.mkdir(exist_ok=True)
PACKAGE = 'br.org.servitas.familia'


def adb(*args):
    return subprocess.check_output(['adb', *args], text=True, stderr=subprocess.STDOUT)


def package_state(label):
    text = adb('shell', 'dumpsys', 'package', PACKAGE)
    (OUT / (label + '-package.txt')).write_text(text)
    result = {}
    for key in ('userId', 'firstInstallTime', 'ceDataInode', 'versionName'):
        field = r'(?:userId|appId)' if key == 'userId' else key
        match = re.search(r'\b' + field + r'=([^\r\n]+)', text)
        if not match:
            raise AssertionError('Missing package field ' + key)
        result[key] = match[1].strip() if key == 'firstInstallTime' else match[1].split()[0]
    return result


def wait_text(wanted, label):
    for _ in range(60):
        try:
            adb('shell', 'uiautomator', 'dump', '/sdcard/update-window.xml')
            adb('pull', '/sdcard/update-window.xml', str(OUT / (label + '.xml')))
            root = ET.parse(OUT / (label + '.xml')).getroot()
            for node in root.iter():
                text = node.get('text', '') + ' ' + node.get('content-desc', '')
                if wanted in text:
                    return node
        except (subprocess.CalledProcessError, ET.ParseError):
            pass
        time.sleep(1)
    raise AssertionError('UI text missing: ' + wanted)


try:
    adb('shell', 'svc', 'wifi', 'disable')
    adb('shell', 'svc', 'data', 'disable')
    print(adb('install', 'published-2.9.34.apk'), flush=True)
    before = package_state('before')
    assert before['versionName'] == '2.9.34', before
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    wait_text('Somos Servos', 'before-offline')
    # No uninstall or clear between the two signed releases.
    print(adb('install', '-r', 'published-2.9.35.apk'), flush=True)
    after = package_state('after')
    assert after['versionName'] == '2.9.35', after
    for key in ('userId', 'firstInstallTime', 'ceDataInode'):
        assert before[key] == after[key], (key, before, after)
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/.MainActivity')
    wait_text('Somos Servos', 'after-offline')
    # Open a packaged PDF through the existing fallback UI after updating.
    adb('shell', 'input', 'swipe', '520', '1700', '520', '600', '500')
    node = wait_text('Folhas do ano (PDF)', 'after-materials')
    bounds = list(map(int, re.findall(r'\d+', node.get('bounds', ''))))
    assert len(bounds) == 4 and bounds[2] > bounds[0] and bounds[3] > bounds[1], bounds
    adb('shell', 'input', 'tap', str((bounds[0] + bounds[2]) // 2), str((bounds[1] + bounds[3]) // 2))
    wait_text('Página 1 de', 'after-local-pdf')
    subprocess.run(['adb', 'exec-out', 'screencap', '-p'], stdout=(OUT / 'after-local-pdf.png').open('wb'), check=True)
    message = 'PASS: Published signed 2.9.34 -> 2.9.35 installed with -r; UID, firstInstallTime and data inode retained; offline fallback and packaged PDF opened.'
    print(message, flush=True)
    (OUT / 'result.txt').write_text(message + '\n')
finally:
    (OUT / 'logcat.txt').write_text(adb('logcat', '-d'))
