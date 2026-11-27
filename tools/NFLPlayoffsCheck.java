package io.freemytv;
import org.json.*;
import java.util.*;

public final class NFLPlayoffsCheck {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    static List<SportsFeedParser.Table> tables(){
        List<SportsFeedParser.Table> all=new ArrayList<>();
        for(String group:new String[]{"AFC","NFC"}){SportsFeedParser.Table t=new SportsFeedParser.Table();t.key="nfl";t.group=group;t.season="2026";
            for(int n=1;n<=16;n++){SportsFeedParser.Row r=new SportsFeedParser.Row();r.id=group+n;r.abbr=r.name=r.id;r.rank=n;t.rows.add(r);}all.add(t);}
        return all;
    }
    static SportsFeedParser.Game game(String group,int home,int away,int h,int a,boolean completed){
        SportsFeedParser.Game g=new SportsFeedParser.Game();g.source="nfl";g.id=group+home+"-"+away;g.homeId=group+home;g.awayId=group+away;g.home=g.homeId;g.away=g.awayId;g.homeScore=""+h;g.awayScore=""+a;g.complete=completed;g.state=completed?"post":"pre";return g;
    }
    static NFLRounds.Round round(int stage,List<SportsFeedParser.Game> games){
        int[] weeks={1,2,3,5};String[] labels={"Wild Card","Divisional Round","Conference Championships","Super Bowl"};
        return new NFLRounds.Round(new NFLRounds.Ref(2026,3,weeks[stage],labels[stage]),null,100,games);
    }
    static JSONObject root(int year,int week)throws Exception{
        JSONArray entries=new JSONArray();String[] labels={"Wild Card","Divisional Round","Conference Championships","Pro Bowl","Super Bowl"};
        for(int i=0;i<labels.length;i++)entries.put(new JSONObject().put("label",labels[i]).put("value",i+1));
        JSONArray phases=new JSONArray().put(new JSONObject().put("value",2).put("entries",new JSONArray().put(new JSONObject().put("label","Week 18").put("value",18))))
            .put(new JSONObject().put("value",3).put("entries",entries));
        return new JSONObject().put("season",new JSONObject().put("year",year).put("type",3)).put("week",new JSONObject().put("number",week))
            .put("leagues",new JSONArray().put(new JSONObject().put("calendar",phases))).put("events",new JSONArray());
    }
    public static void main(String[] args)throws Exception{
        List<SportsFeedParser.Table> tables=tables();
        NFLPlayoffs.Snapshot regular=new NFLPlayoffs.Snapshot(2026,2,100);
        NFLPlayoffs.Board b=new NFLPlayoffs.Board(regular,tables);
        check(b.provisional&&b.phaseKnown&&b.afc.wild.size()==3&&b.nfc.wild.size()==3,"Regular season shows a complete, explicitly provisional field");
        check(b.afc.wild.get(0).home.seed==2&&b.afc.wild.get(0).away.seed==7&&b.afc.wild.get(1).home.seed==3&&b.afc.wild.get(1).away.seed==6&&b.afc.wild.get(2).home.seed==4&&b.afc.wild.get(2).away.seed==5,"Correct 2/7, 3/6 and 4/5 Wild Card projection");
        check(b.afc.seed(1).id.equals("AFC1")&&b.afc.divisional.get(0).away.id.isEmpty()&&b.afc.conference.home.id.isEmpty()&&b.superBowl.home.id.isEmpty(),"First seeds receive projected byes; no future winner or fake score is invented");
        NFLPlayoffs.Board offline=new NFLPlayoffs.Board(null,tables);check(!offline.phaseKnown&&offline.provisional,"Unknown phase stays provisional when offline");
        NFLPlayoffs.Snapshot post=new NFLPlayoffs.Snapshot(2026,3,100);
        List<SportsFeedParser.Game> wild=new ArrayList<>();
        for(String group:new String[]{"AFC","NFC"}){wild.add(game(group,2,7,17,24,true));wild.add(game(group,3,6,28,21,true));wild.add(game(group,4,5,20,24,true));}
        post.rounds[0]=round(0,wild);b=new NFLPlayoffs.Board(post,tables);
        check(!b.provisional&&b.afc.divisional.get(0).home.seed==1&&b.afc.divisional.get(0).away.seed==7,"Top seed hosts the lowest surviving seed after an upset");
        check(b.afc.divisional.get(1).home.seed==3&&b.afc.divisional.get(1).away.seed==5,"Other two survivors meet, higher seed at home");
        wild.get(0).complete=false;b=new NFLPlayoffs.Board(post,tables);check(b.afc.divisional.get(0).away.id.isEmpty(),"An unfinished Wild Card cannot produce a premature Divisional opponent");wild.get(0).complete=true;
        wild.get(0).homeScore="24";b=new NFLPlayoffs.Board(post,tables);check(b.afc.divisional.get(0).away.id.isEmpty(),"A tied or unresolved game never advances a team");wild.get(0).homeScore="17";
        post.rounds[1]=round(1,Arrays.asList(game("AFC",1,7,28,24,true),game("AFC",3,5,17,20,true),game("NFC",1,7,21,24,true),game("NFC",3,5,27,20,true)));
        b=new NFLPlayoffs.Board(post,tables);check(b.afc.conference.home.seed==1&&b.afc.conference.away.seed==5&&b.nfc.conference.home.seed==3&&b.nfc.conference.away.seed==7,"Conference finals advance only Divisional winners");
        check(b.superBowl.home.id.isEmpty(),"Super Bowl remains undecided before conference results");
        post.rounds[2]=round(2,Arrays.asList(game("AFC",1,5,28,21,true),game("NFC",3,7,17,24,true)));
        b=new NFLPlayoffs.Board(post,tables);check(b.superBowl.home.id.equals("AFC1")&&b.superBowl.away.id.equals("NFC7"),"Conference winners reach the Super Bowl");
        SportsFeedParser.Game sb=game("AFC",1,1,30,20,true);sb.awayId="NFC7";post.rounds[3]=round(3,Collections.singletonList(sb));
        b=new NFLPlayoffs.Board(post,tables);check(b.superBowl.winner().id.equals("AFC1"),"Actual final result determines the champion");
        NFLPlayoffs.Snapshot restored=NFLPlayoffs.Snapshot.read(new JSONObject(post.json().toString()));check(restored.rounds[0].games.size()==6&&restored.rounds[3].games.size()==1,"Bracket cache preserves all four rounds");
        JSONObject calendar=root(2026,5);List<NFLRounds.Ref> refs=NFLPlayoffs.calendar(calendar);check(refs.size()==4&&refs.get(3).week==5,"Calendar identifies postseason rounds by label and skips Pro Bowl");
        NFLRounds.Round superRound=NFLRounds.parse(calendar.toString(),100);check(superRound.previous.week==3&&superRound.previous.type==3,"Super Bowl results refer to conference finals, not Pro Bowl");
        NFLRounds.Round first=NFLRounds.parse(root(2026,1).toString(),100);check(first.previous.type==2&&first.previous.week==18,"First playoff round follows the final regular-season week");
        boolean rejected=false;try{post.add(new NFLRounds.Ref(2026,3,1,"Wild Card"),root(2025,1).toString());}catch(Exception expected){rejected=true;}check(rejected,"Mismatched season is rejected");
        rejected=false;try{post.add(new NFLRounds.Ref(2026,3,1,"Wild Card"),root(2026,2).toString());}catch(Exception expected){rejected=true;}check(rejected,"Mismatched round is rejected");
        JSONObject corrupt=post.json();corrupt.put("year",2027);rejected=false;try{NFLPlayoffs.Snapshot.read(corrupt);}catch(Exception expected){rejected=true;}check(rejected,"Cached postseason cannot leak into another season");
        tables.get(0).season="2025";b=new NFLPlayoffs.Board(regular,tables);check(!b.warning.isEmpty()&&b.afc.seed(1).id.isEmpty(),"Old standings cannot seed the current playoff field");
        check(NFLPlayoffs.stage("Pro Bowl")==-1&&NFLPlayoffs.stage("Finais de conferência")==2,"Competitive-round labels work in both API and cached localized form");
        System.out.println("PASS: NFL provisional seeding, byes, four phases, postseason reseeding, complete-only advancement, official final, missing data, season/cache identity and Pro Bowl exclusion.");
    }
}
