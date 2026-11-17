package io.freemytv;

import android.graphics.*;
import android.content.Context;
import android.view.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.*;

/** Full-screen F1 sections using the sports home's existing data, with no extra activity or account access. */
final class F1DetailsScreen {
    private static final int RED=0xFFFF343A,TEXT=0xFFF4F4F6,MUTED=0xFFB5B5C0,PANEL=0xFF151519,BORDER=0xFF55555E,ROW=0xFF232329;
    private final SportsData sports;
    private final F1Logos logos;
    private final F1CircuitDiagram circuitDiagram;
    private final RacingAgenda agenda;
    private final Typeface normal,bold;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path trackPath=new Path();
    private final RectF[] nav=new RectF[5];
    private final String[] labels={"‹ Esportes Dash","Visão geral","Pilotos","Construtores","Calendário"};
    private final RectF trackCard=new RectF(48,190,730,690),programCard=new RectF(48,710,730,1008),driversCard=new RectF(752,190,1298,1008),constructorsCard=new RectF(1320,190,1872,696),calendarCard=new RectF(1320,716,1872,1008);
    private final RectF full=new RectF(48,190,1872,1008);
    private final SimpleDateFormat clock=fmt("HH:mm"),date=fmt("EEE, dd 'de' MMMM"),day=fmt("EEE dd/MM"),hour=fmt("HH:mm"),calendarDate=fmt("dd/MM"),stamp=fmt("dd/MM HH:mm");
    private int page,focus=1;
    F1DetailsScreen(Context context,SportsData sports,RacingAgenda agenda,Typeface normal,Typeface bold,Runnable changed){
        logos=new F1Logos(context,changed);circuitDiagram=new F1CircuitDiagram(context,bold,changed);
        this.sports=sports;this.agenda=agenda;this.normal=normal;this.bold=bold;
        for(int i=0;i<nav.length;i++)nav[i]=new RectF(48+i*368.8f,112,48+i*368.8f+348.8f,166);
        paint.setSubpixelText(true);paint.setDither(true);paint.setHinting(Paint.HINTING_ON);
    }
    private static SimpleDateFormat fmt(String pattern){SimpleDateFormat f=new SimpleDateFormat(pattern,new Locale("pt","BR"));f.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));return f;}
    void close(){logos.close();circuitDiagram.close();}
    void reset(){page=0;focus=1;}
    String description(){return "Fórmula 1. "+labels[page+1]+". Selecionado: "+labels[focus]+". Setas escolhem a seção, OK abre, Voltar retorna ao Esportes Dash.";}
    void draw(Canvas canvas){
        logo(canvas,"f1",new RectF(48,29,258,81),true);
        right(canvas,clock.format(new Date()),1872,62,46,TEXT,false);
        right(canvas,date.format(new Date()),1872,90,18,MUTED,false);
        for(int i=0;i<nav.length;i++){
            fill(canvas,nav[i],i==page+1?0xFF351B20:PANEL,11);
            border(canvas,nav[i],i==focus?RED:BORDER,i==focus?4:1.5f,11);
            center(canvas,labels[i],nav[i].centerX(),149,24,TEXT,true);
        }
        if(page==0)overview(canvas);else if(page==1)drivers(canvas);else if(page==2)constructors(canvas);else calendar(canvas);
        text(canvas,"Horários de Brasília · programação sujeita a alterações",48,1058,18,MUTED,false);
        right(canvas,"SETAS escolher seção · OK abrir · VOLTAR Esportes Dash",1872,1058,18,MUTED,false);
    }
    private SportsFeedParser.Table table(String key){List<SportsFeedParser.Table> all=sports.tables(key);return all.isEmpty()?null:all.get(0);}
    private RacingAgenda.Event next(){for(RacingAgenda.Event e:agenda.currentRaceSessions())if(e.at>System.currentTimeMillis())return e;return null;}
    private void overview(Canvas c){
        card(c,trackCard);text(c,"PRÓXIMO GP",72,228,20,RED,true);
        text(c,fit(agenda.raceName,39,630,true),72,279,39,TEXT,true);
        text(c,fit(agenda.circuitName,23,630,false),72,313,23,MUTED,false);
        F1CircuitDiagram.Diagram diagram=circuitDiagram.get(agenda.circuitId);
        if(diagram!=null)circuitDiagram.draw(c,diagram,new RectF(80,352,704,587));
        else drawTrack(c,new RectF(96,352,682,587));
        String length=diagram!=null?diagram.length:agenda.circuitId.equals(sports.trackId)?sports.trackLength:"A confirmar";
        circuitStat(c,"EXTENSÃO",length,72,630,199);
        if(diagram!=null){circuitStat(c,"CORRIDA",diagram.laps.isEmpty()?"A confirmar":diagram.laps+" voltas",282,630,199);circuitStat(c,"CURVAS",String.valueOf(diagram.corners),492,630,212);}
        program(c);
        SportsFeedParser.Table drivers=table("f1-drivers");card(c,driversCard);text(c,"PILOTOS",776,232,27,RED,true);
        text(c,drivers==null?"Consultando classificação":"Mundial "+drivers.season,776,266,21,MUTED,false);
        right(c,"PTS",1273,266,18,MUTED,true);
        if(drivers!=null){
            for(int i=0;i<Math.min(12,drivers.rows.size());i++){
                SportsFeedParser.Row r=drivers.rows.get(i);float y=driversCard.top+126+i*53;
                if(i%2==0)fill(c,new RectF(766,y-26,1284,y+23),ROW,5);
                right(c,String.valueOf(r.rank),800,y,20,MUTED,false);
                logo(c,r.record,new RectF(812,y-21,866,y+17),false);
                text(c,fit(r.name,25,294,true),880,y+4,25,TEXT,true);
                right(c,r.points,1270,y+4,28,RED,true);
            }
            text(c,"Classificação completa na aba Pilotos",776,976,18,MUTED,false);
        }else text(c,"Fonte ainda não disponível",776,335,23,MUTED,false);
        SportsFeedParser.Table teams=table("f1-constructors");card(c,constructorsCard);text(c,"CONSTRUTORES",1344,232,27,RED,true);
        right(c,"PTS",1846,268,18,MUTED,true);
        text(c,teams==null?"Consultando classificação":"Mundial "+teams.season,1344,268,21,MUTED,false);
        if(teams!=null)for(int i=0;i<Math.min(12,teams.rows.size());i++){
            SportsFeedParser.Row r=teams.rows.get(i);float y=constructorsCard.top+126+i*32;
            if(i%2==0)fill(c,new RectF(1334,y-25,1858,y+6),ROW,4);
            right(c,String.valueOf(r.rank),1366,y,20,MUTED,false);logo(c,r.name,new RectF(1382,y-23,1434,y+4),false);text(c,fit(teamName(r.name),27,300,true),1450,y,27,TEXT,true);right(c,r.points,1846,y,28,RED,true);
        }
        card(c,calendarCard);text(c,"PRÓXIMAS CORRIDAS",1344,756,26,RED,true);
        List<RacingAgenda.Event> upcoming=agenda.seasonRaces();int shown=0;
        for(RacingAgenda.Event e:upcoming){if(e.at<System.currentTimeMillis())continue;if(shown>=3)break;float y=calendarCard.top+82+shown*64;
            text(c,calendarDate.format(new Date(e.at)),1346,y,28,RED,true);
            text(c,fit(e.detail,25,404,true),1434,y,25,TEXT,true);
            text(c,e.dateOnly?"Horário a confirmar":day.format(new Date(e.at))+" · "+hour.format(new Date(e.at)),1434,y+28,20,MUTED,false);shown++;
        }
        if(shown==0)text(c,"Aguardando calendário confirmado",1344,810,22,MUTED,false);
        text(c,"Temporada completa na aba Calendário",1344,987,16,MUTED,false);
    }
    private void circuitStat(Canvas c,String label,String value,float x,float y,float width){
        fill(c,new RectF(x-4,y,x+width,y+47),ROW,6);
        text(c,label,x+9,y+16,14,MUTED,true);text(c,value,x+9,y+40,25,TEXT,true);
    }
    private void program(Canvas c){
        card(c,programCard);text(c,"PROGRAMAÇÃO DO GP",72,programCard.top+36,25,RED,true);right(c,"BRASÍLIA",704,programCard.top+35,16,MUTED,true);
        List<RacingAgenda.Event> sessions=agenda.currentRaceSessions();RacingAgenda.Event next=next();
        float gap=Math.min(28,132f/Math.max(1,sessions.size()-1));
        for(int i=0;i<Math.min(7,sessions.size());i++){
            RacingAgenda.Event e=sessions.get(i);float y=programCard.top+72+i*gap;int color=TEXT;
            text(c,fit(sessionName(e.title),22,244,true),72,y,22,color,true);
            text(c,day.format(new Date(e.at)),336,y,21,MUTED,false);right(c,e.dateOnly?"A definir":hour.format(new Date(e.at)),704,y,23,color,true);
        }
        RectF nextBox=new RectF(68,922,710,989);fill(c,nextBox,ROW,10);text(c,"PRÓXIMA SESSÃO",84,943,14,RED,true);
        text(c,next==null?"Aguardando calendário":sessionName(next.title),84,975,26,TEXT,true);
        if(next!=null)right(c,next.dateOnly?"Horário a confirmar":day.format(new Date(next.at))+" · "+hour.format(new Date(next.at)),694,975,21,TEXT,true);
    }
    private void drivers(Canvas c){
        card(c,full);SportsFeedParser.Table table=table("f1-drivers");text(c,"CAMPEONATO DE PILOTOS",78,246,35,RED,true);
        if(table==null){text(c,"Classificação ainda não disponível",78,320,25,MUTED,false);return;}
        text(c,"Temporada "+table.season+" · "+table.rows.size()+" pilotos · atualizado "+stamp.format(new Date(table.at)),78,280,20,MUTED,false);
        int perColumn=(table.rows.size()+1)/2;float gap=Math.min(50,650f/Math.max(1,perColumn));
        for(int col=0;col<2;col++){
            float x=80+col*900;
            text(c,"PILOTO",x+112,316,17,MUTED,true);
            center(c,"VIT",x+628,316,17,MUTED,true);center(c,"PÓD",x+710,316,17,MUTED,true);right(c,"PTS",x+836,316,17,MUTED,true);
        }
        for(int i=0;i<table.rows.size();i++){
            SportsFeedParser.Row r=table.rows.get(i);int col=i/perColumn,row=i%perColumn;float x=80+col*900,y=359+row*gap;
            if(row%2==0)fill(c,new RectF(x-8,y-29,x+852,y+20),ROW,5);
            right(c,String.valueOf(r.rank),x+30,y,22,MUTED,false);
            logo(c,r.record,new RectF(x+46,y-24,x+98,y+14),false);
            text(c,fit(r.name,30,458,true),x+112,y+4,30,TEXT,true);
            center(c,r.played,x+628,y+4,27,TEXT,true);
            center(c,sports.f1Podiums(table.season,r.id,true),x+710,y+4,27,TEXT,true);
            right(c,r.points,x+836,y+4,30,RED,true);
        }
    }

    private void constructors(Canvas c){
        card(c,full);SportsFeedParser.Table table=table("f1-constructors");text(c,"CAMPEONATO DE CONSTRUTORES",78,246,35,RED,true);
        if(table==null){text(c,"Classificação ainda não disponível",78,320,25,MUTED,false);return;}
        text(c,"Temporada "+table.season+" · atualizado "+stamp.format(new Date(table.at)),78,280,20,MUTED,false);
        text(c,"CONSTRUTOR",244,326,19,MUTED,true);right(c,"PONTOS",1050,326,19,MUTED,true);right(c,"VITÓRIAS",1210,326,19,MUTED,true);right(c,"PÓDIOS",1370,326,19,MUTED,true);
        text(c,"PONTOS EM RELAÇÃO AO LÍDER",1450,326,17,MUTED,false);
        float gap=Math.min(54,620f/Math.max(1,table.rows.size()));double leader=table.rows.isEmpty()?1:Math.max(1,number(table.rows.get(0).points));
        for(int i=0;i<table.rows.size();i++){
            SportsFeedParser.Row r=table.rows.get(i);float y=375+i*gap;
            if(i%2==0)fill(c,new RectF(70,y-31,1850,y+12),ROW,6);
            right(c,String.valueOf(r.rank),124,y,24,MUTED,false);logo(c,r.name,new RectF(156,y-30,218,y+10),false);text(c,fit(teamName(r.name),33,690,true),244,y,33,TEXT,true);
            right(c,r.points,1050,y,33,RED,true);right(c,r.played,1210,y,29,TEXT,true);right(c,sports.f1Podiums(table.season,r.id,false),1370,y,29,TEXT,true);
            fill(c,new RectF(1450,y-20,1810,y-3),0xFF393238,7);
            float width=(float)Math.max(0,Math.min(1,number(r.points)/leader))*360;if(width>0)fill(c,new RectF(1450,y-20,1450+width,y-3),RED,7);
        }
    }
    private void calendar(Canvas c){
        card(c,full);text(c,"CALENDÁRIO DA TEMPORADA",78,246,35,RED,true);text(c,"Horários de Brasília · datas e sessões sujeitas a alterações",78,280,20,MUTED,false);
        List<RacingAgenda.Event> races=agenda.seasonRaces();if(races.isEmpty()){text(c,"Calendário ainda não disponível",78,340,26,MUTED,false);return;}
        int perColumn=(races.size()+1)/2;float gap=Math.min(51,650f/Math.max(1,perColumn));
        for(int i=0;i<races.size();i++){
            RacingAgenda.Event race=races.get(i);int col=i/perColumn,row=i%perColumn;float x=80+col*900,y=350+row*gap;
            boolean upcoming=race.detail.equals(agenda.raceName);if(row%2==0)fill(c,new RectF(x-8,y-31,x+852,y+12),ROW,5);
            text(c,calendarDate.format(new Date(race.at)),x,y,29,RED,true);
            text(c,fit(race.detail,27,582,true),x+108,y,27,TEXT,true);right(c,race.dateOnly?"A definir":hour.format(new Date(race.at)),x+835,y,24,TEXT,true);
        }
    }
    private void drawTrack(Canvas c,RectF bounds){
        sports.ensureTrack(agenda.circuitId);float[] points=sports.track;if(points==null||!agenda.circuitId.equals(sports.trackId)){center(c,"Traçado em atualização",bounds.centerX(),bounds.centerY(),25,MUTED,false);return;}
        float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for(int i=0;i<points.length;i+=2){float x=-points[i+1],y=points[i];minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}
        float scale=Math.min(bounds.width()/Math.max(.01f,maxX-minX),bounds.height()/Math.max(.01f,maxY-minY));trackPath.reset();float sx=0,sy=0;
        for(int i=0;i<points.length;i+=2){float x=bounds.centerX()+(-points[i+1]-(minX+maxX)/2)*scale,y=bounds.centerY()+(points[i]-(minY+maxY)/2)*scale;
            if(i==0){trackPath.moveTo(x,y);sx=x;sy=y;}else trackPath.lineTo(x,y);}
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(0xFF57252B);paint.setStrokeWidth(17);c.drawPath(trackPath,paint);
        paint.setColor(RED);paint.setStrokeWidth(5);c.drawPath(trackPath,paint);paint.setStyle(Paint.Style.FILL);paint.setStrokeCap(Paint.Cap.BUTT);paint.setColor(RED);c.drawCircle(sx,sy,6,paint);
    }
    /** -1 = unhandled, 0 = handled, 1 = return to sports dashboard. */
    int onKey(KeyEvent event){
        int key=event.getKeyCode();boolean ours=key==KeyEvent.KEYCODE_BACK||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER;
        if(!ours)return -1;if(event.getAction()!=KeyEvent.ACTION_DOWN)return 0;if(key==KeyEvent.KEYCODE_BACK)return 1;
        if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_UP)focus=Math.max(0,focus-1);
        else if(key==KeyEvent.KEYCODE_DPAD_RIGHT||key==KeyEvent.KEYCODE_DPAD_DOWN)focus=Math.min(nav.length-1,focus+1);
        else if(focus==0)return 1;else page=focus-1;return 0;
    }
    boolean touch(float x,float y){
        for(int i=0;i<nav.length;i++)if(nav[i].contains(x,y)){focus=i;if(i==0)return true;page=i-1;return false;}
        if(page==0){if(driversCard.contains(x,y)){page=1;focus=2;}else if(constructorsCard.contains(x,y)){page=2;focus=3;}else if(calendarCard.contains(x,y)){page=3;focus=4;}}
        return false;
    }
    private void logo(Canvas c,String team,RectF bounds,boolean red){
        float[] transform=new float[9];c.getMatrix().getValues(transform);
        float scale=Math.max(.01f,(float)Math.hypot(transform[Matrix.MSCALE_X],transform[Matrix.MSKEW_Y]));
        Bitmap image=logos.get(team,Math.max(1,Math.round(bounds.width()*scale)),Math.max(1,Math.round(bounds.height()*scale)));
        if(image==null)return;
        float w=image.getWidth()/scale,h=image.getHeight()/scale;
        float left=(Math.round(bounds.centerX()*scale-image.getWidth()/2f))/scale;
        float top=(Math.round(bounds.centerY()*scale-image.getHeight()/2f))/scale;
        paint.setColorFilter(red?new PorterDuffColorFilter(RED,PorterDuff.Mode.SRC_IN):null);
        paint.setFilterBitmap(true);paint.setAlpha(255);
        c.drawBitmap(image,null,new RectF(left,top,left+w,top+h),paint);
        paint.setColorFilter(null);

    }
    private static String teamName(String value){if(value.equals("RB F1 Team"))return "Racing Bulls";if(value.equals("Haas F1 Team"))return "Haas";if(value.equals("Cadillac F1 Team"))return "Cadillac";return value;}
    private static String sessionName(String value){if(value.equals("Classificação"))return "Qualy";if(value.equals("Classificação Sprint"))return "Qualy Sprint";if(value.equals("Corrida"))return "Race";return value;}

    private static double number(String value){try{return Double.parseDouble(value);}catch(Exception e){return 0;}}
    private void card(Canvas c,RectF b){fill(c,b,PANEL,17);border(c,b,BORDER,2,17);}
    private void fill(Canvas c,RectF b,int color,float radius){paint.setStyle(Paint.Style.FILL);paint.setColor(color);c.drawRoundRect(b,radius,radius,paint);}
    private void border(Canvas c,RectF b,int color,float width,float radius){paint.setStyle(Paint.Style.STROKE);paint.setColor(color);paint.setStrokeWidth(width);c.drawRoundRect(b,radius,radius,paint);paint.setStyle(Paint.Style.FILL);}
    private void text(Canvas c,String value,float x,float y,float size,int color,boolean strong){paint.setStyle(Paint.Style.FILL);paint.setTypeface(strong?bold:normal);paint.setTextSize(size);paint.setColor(color);c.drawText(value,x,y,paint);}
    private void right(Canvas c,String value,float x,float y,float size,int color,boolean strong){paint.setTypeface(strong?bold:normal);paint.setTextSize(size);text(c,value,x-paint.measureText(value),y,size,color,strong);}
    private void center(Canvas c,String value,float x,float y,float size,int color,boolean strong){paint.setTypeface(strong?bold:normal);paint.setTextSize(size);text(c,value,x-paint.measureText(value)/2,y,size,color,strong);}
    private String fit(String value,float size,float width,boolean strong){paint.setTypeface(strong?bold:normal);paint.setTextSize(size);if(paint.measureText(value)<=width)return value;int n=paint.breakText(value,true,Math.max(0,width-paint.measureText("…")),null);return value.substring(0,n)+"…";}
}
