package libsidplay.sidtune;

import static java.util.Optional.ofNullable;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import libsidutils.PathUtils;
import libsidutils.sidid.SidIdInfo.PlayerInfoSection;
import lowlevel.ID3V2Decoder;

/**
 * Special MP3 tune implementation. This is not a program present in the C64
 * memory, it is just a file played back by the jump3r library. However this C64
 * emulator requires tunes so this is a dummy to meet that requirement.
 *
 * @author Ken Händel
 *
 */
public class MP3Tune extends SidTune {

	private String mp3Filename;

	/**
	 * MP3 decoder.
	 */
	private ID3V2Decoder decoder = new ID3V2Decoder();

	@Override
	public Integer placeProgramInMemory(byte[] c64buf) {
		// No program to load
		return null;
	}

	@Override
	public void save(String destFileName) throws IOException {
		throw new RuntimeException("Saving of this format is not possible!");
	}

	@Override
	public Collection<String> identify() {
		Collection<String> names = new ArrayList<>();
		// The player is called jump3r ;-)
		names.add("jump3r");
		return names;
	}

	@Override
	public PlayerInfoSection getPlayerInfo(String playerName) {
		// unused
		return null;
	}

	public static final SidTune load(final File file) throws IOException, SidTuneError {
		if (!file.getName().toLowerCase(Locale.ENGLISH).endsWith(".mp3")) {
			throw new SidTuneError("MP3: Bad file extension, expected: .mp3");
		}
		final MP3Tune mp3 = new MP3Tune();

		mp3.mp3Filename = file.getAbsolutePath();
		try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
			mp3.decoder.read(randomAccessFile);

			String title = ofNullable(mp3.decoder.getTitle()).map(MP3Tune::replaceNonPrintable).orElse(null);
			String interpret = ofNullable(mp3.decoder.getInterpret()).map(MP3Tune::replaceNonPrintable).orElse(null);
			String albumInterpret = ofNullable(mp3.decoder.getAlbumInterpret()).map(MP3Tune::replaceNonPrintable)
					.orElse(null);
			String genre = ofNullable(mp3.decoder.getGenre()).map(MP3Tune::replaceNonPrintable).orElse(null);
			String album = ofNullable(mp3.decoder.getAlbum()).map(MP3Tune::replaceNonPrintable).orElse(null);
			String year = ofNullable(mp3.decoder.getYear()).map(MP3Tune::replaceNonPrintable).orElse(null);

			mp3.info.infoString.add(title != null ? title : PathUtils.getFilenameWithoutSuffix(file.getName()));
			if (interpret != null) {
				mp3.info.infoString.add(interpret);
			} else if (albumInterpret != null) {
				mp3.info.infoString.add(albumInterpret);
			} else {
				mp3.info.infoString.add("<?>");
			}
			if (album != null && year != null) {
				mp3.info.infoString.add(album + " (" + year + ")" + (genre != null ? " / " + genre : ""));
			} else if (album != null) {
				mp3.info.infoString.add(album + (genre != null ? " / " + genre : ""));
			} else {
				mp3.info.infoString.add("<?>");
			}
		}
		return mp3;
	}

	/**
	 * Get cover art bitmap.
	 *
	 * @return cover art bitmap
	 */
	public byte[] getCoverArt() {
		return decoder.getImageBytes();
	}

	@Override
	public String getMD5Digest(MD5Method md5Method) {
		// unused
		return null;
	}

	public String getMP3Filename() {
		return mp3Filename;
	}

	@Override
	protected long getInitDelay() {
		// MP3 can play immediately
		return 0;
	}

	private static String replaceNonPrintable(String str) {
		return str.replaceAll("\\p{Cntrl}", "");
	}
}
