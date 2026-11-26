package io.freemytv;

import java.util.*;

/** Shared league selection rules, independent of Android rendering. */
final class LeaguePanelModel {
    static final int TABLE_PAGE=22,GAMES_PAGE=24;
    static String conference(String league,int index){
        if(league.equals("nfl"))return index==0?"AFC":"NFC";
        if(league.equals("mlb"))return index==0?"Americana":"Nacional";
        return index==0?"Leste":"Oeste";
    }
    static boolean perTeam(String league){return league.equals("nba")||league.equals("nhl")||league.equals("mlb");}
    static SportsFeedParser.Table table(List<SportsFeedParser.Table> tables,String league,int index){
        String group=conference(league,index);
        for(SportsFeedParser.Table table:tables)if(table.key.equals(league)&&table.group.equalsIgnoreCase(group))return table;
        return null;
    }
    static List<SportsFeedParser.Game> games(String league,List<SportsFeedParser.Game> input,boolean results,long now){
        Map<String,SportsFeedParser.Game> unique=new HashMap<>();
        for(SportsFeedParser.Game game:input){
            if(!game.source.equals(league)||game.at<=0)continue;
            SportsFeedParser.Game old=unique.get(game.id);if(old==null||newer(game,old))unique.put(game.id,game);
        }
        List<SportsFeedParser.Game> selected=new ArrayList<>();
        if(perTeam(league)){
            Map<String,SportsFeedParser.Game> byTeam=new HashMap<>();
            for(SportsFeedParser.Game game:unique.values()){
                if(results?(!game.complete||game.at>now):(game.complete||DayGames.unavailable(game)))continue;
                for(String team:Arrays.asList(team(game.homeId,game.homeAbbr,game.home),team(game.awayId,game.awayAbbr,game.away))){
                    if(team.isEmpty())continue;SportsFeedParser.Game old=byTeam.get(team);
                    if(old==null||preferred(game,old,results))byTeam.put(team,game);
                }
            }
            Map<String,SportsFeedParser.Game> matches=new HashMap<>();for(SportsFeedParser.Game game:byTeam.values())matches.put(game.id,game);
            return DayGames.select(new ArrayList<>(matches.values()),DayGames.ALL);
        }
        for(SportsFeedParser.Game game:unique.values()){
            boolean include=!results||game.complete&&game.at<=now;
            if(include)selected.add(game);
        }
        return DayGames.select(selected,DayGames.ALL);
    }
    /** Two fixtures per overview card; results start at the newest pair and then show older pairs. */
    static List<SportsFeedParser.Game> summaryGames(List<SportsFeedParser.Game> games,int page,boolean results){
        if(games.isEmpty())return Collections.emptyList();
        int current=DayGames.clamp(page,games.size(),2),from,to;
        if(results){to=games.size()-current*2;from=Math.max(0,to-2);}
        else{from=current*2;to=Math.min(games.size(),from+2);}
        return games.subList(from,to);
    }
    private static String team(String id,String abbr,String name){return !id.isEmpty()?id:!abbr.isEmpty()?abbr:name;}
    private static boolean newer(SportsFeedParser.Game game,SportsFeedParser.Game old){
        // Calendar endpoints can lag behind the live scoreboard; never downgrade a final/live game to pregame.
        if(game.complete!=old.complete)return game.complete;
        if(!DayGames.unavailable(game)&&!DayGames.unavailable(old)&&!game.state.equals(old.state)){
            if(game.state.equals("in"))return true;if(old.state.equals("in"))return false;
        }
        // Team calendars can omit a live score; keep the scoreboard's known score for the same state.
        if(game.complete==old.complete&&game.state.equals(old.state)&&(game.complete||game.state.equals("in"))){
            boolean score=hasScore(game),oldScore=hasScore(old);if(score!=oldScore)return score;
        }
        return game.fetched>old.fetched;
    }
    private static boolean hasScore(SportsFeedParser.Game game){return !game.homeScore.isEmpty()&&!game.awayScore.isEmpty()&&!game.homeScore.equals("—")&&!game.awayScore.equals("—");}
    static List<SportsFeedParser.Game> trimCalendar(List<SportsFeedParser.Game> games,String team,long now){
        return trimCalendar("nba",games,team,now);
    }
    static List<SportsFeedParser.Game> trimCalendar(String league,List<SportsFeedParser.Game> games,String team,long now){
        List<SportsFeedParser.Game> ended=new ArrayList<>(),current=new ArrayList<>();
        for(SportsFeedParser.Game game:games)if(game.source.equals(league)&&game.at>0&&(team.equals(game.homeId)||team.equals(game.awayId))){
            if(game.complete&&game.at<=now)ended.add(game);
            else if(!game.complete&&!DayGames.unavailable(game))current.add(game);
        }
        ended.sort((a,b)->Long.compare(b.at,a.at));current.sort((a,b)->preferred(a,b,false)?-1:preferred(b,a,false)?1:0);
        List<SportsFeedParser.Game> selected=new ArrayList<>(ended.subList(0,Math.min(2,ended.size())));
        selected.addAll(current.subList(0,Math.min(3,current.size())));return selected;
    }
    private static boolean preferred(SportsFeedParser.Game game,SportsFeedParser.Game old,boolean results){
        if(results)return game.at!=old.at?game.at>old.at:game.id.compareTo(old.id)<0;
        boolean live=game.state.equals("in"),oldLive=old.state.equals("in");if(live!=oldLive)return live;
        if(game.timeValid!=old.timeValid)return game.timeValid;
        return game.at!=old.at?game.at<old.at:game.id.compareTo(old.id)<0;
    }
    static String percentage(SportsFeedParser.Row row){
        try{
            String[] values=row.record.split("[–—-]");if(values.length<2)return "—";
            double wins=Double.parseDouble(values[0]),losses=Double.parseDouble(values[1]),ties=values.length>2?Double.parseDouble(values[2]):0,total=wins+losses+ties;
            return total>0?String.format(new Locale("pt","BR"),"%.1f%%",100*(wins+ties*.5)/total):"—";
        }catch(Exception ignored){return "—";}
    }
}
