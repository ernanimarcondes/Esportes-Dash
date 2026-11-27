package io.freemytv;
import org.json.*;
import java.nio.file.*;
import java.util.*;
final class LeaguePlayoffsCheck {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static JSONObject entry(int seed,int side,String division)throws Exception{return new JSONObject().put("team",new JSONObject().put("id",side+"-"+seed).put("name","Team "+seed).put("abbreviation","T"+seed).put("logos",new JSONArray().put(new JSONObject().put("href","https://a.espncdn.com/i/teamlogos/nba/500/bos.png")))).put("stats",new JSONArray().put(new JSONObject().put("name","playoffSeed").put("value",seed)));}
    static String standings(String league)throws Exception{
        JSONArray conferences=new JSONArray();for(int side=0;side<2;side++){JSONObject group=new JSONObject().put("name",league.equals("mlb")?(side==0?"American League":"National League"):(side==0?"Eastern Conference":"Western Conference"));
            if(league.equals("nhl")){JSONArray divisions=new JSONArray();for(int d=0;d<2;d++){JSONArray entries=new JSONArray();int[][] seeds={{1,4,6,8,9,10,12,15},{2,3,5,7,11,13,14,16}};for(int seed:seeds[d])entries.put(entry(seed,side,""));divisions.put(new JSONObject().put("name",d==0?"Atlantic Division":"Metropolitan Division").put("standings",new JSONObject().put("entries",entries)));}group.put("children",divisions);}
            else{JSONArray entries=new JSONArray();for(int seed=1;seed<=15;seed++)entries.put(entry(seed,side,""));group.put("standings",new JSONObject().put("entries",entries));}conferences.put(group);}
        return new JSONObject().put("season",new JSONObject().put("year",2027)).put("children",conferences).toString();
    }
    static void pair(LeaguePlayoffs.Series s,int a,int b){check(s.a.seed==a&&s.b.seed==b,"Wrong bracket pairing: "+s.a.seed+"/"+s.b.seed);}
    static JSONObject game(String note,int type,int year,int a,int b,int wa,int wb,int best,boolean complete)throws Exception{
        JSONArray teams=new JSONArray().put(new JSONObject().put("id","0-"+a)).put(new JSONObject().put("id","0-"+b));
        JSONObject series=new JSONObject().put("completed",complete).put("totalCompetitions",best).put("competitors",new JSONArray().put(new JSONObject().put("id","0-"+a).put("wins",wa)).put(new JSONObject().put("id","0-"+b).put("wins",wb)));
        return new JSONObject().put("season",new JSONObject().put("year",year).put("type",type)).put("date","2027-04-22T00:00Z").put("competitions",new JSONArray().put(new JSONObject().put("notes",new JSONArray().put(new JSONObject().put("headline",note))).put("competitors",teams).put("series",series)));
    }
    public static void main(String[] args)throws Exception{
        for(String league:new String[]{"nba","nhl","mlb"}){
            LeaguePlayoffs.Snapshot s=LeaguePlayoffs.parse(league,2027,standings(league),new JSONArray(),1);LeaguePlayoffs.Board b=new LeaguePlayoffs.Board(league,s);check(b.provisional,"Regular season must be provisional");check(b.championship.winner()==null,"No projected champion");
            check(LeaguePlayoffs.Snapshot.read(s.json()).teams.size()==s.teams.size(),"Cache round trip");
            if(league.equals("nba")){pair(b.first[0].get(0),1,8);pair(b.first[0].get(1),4,5);pair(b.first[0].get(2),2,7);pair(b.first[0].get(3),3,6);check(b.playIn[0].size()==3,"All play-in games");pair(b.playIn[0].get(0),7,8);pair(b.playIn[0].get(1),9,10);}
            if(league.equals("nhl")){pair(b.first[0].get(0),1,8);pair(b.first[0].get(1),4,6);pair(b.first[0].get(2),2,7);pair(b.first[0].get(3),3,5);}
            if(league.equals("mlb")){pair(b.first[0].get(0),4,5);pair(b.first[0].get(1),3,6);check(b.second[0].get(0).a.seed==1&&b.second[0].get(1).a.seed==2,"MLB top two byes");check(LeaguePlayoffs.best(league,0)==3&&LeaguePlayoffs.best(league,1)==5,"MLB series lengths");}
        }
        JSONArray games=new JSONArray().put(game("East 1st Round - Game 4",3,2027,1,8,4,0,7,true)).put(game("East 1st Round - Game 3",3,2027,1,8,3,0,7,false)).put(game("East 1st Round - Game 1",2,2027,2,7,1,0,7,false)).put(game("East 1st Round - Game 1",3,2026,2,7,1,0,7,false));
        LeaguePlayoffs.Snapshot snapshot=LeaguePlayoffs.parse("nba",2027,standings("nba"),games,3);check(snapshot.series.size()==1,"Reject regular and other seasons; deduplicate series");check(snapshot.series.get(0).winner().seed==1,"Confirmed series winner only");
        LeaguePlayoffs.Board board=new LeaguePlayoffs.Board("nba",snapshot);check(board.second[0].get(0).a.seed==1,"Winner advances fixed bracket");check(board.second[0].get(0).b.id.isEmpty(),"Unfinished neighbor stays unknown");
        snapshot.series.get(0).complete=false;check(snapshot.series.get(0).winner()==null,"No advance without source confirmation");
        boolean rejected=false;try{LeaguePlayoffs.parse("nba",2026,standings("nba"),new JSONArray(),1);}catch(Exception expected){rejected=true;}check(rejected,"Wrong-season standings rejected");
        check(LeaguePlayoffs.stage("nba","NBA Cup Final")==2,"Cup excluded by season metadata before stage parsing");
        if(args.length>0){Path root=Paths.get(args[0]);for(String league:new String[]{"nba","nhl","mlb"}){
            JSONArray events=new JSONArray();int[] months=league.equals("mlb")?new int[]{9,10,11}:new int[]{4,5,6};for(int month:months){JSONArray a=new JSONObject(new String(Files.readAllBytes(root.resolve(league+"-post-"+month+".json")),"UTF-8")).getJSONArray("events");for(int i=0;i<a.length();i++)events.put(a.get(i));}
            String standings=league.equals("nba")?new String(Files.readAllBytes(root.resolve("nba-standings-2026.json")),"UTF-8"):new String(Files.readAllBytes(root.resolve(league+"-div-2026.json")),"UTF-8");
            LeaguePlayoffs.Snapshot real=LeaguePlayoffs.parse(league,2026,standings,events,9);LeaguePlayoffs.Board b=new LeaguePlayoffs.Board(league,real);check(b.championship.official&&b.championship.winner()!=null,"Real official final: "+league);
            Set<String> brackets=new HashSet<>();for(int side=0;side<2;side++){for(LeaguePlayoffs.Series row:b.first[side]){check(row.official,"All opening series are official");check(brackets.add(row.key()),"Unique opening series");}for(LeaguePlayoffs.Series row:b.second[side]){check(row.official,"All second round series are official");check(brackets.add(row.key()),"Unique second-round series");}check(b.conference[side].official,"Official conference final");}int expected=league.equals("mlb")?11:league.equals("nba")?21:15;check(real.series.size()==expected,"All real series: "+league+"="+real.series.size());System.out.println("PASS real feed "+league+": "+real.series.size()+" series; champion "+b.championship.winner().name);
        }}
        System.out.println("PASS: NBA Play-In and bracket, NHL divisions/wild cards, MLB byes/series lengths, complete-only advancement, source deduplication, cache and season checks.");
    }
}
