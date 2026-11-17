package io.freemytv;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Date tabs over the existing recent-results window, using the viewer's Brazilian match date. */
final class RecentGames {
    static final int DAYS=4,ALL=-1,LIST_ROWS=24,LIST_PER_PAGE=48;
    static final ZoneId ZONE=ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("dd/MM"),WEEKDAY=DateTimeFormatter.ofPattern("EEE",new Locale("pt","BR"));
    static int offsetForTab(int tab){return tab<=0?ALL:DAYS-Math.min(tab,DAYS-1);}
    static int tabForOffset(int offset){return offset<1?0:DAYS-Math.min(offset,DAYS-1);}
    static LocalDate day(int offset,long now){return Instant.ofEpochMilli(now).atZone(ZONE).toLocalDate().minusDays(offset);}
    static long dateAt(int offset,long now){return day(offset,now).atStartOfDay(ZONE).toInstant().toEpochMilli();}
    static String label(int offset,long now){
        if(offset==ALL)return "Todos";
        LocalDate day=day(offset,now);String prefix=offset==0?"Hoje":offset==1?"Ontem":WEEKDAY.format(day);
        prefix=Character.toUpperCase(prefix.charAt(0))+prefix.substring(1);
        return prefix+" · "+DATE.format(day);
    }
    static List<SportsFeedParser.Game> select(List<SportsFeedParser.Game> source,int offset,long now){
        if(offset<ALL||offset>=DAYS)return Collections.emptyList();
        LocalDate day=offset==ALL?null:day(offset,now);List<SportsFeedParser.Game> matches=new ArrayList<>();
        for(SportsFeedParser.Game game:source)if(game.complete&&game.at>0&&game.at>=now-72*3600000L&&game.at<=now&&(offset==ALL||Instant.ofEpochMilli(game.at).atZone(ZONE).toLocalDate().equals(day)))matches.add(game);
        List<SportsFeedParser.Game> selected=DayGames.select(matches,DayGames.ALL);
        if(offset==ALL)selected.sort(Comparator.comparing((SportsFeedParser.Game game)->Instant.ofEpochMilli(game.at).atZone(ZONE).toLocalDate()).thenComparingLong(game->game.timeValid?game.at:Long.MAX_VALUE).thenComparing(game->game.id));
        return selected;
    }
    static String competition(SportsFeedParser.Game game){
        if(!game.source.equals("soccer"))return game.source.toUpperCase(Locale.ROOT);
        switch(game.league){case "Brasileirão Série A":return "BRA A";case "Brasileirão Série B":return "BRA B";case "Premier League":return "ING";case "Ligue 1":return "FRA";case "LaLiga":return "ESP";case "Liga Portugal":return "POR";case "Bundesliga":return "ALE";case "Libertadores":return "LIB";case "Champions League":return "UCL";case "Liga das Nações":return "Nações";case "Amistosos de seleções":return "Amist.";default:return "FUT";}
    }
    static int latest(List<SportsFeedParser.Game> source,long now){for(int i=0;i<DAYS;i++)if(!select(source,i,now).isEmpty())return i;return 0;}
}
