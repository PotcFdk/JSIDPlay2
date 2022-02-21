package libsidutils.status;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.SIDChip;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import sidplay.Player;

public class Status {

	private Player player;
	private ResourceBundle resourceBundle;

	public Status(Player player, ResourceBundle resourceBundle) {
		this.player = player;
		this.resourceBundle = resourceBundle;
	}

	public String determineVideoNorm() {
		return CPUClock.getCPUClock(player.getConfig().getEmulationSection(), player.getTune()).name();
	}

	public String determineChipModels() {
		IEmulationSection emulation = player.getConfig().getEmulationSection();
		StringBuilder line = new StringBuilder();
		if (SidTune.isSIDUsed(emulation, player.getTune(), 0)) {
			determineChipModel(line, 0);
			if (SidTune.isSIDUsed(emulation, player.getTune(), 1)) {
				line.append("+");
				determineChipModel(line, 1);
				if (SidTune.isSIDUsed(emulation, player.getTune(), 2)) {
					line.append("+");
					determineChipModel(line, 2);
				}
			}

		}
		return line.toString();
	}

	private void determineChipModel(StringBuilder line, int sidNum) {
		IEmulationSection emulation = player.getConfig().getEmulationSection();

		ChipModel chipModel = ChipModel.getChipModel(emulation, player.getTune(), sidNum);
		int sidBase = SidTune.getSIDAddress(emulation, player.getTune(), sidNum);
		if (sidBase != SIDChip.DEF_BASE_ADDRESS) {
			line.append(String.format("%s(at 0x%4x)", chipModel, sidBase));
		} else {
			line.append(chipModel);
		}
	}

	public String determineEmulations() {
		IEmulationSection emulation = player.getConfig().getEmulationSection();
		StringBuilder line = new StringBuilder();
		switch (emulation.getEngine()) {
		case EMULATION:
			line.append(Emulation.getEmulation(emulation, 0).name());
			if (SidTune.isSIDUsed(emulation, player.getTune(), 1)) {
				String stereoEmulation = Emulation.getEmulation(emulation, 1).name();
				line.append("+");
				line.append(stereoEmulation);
				if (SidTune.isSIDUsed(emulation, player.getTune(), 2)) {
					String thirdEmulation = Emulation.getEmulation(emulation, 2).name();
					line.append("+");
					line.append(thirdEmulation);
				}
			}
			break;
		case HARDSID:
		case SIDBLASTER:
		case EXSID:
			Integer deviceCount = player.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceCount(), null);
			if (deviceCount != null) {
				if (SidTune.isSIDUsed(emulation, player.getTune(), 0)) {
					determineEmulation(line, 0);
					if (SidTune.isSIDUsed(emulation, player.getTune(), 1)) {
						line.append("+");
						determineEmulation(line, 1);
						if (SidTune.isSIDUsed(emulation, player.getTune(), 2)) {
							line.append("+");
							determineEmulation(line, 2);
						}
					}
				}
				line.append(" ").append(String.format(resourceBundle.getString("DEVICES"), deviceCount));
				break;
			}
			// $FALL-THROUGH$
		case NETSID:
			line.append(emulation.getEngine().name());
			if (SidTune.isSIDUsed(emulation, player.getTune(), 1)) {
				line.append("+");
				line.append(emulation.getEngine().name());
				if (SidTune.isSIDUsed(emulation, player.getTune(), 2)) {
					line.append("+");
					line.append(emulation.getEngine().name());
				}
			}
			break;
		default:
			break;
		}
		return line.toString();
	}

	private void determineEmulation(StringBuilder line, int sidNum) {
		IEmulationSection emulation = player.getConfig().getEmulationSection();

		Integer deviceId = player.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceId(sidNum), null);
		String deviceName = player.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceName(sidNum), null);
		ChipModel deviceChipModel = player
				.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceChipModel(sidNum), null);
		if (deviceId != null) {
			line.append(String.format(resourceBundle.getString("DEVICE"), emulation.getEngine().name(), deviceId,
					Optional.ofNullable(deviceChipModel).orElse(ChipModel.AUTO),
					Optional.ofNullable(deviceName).orElse("")));
		} else {
			line.append(emulation.getEngine().name());
		}
	}

	public String determineTuneSpeed() {
		double tuneSpeed = player.getC64().determineTuneSpeed();
		if (tuneSpeed > 0) {
			return (String.format(", %s: %.1fx", resourceBundle.getString("SPEED"), tuneSpeed));
		}
		return "";
	}

	public String determineSong() {
		SidTune tune = player.getTune();
		if (tune != null) {
			SidTuneInfo info = tune.getInfo();
			if (info.getSongs() > 1) {
				return String.format(", %s: %d/%d", resourceBundle.getString("SONG"), info.getCurrentSong(),
						info.getSongs());
			}
		}
		return "";
	}

	public String determineDiskActivity(boolean showTrack) {
		C1541 c1541 = player.getFloppies()[0];
		if (showTrack) {
			int halfTrack = c1541.getDiskController().getHalfTrack();
			if (c1541.getDiskController().isMotorOn()) {
				return String.format("%s: %02d", resourceBundle.getString("FLOPPY_TRACK"), halfTrack >> 1);
			}
		} else {
			if (c1541.getDiskController().isMotorOn()) {
				return "*";
			} else {
				return " ";
			}
		}
		return "";
	}

	public String determineTapeActivity(boolean showCounter) {
		Datasette datasette = player.getDatasette();
		if (showCounter) {
			if (datasette.getMotor()) {
				return String.format("%s: %03d", resourceBundle.getString("DATASETTE_COUNTER"), datasette.getCounter());
			}
		} else {
			if (datasette.getMotor()) {
				return "+";
			} else {
				return " ";
			}
		}
		return "";
	}

	public String determineHeap() {
		Runtime runtime = Runtime.getRuntime();
		return String.format("%s: %sMb/%sMb", resourceBundle.getString("MEMORY"),
				runtime.totalMemory() - runtime.freeMemory() >> 20, runtime.maxMemory() >> 20);
	}

	public String determineTime(boolean longFormat) {
		double timeInSeconds = player.time();
		if (longFormat) {
			return String.format("%s: %s", resourceBundle.getString("TIME"),
					new SimpleDateFormat("mm:ss.SSS").format(new Date((long) (timeInSeconds * 1000))));
		} else {
			return new SimpleDateFormat("mm:ss").format(new Date((long) (timeInSeconds * 1000)));
		}
	}

	public String determineSongLength() {
		SidTune tune = player.getTune();
		double songLength = tune != null ? player.getSidDatabaseInfo(db -> db.getSongLength(tune), 0.) : 0;
		if (songLength > 0) {
			// song length well-known?
			return new SimpleDateFormat("mm:ss.SSS").format(new Date((long) (songLength * 1000)));
		}
		return "";
	}

	public String determineRecording() {
		if (player.getAudioDriver().isRecording()) {
			return String.format("%s: %s (%s)", resourceBundle.getString("RECORDING_FILENAME"),
					player.getRecordingFilename(), getFileSize(new File(player.getRecordingFilename()).length()));
		}
		return "";
	}

	private String getFileSize(long size) {
		if (size <= 0) {
			return "0 b";
		}
		final String[] units = new String[] { "b", "Kb", "Mb", "Gb", "Tb" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
}
