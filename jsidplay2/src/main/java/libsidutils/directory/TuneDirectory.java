package libsidutils.directory;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static libsidutils.Petscii.stringToPetscii;
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
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.siddatabase.SidDatabase;

public class TuneDirectory extends Directory {

	private static final int MAXLEN_FILENAME = 20;
	private static final String TAG_UNKNOWN = "<???>";

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
		String author = TAG_UNKNOWN;
		String released = TAG_UNKNOWN;
		if (descriptionIt.hasNext()) {
			title = descriptionIt.next();
		}
		if (descriptionIt.hasNext()) {
			author = descriptionIt.next();
		}
		if (descriptionIt.hasNext()) {
			released = descriptionIt.next();
		}
		super.title = stringToPetscii(title, 16);
		// Directory id: start song '/' song count
		id = (String.valueOf(info.getStartSong()) + "/" + String.valueOf(Math.max(1, info.getSongs())))
				.getBytes(ISO_8859_1);

		dirEntries.add(new DirEntry(0, stringToPetscii(title.toUpperCase(Locale.US), MAXLEN_FILENAME), FILETYPE_NONE));
		dirEntries.add(new DirEntry(0, stringToPetscii(author.toUpperCase(Locale.US), MAXLEN_FILENAME), FILETYPE_NONE));
		dirEntries
				.add(new DirEntry(0, stringToPetscii(released.toUpperCase(Locale.US), MAXLEN_FILENAME), FILETYPE_NONE));
		dirEntries.add(
				new DirEntry(0, stringToPetscii("FORMAT" + "=" + tune.getClass().getSimpleName().toUpperCase(Locale.US),
						MAXLEN_FILENAME), FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("PLAYERID" + "=" + tune.identify().stream().collect(Collectors.joining(",")),
						MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("NO_SONGS" + "=" + String.valueOf(info.getSongs()), MAXLEN_FILENAME), FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("STARTSONG" + "=" + String.valueOf(info.getStartSong()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("CLOCKFREQ" + "=" + String.valueOf(info.getClockSpeed()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("SPEED" + "=" + String.valueOf(tune.getSongSpeed(1)), MAXLEN_FILENAME), FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("SIDMODEL1" + "=" + String.valueOf(info.getSIDModel(0)), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("SIDMODEL2" + "=" + String.valueOf(info.getSIDModel(1)), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("SIDMODEL3" + "=" + String.valueOf(info.getSIDModel(2)), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("COMPAT" + "=" + String.valueOf(info.getCompatibility()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("TUNE_LGTH" + "=" + String.valueOf(lengthFnct.getAsDouble()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("AUDIO" + "="
						+ (info.getSIDChipBase(1) != 0 ? info.getSIDChipBase(2) != 0 ? "3-SID" : "STEREO" : "MONO"),
						MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("CHIP_BASE1" + "=" + String.valueOf(info.getSIDChipBase(0)), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("CHIP_BASE2" + "=" + String.valueOf(info.getSIDChipBase(1)), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("CHIP_BASE3" + "=" + String.valueOf(info.getSIDChipBase(2)), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("DRV_ADDR" + "=" + String.valueOf(info.getDeterminedDriverAddr()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("LOAD_ADDR" + "=" + String.valueOf(info.getLoadAddr()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("LOAD_LGTH" + "=" + String.valueOf(info.getC64dataLen()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("INIT_ADDR" + "=" + String.valueOf(info.getInitAddr()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(
				new DirEntry(0, stringToPetscii("PLY_ADDR" + "=" + String.valueOf(info.getPlayAddr()), MAXLEN_FILENAME),
						FILETYPE_NONE));
		dirEntries.add(new DirEntry(0, stringToPetscii(
				"FILE_DATE" + "=" + String.valueOf(
						Instant.ofEpochMilli(tuneFile.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime()),
				MAXLEN_FILENAME), FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("SIZE_KB" + "=" + String.valueOf(tuneFile.length() >> 10), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("SIZE_B" + "=" + String.valueOf(tuneFile.length()), MAXLEN_FILENAME), FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("RELOC_PAGE" + "=" + String.valueOf(info.getRelocStartPage()), MAXLEN_FILENAME),
				FILETYPE_NONE));
		dirEntries.add(new DirEntry(0,
				stringToPetscii("RELOC_PAGES" + "=" + String.valueOf(info.getRelocPages()), MAXLEN_FILENAME),
				FILETYPE_NONE));
	}
}
