package io.freemytv;

import android.graphics.*;
import android.os.*;
import java.util.concurrent.*;

/** Clean native strokes, native corner text and one supersampled reduction per display size.
 * Geometry and source credits are bundled per circuit. No internal sector plates are rendered.
 */
final class F1TrackArtwork {
    private static final int[] COLORS={0xFFFF343A,0xFF32B8FF,0xFFFFDC32};
    private final Path[] sectors;
    private final F1CircuitDiagram.Diagram diagram;
    private final Typeface bold;
    private final Runnable changed;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile Bitmap ready;
    private volatile int readyWidth,readyHeight;
    private volatile boolean pending,closed;
    F1TrackArtwork(F1CircuitDiagram.Diagram diagram,Typeface bold,Runnable changed){
        this.diagram=diagram;this.sectors=diagram.sectors;this.bold=bold;this.changed=changed;
    }
    void draw(Canvas c,RectF bounds){
        float[] values=new float[9];c.getMatrix().getValues(values);float displayScale=Math.max(.01f,(float)Math.hypot(values[Matrix.MSCALE_X],values[Matrix.MSKEW_Y]));
        int width=Math.max(1,Math.round((bounds.width()+32)*displayScale)),height=Math.max(1,Math.round((bounds.height()+72)*displayScale));
        Bitmap image=ready;
        if(image!=null&&width==readyWidth&&height==readyHeight){
            float x=Math.round((bounds.left-16)*displayScale)/displayScale,y=Math.round((bounds.top-16)*displayScale)/displayScale;
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);c.drawBitmap(image,null,new RectF(x,y,x+width/displayScale,y+height/displayScale),p);
        }else{
            render(c,bounds);
            if(!pending&&!closed){pending=true;final float scale=displayScale;
                worker.execute(()->{
                    Bitmap high=null;
                    try{
                        // Rasterize at 3x the actual panel pixels, then reduce once with alpha-aware Lanczos.
                        high=Bitmap.createBitmap(width*3,height*3,Bitmap.Config.ARGB_8888);Canvas source=new Canvas(high);source.scale(scale*3,scale*3);
                        render(source,new RectF(16,16,width/scale-16,height/scale-56));Bitmap raster=ShieldRasterizer.reduce(high,Math.max(width,height));
                        if(!closed){readyWidth=width;readyHeight=height;ready=raster;}
                    }finally{if(high!=null)high.recycle();pending=false;main.post(()->{if(!closed)changed.run();});}
                });
            }
        }
    }
    private void render(Canvas c,RectF bounds){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setDither(true);p.setSubpixelText(true);p.setTypeface(bold);
        float sx=bounds.width()/diagram.bounds.width(),sy=bounds.height()/diagram.bounds.height();
        sx=Math.min(sx,sy*1.65f);sy=Math.min(sy,sx*1.3f);
        float ox=bounds.centerX()-diagram.bounds.centerX()*sx,oy=bounds.centerY()-diagram.bounds.centerY()*sy;
        Matrix matrix=new Matrix();matrix.setScale(sx,sy);matrix.postTranslate(ox,oy);Path target=new Path();
        p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);
        for(Path path:sectors){path.transform(matrix,target);p.setColor(0xFFCED4DE);p.setStrokeWidth(11.5f);c.drawPath(target,p);}
        for(Path path:sectors){path.transform(matrix,target);p.setColor(0xFF151519);p.setStrokeWidth(8.5f);c.drawPath(target,p);}
        for(int i=0;i<3;i++){sectors[i].transform(matrix,target);p.setColor(COLORS[i]);p.setStrokeWidth(3.2f);c.drawPath(target,p);}
        p.setStrokeCap(Paint.Cap.BUTT);p.setStyle(Paint.Style.FILL);p.setTextSize(18);
        float[] positions=new float[diagram.labels.size()*2];
        for(int i=0;i<diagram.labels.size();i++){F1CircuitDiagram.Label label=diagram.labels.get(i);positions[i*2]=ox+label.x*sx;positions[i*2+1]=oy+label.y*sy;}
        for(int pass=0;pass<7;pass++)for(int i=0;i<diagram.labels.size();i++)for(int j=i+1;j<diagram.labels.size();j++){
            float dx=positions[j*2]-positions[i*2],dy=positions[j*2+1]-positions[i*2+1],distance=(float)Math.hypot(dx,dy);
            if(distance<26&&distance>.01f){float move=(26-distance)*.28f;dx=dx/distance*move;dy=dy/distance*move;positions[i*2]-=dx;positions[i*2+1]-=dy;positions[j*2]+=dx;positions[j*2+1]+=dy;}
        }
        for(int i=0;i<diagram.labels.size();i++){
            float x=positions[i*2],y=positions[i*2+1];
            p.setColor(0xFF151519);c.drawCircle(x,y,12.5f,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.9f);p.setColor(0xFF8993A2);c.drawCircle(x,y,12.5f,p);p.setStyle(Paint.Style.FILL);
            String number=diagram.labels.get(i).number;p.setColor(0xFFF4F4F6);c.drawText(number,x-p.measureText(number)/2,y-(p.ascent()+p.descent())/2,p);
        }
        float fx=ox+diagram.startX*sx,fy=oy+diagram.startY*sy;
        for(int row=0;row<3;row++)for(int col=0;col<4;col++){p.setColor((row+col)%2==0?Color.WHITE:Color.BLACK);c.drawRect(fx+8+col*4,fy-12+row*4,fx+12+col*4,fy-8+row*4,p);}
        p.setColor(0xFFF4F4F6);p.setStrokeWidth(1.5f);c.drawLine(fx+8,fy-12,fx+8,fy+8,p);
        p.setTextSize(15);
        for(int i=0;i<3;i++){float x=bounds.centerX()-150+i*115;p.setColor(COLORS[i]);c.drawCircle(x,bounds.bottom+21,4,p);p.setColor(0xFFB5B5C0);c.drawText("SETOR "+(i+1),x+12,bounds.bottom+26,p);}
    }
    void close(){closed=true;worker.shutdownNow();ready=null;}
}
