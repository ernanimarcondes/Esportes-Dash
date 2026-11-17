package io.freemytv;
public final class TeamLogoPolicyCheck {
 static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
 static void checkSport(String sport,String id,boolean scoreboard){
  String base="https://a.espncdn.com/i/teamlogos/"+sport+"/",normal=base+"500/"+id+".png",dark=base+"500-dark/"+(scoreboard?"scoreboard/":"")+id+".png";
  for(String input:new String[]{normal,dark,base+"100/"+id+".png",base+"500/scoreboard/"+id+".png"}){
   String[] urls=TeamLogoPolicy.candidates(input);check(urls.length>=2&&urls[0].equals(dark)&&urls[1].equals(normal),sport+" high-quality dark and original fallback");
  }
 }
 public static void main(String[] args){
  checkSport("nba","bos",true);checkSport("nfl","dal",true);checkSport("nhl","tb",true);checkSport("mlb","nyy",true);
  checkSport("soccer","2029",false);checkSport("countries","bra",false);
  for(String suffix:new String[]{"soccer/500/3458.png","nfl/500/nyj.png","mlb/500/cle.png"}){
   String normal="https://a.espncdn.com/i/teamlogos/"+suffix;String[] current=TeamLogoPolicy.candidates(normal);check(current.length==1&&current[0].equals(normal),"Keep current team marks when dark versions are obsolete");
  }
  String[] match=TeamLogoPolicy.candidates("https://a.espncdn.com/i/teamlogos/nhl/500/scoreboard/tb.png");check(match.length==3&&match[0].equals("https://a.espncdn.com/i/teamlogos/nhl/500-dark/scoreboard/tb.png")&&match[2].equals("https://a.espncdn.com/i/teamlogos/nhl/500/scoreboard/tb.png"),"Match cards must not duplicate scoreboard; preserve exact feed URL as final fallback");
  for(String invalid:new String[]{null,"","http://a.espncdn.com/i/teamlogos/nba/500/bos.png","https://a.espncdn.com.evil.test/logo.png","https://example.com/logo.png"})check(TeamLogoPolicy.candidates(invalid).length==0,"External URL rejected");
  String other="https://a.espncdn.com/other/logo.png";check(TeamLogoPolicy.candidates(other).length==1&&TeamLogoPolicy.candidates(other)[0].equals(other),"Unknown ESPN path remains usable");
  System.out.println("PASS: football, national teams, NBA/NFL/NHL/MLB dark variants; high-resolution normalization; original fallback; invalid hosts rejected.");
 }
}
