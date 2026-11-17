package io.freemytv;

import java.util.regex.*;
import java.util.LinkedHashSet;

/** Official ESPN dark-background variants for every sports card, with the normal original as fallback. */
final class TeamLogoPolicy {
    private static final String HOST="https://a.espncdn.com/";
    private static final Pattern TEAM=Pattern.compile("^(https://a\\.espncdn\\.com/i/teamlogos/)(nba|nfl|nhl|mlb|soccer|countries)/500/(.+)$");
    static String[] candidates(String address){
        if(address==null||!address.startsWith(HOST))return new String[0];
        String supplied=address.replaceAll("/(?:50|100|150|200|250)(-dark)?/","/500$1/");
        String normal=supplied.replace("/500-dark/scoreboard/","/500/").replace("/500-dark/","/500/").replace("/500/scoreboard/","/500/");
        Matcher match=TEAM.matcher(normal);if(!match.matches())return new String[]{normal};
        String sport=match.group(2),id=match.group(3).split("[?]",2)[0];
        // These dark variants still carry superseded marks; keep the current identity.
        if((sport.equals("soccer")&&id.equals("3458.png"))||(sport.equals("nfl")&&id.equals("nyj.png"))||(sport.equals("mlb")&&id.equals("cle.png")))return new String[]{normal};
        String suffix=(sport.equals("soccer")||sport.equals("countries"))?"":"scoreboard/";
        String dark=match.group(1)+sport+"/500-dark/"+suffix+match.group(3);
        LinkedHashSet<String> urls=new LinkedHashSet<>();urls.add(dark);urls.add(normal);urls.add(supplied);return urls.toArray(new String[0]);
    }
}
