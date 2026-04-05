package io.fatsan.fac.security;

import java.util.Locale;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.TabCompleteEvent;

public final class WorldSeedGuardListener implements Listener {
  private static final String BLOCK_MESSAGE = "[FAC] /seed command is blocked to protect world-seed privacy.";

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
    if (isSeedCommand(event.getMessage())) {
      event.setCancelled(true);
      event.getPlayer().sendMessage(BLOCK_MESSAGE);
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
  public void onServerCommand(ServerCommandEvent event) {
    if (isSeedCommand(event.getCommand())) {
      event.setCancelled(true);
      CommandSender sender = event.getSender();
      sender.sendMessage(BLOCK_MESSAGE);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onTabComplete(TabCompleteEvent event) {
    String buffer = event.getBuffer();
    if (!buffer.startsWith("/")) {
      return;
    }

    String normalized = normalize(buffer.substring(1));
    if ("s".equals(normalized)
        || "se".equals(normalized)
        || "see".equals(normalized)
        || "seed".equals(normalized)) {
      event.getCompletions().removeIf(completion -> "seed".equals(normalize(completion)));
    }
  }

  private static boolean isSeedCommand(String raw) {
    if (raw == null || raw.isBlank()) {
      return false;
    }

    String input = raw.startsWith("/") ? raw.substring(1) : raw;
    String[] parts = input.split("\\s+");
    if (parts.length == 0) {
      return false;
    }

    String command = normalize(parts[0]);
    return command.equals("seed") || command.endsWith(":seed") || command.endsWith("minecraft:seed");
  }

  private static String normalize(String value) {
    return value.toLowerCase(Locale.ROOT).trim();
  }
}
