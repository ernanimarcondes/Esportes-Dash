package io.freemytv;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.OffsetDateTime;
import java.util.*;

/** Public-feed normalization, independent of Android so dates and competition rules can be checked on a JVM. */
final class SportsFeedParser {
    static final String[] TABLE_KEYS = {"bra.1", "bra.2", "eng.1", "fra.1", "esp.1", "por.1", "ger.1", "nba", "nfl", "nhl", "mlb"};
    static final String[] TABLE_NAMES = {"Brasileirão Série A", "Brasileirão Série B", "Premier League", "Ligue 1", "LaLiga", "Liga Portugal", "Bundesliga", "NBA", "NFL", "NHL", "MLB"};
    static final int FOOTBALL_COUNT = 7;
    static final String[] SCORE_KEYS = {"soccer", "nba", "nfl", "nhl", "mlb"};
    private static final Map<String,String> FOOTBALL = new LinkedHashMap<>();
    static {
        String[][] leagues = {{"630","Brasileirão Série A"},{"4007","Brasileirão Série B"},{"700","Premier League"},{"710","Ligue 1"},
                {"740","LaLiga"},{"715","Liga Portugal"},{"720","Bundesliga"},{"730","Serie A italiana"},
                {"783","Libertadores"},{"5454","Sul-Americana"},{"775","Champions League"},{"776","Europa League"},
                {"20296","Conference League"},{"3918","Copa da Inglaterra"},{"3920","Copa da Liga Inglesa"},
                {"3951","Copa do Rei"},{"3954","Copa da Alemanha"},{"3952","Copa da França"},{"20922","Taça de Portugal"},
                {"606","Copa do Mundo"},{"787","Eliminatórias sul-americanas"},{"786","Eliminatórias europeias"},
                {"781","Eurocopa"},{"3947","Eliminatórias da Euro"},{"2395","Liga das Nações"},
                {"780","Copa América"},{"3922","Amistosos de seleções"},{"8306","Copa do Brasil"}};
        for (String[] l : leagues) FOOTBALL.put(l[0], l[1]);
    }

    static final class Row {
        int rank;
        String id="", name="", abbr="", logo="", record="—", points="—", played="—";
        double order;
        JSONObject json() throws Exception { return new JSONObject().put("rank",rank).put("id",id).put("name",name)
                .put("abbr",abbr).put("logo",logo).put("record",record).put("points",points).put("played",played); }
        static Row read(JSONObject o) { Row r=new Row();r.rank=o.optInt("rank");r.id=o.optString("id");r.name=o.optString("name");
            r.abbr=o.optString("abbr");r.logo=o.optString("logo");r.record=o.optString("record","—");
            r.points=o.optString("points","—");r.played=o.optString("played","—");return r; }
    }
    static final class Table {
        String key="", title="", group="", season="";
        long at;
        List<Row> rows=new ArrayList<>();
        JSONObject json() throws Exception { JSONArray a=new JSONArray();for(Row r:rows)a.put(r.json());return new JSONObject()
                .put("key",key).put("title",title).put("group",group).put("season",season).put("at",at).put("rows",a); }
        static Table read(JSONObject o) { Table t=new Table();t.key=o.optString("key");t.title=o.optString("title");t.group=o.optString("group");
            t.season=o.optString("season");t.at=o.optLong("at");JSONArray a=o.optJSONArray("rows");for(int i=0;a!=null&&i<a.length();i++)t.rows.add(Row.read(a.optJSONObject(i)));return t; }
    }
    static final class Game {
        String id="", source="", league="", home="", away="", homeAbbr="", awayAbbr="", homeLogo="", awayLogo="";
        String homeScore="—", awayScore="—", state="pre", status="", broadcast="", note="", homeId="", awayId="";
        long at, fetched;
        boolean complete, timeValid=true, clubFilter, friendly, goalDetailsParsed;
        List<FootballGoals.Goal> goals=new ArrayList<>();
        JSONObject json() throws Exception { JSONObject result=new JSONObject().put("id",id).put("source",source).put("league",league)
                .put("home",home).put("away",away).put("homeAbbr",homeAbbr).put("awayAbbr",awayAbbr)
                .put("homeLogo",homeLogo).put("awayLogo",awayLogo).put("homeScore",homeScore).put("awayScore",awayScore)
                .put("state",state).put("status",status).put("broadcast",broadcast).put("note",note)
                .put("at",at).put("fetched",fetched).put("complete",complete).put("timeValid",timeValid)
                .put("homeId",homeId).put("awayId",awayId).put("clubFilter",clubFilter).put("friendly",friendly);
            if(source.equals("soccer")||source.equals("nhl")){JSONArray events=new JSONArray();for(FootballGoals.Goal goal:goals)events.put(goal.json());result.put("goalDetailsParsed",goalDetailsParsed).put("goals",events);}return result; }
        static Game read(JSONObject o) { Game g=new Game();g.id=o.optString("id");g.source=o.optString("source");g.league=o.optString("league");
            g.home=o.optString("home");g.away=o.optString("away");g.homeAbbr=o.optString("homeAbbr");g.awayAbbr=o.optString("awayAbbr");
            g.homeLogo=o.optString("homeLogo");g.awayLogo=o.optString("awayLogo");g.homeScore=o.optString("homeScore","—");
            g.awayScore=o.optString("awayScore","—");g.state=o.optString("state");g.status=o.optString("status");
            g.broadcast=o.optString("broadcast");g.note=o.optString("note");g.at=o.optLong("at");g.fetched=o.optLong("fetched");
            g.complete=o.optBoolean("complete");g.timeValid=o.optBoolean("timeValid",true);g.homeId=o.optString("homeId");g.awayId=o.optString("awayId");
            g.clubFilter=o.optBoolean("clubFilter");g.friendly=o.optBoolean("friendly");g.goalDetailsParsed=o.optBoolean("goalDetailsParsed");JSONArray goals=o.optJSONArray("goals");for(int i=0;goals!=null&&i<goals.length();i++){JSONObject goal=goals.optJSONObject(i);if(goal!=null)g.goals.add(new FootballGoals.Goal(goal));}return g; }
    }

