package io.freemytv;
import java.time.*;
import java.util.*;
public final class RecentGamesCheck {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static SportsFeedParser.Game game(String id,String time,boolean complete){SportsFeedParser.Game g=new SportsFeedParser.Game();g.id=id;g.at=Instant.parse(time).toEpochMilli();g.complete=complete;return g;}
 public static void main(String[] args){
  long now=Instant.parse("2026-11-17T21:00:00Z").toEpochMilli();
  SportsFeedParser.Game today=game("today","2026-11-17T20:00:00Z",true),late=game("late","2026-11-17T01:30:00Z",true),early=game("early","2026-11-16T23:00:00Z",true),old=game("old","2026-11-14T20:59:59Z",true),limit=game("limit","2026-11-14T21:00:00Z",true),future=game("future","2026-11-18T00:00:00Z",true),live=game("live","2026-11-17T19:00:00Z",false);
  List<SportsFeedParser.Game> source=Arrays.asList(late,old,future,early,live,today,limit);
  List<SportsFeedParser.Game> current=RecentGames.select(source,0,now);check(current.size()==1&&current.get(0)==today,"Today includes completed games only");
  List<SportsFeedParser.Game> yesterday=RecentGames.select(source,1,now);check(yesterday.size()==2&&yesterday.get(0)==early&&yesterday.get(1)==late,"Brazilian match date and ascending kickoff order");
  check(RecentGames.select(source,2,now).isEmpty(),"Empty day");check(RecentGames.select(source,3,now).size()==1&&RecentGames.select(source,3,now).get(0)==limit,"Rolling recent-results boundary");
  check(RecentGames.select(source,4,now).isEmpty()&&RecentGames.select(source,-2,now).isEmpty(),"Out-of-range dates");
  check(RecentGames.latest(source,now)==0&&RecentGames.latest(Arrays.asList(late),now)==1&&RecentGames.latest(Collections.emptyList(),now)==0,"Open the latest date with results");
  check(RecentGames.label(0,now).equals("Hoje · 17/11")&&RecentGames.label(1,now).equals("Ontem · 16/11")&&RecentGames.label(2,now).contains("15/11"),"Date-tab labels");
  long tomorrow=now+86400000L;check(RecentGames.label(0,tomorrow).contains("18/11")&&RecentGames.latest(Arrays.asList(today),tomorrow)==1,"Date tabs follow the Brazilian calendar after midnight");
  check(Instant.ofEpochMilli(RecentGames.dateAt(1,now)).atZone(RecentGames.ZONE).toLocalDate().equals(LocalDate.of(2026,11,16)),"Selected-date header");
  check(RecentGames.offsetForTab(0)==RecentGames.ALL&&RecentGames.tabForOffset(RecentGames.ALL)==0,"All results must be the first tab");
  for(int tab=1;tab<4;tab++)check(RecentGames.offsetForTab(tab)==4-tab&&RecentGames.tabForOffset(4-tab)==tab,"Older-to-newer date tabs after All");
  check(RecentGames.label(RecentGames.offsetForTab(0),now).equals("Todos")&&RecentGames.label(RecentGames.offsetForTab(1),now).contains("14/11")&&RecentGames.label(RecentGames.offsetForTab(3),now).contains("16/11"),"No dedicated today tab");
  List<SportsFeedParser.Game> all=RecentGames.select(source,RecentGames.ALL,now);check(all.size()==4&&all.get(0)==limit&&all.get(3)==today,"All dates include all eligible recent results in chronological order");
  check(DayGames.pages(48,RecentGames.LIST_PER_PAGE)==1&&DayGames.pages(49,RecentGames.LIST_PER_PAGE)==2&&DayGames.pages(96,RecentGames.LIST_PER_PAGE)==2,"Compact list pagination");
  DayGamesRotation listRotation=new DayGamesRotation();listRotation.enter(0);listRotation.tick(49,0,RecentGames.LIST_PER_PAGE);listRotation.tick(49,19999,RecentGames.LIST_PER_PAGE);check(listRotation.page==0,"List remains twenty seconds");listRotation.tick(49,20000,RecentGames.LIST_PER_PAGE);check(listRotation.page==1,"Overflow list advances automatically");listRotation.tick(49,40000,RecentGames.LIST_PER_PAGE);check(listRotation.page==0,"List wraps without losing the final result");
  GoalLinesRotation compact=new GoalLinesRotation();for(int t=0;t<5000;t+=500)check(compact.page("home",3,t,1)==0,"Compact goal row remains five seconds");check(compact.page("home",3,5000,1)==1,"Compact goal row advances");
  System.out.println("PASS: date tabs, Brazilian midnight, chronological results, completed-only filter, rolling window, All-first tabs and compact list pagination, empty dates and next-day refresh.");
 }
}
