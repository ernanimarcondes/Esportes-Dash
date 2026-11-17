package io.freemytv;

/** Twenty seconds per visible schedule page; each selected filter starts from its first page. */
final class DayGamesRotation {
    static final long INTERVAL=20000L;
    int page;
    private int knownPages=-1;
    private long nextAt;
    void enter(long now){page=0;knownPages=-1;nextAt=now+INTERVAL;}
    void tick(int count,long now){tick(count,now,DayGames.PER_PAGE);}
    void tick(int count,long now,int perPage){
        int pages=DayGames.pages(count,perPage);
        if(pages!=knownPages){page=DayGames.clamp(page,count,perPage);knownPages=pages;nextAt=now+INTERVAL;return;}
        if(pages>1&&now>=nextAt){page=(page+1)%pages;nextAt=now+INTERVAL;}
    }
}
