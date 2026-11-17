package io.freemytv;

import org.json.*;
import java.time.Instant;
import java.util.*;

/** Immutable full-season Grand Prix results, assembled across the existing podium feed pages. */
final class F1RaceResults {
    static final class Finish {
        final int position,fastestRank;final String name,team,points,time,status,fastestTime;
        Finish(JSONObject o){position=o.optInt("position");name=o.optString("name");team=o.optString("team");points=o.optString("points","0");time=o.optString("time");status=o.optString("status");fastestRank=o.optInt("fastestRank",0);fastestTime=o.optString("fastestTime");}
        String display(){
            String s=status.toLowerCase(Locale.ROOT);
            if(s.contains("disqual")||s.equals("excluded"))return "DSQ";
            if(s.equals("did not start"))return "DNS";
            if(s.contains("did not qualify"))return "DNQ";
            if(s.equals("not classified"))return "NC";
            if(s.matches("\\+?[0-9]+ laps?"))return status.replace("Laps","voltas").replace("Lap","volta");
            if(!s.isEmpty()&&!s.equals("finished")&&!s.equals("lapped"))return "DNF";
            if(!time.isEmpty())return time;
            if(s.equals("finished"))return "Concluído";
            if(s.equals("lapped"))return "Retardatário";
            return status.isEmpty()?"—":"DNF";
        }
    }
    static final class Race {
        final int round;final String name,circuit;final long at;final List<Finish> finishers;final Finish fastest;
        Race(JSONObject o)throws Exception{
            round=o.getInt("round");name=o.getString("name");circuit=o.optString("circuit");at=o.optLong("at");List<Finish> rows=new ArrayList<>();
            JSONArray a=o.getJSONArray("results");for(int i=0;i<a.length();i++)rows.add(new Finish(a.getJSONObject(i)));
            rows.sort(Comparator.comparingInt(r->r.position));finishers=Collections.unmodifiableList(rows);Finish best=null;for(Finish row:rows)if(row.fastestRank==1&&!row.fastestTime.isEmpty()){best=row;break;}fastest=best;
        }
    }
    final String season;final long at;final boolean hasLapData;final List<Race> races;private final String body;
    F1RaceResults(JSONObject o)throws Exception{
        season=o.getString("season");at=o.getLong("at");hasLapData=o.optInt("schema",1)>=2;List<Race> list=new ArrayList<>();JSONArray a=o.getJSONArray("races");
        for(int i=0;i<a.length();i++){Race race=new Race(a.getJSONObject(i));if(!race.finishers.isEmpty())list.add(race);}
        list.sort((x,y)->x.round!=y.round?Integer.compare(x.round,y.round):Long.compare(x.at,y.at));races=Collections.unmodifiableList(list);body=o.toString();
    }
    JSONObject json()throws Exception{return new JSONObject(body);}
    static final class Builder {
        final String season;final Map<Integer,JSONObject> races=new TreeMap<>();
        Builder(String season){this.season=season;}
        void add(JSONObject race)throws Exception{
            int round=race.getInt("round");JSONObject item=races.get(round);
            if(item==null){
                String time=race.optString("time");if(time.isEmpty())time="00:00:00Z";long at=Instant.parse(race.getString("date")+"T"+time).toEpochMilli();
                JSONObject circuit=race.optJSONObject("Circuit");
                item=new JSONObject().put("round",round).put("name",race.getString("raceName")).put("circuit",circuit==null?"":circuit.optString("circuitName")).put("at",at).put("results",new JSONArray());races.put(round,item);
            }
            JSONArray results=race.getJSONArray("Results"),out=item.getJSONArray("results");
            for(int i=0;i<results.length();i++){
                JSONObject r=results.getJSONObject(i),driver=r.getJSONObject("Driver"),team=r.optJSONObject("Constructor"),time=r.optJSONObject("Time"),fastest=r.optJSONObject("FastestLap"),lapTime=fastest==null?null:fastest.optJSONObject("Time");
                out.put(new JSONObject().put("position",r.getInt("position")).put("name",driver.optString("givenName")+" "+driver.optString("familyName"))
                    .put("team",team==null?"":team.optString("name")).put("points",r.optString("points","0")).put("status",r.optString("status"))
                    .put("time",time==null?"":time.optString("time")).put("fastestRank",fastest==null?0:fastest.optInt("rank",0)).put("fastestTime",lapTime==null?"":lapTime.optString("time")));
            }
        }
        F1RaceResults finish(long at)throws Exception{
            JSONArray list=new JSONArray();for(JSONObject race:races.values())list.put(race);
            return new F1RaceResults(new JSONObject().put("schema",2).put("season",season).put("at",at).put("races",list));
        }
    }
}
