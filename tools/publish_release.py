"""Publish the reviewed 1.3.0 release on demand, without a scheduled trigger."""
import json, os, urllib.error, urllib.request

REPOSITORY='ernanimarcondes/Esportes-Dash'
COMMIT='14ae742726098c455fbb0c81149f7a6e82ae020f'
TREE='8b10f3e1e1e7b19be7a6b3e057551f3932894b6f'
TAG='v1.3.0'
NOTES='''## Novos painéis de jogos e resultados

- Jogos do dia com identidade verde-esmeralda, seis cartões, filtros e troca automática a cada 20 segundos.
- Resultados recentes em roxo, aba Todos em lista primeiro e datas do mais antigo ao mais novo.
- Autores e momentos dos gols de futebol e NHL nos detalhes, com alternância a cada cinco segundos quando necessário.
- Tela principal compacta, sem autores dos gols; melhorias nos caches e redução de redesenhos ociosos.
- Testes de cronologia, paginação, gols e temporizadores; documentação e prints atualizados.

Os dados esportivos dependem da cobertura das fontes e são atualizados periodicamente. Consulte o README para compilação e instalação.

Baseado no [Free My TV, de simbasjungles e colaboradores](https://github.com/simbasjungles/free-my-tv), e desenvolvido a partir do [TV-Dashboard](https://github.com/ernanimarcondes/TV-Dashboard). Licenças e créditos preservados em THIRD_PARTY_NOTICES.md.
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
        'name':'Esportes-Dash 1.3.0','body':NOTES,'draft':False,'prerelease':False,'make_latest':'true'})
    if result.get('tag_name')!=TAG or result.get('draft'):raise RuntimeError('Unexpected publication response')
    print('Published: '+result['html_url']);return 'published'

if __name__=='__main__':
    if os.environ.get('GITHUB_REPOSITORY')!=REPOSITORY:raise SystemExit('Repository mismatch')
    publish(os.environ.get('GITHUB_EVENT_NAME',''))