    static List<Table> standings(String key,String body,long at) throws Exception {
        JSONObject root=new JSONObject(body);JSONObject season=root.optJSONObject("season");
        String label=season==null?"":season.optString("displayName",String.valueOf(season.optInt("year")));
        List<Table> out=new ArrayList<>();collect(root,key,label,at,out);
        if(out.isEmpty())throw new IllegalArgumentException("No standings groups");
        return out;
    }
    private static void collect(JSONObject group,String key,String season,long at,List<Table> out) throws Exception {
        JSONObject standings=group.optJSONObject("standings");JSONArray entries=standings==null?null:standings.optJSONArray("entries");
        if(entries!=null&&entries.length()>0){
            Table t=new Table();t.key=key;t.title=tableName(key);t.group=groupName(group.optString("name"),key);t.season=season;t.at=at;
            for(int i=0;i<entries.length();i++){
                JSONObject e=entries.getJSONObject(i),team=e.getJSONObject("team");Map<String,String> stats=new HashMap<>();
                JSONArray a=e.optJSONArray("stats");for(int j=0;a!=null&&j<a.length();j++){JSONObject s=a.getJSONObject(j);stats.put(s.optString("name"),s.optString("displayValue",s.optString("value")));}
                Row r=new Row();r.id=team.optString("id");r.name=team.optString("shortDisplayName",team.optString("displayName"));
                r.abbr=team.optString("abbreviation",r.name);r.logo=logo(team);
                r.rank=number(stats.get(key.contains(".")?"rank":"playoffSeed"),999);
                if(r.rank<=0)r.rank=999;
                r.points=value(stats,"points");r.played=value(stats,"gamesPlayed");
                r.record=value(stats,"wins")+"–"+value(stats,"losses");
                if(key.equals("nfl"))r.record+="–"+value(stats,"ties");
                if(key.equals("nhl"))r.record+="–"+value(stats,stats.containsKey("otLosses")?"otLosses":"overtimeLosses");
                r.order=decimal(stats.get("winPercent"));if(key.equals("nhl")||key.contains("."))r.order=decimal(stats.get("points"));
                t.rows.add(r);
            }
            t.rows.sort((a,b)->a.rank!=b.rank?Integer.compare(a.rank,b.rank):Double.compare(b.order,a.order));
            for(int i=0;i<t.rows.size();i++)t.rows.get(i).rank=i+1;
            out.add(t);
        }else{
            JSONArray children=group.optJSONArray("children");for(int i=0;children!=null&&i<children.length();i++)collect(children.getJSONObject(i),key,season,at,out);
        }
    }
    static Table f1Standings(String kind,String body,long at)throws Exception{
        JSONArray lists=new JSONObject(body).getJSONObject("MRData").getJSONObject("StandingsTable").getJSONArray("StandingsLists");
        if(lists.length()==0)throw new IllegalArgumentException("F1 standings unavailable");
        JSONObject standing=lists.getJSONObject(0);boolean drivers=kind.equals("driver");
        JSONArray entries=standing.getJSONArray(drivers?"DriverStandings":"ConstructorStandings");
        Table table=new Table();table.key=drivers?"f1-drivers":"f1-constructors";table.title="Fórmula 1";
        table.group=drivers?"Pilotos":"Construtores";table.season=standing.getString("season");table.at=at;
        for(int i=0;i<entries.length();i++){
            JSONObject entry=entries.getJSONObject(i);Row row=new Row();row.rank=entry.getInt("position");row.points=entry.getString("points");row.played=entry.optString("wins");
            if(drivers){JSONObject driver=entry.getJSONObject("Driver");row.id=driver.getString("driverId");
                row.name=driver.optString("givenName")+" "+driver.optString("familyName");row.abbr=driver.optString("code");
                JSONArray constructors=entry.optJSONArray("Constructors");row.record=constructors!=null&&constructors.length()>0?constructors.getJSONObject(0).optString("name"):"";
            }else{JSONObject team=entry.getJSONObject("Constructor");row.id=team.getString("constructorId");row.name=team.getString("name");row.abbr=row.id;row.record="";}
            table.rows.add(row);
        }
        table.rows.sort(Comparator.comparingInt(row->row.rank));if(table.rows.isEmpty())throw new IllegalArgumentException("Empty F1 standings");return table;
    }
    static String tableName(String key){for(int i=0;i<TABLE_KEYS.length;i++)if(TABLE_KEYS[i].equals(key))return TABLE_NAMES[i];return key;}
    private static String groupName(String name,String key){
        if(key.contains("."))return "Classificação geral";
        if(name.contains("Eastern"))return "Leste";if(name.contains("Western"))return "Oeste";
        if(name.contains("American Football"))return "AFC";if(name.contains("National Football"))return "NFC";
        if(name.contains("American League"))return "Americana";if(name.contains("National League"))return "Nacional";return name;
    }

