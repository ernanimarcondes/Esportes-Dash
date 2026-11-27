# Alterações

## 1.6.0 — 2026-11-27

- Nova aba Playoffs no final dos painéis NBA, NFL, NHL e MLB, preservando Visão geral, classificações, Jogos e Resultados.
- Chaves próprias de cada liga, Play-In da NBA, folgas, séries e reseeding da NFL; consulta da temporada correta e progressão somente com resultados confirmados.
- Panorama provisório antes da chave oficial, com cache local e consultas periódicas de cinco minutos na NFL; nas demais ligas, cinco minutos na pós-temporada e 15 minutos no panorama provisório.
- NFL mantém Jogos e Resultados pelas rodadas oficiais competitivas e exclui o Pro Bowl.
- Logos de conferências e troféus ilustrados a partir de artes oficiais, com enquadramento, alinhamento e áreas de legenda padronizados.
- Documentação, quatro capturas dos playoffs e créditos atualizados; testes determinísticos das regras das quatro ligas.
- APK público 1.6.0 com a assinatura persistente da distribuição pública anterior e checksum SHA-256, sem configurações pessoais.

## 1.5.0 — 2026-11-26

- Galeria de capturas reais de NHL e MLB, classificações alinhadas, resultados e exemplo da troca automática dos resumos na NBA; Visão geral da NFL atualizada.

- Novas telas exclusivas da NHL e MLB, com Visão geral, classificações completas das duas conferências/ligas, Jogos e Resultados.
- OK no quadro compartilhado abre a liga exibida no momento do clique; Voltar retorna ao mesmo quadro.
- Identidade visual própria: NHL em prata/grafite e MLB em azul-marinho/vermelho, sempre com fundo preto e logos originais de 500 px.
- Classificações com linhas e colunas uniformes, escudos centralizados e tamanho visual equilibrado sem distorção das proporções.
- Resumos da Visão geral de NBA, NFL, NHL e MLB alternam até dois confrontos a cada 15 segundos, percorrem toda a lista e recomeçam ao retornar à aba. Resultados começam pelos mais recentes.
- Calendários por time de NHL e MLB seguem o padrão da NBA: confronto em andamento ou próximo por time; último confronto encerrado por time, sem janela fixa de datas.
- Tratamento do intervalo entre temporadas da MLB, datas com ano quando necessário, migração do cache da NBA e preservação de placares conhecidos diante de calendários atrasados.
- APK público atualizado com a mesma assinatura da versão 1.4.1; permite atualização sem desinstalar essa distribuição pública. Configurações pessoais e chaves permanecem fora da publicação.

## 1.4.1 — 2026-11-22

- Primeiro APK público pronto para instalar em Releases, com assinatura própria persistente, checksum SHA-256 e instruções de instalação sem compilação.

- Corrigido o botão TV / HDMI da tela inicial, que agora abre um seletor de entradas independente do menu original.
- TV · canais / antena solicita o sintonizador, evitando retomar a última entrada HDMI nas TCL compatíveis.
- Entradas HDMI usam o nome do dispositivo e o estado de conexão informado pela televisão; entradas em espera continuam selecionáveis.
- Compatibilidade com os filtros MIME de canais e entradas da TCL, preservando os caminhos específicos dos demais fabricantes.

## 1.4.0 — 2026-11-21

- Novos painéis exclusivos da NBA e NFL, abertos com OK nos quadros da tela inicial e retorno por Voltar ao quadro selecionado.
- Identidade visual própria de cada liga, fundo preto, logos originais em alta resolução e escudos com redução de alta qualidade para a TV.
- Visão geral, classificações completas por conferência (Leste/Oeste e AFC/NFC), campanha e aproveitamento, Jogos e Resultados.
- NBA: Jogos reúne o confronto em andamento de cada time ou seu próximo confronto; Resultados reúne seu último confronto encerrado. Inclui ambas as conferências, sem recorte por datas e sem repetir partidas compartilhadas. Títulos simplificados para Jogos e Resultados.
- NFL: Jogos usa a semana oficial atual, incluindo partidas encerradas dessa semana; Resultados usa a semana anterior. Preserva a identidade de temporada/fase/semana no cache e as transições do calendário, sem inferir rodadas por datas.
- Calendários da NBA carregados por time, com cache limitado pela sequência de partidas, gravação agrupada e preservação de placares conhecidos quando o calendário não oferece o placar em andamento.
- Classificações de futebol, NBA, NFL, NHL e MLB e programação de F1 consultadas a cada 15 minutos enquanto o painel está aberto. Jogos de hoje mantêm cinco minutos; clima e resultados antigos mantêm 30 minutos.
- Aba Todos dos Jogos do dia em lista de duas colunas, até 48 partidas por página e troca a cada 20 segundos; demais filtros continuam em cartões.
- Documentação, prints, créditos e testes atualizados, incluindo confrontos por time sem limites de data, semanas da NFL e preservação dos placares.

