package io.freemytv;

import android.graphics.*;
import android.os.SystemClock;
import android.view.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.*;

/** Shared match panel: chronological All lists and six-card filtered tabs, with independent themes. */
final class DayGamesScreen {
    private static final int FOCUS=0xFFF4F7F5,AMBER=0xFFFFCA5C,TEXT=0xFFF4F7F5,GREEN=0xFF7DF0B7;
    private final int ACCENT,MUTED,PANEL,BORDER,ACTIVE,DIVIDER;
    private final boolean results;
    private final SportsData sports;
    private final Typeface normal,bold;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF[] nav=new RectF[5];
    private final DayGamesRotation rotation=new DayGamesRotation();
    private final GoalLinesRotation goalRotation=new GoalLinesRotation();
    private final String[] filters={"Todos","A seguir","Em andamento","Encerrados"};
    private final SimpleDateFormat clock=fmt("HH:mm"),date=fmt("EEEE, dd 'de' MMMM"),hour=fmt("HH:mm"),shortDate=fmt("dd/MM");
    private int focus=1,filter,count;
    DayGamesScreen(SportsData sports,Typeface normal,Typeface bold){this(sports,normal,bold,false);}
    DayGamesScreen(SportsData sports,Typeface normal,Typeface bold,boolean results){
        this.sports=sports;this.normal=normal;this.bold=bold;this.results=results;
        ACCENT=results?0xFFB69CFF:0xFF43E6A4;MUTED=results?0xFFB7AFBF:0xFFABB8B0;
        PANEL=results?0xFF19151F:0xFF151A18;BORDER=results?0xFF51445F:0xFF3D5448;
        ACTIVE=results?0xFF332447:0xFF18392B;DIVIDER=results?0xFF382D44:0xFF2D3C33;
        for(int i=0;i<nav.length;i++)nav[i]=new RectF(48+i*368.8f,128,396.8f+i*368.8f,184);
        p.setSubpixelText(true);p.setDither(true);p.setHinting(Paint.HINTING_ON);
    }
    private static SimpleDateFormat fmt(String pattern){SimpleDateFormat f=new SimpleDateFormat(pattern,new Locale("pt","BR"));f.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));return f;}
    void reset(){focus=1;filter=DayGames.ALL;goalRotation.reset();rotation.enter(SystemClock.elapsedRealtime());}
    void reset(List<SportsFeedParser.Game> source){reset();if(results){filter=RecentGames.ALL;focus=1;}}
    void draw(Canvas c,List<SportsFeedParser.Game> source){
        long now=System.currentTimeMillis();boolean list=results?filter==RecentGames.ALL:filter==DayGames.ALL;List<SportsFeedParser.Game> games=results?RecentGames.select(source,filter,now):DayGames.select(source,filter);count=games.size();rotation.tick(count,SystemClock.elapsedRealtime(),list?DayGames.LIST_PER_PAGE:DayGames.PER_PAGE);int page=rotation.page;
        text(c,results?"RESULTADOS RECENTES":"JOGOS DO DIA",48,67,44,ACCENT,true);
        text(c,results&&list?"Últimos 3 dias · ordem de data e horário":date.format(new Date(results?RecentGames.dateAt(filter,now):now)),48,103,23,MUTED,false);
        right(c,clock.format(new Date()),1872,65,48,TEXT,false);
        right(c,"Horários de Brasília",1872,101,20,MUTED,false);
        button(c,nav[0],"‹ Esportes Dash",focus==0,false,true);
        for(int i=0;i<filters.length;i++){
            String label;
            int tabFilter=results?RecentGames.offsetForTab(i):i;
            if(results)label=RecentGames.label(tabFilter,now);
            else{int total=0;for(SportsFeedParser.Game g:source)if(DayGames.matches(g,i))total++;label=filters[i]+" · "+total;}
            button(c,nav[i+1],label,focus==i+1,filter==tabFilter,true);
        }
        if(games.isEmpty()){
            fill(c,new RectF(48,226,1872,1024),PANEL,17);
            center(c,results?(list?"Nenhum resultado disponível":"Nenhuma partida encerrada nesta data"):source.isEmpty()?"Nenhum jogo na programação recebida":"Nenhuma partida neste filtro",960,555,38,TEXT,true);
            center(c,results?(source.isEmpty()?sports.gamesStatus(true):"Escolha outra data acima"):source.isEmpty()?sports.gamesStatus(false):"Escolha Todos para ver a programação completa",960,601,23,MUTED,false);
        }else if(list){matchesList(c,games,page);}else{
            int from=page*DayGames.PER_PAGE,to=Math.min(from+DayGames.PER_PAGE,count);
            for(int i=from;i<to;i++){
                int position=i-from;float x=48+(position%2)*924,y=226+(position/2)*272;
                fixture(c,games.get(i),new RectF(x,y,x+900,y+254));
            }
            // Warm the next page before the automatic transition, including the cycle restart.
            int nextFrom=((page+1)%DayGames.pages(count))*DayGames.PER_PAGE;
            if(DayGames.pages(count)>1)for(int i=nextFrom;i<Math.min(nextFrom+DayGames.PER_PAGE,count);i++){
                SportsFeedParser.Game g=games.get(i);sports.logo(g.homeLogo,pixels(c,82));sports.logo(g.awayLogo,pixels(c,82));
            }
        }
        text(c,list?(results?"Atualização periódica · resultados em ordem cronológica":"Atualização periódica · jogos em ordem de horário"):"Atualização periódica · (P): pênalti · (GC): gol contra · P1–P3/PR: NHL",48,1066,17,MUTED,false);
        right(c,"SETAS navegar · OK selecionar · VOLTAR Esportes Dash",1872,1066,17,MUTED,false);
    }
    private void matchesList(Canvas c,List<SportsFeedParser.Game> games,int page){
        int from=page*DayGames.LIST_PER_PAGE,to=Math.min(from+DayGames.LIST_PER_PAGE,games.size()),perColumn=(to-from+1)/2;
        for(int col=0;col<2;col++){
            float x=48+col*924;RectF bounds=new RectF(x,226,x+900,1024);fill(c,bounds,PANEL,15);border(c,bounds,BORDER,1.5f,15);
            text(c,results?"DATA / HORA":"HORA",x+15,260,16,MUTED,true);text(c,"COMP.",x+129,260,16,MUTED,true);
            text(c,"MANDANTE",x+226,260,16,MUTED,true);center(c,results?"PLACAR":"PLACAR / STATUS",x+486,260,16,MUTED,true);right(c,"VISITANTE",x+824,260,16,MUTED,true);
            line(c,x+14,276,x+886,276,DIVIDER,1);
        }
        for(int i=from;i<to;i++){
            int index=i-from,col=index/perColumn,row=index%perColumn;SportsFeedParser.Game game=games.get(i);float x=48+col*924,y=303+row*30;
            if(row%2==0)fill(c,new RectF(x+9,y-22,x+891,y+8),results?0xFF241E2D:0xFF1E2B24,3);
            String kickoff=game.timeValid?hour.format(new Date(game.at)):"—";
            text(c,results?shortDate.format(new Date(game.at))+" "+kickoff:kickoff,x+15,y,16,MUTED,false);
            text(c,fit(RecentGames.competition(game),16,62,true),x+129,y,16,ACCENT,true);
            logo(c,game.homeLogo,game.homeAbbr,new RectF(x+190,y-20,x+216,y+6));
            text(c,fit(game.home,20,200,true),x+226,y,20,TEXT,true);
            boolean hasScore=game.complete||game.state.equals("in");
            String score=hasScore?game.homeScore+" – "+game.awayScore:DayGames.status(game,System.currentTimeMillis());
            if(hasScore&&game.note.startsWith("Pênaltis "))score+=" ("+game.note.substring(9)+")";
            float size=hasScore?22:16;p.setTypeface(bold);p.setTextSize(size);if(p.measureText(score)>112)size=Math.max(12,size*112/p.measureText(score));
            int scoreColor=!game.complete&&game.state.equals("in")?GREEN:DayGames.unavailable(game)?AMBER:hasScore?TEXT:MUTED;
            center(c,score,x+486,y,size,scoreColor,true);
            right(c,fit(game.away,20,270,true),x+824,y,20,TEXT,true);
            logo(c,game.awayLogo,game.awayAbbr,new RectF(x+840,y-20,x+866,y+6));
        }
        int next=((page+1)%DayGames.pages(games.size(),DayGames.LIST_PER_PAGE))*DayGames.LIST_PER_PAGE;
        if(games.size()>DayGames.LIST_PER_PAGE)for(int i=next;i<Math.min(next+DayGames.LIST_PER_PAGE,games.size());i++){SportsFeedParser.Game game=games.get(i);sports.logo(game.homeLogo,pixels(c,26));sports.logo(game.awayLogo,pixels(c,26));}
    }
    private void fixture(Canvas c,SportsFeedParser.Game g,RectF b){
        fill(c,b,PANEL,17);border(c,b,BORDER,1.5f,17);
        String sport=g.source.equals("soccer")?"FUTEBOL":g.source.toUpperCase(Locale.ROOT);
        text(c,fit(g.league,26,660,true),b.left+24,b.top+36,26,ACCENT,true);
        if(!g.league.equalsIgnoreCase(sport))right(c,sport,b.right-24,b.top+35,18,MUTED,true);
        line(c,b.left+24,b.top+55,b.right-24,b.top+55,DIVIDER,1);
        logo(c,g.homeLogo,g.homeAbbr,new RectF(b.left+26,b.top+68,b.left+108,b.top+150));
        logo(c,g.awayLogo,g.awayAbbr,new RectF(b.right-108,b.top+68,b.right-26,b.top+150));
        name(c,g.home,b.left+126,b.top+118,225,false);
        name(c,g.away,b.right-126,b.top+118,225,true);
        boolean score=g.complete||g.state.equals("in");
        String kickoff=results?shortDate.format(new Date(g.at))+(g.timeValid?" · "+hour.format(new Date(g.at)):""):score?(g.timeValid?hour.format(new Date(g.at)):"HOJE"):"HOJE";
        center(c,kickoff,b.centerX(),b.top+89,17,MUTED,false);
        center(c,score?g.homeScore+"  –  "+g.awayScore:g.timeValid?hour.format(new Date(g.at)):"A definir",b.centerX(),b.top+130,score?40:42,TEXT,true);
        int color=g.state.equals("in")&&!g.complete?GREEN:DayGames.unavailable(g)?AMBER:MUTED;
        center(c,DayGames.status(g,System.currentTimeMillis()),b.centerX(),b.top+200,22,color,true);
        if((g.source.equals("soccer")||g.source.equals("nhl"))&&(g.complete||g.state.equals("in"))){scorers(c,g,true,b);scorers(c,g,false,b);}
        if(!g.note.isEmpty())center(c,fit(g.note,17,b.width()-60,false),b.centerX(),b.top+228,17,MUTED,false);
    }
    private void scorers(Canvas c,SportsFeedParser.Game game,boolean home,RectF bounds){
        List<String> lines=new ArrayList<>();p.setTypeface(normal);p.setTextSize(18);
        for(String value:sports.goalLines(game,home)){
            while(p.measureText(value)>330){int n=p.breakText(value,true,330,null),space=value.lastIndexOf(' ',Math.max(0,n-1));if(space>0)n=space;n=Math.max(1,n);lines.add(value.substring(0,n).trim());value=value.substring(n).trim();}
            if(!value.isEmpty())lines.add(value);
        }
        if(lines.isEmpty())return;
        int from=goalRotation.page(game.id+(home?":home":":away"),lines.size(),SystemClock.elapsedRealtime())*GoalLinesRotation.ROWS;
        for(int i=from;i<Math.min(from+GoalLinesRotation.ROWS,lines.size());i++){
            String line=lines.get(i);float y=bounds.top+173+(i-from)*22;
            if(home)text(c,line,bounds.left+26,y,18,MUTED,false);else right(c,line,bounds.right-26,y,18,MUTED,false);
        }
    }
    private void name(Canvas c,String value,float x,float y,float width,boolean alignRight){
        p.setTypeface(bold);p.setTextSize(30);
        if(p.measureText(value)<=width){nameLine(c,value,x,y+8,alignRight);return;}
        int length=p.breakText(value,true,width,null),space=value.lastIndexOf(' ',Math.max(0,length-1));
        if(space>0)length=space;length=Math.max(1,length);
        nameLine(c,value.substring(0,length).trim(),x,y-8,alignRight);
        nameLine(c,fit(value.substring(length).trim(),30,width,true),x,y+23,alignRight);
    }
    private void nameLine(Canvas c,String value,float x,float y,boolean alignRight){if(alignRight)right(c,value,x,y,30,TEXT,true);else text(c,value,x,y,30,TEXT,true);}
    private int pixels(Canvas c,float logical){float[] m=new float[9];c.getMatrix().getValues(m);return Math.max(1,Math.round(logical*(float)Math.hypot(m[Matrix.MSCALE_X],m[Matrix.MSKEW_Y])));}
    private void logo(Canvas c,String address,String abbr,RectF bounds){
        int target=pixels(c,bounds.width());Bitmap image=sports.logo(address,target);
        if(image==null){center(c,abbr.isEmpty()?"•":fit(abbr,24,bounds.width(),true),bounds.centerX(),bounds.centerY()+9,24,MUTED,true);return;}
        float[] m=new float[9];c.getMatrix().getValues(m);float scale=Math.max(.01f,(float)Math.hypot(m[Matrix.MSCALE_X],m[Matrix.MSKEW_Y]));
        float left=(Math.round(bounds.centerX()*scale+m[Matrix.MTRANS_X]-image.getWidth()/2f)-m[Matrix.MTRANS_X])/scale;
        float top=(Math.round(bounds.centerY()*scale+m[Matrix.MTRANS_Y]-image.getHeight()/2f)-m[Matrix.MTRANS_Y])/scale;
        p.setColor(Color.WHITE);p.setFilterBitmap(true);c.drawBitmap(image,null,new RectF(left,top,left+image.getWidth()/scale,top+image.getHeight()/scale),p);
    }
    private void button(Canvas c,RectF b,String label,boolean selected,boolean active,boolean enabled){
        fill(c,b,active?ACTIVE:PANEL,11);border(c,b,selected?FOCUS:BORDER,selected?4:1.5f,11);
        center(c,label,b.centerX(),b.centerY()+9,24,enabled?TEXT:0xFF657080,true);
    }
    private void fill(Canvas c,RectF b,int color,float radius){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(b,radius,radius,p);}
    private void border(Canvas c,RectF b,int color,float width,float radius){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setColor(color);c.drawRoundRect(b,radius,radius,p);p.setStyle(Paint.Style.FILL);}
    private void line(Canvas c,float x,float y,float r,float b,int color,float width){p.setColor(color);p.setStrokeWidth(width);c.drawLine(x,y,r,b,p);}
    private void text(Canvas c,String value,float x,float y,float size,int color,boolean strong){p.setStyle(Paint.Style.FILL);p.setTypeface(strong?bold:normal);p.setTextSize(size);p.setTextAlign(Paint.Align.LEFT);p.setColor(color);c.drawText(value,x,y,p);}
    private void right(Canvas c,String value,float x,float y,float size,int color,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);text(c,value,x-p.measureText(value),y,size,color,strong);}
    private void center(Canvas c,String value,float x,float y,float size,int color,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);text(c,value,x-p.measureText(value)/2,y,size,color,strong);}
    private String fit(String value,float size,float width,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);if(p.measureText(value)<=width)return value;int n=p.breakText(value,true,Math.max(0,width-p.measureText("…")),null);return value.substring(0,n)+"…";}
    private boolean activate(){
        if(focus==0)return true;
        filter=results?RecentGames.offsetForTab(focus-1):focus-1;rotation.enter(SystemClock.elapsedRealtime());
        return false;
    }
    /** -1 unhandled, 0 handled, 1 return to sports home. */
    int onKey(KeyEvent e){
        int key=e.getKeyCode();boolean ours=key==KeyEvent.KEYCODE_BACK||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER;
        if(!ours)return -1;if(e.getAction()!=KeyEvent.ACTION_DOWN)return 0;
        if(key==KeyEvent.KEYCODE_BACK)return 1;
        if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_UP)focus=Math.max(0,focus-1);
        else if(key==KeyEvent.KEYCODE_DPAD_RIGHT||key==KeyEvent.KEYCODE_DPAD_DOWN)focus=Math.min(4,focus+1);
        else if(activate())return 1;
        return 0;
    }
    boolean touch(float x,float y){
        for(int i=0;i<nav.length;i++)if(nav[i].contains(x,y)){focus=i;return activate();}
        return false;
    }
}
