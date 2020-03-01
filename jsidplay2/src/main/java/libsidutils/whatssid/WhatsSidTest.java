package libsidutils.whatssid;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import libsidplay.config.IEmulationSection;
import libsidplay.config.ISidPlay2Section;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import sidplay.Player;
import sidplay.audio.Audio;
import sidplay.ini.IniConfig;

/**
 * Beta: Shazam like feature: Analyze tunes to recognize a currently played tune
 * 
 * This is the test program.
 * 
 * @author ken
 *
 */
@Parameters(resourceBundle = "libsidutils.whatssid.WhatsSidTest")
public class WhatsSidTest {

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(descriptionKey = "DIRECTORY", required = true)
	private String directoryPath;

	public static void main(String[] args) throws IOException, SidTuneError {
		new WhatsSidTest().test(args);
	}

	private void test(String[] args) throws IOException, SidTuneError {
		JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
		commander.parse(args);
		if (help) {
			commander.usage();
			System.out.println("Press <enter> to exit!");
			System.in.read();
			System.exit(0);
		}
		IniConfig config = new IniConfig(false);

		ISidPlay2Section sidplay2Section = config.getSidplay2Section();
		IAudioSection audioSection = config.getAudioSection();
		IEmulationSection emulationSection = config.getEmulationSection();

		sidplay2Section.setHvsc(null);
		sidplay2Section.setDefaultPlayLength(30);
		audioSection.setAudio(Audio.WHATS_SID);
		audioSection.setAudioBufferSize(1024);
		audioSection.setSamplingRate(SamplingRate.MEDIUM);
		emulationSection.setFilterName(0, Engine.EMULATION, Emulation.RESIDFP, ChipModel.MOS6581, "FilterReSID6581");

		Player player = new Player(config);
		player.setSidDatabase(null);

		System.out.println();
		System.out.println("Analyzing phase...");
		File dir = new File(directoryPath);
		for (File file : dir.listFiles()) {
			player.setRecordingFilenameProvider(tune -> PathUtils.getFilenameWithoutSuffix(file.getAbsolutePath()));
			player.play(SidTune.load(file));
			player.stopC64(false);
		}

		System.out.println();
		System.out.println("Matching phase...");
		audioSection.setAudio(Audio.WHATS_SID_MATCHER);
		audioSection.setAudioBufferSize(1024);
		audioSection.setSamplingRate(SamplingRate.MEDIUM);
		for (File file : dir.listFiles()) {
			player.setRecordingFilenameProvider(tune -> PathUtils.getFilenameWithoutSuffix(file.getAbsolutePath()));
			player.play(SidTune.load(file));
			player.stopC64(false);
		}

	}
}
