"""Publish the reviewed 1.4.0 release on demand, without a scheduled trigger."""
import json, os, urllib.error, urllib.request

REPOSITORY='ernanimarcondes/Esportes-Dash'
COMMIT='be7d9e43797c8ce200fdf5f51c6f3f8fccea0d42'
TREE='1099c3ff4ca4ccfa0d9e0fb499f68324cb3940be'
TAG='v1.4.0'
NOTES='''## NBA, NFL e melhorias do Esportes Dash

- Novos painéis exclusivos de NBA e NFL, com identidade visual própria, fundo preto, logos das ligas e escudos em alta qualidade.
- Visão geral, classificações completas por conferência (Leste/Oeste e AFC/NFC), campanha, aproveitamento, Jogos e Resultados.
- NBA: confronto em andamento ou próximo confronto de cada time em Jogos; último confronto encerrado de cada time em Resultados. Ambas as conferências, sem recorte por datas e sem repetir partidas compartilhadas.
- NFL: semana oficial atual em Jogos e semana anterior em Resultados, com a identidade das rodadas preservada no cache.
- Classificações e programação da F1 consultadas a cada 15 minutos; placares de hoje mantêm cinco minutos e clima/resultados antigos mantêm 30 minutos.
- Aba Todos dos Jogos do dia em lista cronológica de duas colunas, com páginas automáticas a cada 20 segundos.
- Cache compacto dos calendários da NBA, proteção dos placares conhecidos e novos testes de confrontos por time e rodadas da NFL.
- README, changelog, créditos e sete prints de documentação atualizados.

Os dados são atualizados periodicamente e dependem da cobertura das fontes. Calendários completos dos times NBA são conservados por seis horas; o cache guarda apenas a sequência de confrontos necessária à exibição. Instruções de compilação e instalação estão no README. Configurações pessoais, contas, credenciais, caches e chaves de assinatura não são publicados.

Baseado no [Free My TV, de simbasjungles e colaboradores](https://github.com/simbasjungles/free-my-tv), e desenvolvido a partir do [TV-Dashboard](https://github.com/ernanimarcondes/TV-Dashboard). Licença MIT e avisos dos componentes preservados. Logos e dados pertencem aos respectivos titulares; créditos em THIRD_PARTY_NOTICES.md.
'''

def request(path,method='GET',data=None,optional=False):
    headers={'Accept':'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28',
             'Authorization':'Bearer '+os.environ['GITHUB_TOKEN'],'User-Agent':'Esportes-Dash-release'}
    body=None if data is None else json.dumps(data).encode('utf-8')
    if body is not None:headers['Content-Type']='application/json'
    req=urllib.request.Request('https://api.github.com/repos/'+REPOSITORY+'/'+path,data=body,headers=headers,method=method)
    try:
        with urllib.request.urlopen(req,timeout=30) as response:return json.load(response)
    except urllib.error.HTTPError as error:
        if optional and error.code==404:return None
        raise RuntimeError('GitHub API returned HTTP '+str(error.code)) from None

def publish(event,api=request):
    if event not in {'push','workflow_dispatch'}:
        raise RuntimeError('An explicit publication trigger is required')
    commit=api('git/commits/'+COMMIT)
    if commit.get('sha')!=COMMIT or commit.get('tree',{}).get('sha')!=TREE:
        raise RuntimeError('Reviewed commit/tree mismatch')
    tag=api('git/ref/tags/'+TAG,optional=True)
    if tag and (tag.get('object',{}).get('type')!='commit' or tag['object'].get('sha')!=COMMIT):
        raise RuntimeError('Tag already points to different content')
    release=api('releases/tags/'+TAG,optional=True)
    if release:
        if release.get('draft') or not tag:raise RuntimeError('Unexpected existing release state')
        print('Release already published: '+release['html_url']);return 'already-published'
    result=api('releases',method='POST',data={'tag_name':TAG,'target_commitish':COMMIT,
        'name':'Esportes-Dash 1.4.0','body':NOTES,'draft':False,'prerelease':False,'make_latest':'true'})
    if result.get('tag_name')!=TAG or result.get('draft'):raise RuntimeError('Unexpected publication response')
    print('Published: '+result['html_url']);return 'published'

if __name__=='__main__':
    if os.environ.get('GITHUB_REPOSITORY')!=REPOSITORY:raise SystemExit('Repository mismatch')
    publish(os.environ.get('GITHUB_EVENT_NAME',''))
