package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class BlockBreakItemTypeCadenceCheck extends AbstractBufferedCheck {
  private final Map<String,String> lastType=new ConcurrentHashMap<>(); private final Map<String,Integer> streak=new ConcurrentHashMap<>();
  public BlockBreakItemTypeCadenceCheck(int limit){super(limit);} @Override public String name(){return "BlockBreakItemTypeCadence";} @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof BlockBreakEventSignal b)||b.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category());
    String p=lastType.put(b.playerId(),b.itemTypeKey()); if(p==null) return CheckResult.clean(name(),category()); int s=(p.equals(b.itemTypeKey()) && b.intervalNanos()/1_000_000L<60L)?streak.getOrDefault(b.playerId(),0)+1:0; streak.put(b.playerId(),s);
    if(s>=8){int bf=incrementBuffer(b.playerId()); if(overLimit(bf)) return new CheckResult(true,name(),category(),"item type cadence lock",Math.min(1D,bf/7D),false);} else coolDown(b.playerId()); return CheckResult.clean(name(),category()); }
}
