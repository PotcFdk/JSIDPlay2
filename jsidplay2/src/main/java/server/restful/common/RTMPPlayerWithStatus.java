package server.restful.common;

import static server.restful.common.IServletSystemProperties.RTMP_EXCEEDS_MAXIMUM_DURATION;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_YET_PLAYED_TIMEOUT;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.ResourceBundle;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Event;
import libsidplay.components.keyboard.KeyTableEntry;
import libsidutils.status.Status;
import sidplay.Player;
import sidplay.audio.AudioDriver;
import sidplay.audio.ProxyDriver;
import sidplay.audio.xuggle.XuggleVideoDriver;
import sidplay.player.State;
import ui.common.filefilter.DiskFileFilter;

public final class RTMPPlayerWithStatus {

	private static final DiskFileFilter DISK_FILE_FILTER = new DiskFileFilter();

	private final Player player;

	private final Status status;

	private File diskImage;

	private final LocalDateTime created;

	private LocalDateTime validUntil;

	private int playCounter, counter;

	public RTMPPlayerWithStatus(Player player, File diskImage, ResourceBundle resourceBundle) {
		this.player = player;
		this.diskImage = diskImage;
		status = new Status(player, resourceBundle);
		created = LocalDateTime.now();
		validUntil = created.plusSeconds(RTMP_NOT_YET_PLAYED_TIMEOUT);
		addStatusTextListener();
	}

	public void onPlay() {
		++playCounter;
		validUntil = created.plusSeconds(RTMP_EXCEEDS_MAXIMUM_DURATION);
	}

	public void onPlayDone() {
		if (--playCounter == 0) {
			validUntil = LocalDateTime.now().plusSeconds(RTMP_NOT_YET_PLAYED_TIMEOUT);
		}
	}

	public boolean isInvalid() {
		return validUntil.isBefore(LocalDateTime.now());
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

	public void setDefaultSidModel6581() {
		player.getConfig().getEmulationSection().setDefaultSidModel(ChipModel.MOS6581);
		player.updateSIDChipConfiguration();
	}

	public void setDefaultSidModel8580() {
		player.getConfig().getEmulationSection().setDefaultSidModel(ChipModel.MOS8580);
		player.updateSIDChipConfiguration();
	}

	public void setDefaultEmulationReSid() {
		player.getConfig().getEmulationSection().setDefaultEmulation(Emulation.RESID);
		player.updateSIDChipConfiguration();
	}

	public void setDefaultEmulationReSidFp() {
		player.getConfig().getEmulationSection().setDefaultEmulation(Emulation.RESIDFP);
		player.updateSIDChipConfiguration();
	}

	public void typeKey(KeyTableEntry key) {
		player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Pressed") {
			@Override
			public void event() throws InterruptedException {
				player.getC64().getKeyboard().keyPressed(key);

				player.getC64().getEventScheduler()
						.scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Released") {
							@Override
							public void event() throws InterruptedException {
								player.getC64().getKeyboard().keyReleased(key);
							}
						});
			}
		});
	}

	public void pressKey(KeyTableEntry key) {
		player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Pressed") {
			@Override
			public void event() throws InterruptedException {
				player.getC64().getKeyboard().keyPressed(key);
			}
		});
	}

	public void releaseKey(KeyTableEntry key) {
		player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Released") {
			@Override
			public void event() throws InterruptedException {
				player.getC64().getKeyboard().keyReleased(key);
			}
		});
	}

	public void joystick(int number, int value) {
		player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(new Event("Virtual Joystick Pressed") {
			@Override
			public void event() throws InterruptedException {
				player.getC64().setJoystick(number, () -> (byte) (0xff ^ value));

				player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(new Event("Virtual Joystick Released") {
					@Override
					public void event() throws InterruptedException {
						player.getC64().setJoystick(number, () -> (byte) 0xff);
					}
				});
			}
		});
	}

	private void setNextDiskImage() {
		if (diskImage != null) {
			Iterator<File> asListIt = Arrays.asList(
					Optional.ofNullable(diskImage.getParentFile().listFiles(DISK_FILE_FILTER)).orElse(new File[0]))
					.stream().sorted().iterator();
			while (asListIt.hasNext()) {
				File siblingFile = asListIt.next();
				if (siblingFile.equals(diskImage) && asListIt.hasNext()) {
					diskImage = asListIt.next();
					break;
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

	private void addStatusTextListener() {
		player.stateProperty().addListener(event -> {
			if (event.getNewValue() == State.START) {

				player.getC64().getEventScheduler().schedule(new Event("Update Status Text") {
					@Override
					public void event() throws InterruptedException {

						getXuggleVideoDriver().ifPresent(xuggleVideoDriver -> {
							xuggleVideoDriver.setStatusText(createStatusText());
							int statusTextOffset = xuggleVideoDriver.getStatusTextOffset();
							int statusTextOverflow = xuggleVideoDriver.getStatusTextOverflow();

							// scroll forward after some time
							if (counter++ > 10) {
								if (statusTextOverflow > 0) {
									xuggleVideoDriver.setStatusTextOffset(statusTextOffset + 8);
								}
							}
							// reset scroll status if scroll has finished
							if (statusTextOverflow == 0) {
								counter = 0;
								xuggleVideoDriver.setStatusTextOffset(0);
							}
						});
						player.getC64().getEventScheduler().schedule(this,
								(long) (player.getC64().getClock().getCpuFrequency()));
					}
				}, 0);
			}
		});
	}

	private Optional<XuggleVideoDriver> getXuggleVideoDriver() {
		AudioDriver audioDriver = player.getAudioDriver();
		if (audioDriver instanceof ProxyDriver) {
			ProxyDriver proxyDriver = (ProxyDriver) audioDriver;
			if (proxyDriver.getDriverTwo() instanceof XuggleVideoDriver) {
				return Optional.of((XuggleVideoDriver) proxyDriver.getDriverTwo());
			}
		}
		return Optional.empty();
	}

	private String createStatusText() {
		StringBuilder result = new StringBuilder();

		String determinePSID64 = status.determinePSID64();
		result.append(status.determineTime(false));
		result.append(", ");
		result.append(status.determineVideoNorm());
		result.append(", ");
		result.append(status.determineChipModels());
		result.append(", ");
		result.append(status.determineEmulations());
		result.append(determinePSID64.isEmpty() ? "" : ", " + determinePSID64);
		result.append(", ");
		result.append(status.determineTapeActivity(false));
		result.append(status.determineDiskActivity(false));
		result.append(diskImage.getName());

		return result.toString();
	}

}
