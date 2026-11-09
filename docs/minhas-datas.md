# Minhas datas — Hoje na Família Servita

Base: `c3b0ad216b2283085d95811276ebeb809e0826d2`, Android 2.9.37 (318).

No cartão da celebração, **Guardar esta data** marca o dia com o ano escolhido. Em **Calendário → Minhas datas**, a pessoa consulta, abre ou remove as marcações. Datas futuras e de hoje aparecem antes das passadas. O calendário sinaliza os dias guardados com um traço inferior e um rótulo acessível.

A lista é local, limitada a 100 datas e sem duplicação. A exclusão remove apenas a marcação. Títulos são consultados na base existente; não há uma segunda cópia editorial do santoral. A marcação não se repete anualmente e não dispara notificações. A interface informa que as datas ficam neste aparelho. Armazenamento recusado, cheio ou corrompido é sinalizado sem declarar uma gravação bem-sucedida nem sobrescrever dados inválidos.

A alteração atualiza `index.html` e o cache web. Não muda a identidade PWA, o APK distribuído, os textos litúrgicos, o santoral, o ICS ou os documentos. No aplicativo Android existente, a página inicial é consultada no endereço web do projeto; não é necessária uma nova compilação Android para esta interface.

Verificações: JavaScript/JSON/cache; 24 testes já existentes; testes DOM incluindo gravação, remoção, persistência, datas em anos diferentes, ordem, abertura e falhas de armazenamento. A conferência em navegador utiliza Service Worker real, recarrega a página em modo offline e testa abertura/remoção em 320, 390 e 768 px. Os resultados da execução estão no PR.

Os arquivos ICS/ZIP continuam identificados como dependentes de internet no aplicativo Android. Não foi declarada correção de horário de ICS nem implementação de notificações.
