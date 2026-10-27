# Melhorias para o uso diário

Proposta com cinco recursos, sem alteração das datas, dos textos litúrgicos, da identidade PWA, dos workflows ou do APK distribuído.

- Nos dias sem memória cadastrada, três reflexões originais sobre Maria, fraternidade e serviço se alternam por data civil, com oração pessoal e identificação devocional. A celebração principal continua como Liturgia do dia. Erros no carregamento mantêm o aviso de erro.
- O Calendário abre um mês visual com navegação, seleção de dia, indicação das memórias cadastradas e retorno a hoje. O seletor de data continua disponível. Os pontos não definem precedência litúrgica.
- Compartilhar esta data abre o compartilhamento nativo. Na ausência dele, copia o link; se a cópia não estiver disponível, apresenta o endereço selecionável. O cancelamento não dispara outro compartilhamento.
- A data e a hora consultada ficam no armazenamento local. Na próxima abertura, a pessoa pode continuar ou dispensar a leitura anterior. Links recebidos com uma data mantêm prioridade; bloqueio de armazenamento não impede o uso. I e II Vésperas possuem identificadores distintos para a retomada.
- O acesso de formação aponta para páginas específicas de oito pessoas ou celebrações, ou para a seção correspondente da Biblioteca quando não há artigo específico cadastrado.

As reflexões e a interface estão no próprio index.html, sem novas dependências de execução ou chamadas de rede. O cache recebeu uma revisão interna para disponibilizar a atualização offline. Os links do blog requerem conexão. Não houve aumento da versão Android nem publicação de release.

## Verificação desta proposta

- `node --test tests/uso-diario.test.cjs`: cinco testes dos recursos novos.
- `node scripts/check-web.cjs`: sintaxe, identidade PWA e integridade dos arquivos offline.
- `git diff --check`: sem erros de espaços.
- `tests/uso-diario-dom.cjs`: verifica a interface em DOM simulado, usando a base real. Cobre reflexão e falha de carga; seleção mensal; compartilhamento nativo, cancelamento, cópia e alternativa manual; memória original; formação relacionada; retomada da hora escolhida, hora removida e armazenamento negado. Não substitui inspeção visual.

Para executar a verificação de DOM sem acrescentar dependências ao aplicativo:

```sh
npm install --prefix /tmp/servita-dom-qa jsdom --no-package-lock --no-audit --no-fund
NODE_PATH=/tmp/servita-dom-qa/node_modules node tests/uso-diario-dom.cjs
```

Os nove destinos do blog foram verificados nesta revisão. Não foram repetidos os roteiros anteriores do Samsung A56, os testes de PDF ou o build Android. A proposta ainda requer conferência visual da nova interface no computador e no celular. Não houve publicação desta proposta.
