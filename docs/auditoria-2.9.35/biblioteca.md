# Biblioteca Servita — páginas e atualização

## Visto no código e no APK

[biblioteca-servita.html](https://github.com/charlieleitao-spec/hoje-familia-servita/blob/fc95e842a92bb2e727e19a1631e76bc7eec96f74/biblioteca-servita.html) monta #page a partir de start_page do catálogo. [PdfLink.java](https://github.com/charlieleitao-spec/hoje-familia-servita/blob/fc95e842a92bb2e727e19a1631e76bc7eec96f74/android-pdf/PdfLink.java) interpreta o fragmento; MainActivity repassa o número no extra page; [LocalPdfActivity](https://github.com/charlieleitao-spec/hoje-familia-servita/blob/fc95e842a92bb2e727e19a1631e76bc7eec96f74/android-pdf/LocalPdfActivity.java#L84) converte página humana para índice zero e limita ao tamanho do PDF.

| Documento | Página física do PDF | Índice interno |
|---|---:|---:|
| Constituições | 43 | 42 |
| Regra de Santo Agostinho | 25 | 24 |
| Diretório Geral | 131 | 130 |

O volume empacotado tem 152 páginas: os três destinos estão dentro do intervalo. São páginas físicas, não necessariamente os números impressos no texto. Os cinco documentos do catálogo usam três arquivos PDF. Biblioteca e Somos Servos empacotados coincidem byte a byte com os HTMLs da tag auditada.

[LocalPdfStore.java](https://github.com/charlieleitao-spec/hoje-familia-servita/blob/fc95e842a92bb2e727e19a1631e76bc7eec96f74/android-pdf/LocalPdfStore.java) copia novamente o PDF dos assets em cada abertura; não depende de download ou de uma cópia antiga no cache. O build incorpora Biblioteca, catálogo e PDFs. Assim, a disponibilidade após uma atualização Android concluída é sustentada pelo código, inclusive sem consulta online prévia. A atualização efetiva por cima não foi executada nesta auditoria.

PdfLinkTest já contém 43/25/131; PdfInstrumentation já verifica o catálogo offline e a abertura do leitor nessas páginas via Intent. Esses testes foram lidos, não executados aqui. A instrumentação não substitui verificar o clique completo no link da Biblioteca.

## Proposta, ainda sem implementar

Manter a lógica existente. Especificar a nota por ambiente: “No aplicativo Android, os cinco documentos estão incluídos na instalação ou atualização concluída e podem ser lidos sem internet.” Na versão web, explicar separadamente a dependência de um carregamento e cache completos. A frase atual é ampla demais se aplicada igualmente a Android e navegador. Garantir acesso à Biblioteca pelo fallback é uma pendência do parecer offline.

## Sem teste em execução

Instalar 2.9.35 sobre uma versão anterior sem desinstalar, abrir cada link em modo avião, limpar apenas cache e repetir; conferir páginas 43/25/131 visualmente. Não confirmar preservação de dados/assinatura por simples leitura do código.

## Base e limites da auditoria

Auditoria de 28/10/2026; código/tag android-v2.9.35 no commit fc95e842a92bb2e727e19a1631e76bc7eec96f74.
APK público Familia-Servita-2.9.35.apk: SHA-256 5c4388ba0ebebab693e534853048ee5adf31acf3fb8177de3d737ebda1a4ff82, igual ao digest da release. Inspeção estática: contêiner ZIP sem erro de CRC; 79 PDFs abertos com pypdf em modo estrito, 618 páginas. Isso não equivale a renderização ou teste Android. A assinatura v2 informada na análise de origem não foi verificada criptograficamente nesta auditoria.

Nenhum teste em aparelho ou emulador foi executado nesta auditoria. Testes existentes citados abaixo foram apenas lidos. Este PR contém somente documentação: não altera calendário, graus, datas, as 32 páginas, conteúdo litúrgico, downloads PDF/ZIP, CNBB em nova aba ou navegação aprovada na 2.9.31. Sem implementação, publicação ou merge.
