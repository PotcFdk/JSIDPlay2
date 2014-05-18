package libsidplay.sidtune;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import javafx.application.Platform;
import javafx.scene.image.Image;
import lowlevel.ID3V2Decoder;

/**
 * Special MP3 tune implementation. This is not a program present in the C64
 * memory, it is just a file played back by the jump3r library. However this C64
 * emulator requires tunes so this is a dummy to meet that requirement.
 * 
 * @author Ken
 * 
 */
public class MP3Tune extends SidTune {

	private String mp3Filename;

	/**
	 * MP3 decoder.
	 */
	private ID3V2Decoder decoder = new ID3V2Decoder();

	/**
	 * Cover art stored inside MP3.
	 */
	private Image image;

	@Override
	public int placeProgramInMemory(byte[] c64buf) {
		// No program to load
		return -1;
	}

	@Override
	public void save(String destFileName, boolean overWriteFlag)
			throws IOException {
		throw new RuntimeException("Saving of this format is not possible!");
	}

	@Override
	public Collection<String> identify() {
		Collection<String> names = new ArrayList<String>();
		// The player is called jump3r ;-)
		names.add("jump3r");
		return names;
	}

	@Override
	public long getInitDelay() {
		// MP3 can play immediately
		return 0;
	}

	public static final SidTune load(final String filename) throws IOException,
			SidTuneError {
		if (!filename.toLowerCase(Locale.ENGLISH).endsWith(".mp3")) {
			throw new SidTuneError("Bad file extension expected: .mp3");
		}
		final MP3Tune mp3 = new MP3Tune();

		mp3.mp3Filename = filename;
		mp3.info.startSong = 1;
		mp3.info.songs = 1;
		try (RandomAccessFile file = new RandomAccessFile(filename, "r")) {
			mp3.decoder.read(file);
			mp3.info.infoString.add(mp3.decoder.getTitle());
			String interpret = mp3.decoder.getInterpret();
			String albumInterpret = mp3.decoder.getAlbumInterpret();
			String genre = mp3.decoder.getGenre();
			if (interpret != null) {
				mp3.info.infoString.add(interpret);
			} else {
				mp3.info.infoString.add(albumInterpret);
			}
			String album = mp3.decoder.getAlbum();
			String year = mp3.decoder.getYear();
			if (album != null && year != null) {
				mp3.info.infoString.add(album + " (" + year + ")"
						+ (genre != null ? " / " + genre : ""));
			} else {
				mp3.info.infoString.add(album
						+ (genre != null ? " / " + genre : ""));
			}
			if (mp3.decoder.getImageBytes() != null
					&& Platform.isFxApplicationThread()) {
				mp3.image = new Image(new ByteArrayInputStream(
						mp3.decoder.getImageBytes()));
			}
		}
		return mp3;
	}

	@Override
	public String getMD5Digest() {
		return null;
	}

	@Override
	public Image getImage() {
		return image;
	}

	public String getMP3Filename() {
		return mp3Filename;
	}

}
