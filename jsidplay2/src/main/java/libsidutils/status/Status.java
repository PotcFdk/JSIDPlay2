package libsidutils.status;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.ResourceBundle;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
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

	public String determineChipModels(boolean withAddress) {
		IEmulationSection emulation = player.getConfig().getEmulationSection();
		StringBuilder line = new StringBuilder();
		if (SidTune.isSIDUsed(emulation, player.getTune(), 0)) {
			determineChipModel(line, 0, withAddress);
			if (SidTune.isSIDUsed(emulation, player.getTune(), 1)) {
				line.append("+");
				determineChipModel(line, 1, withAddress);
				if (SidTune.isSIDUsed(emulation, player.getTune(), 2)) {
					line.append("+");
					determineChipModel(line, 2, withAddress);
				}
			}

		}
		return line.toString();
	}

	public void determineChipModel(StringBuilder line, int sidNum, boolean withAddress) {
		IEmulationSection emulation = player.getConfig().getEmulationSection();

		ChipModel chipModel = ChipModel.getChipModel(emulation, player.getTune(), sidNum);
		int sidBase = SidTune.getSIDAddress(emulation, player.getTune(), sidNum);
		if (withAddress) {
			line.append(String.format("%s(at 0x%4x)", chipModel, sidBase));
		} else {
			line.append(chipModel);
		}
	}

	public String determineEmulations(boolean all) {
		IEmulationSection emulation = player.getConfig().getEmulationSection();
		StringBuilder line = new StringBuilder();
		switch (emulation.getEngine()) {
		case EMULATION:
			line.append(Emulation.getEmulation(emulation, 0).name());
			if (all && SidTune.isSIDUsed(emulation, player.getTune(), 1)) {
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
					if (all & SidTune.isSIDUsed(emulation, player.getTune(), 1)) {
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
			if (all & SidTune.isSIDUsed(emulation, player.getTune(), 1)) {
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

	public void determineEmulation(StringBuilder line, int sidNum) {
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

	public String determinePlayTime(boolean inMillis) {
		double timeInSeconds = player.time();
		if (inMillis) {
			return new SimpleDateFormat("mm:ss.SSS").format(new Date((long) (timeInSeconds * 1000)));
		} else {
			return new SimpleDateFormat("mm:ss").format(new Date((long) (timeInSeconds * 1000)));
		}
	}

	public String determineSongLength() {
		SidTune tune = player.getTune();
		double songLength = tune != null ? player.getSidDatabaseInfo(db -> db.getSongLength(tune), 0.) : 0;
		if (songLength > 0) {
			// song length well-known?
			return new SimpleDateFormat("/mm:ss.SSS").format(new Date((long) (songLength * 1000)));
		}
		return "";
	}

}
