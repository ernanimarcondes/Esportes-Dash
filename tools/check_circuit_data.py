"""Validate bundled circuit topology, native labels and source attribution (no Android SDK needed)."""
import hashlib,json,math
from pathlib import Path
root=Path(__file__).resolve().parent.parent/'app/assets/sports/circuits'
index=json.loads((root/'index.json').read_text(encoding='utf-8'));seen=set()
for item in index['circuits']:
    data=json.loads((root/(item['id']+'.json')).read_text(encoding='utf-8'))
    assert data['id']==item['id']
    assert len(data['centerlines'])==3 and all(len(s)==1 for s in data['centerlines']),data['id']
    expected={str(i) for i in range(1,data['corners']+1)}
    assert expected<={label['number'] for label in data['labels']},data['id']
    assert data['length'] and int(data['laps'])>0 and data['source']['page'].startswith('https://www.formula1.com/'),data['id']
    assert not {'plates','glyph','road'}&data.keys(),data['id']
    for sector in data['centerlines']:
        for line in sector:
            assert len(line)>=2
            assert all(math.isfinite(x) and math.isfinite(y) for x,y in line)
    for a,b in [(0,1),(1,2),(2,0)]:
        x,y=data['centerlines'][a][0],data['centerlines'][b][0]
        assert min(math.dist(x[i],y[j]) for i in [0,-1] for j in [0,-1])<.01,(data['id'],'open sector boundary')
    digest=hashlib.sha256(json.dumps(data['centerlines']).encode()).hexdigest()
    assert digest not in seen,(data['id'],'duplicate geometry');seen.add(digest)
assert len(seen)==26
assert json.loads((root/'madring.json').read_text())['corners']==22
print('Circuit check passed: 26 distinct closed circuits, native corner numbers, three continuous sectors and public source credits.')
