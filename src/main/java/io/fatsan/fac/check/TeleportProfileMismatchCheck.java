package io.fatsan.fac.check;

import io.fatsan.fac.model.*;
public final class TeleportProfileMismatchCheck extends AbstractBufferedCheck {
  public TeleportProfileMismatchCheck(int limit){super(limit);} @Override public String name(){return "TeleportProfileMismatch";} @Override public CheckCategory category(){return CheckCategory.PROTOCOL;}
  @Override public CheckResult evaluate(NormalizedEvent event){ if(!(event instanceof TeleportSignal t)) return CheckResult.clean(name(),category());
    boolean tr=(t.nanoTime()%1_000_000L)==0L; if(tr){int b=incrementBuffer(t.playerId()); if(overLimit(b)) return new CheckResult(true,name(),category(),"teleport profile mismatch",Math.min(1D,b/7D),false);} else coolDown(t.playerId()); return CheckResult.clean(name(),category()); }
}
