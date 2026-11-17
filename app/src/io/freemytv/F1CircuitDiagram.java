package io.freemytv;

import android.content.Context;
import android.graphics.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Official circuit geometries traced offline, with readable labels and a consistent sector palette. */
final class F1CircuitDiagram {
    private static final int[] COLORS={0xFFFF343A,0xFF32B8FF,0xFFFFDC32};
    static final class Label {float x,y;String number;Path glyph;}
    static final class Diagram {
        String id,length,laps;int corners;RectF bounds;
        float startX,startY;Path[] sectors=new Path[3];List<Label> labels=new ArrayList<>();
    }
    private final Context context;
    private final F1VegasArtwork vegas;
    private final Runnable changed;
    private final Typeface bold;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Map<String,Diagram> diagrams=new ConcurrentHashMap<>();
    private final Map<String,F1TrackArtwork> artworks=new ConcurrentHashMap<>();
    private final Set<String> requested=Collections.synchronizedSet(new HashSet<>());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile boolean closed;
    F1CircuitDiagram(Context context,Typeface bold,Runnable changed){this.context=context.getApplicationContext();this.bold=bold;this.changed=changed;vegas=new F1VegasArtwork(bold,changed);}
    Diagram get(String id){
        if(id==null||!id.matches("[a-z_]+")||closed)return null;
        Diagram found=diagrams.get(id);if(found!=null)return found;
        if(requested.add(id))worker.execute(()->{
            try(InputStream in=context.getAssets().open("sports/circuits/"+id+".json")){
                ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[8192];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
                JSONObject o=new JSONObject(new String(out.toByteArray(),"UTF-8"));Diagram d=parse(o);if(!closed)diagrams.put(id,d);
            }catch(Exception e){android.util.Log.w("F1Circuit","Detailed diagram unavailable: "+id);}
            main.post(()->{if(!closed)changed.run();});
        });
        return null;
    }
    private static Diagram parse(JSONObject o)throws Exception {
        Diagram d=new Diagram();d.id=o.getString("id");d.length=o.getString("length").replace('.',',').replace("km"," km");d.laps=o.getString("laps");d.corners=o.getInt("corners");
        JSONArray b=o.getJSONArray("bounds");d.bounds=new RectF((float)b.getDouble(0),(float)b.getDouble(1),(float)b.getDouble(2),(float)b.getDouble(3));
        JSONArray sectors=o.getJSONArray("centerlines");for(int i=0;i<3;i++)d.sectors[i]=centerline(sectors.getJSONArray(i));
        JSONArray start=o.getJSONArray("start");d.startX=(float)start.getDouble(0);d.startY=(float)start.getDouble(1);
        JSONArray labels=o.getJSONArray("labels");for(int i=0;i<labels.length();i++){
            JSONObject item=labels.getJSONObject(i);Label label=new Label();label.x=(float)item.getDouble("x");label.y=(float)item.getDouble("y");
            label.number=item.getString("number");d.labels.add(label);
        }
        return d;
    }
    private static Path centerline(JSONArray fragments)throws Exception {
        Path path=new Path();
        for(int f=0;f<fragments.length();f++){
            JSONArray points=fragments.getJSONArray(f);if(points.length()<2)continue;
            int count=points.length();float[] xs=new float[count],ys=new float[count];
            for(int i=0;i<count;i++){JSONArray xy=points.getJSONArray(i);xs[i]=(float)xy.getDouble(0);ys[i]=(float)xy.getDouble(1);}
            path.moveTo(xs[0],ys[0]);
            for(int i=1;i<count-1;i++){
                float dx=xs[i]-xs[i-1],dy=ys[i]-ys[i-1],nx=xs[i+1]-xs[i],ny=ys[i+1]-ys[i];
                float before=(float)Math.hypot(dx,dy),after=(float)Math.hypot(nx,ny),radius=Math.min(4,Math.min(before,after)*.25f);
                if(before<.01f||after<.01f){path.lineTo(xs[i],ys[i]);continue;}
                path.lineTo(xs[i]-dx/before*radius,ys[i]-dy/before*radius);
                path.quadTo(xs[i],ys[i],xs[i]+nx/after*radius,ys[i]+ny/after*radius);
            }
            path.lineTo(xs[count-1],ys[count-1]);
        }
        return path;
    }
    void draw(Canvas c,Diagram d,RectF bounds){
        RectF inset=new RectF(bounds);inset.inset(15,0);
        if(d.id.equals("vegas")){vegas.draw(c,inset);return;}
        F1TrackArtwork artwork=artworks.get(d.id);
        if(artwork==null){artwork=new F1TrackArtwork(d,bold,()->{if(!closed)changed.run();});artworks.put(d.id,artwork);}
        artwork.draw(c,inset);
    }
    void close(){closed=true;worker.shutdownNow();vegas.close();for(F1TrackArtwork a:artworks.values())a.close();artworks.clear();diagrams.clear();}
}
