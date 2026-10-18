# Recursos da Família Servita

Gerados de `santoral.json`, já sincronizado pelo Liturgia OSM. Não editar os
resultados manualmente. Biografias e orações não são incluídas nesta exportação.

- `pdfs/`: 12 folhas A4 e o PDF anual reunido.
- `santoral-servita.json`: schema_version, fonte, hash da origem e celebrações.
- Cada celebração tem id, mes, dia, nome, categoria, genero, grau,
  grau_confirmado, exibir_selo e nota_data quando existente.
- O id mantém a correspondência com a ficha do aplicativo. `exibir_selo=false`
  impede rótulos inadequados em celebrações marianas, coletivas e institucionais.
- Datas e graus reproduzem a fonte. A celebração litúrgica depende das normas
  e precedências do calendário oficial. Março não tem celebração própria na base.
- Ao reutilizar, citar "Liturgia OSM / Família Servita" e a fonte indicada no JSON.
  Este arquivo não concede direitos sobre outros conteúdos do aplicativo.

Para gerar novamente a partir da raiz do repositório:

```sh
python3 -m pip install -r scripts/requirements-recursos.txt
python3 scripts/gerar-recursos.py
```

Requer as fontes DejaVu em `/usr/share/fonts/truetype/dejavu/`.
O pacote não contém uma segunda cópia do santoral completo: usar sempre a base
atual do repositório, sincronizada a partir do Liturgia OSM.
