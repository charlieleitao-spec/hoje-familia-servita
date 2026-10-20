#!/usr/bin/env python3
"""Gera a folha mensal (PDF, A4) das celebrações servitas a partir do santoral.json.

Uso:
    python3 gerar-folha-mensal.py caminho/santoral.json pasta_saida [--mes N]

Regras de exibição (alinhadas ao app):
- linha de data: "data · grau"
- selo ao lado do nome: Santo/Santa, Beato/Beata, Servo de Deus
- exibir_selo == false (ou categoria nula) não mostra selo
- se o título já começa com Santo/Santa/São, o selo é omitido (evita repetição)
"""
import json
import os
import sys

from functools import partial
from reportlab.pdfgen.canvas import Canvas
from pypdf import PdfReader, PdfWriter
from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (HRFlowable, KeepTogether, Paragraph,
                                SimpleDocTemplate, Spacer, Table, TableStyle)

FONT_DIR = "/usr/share/fonts/truetype/dejavu/"
pdfmetrics.registerFont(TTFont("Serif", FONT_DIR + "DejaVuSerif.ttf"))
pdfmetrics.registerFont(TTFont("Serif-Bold", FONT_DIR + "DejaVuSerif-Bold.ttf"))
pdfmetrics.registerFont(TTFont("Sans", FONT_DIR + "DejaVuSans.ttf"))
pdfmetrics.registerFont(TTFont("Sans-Bold", FONT_DIR + "DejaVuSans-Bold.ttf"))
pdfmetrics.registerFontFamily("Serif", normal="Serif", bold="Serif-Bold")
pdfmetrics.registerFontFamily("Sans", normal="Sans", bold="Sans-Bold")

