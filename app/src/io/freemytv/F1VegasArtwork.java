package io.freemytv;

import android.graphics.*;
import android.os.*;
import java.util.concurrent.*;

/** Clean, compact schematic of the official Las Vegas circuit. Only this circuit uses the refinement.
 * Source: https://www.formula1.com/en/racing/2026/las-vegas
 * Curves and sectors follow the published diagram; internal sector plates are deliberately omitted.
 */
final class F1VegasArtwork {
    private static final int[] COLORS={0xFFFF343A,0xFF32B8FF,0xFFFFDC32};
    private static final float[][] TURNS={{1580,50},{1500,132},{1640,295},{1505,290},{697,380},{738,98},{527,40},{550,162},{424,43},{380,430},{160,560},{110,745},{715,952},{1727,1035},{1655,944},{1805,910},{1810,339}};
    private final Path[] sectors={new Path(),new Path(),new Path()};
    private final Typeface bold;
    private final Runnable changed;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile Bitmap ready;
    private volatile int readyWidth,readyHeight;
    private volatile boolean pending,closed;
    F1VegasArtwork(Typeface bold,Runnable changed){
        this.bold=bold;this.changed=changed;
        Path a=sectors[0];a.moveTo(1722,254);a.cubicTo(1655,176,1605,78,1576,99);a.cubicTo(1520,129,1570,215,1592,255);a.cubicTo(1624,341,1560,337,1480,340);a.lineTo(762,356);a.quadTo(728,356,728,316);a.lineTo(728,260);
        a=sectors[1];a.moveTo(728,260);a.lineTo(727,190);a.quadTo(714,80,598,76);a.cubicTo(548,68,532,60,530,90);a.cubicTo(520,146,484,108,466,88);a.cubicTo(451,65,440,87,440,110);a.lineTo(444,345);a.cubicTo(444,452,398,496,342,520);a.lineTo(206,580);a.quadTo(185,590,175,615);a.lineTo(155,714);a.quadTo(153,730,172,737);a.lineTo(280,800);
        a=sectors[2];a.moveTo(280,800);a.lineTo(640,987);a.quadTo(680,1005,725,1004);a.lineTo(1655,1004);a.quadTo(1692,1004,1693,973);a.lineTo(1693,950);a.lineTo(1745,899);a.quadTo(1768,878,1768,846);a.lineTo(1776,382);a.cubicTo(1776,330,1758,295,1722,254);
    }
    Path[] compactSectors(){return sectors;}
    void draw(Canvas c,RectF bounds){
        float[] values=new float[9];c.getMatrix().getValues(values);float displayScale=Math.max(.01f,(float)Math.hypot(values[Matrix.MSCALE_X],values[Matrix.MSKEW_Y]));
        int width=Math.max(1,Math.round(bounds.width()*displayScale)),height=Math.max(1,Math.round((bounds.height()+40)*displayScale));
        Bitmap image=ready;
        if(image!=null&&width==readyWidth&&height==readyHeight){
            float x=Math.round(bounds.left*displayScale)/displayScale,y=Math.round(bounds.top*displayScale)/displayScale;
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);c.drawBitmap(image,null,new RectF(x,y,x+width/displayScale,y+height/displayScale),p);
        }else{
            render(c,bounds);
            if(!pending&&!closed){pending=true;final float scale=displayScale;
                worker.execute(()->{
                    Bitmap high=null;
                    try{
                        // Rasterize at 3x the actual panel pixels, then reduce once with alpha-aware Lanczos.
                        high=Bitmap.createBitmap(width*3,height*3,Bitmap.Config.ARGB_8888);Canvas source=new Canvas(high);source.scale(scale*3,scale*3);
                        render(source,new RectF(0,0,width/scale,height/scale-40));Bitmap raster=ShieldRasterizer.reduce(high,Math.max(width,height));
                        if(!closed){readyWidth=width;readyHeight=height;ready=raster;}
                    }finally{if(high!=null)high.recycle();pending=false;main.post(()->{if(!closed)changed.run();});}
                });
            }
        }
    }
    private void render(Canvas c,RectF bounds){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setDither(true);p.setSubpixelText(true);p.setTypeface(bold);
        float sx=bounds.width()/1780f,sy=bounds.height()/1140f;
        float ox=bounds.centerX()-960*sx,oy=bounds.centerY()-540*sy;
        Matrix matrix=new Matrix();matrix.setScale(sx,sy);matrix.postTranslate(ox,oy);Path target=new Path();
        p.setStyle(Paint.Style.STROKE);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);
        for(Path path:sectors){path.transform(matrix,target);p.setColor(0xFFCED4DE);p.setStrokeWidth(11.5f);c.drawPath(target,p);}
        for(Path path:sectors){path.transform(matrix,target);p.setColor(0xFF151519);p.setStrokeWidth(8.5f);c.drawPath(target,p);}
        for(int i=0;i<3;i++){sectors[i].transform(matrix,target);p.setColor(COLORS[i]);p.setStrokeWidth(3.2f);c.drawPath(target,p);}
        p.setStrokeCap(Paint.Cap.BUTT);p.setStyle(Paint.Style.FILL);p.setTextSize(18);
        for(int i=0;i<TURNS.length;i++){
            float x=ox+TURNS[i][0]*sx,y=oy+TURNS[i][1]*sy;
            p.setColor(0xFF151519);c.drawCircle(x,y,12.5f,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.9f);p.setColor(0xFF8993A2);c.drawCircle(x,y,12.5f,p);p.setStyle(Paint.Style.FILL);
            String number=String.valueOf(i+1);p.setColor(0xFFF4F4F6);c.drawText(number,x-p.measureText(number)/2,y-(p.ascent()+p.descent())/2,p);
        }
        float fx=ox+1722*sx,fy=oy+254*sy;
        for(int row=0;row<3;row++)for(int col=0;col<4;col++){p.setColor((row+col)%2==0?Color.WHITE:Color.BLACK);c.drawRect(fx+8+col*4,fy-12+row*4,fx+12+col*4,fy-8+row*4,p);}
        p.setColor(0xFFF4F4F6);p.setStrokeWidth(1.5f);c.drawLine(fx+8,fy-12,fx+8,fy+8,p);
        p.setTextSize(15);
        for(int i=0;i<3;i++){float x=bounds.centerX()-150+i*115;p.setColor(COLORS[i]);c.drawCircle(x,bounds.bottom+21,4,p);p.setColor(0xFFB5B5C0);c.drawText("SETOR "+(i+1),x+12,bounds.bottom+26,p);}
    }
    void close(){closed=true;worker.shutdownNow();ready=null;}
}
