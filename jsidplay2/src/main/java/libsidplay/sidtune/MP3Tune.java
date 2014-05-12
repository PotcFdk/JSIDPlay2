package libsidplay.sidtune;

import java.io.ByteArrayInputStream;
import java.io.File;
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

	public static final SidTune load(final File file) throws IOException,
			SidTuneError {
		if (!file.getName().toLowerCase(Locale.ENGLISH).endsWith(".mp3")) {
			return null;
		}
		final MP3Tune sidTune = new MP3Tune();
		// fill out some minimal information of an MP3 tune
		sidTune.mp3Filename = file.getAbsolutePath();
		sidTune.info.startSong = 1;
		sidTune.info.songs = 1;
		try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
			sidTune.decoder.read(randomAccessFile);
			sidTune.info.infoString.add(sidTune.decoder.getTitle());
			String interpret = sidTune.decoder.getInterpret();
			String albumInterpret = sidTune.decoder.getAlbumInterpret();
			String genre = sidTune.decoder.getGenre();
			if (interpret != null) {
				sidTune.info.infoString.add(interpret);
			} else {
				sidTune.info.infoString.add(albumInterpret);
			}
			String album = sidTune.decoder.getAlbum();
			String year = sidTune.decoder.getYear();
			if (album != null && year != null) {
				sidTune.info.infoString.add(album + " (" + year + ")"
						+ (genre != null ? " / " + genre : ""));
			} else {
				sidTune.info.infoString.add(album
						+ (genre != null ? " / " + genre : ""));
			}
			try {
				sidTune.info.startSong = Integer.valueOf(sidTune.decoder
						.getTrack());
			} catch (NumberFormatException e) {
				// ignore
			}
			if (sidTune.decoder.getImageBytes() != null
					&& Platform.isFxApplicationThread()) {
				sidTune.image = new Image(new ByteArrayInputStream(
						sidTune.decoder.getImageBytes()));
			}
		}
		return sidTune;
	}

	@Override
	public String getMD5Digest() {
		throw new RuntimeException(
				"Unsupported operation to create MD5 checksum!");
	}

	@Override
	public Image getImage() {
		return image;
	}

	public String getMP3Filename() {
		return mp3Filename;
	}

}
