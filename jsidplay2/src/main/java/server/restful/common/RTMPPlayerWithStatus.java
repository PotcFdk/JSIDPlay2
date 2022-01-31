package server.restful.common;

import static server.restful.common.IServletSystemProperties.RTMP_DURATION_TOO_LONG_TIMEOUT;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_PLAYED_TIMEOUT;

import java.time.Duration;
import java.time.LocalDateTime;

import sidplay.Player;

public class RTMPPlayerWithStatus {

	private static enum Status {
		CREATED, ON_PLAY;
	}

	private Status status;

	private final LocalDateTime created;

	private final Player player;

	public RTMPPlayerWithStatus(Player player) {
		this.status = Status.CREATED;
		this.created = LocalDateTime.now();
		this.player = player;
	}

	public Player getPlayer() {
		return player;
	}

	public void setOnPlay() {
		status = Status.ON_PLAY;
	}

	public boolean toRemove() {
		return notYetPlayed() || exceedsMaximumDuration();
	}

	private boolean notYetPlayed() {
		return status == Status.CREATED
				&& Duration.between(created, LocalDateTime.now()).getSeconds() > RTMP_NOT_PLAYED_TIMEOUT;
	}

	private boolean exceedsMaximumDuration() {
		return status == Status.ON_PLAY
				&& Duration.between(created, LocalDateTime.now()).getSeconds() > RTMP_DURATION_TOO_LONG_TIMEOUT;
	}
}