ARQUIVOS_MESES = ["janeiro", "fevereiro", "marco", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"]
MESES = ["janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho",
         "agosto", "setembro", "outubro", "novembro", "dezembro"]

GRAU = {
    "solenidade": "Solenidade",
    "festa": "Festa",
    "memoria": "Memória",
    "memoria_facultativa": "Memória facultativa",
    "comemoracao": "Comemoração",
}

# Identidade visual: azul profundo e dourado discreto (tons usados no blog)
AZUL = colors.HexColor("#1F3A5F")
DOURADO = colors.HexColor("#B08A2E")
CINZA = colors.HexColor("#555555")
FILETE = colors.HexColor("#D9D9D9")
FUNDO_SOLENE = colors.HexColor("#F4EEDC")


def selo(e):
    """Retorna o selo (rótulo da categoria) ou None."""
    if e.get("exibir_selo") is False:
        return None
    cat, gen = e.get("categoria"), e.get("genero")
    if not cat:
        return None
    titulo = (e.get("title") or "").strip()
    if cat == "servo_de_deus":
        return "Servo de Deus"
    if cat == "beato":
        return "Beata" if gen == "f" else "Beato"
    if cat == "santo":
        if titulo.lower().startswith(("santo", "santa", "são", "sao")):
            return None
        return "Santa" if gen == "f" else "Santo"
    return None


def linha_data(e):
    grau = "Memória biográfica" if e.get("categoria") == "servo_de_deus" else GRAU.get(e.get("grau"), "")
    nota = e.get("nota_data")
    base = f"{e['day']} de {MESES[e['month'] - 1]}"
    partes = [base] + ([grau] if grau else [])
    texto = " · ".join(partes)
    if nota:
        texto += f" · {nota}"
    return texto


def estilos():
    return {
        "titulo": ParagraphStyle("titulo", fontName="Serif-Bold", fontSize=26,
                                 leading=30, textColor=AZUL, alignment=TA_LEFT),
        "sub": ParagraphStyle("sub", fontName="Sans", fontSize=10, leading=14,
                              textColor=CINZA),
        "dia": ParagraphStyle("dia", fontName="Serif-Bold", fontSize=26,
                              leading=28, textColor=DOURADO, alignment=1),
        "nome": ParagraphStyle("nome", fontName="Serif-Bold", fontSize=14,
                               leading=18, textColor=AZUL),
        "data": ParagraphStyle("data", fontName="Sans", fontSize=9.5, leading=13,
                               textColor=CINZA),
        "vazio": ParagraphStyle("vazio", fontName="Serif", fontSize=12,
                                leading=17, textColor=CINZA),
    }


def esc(t):
    return (t or "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def linha_celebracao(e, st):
    s = selo(e)
    nome = esc(e["title"])
    if s:
        nome += (f' &nbsp;<font name="Sans-Bold" size="8" color="#B08A2E">'
                 f'{s.upper()}</font>')
    celula_dia = Paragraph(str(e["day"]), st["dia"])
    celula_txt = [Paragraph(nome, st["nome"]), Paragraph(esc(linha_data(e)), st["data"])]
    t = Table([[celula_dia, celula_txt]], colWidths=[22 * mm, 148 * mm])
    estilo = [
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("TOPPADDING", (0, 0), (-1, -1), 7),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
        ("LINEBELOW", (0, 0), (-1, 0), 0.5, FILETE),
    ]
    if e.get("grau") in ("solenidade", "festa"):
        estilo.append(("BACKGROUND", (0, 0), (-1, -1), FUNDO_SOLENE))
    t.setStyle(TableStyle(estilo))
    return KeepTogether([t])


def rodape(canvas, doc):
    canvas.saveState()
    canvas.setFont("Sans", 7.5)
    canvas.setFillColor(CINZA)
    canvas.drawString(20 * mm, 12 * mm,
                      "Família Servita · Santoral da Ordem dos Servos de Maria")
    canvas.drawRightString(190 * mm, 12 * mm, "Fonte: app Liturgia OSM")
    canvas.setStrokeColor(DOURADO)
    canvas.setLineWidth(1.2)
    canvas.line(20 * mm, 17 * mm, 190 * mm, 17 * mm)
    canvas.restoreState()


def gerar_mes(dados, mes, pasta):
    st = estilos()
    nome_arq = os.path.join(pasta, f"folha-servita-{mes:02d}-{ARQUIVOS_MESES[mes - 1]}.pdf")
    doc = SimpleDocTemplate(
        nome_arq, pagesize=A4, leftMargin=20 * mm, rightMargin=20 * mm,
        topMargin=18 * mm, bottomMargin=24 * mm,
        title=f"Celebrações servitas de {MESES[mes - 1]}",
        author="Família Servita")
    story = [
        Paragraph(MESES[mes - 1].capitalize(), st["titulo"]),
        Paragraph("Celebrações próprias da Família Servita", st["sub"]),
        Paragraph("Datas de referência. A celebração litúrgica segue as normas e precedências do calendário oficial.", st["sub"]),
        Spacer(1, 4),
        HRFlowable(width="100%", thickness=1.2, color=DOURADO, spaceAfter=8),
    ]
    itens = sorted((e for e in dados if e["month"] == mes), key=lambda e: e["day"])
    if not itens:
        story.append(Spacer(1, 20))
        story.append(Paragraph(
            "Neste mês não há celebrações próprias no santoral servita.", st["vazio"]))
    for e in itens:
        story.append(linha_celebracao(e, st))
    doc.build(story, onFirstPage=rodape, onLaterPages=rodape, canvasmaker=partial(Canvas, invariant=1))
    return nome_arq, len(itens)


def main():
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    caminho, pasta = sys.argv[1], sys.argv[2]
    mes_unico = int(sys.argv[sys.argv.index("--mes") + 1]) if "--mes" in sys.argv else None
    os.makedirs(pasta, exist_ok=True)
    with open(caminho, encoding="utf-8") as f:
        dados = json.load(f)
    arquivos = []
    for mes in ([mes_unico] if mes_unico else range(1, 13)):
        arq, n = gerar_mes(dados, mes, pasta)
        arquivos.append(arq)
        if len(PdfReader(arq).pages) != 1:
            raise ValueError("A folha mensal deve caber em uma página: " + arq)
        print(f"{os.path.basename(arq)}: {n} celebração(ões)")
    if not mes_unico:
        writer = PdfWriter()
        for arq in arquivos:
            writer.append(arq)
        writer.add_metadata({"/Title": "Calendário da Família Servita", "/Author": "Família Servita"})
        with open(os.path.join(pasta, "folha-servita-ano-completo.pdf"), "wb") as target:
            writer.write(target)


if __name__ == "__main__":
    main()
