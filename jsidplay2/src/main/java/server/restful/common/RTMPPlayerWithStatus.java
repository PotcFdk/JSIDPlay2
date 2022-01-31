package server.restful.common;

import java.time.LocalDateTime;

import sidplay.Player;

public class RTMPPlayerWithStatus {

	public static enum Status {
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

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public Player getPlayer() {
		return player;
	}
}
