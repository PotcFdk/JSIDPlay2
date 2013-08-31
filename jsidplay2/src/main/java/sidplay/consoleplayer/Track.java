package sidplay.consoleplayer;

public class Track {
	public int getFirst() {
		return first;
	}

	public void setFirst(int first) {
		this.first = first;
	}

	public int getSelected() {
		return selected;
	}

	public void setSelected(int selected) {
		this.selected = selected;
	}

	public int getSongs() {
		return songs;
	}

	public void setSongs(int songs) {
		this.songs = songs;
	}

	public boolean isLoop() {
		return loop;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	public boolean isSingle() {
		return single;
	}

	public void setSingle(boolean single) {
		this.single = single;
	}

	/**
	 * First song number of the play-list. 0 is used, to reset the play-list
	 * start to the start tune, if a different tune is loaded.
	 */
	private int first;
	/**
	 * Current song number. If first > 0 it wraps around the count of songs
	 * 0 means use start song of the tune file.
	 */
	private int selected;
	/**
	 * Number of songs in the play-list.
	 */
	private int songs;
	/**
	 * Loop, if the play-list is played.
	 */
	private boolean loop;
	/**
	 * Always plays a single song (start song)
	 */
	private boolean single;

	public int getCurrentSong() {
		return selected;
	}

	public int getCurrentSongCount() {
		return songs;
	}

	public void setCurrentSingle(boolean s) {
		single = s;
	}

}