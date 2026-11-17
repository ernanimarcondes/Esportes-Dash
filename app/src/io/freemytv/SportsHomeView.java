package io.freemytv;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.*;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import java.text.SimpleDateFormat;
import java.util.*;

/** Eight-zone sports dashboard, with independent rotation clocks and a shared app dock. */
final class SportsHomeView extends RacingHomeView {
    private static final int ACCENT=0xFF59C7FF,AMBER=0xFFFFCA5C,WHITE=0xFFE8EDF2,MUTED=0xFFA7B1BE,PANEL=0xFF151A21,BORDER=0xFF64748B;
    private static final long FOOTBALL_INTERVAL=30000L, GAME_INTERVAL=10000L, AMERICAN_INTERVAL=15000L;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF box=new RectF();private final Path circuit=new Path();
    private final Typeface normal,bold;
    private final SportsData sports;
    private F1DetailsScreen f1Details;
    private boolean f1Open;
    private final RectF[] areas=new RectF[13];
    private final SimpleDateFormat clock=fmt("HH:mm"),date=fmt("dd 'de' MMMM"),weekday=fmt("EEEE"),time=fmt("HH:mm"),shortDate=fmt("dd/MM"),todayDate=fmt("dd/MM/yyyy"),stamp=fmt("dd/MM HH:mm"),session=fmt("EEE dd/MM · HH:mm"),sessionDay=fmt("EEE dd/MM");
    private float zoom=1,ox,oy;private int focus=7;private boolean running;
    private long activeTime,startTime,nextRefresh,lastGames,nextWarm;
    private List<SportsFeedParser.Game> today=new ArrayList<>(),recent=new ArrayList<>();
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(!running)return;long now=SystemClock.elapsedRealtime();
        if(now-lastGames>=5000){today=sports.games(false);recent=sports.games(true);lastGames=now;}
        if(now>=nextRefresh){sports.refresh();nextRefresh=now+60000;}
        invalidate();postDelayed(this,500);
    }};
    SportsHomeView(Activity activity){
        super(activity);normal=Ui.medium;Typeface strong;
        try{strong=Typeface.createFromAsset(activity.getAssets(),"fonts/rajdhani/Rajdhani-Bold.ttf");}catch(Exception e){strong=Ui.displaySemibold;}bold=strong;
        p.setSubpixelText(true);p.setDither(true);p.setHinting(Paint.HINTING_ON);
        sports=new SportsData(activity,()->invalidate());today=sports.games(false);recent=sports.games(true);
        areas[0]=new RectF(48,138,456,926);areas[1]=new RectF(478,138,1158,514);areas[2]=new RectF(478,536,1158,926);
        areas[3]=new RectF(1180,138,1872,514);areas[4]=new RectF(1180,536,1396,926);
        areas[5]=new RectF(1418,536,1634,926);areas[6]=new RectF(1656,536,1872,926);
        for(int i=0;i<6;i++)areas[7+i]=new RectF(48+i*308,954,48+i*308+284,1022);
        setContentDescription("Esportes Dash. Futebol, jogos do dia, resultados recentes, Fórmula 1, NBA, NFL, NHL e MLB. Aplicativos e ajustes abaixo. Use as setas e OK para detalhes.");
    }
    private static SimpleDateFormat fmt(String pattern){SimpleDateFormat f=new SimpleDateFormat(pattern,new Locale("pt","BR"));f.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));return f;}
    @Override void resume(){super.resume();if(running)return;running=true;startTime=SystemClock.elapsedRealtime();nextRefresh=lastGames=0;post(tick);}
    @Override void pause(){if(running)activeTime+=SystemClock.elapsedRealtime()-startTime;running=false;removeCallbacks(tick);super.pause();}
    @Override void destroy(){pause();sports.close();if(f1Details!=null)f1Details.close();super.destroy();}
    @Override void reset(boolean animate){f1Open=false;focus=7;invalidate();}
    @Override public void onData(){invalidate();}
    private long elapsed(){return activeTime+(running?SystemClock.elapsedRealtime()-startTime:0);}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){zoom=Math.min(w/1920f,h/1080f);ox=(w-1920*zoom)/2;oy=(h-1080*zoom)/2;}
    @Override protected void onDraw(Canvas c){
        c.drawColor(Color.BLACK);c.save();c.translate(ox,oy);c.scale(zoom,zoom);p.setStyle(Paint.Style.FILL);
        if(f1Open){f1Details.draw(c);c.restore();return;}
        header(c);football(c);games(c,1,false);games(c,2,true);formula(c);narrow(c,4);narrow(c,5);narrow(c,6);
        String[] dock={"YouTube","Prime Video","Spotify","Aplicativos","TV / HDMI","Ajustes"};
        for(int i=0;i<dock.length;i++){panel(c,7+i);text(c,dock[i],areas[7+i].left+23,998,23,WHITE,true);right(c,"›",areas[7+i].right-23,998,28,ACCENT,false);}
        if(SystemClock.elapsedRealtime()>=nextWarm){nextWarm=SystemClock.elapsedRealtime()+1500;warmNext();}
        text(c,sports.status(),48,1062,17,MUTED,false);right(c,"SETAS para navegar · OK para detalhes",1872,1062,17,MUTED,false);c.restore();
    }
    /** Prepare the next page before it becomes visible; current-page image jobs are enqueued by drawing first. */
    private void warmNext(){
        int matchPixels=Math.max(8,Math.round(66*zoom));
        for(List<SportsFeedParser.Game> list:Arrays.asList(today,recent))if(!list.isEmpty()){
            int pages=(list.size()+1)/2,page=((int)(elapsed()/GAME_INTERVAL)+1)%pages;
            for(int i=page*2;i<Math.min(page*2+2,list.size());i++){
                SportsFeedParser.Game g=list.get(i);sports.logo(g.homeLogo,matchPixels);sports.logo(g.awayLogo,matchPixels);
            }
        }
        int phase=(int)(elapsed()/AMERICAN_INTERVAL)+1;
        for(int id=4;id<=6;id++){
            SportsFeedParser.Table t=narrowTable(id,phase);if(t==null)continue;
            int pages=Math.max(1,(t.rows.size()+7)/8),page=(phase/(id==6?4:2))%pages;
            for(int i=page*8;i<Math.min(page*8+8,t.rows.size());i++)sports.logo(t.rows.get(i).logo,Math.max(8,Math.round(28*zoom)));
        }
        int football=((int)(elapsed()/FOOTBALL_INTERVAL)+1)%SportsFeedParser.FOOTBALL_COUNT;
        for(SportsFeedParser.Table t:sports.tables(SportsFeedParser.TABLE_KEYS[football]))for(SportsFeedParser.Row r:t.rows)sports.logo(r.logo,Math.max(8,Math.round(28*zoom)));
    }
    private void header(Canvas c){
        Date now=new Date();text(c,clock.format(now),48,94,67,WHITE,false,Ui.light);
        text(c,weekday.format(now).toUpperCase(new Locale("pt","BR")),240,51,18,ACCENT,false);
        text(c,date.format(now),240,88,27,WHITE,false);
        HomeData.Weather w=data.weather;String temperature=Double.isNaN(w.temp)?"—°":Math.round(w.temp)+"°";
        String city=Prefs.city().toUpperCase(new Locale("pt","BR")),summary=condition(w.code);
        p.setTypeface(bold);p.setTextSize(58);float temperatureWidth=p.measureText(temperature),labelsRight=1872-temperatureWidth-26;
        p.setTypeface(bold);p.setTextSize(19);float cityWidth=p.measureText(city);
        p.setTypeface(normal);p.setTextSize(20);float labelsWidth=Math.max(cityWidth,p.measureText(summary)),labelsLeft=labelsRight-labelsWidth;
        weatherIcon(c,labelsLeft-46,68,21,w.code);
        verticalText(c,city,labelsLeft,51,19,ACCENT,true);verticalText(c,summary,labelsLeft,83,20,MUTED,false);
        verticalText(c,temperature,1872-temperatureWidth,68,58,WHITE,true);
    }
    private void football(Canvas c){
        RectF b=areas[0];panel(c,0);int phase=(int)(elapsed()/FOOTBALL_INTERVAL)%SportsFeedParser.FOOTBALL_COUNT;
        String key=SportsFeedParser.TABLE_KEYS[phase];List<SportsFeedParser.Table> list=sports.tables(key);SportsFeedParser.Table t=list.isEmpty()?null:list.get(0);
        text(c,"FUTEBOL",b.left+22,b.top+35,18,ACCENT,true);
        text(c,fit(SportsFeedParser.tableName(key),30,b.width()-42,true),b.left+22,b.top+76,30,WHITE,true);
        text(c,t==null?"Consultando classificação…":"Temporada "+season(t.season),b.left+22,b.top+105,17,MUTED,false);
        if(t!=null){
            text(c,"#",b.left+20,b.top+135,15,MUTED,false);text(c,"CLUBE",b.left+85,b.top+135,15,MUTED,false);
            right(c,"J",b.right-75,b.top+135,15,MUTED,false);right(c,"PTS",b.right-20,b.top+135,15,ACCENT,true);
            float step=Math.min(29.5f,(b.height()-198)/Math.max(1,t.rows.size()));
            for(int i=0;i<t.rows.size();i++){
                SportsFeedParser.Row r=t.rows.get(i);float y=b.top+163+i*step;
                if(i%2==0)fill(c,b.left+12,y-22,b.right-12,y+6,0xFF1E2732,4);
                text(c,String.valueOf(r.rank),b.left+20,y,19,MUTED,false);logo(c,r.logo,r.abbr,b.left+50,y-24,28);
                text(c,fit(r.name,20,b.width()-169,false),b.left+85,y,20,WHITE,false);
                right(c,r.played,b.right-75,y,18,MUTED,false);right(c,r.points,b.right-20,y,22,WHITE,true);
            }
            text(c,"Atual. "+stamp.format(new Date(t.at)),b.left+22,b.bottom-20,15,MUTED,false);
        }else empty(c,b,"Fonte ainda não disponível");
    }
    private SportsFeedParser.Table narrowTable(int id,int phase){
        String key=id==4?"nba":id==5?"nfl":phase%4<2?"nhl":"mlb";
        List<SportsFeedParser.Table> groups=sports.tables(key);int index=phase%2;
        return groups.size()>index?groups.get(index):null;
    }
    private void narrow(Canvas c,int id){
        RectF b=areas[id];panel(c,id);int phase=(int)(elapsed()/AMERICAN_INTERVAL);SportsFeedParser.Table t=narrowTable(id,phase);
        String league=id==4?"NBA":id==5?"NFL":phase%4<2?"NHL":"MLB";
        String group=t==null?(league.equals("NFL")?(phase%2==0?"AFC":"NFC"):league.equals("MLB")?(phase%2==0?"Americana":"Nacional"):(phase%2==0?"Leste":"Oeste")):t.group;
        text(c,league,b.left+17,b.top+39,28,ACCENT,true);text(c,group,b.left+17,b.top+73,23,WHITE,true);
        if(t==null){text(c,"Consultando…",b.left+17,b.top+153,19,MUTED,false);return;}
        int pages=Math.max(1,(t.rows.size()+7)/8);int page=(phase/(id==6?4:2))%pages;int from=page*8,to=Math.min(from+8,t.rows.size());
        right(c,"#",b.left+32,b.top+106,14,MUTED,false);center(c,"TIME",b.left+94,b.top+106,14,MUTED,false);
        right(c,league.equals("NHL")?"PTS":league.equals("NFL")?"V-D-E":"V-D",b.right-15,b.top+106,14,MUTED,false);
        for(int i=from;i<to;i++){
            SportsFeedParser.Row r=t.rows.get(i);float y=b.top+134+(i-from)*31;
            if(i%2==0)fill(c,b.left+9,y-20,b.right-9,y+6,0xFF1E2732,4);
            right(c,String.valueOf(r.rank),b.left+32,y,17,MUTED,false);logo(c,r.logo,r.abbr,b.left+49,y-21,28);
            center(c,fit(r.abbr,18,46,true),b.left+114,y,18,WHITE,true);
            right(c,league.equals("NHL")?r.points:r.record,b.right-15,y,league.equals("NFL")?17:19,WHITE,true);
        }
        text(c,season(t.season),b.left+17,b.bottom-16,15,MUTED,false);
    }
    private void games(Canvas c,int id,boolean completed){
        RectF b=areas[id];panel(c,id);List<SportsFeedParser.Game> all=completed?recent:today;
        text(c,completed?"RESULTADOS RECENTES":"JOGOS DO DIA - "+todayDate.format(new Date()),b.left+22,b.top+34,21,ACCENT,true);
        if(all.isEmpty()){
            text(c,completed?"Nenhuma partida encerrada no período":"Sem jogos na programação recebida",b.left+24,b.top+136,24,WHITE,true);
            text(c,fit(sports.gamesStatus(completed),18,b.width()-48,false),b.left+24,b.top+175,18,MUTED,false);
        }else{
            int pages=(all.size()+1)/2,page=(int)(elapsed()/GAME_INTERVAL)%pages,from=page*2,to=Math.min(from+2,all.size());
            float row=(b.height()-69)/2;
            for(int i=from;i<to;i++){
                SportsFeedParser.Game g=all.get(i);float y=b.top+61+(i-from)*row;
                if(i>from)line(c,b.left+22,y-9,b.right-22,y-9,0xFF2F3B49,1);
                String label=g.league+(completed?" · "+shortDate.format(new Date(g.at)):"");
                text(c,fit(label,20,b.width()-50,false),b.left+24,y+16,20,ACCENT,false);
                logo(c,g.homeLogo,g.homeAbbr,b.left+24,y+43,66);logo(c,g.awayLogo,g.awayAbbr,b.right-90,y+43,66);
                text(c,fit(g.home,28,172,true),b.left+104,y+84,28,WHITE,true);
                right(c,fit(g.away,28,172,true),b.right-104,y+84,28,WHITE,true);
                String middle=g.complete||g.state.equals("in")?g.homeScore+"  –  "+g.awayScore:g.timeValid?time.format(new Date(g.at)):"A definir";
                center(c,middle,b.centerX(),y+84,34,WHITE,true);
                String below=gameStatus(g);
                if(!below.isEmpty())center(c,fit(below,18,b.width()-48,false),b.centerX(),y+132,18,MUTED,false);
            }
        }
    }
    private F1Rotation.Frame f1Frame(){
        List<SportsFeedParser.Table> drivers=sports.tables("f1-drivers");
        return F1Rotation.at(elapsed(),drivers.isEmpty()?0:drivers.get(0).rows.size());
    }
    private SportsFeedParser.Table f1Table(){
        int phase=f1Frame().phase;if(phase==F1Rotation.PROGRAM)return null;
        List<SportsFeedParser.Table> tables=sports.tables(phase==F1Rotation.DRIVERS?"f1-drivers":"f1-constructors");return tables.isEmpty()?null:tables.get(0);
    }
    private void formula(Canvas c){
        if(f1Frame().phase!=F1Rotation.PROGRAM){formulaStandings(c);return;}
        RectF b=areas[3];panel(c,3);text(c,"FÓRMULA 1",b.left+23,b.top+34,21,ACCENT,true);
        text(c,fit(agenda.raceName,35,325,true),b.left+23,b.top+79,35,WHITE,true);
        text(c,fit(agenda.circuitName,18,325,false),b.left+23,b.top+108,18,MUTED,false);
        List<RacingAgenda.Event> weekend=agenda.currentRaceSessions();RacingAgenda.Event next=null;
        for(RacingAgenda.Event e:weekend)if(e.at>System.currentTimeMillis()){next=e;break;}
        text(c,"PROGRAMAÇÃO DO GP",b.left+28,b.top+137,14,ACCENT,true);
        int rows=Math.min(7,weekend.size());float gap=rows>5?19:24;
        for(int i=0;i<rows;i++){
            RacingAgenda.Event e=weekend.get(i);float y=b.top+163+i*gap;int color=e==next?WHITE:MUTED;
            text(c,fit(sessionName(e.title),rows>5?15:17,72,true),b.left+28,y,rows>5?15:17,color,e==next);
            text(c,sessionDay.format(new Date(e.at)),b.left+104,y,14,color,false);
            right(c,e.dateOnly?"—":time.format(new Date(e.at)),b.left+252,y,17,e==next?AMBER:WHITE,true);
        }
        sports.ensureTrack(agenda.circuitId);float[] points=sports.track;
        if(points!=null&&sports.trackId.equals(agenda.circuitId)){
            float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
            for(int i=0;i<points.length;i+=2){float x=-points[i+1],y=points[i];minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}
            float size=Math.min(300/Math.max(.01f,maxX-minX),168/Math.max(.01f,maxY-minY));
            circuit.reset();float firstX=0,firstY=0;
            for(int i=0;i<points.length;i+=2){float x=b.left+509+(-points[i+1]-(minX+maxX)/2)*size,y=b.top+165+(points[i]-(minY+maxY)/2)*size;
                if(i==0){circuit.moveTo(x,y);firstX=x;firstY=y;}else circuit.lineTo(x,y);}
            p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeWidth(14);p.setColor(0xFF234557);c.drawPath(circuit,p);
            p.setStrokeWidth(4);p.setColor(ACCENT);c.drawPath(circuit,p);p.setStyle(Paint.Style.FILL);p.setStrokeCap(Paint.Cap.BUTT);p.setColor(WHITE);c.drawCircle(firstX,firstY,4,p);
        }else center(c,"Traçado em atualização",b.left+509,b.top+185,18,MUTED,false);
        nextSession(c,b,next);
    }
    private void nextSession(Canvas c,RectF b,RacingAgenda.Event next){
        fill(c,b.left+20,b.bottom-89,b.right-20,b.bottom-19,0xFF2C2619,9);
        text(c,"PRÓXIMA SESSÃO",b.left+35,b.bottom-65,15,AMBER,true);
        if(next!=null){String name=next.title.startsWith("Treino livre")?next.title:sessionName(next.title);
            text(c,name,b.left+35,b.bottom-35,25,WHITE,true);
            right(c,next.dateOnly?shortDate.format(new Date(next.at))+" · a confirmar":session.format(new Date(next.at)),b.right-33,b.bottom-35,21,WHITE,true);
        }else text(c,"Aguardando calendário confirmado",b.left+35,b.bottom-35,23,WHITE,true);
    }
    private void formulaStandings(Canvas c){
        RectF b=areas[3];panel(c,3);F1Rotation.Frame frame=f1Frame();boolean drivers=frame.phase==F1Rotation.DRIVERS;
        text(c,"FÓRMULA 1",b.left+23,b.top+34,21,ACCENT,true);
        text(c,drivers?"Campeonato de pilotos":"Campeonato de construtores",b.left+23,b.top+76,29,WHITE,true);
        SportsFeedParser.Table table=f1Table();
        if(table==null)text(c,"Consultando classificação…",b.left+23,b.top+135,21,MUTED,false);
        else{
            text(c,"Temporada "+season(table.season),b.left+23,b.top+102,17,MUTED,false);
            int size=drivers?F1Rotation.DRIVERS_PER_PAGE:12,page=drivers?frame.page:0;
            int from=page*size,to=Math.min(from+size,table.rows.size()),perColumn=(to-from+1)/2;
            for(int i=from;i<to;i++){
                SportsFeedParser.Row r=table.rows.get(i);int index=i-from,col=index/perColumn,row=index%perColumn;
                float x=b.left+24+col*326,y=b.top+132+row*27;
                if(row%2==0)fill(c,x-5,y-20,x+309,y+6,0xFF1E2732,4);
                right(c,String.valueOf(r.rank),x+23,y,17,MUTED,false);
                text(c,fit(r.name,21,220,true),x+37,y,21,WHITE,true);right(c,r.points,x+301,y,22,ACCENT,true);
            }
        }
        RacingAgenda.Event next=null;for(RacingAgenda.Event event:agenda.currentRaceSessions())if(event.at>System.currentTimeMillis()){next=event;break;}
        nextSession(c,b,next);
    }
    private static String sessionName(String name){
        switch(name){case "Treino livre 1":return "TL 1";case "Treino livre 2":return "TL 2";case "Treino livre 3":return "TL 3";
            case "Classificação":return "Qualy";case "Classificação Sprint":return "Qualy Sprint";case "Corrida":return "Race";default:return name;}
    }
    private String gameStatus(SportsFeedParser.Game g){
        if(g.complete)return "Encerrado"+(g.note.isEmpty()?"":" · "+g.note);
        if(g.status.contains("POSTPONED"))return "Adiado · horário sujeito a alteração";
        if(g.status.contains("CANCELED")||g.status.contains("CANCELLED"))return "Cancelado";
        if(g.status.contains("SUSPEND"))return "Partida suspensa";
        if(g.state.equals("in"))return "Em andamento · placar atualizado periodicamente";
        return g.at<System.currentTimeMillis()-5*60000L?"Horário previsto · situação a confirmar":"";
    }
    private void panel(Canvas c,int id){RectF b=areas[id];p.setStyle(Paint.Style.FILL);p.setColor(PANEL);c.drawRoundRect(b,17,17,p);
        p.setStyle(Paint.Style.STROKE);p.setColor(focus==id?AMBER:BORDER);p.setStrokeWidth(6);c.drawRoundRect(b,17,17,p);p.setStyle(Paint.Style.FILL);}
    private void logo(Canvas c,String url,String abbr,float x,float y,float size){
        int pixels=Math.max(8,Math.round(size*zoom));Bitmap bitmap=sports.logo(url,pixels);
        if(bitmap==null){fill(c,x,y,x+size,y+size,0xFF273442,5);
            text(c,abbr.isEmpty()?"•":abbr.substring(0,1),x+size*.22f,y+size*.77f,size*.67f,WHITE,true);return;}
        float leftPixel=Math.round((x+size/2)*zoom+ox-bitmap.getWidth()/2f),topPixel=Math.round((y+size/2)*zoom+oy-bitmap.getHeight()/2f);
        float left=(leftPixel-ox)/zoom,top=(topPixel-oy)/zoom;
        box.set(left,top,left+bitmap.getWidth()/zoom,top+bitmap.getHeight()/zoom);p.setColor(WHITE);
        p.setFilterBitmap(false);c.drawBitmap(bitmap,null,box,p);p.setFilterBitmap(true);
    }
    private void fill(Canvas c,float x,float y,float r,float b,int color,float radius){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(x,y,r,b,radius,radius,p);}
    private void line(Canvas c,float x,float y,float r,float b,int color,float width){p.setColor(color);p.setStrokeWidth(width);c.drawLine(x,y,r,b,p);}
    private void text(Canvas c,String value,float x,float y,float size,int color,boolean strong){text(c,value,x,y,size,color,strong,strong?bold:normal);}
    private void text(Canvas c,String value,float x,float y,float size,int color,boolean strong,Typeface font){p.setStyle(Paint.Style.FILL);p.setTypeface(font);p.setTextSize(size);p.setColor(color);p.setTextAlign(Paint.Align.LEFT);c.drawText(value,x,y,p);}
    /** Align visible glyphs to a shared vertical centre, including the raised degree symbol. */
    private void verticalText(Canvas c,String value,float x,float centerY,float size,int color,boolean strong){
        p.setTypeface(strong?bold:normal);p.setTextSize(size);Rect bounds=new Rect();p.getTextBounds(value,0,value.length(),bounds);
        text(c,value,x,centerY-(bounds.top+bounds.bottom)/2f,size,color,strong);
    }
    private void right(Canvas c,String value,float x,float y,float size,int color,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);text(c,value,x-p.measureText(value),y,size,color,strong);}
    private void center(Canvas c,String value,float x,float y,float size,int color,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);text(c,value,x-p.measureText(value)/2,y,size,color,strong);}
    private String fit(String value,float size,float width,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);if(p.measureText(value)<=width)return value;
        int n=p.breakText(value,true,Math.max(0,width-p.measureText("…")),null);return value.substring(0,n)+"…";}
    private static String season(String value){java.util.regex.Matcher m=java.util.regex.Pattern.compile("20[0-9]{2}(?:[-–][0-9]{2,4})?").matcher(value);return m.find()?m.group():value;}
    private void empty(Canvas c,RectF b,String value){text(c,value,b.left+22,b.top+190,21,MUTED,false);}
    private String condition(int code){if(code<0)return "Consultando clima";if(code==0)return "Céu limpo";if(code<=2)return "Parcialmente nublado";if(code==3)return "Nublado";if(code<51)return "Névoa";if(code<71)return "Chuva";if(code<86)return "Neve";return "Tempestade";}
    private void weatherIcon(Canvas c,float x,float y,float s,int code){p.setColor(code==0?AMBER:0xFFD7E1ED);if(code==0)c.drawCircle(x,y,s,p);
        else{x-=s*.2f;y+=s*.25f;c.drawCircle(x-s*.5f,y,s*.55f,p);c.drawCircle(x,y-s*.3f,s*.7f,p);c.drawCircle(x+s*.6f,y,s*.5f,p);c.drawRoundRect(x-s*.7f,y,x+s*.8f,y+s*.5f,6,6,p);}}
    @Override boolean onKey(KeyEvent e){
        if(f1Open){int action=f1Details.onKey(e);if(action>=0){if(action==1){f1Open=false;focus=3;}invalidate();return true;}}
        int key=e.getKeyCode();if(key==KeyEvent.KEYCODE_BACK){if(e.getAction()==KeyEvent.ACTION_DOWN)reset(false);return true;}
        if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER){if(e.getAction()==KeyEvent.ACTION_DOWN)activate();return true;}
        if(key<19||key>22)return false;if(e.getAction()!=KeyEvent.ACTION_DOWN)return true;
        RectF here=areas[focus];float best=Float.MAX_VALUE;int target=focus;
        for(int i=0;i<areas.length;i++)if(i!=focus){RectF next=areas[i];float dx=next.centerX()-here.centerX(),dy=next.centerY()-here.centerY();boolean horizontal=key==21||key==22;
            float primary=horizontal?dx:dy,perp=horizontal?dy:dx;if((key==19||key==21)?primary>=-1:primary<=1)continue;
            float score=Math.abs(primary)+Math.abs(perp)*2.4f;if(score<best){best=score;target=i;}}
        focus=target;invalidate();return true;
    }
    @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()!=MotionEvent.ACTION_UP)return true;float x=(e.getX()-ox)/zoom,y=(e.getY()-oy)/zoom;
        if(f1Open){if(f1Details.touch(x,y)){f1Open=false;focus=3;}invalidate();performClick();return true;}
        for(int i=0;i<areas.length;i++)if(areas[i].contains(x,y)){focus=i;activate();invalidate();performClick();break;}return true;}
    private void activate(){
        if(focus==0){List<SportsFeedParser.Table> ts=sports.tables(SportsFeedParser.TABLE_KEYS[(int)(elapsed()/FOOTBALL_INTERVAL)%SportsFeedParser.FOOTBALL_COUNT]);if(!ts.isEmpty())tableDetails(ts.get(0));}
        else if(focus==1||focus==2)gameDetails(focus==2);
        else if(focus==3){if(f1Details==null)f1Details=new F1DetailsScreen(getContext(),sports,agenda,normal,bold,()->invalidate());f1Details.reset();f1Open=true;invalidate();}
        else if(focus>=4&&focus<=6){SportsFeedParser.Table t=narrowTable(focus,(int)(elapsed()/AMERICAN_INTERVAL));if(t!=null)tableDetails(t);}
        else if(focus<10)openApp(focus-7);else if(focus==10)showAllApps();else if(focus==11)showInputs();else settings();
    }
    private void tableDetails(SportsFeedParser.Table t){StringBuilder message=new StringBuilder("Temporada "+t.season+"\nAtualizado: "+stamp.format(new Date(t.at))+"\n\n");
        for(SportsFeedParser.Row r:t.rows)message.append(r.rank).append(". ").append(r.name).append("   ").append(t.key.startsWith("f1-")?r.points+" pts"+(r.record.isEmpty()?"":" · "+r.record):t.key.contains(".")?r.points+" pts · "+r.played+" jogos":t.key.equals("nhl")?r.points+" pts · "+r.record:r.record).append("\n");
        new AlertDialog.Builder(activity).setTitle(t.title+" · "+t.group).setMessage(message.toString()).setPositiveButton("FECHAR",null).show();}
    private void gameDetails(boolean results){List<SportsFeedParser.Game> list=results?recent:today;StringBuilder message=new StringBuilder(sports.gamesStatus(results)+"\n\n");
        for(SportsFeedParser.Game g:list)message.append(g.league).append(" · ").append(session.format(new Date(g.at))).append("\n")
                .append(g.home).append(g.complete||g.state.equals("in")?" "+g.homeScore+"  –  "+g.awayScore+" ":" × ").append(g.away).append("\n")
                .append(gameStatus(g)).append("\n\n");
        new AlertDialog.Builder(activity).setTitle(results?"Resultados · últimas 72 horas":"Jogos de hoje · Brasília").setMessage(message.toString()).setPositiveButton("FECHAR",null).show();}
}
