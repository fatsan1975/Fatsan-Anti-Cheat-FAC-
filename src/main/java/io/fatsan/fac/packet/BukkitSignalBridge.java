package io.fatsan.fac.packet;

import io.fatsan.fac.model.BlockBreakContextSignal;
import io.fatsan.fac.model.BlockBreakEventSignal;
import io.fatsan.fac.model.BlockPlaceEventSignal;
import io.fatsan.fac.model.CombatContextSignal;
import io.fatsan.fac.model.CombatHitEvent;
import io.fatsan.fac.model.FishingSignal;
import io.fatsan.fac.model.HotbarSignal;
import io.fatsan.fac.model.InventoryClickEventSignal;
import io.fatsan.fac.model.KeepAliveSignal;
import io.fatsan.fac.model.MovementContextSignal;
import io.fatsan.fac.model.MovementEvent;
import io.fatsan.fac.model.PlayerStateEvent;
import io.fatsan.fac.model.RotationEvent;
import io.fatsan.fac.model.TeleportSignal;
import io.fatsan.fac.config.WorldConfigService;
import io.fatsan.fac.service.PacketContext;
import io.fatsan.fac.service.PlayerExemptionService;
import io.fatsan.fac.service.PlayerGraceService;
import io.fatsan.fac.service.PlayerSignalTracker;
import io.fatsan.fac.service.PlayerStateService;
import io.fatsan.fac.service.PotionEffectRegistry;
import io.fatsan.fac.service.VelocityTracker;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.GameMode;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Damageable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BukkitSignalBridge implements Listener {
  private final PacketIntakeService intake;
  private final PlayerSignalTracker tracker;
  private final PlayerStateService playerStateService;
  private final int keepAliveSampleIntervalMillis;
  private final io.fatsan.fac.engine.AntiCheatEngine engine;
  private final VelocityTracker velocityTracker;
  private final PlayerGraceService graceService;
  private final PlayerExemptionService exemptionService;
  private final PotionEffectRegistry potionRegistry;
  private final PacketContext packetContext;
  private final WorldConfigService worldConfigService;
  private static final long ITEM_CONTEXT_CACHE_TTL_NANOS = 2_000_000_000L;
  private static final Map<Integer, CachedItemContext> ITEM_CONTEXT_CACHE = new ConcurrentHashMap<>();

  // ── Iteration 11 — new signal state ──────────────────────────────────────

  /**
   * Players who have sent a PlayerAnimateEvent (arm swing) since their last hit.
   * Consulted in onHit() to populate CombatContextSignal.swingPacketSent.
   * Entry is removed (consumed) on each hit — so it resets per-attack.
   */
  private final Set<UUID> pendingSwing = ConcurrentHashMap.newKeySet();

  /** Last fishing catch timestamp per player for FishingSignal interval. */
  private final Map<UUID, Long> lastFishCatch = new ConcurrentHashMap<>();

  /** Last hotbar slot-change timestamp per player for HotbarSignal interval. */
  private final Map<UUID, Long> lastHotbarChange = new ConcurrentHashMap<>();

  public BukkitSignalBridge(
      PacketIntakeService intake,
      PlayerSignalTracker tracker,
      PlayerStateService playerStateService,
      int keepAliveSampleIntervalMillis,
      io.fatsan.fac.engine.AntiCheatEngine engine,
      VelocityTracker velocityTracker,
      PlayerGraceService graceService,
      PlayerExemptionService exemptionService,
      PotionEffectRegistry potionRegistry,
      PacketContext packetContext,
      WorldConfigService worldConfigService) {
    this.intake = intake;
    this.tracker = tracker;
    this.playerStateService = playerStateService;
    this.keepAliveSampleIntervalMillis = keepAliveSampleIntervalMillis;
    this.engine = engine;
    this.velocityTracker = velocityTracker;
    this.graceService = graceService;
    this.exemptionService = exemptionService;
    this.potionRegistry = potionRegistry;
    this.packetContext = packetContext;
    this.worldConfigService = worldConfigService;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onMove(PlayerMoveEvent event) {
    if (event.getTo() == null || event.getFrom().getWorld() != event.getTo().getWorld()) {
      return;
    }
    Player player = event.getPlayer();
    if (!worldConfigService.isActive(player.getWorld().getName())) {
      return;
    }
    long now = System.nanoTime();
    long interval = tracker.intervalMove(player.getUniqueId(), now);
    double dx = event.getTo().getX() - event.getFrom().getX();
    double dz = event.getTo().getZ() - event.getFrom().getZ();
    float dyaw = event.getTo().getYaw() - event.getFrom().getYaw();
    float dpitch = event.getTo().getPitch() - event.getFrom().getPitch();
    double dy = event.getTo().getY() - event.getFrom().getY();
    double deltaXZ = Math.sqrt(dx * dx + dz * dz);

    if (deltaXZ < 0.38D && player.isOnGround()) {
      playerStateService.updateSafeLocation(player.getUniqueId(), event.getTo());
    }

    intake.emit(
        new MovementEvent(
            playerId(player),
            now,
            deltaXZ,
            dy,
            player.isOnGround(),
            player.getFallDistance(),
            player.isGliding(),
            player.isInsideVehicle(),
            interval));
    org.bukkit.util.Vector vel = player.getVelocity();
    intake.emit(
        new PlayerStateEvent(
            playerId(player),
            now,
            deltaXZ,
            dy,
            player.isOnGround(),
            player.isSprinting(),
            player.isSneaking(),
            player.isHandRaised() && !player.isBlocking(),
            player.isBlocking(),
            player.isInWater(),
            player.isInLava(),
            player.isClimbing(),
            player.isGliding(),
            player.isInsideVehicle(),
            vel.getX(),
            vel.getY(),
            vel.getZ(),
            interval));
    velocityTracker.recordVelocity(playerId(player), vel.getX(), vel.getY(), vel.getZ());
    intake.emit(new RotationEvent(playerId(player), now, dyaw, dpitch));

    // ── Iteration 11: MovementContextSignal ─────────────────────────────────
    double intervalSeconds = interval > 0 && interval < 200_000_000L
        ? interval / 1_000_000_000.0 : 0.0;
    double horizontalSpeedBps = intervalSeconds > 0 ? deltaXZ / intervalSeconds : 0.0;
    boolean insideSolid = !player.isFlying()
        && !player.isGliding()
        && !player.isInsideVehicle()
        && event.getTo() != null
        && event.getTo().getBlock().getType().isSolid();
    intake.emit(new MovementContextSignal(
        playerId(player), now,
        event.getTo() != null ? event.getTo().getX() : event.getFrom().getX(),
        event.getTo() != null ? event.getTo().getY() : event.getFrom().getY(),
        event.getTo() != null ? event.getTo().getZ() : event.getFrom().getZ(),
        deltaXZ, dy, horizontalSpeedBps,
        player.isOnGround(), insideSolid,
        player.isInWater(), player.isInLava(),
        player.isGliding(), player.isInsideVehicle(), player.isClimbing(),
        interval));
    if (tracker.shouldSampleKeepAlive(player.getUniqueId(), now, keepAliveSampleIntervalMillis)) {
      intake.emit(new KeepAliveSignal(playerId(player), now, player.getPing()));
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player player)) {
      return;
    }
    if (!worldConfigService.isActive(player.getWorld().getName())) {
      return;
    }
    long now = System.nanoTime();
    long interval = tracker.intervalHit(player.getUniqueId(), now);
    double distance = player.getLocation().distance(event.getEntity().getLocation());
    boolean critLike = !player.isOnGround() && player.getFallDistance() > 0.0F;
    String targetId = event.getEntity().getUniqueId().toString();
    intake.emit(
        new CombatHitEvent(
            playerId(player),
            now,
            distance,
            critLike,
            player.isOnGround(),
            player.getFallDistance(),
            player.isGliding(),
            player.isInsideVehicle(),
            interval,
            targetId));
    // Record expected knockback so AntiKBCheck can compare against observed velocity
    if (event.getEntity() instanceof Player target) {
      velocityTracker.expectKnockback(target.getUniqueId().toString(), 0.4, 0.4);
    }

    // ── Iteration 11: CombatContextSignal ──────────────────────────────────
    UUID attackerUuid = player.getUniqueId();
    boolean swingPacketSent = pendingSwing.remove(attackerUuid);
    org.bukkit.Location aLoc = player.getLocation();
    org.bukkit.Location vLoc = event.getEntity().getLocation();
    boolean solidBetween = hasSolidBlockBetween(
        aLoc.getWorld(),
        aLoc.getX(), aLoc.getY() + player.getEyeHeight(), aLoc.getZ(),
        vLoc.getX(), vLoc.getY() + 0.9, vLoc.getZ());
    intake.emit(new CombatContextSignal(
        playerId(player), now,
        targetId,
        distance,
        swingPacketSent,
        aLoc.getX(), aLoc.getY(), aLoc.getZ(),
        vLoc.getX(), vLoc.getY(), vLoc.getZ(),
        solidBetween));
  }

  /** Tracks arm swing for NoSwingCheck via CombatContextSignal.swingPacketSent. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onAnimation(PlayerAnimationEvent event) {
    if (!worldConfigService.isActive(event.getPlayer().getWorld().getName())) {
      return;
    }
    if (event.getAnimationType() == org.bukkit.event.player.PlayerAnimationType.ARM_SWING) {
      pendingSwing.add(event.getPlayer().getUniqueId());
    }
  }

  /** Emits FishingSignal when a player reels in a fish. */
  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onFish(PlayerFishEvent event) {
    if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH
        && event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) {
      return;
    }
    if (!(event.getPlayer() instanceof Player player)) return;
    if (!worldConfigService.isActive(player.getWorld().getName())) {
      return;
    }
    long now = System.nanoTime();
    UUID uuid = player.getUniqueId();
    long last = lastFishCatch.getOrDefault(uuid, Long.MIN_VALUE);
    long interval = last == Long.MIN_VALUE ? Long.MAX_VALUE : now - last;
    lastFishCatch.put(uuid, now);
    intake.emit(new FishingSignal(playerId(player), now, interval));
  }

  /** Emits HotbarSignal when a player changes their held item slot. */
  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onItemHeld(PlayerItemHeldEvent event) {
    Player player = event.getPlayer();
    if (!worldConfigService.isActive(player.getWorld().getName())) {
      return;
    }
    long now = System.nanoTime();
    UUID uuid = player.getUniqueId();
    long last = lastHotbarChange.getOrDefault(uuid, Long.MIN_VALUE);
    long interval = last == Long.MIN_VALUE ? Long.MAX_VALUE : now - last;
    lastHotbarChange.put(uuid, now);
    intake.emit(new HotbarSignal(
        playerId(player), now,
        event.getPreviousSlot(), event.getNewSlot(),
        interval));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPlace(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    if (!worldConfigService.isActive(player.getWorld().getName())) {
      return;
    }
    long now = System.nanoTime();
    long interval = tracker.intervalPlace(player.getUniqueId(), now);
    double horizontal = player.getVelocity().setY(0).length();
    String itemKey = event.getItemInHand().getType().getKey().getKey();
    intake.emit(new BlockPlaceEventSignal(playerId(player), now, interval, player.isSprinting(), horizontal, itemKey));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    if (!worldConfigService.isActive(player.getWorld().getName())) {
      return;
    }
    long now = System.nanoTime();
    long interval = tracker.intervalBreak(player.getUniqueId(), now);
    ItemStack hand = player.getInventory().getItemInMainHand();
    int haste = potionAmplifierByName(player, "HASTE", "FAST_DIGGING");
    int fatigue = potionAmplifierByName(player, "MINING_FATIGUE", "SLOW_DIGGING");
    double attackSpeed = 4.0D;
    double movementSpeed = Math.max(0.1D, player.getWalkSpeed());
    ItemContext itemContext = resolveItemContext(hand, now);
    intake.emit(
        new BlockBreakEventSignal(
            playerId(player),
            now,
            interval,
            itemContext.efficiency,
            haste,
            fatigue,
            attackSpeed,
            movementSpeed,
            itemContext.itemTypeKey,
            itemContext.itemAttackBonus,
            itemContext.itemMoveBonus,
            itemContext.enchantWeight,
            itemContext.customContext));

    // ── Iteration 11: BlockBreakContextSignal ───────────────────────────────
    float hardness = event.getBlock().getType().getHardness();
    String materialName = event.getBlock().getType().name();
    intake.emit(new BlockBreakContextSignal(
        playerId(player), now, interval, hardness, materialName));
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    if (!worldConfigService.isActive(player.getWorld().getName())) {
      return;
    }
    long now = System.nanoTime();
    long interval = tracker.intervalInventoryClick(player.getUniqueId(), now);
    boolean movingFast = player.getVelocity().setY(0).length() > 0.23D;
    boolean offhandSwap = event.getSlot() == 40; // slot 40 = offhand in player inventory
    intake.emit(new InventoryClickEventSignal(playerId(player), now, interval, movingFast, offhandSwap));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onTeleport(PlayerTeleportEvent event) {
    intake.emit(new TeleportSignal(playerId(event.getPlayer()), System.nanoTime()));
    if (event.getTo() != null) {
      playerStateService.updateSafeLocation(event.getPlayer().getUniqueId(), event.getTo());
    }
    // Suppress checks for 2s after any teleport (Folia region transitions, /tp, Essentials, etc.)
    graceService.onTeleport(playerId(event.getPlayer()));
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    String playerId = uuid.toString();
    tracker.clear(uuid);
    playerStateService.clear(uuid);
    velocityTracker.clearPlayer(playerId);
    // Release all per-player check state (buffers, window trackers, streak counters)
    intake.registry().clearPlayer(playerId);
    // Release all per-player service state (risk, trust, suspicion pattern, corroboration)
    engine.clearPlayer(playerId);
    // Layer 2 shared state cleanup
    packetContext.clearPlayer(playerId);
    // Phase B service cleanup
    graceService.onQuit(playerId);
    exemptionService.onQuit(playerId);
    potionRegistry.onQuit(playerId);
    // Iteration 11 — new signal state cleanup
    pendingSwing.remove(uuid);
    lastFishCatch.remove(uuid);
    lastHotbarChange.remove(uuid);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    String playerId = uuid.toString();
    tracker.clear(uuid);
    playerStateService.updateSafeLocation(uuid, player.getLocation());

    // Grant login grace — suppresses checks during chunk-loading and initial position desync
    graceService.onLogin(playerId);

    // Mark creative/spectator players — flight checks are permanently exempt for them
    GameMode mode = player.getGameMode();
    if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
      graceService.onCreativeMode(playerId);
    }
    if (mode == GameMode.SPECTATOR) {
      exemptionService.setSpectating(playerId, true);
    }

    // Honour the fac.bypass permission node
    if (player.hasPermission("fac.bypass")) {
      exemptionService.setPermissionBypass(playerId, true);
    }

    // Sync any persistent potion effects the player already has on join
    for (PotionEffect effect : player.getActivePotionEffects()) {
      if (effect.getType() != null) {
        potionRegistry.onEffectAdd(playerId, effect.getType().getKey().getKey(), effect.getAmplifier());
      }
    }
  }

  /** Keeps PotionEffectRegistry in sync with active effects for speed/haste/jump checks. */
  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  public void onPotionEffect(EntityPotionEffectEvent event) {
    if (!(event.getEntity() instanceof Player player)) return;
    var modifiedType = event.getModifiedType();
    if (modifiedType == null) return;
    String effectKey = modifiedType.getKey().getKey();
    String playerId = playerId(player);
    var action = event.getAction();
    if (action == EntityPotionEffectEvent.Action.ADDED
        || action == EntityPotionEffectEvent.Action.CHANGED) {
      var newEffect = event.getNewEffect();
      int amplifier = newEffect != null ? newEffect.getAmplifier() : 0;
      potionRegistry.onEffectAdd(playerId, effectKey, amplifier);
    } else {
      potionRegistry.onEffectRemove(playerId, effectKey);
    }
  }

  /** Updates grace and exemption state when a player changes game mode. */
  @EventHandler(priority = EventPriority.MONITOR)
  public void onGameModeChange(PlayerGameModeChangeEvent event) {
    Player player = event.getPlayer();
    String playerId = playerId(player);
    GameMode newMode = event.getNewGameMode();
    if (newMode == GameMode.CREATIVE || newMode == GameMode.SPECTATOR) {
      graceService.onCreativeMode(playerId);
      exemptionService.setSpectating(playerId, newMode == GameMode.SPECTATOR);
    } else {
      graceService.onSurvivalMode(playerId);
      exemptionService.setSpectating(playerId, false);
    }
  }


  /**
   * DDA (Digital Differential Analyzer) raycast checking for solid blocks
   * between two world positions.
   *
   * <p>Safe to call on a Folia region thread — only accesses blocks in
   * already-loaded chunks (both positions are in-range of the event).
   * Uses 0.25-block resolution (4 steps per block) to avoid missing thin blocks.
   *
   * @return true if any solid block intersects the line segment
   */
  private static boolean hasSolidBlockBetween(
      org.bukkit.World world,
      double x1, double y1, double z1,
      double x2, double y2, double z2) {
    if (world == null) return false;
    double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
    double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (length < 0.01) return false;
    int steps = (int) Math.ceil(length * 4); // 0.25-block resolution
    double sx = dx / steps, sy = dy / steps, sz = dz / steps;
    double cx = x1, cy = y1, cz = z1;
    for (int i = 1; i < steps; i++) {
      cx += sx; cy += sy; cz += sz;
      try {
        if (world.getBlockAt((int) Math.floor(cx), (int) Math.floor(cy),
            (int) Math.floor(cz)).getType().isSolid()) {
          return true;
        }
      } catch (Exception ignored) {
        // Unloaded chunk — don't treat as solid
        return false;
      }
    }
    return false;
  }

  private static int potionAmplifierByName(Player player, String... names) {
    for (PotionEffect effect : player.getActivePotionEffects()) {
      PotionEffectType type = effect.getType();
      if (type == null) continue;
      String key = type.getKey().getKey().toUpperCase(java.util.Locale.ROOT);
      for (String name : names) {
        if (key.contains(name)) return effect.getAmplifier();
      }
    }
    return -1;
  }


  private static double itemAttributeBonus(ItemStack item, String token) {
    if (item == null) return 0.0D;
    var meta = item.getItemMeta();
    if (meta == null) return 0.0D;
    var modifiers = meta.getAttributeModifiers();
    if (modifiers == null) return 0.0D;
    double sum = 0.0D;
    for (var entry : modifiers.entries()) {
      if (entry.getKey() != null && String.valueOf(entry.getKey()).contains(token)) {
        sum += entry.getValue().getAmount();
      }
    }
    return sum;
  }

  private static int enchantWeight(ItemStack item) {
    if (item == null) return 0;
    int total = 0;
    for (int level : item.getEnchantments().values()) {
      total += Math.max(level, 0);
    }
    return total;
  }

  private static ItemContext resolveItemContext(ItemStack item, long nowNanos) {
    if (item == null || item.getType().isAir()) {
      return ItemContext.EMPTY;
    }
    int key = itemFingerprint(item);
    CachedItemContext cached = ITEM_CONTEXT_CACHE.get(key);
    if (cached != null && nowNanos - cached.nanoTime <= ITEM_CONTEXT_CACHE_TTL_NANOS) {
      return cached.context;
    }

    ItemMeta meta = item.getItemMeta();
    int efficiency = item.getEnchantmentLevel(Enchantment.EFFICIENCY);
    double itemAttackBonus = itemAttributeBonus(item, "ATTACK_SPEED");
    double itemMoveBonus = itemAttributeBonus(item, "MOVEMENT_SPEED");
    int enchantWeight = enchantWeight(item);

    boolean hasPersistentData =
        meta != null && meta.getPersistentDataContainer() != null && !meta.getPersistentDataContainer().isEmpty();
    int damage = meta instanceof Damageable damageable ? damageable.getDamage() : 0;
    boolean commandLikeContext =
        meta != null
            && ((meta.hasCustomModelData())
                || meta.isUnbreakable()
                || hasPersistentData
                || damage > 150
                || (meta.hasLore() && !meta.getLore().isEmpty())
                || (meta.hasDisplayName() && meta.getDisplayName().contains("+")));

    boolean customContext =
        commandLikeContext
            || itemAttackBonus > 0.75D
            || itemMoveBonus > 0.10D
            || (itemAttackBonus + itemMoveBonus) > 1.25D
            || enchantWeight > 8
            || efficiency >= 5;

    ItemContext context =
        new ItemContext(
            efficiency,
            item.getType().name(),
            itemAttackBonus,
            itemMoveBonus,
            enchantWeight,
            customContext);
    ITEM_CONTEXT_CACHE.put(key, new CachedItemContext(nowNanos, context));
    if ((nowNanos & 0x1FF) == 0) {
      ITEM_CONTEXT_CACHE.entrySet().removeIf(e -> nowNanos - e.getValue().nanoTime > ITEM_CONTEXT_CACHE_TTL_NANOS);
    }
    return context;
  }

  private static int itemFingerprint(ItemStack item) {
    int hash = 17;
    hash = 31 * hash + item.getType().hashCode();
    hash = 31 * hash + item.getAmount();
    hash = 31 * hash + item.getEnchantments().hashCode();
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      hash = 31 * hash + meta.hashCode();
    }
    return hash;
  }

  private record CachedItemContext(long nanoTime, ItemContext context) {}

  private record ItemContext(
      int efficiency,
      String itemTypeKey,
      double itemAttackBonus,
      double itemMoveBonus,
      int enchantWeight,
      boolean customContext) {
    private static final ItemContext EMPTY = new ItemContext(0, "AIR", 0.0D, 0.0D, 0, false);
  }


  private static String playerId(Player player) {
    return player.getUniqueId().toString();
  }
}
