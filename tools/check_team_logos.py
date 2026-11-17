"""Check shared logo selection for every sports panel, using JDK 17+ without Android SDK."""
from pathlib import Path
import os,subprocess,tempfile
root=Path(__file__).resolve().parent.parent
jdk=Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else None
suffix='.exe' if os.name=='nt' else ''
def tool(name):return str(jdk/(name+suffix)) if jdk else name
with tempfile.TemporaryDirectory(prefix='team-logos-check-') as out:
    subprocess.run([tool('javac'),'-encoding','UTF-8','-d',out,str(root/'app/src/io/freemytv/TeamLogoPolicy.java'),str(root/'tools/TeamLogoPolicyCheck.java')],check=True)
    subprocess.run([tool('java'),'-cp',out,'io.freemytv.TeamLogoPolicyCheck'],check=True)
