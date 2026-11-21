package io.freemytv;
import org.json.*;
import java.nio.file.*;
import java.util.*;
public final class NFLRoundsCheck {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  String currentBody=new String(Files.readAllBytes(Paths.get(args[0])),"UTF-8"),previousBody=new String(Files.readAllBytes(Paths.get(args[1])),"UTF-8");
  NFLRounds.Round current=NFLRounds.parse(currentBody,100),previous=NFLRounds.parse(previousBody,90);
  check(current.ref.year==2026&&current.ref.type==2&&current.ref.week==11&&current.games.size()==13,"Current official week and bye-aware fixture count");
  check(current.previous.same(previous.ref)&&previous.ref.week==10&&previous.games.size()==14,"Previous official week, not trailing 72 hours");
  check(LeaguePanelModel.games("nfl",current.games,false,Long.MAX_VALUE).size()==13,"Current round includes its already completed Thursday game");
  NFLRounds restored=NFLRounds.read(new NFLRounds(current,previous).json());check(restored.current.ref.same(current.ref)&&restored.previous.games.size()==14,"Round cache persists identity and games");
  JSONObject wrong=new JSONObject(currentBody);JSONObject leaked=new JSONObject(wrong.getJSONArray("events").getJSONObject(0).toString());leaked.getJSONObject("week").put("number",12);wrong.getJSONArray("events").put(leaked);check(NFLRounds.parse(wrong.toString(),1).games.size()==13,"Adjacent weeks must not leak into the round");
  JSONObject transition=new JSONObject(currentBody);transition.getJSONObject("week").put("number",1);
  NFLRounds.Round first=NFLRounds.parse(transition.toString(),1);check(first.previous.type==1&&first.previous.week==4,"Regular-season opener follows final preseason week");
  transition.getJSONObject("season").put("type",3);first=NFLRounds.parse(transition.toString(),1);check(first.previous.type==2&&first.previous.week==18,"First playoff round follows regular-season week 18");
  try{JSONObject invalid=new JSONObject(currentBody);invalid.remove("week");NFLRounds.parse(invalid.toString(),1);throw new AssertionError("Missing week accepted");}catch(JSONException expected){}
  System.out.println("PASS: NFL current/previous official rounds, bye weeks, completed current-round games, cache identity, no adjacent-week leakage, season transitions and missing metadata.");
 }
}
