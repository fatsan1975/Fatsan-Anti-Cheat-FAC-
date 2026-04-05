package io.fatsan.fac.security.antiseed;

import java.util.logging.Logger;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Orchestrates all anti-seed-cracker protection layers:
 *
 * <ol>
 *   <li><b>Hashed Seed Spoof</b> — replaces the hashed seed in JOIN_GAME /
 *       RESPAWN packets with a random value (requires PacketEvents).</li>
 *   <li><b>End Pillar Protection</b> — converts bedrock pillar caps to
 *       obsidian so BedrockMarkerFinder finds nothing.</li>
 *   <li><b>Structure Scramble</b> — randomises dungeon floor patterns and
 *       buried-treasure holder blocks to invalidate structure-based data
 *       collection.</li>
 * </ol>
 */
public final class AntiSeedCrackerService {

  private final JavaPlugin plugin;
  private final Logger logger;
  private final boolean hashedSeedSpoof;
  private final boolean endPillarProtection;
  private final boolean structureScramble;

  private EndPillarProtector endPillarProtector;
  private ChunkFeatureScrambler chunkScrambler;

  public AntiSeedCrackerService(JavaPlugin plugin,
                                boolean hashedSeedSpoof,
                                boolean endPillarProtection,
                                boolean structureScramble) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
    this.hashedSeedSpoof = hashedSeedSpoof;
    this.endPillarProtection = endPillarProtection;
    this.structureScramble = structureScramble;
  }

  public void start() {
    int layers = 0;

    if (hashedSeedSpoof && initHashedSeedSpoof()) {
      layers++;
    }

    if (endPillarProtection) {
      endPillarProtector = new EndPillarProtector(plugin);
      plugin.getServer().getPluginManager().registerEvents(endPillarProtector, plugin);
      logger.info("[AntiSeedCracker] End pillar protection enabled");
      layers++;
    }

    if (structureScramble) {
      chunkScrambler = new ChunkFeatureScrambler(plugin);
      plugin.getServer().getPluginManager().registerEvents(chunkScrambler, plugin);
      logger.info("[AntiSeedCracker] Structure scramble enabled (dungeon floor + buried treasure)");
      layers++;
    }

    logger.info("[AntiSeedCracker] Active with " + layers + " protection layer(s)");
  }

  public void stop() {
    if (endPillarProtector != null) {
      HandlerList.unregisterAll(endPillarProtector);
      endPillarProtector = null;
    }
    if (chunkScrambler != null) {
      HandlerList.unregisterAll(chunkScrambler);
      chunkScrambler = null;
    }
  }

  private boolean initHashedSeedSpoof() {
    try {
      Class.forName("com.github.retrooper.packetevents.PacketEvents");
    } catch (ClassNotFoundException e) {
      logger.warning("[AntiSeedCracker] PacketEvents not found — hashed seed spoofing DISABLED. "
          + "Install PacketEvents for full anti-seed-cracker protection.");
      return false;
    }
    HashedSeedSpoofRegistrar.register();
    logger.info("[AntiSeedCracker] Hashed seed spoofing enabled (PacketEvents)");
    return true;
  }
}
