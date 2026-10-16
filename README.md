# Hoje na Família Servita

PWA responsiva com o calendário, o santoral e os textos próprios da Liturgia das Horas da Família Servita.

## Site estável 2.9.15

- calendário diário e próximas celebrações;
- biografias e espiritualidade servita;
- leitor da Liturgia das Horas organizado por hora litúrgica;
- hinos, salmodias, leituras, preces e orações com composição própria;
- instalação no celular com ícones OSM;
- funcionamento offline dos arquivos essenciais;
- navegação acessível entre datas;
- catálogo pesquisável com acesso aos formulários em PDF das Missas Próprias OSM;
- integração com o blog **Somos Servos**;
- tipografia uniforme em todas as páginas, com leitura ajustada para celular;
- índice de Beatos e Servos de Deus atualizado diretamente do santoral canônico;
- correção da exibição dos controles ocultos.

## Revisão 2.9.15

- linha da data da celebração corrigida, evitando conflito com `window.status`;

- datas inválidas não interrompem o calendário;
- títulos e orações dos Servos de Deus reconhecidos pela categoria;
- cópia local do santoral oficial para Causas e Missas sem conexão;
- recuperação do cache em falhas de rede e preservação da identidade PWA;
- links externos tratados sem encerrar o aplicativo quando não há aplicativo compatível.

O santoral local é uma cópia de `liturgia-osm/www/data/santoral.json`; as páginas continuam consultando a fonte online quando disponível. Os PDFs externos requerem internet.

## Observação editorial

Os textos-fonte são preservados integralmente. Registros que fornecem antífonas, mas não trazem os respectivos salmos, permanecem sinalizados e não são completados por reconstrução automática.

## Publicação

O projeto é publicado pelo GitHub Pages a partir da branch `main` e da pasta raiz.

## Aplicativo Android

O diretório `android-source.zip` contém as fontes do aplicativo próprio **Família Servita**. O workflow `build-android.yml` gera o APK 2.9.15 em pull requests como artefato de teste e publica a versão assinada após a integração à `main`.
