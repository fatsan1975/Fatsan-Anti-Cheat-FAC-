package io.fatsan.fac.check;

import io.fatsan.fac.model.*; import java.util.Map; import java.util.concurrent.ConcurrentHashMap;
public final class BlockPlaceSprintStopDesyncCheck extends AbstractBufferedCheck {
  private final Map<String,Boolean> lastSprint=new ConcurrentHashMap<>();
  public BlockPlaceSprintStopDesyncCheck(int limit){super(limit);} @Override public String name(){return "BlockPlaceSprintStopDesync";} @Override public CheckCategory category(){return CheckCategory.WORLD;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof BlockPlaceEventSignal p)||p.intervalNanos()==Long.MAX_VALUE) return CheckResult.clean(name(),category()); Boolean ps=lastSprint.put(p.playerId(),p.sprinting());
    boolean t=ps!=null && ps && !p.sprinting() && p.intervalNanos()/1_000_000L<35L; if(t){int b=incrementBuffer(p.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"sprint-stop place desync",Math.min(1D,b/7D),false);} else coolDown(p.playerId()); return CheckResult.clean(name(),category());}
}
