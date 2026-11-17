package io.freemytv;

import org.json.*;
import java.util.*;

/** Extract only scorers from a public match summary, validating match and team identity first. */
final class MatchGoalsSummary {
    static SportsFeedParser.Game read(SportsFeedParser.Game source,String body)throws Exception{
        JSONObject root=new JSONObject(body),header=root.getJSONObject("header");String event=source.id.substring(source.id.indexOf(':')+1);
        if(!header.getString("id").equals(event))throw new IllegalArgumentException("Different match summary");
        JSONObject competition=header.getJSONArray("competitions").getJSONObject(0);JSONArray teams=competition.getJSONArray("competitors");
        JSONObject home=null,away=null;
        for(int i=0;i<teams.length();i++){JSONObject team=teams.getJSONObject(i);if(team.optString("homeAway").equals("home"))home=team;else if(team.optString("homeAway").equals("away"))away=team;}
        if(home==null||away==null||!home.getJSONObject("team").getString("id").equals(source.homeId)||!away.getJSONObject("team").getString("id").equals(source.awayId))throw new IllegalArgumentException("Different summary teams");
        SportsFeedParser.Game result=SportsFeedParser.Game.read(source.json());result.homeScore=home.optString("score");result.awayScore=away.optString("score");
        if(source.source.equals("soccer")){FootballGoals.read(result,competition);return result;}
        if(!source.source.equals("nhl"))throw new IllegalArgumentException("Not a goal-based sport");
        List<FootballGoals.Goal> goals=new ArrayList<>();Set<String> seen=new HashSet<>();JSONArray plays=root.optJSONArray("plays");
        if(plays==null)throw new IllegalArgumentException("No scoring detail coverage");
        for(int i=0;i<plays.length();i++)try{
            JSONObject play=plays.getJSONObject(i);if(!play.optBoolean("scoringPlay"))continue;
            JSONObject type=play.optJSONObject("type"),period=play.optJSONObject("period"),clock=play.optJSONObject("clock"),team=play.optJSONObject("team");
            if(type==null||!type.optString("id").equals("505")||period==null||team==null)continue;
            String periodText=period.optString("displayValue").toLowerCase(Locale.ROOT);
            if(play.optBoolean("shootout")||periodText.contains("shootout")||periodText.equals("so"))continue;
            String beneficiary=team.optString("id");if(!beneficiary.equals(source.homeId)&&!beneficiary.equals(source.awayId))continue;
            JSONArray participants=play.optJSONArray("participants");JSONObject athlete=null;
            for(int j=0;participants!=null&&j<participants.length();j++){JSONObject participant=participants.getJSONObject(j);if(participant.optString("type").equals("scorer")){athlete=participant.optJSONObject("athlete");break;}}
            String player=athlete==null?"Autor não informado":athlete.optString("displayName","Autor não informado"),id=athlete==null?"":athlete.optString("id"),shortName=athlete==null?player:athlete.optString("shortName",player);
            int number=period.optInt("number",1);String time=clock==null?"":clock.optString("displayValue");
            String minute=(number<=3?"P"+number:number==4?"PR":"PR"+(number-3))+(time.isEmpty()?" · tempo n/d":" "+time);
            String key=play.optString("id",beneficiary+"|"+id+"|"+minute);if(!seen.add(key))continue;
            double seconds=0;String[] parts=time.split(":");if(parts.length==2)seconds=Double.parseDouble(parts[0])*60+Double.parseDouble(parts[1]);
            goals.add(new FootballGoals.Goal(new JSONObject().put("team",beneficiary).put("player",player).put("shortName",shortName).put("playerId",id).put("minute",minute).put("clock",(number-1)*1200+seconds)));
        }catch(Exception ignored){}
        goals.sort(Comparator.comparingDouble(g->g.clock));result.goals=goals;result.goalDetailsParsed=true;
        JSONObject status=competition.optJSONObject("status");JSONObject statusType=status==null?null:status.optJSONObject("type");
        String finish=statusType==null?"":(statusType.optString("detail")+" "+statusType.optString("shortDetail")).toLowerCase(Locale.ROOT);
        if(finish.contains("shootout")||finish.contains("/so"))result.note="Decidido no shootout";
        return result;
    }
    static boolean sameScore(SportsFeedParser.Game source,SportsFeedParser.Game details){return source.id.equals(details.id)&&source.homeId.equals(details.homeId)&&source.awayId.equals(details.awayId)&&source.homeScore.equals(details.homeScore)&&source.awayScore.equals(details.awayScore);}
}
