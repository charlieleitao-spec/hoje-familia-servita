# Rotação do leitor PDF — parecer

## Visto no código

[preparar-pdfs-android.py](https://github.com/charlieleitao-spec/hoje-familia-servita/blob/fc95e842a92bb2e727e19a1631e76bc7eec96f74/scripts/preparar-pdfs-android.py#L72) registra LocalPdfActivity sem screenOrientation nem configChanges; o screenOrientation=unspecified do ZIP pertence a MainActivity. Não há bloqueio explícito de orientação do leitor nessa declaração.

[LocalPdfActivity.java](https://github.com/charlieleitao-spec/hoje-familia-servita/blob/fc95e842a92bb2e727e19a1631e76bc7eec96f74/android-pdf/LocalPdfActivity.java#L141) já salva pageIndex e zoom em onSaveInstanceState. onCreate reabre o mesmo path do Intent, restaura a página salva limitada ao número de páginas e reaplica o zoom depois de renderPage. O comportamento esperado numa recriação por rotação é manter a página e a ampliação. Não é necessário propor nova implementação desse mecanismo.

O deslocamento horizontal/vertical do zoom (panX/panY) não é salvo e volta ao centro. Isso não implica perda de página. renderPage recalcula o bitmap conforme a nova largura e libera o anterior.

Na auditoria inicial, a instrumentação verificava página e zoom após cancelar impressão, sem rotação nem recriação de Activity. Esta proposta acrescentou a verificação de rotação descrita abaixo.

## Proposta

Manter o código até validar o comportamento em execução. Se surgir regressão, registrar a sequência e a evidência antes de modificar. Não travar orientação como solução presumida.

## Sem teste em execução

Abrir Biblioteca na página 43, avançar à 44, ampliar a 125%, girar retrato/paisagem/retrato e conferir página e zoom. Repetir nas páginas 25/131, limites do documento e retorno ao app. Separar rotação real de uma recriação forçada. Confirmar ausência de erro e retorno após impressão. A posição de pan deve ser registrada como não preservada pelo código atual. Na auditoria inicial esses cenários não haviam sido executados. A verificação posterior em emulador está registrada abaixo; repetição nas páginas 25/131 durante rotação e em aparelho físico continuam pendentes.

## Base e limites da auditoria

Auditoria de 28/10/2026; código/tag android-v2.9.35 no commit fc95e842a92bb2e727e19a1631e76bc7eec96f74.
APK público Familia-Servita-2.9.35.apk: SHA-256 5c4388ba0ebebab693e534853048ee5adf31acf3fb8177de3d737ebda1a4ff82, igual ao digest da release. Inspeção estática: contêiner ZIP sem erro de CRC; 79 PDFs abertos com pypdf em modo estrito, 618 páginas. Isso não equivale a renderização ou teste Android. A assinatura v2 informada na análise de origem não foi verificada criptograficamente nesta auditoria.

Na auditoria inicial, os testes Android haviam sido apenas lidos. A execução posterior em emulador está registrada abaixo. A auditoria inicial não alterou o aplicativo: não altera calendário, graus, datas, as 32 páginas, conteúdo litúrgico, downloads PDF/ZIP, CNBB em nova aba ou navegação aprovada na 2.9.31. Sem publicação ou merge. O andamento posterior está registrado abaixo.

## Andamento após autorização para seguir

Preparado teste de instrumentação: abre página 43, avança para 44, amplia a 125%, gira para paisagem e retrato, exige recriação da Activity e confere página, ampliação e orientação. Inclui workflow de emulador API 35 com build debug sem chave de release e sem publicação. Leitor de produção inalterado. YAML e espaços verificados localmente; compilação e execução Android concluídas com sucesso no CI.

PR mantido em rascunho. Sem merge, release, alteração litúrgica ou mudança dos destinos e comportamentos de download/navegação aprovados.

## Evidência de execução em emulador — 28/10/2026

[Execução 40676668674](https://github.com/charlieleitao-spec/hoje-familia-servita/actions/runs/40676668674), commit 60684e5aa1dc35969278dd3c284227705b644a16, Android 15/API 35, perfil Pixel 6, APK debug, Wi-Fi e dados desativados no script. Job concluído com sucesso. Trechos do log:

```text
PASS: 79 PDFs; 618 pages; original print bytes; zoom; Android printing; cancel and return.
PASS: PDF rotation retains page 44 and zoom 125% in landscape and portrait.
```

A rotação exigiu uma nova instância da Activity nas duas orientações e verificou página 44, ampliação 125% e orientação efetiva. Não se trata apenas de inspeção do código. A mesma instrumentação verificou o catálogo offline e páginas iniciais 43, 25 e 131 por Intent, além dos PDFs e impressão. O teste usou a base deste PR; as demais melhorias de interface estão em PRs separados e não foram exercitadas juntas no Android. Artefato de evidência: pdf-rotation-api35, nesta execução.

Continuam sem teste nesta etapa: Samsung A56, atualização por cima, clique completo da Biblioteca até o leitor, rotação nas páginas 25/131 e comportamento das novas telas dos outros PRs. Nenhuma publicação ou merge foi realizado.
