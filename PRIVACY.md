# Privacidade

Este projeto contém configurações genéricas, código, fontes licenciadas e exemplos de dados esportivos públicos. Foi preparado em uma cópia isolada do projeto público TV-Dashboard, sem importar o histórico Git do ambiente pessoal.

Não são incluídos email, senha, cookies, tokens, banco de dados ou sessões de contas YouTube/Spotify. Os atalhos apenas abrem os aplicativos já instalados na TV. Os módulos personalizados de novidades do YouTube e metadados Spotify do Dash Pessoal foram removidos desta variante.

A cidade e as coordenadas escolhidas no aplicativo ficam no dispositivo. As coordenadas são enviadas à Open-Meteo ao consultar o clima. Jogos, classificações, logos e circuitos são consultados nas fontes públicas indicadas no README; respostas e imagens ficam em cache local. O projeto não disponibiliza servidor próprio de coleta.

A infraestrutura herdada do Free My TV mantém recursos opcionais, como TMDB, Plex e Home Assistant, mas nenhuma chave ou servidor pessoal é fornecido. Qualquer configuração local dessas integrações deve permanecer privada.

`config.properties`, arquivos de ambiente, chaves, `.local/`, APKs, logs, capturas e caches não são versionados. O script `tools/privacy_check.py` verifica os arquivos rastreados por padrões conhecidos e arquivos proibidos. Isso é uma revisão preventiva, não uma auditoria formal de segurança nem uma garantia sobre futuras alterações.
