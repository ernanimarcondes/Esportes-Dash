package io.freemytv;

import android.content.Context;
import android.graphics.*;
import java.util.*;

/** Mirrored postseason bracket with each league's identity and real series scores. */
final class LeaguePlayoffsScreen {
    private static final int TEXT=0xFFF1F5FA,MUTED=0xFFB1BDCA,EDGE=0xFF485567;
    private final SportsData sports;private final String league;private final Typeface normal,bold;private final Paint p=new Paint(3);
    private final int[] colors;private final float[] xs={48,350,612,840,1102,1330,1592},ws={280,240,206,240,206,240,280};
    private LeaguePlayoffs.Board board;private long version=-1;private Bitmap cup,cupRaster;private int cupPixels;
    private final Bitmap[] conference=new Bitmap[2],conferenceRaster=new Bitmap[2];private final int[] conferenceSize=new int[2];
    LeaguePlayoffsScreen(Context context,SportsData sports,String league,Typeface normal,Typeface bold){this.sports=sports;this.league=league;this.normal=normal;this.bold=bold;
        colors=league.equals("nhl")?new int[]{0xFFE3E9EF,0xFF9FBAD1}:league.equals("mlb")?new int[]{0xFFFF6274,0xFF78ADFF}:new int[]{0xFF69AEFF,0xFFEF557B};
        try(java.io.InputStream in=context.getAssets().open("sports/playoffs/"+league+"-trophy.png")){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inSampleSize=2;Bitmap image=BitmapFactory.decodeStream(in,null,options);cup=crop(image);}catch(Exception ignored){}
        for(int i=0;i<2;i++)try(java.io.InputStream in=context.getAssets().open("sports/playoffs/"+league+"-"+i+".png")){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;options.inSampleSize=2;conference[i]=crop(BitmapFactory.decodeStream(in,null,options));}catch(Exception ignored){}
    }
    void draw(Canvas c){
        if(board==null||version!=sports.modelVersion()){board=new LeaguePlayoffs.Board(league,sports.leaguePlayoffs(league));version=sports.modelVersion();}
        boolean nba=league.equals("nba"),mlb=league.equals("mlb");float top=nba?370:316;
        conferenceHeading(c,0,nba?225:268);conferenceHeading(c,1,nba?225:268);
        if(board.provisional)center(c,"Panorama provisório",960,nba?239:282,24,MUTED,false);
        if(nba)for(int side=0;side<2;side++)for(int i=0;i<3;i++)playIn(c,board.playIn[side].get(i),new RectF((side==0?48:1128)+i*248,263,(side==0?48:1128)+i*248+240,350),colors[side]);
        String first=mlb?"WILD CARD":"1ª RODADA",second=mlb?"DIVISION SERIES":league.equals("nhl")?"2ª RODADA":"SEMIFINAL";
        String[] labels={first,second,"FINAL "+(mlb?"AL":"LESTE"),"","FINAL "+(mlb?"NL":"OESTE"),second,first};
        for(int i=0;i<7;i++){RectF b=new RectF(xs[i],top,xs[i]+ws[i],1018);fill(c,b,league.equals("nhl")?0xFF0D1014:0xFF0C121B,12);border(c,b,EDGE,1.5f);
            if(i!=3){fill(c,new RectF(b.left+1,top+1,b.right-1,top+49),i<3?league.equals("nhl")?0xFF252C33:0xFF172737:league.equals("nhl")?0xFF1C2833:0xFF291E32,10);center(c,labels[i],b.centerX(),top+33,22,colors[i<3?0:1],true);}}
        for(int side=0;side<2;side++){
            int firstCol=side==0?0:6,secondCol=side==0?1:5,finalCol=side==0?2:4;int accent=colors[side];
            if(mlb)byes(c,side,new RectF(xs[firstCol]+10,386,xs[firstCol]+ws[firstCol]-10,464));
            int count=board.first[side].size();for(int i=0;i<count;i++){
                float y=mlb?510+i*270:nba?434+i*143:384+i*158;float h=mlb?190:nba?130:144;
                series(c,board.first[side].get(i),new RectF(xs[firstCol]+9,y,xs[firstCol]+ws[firstCol]-9,y+h),accent,false);
            }
            for(int i=0;i<2;i++)series(c,board.second[side].get(i),new RectF(xs[secondCol]+9,490+i*280,xs[secondCol]+ws[secondCol]-9,662+i*280),accent,false);
            series(c,board.conference[side],new RectF(xs[finalCol]+8,610,xs[finalCol]+ws[finalCol]-8,878),accent,true);
        }
        firstLinks(c,nba,mlb);
        trophy(c);series(c,board.championship,new RectF(848,PlayoffTrophyLayout.FINAL_TOP,1072,PlayoffTrophyLayout.FINAL_BOTTOM),0xFFEAC56B,true);
        if(board.championship.winner()!=null)center(c,"CAMPEÃO",960,PlayoffTrophyLayout.CAPTION_BASELINE,23,0xFFEAC56B,true);
        if(!board.warning.isEmpty())center(c,board.warning,960,1036,16,MUTED,false);
        links(c,590,612,818,840);links(c,1330,1308,1102,1080);
    }
    private void conferenceHeading(Canvas c,int side,float cy){
        String name=LeaguePlayoffs.sideName(league,side).toUpperCase(Locale.ROOT);float font=league.equals("mlb")?30:40;
        if(side==0)text(c,name,145,baseline(cy,font,true),font,colors[side],true);else right(c,name,1774,baseline(cy,font,true),font,colors[side],true);
        Bitmap source=conference[side];if(source==null)return;int size=pixels(c,64);if(conferenceRaster[side]==null||conferenceSize[side]!=size){conferenceRaster[side]=ShieldRasterizer.reduce(source,size);conferenceSize[side]=size;}
        float x=side==0?58:1798;image(c,conferenceRaster[side],new RectF(x,cy-27,x+64,cy+27));
    }
    private void firstLinks(Canvas c,boolean nba,boolean mlb){p.setColor(EDGE);p.setStrokeWidth(1.5f);
        for(int side=0;side<2;side++){float edge=side==0?328:1592,end=side==0?350:1570,elbow=(edge+end)/2;
            for(int i=0;i<2;i++){float target=576+i*280;
                if(mlb){float from=605+i*270;c.drawLine(edge,from,elbow,from,p);c.drawLine(elbow,from,elbow,target,p);c.drawLine(elbow,target,end,target,p);}
                else{float first=nba?499+i*286:456+i*316,second=first+(nba?143:158);c.drawLine(edge,first,elbow,first,p);c.drawLine(edge,second,elbow,second,p);c.drawLine(elbow,first,elbow,second,p);c.drawLine(elbow,target,end,target,p);}
            }
        }
    }
    private void links(Canvas c,float edge,float finalStart,float finalEnd,float middle){p.setColor(EDGE);p.setStrokeWidth(1.5f);float elbow=(edge+finalStart)/2;c.drawLine(edge,576,elbow,576,p);c.drawLine(edge,856,elbow,856,p);c.drawLine(elbow,576,elbow,856,p);c.drawLine(elbow,744,finalStart,744,p);c.drawLine(finalEnd,744,middle,744,p);}
    private void byes(Canvas c,int side,RectF b){center(c,"FOLGA NO WILD CARD",b.centerX(),b.top+15,16,MUTED,false);
        for(int i=0;i<2;i++){LeaguePlayoffs.Team t=board.seed(side,i+1);float x=b.left+12+i*126;badge(c,t,new RectF(x,b.top+31,x+34,b.top+65));text(c,"#"+(i+1)+" "+t.abbr,x+43,b.top+55,19,TEXT,true);}}
    private void playIn(Canvas c,LeaguePlayoffs.Series s,RectF b,int color){fill(c,b,0xFF131D29,9);border(c,b,EDGE,1);text(c,"PLAY-IN · "+s.hint.toUpperCase(new Locale("pt","BR")),b.left+10,b.top+20,16,color,true);
        badge(c,s.a,new RectF(b.left+10,b.top+35,b.left+40,b.top+65));badge(c,s.b,new RectF(b.right-40,b.top+35,b.right-10,b.top+65));
        text(c,s.a.abbr.isEmpty()?"?":s.a.abbr,b.left+46,b.top+57,19,TEXT,true);right(c,s.b.abbr.isEmpty()?"?":s.b.abbr,b.right-46,b.top+57,19,TEXT,true);
        center(c,s.complete?s.winsA+"–"+s.winsB:"×",b.centerX(),b.top+57,18,MUTED,true);
    }
    private void series(Canvas c,LeaguePlayoffs.Series s,RectF b,int color,boolean large){fill(c,b,0xFF141C27,10);border(c,b,EDGE,1.2f);
        String heading=s.official?s.complete?"Série encerrada":"Melhor de "+s.best:s.hint.isEmpty()?"A definir":s.hint;
        boolean wrap=heading.startsWith("Aguardando ");float header=wrap?53:35;
        if(wrap){center(c,"Aguardando",b.centerX(),b.top+23,16,MUTED,false);center(c,heading.substring(11),b.centerX(),b.top+43,16,MUTED,false);}else center(c,heading,b.centerX(),b.top+24,16,MUTED,false);
        float content=b.height()-header-12,cy=b.top+header+content/4;row(c,s.a,s.winsA,s,b,cy,content/2,color,large);
        p.setColor(0xFF293644);p.setStrokeWidth(1);c.drawLine(b.left+13,b.top+header+content/2,b.right-13,b.top+header+content/2,p);
        row(c,s.b,s.winsB,s,b,cy+content/2,content/2,color,large);
    }
    private void row(Canvas c,LeaguePlayoffs.Team t,int wins,LeaguePlayoffs.Series s,RectF b,float cy,float h,int color,boolean large){
        boolean show=s.official;LeaguePlayoffs.Team winner=s.winner();int ink=winner!=null&&winner.id.equals(t.id)?color:TEXT;
        if(large){float size=Math.min(86,h-17),cx=b.centerX()-(show?20:0);badge(c,t,new RectF(cx-size/2,cy-size/2,cx+size/2,cy+size/2));if(show)right(c,String.valueOf(wins),b.right-14,baseline(cy,31,true),31,ink,true);return;}
        float icon=Math.min(36,h-9),nameX=b.left+76,width=b.right-nameX-(show?35:12);if(t.seed<99)text(c,String.valueOf(t.seed),b.left+10,baseline(cy,14,false),14,MUTED,false);
        badge(c,t,new RectF(b.left+27,cy-icon/2,b.left+27+icon,cy+icon/2));String name=t.name;p.setTypeface(bold);p.setTextSize(23);if(p.measureText(name)>width&&!t.abbr.isEmpty())name=t.abbr;
        text(c,fit(name,23,width),nameX,baseline(cy,23,true),23,ink,true);if(show)right(c,String.valueOf(wins),b.right-11,baseline(cy,25,true),25,ink,true);
    }
    private void badge(Canvas c,LeaguePlayoffs.Team team,RectF b){
        if(team.logo.isEmpty()){p.setColor(EDGE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);Path shape=new Path();shape.moveTo(b.left+3,b.top+3);shape.lineTo(b.centerX(),b.top+6);shape.lineTo(b.right-3,b.top+3);shape.lineTo(b.right-6,b.bottom-10);shape.quadTo(b.centerX(),b.bottom+2,b.left+6,b.bottom-10);shape.close();c.drawPath(shape,p);p.setStyle(Paint.Style.FILL);return;}
        Bitmap image=sports.logo(team.logo,pixels(c,b.width()));if(image!=null)image(c,image,b);else center(c,team.abbr,b.centerX(),b.centerY()+5,14,MUTED,true);
    }
    private void trophy(Canvas c){
        if(cup==null)return;
        float[] matrix=new float[9];c.getMatrix().getValues(matrix);float physicalScale=Math.max(.01f,Math.abs(matrix[Matrix.MSCALE_X]));
        float fit=Math.min(PlayoffTrophyLayout.WIDTH/cup.getWidth(),PlayoffTrophyLayout.HEIGHT/cup.getHeight());
        int target=Math.max(1,Math.round(Math.max(cup.getWidth(),cup.getHeight())*fit*physicalScale));
        if(cupRaster==null||cupPixels!=target){cupRaster=ShieldRasterizer.reduce(cup,target);cupPixels=target;}
        float x=(Math.round(960*physicalScale+matrix[Matrix.MTRANS_X]-cupRaster.getWidth()/2f)-matrix[Matrix.MTRANS_X])/physicalScale;
        float y=(Math.round(PlayoffTrophyLayout.CENTER_Y*physicalScale+matrix[Matrix.MTRANS_Y]-cupRaster.getHeight()/2f)-matrix[Matrix.MTRANS_Y])/physicalScale;
        p.setColor(Color.WHITE);p.setFilterBitmap(true);
        c.drawBitmap(cupRaster,null,new RectF(x,y,x+cupRaster.getWidth()/physicalScale,y+cupRaster.getHeight()/physicalScale),p);
    }
    private static Bitmap crop(Bitmap image){if(image==null)return null;int w=image.getWidth(),h=image.getHeight(),l=w,r=-1,t=h,b=-1;int[] a=new int[w*h];image.getPixels(a,0,w,0,0,w,h);for(int y=0;y<h;y++)for(int x=0;x<w;x++)if((a[y*w+x]>>>24)>8){l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);}return r<l?image:Bitmap.createBitmap(image,l,t,r-l+1,b-t+1);}
    private int pixels(Canvas c,float size){float[] m=new float[9];c.getMatrix().getValues(m);return Math.max(1,Math.round(size*Math.abs(m[0])));}
    private void image(Canvas c,Bitmap image,RectF b){float ratio=Math.min(b.width()/image.getWidth(),b.height()/image.getHeight()),w=image.getWidth()*ratio,h=image.getHeight()*ratio;p.setColor(Color.WHITE);p.setFilterBitmap(true);c.drawBitmap(image,null,new RectF(b.centerX()-w/2,b.centerY()-h/2,b.centerX()+w/2,b.centerY()+h/2),p);}
    private float baseline(float cy,float size,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);Paint.FontMetrics f=p.getFontMetrics();return cy-(f.ascent+f.descent)/2;}
    private void fill(Canvas c,RectF b,int color,float radius){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(b,radius,radius,p);}
    private void border(Canvas c,RectF b,int color,float width){p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(width);c.drawRoundRect(b,10,10,p);p.setStyle(Paint.Style.FILL);}
    private void text(Canvas c,String s,float x,float y,float size,int color,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawText(s,x,y,p);}
    private void center(Canvas c,String s,float x,float y,float size,int color,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);text(c,s,x-p.measureText(s)/2,y,size,color,strong);}
    private void right(Canvas c,String s,float x,float y,float size,int color,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);text(c,s,x-p.measureText(s),y,size,color,strong);}
    private String fit(String s,float size,float width){p.setTypeface(bold);p.setTextSize(size);if(p.measureText(s)<=width)return s;int n=p.breakText(s,true,Math.max(0,width-p.measureText("…")),null);return s.substring(0,n)+"…";}
}
