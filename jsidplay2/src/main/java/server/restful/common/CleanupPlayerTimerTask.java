package server.restful.common;

import static server.restful.common.IServletSystemProperties.RTMP_DURATION_TOO_LONG_TIMEOUT;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_PLAYED_TIMEOUT;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.UUID;

import org.apache.juli.logging.Log;

import server.restful.common.RTMPPlayerWithStatus.Status;
import sidplay.Player;

public class CleanupPlayerTimerTask extends TimerTask {

	public static final Map<UUID, RTMPPlayerWithStatus> PLAYER_MAP = Collections.synchronizedMap(new HashMap<>());

	private Log logger;

	public CleanupPlayerTimerTask(Log logger) {
		this.logger = logger;
	}

	@Override
	public void run() {
		Collection<UUID> toRemove = new ArrayList<>();
		for (UUID uuid : PLAYER_MAP.keySet()) {
			RTMPPlayerWithStatus playerWithStatus = PLAYER_MAP.get(uuid);
			if (playerWithStatus != null) {
				if (playerWithStatus.getStatus() == Status.CREATED
						&& Duration.between(playerWithStatus.getCreated(), LocalDateTime.now())
								.getSeconds() > RTMP_NOT_PLAYED_TIMEOUT) {
					logger.info("CleanupPlayerTimerTask: RTMP_NOT_PLAYED_TIMEOUT RTMP stream of: " + uuid);

					toRemove.add(uuid);
				}
				if (playerWithStatus.getStatus() == Status.ON_PLAY
						&& Duration.between(playerWithStatus.getCreated(), LocalDateTime.now())
								.getSeconds() > RTMP_DURATION_TOO_LONG_TIMEOUT) {
					logger.info("CleanupPlayerTimerTask: RTMP_DURATION_TOO_LONG_TIMEOUT RTMP stream of: " + uuid);

					toRemove.add(uuid);
				}
			}
		}
		for (UUID uuid : toRemove) {
			RTMPPlayerWithStatus playerWithStatus = PLAYER_MAP.get(uuid);
			if (playerWithStatus != null) {
				Player player = playerWithStatus.getPlayer();
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
