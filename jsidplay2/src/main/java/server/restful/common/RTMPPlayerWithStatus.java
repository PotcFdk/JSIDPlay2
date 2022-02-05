package server.restful.common;

import static server.restful.common.IServletSystemProperties.RTMP_EXCEEDS_MAXIMUM_DURATION;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_YET_PLAYED_TIMEOUT;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import sidplay.Player;
import ui.common.filefilter.DiskFileFilter;

public final class RTMPPlayerWithStatus {

	private static final DiskFileFilter DISK_FILE_FILTER = new DiskFileFilter();

	private static enum Status {
		CREATED, ON_PLAY;
	}

	private final LocalDateTime created;

	private final Player player;

	private Status status;

	private File diskImage;

	public RTMPPlayerWithStatus(Player player, File diskImage) {
		this.created = LocalDateTime.now();
		this.player = player;
		this.status = Status.CREATED;
		this.diskImage = diskImage;
	}

	public Player getPlayer() {
		return player;
	}

	public void setOnPlay() {
		status = Status.ON_PLAY;
	}

	public boolean toRemove() {
		return notYetPlayed() || exceedsMaximumDuration();
	}

	private boolean notYetPlayed() {
		return status == Status.CREATED
				&& Duration.between(created, LocalDateTime.now()).getSeconds() > RTMP_NOT_YET_PLAYED_TIMEOUT;
	}

	private boolean exceedsMaximumDuration() {
		return status == Status.ON_PLAY
				&& Duration.between(created, LocalDateTime.now()).getSeconds() > RTMP_EXCEEDS_MAXIMUM_DURATION;
	}

	public void nextDiskImage() {
		if (diskImage != null) {
			List<File> asList = Arrays.asList(diskImage.getParentFile().listFiles(DISK_FILE_FILTER));
			Collections.sort(asList);
			Iterator<File> asListIt = asList.iterator();
			while (asListIt.hasNext()) {
				File siblingFile = asListIt.next();
				if (siblingFile.equals(diskImage) && asListIt.hasNext()) {
					diskImage = asListIt.next();
				}
			}
		}
	}

	public File extract() throws IOException {
		if (diskImage != null) {
			TFile file = new TFile(diskImage);
			if (file.isEntry()) {
				File tmpDir = player.getConfig().getSidplay2Section().getTmpDir();
				File zipEntry = new File(tmpDir, file.getName());
				zipEntry.deleteOnExit();
				TFile.cp_rp(file, zipEntry, TArchiveDetector.ALL);
				return new File(tmpDir, file.getName());
			}
		}
		return diskImage;
	}

}
