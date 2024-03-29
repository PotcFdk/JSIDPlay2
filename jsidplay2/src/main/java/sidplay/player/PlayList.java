package sidplay.player;

import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;

/**
 * PlayList is a track list of songs to play. It starts with the first entry,
 * which is the marker to detect a wrap-around. The current entry is the
 * currently selected song.<BR>
 * e.g. 5 songs in a tune using start song number 3 will result in<BR>
 * [3],4,5,1,2 -&gt; first = 3, length=5, current song is within range 1..5
 *
 * @author Ken Händel
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

	private PlayList() {
	}

	private PlayList(final IConfig config, final SidTune tune) {
		this.config = config;
		this.tune = tune;
		this.current = tune.getInfo().getSelectedSong();
		this.length = tune.getInfo().getSongs();
		this.first = current;
	}

	/**
	 * Get instance results in a new play-list each time the tune changes.
	 *
	 * @param config configuration
	 * @param tune   SID tune
	 * @return play list of the current tune
	 */
	public static PlayList getInstance(final IConfig config, final SidTune tune) {
		if (tune == SidTune.RESET) {
			return NONE;
		}
		if (singleton.tune != tune) {
			singleton = new PlayList(config, tune);
		}
		singleton.tune.getInfo().setSelectedSong(singleton.current);
		singleton.tune.prepare();
		return singleton;
	}

	/**
	 * Get currently selected play list entry.
	 *
	 * @return current song number
	 */
	public int getCurrent() {
		return current;
	}

	/**
	 * Get current track (play list entry relative to the first).
	 *
	 * @return track number
	 */
	public int getTrackNum() {
		int start = current - first + 1;
		return start < 1 ? start + length : start;
	}

	/**
	 * Get number of entries.
	 *
	 * @return number of songs
	 */
	public int getLength() {
		return length;
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
		int last = config.getSidplay2Section().isSingle() ? first : first - 1;
		current = last < 1 ? length : last;
	}

	/**
	 * Is a previous play list entry available?
	 *
	 * @return is a previous song available?
	 */
	public boolean hasPrevious() {
		return current != first;
	}

	/**
	 * Is a next play list entry available?
	 *
	 * @return is a next song available?
	 */
	public boolean hasNext() {
		return getNext() != first;
	}

	/**
	 * Get previous play list entry.
	 *
	 * @return the previous song of the play list
	 */
	public int getPrevious() {
		int previous = config.getSidplay2Section().isSingle() ? current : current - 1;
		return previous < 1 ? length : previous;
	}

	/**
	 * Get next play list entry.
	 *
	 * @return the next song of the play list
	 */
	public int getNext() {
		int next = config.getSidplay2Section().isSingle() ? current : current + 1;
		return next > length ? 1 : next;
	}

	/**
	 * Default play list, if no tune has been loaded.
	 */
	private static final PlayList NONE = new PlayList() {
		@Override
		public int getCurrent() {
			return 1;
		}

		@Override
		public int getTrackNum() {
			return 1;
		}

		@Override
		public int getNext() {
			return 1;
		}

		@Override
		public int getPrevious() {
			return 1;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public boolean hasPrevious() {
			return false;
		}

		@Override
		public int getLength() {
			return 1;
		}

		@Override
		public void first() {
		}

		@Override
		public void last() {
		}

		@Override
		public void next() {
		}

		@Override
		public void previous() {
		}

	};
	/**
	 * Single instance play list per tune.
	 */
	private static PlayList singleton = NONE;
}