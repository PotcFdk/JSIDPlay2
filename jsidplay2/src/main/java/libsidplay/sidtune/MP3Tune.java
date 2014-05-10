package libsidplay.sidtune;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

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

	private ID3V2Decoder decoder = new ID3V2Decoder();

	private Image image;

	@Override
	public int placeProgramInMemory(byte[] c64buf) {
		// No program to load
		return -1;
	}

	@Override
	public void save(String destFileName, boolean overWriteFlag)
			throws IOException {
		// Saving is not possible
	}

	@Override
	public ArrayList<String> identify() {
		ArrayList<String> names = new ArrayList<String>();
		// The player is called jump3r ;-)
		names.add("jump3r");
		return names;
	}

	@Override
	public long getInitDelay() {
		// MP3 can play immediately
		return 0;
	}

	public static final SidTune load(final File f) throws SidTuneError {
		final MP3Tune s = new MP3Tune();
		// fill out some minimal information of an MP3 tune
		s.info.dataFileLen = (int) f.length();
		s.info.file = f;
		s.info.startSong = 1;
		s.info.songs = 1;
		try (RandomAccessFile randomAccessFile = new RandomAccessFile(f, "r")) {
			s.decoder.read(randomAccessFile);
			s.info.infoString.add(s.decoder.getTitle());
			String interpret = s.decoder.getInterpret();
			String albumInterpret = s.decoder.getAlbumInterpret();
			String genre = s.decoder.getGenre();
			if (interpret != null) {
				s.info.infoString.add(interpret);
			} else {
				s.info.infoString.add(albumInterpret);
			}
			String album = s.decoder.getAlbum();
			String year = s.decoder.getYear();
			if (album != null && year != null) {
				s.info.infoString.add(album + " (" + year + ")"
						+ (genre != null ? " / " + genre : ""));
			} else {
				s.info.infoString.add(album
						+ (genre != null ? " / " + genre : ""));
			}
			try {
				s.info.startSong = Integer.valueOf(s.decoder.getTrack());
			} catch (NumberFormatException e) {
				// ignore
			}
			if (s.decoder.getImageBytes() != null
					&& Platform.isFxApplicationThread()) {
				s.image = new Image(new ByteArrayInputStream(
						s.decoder.getImageBytes()));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return s;
	}

	@Override
	public Image getImage() {
		return image;
	}
}
