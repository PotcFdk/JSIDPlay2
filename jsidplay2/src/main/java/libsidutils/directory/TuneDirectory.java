package libsidutils.directory;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.siddatabase.SidDatabase;

public class TuneDirectory {

	private static final String TAG_UNKNOWN = "<???>";

	public static Directory getDirectory(File hvscRoot, File tuneFile) throws IOException, SidTuneError {
		Directory dir = new Directory();

		SidTune tune = SidTune.load(tuneFile);
		DoubleSupplier lengthFnct = () -> 0;
		if (hvscRoot != null) {
			SidDatabase db = new SidDatabase(hvscRoot);
			lengthFnct = () -> db.getTuneLength(tune);
		}

		// Directory title: tune title or filename
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
		dir.setTitle(DirEntry.asciiTopetscii(title, 16));
		// Directory id: start song '/' song count
		dir.setId((String.valueOf(info.getStartSong()) + "/" + String.valueOf(Math.max(1, info.getSongs())))
				.getBytes(ISO_8859_1));
		Collection<DirEntry> entries = dir.getDirEntries();

		addProperty(entries, "TITLE", title);
		addProperty(entries, "AUTHOR", author);
		addProperty(entries, "RELEASED", released);
		addProperty(entries, "FORMAT", tune.getClass().getSimpleName());
		addProperty(entries, "PLAYERID", tune.identify().stream().collect(Collectors.joining(",")));
		addProperty(entries, "NO_SONGS", String.valueOf(info.getSongs()));
		addProperty(entries, "STARTSONG", String.valueOf(info.getStartSong()));
		addProperty(entries, "CLOCKFREQ", String.valueOf(info.getClockSpeed()));
		addProperty(entries, "SPEED", String.valueOf(tune.getSongSpeed(1)));
		addProperty(entries, "SIDMODEL1", String.valueOf(info.getSIDModel(0)));
		addProperty(entries, "SIDMODEL2", String.valueOf(info.getSIDModel(1)));
		addProperty(entries, "SIDMODEL3", String.valueOf(info.getSIDModel(2)));
		addProperty(entries, "COMPAT", String.valueOf(info.getCompatibility()));
		addProperty(entries, "TUNE_LGTH", String.valueOf(lengthFnct.getAsDouble()));
		addProperty(entries, "AUDIO",
				info.getSIDChipBase(1) != 0 ? info.getSIDChipBase(2) != 0 ? "3-SID" : "Stereo" : "Mono");
		addProperty(entries, "CHIP_BASE1", String.valueOf(info.getSIDChipBase(0)));
		addProperty(entries, "CHIP_BASE2", String.valueOf(info.getSIDChipBase(1)));
		addProperty(entries, "CHIP_BASE3", String.valueOf(info.getSIDChipBase(2)));
		addProperty(entries, "DRV_ADDR", String.valueOf(info.getDeterminedDriverAddr()));
		addProperty(entries, "LOAD_ADDR", String.valueOf(info.getLoadAddr()));
		addProperty(entries, "LOAD_LGTH", String.valueOf(info.getC64dataLen()));
		addProperty(entries, "INIT_ADDR", String.valueOf(info.getInitAddr()));
		addProperty(entries, "PLY_ADDR", String.valueOf(info.getPlayAddr()));
		addProperty(entries, "FILE_DATE", String.valueOf(
				Instant.ofEpochMilli(tuneFile.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime()));
		addProperty(entries, "SIZE_KB", String.valueOf(tuneFile.length() >> 10));
		addProperty(entries, "SIZE_B", String.valueOf(tuneFile.length()));
		addProperty(entries, "RELOC_PAGE", String.valueOf(info.getRelocStartPage()));
		addProperty(entries, "RELOC_PAGES", String.valueOf(info.getRelocPages()));

		return dir;
	}

	private static void addProperty(Collection<DirEntry> entries, String property, String value) {
		byte[] filename = DirEntry.asciiTopetscii(property + "=" + value, 20);
		// Pseudo directory entry: tune property '=' value
		entries.add(new DirEntry(0, filename, (byte) -1) {
			@Override
			public void save(File autostartFile) throws IOException {
			}
		});
	}
}
