package io.fatsan.fac.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerRotation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientHeldItemChange;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCloseWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientVehicleMove;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerBoat;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSteerVehicle;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientResourcePackStatus;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerKeepAlive;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowConfirmation;
import io.fatsan.fac.packet.PacketIntakeService;
import io.fatsan.fac.transaction.TransactionManager;
import io.fatsan.fac.model.PacketEvent;
import io.fatsan.fac.model.PacketFlyingEvent;
import io.fatsan.fac.model.PacketInteractEvent;
import io.fatsan.fac.model.PacketDiggingEvent;
import io.fatsan.fac.model.PacketPlaceEvent;
import io.fatsan.fac.model.PacketHeldItemEvent;
import io.fatsan.fac.model.PacketWindowEvent;
import io.fatsan.fac.model.PacketEntityActionEvent;
import io.fatsan.fac.model.PacketKeepAliveEvent;
import io.fatsan.fac.model.PacketTransactionEvent;
import io.fatsan.fac.model.PacketVehicleEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PacketEvents API entegrasyonu - Tüm ham paketleri dinler
 * 1. PacketEvents API entegrasyonu
 * 2. Packet abstraction layer
 * 3-5. Flying packet handlers
 * 6-7. UseEntity packet handlers
 * 8. BlockPlace packet handler
 * 9-11. BlockDig packet handlers
 * 12. HeldItemChange packet handler
 * 13. CloseWindow packet handler
 * 14. WindowClick packet handler
 * 15. CreativeInventoryAction packet handler
 * 16-18. EntityAction packet handlers
 * 19. KeepAlive packet handler (client->server)
 * 20-21. Transaction packet handlers
 * 22. VehicleMove packet handler
 * 23. SteerBoat packet handler
 * 24. SteerVehicle packet handler
 * 25. ResourcePackStatus packet handler
 */
public class PacketEventsListener extends PacketListenerAbstract {
  private static final Logger LOGGER = Logger.getLogger(PacketEventsListener.class.getName());

  private final PacketIntakeService intakeService;
  private final TransactionManager transactionManager;
  private final Plugin plugin;
  private final Map<UUID, PlayerPacketState> playerStates;
  private final io.fatsan.fac.service.PacketContext packetContext;
  private final io.fatsan.fac.service.PacketTimerService packetTimerService;

  public PacketEventsListener(Plugin plugin, PacketIntakeService intakeService,
      TransactionManager transactionManager,
      io.fatsan.fac.service.PacketContext packetContext,
      io.fatsan.fac.service.PacketTimerService packetTimerService) {
    super(PacketListenerPriority.NORMAL);
    this.plugin = plugin;
    this.intakeService = intakeService;
    this.transactionManager = transactionManager;
    this.playerStates = new ConcurrentHashMap<>();
    this.packetContext = packetContext;
    this.packetTimerService = packetTimerService;
  }

  public void register() {
    PacketEvents.getAPI().getEventManager().registerListener(this);
    LOGGER.info("PacketEvents listener registered");
  }

  public void unregister() {
    PacketEvents.getAPI().getEventManager().unregisterListener(this);
    LOGGER.info("PacketEvents listener unregistered");
  }

