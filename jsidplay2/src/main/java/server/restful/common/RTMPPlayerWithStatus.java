package server.restful.common;

import static server.restful.common.IServletSystemProperties.RTMP_EXCEEDS_MAXIMUM_DURATION;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_YET_PLAYED_TIMEOUT;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import libsidplay.common.Event;
import libsidplay.components.keyboard.KeyTableEntry;
import sidplay.Player;
import ui.common.filefilter.DiskFileFilter;

public final class RTMPPlayerWithStatus {

	private static final DiskFileFilter DISK_FILE_FILTER = new DiskFileFilter();

	private final Player player;

	private File diskImage;

	private final LocalDateTime created;

	private LocalDateTime validUntil;

	public RTMPPlayerWithStatus(Player player, File diskImage) {
		this.player = player;
		this.diskImage = diskImage;
		created = LocalDateTime.now();
		validUntil = LocalDateTime.now().plusSeconds(RTMP_NOT_YET_PLAYED_TIMEOUT);
	}

	public void onPlay() {
		validUntil = created.plusSeconds(RTMP_EXCEEDS_MAXIMUM_DURATION);
	}

	public void onPlayDone() {
		validUntil = LocalDateTime.now().plusSeconds(RTMP_NOT_YET_PLAYED_TIMEOUT);
	}

	public boolean isInvalid() {
		return LocalDateTime.now().isAfter(validUntil);
	}

	public LocalDateTime getValidUntil() {
		return validUntil;
	}

	public void quitPlayer() {
		player.quit();
	}

	public void insertNextDisk() {
		try {
			setNextDiskImage();
			player.insertDisk(extract());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void typeKey(KeyTableEntry key) {
		player.getC64().getEventScheduler()
				.scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Pressed: " + key.name()) {
					@Override
					public void event() throws InterruptedException {
						player.getC64().getKeyboard().keyPressed(key);
						player.getC64().getEventScheduler()
								.scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Released: " + key.name()) {
									@Override
									public void event() throws InterruptedException {
										player.getC64().getKeyboard().keyReleased(key);
									}
								});
					}
				});
	}

	public void pressKey(KeyTableEntry key) {
		player.getC64().getEventScheduler()
				.scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Pressed: " + key.name()) {
					@Override
					public void event() throws InterruptedException {
						player.getC64().getKeyboard().keyPressed(key);
					}
				});
	}

	public void releaseKey(KeyTableEntry key) {
		player.getC64().getEventScheduler()
				.scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Released: " + key.name()) {
					@Override
					public void event() throws InterruptedException {
						player.getC64().getKeyboard().keyReleased(key);
					}
				});
	}

	private void setNextDiskImage() {
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

	private File extract() throws IOException {
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
