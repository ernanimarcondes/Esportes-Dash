package io.freemytv;
import org.json.*;
import java.nio.file.*;
import java.util.*;
public final class MatchGoalsSummaryCheck {
 static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
 static String file(String path)throws Exception{return new String(Files.readAllBytes(Paths.get(path)),"UTF-8");}
 static SportsFeedParser.Game find(List<SportsFeedParser.Game> games,String id){for(SportsFeedParser.Game game:games)if(game.id.endsWith(":"+id))return game;throw new AssertionError("Missing fixture "+id);}
 public static void main(String[] args)throws Exception{
  String nhl=file(args[0]);SportsFeedParser.Game source=find(SportsFeedParser.scoreboard("nhl",file(args[1]),1),"401891809");
  SportsFeedParser.Game game=MatchGoalsSummary.read(source,nhl);check(game.goals.size()==6&&FootballGoals.complete(game),"Complete historical NHL scorers");
  check(FootballGoals.lines(game,true).size()==3&&FootballGoals.lines(game,false).size()==1,"Group repeated NHL scorers by team");
  check(game.goals.get(0).minute.equals("P1 11:12")&&game.goals.get(3).minute.equals("P2 6:06"),"Hockey clock and period");
  check(!FootballGoals.lines(game,true).toString().contains("Sebastian Aho"),"Do not present assists as goals");
  check(FootballGoals.lines(SportsFeedParser.Game.read(game.json()),true).equals(FootballGoals.lines(game,true)),"NHL detail cache round trip");
  SportsFeedParser.Game changed=SportsFeedParser.Game.read(source.json());changed.homeScore="999";check(!MatchGoalsSummary.sameScore(changed,game),"Stale scorer details must not match a different score");
  JSONObject wrong=new JSONObject(nhl);wrong.getJSONObject("header").put("id","1");try{MatchGoalsSummary.read(source,wrong.toString());throw new AssertionError("Wrong match accepted");}catch(IllegalArgumentException expected){}
  SportsFeedParser.Game different=SportsFeedParser.Game.read(source.json());different.homeId="wrong";try{MatchGoalsSummary.read(different,nhl);throw new AssertionError("Wrong teams accepted");}catch(IllegalArgumentException expected){}
  SportsFeedParser.Game soccer=find(SportsFeedParser.scoreboard("soccer",file(args[2]),1),"401861189");SportsFeedParser.Game soccerDetails=MatchGoalsSummary.read(soccer,file(args[3]));check(soccerDetails.goals.size()==5&&FootballGoals.complete(soccerDetails),"Football summary fallback includes historical scorers");
  JSONObject shootout=new JSONObject(nhl),competition=shootout.getJSONObject("header").getJSONArray("competitions").getJSONObject(0);JSONArray regular=new JSONArray();JSONObject home=null,away=null;
  for(int i=0;i<shootout.getJSONArray("plays").length();i++){JSONObject play=shootout.getJSONArray("plays").getJSONObject(i);if(!play.optBoolean("scoringPlay"))continue;if(play.getJSONObject("team").getString("id").equals(source.homeId)&&home==null)home=play;if(play.getJSONObject("team").getString("id").equals(source.awayId)&&away==null)away=play;}
  regular.put(home).put(away);JSONObject shot=new JSONObject(home.toString());shot.put("id","shootout-shot").put("period",new JSONObject().put("number",5).put("displayValue","SO"));regular.put(shot);shootout.put("plays",regular);
  for(int i=0;i<competition.getJSONArray("competitors").length();i++){JSONObject team=competition.getJSONArray("competitors").getJSONObject(i);team.put("score",team.optString("homeAway").equals("home")?"2":"1");}
  competition.put("status",new JSONObject().put("type",new JSONObject().put("detail","Final/SO")));SportsFeedParser.Game so=MatchGoalsSummary.read(source,shootout.toString());check(so.goals.size()==2&&FootballGoals.complete(so)&&FootballGoals.lines(so,true).contains("Vitória no shootout"),"Shootout winner is not a regulation goal or an unknown scorer");
  System.out.println("PASS: historical NHL goals and periods, scorer/assist separation, grouping, persisted cache, identity/score checks, historical football fallback and shootout handling.");
 }
}
