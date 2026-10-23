"""Bundle verified PDFs and the native reader into the generated Android project."""
import concurrent.futures
import hashlib
import json
from pathlib import Path
import shutil
import urllib.request


def preparar(root: Path) -> None:
    main = root / 'android/app/src/main'
    if not (main / 'AndroidManifest.xml').is_file():
        raise ValueError('Extraia primeiro o projeto Android')
    manifest = json.loads((root / 'android-pdf/documents.json').read_text())
    source = f"https://raw.githubusercontent.com/{manifest['repository']}/{manifest['commit']}/www/"

    def copy_document(item):
        target = main / 'assets/public' / item['path']
        target.parent.mkdir(parents=True, exist_ok=True)
        content = None
        # Existing copies are reusable only when their hash matches the pinned source.
        if target.is_file() and hashlib.sha256(target.read_bytes()).hexdigest() == item['sha256']:
            content = target.read_bytes()
        else:
            error = None
            for attempt in range(3):
                try:
                    with urllib.request.urlopen(source + item['path'], timeout=30) as response:
                        content = response.read()
                    break
                except Exception as current:
                    error = current
            if content is None:
                raise RuntimeError(f"Falha ao incorporar {item['path']}") from error
        if len(content) != item['size'] or hashlib.sha256(content).hexdigest() != item['sha256']:
            raise ValueError(f"PDF divergente: {item['path']}")
        if not content.startswith(b'%PDF'):
            raise ValueError(f"Resposta inválida: {item['path']}")
        target.write_bytes(content)

    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        list(pool.map(copy_document, manifest['documents']))
    sheets = sorted((root / 'recursos/pdfs').glob('folha-servita-*.pdf'))
    if len(sheets) != 13:
        raise ValueError('Esperadas 12 folhas mensais e uma anual')
    target = main / 'assets/public/recursos/pdfs'
    target.mkdir(parents=True, exist_ok=True)
    for sheet in sheets:
        if not sheet.read_bytes().startswith(b'%PDF'):
            raise ValueError(f'Folha inválida: {sheet.name}')
        shutil.copyfile(sheet, target / sheet.name)
    java = main / 'java/br/org/servitas/familia'
    java.mkdir(parents=True, exist_ok=True)
    for name in ['PdfLink', 'LocalPdfActivity', 'LocalPdfStore', 'LocalPdfPrintAdapter']:
        shutil.copyfile(root / f'android-pdf/{name}.java', java / f'{name}.java')
    tests = root / 'android/app/src/androidTest/java/br/org/servitas/familia/validation'
    tests.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(root / 'android-pdf/PdfInstrumentation.java', tests / 'PdfInstrumentation.java')
    gradle = root / 'android/app/build.gradle'
    text = gradle.read_text()
    if 'testInstrumentationRunner' not in text:
        text = text.replace('defaultConfig {', 'defaultConfig {\n        testInstrumentationRunner "br.org.servitas.familia.validation.PdfInstrumentation"', 1)
    if "testBuildType" not in text:
        text = text.replace('android {', "android {\n    testBuildType 'release'", 1)
    gradle.write_text(text)
    path = main / 'AndroidManifest.xml'
    xml = path.read_text()
    if '.LocalPdfActivity' not in xml:
        xml = xml.replace('</application>', '<activity android:name=".LocalPdfActivity" android:exported="false" />\n</application>')
    path.write_text(xml)
    print(f"Leitor incorporado: {len(manifest['documents']) + len(sheets)} PDFs locais, leitura e impressão sem rede.")


if __name__ == '__main__':
    preparar(Path(__file__).resolve().parents[1])
