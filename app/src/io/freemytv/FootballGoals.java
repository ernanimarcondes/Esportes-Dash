package io.freemytv;

import org.json.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.*;

/** Public match events only: the event team is the beneficiary, including own goals. */
final class FootballGoals {
    static final class Goal {
        final String team,player,shortName,playerId,minute;
        final boolean own,penalty;
        final double clock;
        Goal(JSONObject o){team=o.optString("team");player=o.optString("player");shortName=o.optString("shortName",player);playerId=o.optString("playerId");minute=o.optString("minute");own=o.optBoolean("own");penalty=o.optBoolean("penalty");clock=o.optDouble("clock",0);}
        JSONObject json()throws Exception{return new JSONObject().put("team",team).put("player",player).put("shortName",shortName).put("playerId",playerId).put("minute",minute).put("own",own).put("penalty",penalty).put("clock",clock);}
        String moment(){return (minute.isEmpty()?"min. n/d":minute)+(own?" (GC)":penalty?" (P)":"");}
    }
    static void read(SportsFeedParser.Game game,JSONObject competition){
        game.goalDetailsParsed=true;List<Goal> result=new ArrayList<>();Set<String> seen=new HashSet<>();JSONArray details=competition.optJSONArray("details");
        for(int i=0;details!=null&&i<details.length();i++)try{
            JSONObject event=details.getJSONObject(i);if(!event.optBoolean("scoringPlay")||event.optBoolean("shootout")||event.optInt("scoreValue",1)<=0)continue;
            JSONObject beneficiary=event.optJSONObject("team"),clock=event.optJSONObject("clock");
            String team=beneficiary==null?"":beneficiary.optString("id");
            if(!team.equals(game.homeId)&&!team.equals(game.awayId))continue;
            JSONArray athletes=event.optJSONArray("athletesInvolved");JSONObject athlete=athletes==null||athletes.length()==0?null:athletes.optJSONObject(0);
            String player=athlete==null?"Autor não informado":clean(athlete.optString("displayName",athlete.optString("fullName","Autor não informado")));
            if(player.isEmpty())player="Autor não informado";
            String id=athlete==null?"":athlete.optString("id"),shortName=athlete==null?player:clean(athlete.optString("shortName",player));
            double seconds=clock==null?0:clock.optDouble("value",0);String minute=clock==null?"":clock.optString("displayValue");
            minute=minute.replace('’','\'').replace('′','\'').replace("'+","+");
            if(minute.isEmpty()&&seconds>0)minute=(long)Math.ceil(seconds/60)+"'";
            boolean own=event.optBoolean("ownGoal"),penalty=event.optBoolean("penaltyKick");
            String key=team+"|"+id+"|"+player+"|"+minute+"|"+seconds+"|"+own+"|"+penalty;if(!seen.add(key))continue;
            result.add(new Goal(new JSONObject().put("team",team).put("player",player).put("shortName",shortName).put("playerId",id).put("minute",minute).put("clock",seconds).put("own",own).put("penalty",penalty)));
        }catch(Exception ignored){} // One incomplete event must not hide the match or other goals.
        result.sort(Comparator.comparingDouble(g->g.clock));game.goals=result;
    }
    static boolean complete(SportsFeedParser.Game game){
        try{int home=0,away=0;for(Goal goal:game.goals){if(goal.team.equals(game.homeId))home++;if(goal.team.equals(game.awayId))away++;}
            int expectedHome=Integer.parseInt(game.homeScore),expectedAway=Integer.parseInt(game.awayScore);
            if(game.source.equals("nhl")&&game.note.toLowerCase(Locale.ROOT).contains("shootout")){if(expectedHome>expectedAway)expectedHome--;else if(expectedAway>expectedHome)expectedAway--;}
            return home==expectedHome&&away==expectedAway;
        }catch(Exception ignored){return false;}
    }
    static List<String> lines(SportsFeedParser.Game game,boolean home){
        String team=home?game.homeId:game.awayId;LinkedHashMap<String,StringBuilder> grouped=new LinkedHashMap<>();int known=0;
        for(Goal goal:game.goals)if(goal.team.equals(team)){
            known++;String key=goal.playerId.isEmpty()?goal.player:goal.playerId;
            StringBuilder line=grouped.get(key);
            if(line==null){String name=goal.player.length()>24&&!goal.shortName.isEmpty()?goal.shortName:goal.player;line=new StringBuilder(name+"  ");grouped.put(key,line);}else line.append(", ");
            line.append(goal.moment());
        }
        List<String> result=new ArrayList<>();for(StringBuilder line:grouped.values())result.add(line.toString());
        try{int expected=Integer.parseInt(home?game.homeScore:game.awayScore),missing=expected-known;
            int other=Integer.MAX_VALUE;if(game.source.equals("nhl"))try{other=Integer.parseInt(home?game.awayScore:game.homeScore);}catch(Exception ignored){}
            if(game.source.equals("nhl")&&game.note.toLowerCase(Locale.ROOT).contains("shootout")&&expected>other&&missing==1){result.add("Vitória no shootout");missing=0;}
            if(missing>0)result.add(known==0?"Detalhes dos gols indisponíveis":missing==1?"1 gol sem detalhes na fonte":missing+" gols sem detalhes na fonte");}catch(Exception ignored){}
        return result;
    }
    private static String clean(String name){
        if(!name.contains("Ã")&&!name.contains("Â"))return name;
        for(int i=0;i<name.length();i++)if(name.charAt(i)>255)return name;
        try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(name.getBytes(StandardCharsets.ISO_8859_1))).toString();}catch(Exception ignored){return name;}
    }
}
