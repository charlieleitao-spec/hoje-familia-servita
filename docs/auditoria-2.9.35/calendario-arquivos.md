# Arquivos do cartão do calendário — parecer e proposta

## Visto no código e no APK

[index.html](https://github.com/charlieleitao-spec/hoje-familia-servita/blob/fc95e842a92bb2e727e19a1631e76bc7eec96f74/index.html#L301) oferece “Baixar calendário (.ics)” e “Baixar pacote completo (.zip)”. Ambos usam caminhos relativos. O MainActivity identifica essas extensões e as encaminha ao aplicativo externo; PdfLink não resolve esses formatos. Os arquivos não estão nos assets do APK. O sw.js não os inclui no pré-cache; downloads externos não garantem o cache da WebView.

O servitas.ics do repositório tem 227.733 bytes, cerca de 222 KiB. É pequeno perante o APK de 7.267.827 bytes, mas apenas copiá-lo para assets não o tornaria acessível ao calendário externo.

## Proposta, ainda sem implementar

No curto prazo, manter destinos e comportamento aprovados e propor apenas os rótulos “Baixar calendário (.ics) — requer internet” e “Baixar pacote completo (.zip) — requer internet”. Não atribuir essa exigência aos PDFs locais.

Vale considerar embutir o ICS, sem mudar seus eventos, num trabalho posterior: exigiria expor uma cópia por content URI/FileProvider, permissão temporária de leitura e aplicativo capaz de importar text/calendar. Como isso alteraria o caminho de download/navegação aprovado na 2.9.31, não implementar neste parecer. Registrar também a versão do calendário empacotado para não prometer atualizações offline. Embutir o ZIP não é necessário para acessar os PDFs já locais.

## Sem teste em execução

Download de ICS/ZIP no navegador, importação em aplicativo de calendário, ausência de aplicativo compatível e modo avião. Nenhum teste de importação, botão ou ícone em aparelho foi realizado. Sem alteração de datas, graus ou eventos.

## Base e limites da auditoria

Auditoria de 28/10/2026; código/tag android-v2.9.35 no commit fc95e842a92bb2e727e19a1631e76bc7eec96f74.
APK público Familia-Servita-2.9.35.apk: SHA-256 5c4388ba0ebebab693e534853048ee5adf31acf3fb8177de3d737ebda1a4ff82, igual ao digest da release. Inspeção estática: contêiner ZIP sem erro de CRC; 79 PDFs abertos com pypdf em modo estrito, 618 páginas. Isso não equivale a renderização ou teste Android. A assinatura v2 informada na análise de origem não foi verificada criptograficamente nesta auditoria.

Nenhum teste em aparelho ou emulador foi executado nesta auditoria. Testes existentes citados abaixo foram apenas lidos. Este PR contém somente documentação: não altera calendário, graus, datas, as 32 páginas, conteúdo litúrgico, downloads PDF/ZIP, CNBB em nova aba ou navegação aprovada na 2.9.31. Sem implementação, publicação ou merge.
