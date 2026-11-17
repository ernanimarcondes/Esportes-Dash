package io.freemytv;

import java.util.*;

/** A single chronological schedule, with optional status filters and manual pages. */
final class DayGames {
    static final int ALL=0,UPCOMING=1,LIVE=2,FINISHED=3,PER_PAGE=6;
    static boolean unavailable(SportsFeedParser.Game g){
        return g.status.contains("POSTPONED")||g.status.contains("CANCELED")||g.status.contains("CANCELLED")||g.status.contains("SUSPEND");
    }
    static boolean matches(SportsFeedParser.Game g,int filter){
        if(filter==ALL)return true;
        if(filter==FINISHED)return g.complete;
        if(filter==LIVE)return !g.complete&&g.state.equals("in");
        return !g.complete&&!g.state.equals("in")&&!unavailable(g);
    }
    static List<SportsFeedParser.Game> select(List<SportsFeedParser.Game> source,int filter){
        List<SportsFeedParser.Game> selected=new ArrayList<>();
        for(SportsFeedParser.Game g:source)if(matches(g,filter))selected.add(g);
        selected.sort(Comparator.comparingLong((SportsFeedParser.Game g)->g.timeValid&&g.at>0?g.at:Long.MAX_VALUE).thenComparing(g->g.id));
        return selected;
    }
    static int pages(int count){return pages(count,PER_PAGE);}
    static int pages(int count,int perPage){perPage=Math.max(1,perPage);return Math.max(1,(count+perPage-1)/perPage);}
    static int clamp(int page,int count){return clamp(page,count,PER_PAGE);}
    static int clamp(int page,int count,int perPage){return Math.max(0,Math.min(page,pages(count,perPage)-1));}
    static String status(SportsFeedParser.Game g,long now){
        if(g.complete)return "Encerrado";
        if(g.status.contains("POSTPONED"))return "Adiado";
        if(g.status.contains("CANCELED")||g.status.contains("CANCELLED"))return "Cancelado";
        if(g.status.contains("SUSPEND"))return "Suspenso";
        if(g.state.equals("in"))return "Em andamento";
        if(!g.timeValid)return "Horário a confirmar";
        return g.at<now-5*60000L?"Horário previsto":"A seguir";
    }
}
