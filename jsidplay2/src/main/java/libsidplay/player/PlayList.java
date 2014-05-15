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
	private Integer first;
	/**
	 * Number of entries in the play-list.
	 */
	private int length;
	/**
	 * Current entry of the play-list. It wraps around the number of entries.
	 */
	private Integer current;

	/**
	 * Create a new play list.
	 */
	public PlayList(IConfig config, SidTune tune) {
		this.config = config;
		this.tune = tune;
	}

	/**
	 * Get currently selected play list entry
	 */
	public Integer getCurrent() {
		return current;
	}

	/**
	 * Get current play list entry relative to the first.
	 */
	public int getCurrentRelative() {
		int start = current - first + 1;
		return start < 1 ? start + length : start;
	}

	/**
	 * Choose a play list entry to play (null means start tune).
	 */
	public void setCurrent(Integer songNum) {
		this.current = tune.selectSong(songNum);
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
			if (first == null) {
				first = current;
				length = config.getSidplay2().isSingle() ? 1
						: tune.getInfo().songs;
			}
		} else {
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
		current = last > length ? 1 : last;
	}

	/**
	 * Is a previous play list entry available?
	 */
	public boolean hasPrevious() {
		return current != null && current != first;
	}

	/**
	 * Is a next play list entry available?
	 */
	public boolean hasNext() {
		return current != null && getNext() != first;
	}

	/**
	 * Get previous play list entry (null means there is none).
	 */
	public Integer getPrevious() {
		if (current == null) {
			return null;
		}
		int previous = config.getSidplay2().isSingle() ? current : current - 1;
		return previous < 1 ? length : previous;
	}

	/**
	 * Get next play list entry (null means there is none).
	 */
	public Integer getNext() {
		if (current == null) {
			return null;
		}
		int next = config.getSidplay2().isSingle() ? current : current + 1;
		return next > length ? 1 : next;
	}

}