  @Override
  public void onPacketReceive(PacketReceiveEvent event) {
    UUID playerId = event.getUser().getUUID();
    long nanoTime = System.nanoTime();
    int packetId = event.getPacketId();

    PlayerPacketState state = playerStates.computeIfAbsent(playerId, k -> new PlayerPacketState());

    try {
      // Flying packets (Position + Rotation)
      if (event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING) {
        handleFlying(playerId, nanoTime, state);
      } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION) {
        handlePlayerPosition(playerId, event, nanoTime, state);
      } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {
        handlePlayerRotation(playerId, event, nanoTime, state);
      } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
        handlePlayerPositionAndRotation(playerId, event, nanoTime, state);
      }
      // Combat
      else if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
        handleInteractEntity(playerId, event, nanoTime);
      }
      // Block interaction
      else if (event.getPacketType() == PacketType.Play.Client.PLAYER_DIGGING) {
        handlePlayerDigging(playerId, event, nanoTime);
      } else if (event.getPacketType() == PacketType.Play.Client.PLAYER_BLOCK_PLACEMENT) {
        handleBlockPlacement(playerId, event, nanoTime);
      }
      // Inventory
      else if (event.getPacketType() == PacketType.Play.Client.HELD_ITEM_CHANGE) {
        handleHeldItemChange(playerId, event, nanoTime);
      } else if (event.getPacketType() == PacketType.Play.Client.CLOSE_WINDOW) {
        handleCloseWindow(playerId, event, nanoTime);
      } else if (event.getPacketType() == PacketType.Play.Client.CLICK_WINDOW) {
        handleWindowClick(playerId, event, nanoTime);
      } else if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
        handleCreativeInventoryAction(playerId, event, nanoTime);
      }
      // Entity actions
      else if (event.getPacketType() == PacketType.Play.Client.ENTITY_ACTION) {
        handleEntityAction(playerId, event, nanoTime);
      }
      // KeepAlive response
      else if (event.getPacketType() == PacketType.Play.Client.KEEP_ALIVE) {
        handleKeepAlive(playerId, event, nanoTime);
      } else if (event.getPacketType() == PacketType.Play.Client.PONG) {
        handlePong(playerId, event, nanoTime);
      }
      // Vehicle
      else if (event.getPacketType() == PacketType.Play.Client.VEHICLE_MOVE) {
        handleVehicleMove(playerId, event, nanoTime);
      } else if (event.getPacketType() == PacketType.Play.Client.STEER_BOAT) {
        handleSteerBoat(playerId, event, nanoTime);
      } else if (event.getPacketType() == PacketType.Play.Client.STEER_VEHICLE) {
        handleSteerVehicle(playerId, event, nanoTime);
      }
      // Resource pack
      else if (event.getPacketType() == PacketType.Play.Client.RESOURCE_PACK_STATUS) {
        handleResourcePackStatus(playerId, event, nanoTime);
      }

      state.lastPacketTime = nanoTime;
      state.packetCount++;

    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error processing packet from " + playerId, e);
    }
  }

  @Override
  public void onPacketSend(PacketSendEvent event) {
    UUID playerId = event.getUser().getUUID();
    long nanoTime = System.nanoTime();

    try {
      // Transaction packets (server -> client)
      if (event.getPacketType() == PacketType.Play.Server.WINDOW_CONFIRMATION) {
        handleServerWindowConfirmation(playerId, event, nanoTime);
      }
      // KeepAlive (server -> client)
      else if (event.getPacketType() == PacketType.Play.Server.KEEP_ALIVE) {
        handleServerKeepAlive(playerId, event, nanoTime);
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error processing outgoing packet to " + playerId, e);
    }
  }

  // Flying packet handlers (3-5)
  private void handleFlying(UUID playerId, long nanoTime, PlayerPacketState state) {
    PacketFlyingEvent flyingEvent = new PacketFlyingEvent(
        playerId.toString(),
        nanoTime,
        state.lastX, state.lastY, state.lastZ,
        state.lastYaw, state.lastPitch,
        state.onGround,
        PacketFlyingEvent.FlyingType.FLYING
    );
    intakeService.emit(flyingEvent);
    packetTimerService.onFlyingPacket(playerId.toString(), nanoTime);
    packetContext.setTickBalance(playerId.toString(), packetTimerService.getBalance(playerId.toString()));
  }

  private void handlePlayerPosition(UUID playerId, PacketReceiveEvent event, long nanoTime, PlayerPacketState state) {
    WrapperPlayClientPlayerPosition packet = new WrapperPlayClientPlayerPosition(event);
    state.lastX = packet.getPosition().getX();
    state.lastY = packet.getPosition().getY();
    state.lastZ = packet.getPosition().getZ();
    state.onGround = packet.isOnGround();

    PacketFlyingEvent flyingEvent = new PacketFlyingEvent(
        playerId.toString(),
        nanoTime,
        packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(),
        state.lastYaw, state.lastPitch,
        packet.isOnGround(),
        PacketFlyingEvent.FlyingType.POSITION
    );
    intakeService.emit(flyingEvent);
    packetTimerService.onFlyingPacket(playerId.toString(), nanoTime);
    packetContext.setTickBalance(playerId.toString(), packetTimerService.getBalance(playerId.toString()));
    packetContext.setLastValidPosition(playerId.toString(),
        packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ());
    packetContext.setServerGround(playerId.toString(), packet.isOnGround());
  }

  private void handlePlayerRotation(UUID playerId, PacketReceiveEvent event, long nanoTime, PlayerPacketState state) {
    WrapperPlayClientPlayerRotation packet = new WrapperPlayClientPlayerRotation(event);
    state.lastYaw = packet.getYaw();
    state.lastPitch = packet.getPitch();
    state.onGround = packet.isOnGround();

    PacketFlyingEvent flyingEvent = new PacketFlyingEvent(
        playerId.toString(),
        nanoTime,
        state.lastX, state.lastY, state.lastZ,
        packet.getYaw(), packet.getPitch(),
        packet.isOnGround(),
        PacketFlyingEvent.FlyingType.ROTATION
    );
    intakeService.emit(flyingEvent);
  }

  private void handlePlayerPositionAndRotation(UUID playerId, PacketReceiveEvent event, long nanoTime, PlayerPacketState state) {
    WrapperPlayClientPlayerPositionAndRotation packet = new WrapperPlayClientPlayerPositionAndRotation(event);
    state.lastX = packet.getPosition().getX();
    state.lastY = packet.getPosition().getY();
    state.lastZ = packet.getPosition().getZ();
    state.lastYaw = packet.getYaw();
    state.lastPitch = packet.getPitch();
    state.onGround = packet.isOnGround();

    PacketFlyingEvent flyingEvent = new PacketFlyingEvent(
        playerId.toString(),
        nanoTime,
        packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(),
        packet.getYaw(), packet.getPitch(),
        packet.isOnGround(),
        PacketFlyingEvent.FlyingType.POSITION_AND_ROTATION
    );
    intakeService.emit(flyingEvent);
    packetTimerService.onFlyingPacket(playerId.toString(), nanoTime);
    packetContext.setTickBalance(playerId.toString(), packetTimerService.getBalance(playerId.toString()));
    packetContext.setLastValidPosition(playerId.toString(),
        packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ());
    packetContext.setServerGround(playerId.toString(), packet.isOnGround());
  }

  // Combat packet handlers (6-7)
  private void handleInteractEntity(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);

    PacketInteractEvent.InteractType type = switch (packet.getAction()) {
      case ATTACK -> PacketInteractEvent.InteractType.ATTACK;
      case INTERACT -> PacketInteractEvent.InteractType.INTERACT;
      case INTERACT_AT -> PacketInteractEvent.InteractType.INTERACT_AT;
    };

    PacketInteractEvent interactEvent = new PacketInteractEvent(
        playerId.toString(),
        nanoTime,
        packet.getEntityId(),
        type,
        packet.getTarget().orElse(null)
    );
    intakeService.emit(interactEvent);
  }

  // Block digging handlers (9-11)
  private void handlePlayerDigging(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientPlayerDigging packet = new WrapperPlayClientPlayerDigging(event);

    PacketDiggingEvent.DiggingAction action = switch (packet.getAction()) {
      case START_DIGGING -> PacketDiggingEvent.DiggingAction.START;
      case CANCELLED_DIGGING -> PacketDiggingEvent.DiggingAction.CANCEL;
      case FINISHED_DIGGING -> PacketDiggingEvent.DiggingAction.FINISH;
      case DROP_ITEM_STACK -> PacketDiggingEvent.DiggingAction.DROP_STACK;
      case DROP_ITEM -> PacketDiggingEvent.DiggingAction.DROP_ITEM;
      case RELEASE_USE_ITEM -> PacketDiggingEvent.DiggingAction.RELEASE_ITEM;
      case SWAP_ITEM_WITH_OFFHAND -> PacketDiggingEvent.DiggingAction.SWAP_OFFHAND;
      case STAB -> PacketDiggingEvent.DiggingAction.STAB;
    };

    PacketDiggingEvent diggingEvent = new PacketDiggingEvent(
        playerId.toString(),
        nanoTime,
        packet.getBlockPosition(),
        action,
        packet.getBlockFace()
    );
    intakeService.emit(diggingEvent);
  }

  // Block place handler (8)
  private void handleBlockPlacement(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientPlayerBlockPlacement packet = new WrapperPlayClientPlayerBlockPlacement(event);

    PacketPlaceEvent placeEvent = new PacketPlaceEvent(
        playerId.toString(),
        nanoTime,
        packet.getBlockPosition(),
        packet.getFace(),
        packet.getCursorPosition()
    );
    intakeService.emit(placeEvent);
  }

  // Inventory handlers (12-15)
  private void handleHeldItemChange(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientHeldItemChange packet = new WrapperPlayClientHeldItemChange(event);

    PacketHeldItemEvent heldItemEvent = new PacketHeldItemEvent(
        playerId.toString(),
        nanoTime,
        packet.getSlot()
    );
    intakeService.emit(heldItemEvent);
  }

  private void handleCloseWindow(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientCloseWindow packet = new WrapperPlayClientCloseWindow(event);

    PacketWindowEvent windowEvent = new PacketWindowEvent(
        playerId.toString(),
        nanoTime,
        packet.getWindowId(),
        PacketWindowEvent.WindowAction.CLOSE
    );
    intakeService.emit(windowEvent);
  }

  private void handleWindowClick(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientClickWindow packet = new WrapperPlayClientClickWindow(event);

    PacketWindowEvent windowEvent = new PacketWindowEvent(
        playerId.toString(),
        nanoTime,
        packet.getWindowId(),
        PacketWindowEvent.WindowAction.CLICK,
        packet.getSlot(),
        packet.getButton(),
        packet.getActionNumber().orElse(0)
    );
    intakeService.emit(windowEvent);
  }

  private void handleCreativeInventoryAction(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientCreativeInventoryAction packet = new WrapperPlayClientCreativeInventoryAction(event);

    PacketWindowEvent windowEvent = new PacketWindowEvent(
        playerId.toString(),
        nanoTime,
        0, // Creative inventory
        PacketWindowEvent.WindowAction.CREATIVE_ACTION,
        packet.getSlot(),
        0,
        0,
        packet.getItemStack()
    );
    intakeService.emit(windowEvent);
  }

  // Entity action handlers (16-18)
  private void handleEntityAction(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientEntityAction packet = new WrapperPlayClientEntityAction(event);

    PacketEntityActionEvent.ActionType type = switch (packet.getAction()) {
      case START_SPRINTING -> PacketEntityActionEvent.ActionType.START_SPRINT;
      case STOP_SPRINTING -> PacketEntityActionEvent.ActionType.STOP_SPRINT;
      case START_SNEAKING -> PacketEntityActionEvent.ActionType.START_SNEAK;
      case STOP_SNEAKING -> PacketEntityActionEvent.ActionType.STOP_SNEAK;
      case START_JUMPING_WITH_HORSE -> PacketEntityActionEvent.ActionType.HORSE_JUMP;
      case STOP_JUMPING_WITH_HORSE -> PacketEntityActionEvent.ActionType.HORSE_JUMP_STOP;
      case OPEN_HORSE_INVENTORY -> PacketEntityActionEvent.ActionType.HORSE_INVENTORY;
      case START_FLYING_WITH_ELYTRA -> PacketEntityActionEvent.ActionType.ELYTRA_START;
      default -> PacketEntityActionEvent.ActionType.UNKNOWN;
    };

    PacketEntityActionEvent actionEvent = new PacketEntityActionEvent(
        playerId.toString(),
        nanoTime,
        type,
        packet.getJumpBoost()
    );
    intakeService.emit(actionEvent);
  }

  // KeepAlive handler (19)
  private void handleKeepAlive(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientKeepAlive packet = new WrapperPlayClientKeepAlive(event);

    PacketKeepAliveEvent keepAliveEvent = new PacketKeepAliveEvent(
        playerId.toString(),
        nanoTime,
        packet.getId(),
        true // client response
    );
    intakeService.emit(keepAliveEvent);

    // Notify transaction manager
    transactionManager.handleKeepAliveResponse(playerId, packet.getId(), nanoTime);
  }

  private void handlePong(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientPong packet = new WrapperPlayClientPong(event);
    transactionManager.handlePong(playerId, packet.getId(), nanoTime);
  }

  // Transaction handlers (20-21)
  private void handleServerWindowConfirmation(UUID playerId, PacketSendEvent event, long nanoTime) {
    WrapperPlayServerWindowConfirmation packet = new WrapperPlayServerWindowConfirmation(event);

    PacketTransactionEvent transactionEvent = new PacketTransactionEvent(
        playerId.toString(),
        nanoTime,
        packet.getWindowId(),
        packet.getActionId(),
        packet.isAccepted(),
        PacketTransactionEvent.TransactionDirection.SERVER_TO_CLIENT
    );
    intakeService.emit(transactionEvent);

    // Track transaction
    transactionManager.registerTransaction(playerId, packet.getWindowId(), packet.getActionId(), nanoTime);
  }

  private void handleServerKeepAlive(UUID playerId, PacketSendEvent event, long nanoTime) {
    WrapperPlayServerKeepAlive packet = new WrapperPlayServerKeepAlive(event);
    transactionManager.registerKeepAlive(playerId, packet.getId(), nanoTime);
  }

  // Vehicle handlers (22-24)
  private void handleVehicleMove(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientVehicleMove packet = new WrapperPlayClientVehicleMove(event);

    com.github.retrooper.packetevents.util.Vector3d pos = packet.getPosition();
    com.github.retrooper.packetevents.protocol.world.Location loc =
        new com.github.retrooper.packetevents.protocol.world.Location(
            pos.getX(), pos.getY(), pos.getZ(), packet.getYaw(), packet.getPitch());
    PacketVehicleEvent vehicleEvent = PacketVehicleEvent.move(
        playerId.toString(), nanoTime,
        loc, packet.getYaw(), packet.getPitch());
    intakeService.emit(vehicleEvent);
  }

  private void handleSteerBoat(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientSteerBoat packet = new WrapperPlayClientSteerBoat(event);

    PacketVehicleEvent vehicleEvent = PacketVehicleEvent.boatSteer(
        playerId.toString(), nanoTime,
        null, 0f, 0f,
        packet.isLeftPaddleTurning(), packet.isRightPaddleTurning());
    intakeService.emit(vehicleEvent);
  }

  private void handleSteerVehicle(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientSteerVehicle packet = new WrapperPlayClientSteerVehicle(event);

    PacketVehicleEvent vehicleEvent = PacketVehicleEvent.steer(
        playerId.toString(), nanoTime,
        packet.getForward(), packet.getSideways());
    intakeService.emit(vehicleEvent);
  }

  // Resource pack handler (25)
  private void handleResourcePackStatus(UUID playerId, PacketReceiveEvent event, long nanoTime) {
    WrapperPlayClientResourcePackStatus packet = new WrapperPlayClientResourcePackStatus(event);

    // Resource pack status tracking for connection validation
    PlayerPacketState state = playerStates.get(playerId);
    if (state != null) {
      state.resourcePackStatus = packet.getResult().name();
    }
  }

  public void onPlayerQuit(UUID playerId) {
    playerStates.remove(playerId);
    transactionManager.clearPlayer(playerId);
    packetTimerService.clearPlayer(playerId.toString());
    packetContext.clearPlayer(playerId.toString());
  }

  public PlayerPacketState getPlayerState(UUID playerId) {
    return playerStates.get(playerId);
  }

  /**
   * Per-player packet state tracking
   */
  public static class PlayerPacketState {
    public volatile double lastX, lastY, lastZ;
    public volatile float lastYaw, lastPitch;
    public volatile boolean onGround;
    public volatile long lastPacketTime;
    public volatile long packetCount;
    public volatile String resourcePackStatus;
    public volatile int transactionCounter;
    public volatile long lastTransactionTime;
  }
}