## 1.3.0 — 2026-11-18

- Painel exclusivo de Jogos do dia, verde-esmeralda, seis cartões, filtros de estado e páginas automáticas a cada 20 segundos.
- Painel exclusivo de Resultados recentes, roxo, aba Todos em lista cronológica e abas por data do mais antigo ao mais novo, sem aba Hoje.
- Autores e momentos dos gols de futebol e NHL nos detalhes, agrupados por jogador, com rotação de cinco segundos quando excedem quatro linhas. Consulta de resumos históricos, tratamento de gols contra, pênaltis e shootouts, e cache local validado por partida, equipes e placar.
- Tela principal conserva a apresentação compacta sem autores dos gols.
- Menos redesenhos ociosos, reaproveitamento dos endereços dos escudos e atualização das listas somente quando os modelos mudam; escudos de uso recente permanecem priorizados no cache de disco.
- Testes determinísticos de filtros, cronologia, janela de 72 horas, paginação, temporizadores e cobertura dos gols, com exemplos públicos mínimos.
- Documentação e capturas atualizadas; configurações pessoais, contas, chaves e caches continuam fora do projeto.

## 1.2.3 — 2026-11-17

- Programação do GP com título um pouco maior e aparência uniforme no quadro principal; destaque da Próxima sessão preservado.

- Escudos próprios para fundo escuro em todos os quadros: futebol, seleções, NBA, NFL, NHL, MLB, jogos do dia e resultados recentes.
- Seleção compartilhada dos originais em alta resolução, com redução de alta qualidade e alternativa automática para o escudo normal quando a variante falha.
- Preserva cores oficiais, proporções, tamanho dos escudos e a rotação completa das conferências; evita variantes antigas de Athletico-PR, Jets e Guardians.
- Testes dos caminhos de imagens por esporte, normalização de resolução e alternativas de carregamento.

## 1.2.2 — 2026-11-17

- NBA, NFL, NHL e MLB exibem todas as páginas de uma conferência antes de trocar para a próxima.
- Oito times por página, mantendo 15 segundos e reservando mais espaço entre a tabela e a temporada no rodapé.
- Pré-carregamento dos escudos e abertura dos detalhes acompanham a mesma sequência.
- Testes de paginação, limites de tempo, conferências com tamanhos diferentes e reinício do ciclo.

## 1.2.1 — 2026-11-17

- Os 26 mapas do quadro de F1 na tela principal usam o novo desenho compacto: proporção preservada, traços vetoriais nítidos e setores coloridos, sem números de curvas sobrepostos.
- Logos em alta qualidade junto aos pilotos e construtores no quadro principal.
- Print principal atualizado; mapas completos e números das curvas preservados no painel exclusivo de F1.

## 1.2.0 — 2026-11-17

- Nova aba Resultados no painel de F1, com a classificação final completa de cada GP.
- Corridas em ordem do calendário, da primeira à mais recente; rotação automática a cada 30 segundos, sem controles de pausa ou contadores.
- Piloto e tempo da volta mais rápida no canto superior direito, quando fornecidos pela fonte.
- Consulta periódica dos resultados, cache local e inclusão automática de novos GPs ao final da sequência.
- Print atualizado e documentação; créditos e padrões públicos sem credenciais preservados.

## 1.1.0 — 2026-11-17

- OK no quadro de F1 abre um painel dedicado; Voltar retorna ao quadro selecionado.
- Abas Visão geral, Pilotos, Construtores e Calendário; logos em alta qualidade, vitórias e pódios de GPs.
- Diagramas de 26 circuitos com números nativos, setores vermelho/azul/amarelo, linhas contínuas e redução de alta qualidade.
- Jogos do dia em ordem de horário e resultados em ordem de data/horário.
- Print de F1 revisado, créditos e documentação das próximas melhorias dos demais quadros.


## 1.0.0

- Projeto esportivo independente, derivado de TV-Dashboard e Free My TV.
- Classificações, jogos e resultados, F1 com todas as páginas consecutivas de pilotos e classificações americanas.
- Paleta preta/grafite, azul-gelo e seleção em âmbar.
- Padrões genéricos sem contas, cidade ou chave pessoal e com modo Original preservado.
