package io.freemytv;

import org.json.*;
import java.util.*;

/** Season-aware playoff data and bracket rules. Never treat a regular-season seed as a clinched berth. */
final class NFLPlayoffs {
    static final int WILD=0,DIVISIONAL=1,CONFERENCE=2,SUPER_BOWL=3;
    static int stage(String label){
        String s=label.toLowerCase(Locale.ROOT);
        if(s.contains("pro bowl")||s.contains("all-star"))return -1;
        if(s.contains("wild card")||s.contains("wildcard"))return WILD;
        if(s.contains("divisional"))return DIVISIONAL;
        if(s.contains("confer"))return CONFERENCE;
        if(s.contains("super bowl"))return SUPER_BOWL;
        return -1;
    }
    static List<NFLRounds.Ref> calendar(JSONObject root){
        List<NFLRounds.Ref> out=new ArrayList<>();int year=root.optJSONObject("season")==null?0:root.optJSONObject("season").optInt("year");
        JSONArray leagues=root.optJSONArray("leagues"),calendar=leagues==null||leagues.length()==0?null:leagues.optJSONObject(0).optJSONArray("calendar");
        for(int i=0;calendar!=null&&i<calendar.length();i++){
            JSONObject group=calendar.optJSONObject(i);if(group==null||group.optInt("value")!=3)continue;JSONArray entries=group.optJSONArray("entries");
            for(int j=0;entries!=null&&j<entries.length();j++){
                JSONObject e=entries.optJSONObject(j);if(e==null)continue;String label=e.optString("label");
                if(stage(label)>=0&&e.optInt("value")>0)out.add(new NFLRounds.Ref(year,3,e.optInt("value"),label));
            }
        }
        return out;
    }
    static final class Snapshot {
        final int year,type;final long at;
        final NFLRounds.Round[] rounds=new NFLRounds.Round[4];
        Snapshot(int year,int type,long at){if(year<2000||year>2100||type<1||type>4)throw new IllegalArgumentException("Unknown NFL season");this.year=year;this.type=type;this.at=at;}
        boolean provisional(){return type<3;}
        JSONObject json()throws Exception{JSONArray a=new JSONArray();for(int i=0;i<4;i++)if(rounds[i]!=null)a.put(new JSONObject().put("stage",i).put("round",rounds[i].json()));return new JSONObject().put("year",year).put("type",type).put("at",at).put("rounds",a);}
        static Snapshot read(JSONObject o)throws Exception{
            if(o==null)return null;Snapshot s=new Snapshot(o.getInt("year"),o.getInt("type"),o.getLong("at"));JSONArray a=o.optJSONArray("rounds");
            for(int i=0;a!=null&&i<a.length();i++){JSONObject entry=a.getJSONObject(i);int stage=entry.getInt("stage");NFLRounds.Round r=NFLRounds.Round.read(entry.getJSONObject("round"));
                if(stage<0||stage>3||r.ref.year!=s.year||r.ref.type!=3||NFLPlayoffs.stage(r.ref.label)!=stage)throw new IllegalArgumentException("Mismatched playoff cache");s.rounds[stage]=r;}
            return s;
        }
        void add(NFLRounds.Ref expected,String body)throws Exception{
            int stage=NFLPlayoffs.stage(expected.label);NFLRounds.Round round=NFLRounds.parse(body,at);
            if(stage<0||expected.year!=year||!expected.same(round.ref)||round.ref.type!=3||NFLPlayoffs.stage(round.ref.label)!=stage)throw new IllegalArgumentException("Wrong postseason round");
            int[] maximum={6,4,2,1};if(round.games.size()>maximum[stage])throw new IllegalArgumentException("Unexpected playoff field");rounds[stage]=round;
        }
    }
    static final class Team {
        final String id,name,abbr,logo;final int seed;
        Team(String id,String name,String abbr,String logo,int seed){this.id=id;this.name=name;this.abbr=abbr;this.logo=logo;this.seed=seed;}
        static Team unknown(String label){return new Team("",label,"","",0);}
    }
    static final class Match {
        final Team home,away;final SportsFeedParser.Game game;final String hint;
        Match(Team home,Team away,SportsFeedParser.Game game,String hint){this.home=home;this.away=away;this.game=game;this.hint=hint;}
        Team winner(){
            if(game==null||!game.complete)return null;
            try{int h=Integer.parseInt(game.homeScore),a=Integer.parseInt(game.awayScore);return h==a?null:h>a?home:away;}catch(Exception ignored){return null;}
        }
    }
    static final class Side {
        final String name;final Map<Integer,Team> seeds=new TreeMap<>();final Set<String> ids=new HashSet<>();
        final List<Match> wild=new ArrayList<>(),divisional=new ArrayList<>();Match conference;
        Side(String name){this.name=name;}
        Team seed(int n){Team t=seeds.get(n);return t==null?Team.unknown("A definir"):t;}
    }
    static final class Board {
        final Side afc=new Side("AFC"),nfc=new Side("NFC");final boolean provisional,phaseKnown;final int year;final long at;
        Match superBowl;String warning="";
        Board(Snapshot snapshot,List<SportsFeedParser.Table> tables){
            provisional=snapshot==null||snapshot.provisional();phaseKnown=snapshot!=null;year=snapshot==null?season(tables):snapshot.year;at=snapshot==null?0:snapshot.at;
            Map<String,Team> teams=new HashMap<>();
            for(SportsFeedParser.Table table:tables){
                if(!table.key.equals("nfl")||!table.season.contains(String.valueOf(year)))continue;
                Side side=table.group.equalsIgnoreCase("AFC")?afc:table.group.equalsIgnoreCase("NFC")?nfc:null;if(side==null)continue;
                for(SportsFeedParser.Row row:table.rows){Team t=new Team(row.id,row.name,row.abbr,row.logo,row.rank);teams.put(t.id,t);side.ids.add(t.id);if(row.rank>=1&&row.rank<=7)side.seeds.put(row.rank,t);}
            }
            if(afc.seeds.size()<7||nfc.seeds.size()<7)warning="Classificação da temporada incompleta";
            build(afc,snapshot,teams);build(nfc,snapshot,teams);
            List<Match> finals=official(snapshot,SUPER_BOWL,null,teams);
            if(!finals.isEmpty())superBowl=finals.get(0);
            else{Team a=afc.conference.winner(),n=nfc.conference.winner();superBowl=new Match(a==null?Team.unknown("Campeão AFC"):a,n==null?Team.unknown("Campeão NFC"):n,null,a!=null&&n!=null?"Horário a confirmar":"Aguardando finalistas");}
        }
        private void build(Side side,Snapshot snapshot,Map<String,Team> teams){
            if(provisional){for(int[] pair:new int[][]{{2,7},{3,6},{4,5}})side.wild.add(new Match(side.seed(pair[0]),side.seed(pair[1]),null,"PROJEÇÃO"));}
            else side.wild.addAll(official(snapshot,WILD,side,teams));
            fill(side.wild,3,"Aguardando confrontos");
            side.divisional.addAll(official(snapshot,DIVISIONAL,side,teams));
            if(side.divisional.isEmpty()){
                List<Team> winners=winners(side.wild,3);
                if(winners.size()==3&&!side.seed(1).id.isEmpty()){
                    winners.sort(Comparator.comparingInt(t->t.seed));Team lowest=winners.remove(winners.size()-1);
                    side.divisional.add(new Match(side.seed(1),lowest,null,"Confronto previsto"));side.divisional.add(new Match(winners.get(0),winners.get(1),null,"Confronto previsto"));
                }else{side.divisional.add(new Match(side.seed(1),Team.unknown("A definir"),null,"Aguardando Wild Card"));side.divisional.add(new Match(Team.unknown("A definir"),Team.unknown("A definir"),null,"Aguardando Wild Card"));}
            }
            fill(side.divisional,2,"Aguardando confrontos");
            List<Match> finals=official(snapshot,CONFERENCE,side,teams);
            if(!finals.isEmpty())side.conference=finals.get(0);
            else{List<Team> winners=winners(side.divisional,2);winners.sort(Comparator.comparingInt(t->t.seed));side.conference=new Match(winners.size()==2?winners.get(0):Team.unknown("A definir"),winners.size()==2?winners.get(1):Team.unknown("A definir"),null,winners.size()==2?"Horário a confirmar":"Aguardando classificados");}
        }
        private List<Match> official(Snapshot snapshot,int stage,Side side,Map<String,Team> teams){
            List<Match> result=new ArrayList<>();if(provisional||snapshot==null||snapshot.rounds[stage]==null)return result;
            for(SportsFeedParser.Game g:snapshot.rounds[stage].games){
                if(side!=null&&(!side.ids.contains(g.homeId)||!side.ids.contains(g.awayId)))continue;
                Team h=teams.get(g.homeId),a=teams.get(g.awayId);
                if(h==null)h=new Team(g.homeId,g.home,g.homeAbbr,g.homeLogo,0);if(a==null)a=new Team(g.awayId,g.away,g.awayAbbr,g.awayLogo,0);
                result.add(new Match(h,a,g,""));
            }
            result.sort(Comparator.comparingInt(m->Math.min(m.home.seed==0?99:m.home.seed,m.away.seed==0?99:m.away.seed)));return result;
        }
        private static void fill(List<Match> matches,int count,String hint){while(matches.size()<count)matches.add(new Match(Team.unknown("A definir"),Team.unknown("A definir"),null,hint));}
        private static List<Team> winners(List<Match> matches,int count){
            List<Team> out=new ArrayList<>();Set<String> seen=new HashSet<>();if(matches.size()!=count)return out;
            for(Match match:matches){Team t=match.winner();if(t==null||t.id.isEmpty()||t.seed<1||!seen.add(t.id))return Collections.emptyList();out.add(t);}return out;
        }
        private static int season(List<SportsFeedParser.Table> tables){for(SportsFeedParser.Table table:tables)if(table.key.equals("nfl"))try{return Integer.parseInt(table.season);}catch(Exception ignored){}return 0;}
    }
}
