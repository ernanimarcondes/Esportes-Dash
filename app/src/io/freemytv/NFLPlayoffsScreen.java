package io.freemytv;

import android.graphics.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Native-resolution mirrored bracket. Wild Card feeds are not drawn as fixed paths because NFL reseeds. */
final class NFLPlayoffsScreen {
    private static final int WHITE=0xFFF0F4F8,MUTED=0xFFAEB9C6,PANEL=0xFF121923,BORDER=0xFF455466,RED=0xFFFF5361,BLUE=0xFF6AAEFF;
    private final SportsData sports;private final Typeface normal,bold;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final float[] left={48,350,612,840,1102,1330,1592},width={280,240,206,240,206,240,280};
    private final SimpleDateFormat time=new SimpleDateFormat("EEE dd/MM · HH:mm",new Locale("pt","BR"));
    private NFLPlayoffs.Board board;private long version=-1;
    private Bitmap trophyImage,trophyRaster;private int trophyPixels;
    private final Bitmap[] conferenceImages=new Bitmap[2],conferenceRasters=new Bitmap[2];private int conferencePixels;
    void loadTrophy(android.content.Context context){try(java.io.InputStream in=context.getAssets().open("sports/nfl/super-bowl.png")){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;Bitmap source=BitmapFactory.decodeStream(in,null,options);
        // Display only the trophy portion of the generic mark; keep the source asset intact.
        if(source!=null)trophyImage=Bitmap.createBitmap(source,Math.round(source.getWidth()*.30f),0,Math.round(source.getWidth()*.47f),Math.round(source.getHeight()*.88f));
    }catch(Exception ignored){}
        String[] names={"afc","nfc"};for(int i=0;i<2;i++)try(java.io.InputStream in=context.getAssets().open("sports/nfl/"+names[i]+".png")){BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;conferenceImages[i]=BitmapFactory.decodeStream(in,null,options);}catch(Exception ignored){}
    }
    NFLPlayoffsScreen(SportsData sports,Typeface normal,Typeface bold){this.sports=sports;this.normal=normal;this.bold=bold;time.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));}
    void draw(Canvas c){
        if(board==null||version!=sports.modelVersion()){board=new NFLPlayoffs.Board(sports.nflPlayoffs(),sports.tables("nfl"));version=sports.modelVersion();}
        boolean confirmed=!board.provisional&&!board.afc.seed(1).id.isEmpty()&&!board.nfc.seed(1).id.isEmpty();
        for(NFLPlayoffs.Match m:board.afc.wild)confirmed&=m.game!=null;
        for(NFLPlayoffs.Match m:board.nfc.wild)confirmed&=m.game!=null;
        if(!confirmed)center(c,"Panorama provisório",960,284,24,MUTED,false);
        conferenceHeading(c,0);conferenceHeading(c,1);
        bye(c,board.afc,left[1],width[1],RED);bye(c,board.nfc,left[5],width[5],BLUE);
        String[] headings={"WILD CARD","DIVISIONAL","FINAL AFC","","FINAL NFC","DIVISIONAL","WILD CARD"};
        for(int i=0;i<7;i++){
            RectF column=new RectF(left[i],316,left[i]+width[i],1018);fill(c,column,0xFF0A0E13,12);border(c,column,BORDER,1.5f,12);
            if(i!=3)fill(c,new RectF(left[i]+1,317,left[i]+width[i]-1,366),i<3?0xFF321E28:0xFF172C43,10);
            center(c,headings[i],left[i]+width[i]/2,348,i==2||i==4?22:24,i<3?RED:i>3?BLUE:WHITE,true);
        }
        connect(c,590,818,840);connect(c,1330,1102,1080);
        for(int i=0;i<3;i++){
            match(c,board.afc.wild.get(i),new RectF(left[0]+9,386+i*204,left[0]+width[0]-9,564+i*204),RED);
            match(c,board.nfc.wild.get(i),new RectF(left[6]+9,386+i*204,left[6]+width[6]-9,564+i*204),BLUE);
        }
        for(int i=0;i<2;i++){
            match(c,board.afc.divisional.get(i),new RectF(left[1]+9,490+i*280,left[1]+width[1]-9,662+i*280),RED);
            match(c,board.nfc.divisional.get(i),new RectF(left[5]+9,490+i*280,left[5]+width[5]-9,662+i*280),BLUE);
        }
        match(c,board.afc.conference,new RectF(left[2]+8,610,left[2]+width[2]-8,866),RED,true);
        match(c,board.nfc.conference,new RectF(left[4]+8,610,left[4]+width[4]-8,866),BLUE,true);
        trophy(c,960,PlayoffTrophyLayout.CENTER_Y);
        match(c,board.superBowl,new RectF(left[3]+8,PlayoffTrophyLayout.FINAL_TOP,left[3]+width[3]-8,PlayoffTrophyLayout.FINAL_BOTTOM),WHITE,true);
        NFLPlayoffs.Team champion=board.superBowl.winner();
        if(champion!=null)center(c,"CAMPEÃO",960,PlayoffTrophyLayout.CAPTION_BASELINE,23,RED,true);
        if(!board.warning.isEmpty())center(c,board.warning,960,1000,15,MUTED,false);
    }
    private void bye(Canvas c,NFLPlayoffs.Side side,float x,float w,int accent){
        RectF b=new RectF(x,251,x+w,301);fill(c,b,PANEL,9);border(c,b,accent,1.5f,9);NFLPlayoffs.Team team=side.seed(1);
        logo(c,team,new RectF(x+9,259,x+43,293));text(c,fit("#1 · "+team.name,20,w-61),x+52,273,20,WHITE,true);
        text(c,board.provisional?"Folga provisória":"Folga no Wild Card",x+52,293,15,MUTED,false);
    }
    private void conferenceHeading(Canvas c,int index){
        float cy=274;boolean afc=index==0;
        if(afc)text(c,"AFC",146,baseline(cy,48,true),48,RED,true);
        else right(c,"NFC",1774,baseline(cy,48,true),48,BLUE,true);
        Bitmap source=conferenceImages[index];if(source==null)return;
        float[] matrix=new float[9];c.getMatrix().getValues(matrix);float scale=Math.abs(matrix[Matrix.MSCALE_X]);
        float ratio=Math.min(68f/source.getWidth(),52f/source.getHeight()),width=source.getWidth()*ratio,height=source.getHeight()*ratio;
        int pixels=Math.max(8,Math.round(Math.max(width,height)*scale));
        if(conferencePixels!=pixels){conferenceRasters[0]=conferenceRasters[1]=null;conferencePixels=pixels;}
        if(conferenceRasters[index]==null)conferenceRasters[index]=ShieldRasterizer.reduce(source,pixels);
        float cx=afc?90:1830;p.setColor(Color.WHITE);p.setFilterBitmap(true);
        c.drawBitmap(conferenceRasters[index],null,new RectF(cx-width/2,cy-height/2,cx+width/2,cy+height/2),p);
    }
    private void connect(Canvas c,float divEdge,float finalEdge,float centerEdge){
        p.setColor(BORDER);p.setStrokeWidth(2);float direction=finalEdge>divEdge?1:-1,middle=divEdge+direction*12;
        c.drawLine(divEdge,576,middle,576,p);c.drawLine(divEdge,856,middle,856,p);c.drawLine(middle,576,middle,856,p);c.drawLine(middle,738,finalEdge-direction*206,738,p);
        c.drawLine(finalEdge,738,centerEdge,738,p);
    }
    private void match(Canvas c,NFLPlayoffs.Match m,RectF b,int accent){
        match(c,m,b,accent,false);
    }
    private void match(Canvas c,NFLPlayoffs.Match m,RectF b,int accent,boolean shieldsOnly){
        fill(c,b,PANEL,10);border(c,b,BORDER,1.3f,10);SportsFeedParser.Game g=m.game;
        String heading=g==null?m.hint:g.at>0&&g.timeValid?time.format(new Date(g.at)):"Horário a confirmar";
        boolean wrapped=g==null&&heading.startsWith("Aguardando ");
        if(wrapped){
            center(c,"Aguardando",b.centerX(),b.top+26,17,MUTED,false);
            center(c,heading.substring(11),b.centerX(),b.top+47,17,MUTED,false);
        }else{
            float font=17;p.setTypeface(normal);p.setTextSize(font);
            while(font>13&&p.measureText(heading)>b.width()-24){font-=1;p.setTextSize(font);}
            center(c,fit(heading,font,b.width()-24,false),b.centerX(),b.top+27,font,MUTED,false);
        }
        float contentTop=b.top+(wrapped?64:46),contentBottom=b.bottom-(g==null?14:35),rowHeight=(contentBottom-contentTop)/2;
        float middle=contentTop+rowHeight;NFLPlayoffs.Team winner=m.winner();
        matchTeam(c,m.home,b,contentTop+rowHeight/2,rowHeight,g==null?"":g.homeScore,winner!=null&&winner.id.equals(m.home.id),accent,shieldsOnly);
        p.setColor(0xFF253140);p.setStrokeWidth(1);c.drawLine(b.left+15,middle,b.right-15,middle,p);
        matchTeam(c,m.away,b,middle+rowHeight/2,rowHeight,g==null?"":g.awayScore,winner!=null&&winner.id.equals(m.away.id),accent,shieldsOnly);
        if(g!=null){String status=g.complete?"ENCERRADO":g.state.equals("in")?"EM ANDAMENTO":"A SEGUIR";center(c,status,b.centerX(),b.bottom-12,15,g.state.equals("in")&&!g.complete?accent:MUTED,false);}
    }
    private void matchTeam(Canvas c,NFLPlayoffs.Team t,RectF b,float cy,float rowHeight,String score,boolean winner,int accent,boolean shieldsOnly){
        if(!shieldsOnly){team(c,t,b,cy,score,winner,accent);return;}
        float size=Math.min(rowHeight-18,82),cx=b.centerX()-(score.isEmpty()?0:20);
        logo(c,t,new RectF(cx-size/2,cy-size/2,cx+size/2,cy+size/2));
        if(!score.isEmpty())right(c,score,b.right-15,baseline(cy,30,true),30,winner?accent:WHITE,true);
    }
    private void team(Canvas c,NFLPlayoffs.Team t,RectF b,float cy,String score,boolean winner,int accent){
        boolean compact=b.width()<200;float icon=compact?28:38,logoX=b.left+(compact?26:31),nameX=logoX+icon+(compact?10:15);
        float nameWidth=b.right-12-nameX-(score.isEmpty()?0:36),size=compact?22:24;
        if(t.seed>0)text(c,String.valueOf(t.seed),b.left+11,baseline(cy,15,false),15,MUTED,false);
        logo(c,t,new RectF(logoX,cy-icon/2,logoX+icon,cy+icon/2));
        String name=t.name;
        p.setTypeface(bold);p.setTextSize(size);
        if(p.measureText(name)>nameWidth&&!t.abbr.isEmpty()&&nameWidth<90)name=t.abbr;
        if(t.id.isEmpty()&&name.startsWith("Campeão ")){
            text(c,"Campeão",nameX,baseline(cy-12,22,true),22,WHITE,true);
            text(c,name.substring(8),nameX,baseline(cy+13,22,true),22,WHITE,true);
        }else text(c,fit(name,size,nameWidth),nameX,baseline(cy,size,true),size,winner?accent:WHITE,true);
        if(!score.isEmpty())right(c,score,b.right-11,baseline(cy,26,true),26,winner?accent:WHITE,true);
    }
    private void logo(Canvas c,NFLPlayoffs.Team team,RectF b){
        if(team.logo.isEmpty()){
            p.setColor(BORDER);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);Path shield=new Path();shield.moveTo(b.left+3,b.top+2);shield.lineTo(b.centerX(),b.top+5);shield.lineTo(b.right-3,b.top+2);shield.lineTo(b.right-5,b.centerY()+5);shield.quadTo(b.centerX(),b.bottom,b.left+5,b.centerY()+5);shield.close();c.drawPath(shield,p);p.setStyle(Paint.Style.FILL);return;
        }
        float[] matrix=new float[9];c.getMatrix().getValues(matrix);float scale=(float)Math.hypot(matrix[Matrix.MSCALE_X],matrix[Matrix.MSKEW_Y]);
        Bitmap image=sports.logo(team.logo,Math.max(8,Math.round(b.width()*scale)));if(image==null){center(c,team.abbr,b.centerX(),cy(b)+5,12,MUTED,true);return;}
        float x=(Math.round(b.centerX()*scale+matrix[Matrix.MTRANS_X]-image.getWidth()/2f)-matrix[Matrix.MTRANS_X])/scale;
        float y=(Math.round(b.centerY()*scale+matrix[Matrix.MTRANS_Y]-image.getHeight()/2f)-matrix[Matrix.MTRANS_Y])/scale;
        p.setColor(Color.WHITE);p.setFilterBitmap(true);c.drawBitmap(image,null,new RectF(x,y,x+image.getWidth()/scale,y+image.getHeight()/scale),p);
    }
    private float cy(RectF b){return b.centerY();}
    private void trophy(Canvas c,float cx,float top){
        if(trophyImage==null)return;
        float[] matrix=new float[9];c.getMatrix().getValues(matrix);float physicalScale=Math.abs(matrix[Matrix.MSCALE_X]);
        float scale=Math.min(PlayoffTrophyLayout.WIDTH/trophyImage.getWidth(),PlayoffTrophyLayout.HEIGHT/trophyImage.getHeight());
        float width=trophyImage.getWidth()*scale,height=trophyImage.getHeight()*scale;
        int pixels=Math.max(16,Math.round(Math.max(width,height)*physicalScale));
        if(trophyRaster==null||pixels!=trophyPixels){trophyRaster=ShieldRasterizer.reduce(trophyImage,pixels);trophyPixels=pixels;}
        p.setColor(Color.WHITE);p.setFilterBitmap(true);p.setShader(null);
        float x=(Math.round(cx*physicalScale+matrix[Matrix.MTRANS_X]-trophyRaster.getWidth()/2f)-matrix[Matrix.MTRANS_X])/physicalScale;
        float y=(Math.round(top*physicalScale+matrix[Matrix.MTRANS_Y]-trophyRaster.getHeight()/2f)-matrix[Matrix.MTRANS_Y])/physicalScale;
        c.drawBitmap(trophyRaster,null,new RectF(x,y,x+trophyRaster.getWidth()/physicalScale,y+trophyRaster.getHeight()/physicalScale),p);
    }
    private float baseline(float cy,float size,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);Paint.FontMetrics m=p.getFontMetrics();return cy-(m.ascent+m.descent)/2;}
    private void fill(Canvas c,RectF b,int color,float r){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(b,r,r,p);}
    private void border(Canvas c,RectF b,int color,float width,float r){p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(width);c.drawRoundRect(b,r,r,p);p.setStyle(Paint.Style.FILL);}
    private void text(Canvas c,String value,float x,float y,float size,int color,boolean strong){p.setStyle(Paint.Style.FILL);p.setTypeface(strong?bold:normal);p.setTextSize(size);p.setColor(color);c.drawText(value,x,y,p);}
    private void center(Canvas c,String s,float x,float y,float size,int color,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);text(c,s,x-p.measureText(s)/2,y,size,color,strong);}
    private void right(Canvas c,String s,float x,float y,float size,int color,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);text(c,s,x-p.measureText(s),y,size,color,strong);}
    private String fit(String s,float size,float width){return fit(s,size,width,true);}
    private String fit(String s,float size,float width,boolean strong){p.setTypeface(strong?bold:normal);p.setTextSize(size);if(p.measureText(s)<=width)return s;int count=p.breakText(s,true,Math.max(0,width-p.measureText("…")),null);return s.substring(0,count)+"…";}
}
