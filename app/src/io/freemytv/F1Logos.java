package io.freemytv;

import android.content.Context;
import android.graphics.*;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

/** One alpha-aware reduction from the original asset to the exact physical TV pixel size. */
final class F1Logos {
    private final Context context;
    private final Runnable changed;
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor();
    private final Map<String,Bitmap> images=new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<String> pending=Collections.synchronizedSet(new HashSet<>());
    private volatile boolean closed;
    F1Logos(Context context,Runnable changed){this.context=context.getApplicationContext();this.changed=changed;}
    Bitmap get(String team,int width,int height){
        String name=key(team),cacheKey=name+":"+width+"x"+height;
        Bitmap ready=images.get(cacheKey);if(ready!=null||closed)return ready;
        if(!pending.add(cacheKey))return null;
        worker.execute(()->{
            Bitmap original=null,cropped=null;
            try{
                File directory=new File(context.getCacheDir(),"f1-logos-lanczos-v1");directory.mkdirs();
                File cached=new File(directory,name+"-"+width+"x"+height+".png");
                Bitmap saved=cached.exists()?BitmapFactory.decodeFile(cached.getPath()):null;
                if(saved!=null){if(!closed)images.put(cacheKey,saved);return;}
                try(InputStream input=context.getAssets().open("sports/f1/"+name+".png")){
                BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;options.inScaled=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
                original=BitmapFactory.decodeStream(input,null,options);
                if(original!=null){
                    int w=original.getWidth(),h=original.getHeight(),l=w,t=h,r=-1,b=-1;int[] pixels=new int[w*h];original.getPixels(pixels,0,w,0,0,w,h);
                    for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((pixels[y*w+x]>>>24)>8){l=Math.min(l,x);t=Math.min(t,y);r=Math.max(r,x);b=Math.max(b,y);}
                    cropped=r>=l&&b>=t?Bitmap.createBitmap(original,l,t,r-l+1,b-t+1):original;
                    float scale=Math.min(width/(float)cropped.getWidth(),height/(float)cropped.getHeight());
                    int longest=Math.max(1,Math.round(Math.max(cropped.getWidth(),cropped.getHeight())*scale));
                    Bitmap raster=ShieldRasterizer.reduce(cropped,longest);
                    try(FileOutputStream out=new FileOutputStream(cached)){raster.compress(Bitmap.CompressFormat.PNG,100,out);}
                    if(!closed)images.put(cacheKey,raster);
                }
                }
            }catch(Exception ignored){}
            finally{
                if(cropped!=null&&cropped!=original&&!images.containsValue(cropped))cropped.recycle();
                if(original!=null&&!images.containsValue(original))original.recycle();
                pending.remove(cacheKey);main.post(()->{if(!closed)changed.run();});
            }
        });
        return null;
    }
    void close(){closed=true;worker.shutdownNow();images.clear();}
    private static String key(String name){
        String s=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
        if(s.equals("f1"))return "f1";
        if(s.contains("mercedes"))return "mercedes";
        if(s.contains("ferrari"))return "ferrari";
        if(s.contains("mclaren"))return "mclaren";
        if(s.contains("redbull"))return "redbullracing";
        if(s.contains("racingbull")||s.contains("rbf1")||s.equals("rb"))return "racingbulls";
        if(s.contains("alpine"))return "alpine";
        if(s.contains("haas"))return "haasf1team";
        if(s.contains("audi")||s.contains("sauber"))return "audi";
        if(s.contains("williams"))return "williams";
        if(s.contains("astonmartin"))return "astonmartin";
        if(s.contains("cadillac"))return "cadillac";
        return s;
    }
}
