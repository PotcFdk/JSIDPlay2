package sidplay.player;

import static java.lang.Thread.MIN_PRIORITY;
import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.io.IOException;

import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.config.IEmulationSection;
import libsidplay.config.ISidPlay2Section;
import libsidplay.config.IWhatsSidSection;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import sidplay.Player;
import sidplay.fingerprinting.WhatsSidSupport;

/**
 * Recognize currently played tune regularly and in parallel. Also responsible
 * to auto-detect chip model and override current player settings.
 * 
 * @author ken
 *
 */
public class WhatsSidEvent extends Event {

	private final Player player;
	private final WhatsSidSupport whatsSidSupport;

	private volatile boolean abort;

	public WhatsSidEvent(Player player, WhatsSidSupport whatsSidSupport) {
		super("WhatsSID?");
		this.player = player;
		this.whatsSidSupport = whatsSidSupport;
		init();
	}

	@Override
	public void event() throws InterruptedException {

		final Thread whatsSidMatcherThread = new Thread(() -> {

			IWhatsSidSection whatsSidSection = player.getConfig().getWhatsSidSection();
			int matchRetryTimeInSeconds = whatsSidSection.getMatchRetryTime();
			try {
				if (!abort && whatsSidSection.isEnable() && player.getFingerPrintMatcher() != null) {
					MusicInfoWithConfidenceBean result = whatsSidSupport.match(player.getFingerPrintMatcher());
					if (result != null) {
						player.getWhatsSidHook().accept(result);
						if (whatsSidSection.isDetectChipModel()) {
							setWhatsSidDetectedChipModel(result);
						}
					}
				}
			} catch (Exception e) {
				// server not available? silently ignore!
			} finally {
				if (!abort) {
					player.getC64().getEventScheduler().schedule(this,
							(long) (matchRetryTimeInSeconds * player.getC64().getClock().getCpuFrequency()));
				}
			}
		});
		whatsSidMatcherThread.setPriority(MIN_PRIORITY);
		whatsSidMatcherThread.start();
	}

	public void setAbort(boolean abort) {
		this.abort = abort;
	}

	private void init() {
		IWhatsSidSection whatsSidSection = player.getConfig().getWhatsSidSection();
		int matchStartTimeInSeconds = whatsSidSection.getMatchStartTime();

		whatsSidSupport.reset();
		Double songLength = player.getSidDatabaseInfo(db -> db.getSongLength(player.getTune()), 0.);
		if (songLength > 0 && songLength < matchStartTimeInSeconds) {
			// song too short? start at 90%
			matchStartTimeInSeconds = Math.min((int) (songLength * 0.9), matchStartTimeInSeconds);
		}
		player.getC64().getEventScheduler().schedule(this,
				(long) (matchStartTimeInSeconds * player.getC64().getClock().getCpuFrequency()));
	}

	private void setWhatsSidDetectedChipModel(MusicInfoWithConfidenceBean result) throws IOException, SidTuneError {
		ISidPlay2Section sidPlay2Section = player.getConfig().getSidplay2Section();
		IEmulationSection emulationSection = player.getConfig().getEmulationSection();

		if (!SidTune.canStoreSidModel(player.getTune())) {
			final String infoDir = result.getMusicInfo().getInfoDir();
			SidTune detectedTune = SidTune.load(PathUtils.getFile(infoDir, sidPlay2Section.getHvsc(), null));

			boolean update = false;
			for (int sidNum = 0; sidNum < MAX_SIDS; sidNum++) {
				ChipModel detectedChipModel = detectedTune.getInfo().getSIDModel(sidNum).asChipModel();

				if (detectedChipModel != null
						&& detectedChipModel != ChipModel.getChipModel(emulationSection, player.getTune(), sidNum)) {
					emulationSection.getOverrideSection().getSidModel()[sidNum] = detectedChipModel;
					update = true;
				}
			}
			if (update) {
				player.updateSIDChipConfiguration();
			}
		}
	}
}