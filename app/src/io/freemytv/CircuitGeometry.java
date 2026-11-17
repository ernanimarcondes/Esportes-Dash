package io.freemytv;
import org.json.*;
/** Circuit mapping and normalized geometry adapted from TV-Dashboard. */
final class CircuitGeometry {
    static String trackCode(String id){
        String[][] map={{"vegas","us-2023"},{"losail","qa-2004"},{"yas_marina","ae-2009"},{"albert_park","au-1953"},{"bahrain","bh-2002"},{"jeddah","sa-2021"},{"shanghai","cn-2004"},{"suzuka","jp-1962"},{"miami","us-2022"},{"imola","it-1953"},{"monaco","mc-1929"},{"catalunya","es-1991"},{"villeneuve","ca-1978"},{"red_bull_ring","at-1969"},{"silverstone","gb-1948"},{"spa","be-1925"},{"hungaroring","hu-1986"},{"zandvoort","nl-1948"},{"monza","it-1922"},{"baku","az-2016"},{"marina_bay","sg-2008"},{"americas","us-2012"},{"rodriguez","mx-1962"},{"interlagos","br-1940"}};
        for(String[] p:map)if(p[0].equals(id))return p[1];return null;
    }
    static float[] parseTrack(String body)throws Exception{
        JSONObject feature=new JSONObject(body).getJSONArray("features").getJSONObject(0);JSONObject g=feature.getJSONObject("geometry");JSONArray coords=g.getJSONArray("coordinates");if(g.getString("type").equals("MultiLineString"))coords=coords.getJSONArray(0);
        double lat=coords.getJSONArray(0).getDouble(1),cos=Math.cos(Math.toRadians(lat));double minX=Double.MAX_VALUE,minY=Double.MAX_VALUE,maxX=-Double.MAX_VALUE,maxY=-Double.MAX_VALUE;
        for(int i=0;i<coords.length();i++){JSONArray v=coords.getJSONArray(i);double x=v.getDouble(0)*cos,y=-v.getDouble(1);minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);}
        float[] points=new float[coords.length()*2];double size=Math.max(maxX-minX,maxY-minY);
        for(int i=0;i<coords.length();i++){JSONArray v=coords.getJSONArray(i);points[i*2]=(float)((v.getDouble(0)*cos-(minX+maxX)/2)/size);points[i*2+1]=(float)((-v.getDouble(1)-(minY+maxY)/2)/size);}return points;
    }
}
