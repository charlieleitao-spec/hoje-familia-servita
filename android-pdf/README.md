Leitor PDF do Hoje na Família Servita 2.9.32

Adaptação dos fontes do leitor Android da Liturgia OSM 4.9.117, commit b2b6a61f2878e4e841bebee9ca1ab199d9e4f576. Incorpora os mesmos controles de leitura, ampliação, navegação por página e impressão dos bytes originais.

O build incorpora 63 PDFs litúrgicos daquela revisão, conferidos por SHA-256, e as 13 folhas mensais/anual deste projeto. Não são exigidas permissão de armazenamento nem instalação de um visualizador externo para essas coleções.

O leitor só recebe caminhos locais resolvidos de endereços conhecidos. ZIP, ICS, CNBB e outros links continuam no tratamento externo existente. No navegador, a abertura de PDFs continua usando os endereços públicos.

Os PDFs ficam offline no APK. A navegação geral, as fichas e o calendário preservam o funcionamento atual: dependem da primeira consulta online e dos conteúdos já salvos; esta mudança não torna todo o aplicativo offline desde a instalação.

Validação necessária antes da integração: compilação Android, abertura por links normais e target=_blank, navegação, ampliação, impressão, cancelamento com retorno ao leitor e confirmação no Samsung A56.
