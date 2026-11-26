package io.freemytv;

/** Configurable page rotation; ordinary schedule pages keep their twenty-second interval. */
final class DayGamesRotation {
    static final long INTERVAL=20000L;
    private final long interval;
    DayGamesRotation(){this(INTERVAL);}
    DayGamesRotation(long interval){if(interval<=0)throw new IllegalArgumentException("Positive interval required");this.interval=interval;}
    int page;
    private int knownPages=-1;
    private long nextAt;
    void enter(long now){page=0;knownPages=-1;nextAt=now+interval;}
    void tick(int count,long now){tick(count,now,DayGames.PER_PAGE);}
    void tick(int count,long now,int perPage){
        int pages=DayGames.pages(count,perPage);
        if(pages!=knownPages){page=DayGames.clamp(page,count,perPage);knownPages=pages;nextAt=now+interval;return;}
        if(pages>1&&now>=nextAt){page=(page+1)%pages;nextAt=now+interval;}
    }
}
