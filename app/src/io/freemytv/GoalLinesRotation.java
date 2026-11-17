package io.freemytv;

import java.util.*;

/** Five seconds per group of scorers; preserve progress when the parent match page is hidden. */
final class GoalLinesRotation {
    static final long INTERVAL=5000L;
    static final int ROWS=4;
    private static final class Cursor {int page,pages;long nextAt,lastSeen;}
    private final Map<String,Cursor> cursors=new HashMap<>();
    void reset(){cursors.clear();}
    int page(String key,int count,long now){return page(key,count,now,ROWS);}
    int page(String key,int count,long now,int rows){
        rows=Math.max(1,rows);int pages=Math.max(1,(count+rows-1)/rows);Cursor cursor=cursors.get(key);
        if(cursor==null){cursor=new Cursor();cursor.pages=pages;cursor.nextAt=now+INTERVAL;cursor.lastSeen=now;cursors.put(key,cursor);}
        if(cursor.pages!=pages){cursor.page=Math.min(cursor.page,pages-1);cursor.pages=pages;cursor.nextAt=now+INTERVAL;}
        else if(now-cursor.lastSeen>1500)cursor.nextAt=now+INTERVAL;
        else if(pages>1&&now>=cursor.nextAt){cursor.page=(cursor.page+1)%pages;cursor.nextAt=now+INTERVAL;}
        cursor.lastSeen=now;return cursor.page;
    }
}
