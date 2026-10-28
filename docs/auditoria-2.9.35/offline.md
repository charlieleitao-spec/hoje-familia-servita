# Comportamento offline — parecer e proposta

## Visto no código e no APK

- O MainActivity de android-source.zip aponta para file:///android_asset/offline.html em caso de erro. Esse é o código-base antigo, não a Activity final.
- O workflow aplica android-release.patch e depois substitui a Activity por [MainActivity-revisado.java](https://github.com/charlieleitao-spec/hoje-familia-servita/blob/fc95e842a92bb2e727e19a1631e76bc7eec96f74/MainActivity-revisado.java#L36). HOME continua online; falha de rede na navegação interna principal redireciona para HOME + somos-servos.html. Esse HTML é servido de asset pelo interceptador. O DEX contém a URL do Somos Servos e não contém a string offline.html.
- O APK tem offline.html, Somos Servos, Biblioteca, catálogo e PDFs; não tem index.html nem servita.json. Presença do arquivo offline.html não prova que ele seja a tela usada.
- O site registra [sw.js](https://github.com/charlieleitao-spec/hoje-familia-servita/blob/fc95e842a92bb2e727e19a1631e76bc7eec96f74/sw.js), que inclui index e servita.json no cache. Portanto uma visita online anterior pode permitir funcionamento sem rede; não se pode prometer isso numa primeira abertura. Não foi validada a interação do Service Worker com os interceptadores nativos.
- A tela fallback atual lista folhas PDF, mas não oferece link direto à Biblioteca. PDFs embutidos não significam que todos sejam facilmente descobertos nessa tela.

## Proposta, ainda sem implementar

Prioridade: tornar o fallback explícito e oferecer atalhos aos recursos já locais, usando o mecanismo HTTPS de interceptação existente. Texto sugerido: “Sem conexão. A Biblioteca Servita e os PDFs incluídos no aplicativo continuam disponíveis. As postagens salvas do Somos Servos podem ser consultadas; abrir as matérias e buscar novidades requer internet.” Incluir acesso à Biblioteca, às coleções locais e “Tentar novamente” para HOME.

Alternativa posterior: empacotar cópias idênticas de index.html e servita.json, servindo-as apenas quando a rede falhar. Index tem cerca de 68 KB e JSON 293 KB; auditar também imagens, demais JSONs e páginas referenciadas antes de chamar isso de suporte offline completo. Evitar uma segunda base litúrgica ou alterações editoriais. Precisa de projeto específico para fallback e atualização, preservando a navegação vigente.

## Sem teste em execução

Primeira abertura sem rede; abertura após cache online; cache limpo; perda de conexão; acesso efetivo a todas as coleções a partir do fallback; retorno à HOME. Nenhuma tela foi observada em aparelho nesta auditoria.

## Base e limites da auditoria

Auditoria de 28/10/2026; código/tag android-v2.9.35 no commit fc95e842a92bb2e727e19a1631e76bc7eec96f74.
APK público Familia-Servita-2.9.35.apk: SHA-256 5c4388ba0ebebab693e534853048ee5adf31acf3fb8177de3d737ebda1a4ff82, igual ao digest da release. Inspeção estática: contêiner ZIP sem erro de CRC; 79 PDFs abertos com pypdf em modo estrito, 618 páginas. Isso não equivale a renderização ou teste Android. A assinatura v2 informada na análise de origem não foi verificada criptograficamente nesta auditoria.

Nenhum teste em aparelho ou emulador foi executado nesta auditoria. Testes existentes citados abaixo foram apenas lidos. Este PR contém somente documentação: não altera calendário, graus, datas, as 32 páginas, conteúdo litúrgico, downloads PDF/ZIP, CNBB em nova aba ou navegação aprovada na 2.9.31. Sem implementação, publicação ou merge.
