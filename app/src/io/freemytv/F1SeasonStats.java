package io.freemytv;

import org.json.*;
import java.util.*;

/** Grand Prix podiums only; publish a snapshot only after every results page was received. */
final class F1SeasonStats {
    final String season;
    final long at;
    final int round;
    private final JSONObject drivers,constructors;
    F1SeasonStats(JSONObject o)throws Exception {
        season=o.getString("season");at=o.getLong("at");round=o.getInt("round");
        drivers=o.getJSONObject("drivers");constructors=o.getJSONObject("constructors");
    }
    String podiums(String expectedSeason,String id,boolean driver){
        return season.equals(expectedSeason)?String.valueOf((driver?drivers:constructors).optInt(id,0)):"—";
    }
    JSONObject json()throws Exception{return new JSONObject().put("season",season).put("at",at).put("round",round).put("drivers",drivers).put("constructors",constructors);}
    static final class Builder {
        final String season;
        int count,total=-1,round;
        final Set<String> seen=new HashSet<>();
        final JSONObject drivers=new JSONObject(),constructors=new JSONObject();
        Builder(String season){this.season=season;}
        void add(String body)throws Exception {
            JSONObject data=new JSONObject(body).getJSONObject("MRData"),table=data.getJSONObject("RaceTable");
            int expected=data.getInt("total");
            if(!season.equals(table.getString("season"))||data.getInt("offset")!=count||(total>=0&&total!=expected))throw new IllegalArgumentException("Results changed during pagination");
            total=expected;JSONArray races=table.getJSONArray("Races");int before=count;
            for(int i=0;i<races.length();i++){
                JSONObject race=races.getJSONObject(i);int raceRound=race.getInt("round");round=Math.max(round,raceRound);
                JSONArray results=race.getJSONArray("Results");
                for(int j=0;j<results.length();j++){
                    JSONObject result=results.getJSONObject(j);String driver=result.getJSONObject("Driver").getString("driverId");
                    if(!seen.add(raceRound+":"+driver))throw new IllegalArgumentException("Duplicate result");count++;
                    if(result.getInt("position")<=3){increment(drivers,driver);increment(constructors,result.getJSONObject("Constructor").getString("constructorId"));}
                }
            }
            if(count==before&&count<total)throw new IllegalArgumentException("Incomplete results page");
        }
        boolean complete(){return total>=0&&count==total;}
        F1SeasonStats finish(long at)throws Exception {
            if(!complete())throw new IllegalArgumentException("Incomplete season results");
            return new F1SeasonStats(new JSONObject().put("season",season).put("at",at).put("round",round).put("drivers",drivers).put("constructors",constructors));
        }
        private static void increment(JSONObject counts,String id)throws Exception{counts.put(id,counts.optInt(id,0)+1);}
    }
}
