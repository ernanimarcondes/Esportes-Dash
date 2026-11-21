package io.freemytv;
import java.util.*;
public final class LeaguePanelModelCheck {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static SportsFeedParser.Game game(String id,String league,long at,long fetched,boolean complete){SportsFeedParser.Game g=new SportsFeedParser.Game();g.id=id;g.source=league;g.at=at;g.fetched=fetched;g.complete=complete;return g;}
 static SportsFeedParser.Game match(String id,String home,String away,long at,boolean complete){SportsFeedParser.Game g=game(id,"nba",at,2,complete);g.homeId=home;g.awayId=away;return g;}
 public static void main(String[] args){
  long now=java.time.Instant.parse("2026-11-21T21:00:00Z").toEpochMilli(),day=86400000L;
  SportsFeedParser.Game ab=match("ab","A","B",now-10*day,true),ac=match("ac","A","C",now-2*day,true),bc=match("bc","B","C",now-day,true),da=match("da","D","A",now-90*day,true),ad=match("ad","A","D",now+10*day,false),bd=match("bd","B","D",now+day,false),cd=match("cd","C","D",now-1000,false),far=match("far","E","F",now+50*day,false),cancel=match("cancel","A","B",now+1000,false);
  cd.state="in";cancel.status="STATUS_POSTPONED";
  SportsFeedParser.Game stale=match("cd","C","D",now-1000,false);stale.fetched=1;cd.fetched=2;
  List<SportsFeedParser.Game> source=Arrays.asList(ab,ac,bc,da,ad,bd,stale,cd,far,cancel);
  List<SportsFeedParser.Game> current=LeaguePanelModel.games("nba",source,false,now);Set<String> ids=new HashSet<>();for(SportsFeedParser.Game g:current)ids.add(g.id);
  check(ids.equals(new HashSet<>(Arrays.asList("ad","bd","cd","far"))),"Each team's current matchup: live first, otherwise next fixture, even many days away; canceled games excluded");
  List<SportsFeedParser.Game> results=LeaguePanelModel.games("nba",source,true,now);ids.clear();for(SportsFeedParser.Game g:results)ids.add(g.id);
  check(ids.equals(new HashSet<>(Arrays.asList("ac","bc","da"))),"Each team's last completed matchup, including a 90-day gap, without a shared date window");
  check(Collections.frequency(current,cd)==1&&current.contains(cd)&&!current.contains(stale),"Shared opponents do not duplicate fixtures; fresh state replaces stale copy");
  check(source.get(0)==ab&&current.get(0)==cd,"Input unchanged and selected fixtures ordered chronologically");
  SportsFeedParser.Game other=game("other","nfl",now,2,false);check(LeaguePanelModel.games("nba",Arrays.asList(other),false,now).isEmpty(),"Other leagues excluded");
  List<SportsFeedParser.Game> trimmed=LeaguePanelModel.trimCalendar(source,"A",now);check(trimmed.size()<=5&&trimmed.contains(ac)&&trimmed.contains(ab)&&trimmed.contains(ad)&&!trimmed.contains(cancel)&&!trimmed.contains(da),"Calendar cache keeps relevant match sequence, not a date window or the whole season");
  check(LeaguePanelModel.trimCalendar(source,"E",now).contains(far),"Calendar cache retains the next matchup even fifty days away");
  SportsFeedParser.Game liveScore=match("live","A","B",now-1000,false),calendarCopy=match("live","A","B",now-1000,false);liveScore.state=calendarCopy.state="in";liveScore.homeScore="115";liveScore.awayScore="110";liveScore.fetched=2;calendarCopy.fetched=3;
  List<SportsFeedParser.Game> kept=LeaguePanelModel.games("nba",Arrays.asList(liveScore,calendarCopy),false,now);check(kept.size()==1&&kept.get(0)==liveScore,"A newer team calendar without scores must not erase a known live scoreboard");
  SportsFeedParser.Table east=new SportsFeedParser.Table(),west=new SportsFeedParser.Table();east.key=west.key="nba";east.group="Leste";west.group="Oeste";
  check(LeaguePanelModel.table(Arrays.asList(west,east),"nba",0)==east&&LeaguePanelModel.table(Arrays.asList(west,east),"nba",1)==west,"Conference selection independent of feed order");
  check(LeaguePanelModel.table(Arrays.asList(west),"nba",0)==null,"Missing conference must not duplicate another one");
  check(LeaguePanelModel.conference("nfl",0).equals("AFC")&&LeaguePanelModel.conference("nfl",1).equals("NFC"),"NFL names");
  SportsFeedParser.Row row=new SportsFeedParser.Row();row.record="10–5";check(LeaguePanelModel.percentage(row).equals("66,7%"),"NBA winning percentage");row.record="6–3–1";check(LeaguePanelModel.percentage(row).equals("65,0%"),"NFL ties count as half a win");row.record="0–0";check(LeaguePanelModel.percentage(row).equals("—"),"No divide by zero");row.record="unavailable";check(LeaguePanelModel.percentage(row).equals("—"),"Unknown record");
  check(DayGames.pages(30,LeaguePanelModel.GAMES_PAGE)==2&&DayGames.pages(16,LeaguePanelModel.TABLE_PAGE)==1,"Games paginate; full NBA/NFL conferences fit on one screen");
  System.out.println("PASS: league/conference routing, missing groups, freshest score/state, per-team current and last fixtures without date windows, percentages with ties and page capacity.");
 }
}
