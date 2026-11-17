package io.freemytv;

/** Automatic rotation while the results page is visible; GPs follow the calendar and newly published results join the end. */
final class F1ResultsRotation {
    static final long INTERVAL=30000;
    int index;private long nextAt;
    void enter(long now){index=0;restart(now);}
    void restart(long now){nextAt=now+INTERVAL;}
    void tick(int count,long now){if(count<=0)return;index=Math.min(index,count-1);if(now>=nextAt){index=(index+1)%count;restart(now);}}
    void refresh(F1RaceResults previous,F1RaceResults current,long now){
        int round=previous!=null&&!previous.races.isEmpty()?previous.races.get(Math.min(index,previous.races.size()-1)).round:-1;
        index=0;
        if(previous!=null&&current!=null&&current.season.equals(previous.season))for(int i=0;i<current.races.size();i++)if(current.races.get(i).round==round){index=i;break;}
        restart(now);
    }
}
