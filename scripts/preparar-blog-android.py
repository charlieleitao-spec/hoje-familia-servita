"""Incorpora o HTML local depois da extração do android-source.zip."""
import argparse
import os
from pathlib import Path
import re


def preparar(root: Path, endpoint: str = "", allow_unconfigured: bool = False) -> Path:
    html = (root / "somos-servos.html").read_text(encoding="utf-8")
    pattern = r"const API_URL = '([^']*)';"
    current = re.search(pattern, html)
    if current is None:
        raise ValueError("Constante API_URL não encontrada")
    url = endpoint.strip() or current.group(1)
    if not (allow_unconfigured and not url) and not re.fullmatch(r"https://script\.google\.com/macros/s/[A-Za-z0-9_-]+/exec", url):
        raise ValueError("Configure SOMOS_SERVOS_GAS_URL com a URL pública /exec do Web App antes de compilar")
    html = re.sub(pattern, lambda _: "const API_URL = '" + url + "';", html, count=1)
    output = root / "android/app/src/main/assets/somos-servos.html"
    if not (root / "android/app/src/main/AndroidManifest.xml").is_file():
        raise ValueError("Extraia primeiro o projeto Android")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(html, encoding="utf-8")
    return output


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--allow-unconfigured", action="store_true",
                        help="Somente debug: permite URL vazia, mantendo aviso de configuração na tela")
    args = parser.parse_args()
    preparar(Path(__file__).resolve().parents[1], os.environ.get("SOMOS_SERVOS_GAS_URL", ""), args.allow_unconfigured)
    print("Leitor local incorporado ao APK.")
