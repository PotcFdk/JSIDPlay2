package libsidutils.directory;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static libsidutils.Petscii.iso88591ToPetscii;
import static libsidutils.directory.DirEntry.FILETYPE_NONE;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Model;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.siddatabase.SidDatabase;

/**
 * Pseudo directory to display tune contents.
 * 
 * @author ken
 *
 */
public class TuneDirectory extends Directory {

	private static final int MAXLEN_FILENAME = 20;

	public TuneDirectory(File hvscRoot, File tuneFile) throws IOException, SidTuneError {
		SidTune tune = SidTune.load(tuneFile);
		DoubleSupplier lengthFnct = () -> 0;
		if (hvscRoot != null) {
			SidDatabase db = new SidDatabase(hvscRoot);
			lengthFnct = () -> db.getTuneLength(tune);
		}
		SidTuneInfo info = tune.getInfo();
		Iterator<String> descriptionIt = info.getInfoString().iterator();
		String title = tuneFile.getName();
		String author = null;
		String released = null;
		if (descriptionIt.hasNext()) {
			title = descriptionIt.next();
		}
		if (descriptionIt.hasNext()) {
			author = descriptionIt.next();
		}
		if (descriptionIt.hasNext()) {
			released = descriptionIt.next();
		}
		super.title = iso88591ToPetscii(title.toUpperCase(Locale.US), 16);
		// Directory id: start song '/' song count
		id = (String.valueOf(info.getStartSong()) + "/" + String.valueOf(Math.max(1, info.getSongs())))
				.getBytes(ISO_8859_1);

		if (author != null) {
			dirEntries.add(
					new DirEntry(0, iso88591ToPetscii(author.toUpperCase(Locale.US), MAXLEN_FILENAME), FILETYPE_NONE));
		}
		if (released != null) {
			dirEntries.add(
					new DirEntry(0, iso88591ToPetscii(released.toUpperCase(Locale.US), MAXLEN_FILENAME), FILETYPE_NONE));
		}
		dirEntries.add(
				new DirEntry(0, iso88591ToPetscii("FORMAT" + "=" + tune.getClass().getSimpleName().toUpperCase(Locale.US),
						MAXLEN_FILENAME), FILETYPE_NONE));
		dirEntries
				.add(new DirEntry(0,
						iso88591ToPetscii("PLAYERID" + "="
								+ tune.identify().stream().collect(Collectors.joining(",")).toUpperCase(Locale.US),
								MAXLEN_FILENAME),
						FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("CLOCKFREQ" + "=" + String.valueOf(info.getClockSpeed()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("SPEED" + "=" + String.valueOf(tune.getSongSpeed(1)), MAXLEN_FILENAME), FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("SIDMODEL1" + "=" + String.valueOf(info.getSIDModel(0)), MAXLEN_FILENAME),
				FILETYPE_NONE));
		if (info.getSIDModel(1) != Model.UNKNOWN) {
			dirEntries.add(new DirEntry(0,
					iso88591ToPetscii("SIDMODEL2" + "=" + String.valueOf(info.getSIDModel(1)), MAXLEN_FILENAME),
					FILETYPE_NONE));
		}
		if (info.getSIDModel(2) != Model.UNKNOWN) {
			dirEntries.add(new DirEntry(0,
					iso88591ToPetscii("SIDMODEL3" + "=" + String.valueOf(info.getSIDModel(2)), MAXLEN_FILENAME),
					FILETYPE_NONE));
		}
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("COMPAT" + "=" + String.valueOf(info.getCompatibility()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("TUNE_LGTH" + "=" + String.valueOf(lengthFnct.getAsDouble()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("AUDIO" + "="
						+ (info.getSIDChipBase(1) != 0 ? info.getSIDChipBase(2) != 0 ? "3-SID" : "STEREO" : "MONO"),
						MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("CHIP_BASE1" + "=" + String.valueOf(info.getSIDChipBase(0)), MAXLEN_FILENAME),
				FILETYPE_NONE));
		if (info.getSIDChipBase(1) != 0) {
			dirEntries.add(new DirEntry(0,
					iso88591ToPetscii("CHIP_BASE2" + "=" + String.valueOf(info.getSIDChipBase(1)), MAXLEN_FILENAME),
					FILETYPE_NONE));
		}
		if (info.getSIDChipBase(2) != 0) {
			dirEntries.add(new DirEntry(0,
					iso88591ToPetscii("CHIP_BASE3" + "=" + String.valueOf(info.getSIDChipBase(2)), MAXLEN_FILENAME),
					FILETYPE_NONE));
		}
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("DRV_ADDR" + "=" + String.valueOf(info.getDeterminedDriverAddr()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("LOAD_ADDR" + "=" + String.valueOf(info.getLoadAddr()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("LOAD_LGTH" + "=" + String.valueOf(info.getC64dataLen()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("INIT_ADDR" + "=" + String.valueOf(info.getInitAddr()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(
				new DirEntry(0, iso88591ToPetscii("PLY_ADDR" + "=" + String.valueOf(info.getPlayAddr()), MAXLEN_FILENAME),
						FILETYPE_NONE));
		dirEntries.add(new DirEntry(0, iso88591ToPetscii(
				"FILE_DATE" + "=" + String.valueOf(
						Instant.ofEpochMilli(tuneFile.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime()),
				MAXLEN_FILENAME), FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("SIZE_KB" + "=" + String.valueOf(tuneFile.length() >> 10), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("SIZE_B" + "=" + String.valueOf(tuneFile.length()), MAXLEN_FILENAME), FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("RELOC_PAGE" + "=" + String.valueOf(info.getRelocStartPage()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				iso88591ToPetscii("RELOC_PAGES" + "=" + String.valueOf(info.getRelocPages()), MAXLEN_FILENAME),
				FILETYPE_NONE));
	}
}
