package io.fatsan.fac.security.antiseed;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerJoinGame;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerRespawn;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Intercepts JOIN_GAME and RESPAWN packets to replace the hashed seed with a
 * random value. This prevents SeedCrackerX from using the hashed seed for its
 * final brute-force step.
 */
final class HashedSeedSpoofListener extends PacketListenerAbstract {

  HashedSeedSpoofListener() {
    super(PacketListenerPriority.HIGHEST);
  }

  @Override
  public void onPacketSend(PacketSendEvent event) {
    if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
      WrapperPlayServerJoinGame wrapper = new WrapperPlayServerJoinGame(event);
      wrapper.setHashedSeed(ThreadLocalRandom.current().nextLong());
    } else if (event.getPacketType() == PacketType.Play.Server.RESPAWN) {
      WrapperPlayServerRespawn wrapper = new WrapperPlayServerRespawn(event);
      wrapper.setHashedSeed(ThreadLocalRandom.current().nextLong());
    }
  }
}
