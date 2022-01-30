package server.restful.common;

import java.time.LocalDateTime;

public enum RTMPPlayerStatus {
	CREATED, ON_PLAY;

	private LocalDateTime created;

	private RTMPPlayerStatus() {
		this.created = LocalDateTime.now();
	}

	public LocalDateTime getCreated() {
		return created;
	}
}