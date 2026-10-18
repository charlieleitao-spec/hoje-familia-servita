#!/usr/bin/env python3
"""Deriva dados públicos e PDFs do santoral já sincronizado do Liturgia OSM."""
import hashlib
import json
import subprocess
import sys
import zipfile
from pathlib import Path
ROOT = Path(__file__).resolve().parent.parent
source = ROOT / 'santoral.json'
records = json.loads(source.read_text())
assert len({r['id'] for r in records}) == len(records)
fields = {'id':'id','month':'mes','day':'dia','title':'nome','categoria':'categoria','genero':'genero','grau':'grau','grau_confirmado':'grau_confirmado','nota_data':'nota_data'}
public = []
for r in records:
    row = {target:r[key] for key,target in fields.items() if key in r}
    row['exibir_selo'] = r.get('exibir_selo', True)
    public.append(row)
output = ROOT / 'recursos'
output.mkdir(exist_ok=True)
result = {'schema_version':1,'titulo':'Santoral da Família Servita','fonte':'Liturgia OSM','fonte_url':'https://github.com/charlieleitao-spec/liturgia-osm/blob/main/www/data/santoral.json','fonte_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'nota':'Datas de referência. Observar o calendário oficial e suas precedências.','celebracoes':public}
(output/'santoral-servita.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
subprocess.run([sys.executable,str(ROOT/'scripts/gerar-folha-mensal.py'),str(source),str(output/'pdfs')],check=True)
with zipfile.ZipFile(output/'folha-servita-pacote.zip','w',zipfile.ZIP_DEFLATED) as z:
    files = [*sorted((output/'pdfs').glob('*.pdf')),output/'santoral-servita.json',output/'LEIAME.md',ROOT/'scripts/gerar-folha-mensal.py',ROOT/'scripts/gerar-recursos.py',ROOT/'scripts/requirements-recursos.txt']
    for f in files:
        name = str(f.relative_to(ROOT))
        info=zipfile.ZipInfo(name,date_time=(2026,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED
        z.writestr(info,f.read_bytes())
print('Recursos gerados:',len(public),'celebrações; 12 folhas, PDF anual, JSON e ZIP.')
