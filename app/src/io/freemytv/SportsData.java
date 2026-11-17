package io.freemytv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/** Separate cache and bounded background fetching for the sports profile. No account APIs. */
final class SportsData {
    static final ZoneId ZONE=ZoneId.of("America/Sao_Paulo");
    static final class Snapshot {
        String key;long at;List<SportsFeedParser.Game> games=new ArrayList<>();
        JSONObject json() throws Exception {JSONArray a=new JSONArray();for(SportsFeedParser.Game g:games)a.put(g.json());return new JSONObject().put("key",key).put("at",at).put("games",a);}
        static Snapshot read(JSONObject o){Snapshot s=new Snapshot();s.key=o.optString("key");s.at=o.optLong("at");JSONArray a=o.optJSONArray("games");for(int i=0;a!=null&&i<a.length();i++)s.games.add(SportsFeedParser.Game.read(a.optJSONObject(i)));return s;}
    }
    private final Context context;
    private final Runnable changed;
    private final java.util.concurrent.atomic.AtomicLong modelVersion=new java.util.concurrent.atomic.AtomicLong();
    long modelVersion(){return modelVersion.get();}
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService feeds=Executors.newFixedThreadPool(3),pictures=Executors.newFixedThreadPool(2);
    private final Map<String,List<SportsFeedParser.Table>> tables=new ConcurrentHashMap<>();
    private final Map<String,Snapshot> snapshots=new ConcurrentHashMap<>();
    private static final class GoalSnapshot {final long at;final SportsFeedParser.Game game;GoalSnapshot(long at,SportsFeedParser.Game game){this.at=at;this.game=game;}}
    private final Map<String,GoalSnapshot> matchGoals=new ConcurrentHashMap<>();
    private final Map<String,Long> attempts=new ConcurrentHashMap<>();
    private final Map<String,Bitmap> sourceLogos=Collections.synchronizedMap(new LinkedHashMap<String,Bitmap>(24,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,Bitmap> e){return size()>24;}
    });
    private final Map<String,Long> logoFailures=new ConcurrentHashMap<>();
    private final Set<String> pending=Collections.synchronizedSet(new HashSet<>()),failed=Collections.synchronizedSet(new HashSet<>());
    private final Map<String,Bitmap> logos=Collections.synchronizedMap(new LinkedHashMap<String,Bitmap>(128,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,Bitmap> e){return size()>256;}
    });
    private volatile boolean closed,cacheLoaded;
    volatile F1SeasonStats f1Stats;
    volatile F1RaceResults f1Results;
    volatile float[] track;
    volatile String trackId="vegas",trackLength="6,201 km";
    SportsData(Context c,Runnable changed){
        context=c.getApplicationContext();this.changed=changed;
        try{load(new JSONObject(read(context.getAssets().open("sports/seed.json"))));}catch(Exception ignored){}
        try{f1Stats=new F1SeasonStats(new JSONObject(read(context.getAssets().open("sports/f1/stats-seed.json"))));}catch(Exception ignored){}
        try{f1Results=new F1RaceResults(new JSONObject(read(context.getAssets().open("sports/f1/results-seed.json"))));}catch(Exception ignored){}
        try{track=CircuitGeometry.parseTrack(read(context.getAssets().open("racing/vegas.geojson")));}catch(Exception ignored){}
        feeds.execute(()->{
            try{load(new JSONObject(read(new FileInputStream(file()))));}catch(Exception ignored){}
            loadGoalCache();cacheLoaded=true;notifyDataChanged();refresh();
        });
    }
    private void load(JSONObject root){
        if(root.optInt("schema")!=2)return;
        JSONObject stats=root.optJSONObject("f1Stats");
        if(stats!=null)try{F1SeasonStats incoming=new F1SeasonStats(stats);if(f1Stats==null||incoming.at>f1Stats.at)f1Stats=incoming;}catch(Exception ignored){}
        JSONObject results=root.optJSONObject("f1Results");
        if(results!=null)try{F1RaceResults incoming=new F1RaceResults(results);if(f1Results==null||incoming.at>f1Results.at)f1Results=incoming;}catch(Exception ignored){}
        JSONArray ts=root.optJSONArray("tables");Map<String,List<SportsFeedParser.Table>> incoming=new LinkedHashMap<>();
        for(int i=0;ts!=null&&i<ts.length();i++){SportsFeedParser.Table t=SportsFeedParser.Table.read(ts.optJSONObject(i));
            incoming.computeIfAbsent(t.key,k->new ArrayList<>()).add(t);}
        for(Map.Entry<String,List<SportsFeedParser.Table>> e:incoming.entrySet()){
            List<SportsFeedParser.Table> current=tables.get(e.getKey());
            if(current==null||current.isEmpty()||(!e.getValue().isEmpty()&&e.getValue().get(0).at>current.get(0).at))tables.put(e.getKey(),e.getValue());
        }
        JSONArray ss=root.optJSONArray("snapshots");for(int i=0;ss!=null&&i<ss.length();i++){
            Snapshot s=Snapshot.read(ss.optJSONObject(i));Snapshot old=snapshots.get(s.key);if(old==null||s.at>old.at)snapshots.put(s.key,s);
        }
    }
    List<SportsFeedParser.Table> tables(String key){List<SportsFeedParser.Table> t=tables.get(key);return t==null?Collections.emptyList():t;}
    List<SportsFeedParser.Game> games(boolean recent){
        long now=System.currentTimeMillis();LocalDate today=Instant.ofEpochMilli(now).atZone(ZONE).toLocalDate();
        Map<String,SportsFeedParser.Game> unique=new HashMap<>();Set<String> clubs=new HashSet<>();
        for(int i=0;i<SportsFeedParser.FOOTBALL_COUNT;i++)for(SportsFeedParser.Table t:tables(SportsFeedParser.TABLE_KEYS[i]))for(SportsFeedParser.Row r:t.rows)clubs.add(r.id);
        for(Snapshot s:snapshots.values())for(SportsFeedParser.Game g:s.games){
            if(g.at<=0||!SportsFeedParser.featured(g,clubs))continue;
            boolean include=recent?SportsFeedParser.isRecent(g,now):SportsFeedParser.isToday(g,now);
            if(!include)continue;SportsFeedParser.Game old=unique.get(g.id);if(old==null||g.fetched>old.fetched)unique.put(g.id,g);
        }
        List<SportsFeedParser.Game> sorted=new ArrayList<>(unique.values());
        // Stable chronological order across every competition, including completed games.
        sorted.sort(Comparator.comparingLong((SportsFeedParser.Game g)->g.at).thenComparing(g->g.id));
        return sorted;
    }

    void refresh(){
        if(closed||!cacheLoaded)return;long now=System.currentTimeMillis();LocalDate today=LocalDate.now(ZONE);
        for(String key:SportsFeedParser.TABLE_KEYS){List<SportsFeedParser.Table> list=tables(key);long at=list.isEmpty()?0:list.get(0).at;
            if(now-at>6*3600000L)fetchTable(key);}
        for(String kind:new String[]{"driver","constructor"}){List<SportsFeedParser.Table> list=tables("f1-"+kind+"s");
            if(list.isEmpty()||now-list.get(0).at>15*60000L)fetchF1(kind);}
        List<SportsFeedParser.Table> driverTables=tables("f1-drivers");
        if(!driverTables.isEmpty()){
            String season=driverTables.get(0).season;
            if(f1Stats==null||!f1Stats.season.equals(season)||now-f1Stats.at>15*60000L||f1Results==null||!f1Results.hasLapData||!f1Results.season.equals(season))fetchF1Stats(season);
        }
        for(int offset=-3;offset<=1;offset++){
            String date=today.plusDays(offset).format(DateTimeFormatter.BASIC_ISO_DATE);long ttl=offset>=0?5*60000L:30*60000L;
            for(String source:SportsFeedParser.SCORE_KEYS){String key=source+"-"+date;Snapshot s=snapshots.get(key);
                if(s==null||now-s.at>ttl||(source.equals("soccer")&&s.games.stream().anyMatch(g->!g.goalDetailsParsed)))fetchGames(source,date,key);}
        }
        snapshots.keySet().removeIf(k->{try{return LocalDate.parse(k.substring(k.lastIndexOf('-')+1),DateTimeFormatter.BASIC_ISO_DATE).isBefore(today.minusDays(3));}catch(Exception e){return true;}});
    }
    private boolean begin(String key){long now=System.currentTimeMillis();Long last=attempts.get(key);
        if(closed||(last!=null&&now-last<5*60000L)||!pending.add(key))return false;attempts.put(key,now);return true;}
    private void fetchTable(String key){String task="table-"+key;if(!begin(task))return;
        feeds.execute(()->{try{
            String url="https://site.api.espn.com/apis/v2/sports/"+path(key)+"/standings";
            if(key.equals("mlb"))url+="?season="+LocalDate.now(ZONE).getYear()+"&seasontype=2";
            List<SportsFeedParser.Table> parsed=SportsFeedParser.standings(key,get(url),System.currentTimeMillis());
            tables.put(key,parsed);failed.remove(task);save();
        }catch(Exception e){failed.add(task);android.util.Log.w("SportsData","Table unavailable: "+key);}
            finally{pending.remove(task);notifyDataChanged();}});
    }
    private void fetchF1(String kind){String task="table-f1-"+kind;if(!begin(task))return;
        feeds.execute(()->{try{
            SportsFeedParser.Table table=SportsFeedParser.f1Standings(kind,get("https://api.jolpi.ca/ergast/f1/current/"+kind+"standings.json?limit=100"),System.currentTimeMillis());
            tables.put(table.key,Collections.singletonList(table));failed.remove(task);save();
        }catch(Exception e){failed.add(task);android.util.Log.w("SportsData","F1 standings unavailable: "+kind);}
            finally{pending.remove(task);notifyChanged();}});
    }
    String f1Podiums(String season,String id,boolean driver){F1SeasonStats stats=f1Stats;return stats==null?"—":stats.podiums(season,id,driver);}
    private void fetchF1Stats(String season){
        if(!season.matches("[0-9]{4}"))return;String task="f1-podiums";if(!begin(task))return;
        feeds.execute(()->{try{
            F1SeasonStats.Builder builder=new F1SeasonStats.Builder(season);
            for(int page=0;page<30&&!builder.complete();page++)builder.add(get("https://api.jolpi.ca/ergast/f1/"+season+"/results.json?limit=100&offset="+builder.count));
            long at=System.currentTimeMillis();F1SeasonStats stats=builder.finish(at);F1RaceResults results=builder.resultSnapshot(at);f1Stats=stats;f1Results=results;failed.remove(task);save();
        }catch(Exception e){failed.add(task);android.util.Log.w("SportsData","F1 podium totals unavailable");}
            finally{pending.remove(task);notifyChanged();}});
    }
    List<String> goalLines(SportsFeedParser.Game game,boolean home){
        if(!game.source.equals("soccer")&&!game.source.equals("nhl"))return Collections.emptyList();
        if(FootballGoals.complete(game))return FootballGoals.lines(game,home);
        GoalSnapshot cached=matchGoals.get(game.id);boolean valid=cached!=null&&MatchGoalsSummary.sameScore(game,cached.game);
        long ttl=game.complete?30*60000L:5*60000L;
        if(!valid||System.currentTimeMillis()-cached.at>ttl||!FootballGoals.complete(cached.game))fetchGoalDetails(game);
        if(valid&&cached.game.goals.size()>=game.goals.size())return FootballGoals.lines(cached.game,home);
        String score=home?game.homeScore:game.awayScore;
        if(game.goals.isEmpty()&&!score.equals("0")&&pending.contains("goal-details:"+game.id))return Collections.singletonList("Carregando autores…");
        return FootballGoals.lines(game,home);
    }
    private void fetchGoalDetails(SportsFeedParser.Game game){
        String event=game.id.substring(game.id.indexOf(':')+1);if(!event.matches("[0-9]+"))return;
        String task="goal-details:"+game.id;if(!begin(task))return;
        feeds.execute(()->{try{
            String path=game.source.equals("nhl")?"hockey/nhl":"soccer/all";
            SportsFeedParser.Game detailed=MatchGoalsSummary.read(game,get("https://site.api.espn.com/apis/site/v2/sports/"+path+"/summary?event="+event));
            if(!closed){matchGoals.put(game.id,new GoalSnapshot(System.currentTimeMillis(),detailed));saveGoalCache();}
        }catch(Exception ignored){android.util.Log.w("SportsData","Goal summary unavailable: "+game.source);}
            finally{pending.remove(task);notifyChanged();}});
    }
    private void loadGoalCache(){try{
        JSONObject root=new JSONObject(read(new FileInputStream(new File(context.getFilesDir(),"match-goals-cache.json"))));if(root.optInt("schema")!=1)return;
        JSONArray rows=root.getJSONArray("matches");long oldest=System.currentTimeMillis()-4*86400000L;
        for(int i=0;i<Math.min(rows.length(),96);i++)try{JSONObject row=rows.getJSONObject(i);SportsFeedParser.Game game=SportsFeedParser.Game.read(row.getJSONObject("game"));if(game.at>=oldest)matchGoals.put(game.id,new GoalSnapshot(row.getLong("at"),game));}catch(Exception ignored){}
    }catch(Exception ignored){}}
    private synchronized void saveGoalCache(){if(closed)return;try{
        long oldest=System.currentTimeMillis()-4*86400000L;matchGoals.entrySet().removeIf(e->e.getValue().game.at<oldest);
        List<GoalSnapshot> rows=new ArrayList<>(matchGoals.values());rows.sort((a,b)->Long.compare(b.at,a.at));JSONArray matches=new JSONArray();
        for(int i=0;i<Math.min(rows.size(),96);i++){GoalSnapshot row=rows.get(i);matches.put(new JSONObject().put("at",row.at).put("game",row.game.json()));}
        File temp=new File(context.getFilesDir(),"match-goals-cache.tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(new JSONObject().put("schema",1).put("matches",matches).toString().getBytes(StandardCharsets.UTF_8));}
        temp.renameTo(new File(context.getFilesDir(),"match-goals-cache.json"));
    }catch(Exception ignored){}}
    private void fetchGames(String source,String date,String key){if(!begin(key))return;
        feeds.execute(()->{try{
            String url="https://site.api.espn.com/apis/site/v2/sports/"+(source.equals("soccer")?"soccer/all":path(source))
                    +"/scoreboard?dates="+date+"&limit=1000&region=br";
            Snapshot s=new Snapshot();s.key=key;s.at=System.currentTimeMillis();s.games=SportsFeedParser.scoreboard(source,get(url),s.at);
            snapshots.put(key,s);failed.remove(key);save();
        }catch(Exception e){failed.add(key);android.util.Log.w("SportsData","Schedule unavailable: "+key);}
            finally{pending.remove(key);notifyDataChanged();}});
    }
    private static String path(String key){switch(key){case "nba":return "basketball/nba";case "nfl":return "football/nfl";
        case "nhl":return "hockey/nhl";case "mlb":return "baseball/mlb";default:return "soccer/"+key;}}
    private synchronized void save(){if(closed)return;try{
        JSONArray ts=new JSONArray(),ss=new JSONArray();for(List<SportsFeedParser.Table> list:tables.values())for(SportsFeedParser.Table t:list)ts.put(t.json());
        for(Snapshot s:snapshots.values())ss.put(s.json());byte[] body=new JSONObject().put("schema",2).put("tables",ts).put("snapshots",ss).put("f1Stats",f1Stats==null?JSONObject.NULL:f1Stats.json()).put("f1Results",f1Results==null?JSONObject.NULL:f1Results.json()).toString().getBytes(StandardCharsets.UTF_8);
        File temp=new File(context.getFilesDir(),"sports-dashboard.tmp");try(FileOutputStream out=new FileOutputStream(temp)){out.write(body);}temp.renameTo(file());
    }catch(Exception ignored){}}
    private File file(){return new File(context.getFilesDir(),"sports-dashboard.json");}
    String status(){if(!failed.isEmpty())return "Parte das fontes indisponível · dados salvos";return pending.isEmpty()?"Atualização periódica · horários de Brasília":"Atualizando fontes…";}
    String gamesStatus(boolean recent){
        LocalDate today=LocalDate.now(ZONE);long now=System.currentTimeMillis(),oldest=Long.MAX_VALUE;int covered=0;
        for(int offset=recent?-3:0;offset<=1;offset++)for(String source:SportsFeedParser.SCORE_KEYS){
            Snapshot s=snapshots.get(source+"-"+today.plusDays(offset).format(DateTimeFormatter.BASIC_ISO_DATE));if(s!=null){covered++;oldest=Math.min(oldest,s.at);}}
        if(covered==0)return "Consultando programação…";
        if(covered<(recent?25:10))return "Cobertura parcial · algumas fontes indisponíveis";
        if(now-oldest>60*60000L)return "Dados salvos · aguardando atualização";
        return recent?"Últimas 72 horas · somente partidas encerradas":"Hoje · horários de Brasília";
    }
    /** A single alpha-aware reduction from the full original to the physical on-screen pixel size. */
    Bitmap logo(String address,int pixels){
        final String[] candidates=TeamLogoPolicy.candidates(address);if(candidates.length==0)return null;
        final String url=candidates[0];
        final int target=Math.max(8,Math.min(192,pixels));final String rasterKey=url+"#"+target;
        Bitmap cached=logos.get(rasterKey);if(cached!=null||closed)return cached;
        Long failure=logoFailures.get(url);if(failure!=null&&System.currentTimeMillis()-failure<10*60000L)return null;
        String key="logo:"+rasterKey;if(!pending.add(key))return null;
        pictures.execute(()->{try{
            File dir=new File(context.getCacheDir(),"sports-shields-hq-v2");dir.mkdirs();String name=hash(url);
            File rasterFile=new File(dir,name+"-"+target+".png");Bitmap ready=rasterFile.exists()?BitmapFactory.decodeFile(rasterFile.getPath()):null;
            if(ready!=null)rasterFile.setLastModified(System.currentTimeMillis());
            if(ready==null){
                Bitmap original=null;
                for(String candidate:candidates){
                    try{original=originalLogo(candidate,dir);}catch(Exception ignored){}
                    if(original!=null)break;
                }
                if(original!=null){ready=ShieldRasterizer.reduce(original,target);
                    try(FileOutputStream out=new FileOutputStream(rasterFile)){ready.compress(Bitmap.CompressFormat.PNG,100,out);}}
                File[] all=dir.listFiles();if(all!=null&&all.length>800){Arrays.sort(all,Comparator.comparingLong(File::lastModified));for(int i=0;i<all.length-650;i++)all[i].delete();}
            }
            if(ready!=null&&!closed){logos.put(rasterKey,ready);logoFailures.remove(url);}else logoFailures.put(url,System.currentTimeMillis());
        }catch(Exception ignored){logoFailures.put(url,System.currentTimeMillis());}finally{pending.remove(key);notifyChanged();}});return null;
    }
    private Bitmap originalLogo(String url,File dir)throws Exception{
        Bitmap original=sourceLogos.get(url);File originalFile=new File(dir,hash(url)+".png");
        if(original==null&&originalFile.exists())original=BitmapFactory.decodeFile(originalFile.getPath());
        if(original==null){
            byte[] raw=bytes(url,3000000);BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(raw,0,raw.length,options);
            if(options.outWidth<=0||options.outHeight<=0)return null;
            options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>1024)options.inSampleSize*=2;
            options.inJustDecodeBounds=false;options.inScaled=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
            original=BitmapFactory.decodeByteArray(raw,0,raw.length,options);
            if(original!=null){original=cropShield(original);try(FileOutputStream out=new FileOutputStream(originalFile)){original.compress(Bitmap.CompressFormat.PNG,100,out);}}
        }
        if(original!=null){sourceLogos.put(url,original);originalFile.setLastModified(System.currentTimeMillis());}return original;
    }
    private static Bitmap cropShield(Bitmap original){
        int w=original.getWidth(),h=original.getHeight(),left=w,top=h,right=-1,bottom=-1;int[] colors=new int[w*h];original.getPixels(colors,0,w,0,0,w,h);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((colors[y*w+x]>>>24)>8){left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}
        if(right<left||bottom<top)return original;
        return Bitmap.createBitmap(original,left,top,right-left+1,bottom-top+1);
    }
    void ensureTrack(String id){if(id==null||id.isEmpty()||id.equals(trackId)||closed)return;
        String code=CircuitGeometry.trackCode(id);if(code==null||!begin("track:"+id))return;
        feeds.execute(()->{try{
            String body=get("https://raw.githubusercontent.com/bacinger/f1-circuits/master/circuits/"+code+".geojson");float[] geometry=CircuitGeometry.parseTrack(body);
            JSONObject properties=new JSONObject(body).getJSONArray("features").getJSONObject(0).getJSONObject("properties");
            track=geometry;trackId=id;trackLength=String.format(new Locale("pt","BR"),"%.3f km",properties.optDouble("length")/1000);
        }catch(Exception ignored){}finally{pending.remove("track:"+id);notifyChanged();}});
    }
    private static String hash(String value) throws Exception {byte[] d=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(int i=0;i<12;i++)out.append(String.format(Locale.ROOT,"%02x",d[i]&255));return out.toString();}
    private static String get(String url) throws Exception {return new String(bytes(url,5000000),StandardCharsets.UTF_8);}
    private static byte[] bytes(String address,int limit) throws Exception {HttpURLConnection conn=(HttpURLConnection)new URL(address).openConnection();
        conn.setConnectTimeout(10000);conn.setReadTimeout(12000);conn.setRequestProperty("User-Agent","TV-Dashboard/1.5 sports profile");
        try{if(conn.getResponseCode()!=200)throw new IOException("Feed unavailable");return all(conn.getInputStream(),limit);}finally{conn.disconnect();}}
    private static String read(InputStream in)throws Exception{return new String(all(in,10000000),StandardCharsets.UTF_8);}
    private static byte[] all(InputStream input,int limit)throws Exception{try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){
        byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>limit)throw new IOException("Response limit");out.write(buffer,0,n);}return out.toByteArray();}}
    private void notifyDataChanged(){modelVersion.incrementAndGet();notifyChanged();}
    private void notifyChanged(){main.post(()->{if(!closed)changed.run();});}
    void close(){closed=true;feeds.shutdownNow();pictures.shutdownNow();logos.clear();sourceLogos.clear();matchGoals.clear();}
}
