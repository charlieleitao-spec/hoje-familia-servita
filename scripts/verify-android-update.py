"""Check package identity and data directory across a real adb install -r."""
import os
import re
import sys
from pathlib import Path


def state(path):
    text = Path(path).read_text()
    result = {}
    for key, pattern in {
        'uid': r'\b(?:userId|appId)=(\d+)',
        'firstInstallTime': r'\bfirstInstallTime=([^\r\n]+)',
        'ceDataInode': r'\bceDataInode=(\d+)',
        'versionName': r'\bversionName=([^\s]+)',
        'versionCode': r'\bversionCode=(\d+)',
    }.items():
        match = re.search(pattern, text)
        if not match:
            raise AssertionError('Missing package field: ' + key)
        result[key] = match[1].strip()
    return result


before, after = (state(path) for path in sys.argv[1:3])
for key in ('uid', 'firstInstallTime', 'ceDataInode'):
    if before[key] != after[key]:
        raise AssertionError('Update changed ' + key)
if (after['versionName'] != os.environ['APP_VERSION'] or
        after['versionCode'] != os.environ['APP_VERSION_CODE']):
    raise AssertionError('Unexpected release version: ' + str(after))
if int(after['versionCode']) <= int(before['versionCode']):
    raise AssertionError('versionCode did not increase')
print('PASS: Official signed update ' + before['versionName'] + ' -> ' + after['versionName'] + '; UID, firstInstallTime and data inode retained.')
