"""Check complete-conference rotation and eight-team pages with JDK 17+, no Android SDK required."""
from pathlib import Path
import os,subprocess,tempfile
root=Path(__file__).resolve().parent.parent
jdk=Path(os.environ['JAVA_HOME'])/'bin' if os.environ.get('JAVA_HOME') else None
suffix='.exe' if os.name=='nt' else ''
def tool(name):return str(jdk/(name+suffix)) if jdk else name
with tempfile.TemporaryDirectory(prefix='standings-check-') as out:
    subprocess.run([tool('javac'),'-encoding','UTF-8','-d',out,str(root/'app/src/io/freemytv/StandingsRotation.java'),str(root/'tools/StandingsRotationCheck.java')],check=True)
    subprocess.run([tool('java'),'-cp',out,'io.freemytv.StandingsRotationCheck'],check=True)
