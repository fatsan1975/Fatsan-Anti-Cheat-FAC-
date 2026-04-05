package io.fatsan.fac.check;

import io.fatsan.fac.model.CheckCategory;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.model.CombatContextSignal;
import io.fatsan.fac.model.NormalizedEvent;

/**
 * Detects attacks performed without an arm-swing packet.
 *
 * <p><b>Cheat behaviour:</b> Most killaura, aimbot, and reach-extended cheats
 * inject raw {@code ServerboundInteractPacket} (attack) packets directly,
 * bypassing the {@code ServerboundSwingPacket} that vanilla clients always send
 * immediately before an attack.  The absence of a swing is a strong signal that
 * the attack did not originate from legitimate client input.
 *
 * <p><b>Signal:</b> {@link CombatContextSignal#swingPacketSent()} is set to
 * {@code false} if no {@code PlayerAnimateEvent} (swing) was observed between
 * the previous hit and this one.  The flag is tracked per-player in
 * {@code BukkitSignalBridge.pendingSwing}.
 *
 * <p><b>False-positive mitigations:</b>
 * <ul>
 *   <li>Buffer of 4 — a single missed swing (lag spike) does not flag.</li>
 *   <li>Via/GeyserMC clients occasionally batch or reorder packets; the buffer
 *       absorbs one or two anomalous events.</li>
 * </ul>
 *
 * <p><b>Folia thread safety:</b> {@code CombatContextSignal} is constructed on
 * the region thread in {@code BukkitSignalBridge.onHit()} and passed
 * immutably to the check thread pool.  All state in this check is per-player
 * in {@code AbstractBufferedCheck.buffers} which uses {@code ConcurrentHashMap}.
 */
public final class NoSwingCheck extends AbstractBufferedCheck {

  /**
   * @param limit  escalation buffer limit — recommend 4 to absorb lag-induced
   *               packet reordering
   */
  public NoSwingCheck(int limit) {
    super(limit);
  }

  @Override
  public String name() {
    return "NoSwing";
  }

  @Override
  public CheckCategory category() {
    return CheckCategory.COMBAT;
  }

  @Override
  public CheckResult evaluate(NormalizedEvent event) {
    if (!(event instanceof CombatContextSignal ctx)) {
      return CheckResult.clean(name(), category());
    }

    if (!ctx.swingPacketSent()) {
      int buf = incrementBuffer(ctx.playerId());
      if (overLimit(buf)) {
        return new CheckResult(
            true,
            name(),
            category(),
            "Attack without arm-swing packet (buf=" + buf + ")",
            Math.min(1.0, buf / 6.0),
            true);
      }
    } else {
      coolDown(ctx.playerId());
    }
    return CheckResult.clean(name(), category());
  }
}
