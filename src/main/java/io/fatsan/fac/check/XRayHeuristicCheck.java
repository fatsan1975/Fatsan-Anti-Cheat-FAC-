package io.fatsan.fac.check;

import io.fatsan.fac.model.BlockBreakContextSignal;
import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.NormalizedEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayDeque;

/**
 * Detects X-ray / ore-finder by tracking the ratio of valuable ores in recent
 * block breaks.
 *
 * <p><b>Cheat behaviour:</b> X-ray texture packs or cheats reveal hidden ores,
 * allowing players to mine directly to them.  This produces an abnormally high
 * ratio of valuable ore blocks in their break history compared to the stone and
 * dirt that a legitimate miner would also break while tunnelling.
 *
 * <p><b>Detection method:</b> We maintain a rolling window of the last
 * {@value #WINDOW_SIZE} broken block types.  If the proportion of "valuable"
 * blocks (diamonds, emeralds, ancient debris, etc.) exceeds
 * {@value #ORE_RATIO_THRESHOLD}, flag.
 *
 * <p><b>Valuable block set:</b> Diamond ore, deepslate diamond ore, emerald ore,
 * deepslate emerald ore, ancient debris, nether gold ore, gold ore, deepslate
 * gold ore, netherite scrap, netherite block.
 *
 * <p><b>Alert-only:</b> X-ray detection has high false-positive risk for players
 * who are cave-mining (caves expose ores directly).  This check is a heuristic
 * hint only, not a punish trigger.
 */
public final class XRayHeuristicCheck extends AbstractWindowCheck {

  private static final int WINDOW_SIZE = 20;
  private static final double ORE_RATIO_THRESHOLD = 0.40; // 40% valuable blocks

  private final Map<String, ArrayDeque<Boolean>> oreHistory = new ConcurrentHashMap<>();

  public XRayHeuristicCheck(int limit) {
    super(limit, WINDOW_SIZE);
  }

  @Override
  public String name() {
    return "XRayHeuristic";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.WORLD;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof BlockBreakContextSignal ctx)) {
      return CheckResult.clean(name(), category());
    }

    boolean isValuable = isValuableOre(ctx.blockMaterialName());

    ArrayDeque<Boolean> history = oreHistory.computeIfAbsent(
        ctx.playerId(), k -> new ArrayDeque<>(WINDOW_SIZE + 1));
    history.addLast(isValuable);
    if (history.size() > WINDOW_SIZE) {
      history.pollFirst();
    }

    if (history.size() < WINDOW_SIZE) {
      return CheckResult.clean(name(), category());
    }

    long oreCount = history.stream().filter(Boolean::booleanValue).count();
    double ratio = (double) oreCount / history.size();

    if (ratio >= ORE_RATIO_THRESHOLD) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "X-ray: ore ratio=" + String.format("%.1f%%", ratio * 100)
                + " in last " + WINDOW_SIZE + " breaks buf=" + buf,
            Math.min(0.8, ratio),
            false); // alert-only
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }

  private static boolean isValuableOre(String materialName) {
    if (materialName == null) return false;
    String m = materialName.toUpperCase();
    return m.equals("DIAMOND_ORE")
        || m.equals("DEEPSLATE_DIAMOND_ORE")
        || m.equals("EMERALD_ORE")
        || m.equals("DEEPSLATE_EMERALD_ORE")
        || m.equals("ANCIENT_DEBRIS")
        || m.equals("GOLD_ORE")
        || m.equals("DEEPSLATE_GOLD_ORE")
        || m.equals("NETHER_GOLD_ORE");
  }

  @Override
  public void onPlayerQuit(String playerId) {
    super.onPlayerQuit(playerId);
    oreHistory.remove(playerId);
  }
}
