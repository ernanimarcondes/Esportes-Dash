package io.freemytv;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.*;

/** Dedicated NBA/NFL/NHL/MLB panels; all data and team shields share the sports home's cache. */
final class LeagueDetailsScreen {
    private static final int TEXT=0xFFF1F5FA,MUTED=0xFFAFBBC9,PANEL=0xFF131922,BORDER=0xFF3B4A5E,ROW=0xFF1C2735;
    private final SportsData sports;
    private final Typeface normal,bold;
    private final String league;
    private final int accent,secondary,panelColor,borderColor,rowColor,selectedColor;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF[] nav=new RectF[6];
    private final DayGamesRotation rotation=new DayGamesRotation();
    private final DayGamesRotation upcomingSummary=new DayGamesRotation(15000L),recentSummary=new DayGamesRotation(15000L);
    private final SimpleDateFormat clock=fmt("HH:mm"),date=fmt("EEE, dd 'de' MMMM"),day=fmt("EEE dd/MM"),hour=fmt("HH:mm"),dayWithYear=fmt("EEE dd/MM/yy"),year=fmt("yyyy");
    private Bitmap leagueOriginal,leagueRaster;
    private int rasterSize,page,focus=1;
    private long modelVersion=-1,modelMinute=-1;
    private String currentYear="";
    private List<SportsFeedParser.Game> upcoming=Collections.emptyList(),recent=Collections.emptyList();
    LeagueDetailsScreen(Context context,SportsData sports,String league,Typeface normal,Typeface bold){
        this.sports=sports;this.league=league;this.normal=normal;this.bold=bold;
        switch(league){
            case "nhl":accent=0xFFE2E8EE;secondary=0xFFB0C8DA;panelColor=0xFF15181C;borderColor=0xFF616A75;rowColor=0xFF252B32;selectedColor=0xFF303941;break;
            case "mlb":accent=0xFF71A8FF;secondary=0xFFFF5667;panelColor=0xFF101827;borderColor=0xFF3E5477;rowColor=0xFF1B2941;selectedColor=0xFF21385D;break;
            default:accent=league.equals("nba")?0xFF69AEFF:0xFFFF5361;secondary=league.equals("nba")?0xFFEF4168:0xFF69A5ED;
                panelColor=PANEL;borderColor=BORDER;rowColor=ROW;selectedColor=0xFF202C3C;
        }
        try{Bitmap source=BitmapFactory.decodeStream(context.getAssets().open("sports/leagues/"+league+".png"));leagueOriginal=crop(source);}catch(Exception ignored){}
        float width=(1824-5*18)/6;for(int i=0;i<nav.length;i++)nav[i]=new RectF(48+i*(width+18),128,48+i*(width+18)+width,184);
        p.setSubpixelText(true);p.setDither(true);p.setHinting(Paint.HINTING_ON);
    }
    String league(){return league;}
    void reset(){page=0;focus=1;resetRotations();}
    private void resetRotations(){long now=SystemClock.elapsedRealtime();rotation.enter(now);upcomingSummary.enter(now);recentSummary.enter(now);}
    void close(){if(leagueRaster!=null&&leagueRaster!=leagueOriginal)leagueRaster.recycle();if(leagueOriginal!=null)leagueOriginal.recycle();}
    private static SimpleDateFormat fmt(String pattern){SimpleDateFormat f=new SimpleDateFormat(pattern,new Locale("pt","BR"));f.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));return f;}
    private static Bitmap crop(Bitmap source){
        if(source==null)return null;int w=source.getWidth(),h=source.getHeight(),l=w,t=h,r=-1,b=-1;int[] data=new int[w*h];source.getPixels(data,0,w,0,0,w,h);
        for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((data[y*w+x]>>>24)>8){l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);}
        return r>=l&&b>=t?Bitmap.createBitmap(source,l,t,r-l+1,b-t+1):source;
    }
    private void update(){
        long now=System.currentTimeMillis(),minute=now/60000,version=sports.modelVersion();
        if(version==modelVersion&&minute==modelMinute)return;
        currentYear=year.format(new Date(now));
        upcoming=LeaguePanelModel.games(league,sports.leagueGames(league,false),false,now);recent=LeaguePanelModel.games(league,sports.leagueGames(league,true),true,now);modelVersion=version;modelMinute=minute;
    }
    void draw(Canvas c){
        update();header(c);
        String[] labels={"‹ Esportes Dash","Visão geral",LeaguePanelModel.conference(league,0),LeaguePanelModel.conference(league,1),"Jogos","Resultados"};
        for(int i=0;i<nav.length;i++){
            fill(c,nav[i],i==page+1?selectedColor:panelColor,10);border(c,nav[i],i==focus?accent:borderColor,i==focus?4:1.5f,10);
            center(c,labels[i],nav[i].centerX(),163,24,TEXT,true);
        }
        if(page==0)overview(c);else if(page==1||page==2)standings(c,page-1);else matches(c,page==4);
        String note=league.equals("nhl")?"PTS: pontos · V–D–OT: vitórias, derrotas e derrotas na prorrogação/shootout":league.equals("nfl")?"V–D–E: vitórias, derrotas e empates":"V–D: vitórias e derrotas · horários de Brasília";
        text(c,note,48,1060,17,MUTED,false);
        right(c,"SETAS escolher seção · OK abrir · VOLTAR Esportes Dash",1872,1060,18,MUTED,false);
    }
    private void header(Canvas c){
        if(leagueOriginal!=null){
            int target=pixels(c,league.equals("mlb")?145:87);if(leagueRaster==null||target!=rasterSize){if(leagueRaster!=null&&leagueRaster!=leagueOriginal)leagueRaster.recycle();leagueRaster=ShieldRasterizer.reduce(leagueOriginal,target);rasterSize=target;}
            image(c,leagueRaster,new RectF(48,20,league.equals("mlb")?210:150,107));
        }else text(c,league.toUpperCase(Locale.ROOT),48,78,56,accent,true);
        float headerX=league.equals("mlb")?234:174;
        String sport=league.equals("nba")?"BASQUETE":league.equals("nfl")?"FUTEBOL AMERICANO":league.equals("nhl")?"HÓQUEI NO GELO":"BEISEBOL";
        text(c,sport,headerX,62,34,accent,true);
        List<SportsFeedParser.Table> tables=sports.tables(league);String season=tables.isEmpty()?"Consultando temporada":"Temporada "+tables.get(0).season;
        if(LeaguePanelModel.perTeam(league)){String status=sports.leagueCalendarStatus(league);if(!status.isEmpty())season+=" · "+status;}
        text(c,season,headerX,94,21,MUTED,false);
        right(c,clock.format(new Date()),1872,61,46,TEXT,false);right(c,date.format(new Date()),1872,94,19,MUTED,false);
    }
    private SportsFeedParser.Table table(int group){return LeaguePanelModel.table(sports.tables(league),league,group);}
    private void overview(Canvas c){
        for(int group=0;group<2;group++){
            float x=48+group*924;RectF b=new RectF(x,210,x+900,676);card(c,b);
            text(c,(!league.equals("nfl")&&!league.equals("mlb")?"CONFERÊNCIA ":league.equals("mlb")?"LIGA ":"")+LeaguePanelModel.conference(league,group).toUpperCase(new Locale("pt","BR")),x+26,252,29,accent,true);
            text(c,"TIME",x+168,288,17,MUTED,true);right(c,recordHeading(),x+760,288,17,MUTED,true);right(c,extraHeading(),x+872,288,17,MUTED,true);
            SportsFeedParser.Table table=table(group);
            if(table==null){text(c,"Classificação indisponível no momento",x+26,358,26,MUTED,false);continue;}
            for(int i=0;i<Math.min(8,table.rows.size());i++)tableRow(c,table.rows.get(i),x,326+i*40,40,27);
            text(c,"Classificação completa na aba "+LeaguePanelModel.conference(league,group),x+26,656,17,MUTED,false);
        }
        summary(c,new RectF(48,696,948,1018),false);summary(c,new RectF(972,696,1872,1018),true);
    }
    /** Every entry uses one centered row, a fixed logo column and consistent text columns. */
    private void tableRow(Canvas c,SportsFeedParser.Row row,float x,float centerY,float rowHeight,float font){
        if(row.rank%2==1)fill(c,new RectF(x+14,centerY-rowHeight/2+3,x+886,centerY+rowHeight/2-3),rowColor,5);
        right(c,String.valueOf(row.rank),x+65,rowBaseline(centerY,23,false),23,MUTED,false);
        float badgeHeight=Math.min(54,rowHeight-12);
        standingsLogo(c,row,new RectF(x+76,centerY-badgeHeight/2,x+146,centerY+badgeHeight/2));
        text(c,fit(row.name,font,432,true),x+168,rowBaseline(centerY,font,true),font,TEXT,true);
        right(c,league.equals("nhl")?row.points:row.record,x+760,rowBaseline(centerY,font,true),font,TEXT,true);
        right(c,league.equals("nhl")?row.record:LeaguePanelModel.percentage(row),x+872,rowBaseline(centerY,22,false),22,MUTED,false);
    }
    private float rowBaseline(float centerY,float size,boolean strong){
        p.setTypeface(strong?bold:normal);p.setTextSize(size);Paint.FontMetrics metrics=p.getFontMetrics();
        return centerY-(metrics.ascent+metrics.descent)/2;
    }
    /** Balance wide and tall marks by their visible bounding area, without stretching their identity. */
    private void standingsLogo(Canvas c,SportsFeedParser.Row row,RectF cell){
        Bitmap source=sports.logo(row.logo,pixels(c,Math.max(cell.width(),cell.height())));
        if(source==null){center(c,fit(row.abbr,18,cell.width(),true),cell.centerX(),rowBaseline(cell.centerY(),18,true),18,MUTED,true);return;}
        float ratio=source.getWidth()/(float)source.getHeight(),edge=cell.height()*.82f;
        float w=edge*(float)Math.sqrt(ratio),h=edge/(float)Math.sqrt(ratio);
        float scale=Math.min(1,Math.min(cell.width()/w,cell.height()/h));w*=scale;h*=scale;
        Bitmap ready=sports.logo(row.logo,pixels(c,Math.max(w,h)));
        if(ready!=null){image(c,ready,cell);return;}
        p.setColor(Color.WHITE);p.setFilterBitmap(true);
        c.drawBitmap(source,null,new RectF(cell.centerX()-w/2,cell.centerY()-h/2,cell.centerX()+w/2,cell.centerY()+h/2),p);
    }
    private String recordHeading(){return league.equals("nhl")?"PTS":league.equals("nfl")?"V–D–E":"V–D";}
    private String extraHeading(){return league.equals("nhl")?"V–D–OT":"APROV.";}
    private void summary(Canvas c,RectF b,boolean results){
        card(c,b);List<SportsFeedParser.Game> games=results?recent:upcoming;
        String heading=LeaguePanelModel.perTeam(league)?(results?"ÚLTIMO CONFRONTO POR TIME":"CONFRONTOS ATUAIS"):(results?"ÚLTIMA RODADA · ":"RODADA ATUAL · ")+sports.leagueRoundLabel(league,results);
        text(c,heading,b.left+26,b.top+44,23,results?secondary:accent,true);
        if(games.isEmpty()){text(c,"Nenhuma partida disponível nesta seção",b.left+26,b.top+132,24,MUTED,false);return;}
        DayGamesRotation clock=results?recentSummary:upcomingSummary;
        clock.tick(games.size(),SystemClock.elapsedRealtime(),2);
        List<SportsFeedParser.Game> visible=LeaguePanelModel.summaryGames(games,clock.page,results);
        for(int i=0;i<visible.size();i++){
            SportsFeedParser.Game g=visible.get(i);float y=b.top+102+i*110;
            text(c,gameDate(g.at)+(g.timeValid?" · "+hour.format(new Date(g.at)):""),b.left+26,y-13,18,MUTED,false);
            logo(c,g.homeLogo,g.homeAbbr,new RectF(b.left+26,y,b.left+76,y+50));
            logo(c,g.awayLogo,g.awayAbbr,new RectF(b.right-76,y,b.right-26,y+50));
            text(c,fit(g.home,27,260,true),b.left+92,y+33,27,TEXT,true);
            right(c,fit(g.away,27,260,true),b.right-92,y+33,27,TEXT,true);
            String value=g.complete||g.state.equals("in")?g.homeScore+" – "+g.awayScore:g.timeValid?hour.format(new Date(g.at)):"A definir";
            center(c,value,b.centerX(),y+32,29,g.state.equals("in")&&!g.complete?accent:TEXT,true);
            if(i==0)line(c,b.left+26,y+67,b.right-26,y+67,borderColor);
        }
    }
    private void standings(Canvas c,int group){
        SportsFeedParser.Table table=table(group);String title=(!league.equals("nfl")&&!league.equals("mlb")?"CONFERÊNCIA ":league.equals("mlb")?"LIGA ":"")+LeaguePanelModel.conference(league,group);
        text(c,title.toUpperCase(new Locale("pt","BR")),48,242,31,accent,true);
        if(table==null){text(c,"Aguardando a classificação da fonte",48,325,29,MUTED,false);return;}
        rotation.tick(table.rows.size(),SystemClock.elapsedRealtime(),LeaguePanelModel.TABLE_PAGE);
        int from=rotation.page*LeaguePanelModel.TABLE_PAGE,to=Math.min(from+LeaguePanelModel.TABLE_PAGE,table.rows.size()),perColumn=(to-from+1)/2;
        for(int col=0;col<2;col++){
            float x=48+col*924;card(c,new RectF(x,264,x+900,1018));
            text(c,"#",x+40,309,19,MUTED,true);text(c,"TIME",x+168,309,19,MUTED,true);
            right(c,recordHeading(),x+760,309,19,MUTED,true);right(c,extraHeading(),x+872,309,19,MUTED,true);
        }
        float step=Math.min(82,654f/Math.max(1,perColumn));
        for(int i=from;i<to;i++){
            int offset=i-from,col=offset/perColumn,row=offset%perColumn;SportsFeedParser.Row r=table.rows.get(i);float x=48+col*924,y=338+(row+.5f)*step;
            tableRow(c,r,x,y,step,32);
        }
    }
    private void matches(Canvas c,boolean results){
        List<SportsFeedParser.Game> games=results?recent:upcoming;
        String title=results?"RESULTADOS":"JOGOS";
        if(league.equals("nfl"))title+=" · "+sports.leagueRoundLabel(league,results);
        text(c,title,48,242,30,accent,true);
        if(games.isEmpty()){card(c,new RectF(48,264,1872,1018));text(c,"Nenhuma partida disponível nesta seção",80,352,29,MUTED,false);return;}
        rotation.tick(games.size(),SystemClock.elapsedRealtime(),LeaguePanelModel.GAMES_PAGE);
        int from=rotation.page*LeaguePanelModel.GAMES_PAGE,to=Math.min(from+LeaguePanelModel.GAMES_PAGE,games.size()),perColumn=(to-from+1)/2;
        for(int col=0;col<2;col++){
            float x=48+col*924;card(c,new RectF(x,264,x+900,1018));
            text(c,"DATA / HORA",x+20,304,17,MUTED,true);text(c,"MANDANTE",x+202,304,17,MUTED,true);
            center(c,"PLACAR / HORA",x+472,304,17,MUTED,true);right(c,"VISITANTE",x+820,304,17,MUTED,true);
        }
        for(int i=from;i<to;i++){
            int offset=i-from,col=offset/perColumn,row=offset%perColumn;SportsFeedParser.Game g=games.get(i);float x=48+col*924,y=352+row*55;
            if(row%2==0)fill(c,new RectF(x+12,y-27,x+888,y+24),rowColor,4);
            text(c,gameDate(g.at),x+20,y-3,18,MUTED,false);text(c,g.timeValid?hour.format(new Date(g.at)):"A confirmar",x+20,y+19,18,TEXT,false);
            logo(c,g.homeLogo,g.homeAbbr,new RectF(x+144,y-24,x+188,y+20));logo(c,g.awayLogo,g.awayAbbr,new RectF(x+836,y-24,x+880,y+20));
            text(c,fit(g.home,26,202,true),x+202,y+6,26,TEXT,true);right(c,fit(g.away,26,250,true),x+820,y+6,26,TEXT,true);
            String value=g.complete||g.state.equals("in")?g.homeScore+" – "+g.awayScore:g.timeValid?hour.format(new Date(g.at)):"A definir";
            center(c,value,x+472,y+1,26,TEXT,true);
            center(c,DayGames.status(g,System.currentTimeMillis()),x+472,y+22,15,g.state.equals("in")&&!g.complete?accent:MUTED,false);
        }
    }
    private String gameDate(long at){Date value=new Date(at);return year.format(value).equals(currentYear)?day.format(value):dayWithYear.format(value);}
    private void logo(Canvas c,String url,String abbr,RectF b){Bitmap image=sports.logo(url,pixels(c,b.width()));if(image!=null)image(c,image,b);else center(c,fit(abbr,18,b.width(),true),b.centerX(),b.centerY()+6,18,MUTED,true);}
    private int pixels(Canvas c,float value){float[] m=new float[9];c.getMatrix().getValues(m);return Math.max(1,Math.round(value*(float)Math.hypot(m[Matrix.MSCALE_X],m[Matrix.MSKEW_Y])));}
    private void image(Canvas c,Bitmap image,RectF b){
        float[] m=new float[9];c.getMatrix().getValues(m);float scale=Math.max(.01f,(float)Math.hypot(m[Matrix.MSCALE_X],m[Matrix.MSKEW_Y]));
        float left=(Math.round(b.centerX()*scale+m[Matrix.MTRANS_X]-image.getWidth()/2f)-m[Matrix.MTRANS_X])/scale;
        float top=(Math.round(b.centerY()*scale+m[Matrix.MTRANS_Y]-image.getHeight()/2f)-m[Matrix.MTRANS_Y])/scale;
        p.setColor(Color.WHITE);p.setFilterBitmap(true);c.drawBitmap(image,null,new RectF(left,top,left+image.getWidth()/scale,top+image.getHeight()/scale),p);
    }
    private void card(Canvas c,RectF b){fill(c,b,panelColor,15);border(c,b,borderColor,1.5f,15);}
    private void fill(Canvas c,RectF b,int color,float radius){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(b,radius,radius,p);}
    private void border(Canvas c,RectF b,int color,float width,float radius){p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(width);c.drawRoundRect(b,radius,radius,p);p.setStyle(Paint.Style.FILL);}
    private void line(Canvas c,float x,float y,float right,float bottom,int color){p.setColor(color);p.setStrokeWidth(1);c.drawLine(x,y,right,bottom,p);}
    private void text(Canvas c,String value,float x,float y,float size,int color,boolean strong){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(strong?bold:normal);p.setTextSize(size);c.drawText(value,x,y,p);}
    private void right(Canvas c,String value,float x,float y,float size,int color,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);text(c,value,x-p.measureText(value),y,size,color,strong);}
    private void center(Canvas c,String value,float x,float y,float size,int color,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);text(c,value,x-p.measureText(value)/2,y,size,color,strong);}
    private String fit(String value,float size,float width,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);if(p.measureText(value)<=width)return value;int n=p.breakText(value,true,Math.max(0,width-p.measureText("…")),null);return value.substring(0,n)+"…";}
    private boolean activate(){if(focus==0)return true;page=focus-1;resetRotations();return false;}
    int onKey(KeyEvent e){
        int key=e.getKeyCode();if(key!=4&&(key<19||key>23)&&key!=66)return -1;if(e.getAction()!=KeyEvent.ACTION_DOWN)return 0;
        if(key==4)return 1;if(key==21||key==19)focus=Math.max(0,focus-1);else if(key==22||key==20)focus=Math.min(nav.length-1,focus+1);else if(activate())return 1;return 0;
    }
    boolean touch(float x,float y){for(int i=0;i<nav.length;i++)if(nav[i].contains(x,y)){focus=i;return activate();}return false;}
}
