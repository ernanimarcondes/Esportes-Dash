package io.freemytv;
import org.json.*;
import java.nio.file.*;
import java.util.*;
public final class FootballGoalsCheck {
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
 static SportsFeedParser.Game find(List<SportsFeedParser.Game> games,String id){for(SportsFeedParser.Game g:games)if(g.id.equals("soccer:"+id))return g;throw new AssertionError("Fixture missing "+id);}
 public static void main(String[] args)throws Exception{
  String body=new String(Files.readAllBytes(Paths.get(args[0])),"UTF-8");List<SportsFeedParser.Game> games=SportsFeedParser.scoreboard("soccer",body,123);
  SportsFeedParser.Game brazil=find(games,"401916335");check(brazil.goalDetailsParsed&&brazil.goals.size()==7,"Seven Brazilian goals from the real feed");
  check(FootballGoals.lines(brazil,true).isEmpty(),"Own goal must not be credited to the author's team");
  List<String> away=FootballGoals.lines(brazil,false);check(away.size()==5,"Repeated scorer must be grouped");
  check(away.get(0).contains("Vinícius Júnior")&&away.get(0).contains("9', 15', 21'"),"Player accents and multiple minutes");check(away.get(4).contains("74' (GC)"),"Own goal annotation and minute");
  SportsFeedParser.Game norway=find(games,"401861194");boolean penalty=false;for(FootballGoals.Goal g:norway.goals)if(g.penalty){penalty=true;check(g.minute.equals("90+6'")&&g.moment().contains("(P)"),"Added time and penalty");}check(penalty,"Real penalty event missing");
  SportsFeedParser.Game restored=SportsFeedParser.Game.read(brazil.json());check(restored.goalDetailsParsed&&restored.goals.size()==7&&FootballGoals.lines(restored,false).equals(away),"Goal cache round trip");
  JSONObject comp=new JSONObject(body).getJSONArray("events").getJSONObject(0).getJSONArray("competitions").getJSONObject(0);
  JSONArray source=comp.getJSONArray("details");JSONObject goal=null;for(int i=0;i<source.length();i++)if(source.getJSONObject(i).optBoolean("scoringPlay")){goal=source.getJSONObject(i);break;}
  check(goal!=null,"Goal fixture missing");String team=goal.getJSONObject("team").getString("id");SportsFeedParser.Game sample=new SportsFeedParser.Game();sample.homeId=team;sample.awayId="away";
  JSONObject shootout=new JSONObject(goal.toString()).put("shootout",true),card=new JSONObject(goal.toString()).put("scoringPlay",false),unknown=new JSONObject(goal.toString()).put("team",new JSONObject().put("id","not-in-match"));
  FootballGoals.read(sample,new JSONObject().put("details",new JSONArray().put(goal).put(goal).put(shootout).put(card).put(unknown)));
  check(sample.goals.size()==1,"Ignore duplicate goals, cards, shootouts and unknown beneficiaries");
  JSONObject noClock=new JSONObject(goal.toString()).put("clock",new JSONObject().put("value",53));FootballGoals.read(sample,new JSONObject().put("details",new JSONArray().put(noClock)));check(sample.goals.get(0).minute.equals("1'"),"Minute fallback");
  sample.goals.clear();sample.homeScore="2";check(FootballGoals.lines(sample,true).get(0).contains("indisponíveis"),"Missing coverage must not invent scorers");
  check(!SportsFeedParser.Game.read(new JSONObject()).goalDetailsParsed,"Old cache migration marker");
  GoalLinesRotation rotation=new GoalLinesRotation();for(int t=0;t<5000;t+=500)check(rotation.page("test",17,t)==0,"Scorers must remain for five seconds");check(rotation.page("test",17,5000)==1,"Five-second scorer transition");
  for(int t=5500;t<=20000;t+=500)rotation.page("test",17,t);check(rotation.page("test",17,20000)==4,"All scorer groups can be reached");
  for(int t=20500;t<=25000;t+=500)rotation.page("test",17,t);check(rotation.page("test",17,25000)==0,"Scorer cycle wrap");
  check(rotation.page("test",17,60000)==0,"Hidden match must resume without skipping scorers");for(int t=60500;t<=65000;t+=500)rotation.page("test",17,t);check(rotation.page("test",17,65000)==1,"Five full visible seconds after returning");check(rotation.page("test",2,65500)==0,"Shrinking scorer list clamps page");
  System.out.println("PASS: real scorers/minutes, grouped goals, own-goal beneficiary, added time, penalties, shootout/card exclusions, deduplication, accents, cache, missing coverage and 5-second scorer rotation.");
 }
}
