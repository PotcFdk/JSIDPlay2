package libsidutils.status;

import static libsidplay.sidtune.SidTune.RESET;

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

	private final Player player;
	private final ResourceBundle resourceBundle;

	public Status(Player player, ResourceBundle resourceBundle) {
		this.player = player;
		this.resourceBundle = resourceBundle;
	}

	public String determineVideoNorm() {
		return CPUClock.getCPUClock(player.getConfig().getEmulationSection(), player.getTune()).name();
	}

	public String determineChipModels() {
		IEmulationSection emulation = player.getConfig().getEmulationSection();

		StringBuilder result = new StringBuilder();
		determineChipModel(result, emulation, 0);
		if (SidTune.isSIDUsed(emulation, player.getTune(), 1)) {
			result.append("+");
			determineChipModel(result, emulation, 1);
			if (SidTune.isSIDUsed(emulation, player.getTune(), 2)) {
				result.append("+");
				determineChipModel(result, emulation, 2);
			}
		}
		return result.toString();
	}

	private void determineChipModel(StringBuilder result, IEmulationSection emulationSection, int sidNum) {
		ChipModel chipModel = ChipModel.getChipModel(emulationSection, player.getTune(), sidNum);
		int sidBase = SidTune.getSIDAddress(emulationSection, player.getTune(), sidNum);
		if (sidBase != SIDChip.DEF_BASE_ADDRESS) {
			result.append(String.format("%s(at 0x%4x)", chipModel, sidBase));
		} else {
			result.append(chipModel);
		}
	}

	public String determineEmulations() {
		IEmulationSection emulation = player.getConfig().getEmulationSection();

		StringBuilder result = new StringBuilder();
		switch (emulation.getEngine()) {
		case EMULATION:
			result.append(Emulation.getEmulation(emulation, 0).name());
			if (SidTune.isSIDUsed(emulation, player.getTune(), 1)) {
				result.append("+");
				result.append(Emulation.getEmulation(emulation, 1).name());
				if (SidTune.isSIDUsed(emulation, player.getTune(), 2)) {
					result.append("+");
					result.append(Emulation.getEmulation(emulation, 2).name());
				}
			}
			break;
		case HARDSID:
		case SIDBLASTER:
		case EXSID:
			Integer deviceCount = player.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceCount(), null);
			if (deviceCount != null) {
				determineEmulation(result, emulation, 0);
				if (SidTune.isSIDUsed(emulation, player.getTune(), 1)) {
					result.append("+");
					determineEmulation(result, emulation, 1);
					if (SidTune.isSIDUsed(emulation, player.getTune(), 2)) {
						result.append("+");
						determineEmulation(result, emulation, 2);
					}
				}
				result.append(" ");
				result.append(String.format(resourceBundle.getString("DEVICES"), deviceCount));
				break;
			}
			// $FALL-THROUGH$
		case NETSID:
			result.append(emulation.getEngine().name());
			if (SidTune.isSIDUsed(emulation, player.getTune(), 1)) {
				result.append("+");
				result.append(emulation.getEngine().name());
				if (SidTune.isSIDUsed(emulation, player.getTune(), 2)) {
					result.append("+");
					result.append(emulation.getEngine().name());
				}
			}
			break;
		default:
			break;
		}
		return result.toString();
	}

	private void determineEmulation(StringBuilder result, IEmulationSection emulationSection, int sidNum) {
		Integer deviceId = player.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceId(sidNum), null);
		String deviceName = player.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceName(sidNum), null);
		ChipModel deviceChipModel = player
				.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceChipModel(sidNum), null);
		if (deviceId != null) {
			result.append(String.format(resourceBundle.getString("DEVICE"), emulationSection.getEngine().name(),
					deviceId, Optional.ofNullable(deviceChipModel).orElse(ChipModel.AUTO),
					Optional.ofNullable(deviceName).orElse("")));
		} else {
			result.append(emulationSection.getEngine().name());
		}
	}

	public String determineTuneSpeed() {
		double tuneSpeed = player.getC64().determineTuneSpeed();
		if (tuneSpeed > 0) {
			return (String.format("%s: %.1fx", resourceBundle.getString("SPEED"), tuneSpeed));
		}
		return "";
	}

	public String determineSong() {
		SidTune tune = player.getTune();
		if (tune != RESET) {
			SidTuneInfo info = tune.getInfo();
			if (info.getSongs() > 1) {
				return String.format("%s: %d/%d", resourceBundle.getString("SONG"), info.getCurrentSong(),
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
			return c1541.getDiskController().isMotorOn() ? "*" : " ";
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
			return datasette.getMotor() ? "+" : " ";
		}
		return "";
	}

	public String determineHeap() {
		Runtime runtime = Runtime.getRuntime();
		return String.format("%s: %sMb/%sMb", resourceBundle.getString("MEMORY"),
				runtime.totalMemory() - runtime.freeMemory() >> 20, runtime.maxMemory() >> 20);
	}

	public String determineTime(boolean showMillis) {
		double timeInSeconds = player.time();
		if (showMillis) {
			return String.format("%s: %s", resourceBundle.getString("TIME"),
					new SimpleDateFormat("mm:ss.SSS").format(new Date((long) (timeInSeconds * 1000))));
		} else {
			return new SimpleDateFormat("mm:ss").format(new Date((long) (timeInSeconds * 1000)));
		}
	}

	public String determineSongLength(boolean showMillis) {
		SidTune tune = player.getTune();
		if (tune != RESET) {
			double songLength = player.getSidDatabaseInfo(db -> db.getSongLength(tune), 0.);
			if (songLength > 0) {
				// song length well-known?
				if (showMillis) {
					return new SimpleDateFormat("mm:ss.SSS").format(new Date((long) (songLength * 1000)));
				} else {
					return new SimpleDateFormat("mm:ss").format(new Date((long) (songLength * 1000)));
				}
			}
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
