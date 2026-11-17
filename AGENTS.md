# Esportes-Dash

- The only custom home layout is SportsHomeView; preserve the Original mode and upstream recovery protections.
- Preserve the Free My TV and TV-Dashboard attribution, MIT license and all font/asset licenses.
- Never commit personal configuration, city/coordinates, account files, signing keys, personal captures, logs or caches. The reviewed sports-only documentation images at docs/images/f1-dashboard.png, docs/images/f1-results.png, docs/images/jogos-do-dia.png, docs/images/resultados-recentes.png and docs/images/resultados-lista.png are allowed. The user explicitly requested publishing docs/images/esportes-dashboard.png, including the displayed weather city; no account details or credentials are permitted.
- Before publishing run `python tools/privacy_check.py` and compile generic defaults with `python build.py`.
- Keep io.freemytv identity and recovery invariants consistent; document variant/signature incompatibility.
- Do not change TV network, sound, picture or account settings while working on the dashboard.
