"""Fetch pinned PDFs and verify their bytes before exposing them to the PWA."""
import hashlib,json,urllib.request
from pathlib import Path
root=Path(__file__).resolve().parents[1]
manifest=json.loads((root/'android-pdf/documents.json').read_text())
for item in manifest['documents']:
    if not item['path'].startswith('data/pdfs/biblioteca/'): continue
    target=root/item['path']
    if target.is_file() and hashlib.sha256(target.read_bytes()).hexdigest()==item['sha256']: continue
    url=f"https://raw.githubusercontent.com/{manifest['repository']}/{manifest['commit']}/www/{item['path']}"
    with urllib.request.urlopen(url,timeout=60) as response: data=response.read()
    if len(data)!=item['size'] or hashlib.sha256(data).hexdigest()!=item['sha256'] or not data.startswith(b'%PDF'):
        raise ValueError(f"PDF divergente: {item['path']}")
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_bytes(data)
print('Biblioteca: três PDFs originais verificados.')
