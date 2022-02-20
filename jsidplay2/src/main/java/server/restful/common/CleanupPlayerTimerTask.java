package server.restful.common;

import static java.util.Optional.ofNullable;
import static server.restful.common.IServletSystemProperties.RTMP_CLEANUP_PLAYER_COUNTER;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.juli.logging.Log;

import sidplay.Player;

public final class CleanupPlayerTimerTask extends TimerTask {

	private static final Map<UUID, RTMPPlayerWithStatus> PLAYER_MAP = Collections.synchronizedMap(new HashMap<>());

	private final Log logger;

	private int timerCounter;

	public CleanupPlayerTimerTask(Log logger) {
		this.logger = logger;
	}

	public static final void create(UUID uuid, Player player, File diskImage, ResourceBundle resourceBundle) {
		PLAYER_MAP.put(uuid, new RTMPPlayerWithStatus(player, diskImage, resourceBundle));
	}

	public static final void update(UUID uuid, Consumer<RTMPPlayerWithStatus> rtmpPlayerWithStatusConsumer) {
		ofNullable(PLAYER_MAP.get(uuid)).ifPresent(rtmpPlayerWithStatusConsumer);
	}

	@Override
	public final void run() {
		Collection<Entry<UUID, RTMPPlayerWithStatus>> rtmpEntriesToRemove = PLAYER_MAP.entrySet().stream()
				.filter(entrySet -> entrySet.getValue().isInvalid()).collect(Collectors.toList());

		rtmpEntriesToRemove.forEach(this::quitPlayer);

		PLAYER_MAP.entrySet().removeIf(rtmpEntriesToRemove::contains);

		if (timerCounter++ % RTMP_CLEANUP_PLAYER_COUNTER == 0) {
			PLAYER_MAP.entrySet().stream().forEach(this::printPlayer);
		}
	}

	private void quitPlayer(Map.Entry<UUID, RTMPPlayerWithStatus> entry) {
		logger.info("CleanupPlayerTimerTask: AUTO-QUIT RTMP stream of: " + entry.getKey());
		entry.getValue().quitPlayer();
	}

	private void printPlayer(Entry<UUID, RTMPPlayerWithStatus> entry) {
		logger.info(String.format("CleanupPlayerTimerTask: RTMP stream left: %s (valid until %s)", entry.getKey(),
				entry.getValue().getValidUntil()));
	}

}
