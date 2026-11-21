# Privacidade

Este projeto contém configurações genéricas, código, fontes licenciadas e exemplos de dados esportivos públicos. Foi preparado em uma cópia isolada do projeto público TV-Dashboard, sem importar o histórico Git do ambiente pessoal.

Não são incluídos email, senha, cookies, tokens, banco de dados ou sessões de contas YouTube/Spotify. Os atalhos apenas abrem os aplicativos já instalados na TV. Os módulos personalizados de novidades do YouTube e metadados Spotify do Dash Pessoal foram removidos desta variante.

A cidade e as coordenadas escolhidas no aplicativo ficam no dispositivo. As coordenadas são enviadas à Open-Meteo ao consultar o clima. Jogos, classificações, logos e circuitos são consultados nas fontes públicas indicadas no README; respostas e imagens ficam em cache local. O projeto não disponibiliza servidor próprio de coleta.

A infraestrutura herdada do Free My TV mantém recursos opcionais, como TMDB, Plex e Home Assistant, mas nenhuma chave ou servidor pessoal é fornecido. Qualquer configuração local dessas integrações deve permanecer privada.

`config.properties`, arquivos de ambiente, chaves, `.local/`, APKs, logs, capturas pessoais e caches não são versionados. O script `tools/privacy_check.py` verifica os arquivos rastreados por padrões conhecidos e arquivos proibidos. Isso é uma revisão preventiva, não uma auditoria formal de segurança nem uma garantia sobre futuras alterações.

`docs/images/f1-dashboard.png` e `docs/images/f1-results.png` são capturas de documentação revisadas e destinadas à publicação. Elas mostram apenas as telas de F1, com dados esportivos públicos, relógio e data; não contém cidade configurada, contas, músicas, inscrições, IPs, chaves ou credenciais. `docs/images/esportes-dashboard.png` é a captura do painel principal publicada a pedido explícito do autor; mostra dados esportivos públicos, relógio e clima com a cidade exibida no painel, sem coordenadas, contas ou credenciais. As capturas revisadas `docs/images/jogos-do-dia.png`, `docs/images/resultados-recentes.png` e `docs/images/resultados-lista.png` mostram somente dados esportivos públicos, relógio e data. Nenhuma outra captura local foi incluída.

Os autores dos gols são extraídos de respostas públicas da ESPN. O cache de detalhes fica apenas no dispositivo e conserva campos esportivos necessários à exibição; não contém sessões ou credenciais. Os exemplos mínimos em `tools/fixtures` são dados esportivos públicos congelados para testes, sem o restante dos resumos da fonte.

As capturas de documentação da NBA, NFL e da aba Todos dos Jogos do dia desta versão foram revisadas para publicação e mostram somente informações esportivas públicas, relógio e data. Os calendários dos times e as rodadas da NFL usam fontes públicas, com cache apenas no dispositivo; nenhum dado de conta é consultado.
