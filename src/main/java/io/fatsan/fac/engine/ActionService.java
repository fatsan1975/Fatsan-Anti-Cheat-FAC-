package io.fatsan.fac.engine;

import io.fatsan.fac.config.FacConfig;
import io.fatsan.fac.model.ActionMode;
import io.fatsan.fac.model.CheckResult;
import io.fatsan.fac.service.ActionDisposition;
import io.fatsan.fac.service.ActionPolicyService;
import io.fatsan.fac.service.ActionRateLimiterService;
import io.fatsan.fac.service.AlertWebhookService;
import io.fatsan.fac.service.CorroborationService;
import io.fatsan.fac.service.DiscordWebhookService;
import io.fatsan.fac.service.PlayerGraceService;
import io.fatsan.fac.service.PlayerStateService;
import io.fatsan.fac.service.PremiumInsightsService;
import io.fatsan.fac.service.ProtocolProfileResolver;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ActionService {
  private final JavaPlugin plugin;
  private final FacConfig config;
  private final PlayerStateService playerStateService;
  private final CorroborationService corroborationService;
  private final Logger logger;
  private final ActionRateLimiterService actionRateLimiterService;
  private final ActionPolicyService actionPolicyService;
  private final ProtocolProfileResolver protocolProfileResolver = new ProtocolProfileResolver();
  private final AlertWebhookService alertWebhookService;
  private final PremiumInsightsService premiumInsightsService;
  private final PlayerGraceService graceService;
  private final DiscordWebhookService discordWebhookService;
  private final boolean premiumActive;

  public ActionService(
      JavaPlugin plugin,
      FacConfig config,
      PlayerStateService playerStateService,
      CorroborationService corroborationService,
      AlertWebhookService alertWebhookService,
      PremiumInsightsService premiumInsightsService,
      ActionPolicyService actionPolicyService,
      PlayerGraceService graceService,
      DiscordWebhookService discordWebhookService,
      boolean premiumActive) {
    this.plugin = plugin;
    this.config = config;
    this.playerStateService = playerStateService;
    this.corroborationService = corroborationService;
    this.logger = plugin.getLogger();
    this.alertWebhookService = alertWebhookService;
    this.premiumInsightsService = premiumInsightsService;
    this.actionPolicyService = actionPolicyService;
    this.graceService = graceService;
    this.discordWebhookService = discordWebhookService;
    this.premiumActive = premiumActive;
    this.actionRateLimiterService =
        new ActionRateLimiterService(
            config.actionAlertCooldownMillis(),
            config.actionSetbackCooldownMillis(),
            config.actionKickCooldownMillis());
  }

  public void handleSuspicion(String playerId, CheckResult result, double totalRisk) {
    if (!config.alertsEnabled()) {
      return;
    }

    ActionPolicyService.ResolvedPolicy policy = actionPolicyService.resolve(result);
    if (!policy.disposition().allowsAlert()) {
      return;
    }

    if (totalRisk < config.riskAlertThreshold()) {
      return;
    }

    Player suspect = Bukkit.getPlayerExact(playerId);
    if (suspect == null) {
      try {
        suspect = Bukkit.getPlayer(UUID.fromString(playerId));
      } catch (IllegalArgumentException ignored) {
      }
    }
    String protocol = "unknown";
    if (suspect != null) {
      protocol = protocolProfileResolver.resolve(suspect).name();
    }

    if (actionRateLimiterService.allowAlert(playerId)) {
      String message =
          "[FAC] "
              + playerId
              + " failed "
              + result.checkName()
              + " severity="
              + result.severity()
              + " risk="
              + String.format(Locale.ROOT, "%.2f", totalRisk)
              + " protocol="
              + protocol
              + " policy="
              + policy.disposition()
              + " family="
              + policy.family()
              + " reason="
              + result.reason();
      logger.info(message);

      if (premiumActive) {
        premiumInsightsService.record(result);
      }
      alertWebhookService.publish(message);

      for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
        if (onlinePlayer.hasPermission("fac.alerts")) {
          onlinePlayer.sendMessage(message);
        }
      }

      if (discordWebhookService != null) {
        discordWebhookService.sendAlert(playerId, result.checkName(),
            result.severity(), totalRisk, result.reason());
      }
    }

    if (!result.actionable()) {
      return;
    }

    ActionDisposition disposition = policy.disposition();
    if (!disposition.allowsSetback()) {
      return;
    }

    if (suspect == null) {
      return;
    }

    if (disposition.requiresCorroboration() && config.corroborationEnabled() && !corroborationService.isCorroborated(playerId)) {
      return;
    }

    final Player target = suspect;
    ActionMode mode = parseMode(config.actionMode());
    if (totalRisk >= config.riskKickThreshold()
        && disposition.allowsKick()
        && mode == ActionMode.KICK
        && actionRateLimiterService.allowKick(playerId)) {
      target.getScheduler().run(plugin, task -> target.kickPlayer("FAC: suspicious behavior"), null);
      return;
    }

    if (totalRisk >= config.riskSetbackThreshold()
        && (mode == ActionMode.SETBACK || mode == ActionMode.KICK)
        && actionRateLimiterService.allowSetback(playerId)) {
      Location safe = playerStateService.getSafeLocation(target.getUniqueId());
      if (safe != null) {
        // Grant setback grace BEFORE the teleport fires to suppress the
        // movement anomaly packets that arrive immediately after correction.
        graceService.onSetback(playerId);
        target
            .getScheduler()
            .run(
                plugin,
                task -> {
                  if (target.isOnline()) {
                    target.teleport(safe);
                  }
                },
                null);
      }
    }
  }

  private static ActionMode parseMode(String value) {
    try {
      return ActionMode.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (Exception ignored) {
      return ActionMode.ALERT;
    }
  }

  /** Removes all action cooldown state and cached protocol profile for the given player. Called on disconnect. */
  public void clearPlayer(String playerId) {
    actionRateLimiterService.clearPlayer(playerId);
    try {
      protocolProfileResolver.clearPlayer(UUID.fromString(playerId));
    } catch (IllegalArgumentException ignored) {
      // playerId is not a valid UUID — skip cache eviction
    }
  }
}
