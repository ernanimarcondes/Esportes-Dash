package io.freemytv;
public final class StandingsRotationCheck {
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
 static void sequence(int[] counts,int[][] expected){
  for(int slot=0;slot<expected.length;slot++){
   for(long time:new long[]{slot*15000L,slot*15000L+14999}){
    StandingsRotation.Frame f=StandingsRotation.at(time,counts);
    check(f.table==expected[slot][0]&&f.page==expected[slot][1],"Wrong conference/page at "+time);
   }
  }
  StandingsRotation.Frame wrap=StandingsRotation.at(expected.length*15000L,counts);check(wrap.table==0&&wrap.page==0,"Cycle must restart");
 }
 public static void main(String[] args){
  check(StandingsRotation.ROWS_PER_PAGE==8,"Eight teams per page");
  sequence(new int[]{8,16},new int[][]{{0,0},{1,0},{1,1}});
  sequence(new int[]{15,15},new int[][]{{0,0},{0,1},{1,0},{1,1}}); // NBA
  sequence(new int[]{16,16},new int[][]{{0,0},{0,1},{1,0},{1,1}}); // NFL
  sequence(new int[]{16,16,15,15},new int[][]{{0,0},{0,1},{1,0},{1,1},{2,0},{2,1},{3,0},{3,1}}); // NHL then MLB
  sequence(new int[]{17,9,8,0},new int[][]{{0,0},{0,1},{0,2},{1,0},{1,1},{2,0},{3,0}});
  StandingsRotation.Frame empty=StandingsRotation.at(100000);check(empty.table==0&&empty.page==0,"Empty input");
  System.out.println("PASS: NBA/NFL and shared NHL/MLB complete every conference before switching; eight-team pages, 15-second boundaries, uneven sizes, empty tables and cycle wrap.");
 }
}
