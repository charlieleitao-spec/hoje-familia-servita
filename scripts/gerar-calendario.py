#!/usr/bin/env python3
"""Gera calendário assinável da Família Servita a partir do santoral.json."""
import argparse
import json
from datetime import date, datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASE = "https://charlieleitao-spec.github.io/hoje-familia-servita/"
def esc(value):
    return str(value).replace("\\", "\\\\").replace("\n", "\\n").replace(",", "\\,").replace(";", "\\;")
def line(value):
    # RFC 5545: dobrar linhas a 75 octetos, sem dividir caracteres UTF-8.
    out, current = [], ""
    for char in value:
        if len((current + char).encode("utf-8")) > 75:
            out.append(current)
            current = " " + char
        else:
            current += char
    out.append(current)
    return "\r\n".join(out)
def event(uid, day, title, description, stamp):
    return ["BEGIN:VEVENT", "UID:" + uid, "DTSTAMP:" + stamp,
            "DTSTART;VALUE=DATE:" + day.strftime("%Y%m%d"),
            "DTEND;VALUE=DATE:" + (day + timedelta(days=1)).strftime("%Y%m%d"),
            "SUMMARY:" + esc(title), "DESCRIPTION:" + esc(description),
            "URL:" + BASE, "TRANSP:TRANSPARENT", "END:VEVENT"]
def generate(source, output, start_year, end_year):
    if start_year > end_year:
        raise ValueError("O ano inicial deve ser menor ou igual ao final")
    records = json.loads(source.read_text(encoding="utf-8"))
    stamp = datetime(start_year, 1, 1, tzinfo=timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    rows = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Familia Servita//Santoral OSM//PT-BR",
            "CALSCALE:GREGORIAN", "METHOD:PUBLISH",
            "X-WR-CALNAME:Família Servita — Santoral e Celebrações",
            "X-WR-CALDESC:Santoral e celebrações servitas; sábados marianos como indicação devocional."]
    for year in range(start_year, end_year + 1):
        occupied = set()
        for record in records:
            try:
                day = date(year, int(record["month"]), int(record["day"]))
            except (ValueError, KeyError, TypeError):
                continue
            occupied.add(day)
            title = record.get("title") or record.get("name")
            if not title:
                continue
            if record.get("categoria") == "servo_de_deus":
                desc = "Memória biográfica de Servo de Deus; não constitui celebração litúrgica aprovada. Consulte a biografia e a oração devocional no aplicativo."
            else:
                grade = str(record.get("grau", "")).replace("_", " ").strip()
                desc = ("Grau: " + grade.capitalize() + ". " if grade else "") + "Celebração da Família Servita. Consulte os textos e informações no aplicativo."
            rows.extend(event(f"servita-{record.get('id', day.strftime('%m%d'))}-{year}@familiaservita", day, title, desc, stamp))
        # Indicação devocional, NÃO afirma celebração litúrgica em sábados impedidos.
        day = date(year, 1, 1)
        while day.year == year:
            if day.weekday() == 5 and day not in occupied:
                rows.extend(event(f"sabado-mariano-{day:%Y%m%d}@familiaservita", day,
                    "Sábado Mariano — recordação devocional",
                    "Recordação devocional, não celebração litúrgica agendada. A memória facultativa de Santa Maria no sábado só pode ser celebrada quando as normas e o calendário litúrgico do dia o permitem. Consulte o calendário oficial.", stamp))
            day += timedelta(days=1)
    rows.append("END:VCALENDAR")
    output.write_bytes(("\r\n".join(line(row) for row in rows) + "\r\n").encode("utf-8"))
if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=ROOT / "santoral.json")
    parser.add_argument("--output", type=Path, default=ROOT / "servitas.ics")
    parser.add_argument("--start-year", type=int, default=2026)
    parser.add_argument("--end-year", type=int, default=2031)
    args = parser.parse_args()
    generate(args.source, args.output, args.start_year, args.end_year)
