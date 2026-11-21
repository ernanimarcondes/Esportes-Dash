"""Check match filtering, pagination, goals and render cadence on a desktop JVM.

Requires JDK 17+ and the JSON-Java (org.json) jar supplied with --json-jar.
No network access or Android device is used by these deterministic checks.
"""
from pathlib import Path
import argparse, os, shutil, subprocess, tempfile

ROOT=Path(__file__).resolve().parent.parent

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--json-jar',type=Path,required=True)
    args=parser.parse_args()
    jar=args.json_jar.resolve()
    if not jar.is_file():parser.error('JSON-Java jar not found')
    def tool(name):
        candidate=Path(os.environ['JAVA_HOME'])/'bin'/(name+('.exe' if os.name=='nt' else '')) if os.environ.get('JAVA_HOME') else shutil.which(name)
        if not candidate or not Path(candidate).is_file():raise SystemExit('JDK 17+ required: '+name)
        return str(candidate)
    sources=['SportsFeedParser','FootballGoals','MatchGoalsSummary','DayGames','RecentGames','DayGamesRotation','GoalLinesRotation','SportsRenderCadence','TeamLogoPolicy','StandingsRotation','LeaguePanelModel','NFLRounds']
    checks=['DayGamesCheck','RecentGamesCheck','FootballGoalsCheck','MatchGoalsSummaryCheck','SportsRenderCadenceCheck','TeamLogoPolicyCheck','StandingsRotationCheck','LeaguePanelModelCheck','NFLRoundsCheck']
    fixtures=ROOT/'tools/fixtures'
    arguments={'NFLRoundsCheck':[fixtures/'nfl-round-current.json',fixtures/'nfl-round-previous.json'],
               'FootballGoalsCheck':[fixtures/'soccer-goals.json'],
               'MatchGoalsSummaryCheck':[fixtures/'nhl-summary.json',fixtures/'nhl-scoreboard.json',fixtures/'soccer-goals.json',fixtures/'soccer-summary.json']}
    with tempfile.TemporaryDirectory(prefix='sports-panels-check-') as directory:
        paths=[ROOT/'app/src/io/freemytv'/(name+'.java') for name in sources]+[ROOT/'tools'/(name+'.java') for name in checks]
        subprocess.run([tool('javac'),'-encoding','UTF-8','-cp',str(jar),'-d',directory,*map(str,paths)],check=True)
        for name in checks:
            subprocess.run([tool('java'),'-cp',directory+os.pathsep+str(jar),'io.freemytv.'+name,*map(str,arguments.get(name,[]))],check=True)
    print('All match-panel checks passed.')

if __name__=='__main__':main()