    static List<Game> scoreboard(String source,String body,long fetched) throws Exception {
        return scoreboard(source,new JSONObject(body),fetched);
    }
    static List<Game> scoreboard(String source,JSONObject root,long fetched) throws Exception {
        JSONArray events=root.optJSONArray("events");
        if(events==null)throw new IllegalArgumentException("No events field");
        List<Game> out=new ArrayList<>();
        for(int i=0;i<events.length();i++)try{
            JSONObject event=events.getJSONObject(i);JSONArray competitions=event.getJSONArray("competitions");
            if(competitions.length()==0)continue;JSONObject c=competitions.getJSONObject(0);
            String league=source.toUpperCase(Locale.ROOT),leagueId="";
            if(source.equals("soccer")){
                String uid=event.optString("uid");int from=uid.indexOf("~l:");if(from<0)continue;
                String id=uid.substring(from+3).split("~")[0];leagueId=id;league=FOOTBALL.get(id);if(league==null)continue;
            }
            JSONArray competitors=c.getJSONArray("competitors");JSONObject home=null,away=null;
            for(int j=0;j<competitors.length();j++){JSONObject p=competitors.getJSONObject(j);if(p.optString("homeAway").equals("home"))home=p;else if(p.optString("homeAway").equals("away"))away=p;}
            if(home==null||away==null)continue;
            Game g=new Game();g.id=source+":"+event.getString("id");g.source=source;g.league=league;g.fetched=fetched;
            g.clubFilter=Arrays.asList("3918","3920","3951","3954","3952","20922","8306").contains(leagueId);
            g.friendly=leagueId.equals("3922");
            g.at=timestamp(event.optString("date",c.optString("date")));g.timeValid=c.optBoolean("timeValid",true);
            JSONObject type=c.optJSONObject("status");if(type==null)type=event.optJSONObject("status");type=type==null?null:type.optJSONObject("type");
            if(type!=null){g.state=type.optString("state","pre");g.complete=type.optBoolean("completed");g.status=type.optString("name");}
            JSONObject ht=home.getJSONObject("team"),at=away.getJSONObject("team");g.homeId=ht.optString("id");g.awayId=at.optString("id");g.home=ht.optString("shortDisplayName",ht.optString("displayName"));
            g.away=at.optString("shortDisplayName",at.optString("displayName"));g.homeAbbr=ht.optString("abbreviation",g.home);
            g.awayAbbr=at.optString("abbreviation",g.away);g.homeLogo=logo(ht);g.awayLogo=logo(at);
            g.homeScore=score(home.opt("score"));g.awayScore=score(away.opt("score"));
            if(home.has("shootoutScore")&&away.has("shootoutScore"))g.note="Pênaltis "+home.optString("shootoutScore")+"–"+away.optString("shootoutScore");
            if(source.equals("soccer"))FootballGoals.read(g,c);
            g.broadcast=broadcastBrazil(c);out.add(g);
        }catch(Exception ignored){} // A malformed event must not suppress other leagues' games.
        return out;
    }
    static String broadcastBrazil(JSONObject competition){
        LinkedHashSet<String> names=new LinkedHashSet<>();JSONArray a=competition.optJSONArray("geoBroadcasts");
        for(int i=0;a!=null&&i<a.length();i++){
            JSONObject g=a.optJSONObject(i);if(g==null)continue;String region=g.optString("region").toLowerCase(Locale.ROOT);
            if(!region.equals("br")&&!region.equals("bra")&&!region.equals("brazil")&&!region.equals("brasil"))continue;
            JSONObject media=g.optJSONObject("media");String name=media==null?"":media.optString("shortName",media.optString("name"));if(!name.isEmpty())names.add(name);
        }
        // Unqualified broadcasts are often US channels. Never present them as Brazilian carriage.
        return String.join(" / ",names);
    }
    static long timestamp(String value){try{return OffsetDateTime.parse(value).toInstant().toEpochMilli();}catch(Exception e){return 0;}}
    static boolean isToday(Game g,long now){return g.at>0&&java.time.Instant.ofEpochMilli(g.at).atZone(SportsDataZone.ZONE).toLocalDate()
            .equals(java.time.Instant.ofEpochMilli(now).atZone(SportsDataZone.ZONE).toLocalDate());}
    static boolean isRecent(Game g,long now){return g.complete&&g.at>0&&g.at>=now-72*3600000L&&g.at<=now;}
    static boolean featured(Game g,Set<String> clubs){
        if(g.clubFilter&&!clubs.isEmpty()&&!clubs.contains(g.homeId)&&!clubs.contains(g.awayId))return false;
        if(g.friendly){Set<String> major=new HashSet<>(Arrays.asList("BRA","ARG","FRA","ESP","POR","ENG","GER","ITA","URU","COL","NED","BEL","CRO","USA","MEX","JPN","KOR","SUI","DEN","MAR","SEN","NGA","ECU","AUS","CHI"));
            return major.contains(g.homeAbbr)||major.contains(g.awayAbbr);}
        return true;
    }
    private static final class SportsDataZone {static final java.time.ZoneId ZONE=java.time.ZoneId.of("America/Sao_Paulo");}
    private static String logo(JSONObject team){String value=team.optString("logo");JSONArray a=team.optJSONArray("logos");
        if(value.isEmpty()&&a!=null&&a.length()>0)value=a.optJSONObject(0).optString("href");
        return value.startsWith("https://a.espncdn.com/")?value:"";}
    private static String score(Object v){if(v==null||v==JSONObject.NULL)return "—";if(v instanceof JSONObject)return ((JSONObject)v).optString("displayValue",((JSONObject)v).optString("value","—"));return String.valueOf(v);}
    private static String value(Map<String,String> s,String k){String v=s.get(k);return v==null||v.isEmpty()?"—":v;}
    private static int number(String s,int fallback){try{return Integer.parseInt(s);}catch(Exception e){return fallback;}}
    private static double decimal(String s){try{return Double.parseDouble(s);}catch(Exception e){return 0;}}
}
