package server.restful.common;

import static server.restful.common.IServletSystemProperties.RTMP_DURATION_TOO_LONG_TIMEOUT;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_PLAYED_TIMEOUT;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.UUID;

import org.apache.juli.logging.Log;

import sidplay.Player;

public class CleanupPlayerTimerTask extends TimerTask {

	public static final Map<UUID, SimpleImmutableEntry<Player, RTMPPlayerStatus>> PLAYER_MAP = Collections
			.synchronizedMap(new HashMap<>());

	private Log logger;

	public CleanupPlayerTimerTask(Log logger) {
		this.logger = logger;
	}

	@Override
	public void run() {
		Collection<UUID> toRemove = new ArrayList<>();
		for (UUID uuid : PLAYER_MAP.keySet()) {
			SimpleImmutableEntry<Player, RTMPPlayerStatus> playerWithStatus = PLAYER_MAP.get(uuid);
			if (playerWithStatus != null) {
				if (playerWithStatus.getValue() == RTMPPlayerStatus.CREATED
						&& Duration.between(playerWithStatus.getValue().getCreated(), LocalDateTime.now())
								.getSeconds() > RTMP_NOT_PLAYED_TIMEOUT) {
					logger.info("CleanupPlayerTimerTask: RTMP_NOT_PLAYED_TIMEOUT RTMP stream of: " + uuid);

					toRemove.add(uuid);
				}
				if (playerWithStatus.getValue() == RTMPPlayerStatus.ON_PLAY
						&& Duration.between(playerWithStatus.getValue().getCreated(), LocalDateTime.now())
								.getSeconds() > RTMP_DURATION_TOO_LONG_TIMEOUT) {
					logger.info("CleanupPlayerTimerTask: RTMP_DURATION_TOO_LONG_TIMEOUT RTMP stream of: " + uuid);

					toRemove.add(uuid);
				}
			}
		}
		for (UUID uuid : toRemove) {
			SimpleImmutableEntry<Player, RTMPPlayerStatus> playerWithStatus = PLAYER_MAP.get(uuid);
			if (playerWithStatus != null) {
				Player player = playerWithStatus.getKey();
				if (player != null) {
					logger.info("CleanupPlayerTimerTask: AUTO-QUIT RTMP stream of: " + uuid);

					player.quit();
				}
			}
		}
		PLAYER_MAP.keySet().removeIf(toRemove::contains);

		for (UUID otherUuid : PLAYER_MAP.keySet()) {
			logger.info("CleanupPlayerTimerTask: RTMP stream left: " + otherUuid);
		}
	}

}
