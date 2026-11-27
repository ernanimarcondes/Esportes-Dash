package io.freemytv;

import org.json.*;
import java.util.*;

/** Postseason series and projected brackets, isolated from the existing daily-game panels. */
final class LeaguePlayoffs {
    static final class Team {
        String id="",name="A definir",abbr="",logo="",division="";int side=-1,seed=999;
        JSONObject json()throws Exception{return new JSONObject().put("id",id).put("name",name).put("abbr",abbr).put("logo",logo).put("division",division).put("side",side).put("seed",seed);}
        static Team read(JSONObject o){Team t=new Team();t.id=o.optString("id");t.name=o.optString("name","A definir");t.abbr=o.optString("abbr");t.logo=o.optString("logo");t.division=o.optString("division");t.side=o.optInt("side",-1);t.seed=o.optInt("seed",999);return t;}
        static Team unknown(String name){Team t=new Team();t.name=name;return t;}
    }
    static final class Series {
        Team a,b;int stage,side,best,winsA,winsB;boolean official,complete;long at;String hint="";
        Series(Team a,Team b,int stage,int side,int best){this.a=a;this.b=b;this.stage=stage;this.side=side;this.best=best;}
        Team winner(){return complete&&winsA!=winsB&&Math.max(winsA,winsB)>=best/2+1?(winsA>winsB?a:b):null;}
        JSONObject json()throws Exception{return new JSONObject().put("a",a.json()).put("b",b.json()).put("stage",stage).put("side",side).put("best",best).put("wa",winsA).put("wb",winsB).put("official",official).put("complete",complete).put("at",at).put("hint",hint);}
        static Series read(JSONObject o)throws Exception{Series s=new Series(Team.read(o.getJSONObject("a")),Team.read(o.getJSONObject("b")),o.getInt("stage"),o.getInt("side"),o.getInt("best"));s.winsA=o.optInt("wa");s.winsB=o.optInt("wb");s.official=o.optBoolean("official");s.complete=o.optBoolean("complete");s.at=o.optLong("at");s.hint=o.optString("hint");return s;}
        String key(){return stage+":"+side+":"+(a.id.compareTo(b.id)<0?a.id+":"+b.id:b.id+":"+a.id);}
    }
    static final class Snapshot {
        String league;int year;long at;boolean provisional=true;final List<Team> teams=new ArrayList<>();final List<Series> series=new ArrayList<>();
        JSONObject json()throws Exception{JSONArray t=new JSONArray(),s=new JSONArray();for(Team x:teams)t.put(x.json());for(Series x:series)s.put(x.json());return new JSONObject().put("league",league).put("year",year).put("at",at).put("provisional",provisional).put("teams",t).put("series",s);}
        static Snapshot read(JSONObject o)throws Exception{Snapshot s=new Snapshot();s.league=o.getString("league");s.year=o.getInt("year");s.at=o.getLong("at");s.provisional=o.optBoolean("provisional",true);if(!supported(s.league)||s.year<2000||s.year>2100)throw new IllegalArgumentException("Invalid postseason cache");JSONArray ts=o.getJSONArray("teams"),ss=o.getJSONArray("series");for(int i=0;i<ts.length();i++)s.teams.add(Team.read(ts.getJSONObject(i)));for(int i=0;i<ss.length();i++)s.series.add(Series.read(ss.getJSONObject(i)));return s;}
    }
    static boolean supported(String key){return key.equals("nba")||key.equals("nhl")||key.equals("mlb");}
    static int stage(String league,String note){String s=note.toLowerCase(Locale.ROOT);
        if(league.equals("nba")){if(s.contains("play-in"))return -1;if(s.contains("nba finals"))return 3;if(s.contains("semifinal"))return 1;if(s.contains("final"))return 2;if(s.contains("1st round")||s.contains("first round"))return 0;}
        if(league.equals("nhl")){if(s.contains("stanley cup final"))return 3;if(s.contains("2nd round")||s.contains("second round"))return 1;if(s.contains("final"))return 2;if(s.contains("1st round")||s.contains("first round"))return 0;}
        if(league.equals("mlb")){if(s.contains("world series"))return 3;if(s.contains("alcs")||s.contains("nlcs"))return 2;if(s.contains("alds")||s.contains("nlds"))return 1;if(s.contains("alwc")||s.contains("nlwc")||s.contains("wild card"))return 0;}
        return -2;
    }
    static int best(String league,int phase){return phase<0?1:league.equals("mlb")?(phase==0?3:phase==1?5:7):7;}
    static Snapshot parse(String league,int year,String standings,JSONArray events,long at)throws Exception{
        JSONObject root=new JSONObject(standings);if(!supported(league)||root.getJSONObject("season").getInt("year")!=year)throw new IllegalArgumentException("Wrong standings season");
        Snapshot s=new Snapshot();s.league=league;s.year=year;s.at=at;collect(root,-1,"",s.teams);
        if(s.teams.size()<20)throw new IllegalArgumentException("Incomplete postseason standings");Map<String,Team> teams=new HashMap<>();for(Team t:s.teams)teams.put(t.id,t);
        Map<String,Series> unique=new LinkedHashMap<>();
        for(int i=0;i<events.length();i++){
            JSONObject event=events.optJSONObject(i),season=event==null?null:event.optJSONObject("season");if(season==null||season.optInt("year")!=year)continue;
            int type=season.optInt("type");if(type!=3&&!(league.equals("nba")&&type==5))continue;
            JSONArray competitions=event.optJSONArray("competitions");JSONObject c=competitions==null?null:competitions.optJSONObject(0);if(c==null)continue;
            String note="";JSONArray notes=c.optJSONArray("notes");for(int n=0;notes!=null&&n<notes.length();n++)note+=" "+notes.optJSONObject(n).optString("headline");
            int phase=stage(league,note);if(phase==-2||phase<0&&type!=5)continue;
            JSONArray competitors=c.optJSONArray("competitors");if(competitors==null||competitors.length()!=2)continue;
            JSONObject ac=competitors.optJSONObject(0),bc=competitors.optJSONObject(1);Team a=teams.get(ac.optString("id")),b=teams.get(bc.optString("id"));if(a==null||b==null||a.id.equals(b.id))continue;
            int side=phase==3?2:a.side;if(phase!=3&&(side<0||b.side!=side))continue;
            Series row=new Series(a,b,phase,side,best(league,phase));row.official=true;row.at=SportsFeedParserTime.parse(event.optString("date"));
            JSONObject data=c.optJSONObject("series");if(data!=null){JSONArray stats=data.optJSONArray("competitors");boolean hasA=false,hasB=false;
                for(int k=0;stats!=null&&k<stats.length();k++){JSONObject stat=stats.getJSONObject(k);if(stat.optString("id").equals(a.id)){row.winsA=stat.optInt("wins");hasA=true;}if(stat.optString("id").equals(b.id)){row.winsB=stat.optInt("wins");hasB=true;}}
                if(!hasA||!hasB||row.winsA<0||row.winsB<0||row.winsA>row.best/2+1||row.winsB>row.best/2+1)continue;
                row.complete=data.optBoolean("completed");
            }else if(phase==-1){JSONObject status=c.optJSONObject("status");row.complete=status!=null&&status.optJSONObject("type")!=null&&status.optJSONObject("type").optBoolean("completed");if(row.complete){row.winsA=ac.optBoolean("winner")?1:0;row.winsB=bc.optBoolean("winner")?1:0;}}
            else continue;
            if(phase==-1){String lower=note.toLowerCase(Locale.ROOT);row.hint=lower.contains("7th place")?"Vaga 7":lower.contains("9th place")?"Eliminatório":"Vaga 8";}
            Series old=unique.get(row.key());if(old==null||row.complete&&!old.complete||row.complete==old.complete&&(row.winsA+row.winsB>old.winsA+old.winsB||row.winsA+row.winsB==old.winsA+old.winsB&&row.at>old.at))unique.put(row.key(),row);
        }
        s.series.addAll(unique.values());s.provisional=s.series.isEmpty();return s;
    }
    private static void collect(JSONObject o,int side,String division,List<Team> out)throws Exception{
        String name=o.optString("name");if(name.contains("Eastern")||name.contains("American League"))side=0;else if(name.contains("Western")||name.contains("National League"))side=1;
        if(o.has("standings")){
            if(name.contains("Division")||name.matches(".*League (East|West|Central)"))division=name;
            JSONArray entries=o.getJSONObject("standings").optJSONArray("entries");for(int i=0;entries!=null&&i<entries.length();i++){
                JSONObject e=entries.getJSONObject(i),team=e.getJSONObject("team");Team t=new Team();t.id=team.optString("id");t.name=team.optString("shortDisplayName",team.optString("displayName"));t.abbr=team.optString("abbreviation");t.side=side;t.division=division;
                JSONArray logos=team.optJSONArray("logos");if(logos!=null&&logos.length()>0)t.logo=logos.getJSONObject(0).optString("href");
                JSONArray stats=e.optJSONArray("stats");for(int j=0;stats!=null&&j<stats.length();j++){JSONObject v=stats.getJSONObject(j);if(v.optString("name").equals("playoffSeed"))t.seed=v.optInt("value",999);}
                if(!t.id.isEmpty()&&side>=0)out.add(t);
            }
        }
        JSONArray children=o.optJSONArray("children");for(int i=0;children!=null&&i<children.length();i++)collect(children.getJSONObject(i),side,division,out);
    }
    static final class Board {
        final String league;final boolean provisional;final List<Series>[] first=new List[]{new ArrayList<>(),new ArrayList<>()},second=new List[]{new ArrayList<>(),new ArrayList<>()},playIn=new List[]{new ArrayList<>(),new ArrayList<>()};
        final Series[] conference=new Series[2];final List<Team>[] teams=new List[]{new ArrayList<>(),new ArrayList<>()};Series championship;String warning="";
        Board(String league,Snapshot snapshot){this.league=league;provisional=snapshot==null||snapshot.provisional||snapshot.series.stream().filter(row->row.stage==0).count()<(league.equals("mlb")?4:8);
            if(snapshot!=null)for(Team t:snapshot.teams)if(t.side>=0&&t.side<2)teams[t.side].add(t);
            for(int side=0;side<2;side++){
                teams[side].sort(Comparator.comparingInt(t->t.seed));if(teams[side].isEmpty())warning="Consultando classificação e confrontos";
                makePlayIn(side,snapshot);
                List<Team[]> pairs=project(side);int count=league.equals("mlb")?2:4;
                List<Series> actual=official(snapshot,side,0);boolean[] used=new boolean[actual.size()];
                for(int i=0;i<count;i++){
                    Team[] pair=pairs.get(i);Series chosen=null;
                    for(int j=0;j<actual.size();j++)if(!used[j]&&(has(actual.get(j),pair[0].id)||has(actual.get(j),pair[1].id))){chosen=actual.get(j);used[j]=true;break;}
                    if(chosen==null&&i<actual.size()&&!used[i]){chosen=actual.get(i);used[i]=true;}
                    if(chosen==null){chosen=new Series(pair[0],pair[1],0,side,best(league,0));chosen.hint=provisional?"Projeção":"A definir";}
                    first[side].add(chosen);
                }
                List<Series> semis=official(snapshot,side,1);
                for(int i=0;i<2;i++){
                    Team a,b;if(league.equals("mlb")){a=seed(side,i+1);b=winner(first[side].get(i),"A definir");}else{a=winner(first[side].get(i*2),"A definir");b=winner(first[side].get(i*2+1),"A definir");}
                    Series row=find(semis,a,b);if(row==null&&i<semis.size())row=semis.get(i);if(row==null){row=new Series(a,b,1,side,best(league,1));row.hint="Aguardando classificados";}second[side].add(row);
                }
                List<Series> finals=official(snapshot,side,2);conference[side]=finals.isEmpty()?new Series(winner(second[side].get(0),"A definir"),winner(second[side].get(1),"A definir"),2,side,7):finals.get(0);conference[side].hint="Aguardando classificados";
            }
            List<Series> finals=official(snapshot,2,3);championship=finals.isEmpty()?new Series(winner(conference[0],"Campeão "+sideName(league,0)),winner(conference[1],"Campeão "+sideName(league,1)),3,2,7):finals.get(0);championship.hint="Aguardando finalistas";
        }
        Team seed(int side,int rank){for(Team t:teams[side])if(t.seed==rank)return t;return Team.unknown("Vaga "+rank);}
        private List<Team[]> project(int side){
            List<Team[]> result=new ArrayList<>();if(league.equals("mlb")){result.add(new Team[]{seed(side,4),seed(side,5)});result.add(new Team[]{seed(side,3),seed(side,6)});return result;}
            if(league.equals("nba")){Team seven=seed(side,7),eight=seed(side,8);for(Series s:playIn[side]){if(s.hint.equals("Vaga 7")&&s.winner()!=null)seven=s.winner();if(s.hint.equals("Vaga 8")&&s.winner()!=null)eight=s.winner();}
                result.add(new Team[]{seed(side,1),eight});result.add(new Team[]{seed(side,4),seed(side,5)});result.add(new Team[]{seed(side,2),seven});result.add(new Team[]{seed(side,3),seed(side,6)});return result;}
            Map<String,List<Team>> divisions=new LinkedHashMap<>();for(Team t:teams[side])if(!t.division.isEmpty())divisions.computeIfAbsent(t.division,k->new ArrayList<>()).add(t);
            List<List<Team>> groups=new ArrayList<>(divisions.values());groups.sort(Comparator.comparingInt(a->a.get(0).seed));
            if(groups.size()==2&&groups.get(0).size()>=3&&groups.get(1).size()>=3){List<Team> wild=new ArrayList<>();for(List<Team> group:groups)wild.addAll(group.subList(3,group.size()));wild.sort(Comparator.comparingInt(t->t.seed));
                if(wild.size()>=2){for(int i=0;i<2;i++){List<Team> group=groups.get(i);result.add(new Team[]{group.get(0),wild.get(i==0?1:0)});result.add(new Team[]{group.get(1),group.get(2)});}return result;}}
            warning="Divisões indisponíveis · cruzamentos a confirmar";for(int i=0;i<4;i++)result.add(new Team[]{Team.unknown("A definir"),Team.unknown("A definir")});return result;
        }
        private void makePlayIn(int side,Snapshot snapshot){if(!league.equals("nba"))return;List<Series> actual=official(snapshot,side,-1);
            String[] labels={"Vaga 7","Eliminatório","Vaga 8"};for(int i=0;i<3;i++){Series row=null;for(Series s:actual)if(s.hint.equals(labels[i]))row=s;
                if(row==null){Team a=i==0?seed(side,7):i==1?seed(side,9):Team.unknown("Perdedor 7/8"),b=i==0?seed(side,8):i==1?seed(side,10):Team.unknown("Vencedor 9/10");
                    if(i==2){Series one=playIn[side].get(0),two=playIn[side].get(1);if(one.winner()!=null)a=one.winner().id.equals(one.a.id)?one.b:one.a;if(two.winner()!=null)b=two.winner();}
                    row=new Series(a,b,-1,side,1);row.hint=labels[i];}playIn[side].add(row);}
        }
    }
    static String sideName(String league,int side){return league.equals("mlb")?(side==0?"Americana":"Nacional"):(side==0?"Leste":"Oeste");}
    private static boolean has(Series s,String id){return !id.isEmpty()&&(s.a.id.equals(id)||s.b.id.equals(id));}
    private static Team winner(Series s,String unknown){Team t=s.winner();return t==null?Team.unknown(unknown):t;}
    private static Series find(List<Series> series,Team a,Team b){for(Series s:series)if(has(s,a.id)||has(s,b.id))return s;return null;}
    private static List<Series> official(Snapshot s,int side,int stage){List<Series> out=new ArrayList<>();if(s!=null)for(Series row:s.series)if(row.side==side&&row.stage==stage)out.add(row);out.sort(Comparator.comparingInt(x->Math.min(x.a.seed,x.b.seed)));return out;}
    private static final class SportsFeedParserTime {static long parse(String value){try{return java.time.OffsetDateTime.parse(value).toInstant().toEpochMilli();}catch(Exception ignored){return 0;}}}
}
