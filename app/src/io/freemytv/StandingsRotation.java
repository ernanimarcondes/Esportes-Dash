package io.freemytv;

/** Complete every page of one conference before advancing to the next conference or league. */
final class StandingsRotation {
    static final long INTERVAL=15000L;
    static final int ROWS_PER_PAGE=8;
    static final class Frame {
        final int table,page;
        Frame(int table,int page){this.table=table;this.page=page;}
    }
    static Frame at(long elapsed,int... counts){
        long pages=0;for(int count:counts)pages+=Math.max(1,(Math.max(0,count)+ROWS_PER_PAGE-1)/ROWS_PER_PAGE);
        if(pages==0)return new Frame(0,0);
        long slot=Math.floorMod(elapsed/INTERVAL,pages);
        for(int i=0;i<counts.length;i++){
            int count=Math.max(1,(Math.max(0,counts[i])+ROWS_PER_PAGE-1)/ROWS_PER_PAGE);
            if(slot<count)return new Frame(i,(int)slot);slot-=count;
        }
        return new Frame(0,0);
    }
}
