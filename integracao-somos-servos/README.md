# Somos Servos no Hoje na Família Servita

O link **Postagens do Somos Servos** fica na seção Recursos. No Android,
`MainActivity-revisado.java` intercepta exclusivamente a URL HTTPS interna
`/hoje-familia-servita/somos-servos.html` e devolve o asset empacotado, inclusive
sem rede. A mesma tela é usada no fallback após falha de rede da página inicial.
Não foi adicionada Activity, dependência AndroidX ou ponte JavaScript.

O HTML canônico é `somos-servos.html` na raiz. Seu cache local usa a mesma origem
HTTPS tanto online quanto offline. Só há postagens offline após uma consulta
bem-sucedida nesta tela. Não se captura silenciosamente a navegação no blog.
Os recursos exibem links públicos, sem copiar calendários nem alterar graus.

## Ativação do Apps Script

1. No Google Cloud, ative Blogger API v3 e crie uma chave restrita a essa API.
2. Crie um projeto Apps Script e copie `gas/Code.gs` e `gas/appsscript.json`.
3. Adicione `BLOGGER_API_KEY` nas Propriedades do script. Não insira a chave no
   repositório, no HTML nem nas variáveis do APK.
4. Execute `testarConfiguracao` e autorize requisições externas. O ID do blog é
   resolvido pelo endereço somosservos.blogspot.com e salvo como texto.
5. Implante como Aplicativo da Web: executar como proprietário; acesso para
   qualquer pessoa, incluindo anônimos. Confirme JSON `ok:true` sem login.
6. Insira a URL pública `/exec` na constante `API_URL` de `somos-servos.html`.
   Assim o site e o APK usam a mesma implantação. Alternativamente, para o APK,
   defina a variável de repositório `SOMOS_SERVOS_GAS_URL`; ela tem precedência
   sobre o valor do HTML. O build recusa URL ausente, /dev ou host diferente.
7. Antes de mesclar, teste o fetch no WebView: a abertura direta da URL no
   navegador não comprova CORS. Depois gere nova versão e valide no aparelho.

O workflow incorpora o HTML após descompactar o Android. Não altera o ZIP-fonte,
assinatura ou versionCode nesta etapa. A compilação release exige configurar a
URL e escolher uma versão superior à instalada antes da distribuição.

## Comportamento e limites

GET simples com redirecionamento para script.googleusercontent.com; timeout de
15 segundos. Primeiro exibe o cache; respostas inválidas, erros e HTML de login
não o sobrescrevem. O servidor usa cache de cinco minutos, lock e uma repetição
em HTTP 429/5xx; quando disponível, retorna cópia antiga marcada com `stale`.
O cache do servidor é de melhor esforço. O localStorage conserva títulos, datas,
links e URLs de miniaturas, não corpos de posts nem arquivos PDF/ZIP/imagens.

O GAS retorna envelope de erro mesmo com HTTP 200; o cliente valida `ok` e schema.
Não há setHeader em ContentService. Não use no-cors ou libere acesso universal a
file://: se CORS não funcionar na implantação real, será preciso implementar um
proxy próprio ou uma chamada nativa com endpoint fixo.

O cache desta integração não é compartilhado com Chrome, outro APK ou a origem
file:// anterior. Uma reinstalação sem preservar dados exige nova consulta online.

## Verificação

`node --test integracao-somos-servos/tests/*.test.cjs`

`python3 -m unittest discover -s integracao-somos-servos/tests -p 'test_*.py'`

São testes locais com mocks, sem credenciais Google. Ainda precisam de validação:
implantação real, CORS, compilação APK e reabertura em modo avião no Samsung A56.
Este código só lê posts publicados; não cria postagens ou gatilhos periódicos.

Fontes oficiais:
- https://developers.google.com/blogger/docs/3.0/reference/posts/list
- https://developers.google.com/apps-script/guides/web
- https://developers.google.com/apps-script/guides/content
- https://developer.android.com/develop/ui/views/layout/webapps/load-local-content
