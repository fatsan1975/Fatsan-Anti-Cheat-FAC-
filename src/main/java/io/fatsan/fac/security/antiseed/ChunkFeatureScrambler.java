package io.fatsan.fac.security.antiseed;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Scrambles dungeon floor patterns and buried-treasure holder blocks in newly
 * generated overworld chunks. This defeats two SeedCrackerX data-collection
 * vectors:
 *
 * <ul>
 *   <li><b>DungeonFinder</b> — reads the mossy-cobblestone / cobblestone floor
 *       pattern below spawners. We randomly swap ~35 % of the tiles.</li>
 *   <li><b>BuriedTreasureFinder</b> — validates the block directly below the
 *       chest (must be one of a specific set). We replace it with sand.</li>
 * </ul>
 */
public final class ChunkFeatureScrambler implements Listener {

  /** Blocks that SeedCrackerX's BuriedTreasureFinder accepts below a chest. */
  private static final Set<Material> TREASURE_HOLDER_BLOCKS = Set.of(
      Material.SANDSTONE, Material.STONE, Material.ANDESITE,
      Material.GRANITE, Material.DIORITE, Material.COAL_ORE,
      Material.IRON_ORE, Material.GOLD_ORE, Material.GRAVEL);

  private final JavaPlugin plugin;
  private final NamespacedKey scrambledKey;

  public ChunkFeatureScrambler(JavaPlugin plugin) {
    this.plugin = plugin;
    this.scrambledKey = new NamespacedKey(plugin, "asc_scrambled");
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onChunkLoad(ChunkLoadEvent event) {
    if (!event.isNewChunk()) {
      return;
    }
    Chunk chunk = event.getChunk();
    if (chunk.getWorld().getEnvironment() != World.Environment.NORMAL) {
      return;
    }

    plugin.getServer().getRegionScheduler().execute(
        plugin, chunk.getWorld(), chunk.getX(), chunk.getZ(),
        () -> processNewChunk(chunk));
  }

  private void processNewChunk(Chunk chunk) {
    if (chunk.getPersistentDataContainer().has(scrambledKey, PersistentDataType.BYTE)) {
      return;
    }

    boolean modified = false;

    for (BlockState state : chunk.getTileEntities()) {
      if (state.getType() == Material.SPAWNER) {
        scrambleDungeonFloor(state.getBlock());
        modified = true;
      }
      if (state.getType() == Material.CHEST) {
        Block block = state.getBlock();
        int localX = block.getX() & 0xF;
        int localZ = block.getZ() & 0xF;
        if (localX == 9 && localZ == 9 && block.getY() > 0 && block.getY() < 90) {
          disguiseBuriedTreasure(block);
          modified = true;
        }
      }
    }

    if (modified) {
      chunk.getPersistentDataContainer().set(scrambledKey, PersistentDataType.BYTE, (byte) 1);
    }
  }

  /**
   * Randomly swaps ~35 % of cobblestone ↔ mossy-cobblestone on the dungeon
   * floor (one layer below the spawner). This makes the floor pattern
   * unmatchable for SeedCrackerX's DungeonFinder.
   */
  private static void scrambleDungeonFloor(Block spawner) {
    ThreadLocalRandom rng = ThreadLocalRandom.current();
    int floorY = spawner.getY() - 1;
    World world = spawner.getWorld();

    for (int dx = -4; dx <= 4; dx++) {
      for (int dz = -4; dz <= 4; dz++) {
        Block floor = world.getBlockAt(spawner.getX() + dx, floorY, spawner.getZ() + dz);
        Material type = floor.getType();
        if ((type == Material.COBBLESTONE || type == Material.MOSSY_COBBLESTONE)
            && rng.nextInt(100) < 35) {
          floor.setType(
              type == Material.COBBLESTONE ? Material.MOSSY_COBBLESTONE : Material.COBBLESTONE,
              false);
        }
      }
    }
  }

  /**
   * Changes the holder block below a buried-treasure chest to sand, which is
   * not in SeedCrackerX's accepted holder list. The chest remains fully
   * functional and findable via explorer maps.
   */
  private static void disguiseBuriedTreasure(Block chest) {
    Block below = chest.getRelative(BlockFace.DOWN);
    if (TREASURE_HOLDER_BLOCKS.contains(below.getType())) {
      below.setType(Material.SAND, false);
    }
  }
}
