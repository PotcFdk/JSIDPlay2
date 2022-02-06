package server.restful.common;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.juli.logging.Log;

import sidplay.Player;

public final class CleanupPlayerTimerTask extends TimerTask {

	private static final Map<UUID, RTMPPlayerWithStatus> PLAYER_MAP = Collections.synchronizedMap(new HashMap<>());

	private final Log logger;

	public CleanupPlayerTimerTask(Log logger) {
		this.logger = logger;
	}

	public static final void create(UUID uuid, Player player, File diskImage) {
		PLAYER_MAP.put(uuid, new RTMPPlayerWithStatus(player, diskImage));
	}

	public static final void onPlay(UUID uuid) {
		Optional.ofNullable(PLAYER_MAP.get(uuid)).ifPresent(RTMPPlayerWithStatus::setOnPlay);
	}

	public static final void onPlayDone(UUID uuid) {
		Optional.ofNullable(PLAYER_MAP.get(uuid)).ifPresent(RTMPPlayerWithStatus::setOnPlayDone);
	}

	public static final void insertNextDisk(UUID uuid) {
		Optional.ofNullable(PLAYER_MAP.get(uuid)).ifPresent(RTMPPlayerWithStatus::insertNextDisk);
	}

	@Override
	public final void run() {
		Collection<Entry<UUID, RTMPPlayerWithStatus>> rtmpEntriesToRemove = PLAYER_MAP.entrySet().stream()
				.filter(entrySet -> entrySet.getValue().isValid()).collect(Collectors.toList());

		rtmpEntriesToRemove.forEach(this::quitPlayer);

		PLAYER_MAP.entrySet().removeIf(rtmpEntriesToRemove::contains);

		PLAYER_MAP.entrySet().stream().forEach(this::printPlayer);
	}

	private void printPlayer(Entry<UUID, RTMPPlayerWithStatus> entry) {
		logger.info("CleanupPlayerTimerTask: RTMP stream left: " + entry.getKey());
	}

	private void quitPlayer(Map.Entry<UUID, RTMPPlayerWithStatus> entry) {
		logger.info("CleanupPlayerTimerTask: AUTO-QUIT RTMP stream of: " + entry.getKey());
		entry.getValue().quitPlayer();
	}

}
