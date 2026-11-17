# Esportes-Dash

Painel de esportes para Android TV, com fundo preto, cartões em grafite, azul-gelo e destaque em âmbar no quadro selecionado e na próxima sessão de F1.

> **Baseado no [Free My TV, de simbasjungles e colaboradores](https://github.com/simbasjungles/free-my-tv), e desenvolvido a partir do [TV-Dashboard](https://github.com/ernanimarcondes/TV-Dashboard).** A licença MIT e os avisos dos componentes utilizados estão preservados. Créditos detalhados em [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

Este é o projeto independente do painel esportivo. Para a base com Dash Pessoal e suporte à implementação de outros perfis, consulte TV-Dashboard.

## O painel

- Classificações: Brasileirão Série A e B, Premier League, Ligue 1, LaLiga, Liga Portugal e Bundesliga. Troca a cada 30 segundos.
- Jogos do dia: dois confrontos por página, escudos e horários de Brasília. Troca a cada 10 segundos.
- Resultados: partidas encerradas nas últimas 72 horas, dois confrontos por página e troca a cada 10 segundos.
- Futebol de clubes e seleções: ligas selecionadas, Libertadores, Sul-Americana, Champions, Europa League, Conference League, copas nacionais e outras competições importantes disponíveis na fonte. Nas copas nacionais, prioriza os clubes das ligas acompanhadas; amistosos priorizam seleções de maior destaque.
- Fórmula 1: programação e traçado do próximo GP por 30 segundos; classificação de pilotos em páginas consecutivas de dez nomes, com 15 segundos por página; construtores por 15 segundos. A próxima sessão permanece visível.
- NBA e NFL: conferências alternadas a cada 15 segundos. NHL e MLB compartilham o quadro e alternam as conferências/ligas a cada 15 segundos. Tabelas menores têm páginas de oito times; OK abre a lista completa.
- Relógio, data, clima da cidade configurada e atalhos para aplicativos, TV/HDMI e ajustes.

Os tempos acima controlam a rotação visual. As consultas de dados são periódicas, com cache local e conservação da última resposta válida. Não é um serviço de placar em tempo real. Não há grade de transmissão de TV. A cobertura depende das fontes e pode ter jogos ausentes, dados indisponíveis ou programação alterada.

## Compilar

Requisitos: Python 3, JDK 17+, Android SDK com `build-tools;34.0.0` e `platforms;android-34`. Defina `JAVA_HOME` e `ANDROID_HOME`:

```text
python build.py
```

O APK genérico fica em `out/Esportes-Dash.apk`. A compilação usa uma chave de desenvolvimento criada localmente em `.local/debug.keystore`. A chave e o APK ficam fora do Git. Guarde sua própria chave para assinar futuras atualizações; não publique a chave.

O build padrão ignora `config.properties` e não contém cidade, coordenadas ou credenciais pessoais. Para definir padrões locais, copie `config.example.properties` para `config.properties` e compile com `python build.py --personal`. A cidade também pode ser escolhida no aplicativo.

## Compatibilidade e instalação

Android TV 8 ou superior. O layout foi validado em uma TCL com Android 11 e interface de 1280×720. Outros firmwares precisam de validação própria. O modo Original e as proteções do Free My TV foram preservados.

**Esta variante preserva o identificador Android `io.freemytv`, usado pela infraestrutura de recuperação do projeto original. Ela não é instalada lado a lado com TV-Dashboard ou Free My TV.** APKs assinados com chaves diferentes não atualizam uns aos outros. Se estiver trocando de variante e precisar desinstalar, volte primeiro para Original pelo aplicativo, guarde suas configurações e só então remova a variante anterior. Não apague nem desinstale aplicativos de sistema.

Use o [ADB oficial do Android](https://developer.android.com/tools/releases/platform-tools):

```text
adb connect ENDERECO_DA_TV:5555
adb install -r out/Esportes-Dash.apk
```

Na TV, abra Free My TV → Home Screen → Esportes Dash e autorize a depuração do próprio aplicativo quando solicitada. Original continua disponível. Leia as [instruções do Free My TV](https://github.com/simbasjungles/free-my-tv#install) antes de ativar a substituição do launcher. Em alguns firmwares TCL é necessário permitir execução em segundo plano:

```text
adb shell "appops set io.freemytv AUTO_START allow || appops set io.freemytv APP_AUTO_START allow"
```

## Fontes e créditos

- [Free My TV](https://github.com/simbasjungles/free-my-tv): código-base, troca de tela inicial e recuperação; MIT.
- [TV-Dashboard](https://github.com/ernanimarcondes/TV-Dashboard): origem desta adaptação esportiva.
- ESPN: tabelas, jogos e escudos públicos. Seus endpoints e a cobertura podem mudar.
- [Jolpica F1](https://github.com/jolpica/jolpica-f1): calendário e classificações de Fórmula 1.
- [Open-Meteo](https://open-meteo.com/): clima das coordenadas configuradas pelo usuário.
- [bacinger/f1-circuits](https://github.com/bacinger/f1-circuits): geometria dos circuitos; MIT, aviso preservado nos assets.
- Inter e Rajdhani: SIL Open Font License, arquivos OFL preservados. Material Symbols: aviso de licença preservado.

Marcas, escudos e dados pertencem aos respectivos titulares. Não há afiliação com TCL, Google, ESPN, Spotify, YouTube, ligas ou equipes.

## Privacidade e desenvolvimento

O painel não usa login de YouTube ou Spotify, não mostra músicas nem consulta inscrições de contas: os botões apenas abrem aplicativos instalados. O código de recuperação e integrações opcionais herdado do Free My TV continua no projeto, sem chaves fornecidas. Consulte [PRIVACY.md](PRIVACY.md).

Antes de publicar:

```text
python tools/privacy_check.py
```

Somente código, assets licenciados e exemplos de dados públicos são versionados. Não publique configurações locais, capturas reais da TV, caches, sessões, chaves ou APKs pessoais.
