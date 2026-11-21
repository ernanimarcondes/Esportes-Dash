package io.freemytv;

import org.json.*;
import java.util.*;

/** Official NFL week identity and its previous calendar entry; never infer weeks from dates. */
final class NFLRounds {
    static final class Ref {
        final int year,type,week;
        final String label;
        Ref(int year,int type,int week,String label){this.year=year;this.type=type;this.week=week;this.label=label;}
        boolean same(Ref other){return other!=null&&year==other.year&&type==other.type&&week==other.week;}
        JSONObject json()throws Exception{return new JSONObject().put("year",year).put("type",type).put("week",week).put("label",label);}
        static Ref read(JSONObject o){return o==null?null:new Ref(o.optInt("year"),o.optInt("type"),o.optInt("week"),o.optString("label"));}
        String query(){return "dates="+year+"&seasontype="+type+"&week="+week;}
    }
    static final class Round {
        final Ref ref,previous;
        final long at;
        final List<SportsFeedParser.Game> games;
        Round(Ref ref,Ref previous,long at,List<SportsFeedParser.Game> games){this.ref=ref;this.previous=previous;this.at=at;this.games=games;}
        JSONObject json()throws Exception{JSONArray rows=new JSONArray();for(SportsFeedParser.Game game:games)rows.put(game.json());return new JSONObject().put("ref",ref.json()).put("previous",previous==null?JSONObject.NULL:previous.json()).put("at",at).put("games",rows);}
        static Round read(JSONObject o)throws Exception{if(o==null)return null;List<SportsFeedParser.Game> games=new ArrayList<>();JSONArray rows=o.optJSONArray("games");for(int i=0;rows!=null&&i<rows.length();i++)games.add(SportsFeedParser.Game.read(rows.getJSONObject(i)));return new Round(Ref.read(o.getJSONObject("ref")),Ref.read(o.optJSONObject("previous")),o.optLong("at"),games);}
    }
    final Round current,previous;
    NFLRounds(Round current,Round previous){this.current=current;this.previous=previous;}
    JSONObject json()throws Exception{return new JSONObject().put("current",current.json()).put("previous",previous==null?JSONObject.NULL:previous.json());}
    static NFLRounds read(JSONObject o)throws Exception{return o==null?null:new NFLRounds(Round.read(o.getJSONObject("current")),Round.read(o.optJSONObject("previous")));}
    static Round parse(String body,long at)throws Exception{
        JSONObject root=new JSONObject(body),season=root.getJSONObject("season");int year=season.getInt("year"),type=season.getInt("type"),week=root.getJSONObject("week").getInt("number");
        if(year<2000||type<1||week<1)throw new IllegalArgumentException("NFL round metadata missing");
        Ref current=new Ref(year,type,week,label(type,week,"")),previous=null,last=null;
        JSONArray leagues=root.optJSONArray("leagues"),calendar=leagues==null||leagues.length()==0?null:leagues.getJSONObject(0).optJSONArray("calendar");
        for(int i=0;calendar!=null&&i<calendar.length();i++){
            JSONObject group=calendar.getJSONObject(i);int phase=group.optInt("value");JSONArray entries=group.optJSONArray("entries");
            for(int j=0;entries!=null&&j<entries.length();j++){
                JSONObject entry=entries.getJSONObject(j);int number=entry.optInt("value");Ref ref=new Ref(year,phase,number,label(phase,number,entry.optString("label")));
                if(ref.same(current)){current=ref;previous=last;}last=ref;
            }
        }
        if(previous==null&&week>1)previous=new Ref(year,type,week-1,label(type,week-1,""));
        JSONArray events=root.getJSONArray("events"),valid=new JSONArray();
        for(int i=0;i<events.length();i++){
            JSONObject event=events.getJSONObject(i),eventWeek=event.optJSONObject("week"),eventSeason=event.optJSONObject("season");
            if(eventWeek!=null&&eventWeek.optInt("number")!=week)continue;
            if(eventSeason!=null&&(eventSeason.optInt("year",year)!=year||eventSeason.optInt("type",type)!=type))continue;
            valid.put(event);
        }
        root.put("events",valid);return new Round(current,previous,at,SportsFeedParser.scoreboard("nfl",root,at));
    }
    private static String label(int type,int week,String label){
        if(type==2)return "Semana "+week;
        if(type==1)return "Pré-temporada · Semana "+week;
        if(label.contains("Wild Card"))return "Wild Card";
        if(label.contains("Divisional"))return "Rodada divisional";
        if(label.contains("Conference"))return "Finais de conferência";
        if(label.contains("Super Bowl"))return "Super Bowl";
        return "Playoffs · Rodada "+week;
    }
}
