package libsidplay;

public class Track {
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

}