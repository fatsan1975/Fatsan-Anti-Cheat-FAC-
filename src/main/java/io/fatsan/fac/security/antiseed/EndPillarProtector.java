package io.fatsan.fac.security.antiseed;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Replaces the bedrock cap at the centre column of each End pillar with
 * obsidian. SeedCrackerX's {@code BedrockMarkerFinder} specifically scans for
 * bedrock at known pillar X/Z positions to measure heights. With no bedrock,
 * the finder collects zero pillar data.
 *
 * <p>Modification is done once per End dimension and persisted via chunk PDC so
 * it survives restarts. Dragon respawns regenerate vanilla pillars — this is
 * acceptable because protection re-triggers on the next player world-change.
 */
public final class EndPillarProtector implements Listener {

  private static final int PILLAR_COUNT = 10;
  private static final int[][] PILLAR_XZ = new int[PILLAR_COUNT][2];

  static {
    for (int i = 0; i < PILLAR_COUNT; i++) {
      double angle = 2.0 * (-Math.PI + (Math.PI / 10.0) * i);
      PILLAR_XZ[i][0] = (int) (42.0 * Math.cos(angle));
      PILLAR_XZ[i][1] = (int) (42.0 * Math.sin(angle));
    }
  }

  private final JavaPlugin plugin;
  private final NamespacedKey protectedKey;

  public EndPillarProtector(JavaPlugin plugin) {
    this.plugin = plugin;
    this.protectedKey = new NamespacedKey(plugin, "asc_end_pillars");
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldChange(PlayerChangedWorldEvent event) {
    World world = event.getPlayer().getWorld();
    if (world.getEnvironment() != World.Environment.THE_END) {
      return;
    }

    int pivotCX = PILLAR_XZ[0][0] >> 4;
    int pivotCZ = PILLAR_XZ[0][1] >> 4;

    plugin.getServer().getRegionScheduler().execute(plugin, world, pivotCX, pivotCZ, () -> {
      Chunk pivot = world.getChunkAt(pivotCX, pivotCZ);
      if (pivot.getPersistentDataContainer().has(protectedKey, PersistentDataType.BYTE)) {
        return;
      }

      for (int i = 0; i < PILLAR_COUNT; i++) {
        int px = PILLAR_XZ[i][0];
        int pz = PILLAR_XZ[i][1];
        int cx = px >> 4;
        int cz = pz >> 4;

        if (cx == pivotCX && cz == pivotCZ) {
          removeCenterBedrock(world, px, pz);
        } else {
          plugin.getServer().getRegionScheduler().execute(
              plugin, world, cx, cz, () -> removeCenterBedrock(world, px, pz));
        }
      }

      pivot.getPersistentDataContainer().set(protectedKey, PersistentDataType.BYTE, (byte) 1);
    });
  }

  private static void removeCenterBedrock(World world, int x, int z) {
    for (int y = 110; y >= 60; y--) {
      Block block = world.getBlockAt(x, y, z);
      if (block.getType() == Material.BEDROCK) {
        block.setType(Material.OBSIDIAN, false);
        return;
      }
    }
  }
}
