package io.freemytv;

/** Static home panels need a timed repaint only at their five-second boundaries or clock changes. */
final class SportsRenderCadence {
    private long lastBucket=-1,lastMinute=-1;
    private boolean initialized,lastDetails;
    void reset(){initialized=false;}
    boolean due(long elapsed,long wallTime,boolean details){
        long bucket=elapsed/(details?1000:5000),minute=wallTime/60000;
        if(initialized&&bucket==lastBucket&&minute==lastMinute&&details==lastDetails)return false;
        initialized=true;lastBucket=bucket;lastMinute=minute;lastDetails=details;return true;
    }
}
