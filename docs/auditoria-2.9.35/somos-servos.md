# Somos Servos — endpoint e falhas

## Visto no código e no APK

URL pública atual em somos-servos.html e no HTML extraído do APK:

https://script.google.com/macros/s/AKfycbwvAnolHxpcUrBZaUXqR7uDVyxt6Jsokk_ZJ4XfRdt3ZPQyTMxkJnIE-U5VgmdMv2kj/exec

A URL não está exclusivamente fixa: [preparar-blog-android.py](https://github.com/charlieleitao-spec/hoje-familia-servita/blob/fc95e842a92bb2e727e19a1631e76bc7eec96f74/scripts/preparar-blog-android.py) permite substituí-la por SOMOS_SERVOS_GAS_URL na compilação; o workflow lê a variável de repositório com esse nome. O endpoint do APK auditado coincide com o HTML versionado.

O fetch usa timeout de 15 segundos, valida resposta HTTP, JSON e esquema, preserva dados salvos e não aceita resposta mais antiga. O catch atual trata genericamente falha de rede, HTTP e dados inválidos; sem cache, orienta apenas conectar à internet. Isso pode confundir quando o serviço está indisponível mesmo com conexão. Ter HTML local não significa ter postagens na primeira abertura; o cache guarda títulos/links/metadados, não as matérias completas nem todas as imagens.

## Proposta avaliada

Mensagem sugerida: “Não foi possível atualizar as postagens. Verifique a conexão e tente novamente. Se o problema continuar, o serviço pode estar temporariamente indisponível ou o endereço de atualização pode precisar ser atualizado.” Havendo cache, acrescentar “Exibindo dados salvos de [data].” Preservar recursos e dados já salvos. Não afirmar que a URL mudou apenas com base numa falha de fetch.

Se a URL pública mudar, conferir e atualizar a constante do site e a variável de build, gerar APK posterior e verificar ambos os ambientes. O APK já instalado conserva o endpoint embutido; modificar só a variável não o corrige retroativamente. Preferir manter uma implantação existente quando possível. A proposta não reimplanta nem chama o serviço.

## Sem teste em execução

Endpoint ao vivo, resposta HTML em vez de JSON, erro HTTP, timeout, URL desativada, armazenamento indisponível e reapresentação do cache. Nenhum cenário foi executado em aparelho ou por chamada ao endpoint nesta auditoria.

## Base e limites da auditoria

Auditoria de 28/10/2026; código/tag android-v2.9.35 no commit fc95e842a92bb2e727e19a1631e76bc7eec96f74.
APK público Familia-Servita-2.9.35.apk: SHA-256 5c4388ba0ebebab693e534853048ee5adf31acf3fb8177de3d737ebda1a4ff82, igual ao digest da release. Inspeção estática: contêiner ZIP sem erro de CRC; 79 PDFs abertos com pypdf em modo estrito, 618 páginas. Isso não equivale a renderização ou teste Android. A assinatura v2 informada na análise de origem não foi verificada criptograficamente nesta auditoria.

Nenhum teste em aparelho ou emulador foi executado nesta auditoria. Testes existentes citados abaixo foram apenas lidos. A auditoria inicial não alterou o aplicativo: não altera calendário, graus, datas, as 32 páginas, conteúdo litúrgico, downloads PDF/ZIP, CNBB em nova aba ou navegação aprovada na 2.9.31. Sem publicação ou merge. O andamento posterior está registrado abaixo.

## Andamento após autorização para seguir

Implementação preparada: a mensagem de falha contempla indisponibilidade do serviço e possível necessidade de atualizar o endereço, conservando data e dados salvos. Endpoint e lógica de cache permanecem iguais. Os 18 testes Node e 5 testes Python existentes passaram localmente, com respostas simuladas; não houve chamada ao serviço nem teste Android.

PR mantido em rascunho. Sem merge, release, alteração litúrgica ou mudança dos destinos e comportamentos de download/navegação aprovados.
