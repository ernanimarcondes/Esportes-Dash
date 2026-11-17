package io.freemytv;

/** Consecutive driver pages are completed before constructors and the next GP cycle. */
final class F1Rotation {
    static final int PROGRAM=0,DRIVERS=1,CONSTRUCTORS=2;
    static final int DRIVERS_PER_PAGE=10;
    static final class Frame {
        final int phase,page;
        Frame(int phase,int page){this.phase=phase;this.page=page;}
    }
    static Frame at(long elapsed,int driverCount){
        int pages=Math.max(1,(Math.max(0,driverCount)+DRIVERS_PER_PAGE-1)/DRIVERS_PER_PAGE);
        long driversDuration=pages*15000L,cycle=30000L+driversDuration+15000L;
        long offset=Math.floorMod(elapsed,cycle);
        if(offset<30000L)return new Frame(PROGRAM,0);
        if(offset<30000L+driversDuration)return new Frame(DRIVERS,(int)((offset-30000L)/15000L));
        return new Frame(CONSTRUCTORS,0);
    }
}
