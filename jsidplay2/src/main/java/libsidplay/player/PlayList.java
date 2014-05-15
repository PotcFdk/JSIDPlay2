package libsidplay.player;

import libsidplay.sidtune.SidTune;
import sidplay.ini.intf.IConfig;

/**
 * PlayList is a track list of songs to play. It starts with the first entry,
 * which is the marker to detect a wrap-around. The current entry is the
 * currently selected song.<BR>
 * e.g. 5 songs in a tune using start song number 3 will result in<BR>
 * [3],4,5,1,2 -> first = 3, length=5, current song is within range 1..5
 * 
 * @author Ken
 *
 */
public class PlayList {
	/**
	 * Configuration.
	 */
	private IConfig config;
	/**
	 * Current Tune.
	 */
	private SidTune tune;

	/**
	 * First entry of the play-list.
	 */
	private int first;
	/**
	 * Number of entries in the play-list.
	 */
	private int length;
	/**
	 * Current entry of the play-list. It wraps around the number of entries.
	 */
	private int current;

	/**
	 * Create a new play list.
	 */
	public PlayList(IConfig config, SidTune tune) {
		this.config = config;
		this.tune = tune;
		setCurrent(null);
	}

	/**
	 * Get currently selected play list entry
	 */
	public Integer getCurrent() {
		return current;
	}

	/**
	 * Get current track (play list entry relative to the first).
	 */
	public int getTrackNum() {
		int start = current - first + 1;
		return start < 1 ? start + length : start;
	}

	/**
	 * Choose a play list entry to play (null means use start tune).
	 */
	public void setCurrent(Integer songNum) {
		if (tune != null) {
			current = tune.selectSong(songNum);
		}
	}

	/**
	 * Get number of entries
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Use the current play list entry (select song of the tune).
	 */
	public void selectCurrentSong() {
		if (tune != null) {
			setCurrent(current);
			// New play-list?
			if (length == 0) {
				first = current;
				length = tune.getInfo().songs;
			}
		} else {
			current = 1;
			first = 1;
			length = 1;
		}
	}

	/**
	 * Choose next play list entry.
	 */
	public void next() {
		current = getNext();
	}

	/**
	 * Choose previous play list entry.
	 */
	public void previous() {
		current = getPrevious();
	}

	/**
	 * Choose first play list entry.
	 */
	public void first() {
		current = first;
	}

	/**
	 * Choose last play list entry.
	 */
	public void last() {
		int last = config.getSidplay2().isSingle() ? first : first - 1;
		current = last < 1 ? length : last;
	}

	/**
	 * Is a previous play list entry available?
	 */
	public boolean hasPrevious() {
		return current != first;
	}

	/**
	 * Is a next play list entry available?
	 */
	public boolean hasNext() {
		return getNext() != first;
	}

	/**
	 * Get previous play list entry (null means there is none).
	 */
	public int getPrevious() {
		int previous = config.getSidplay2().isSingle() ? current : current - 1;
		return previous < 1 ? length : previous;
	}

	/**
	 * Get next play list entry (null means there is none).
	 */
	public int getNext() {
		int next = config.getSidplay2().isSingle() ? current : current + 1;
		return next > length ? 1 : next;
	}

}