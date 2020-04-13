package sidplay.consoleplayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.ResourceBundle;

import libsidplay.common.ChipModel;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import sidplay.Player;
import sidplay.fingerprinting.MusicInfoWithConfidenceBean;
import sidplay.ini.IniConfig;
import sidplay.ini.IniConsoleSection;
import sidplay.player.PlayList;
import sidplay.player.Timer;

public class ConsoleIO {

	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("sidplay.consoleplayer.ConsoleIO");

	private IniConfig config;

	private String filename;

	public ConsoleIO(IniConfig config, String filename) {
		this.config = config;
		this.filename = filename;
	}

	public void menu(Player player, int verboseLevel, boolean quiet, PrintStream out) {
		if (quiet) {
			return;
		}
		final IniConsoleSection console = config.getConsoleSection();

		final SidTune tune = player.getTune();
		final PlayList playList = player.getPlayList();
		final Timer timer = player.getTimer();

		printTopLine(out, console);
		printHeading(out, console);
		printSeparatorLine(out, console);
		if (tune.getInfo().getInfoString().size() == 3) {
			printTitleAutorReleased(out, console, tune);
		} else {
			printDecription(out, console, tune);
		}
		if (tune.getInfo().getInfoString().size() > 0) {
			printSeparatorLine(out, console);
		}
		if (verboseLevel != 0) {
			printFileDetails(out, console, tune);
		}
		printPlaylist(out, console, tune, playList);

		if (verboseLevel > 0) {
			printHorizontalBottomLine(out, console);
			printSongSpeedCIAorVBI(out, console, tune, playList);
		}
		printSongLength(out, console, timer);
		if (verboseLevel > 0) {
			printHorizontalBottomLine(out, console);
			printAddresses(out, console, tune);
			printSIDDetails(out, console, tune);
		}
		printHorizontalBottomLine(out, console);
		printKeyboardControls(out);
	}

	public void decodeKeys(Player player, InputStream in) {
		try {
			if (System.in.available() == 0) {
				return;
			}
			final int key = in.read();
			IEmulationSection emulation = config.getEmulationSection();
			switch (key) {
			case 'h':
				player.firstSong();
				break;

			case 'e':
				player.lastSong();
				break;

			case '>':
				if (player.getPlayList().hasNext()) {
					player.nextSong();
				}
				break;

			case '<':
				if (player.getPlayList().hasPrevious()) {
					player.previousSong();
				}
				break;

			case '.':
				player.configureMixer(mixer -> mixer.fastForward());
				break;

			case ',':
				player.configureMixer(mixer -> mixer.normalSpeed());
				break;

			case 'p':
				player.pauseContinue();
				break;

			case '1': {
				boolean isMuteVoice = emulation.isMuteVoice1() ^ true;
				emulation.setMuteVoice1(isMuteVoice);
				player.configureSID(0, sid -> sid.setVoiceMute(0, isMuteVoice));
				break;
			}

			case '2': {
				boolean isMuteVoice = emulation.isMuteVoice2() ^ true;
				emulation.setMuteVoice2(isMuteVoice);
				player.configureSID(0, sid -> sid.setVoiceMute(1, isMuteVoice));
				break;
			}

			case '3': {
				boolean isMuteVoice = emulation.isMuteVoice3() ^ true;
				emulation.setMuteVoice3(isMuteVoice);
				player.configureSID(0, sid -> sid.setVoiceMute(2, isMuteVoice));
				break;
			}

			case '4': {
				boolean isMuteVoice = emulation.isMuteVoice4() ^ true;
				emulation.setMuteVoice4(isMuteVoice);
				player.configureSID(0, sid -> sid.setVoiceMute(3, isMuteVoice));
				break;
			}

			case '5': {
				boolean isMuteVoice = emulation.isMuteStereoVoice1() ^ true;
				emulation.setMuteStereoVoice1(isMuteVoice);
				player.configureSID(1, sid -> sid.setVoiceMute(0, isMuteVoice));
				break;
			}

			case '6': {
				boolean isMuteVoice = emulation.isMuteStereoVoice2() ^ true;
				emulation.setMuteStereoVoice2(isMuteVoice);
				player.configureSID(1, sid -> sid.setVoiceMute(1, isMuteVoice));
				break;
			}

			case '7': {
				boolean isMuteVoice = emulation.isMuteStereoVoice3() ^ true;
				emulation.setMuteStereoVoice3(isMuteVoice);
				player.configureSID(1, sid -> sid.setVoiceMute(2, isMuteVoice));
				break;
			}

			case '8': {
				boolean isMuteVoice = emulation.isMuteStereoVoice4() ^ true;
				emulation.setMuteStereoVoice4(isMuteVoice);
				player.configureSID(1, sid -> sid.setVoiceMute(3, isMuteVoice));
				break;
			}

			case '9': {
				boolean isMuteVoice = emulation.isMuteThirdSIDVoice1() ^ true;
				emulation.setMuteThirdSIDVoice1(isMuteVoice);
				player.configureSID(2, sid -> sid.setVoiceMute(0, isMuteVoice));
				break;
			}

			case 'a': {
				boolean isMuteVoice = emulation.isMuteThirdSIDVoice2() ^ true;
				emulation.setMuteThirdSIDVoice2(isMuteVoice);
				player.configureSID(2, sid -> sid.setVoiceMute(1, isMuteVoice));
				break;
			}

			case 'b': {
				boolean isMuteVoice = emulation.isMuteThirdSIDVoice3() ^ true;
				emulation.setMuteThirdSIDVoice3(isMuteVoice);
				player.configureSID(2, sid -> sid.setVoiceMute(2, isMuteVoice));
				break;
			}

			case 'c': {
				boolean isMuteVoice = emulation.isMuteThirdSIDVoice4() ^ true;
				emulation.setMuteThirdSIDVoice4(isMuteVoice);
				player.configureSID(2, sid -> sid.setVoiceMute(3, isMuteVoice));
				break;
			}

			case 'f': {
				boolean filterEnable = emulation.isFilter() ^ true;
				emulation.setFilter(filterEnable);
				player.configureSID(0, sid -> sid.setFilterEnable(emulation, 0));
				break;
			}

			case 'g': {
				boolean filterEnable = emulation.isStereoFilter() ^ true;
				emulation.setStereoFilter(filterEnable);
				player.configureSID(1, sid -> sid.setFilterEnable(emulation, 1));
				break;
			}

			case 'G': {
				boolean filterEnable = emulation.isThirdSIDFilter() ^ true;
				emulation.setThirdSIDFilter(filterEnable);
				player.configureSID(2, sid -> sid.setFilterEnable(emulation, 2));
				break;
			}

			case 'q':
				player.quit();
				break;

			default:
				break;
			}
		} catch (final IOException e) {
			player.quit();
		}
	}

	public void whatsSid(MusicInfoWithConfidenceBean musicInfoWithConfidence, boolean quiet, PrintStream out) {
		if (quiet) {
			return;
		}
		out.println("WhatsSid? " + musicInfoWithConfidence.toString());
	}

	private void printTopLine(PrintStream out, final IniConsoleSection console) {
		out.printf("%c%s%c\n", console.getTopLeft(), setFill(console.getHorizontal(), 54), console.getTopRight());
	}

	private void printHeading(PrintStream out, final IniConsoleSection console) {
		out.printf("%c%52s  %c\n", console.getVertical(), BUNDLE.getString("HEADING"), console.getVertical());
	}

	private void printSeparatorLine(PrintStream out, final IniConsoleSection console) {
		out.printf("%c%s%c\n", console.getJunctionLeft(), setFill(console.getHorizontal(), 54),
				console.getJunctionRight());
	}

	private void printTitleAutorReleased(PrintStream out, final IniConsoleSection console, final SidTune tune) {
		Iterator<String> description = tune.getInfo().getInfoString().iterator();
		out.printf("%c %-12s : %37s %c\n", console.getVertical(), BUNDLE.getString("TITLE"), description.next(),
				console.getVertical());
		out.printf("%c %-12s : %37s %c\n", console.getVertical(), BUNDLE.getString("AUTHOR"), description.next(),
				console.getVertical());
		out.printf("%c %-12s : %37s %c\n", console.getVertical(), BUNDLE.getString("RELEASED"), description.next(),
				console.getVertical());
	}

	private void printDecription(PrintStream out, final IniConsoleSection console, final SidTune tune) {
		for (String description : tune.getInfo().getInfoString()) {
			out.printf("%c %-12s : %37s %c\n", console.getVertical(), BUNDLE.getString("DESCRIPTION"), description,
					console.getVertical());
		}
	}

	private void printFileDetails(PrintStream out, final IniConsoleSection console, final SidTune tune) {
		out.printf("%c %-12s : %37s %c\n", console.getVertical(), BUNDLE.getString("FILE_FORMAT"),
				tune.getInfo().getClass().getSimpleName(), console.getVertical());
		out.printf("%c %-12s : %37s %c\n", console.getVertical(), BUNDLE.getString("FILENAMES"),
				new File(filename).getName(), console.getVertical());
	}

	private void printPlaylist(PrintStream out, final IniConsoleSection console, final SidTune tune,
			final PlayList playList) {
		int trackNum = playList.getTrackNum();
		out.printf("%c %-12s : ", console.getVertical(), BUNDLE.getString("PLAYLIST"));
		StringBuffer trackList = new StringBuffer();
		trackList.append(trackNum).append("/").append(playList.getLength());
		trackList.append(" (tune ").append(tune.getInfo().getCurrentSong()).append("/")
				.append(tune.getInfo().getSongs());
		trackList.append("[").append(tune.getInfo().getStartSong()).append("])");
		if (config.getSidplay2Section().isLoop()) {
			trackList.append(" [" + BUNDLE.getString("LOOPING") + "]");
		}
		out.printf("%37s %c\n", trackList.toString(), console.getVertical());
	}

	private void printSongSpeedCIAorVBI(PrintStream out, final IniConsoleSection console, final SidTune tune,
			final PlayList playList) {
		out.printf("%c %-12s : %37s %c\n", console.getVertical(), BUNDLE.getString("SONG_SPEED"),
				tune.getSongSpeed(playList.getCurrent()), console.getVertical());
	}

	private void printSongLength(PrintStream out, final IniConsoleSection console, final Timer timer) {
		out.printf("%c %-12s : ", console.getVertical(), BUNDLE.getString("SONG_LENGTH"));
		if (timer.getEnd() != 0) {
			String time = new SimpleDateFormat("mm:ss.SSS").format(new Date((long) (timer.getEnd() * 1000)));
			out.printf("%37s %c\n", time, console.getVertical());
		} else {
			out.printf("%37s %c\n", BUNDLE.getString("UNLIMITED"), console.getVertical());
		}
	}

	private void printHorizontalBottomLine(PrintStream out, final IniConsoleSection console) {
		out.printf("%c%s%c\n", console.getBottomLeft(), setFill(console.getHorizontal(), 54), console.getBottomRight());
	}

	private void printAddresses(PrintStream out, final IniConsoleSection console, final SidTune tune) {
		StringBuffer line = new StringBuffer();
		// Display PSID Driver location
		line.append(BUNDLE.getString("DRIVER_ADDR") + " = ");
		if (tune.getInfo().getDeterminedDriverAddr() == 0) {
			line.append(BUNDLE.getString("NOT_PRESENT"));
		} else {
			line.append(String.format("$%04x", tune.getInfo().getDeterminedDriverAddr()));
			line.append(String.format("-$%04x",
					tune.getInfo().getDeterminedDriverAddr() + tune.getInfo().getDeterminedDriverLength() - 1));
		}
		if (tune.getInfo().getPlayAddr() == 0xffff) {
			line.append(String.format(", " + BUNDLE.getString("SYS") + " = $%04x", tune.getInfo().getInitAddr()));
		} else {
			line.append(String.format(", " + BUNDLE.getString("INIT") + " = $%04x", tune.getInfo().getInitAddr()));
		}

		out.printf("%c %-12s : %37s %c\n", console.getVertical(), BUNDLE.getString("ADDRESSES"), line.toString(),
				console.getVertical());
		line = new StringBuffer();
		line.append(String.format(BUNDLE.getString("LOAD") + " = $%04x", tune.getInfo().getLoadAddr()));
		line.append(String.format("-$%04x", tune.getInfo().getLoadAddr() + tune.getInfo().getC64dataLen() - 1));
		if (tune.getInfo().getPlayAddr() != 0xffff) {
			line.append(String.format(", %s = $%04x", BUNDLE.getString("PLAY"), tune.getInfo().getPlayAddr()));
		}
		out.printf("%c              : %37s %c\n", console.getVertical(), line.toString(), console.getVertical());
	}

	private void printSIDDetails(PrintStream out, final IniConsoleSection console, final SidTune tune) {
		StringBuffer line = new StringBuffer();
		IEmulationSection emulation = config.getEmulationSection();
		line.append(BUNDLE.getString("FILTER") + (emulation.isFilter() ? " = on, " : " = off, "));
		ChipModel chipModel = ChipModel.getChipModel(emulation, tune, 0);
		line.append(String.format(BUNDLE.getString("MODEL") + " = %s", chipModel));
		if (SidTune.isSIDUsed(emulation, tune, 1)) {
			ChipModel stereoModel = ChipModel.getChipModel(emulation, tune, 1);
			line.append(String.format("(%s)", stereoModel));
		}
		out.printf("%c %-12s : %37s %c\n", console.getVertical(), BUNDLE.getString("SID_DETAILS"), line.toString(),
				console.getVertical());

		if (SidTune.isSIDUsed(emulation, tune, 2)) {
			ChipModel thirdModel = ChipModel.getChipModel(emulation, tune, 2);
			line.setLength(0);
			line.append(String.format("(%s)", thirdModel));
			out.printf("%c %-12s : %37s %c\n", console.getVertical(), BUNDLE.getString("SID_DETAILS"), line.toString(),
					console.getVertical());
		}
	}

	private void printKeyboardControls(PrintStream out) {
		out.println(BUNDLE.getString("KEYBOARD_CONTROLS"));
		out.println(BUNDLE.getString("FORWARD_REWIND"));
		out.println(BUNDLE.getString("FIRST_LAST"));
		out.println(BUNDLE.getString("NORMAL_FAST"));
		out.println(BUNDLE.getString("PAUSE_CONTINUE"));
		out.println(BUNDLE.getString("MUTE_1"));
		out.println(BUNDLE.getString("MUTE_2"));
		out.println(BUNDLE.getString("MUTE_3"));
		out.println(BUNDLE.getString("MUTE_4"));
		out.println(BUNDLE.getString("MUTE_5"));
		out.println(BUNDLE.getString("MUTE_6"));
		out.println(BUNDLE.getString("MUTE_7"));
		out.println(BUNDLE.getString("MUTE_8"));
		out.println(BUNDLE.getString("MUTE_9"));
		out.println(BUNDLE.getString("MUTE_10"));
		out.println(BUNDLE.getString("MUTE_11"));
		out.println(BUNDLE.getString("MUTE_12"));
		out.println(BUNDLE.getString("FILTER_ENABLE"));
		out.println(BUNDLE.getString("STEREO_FILTER_ENABLE"));
		out.println(BUNDLE.getString("3RD_SID_FILTER_ENABLE"));
		out.println(BUNDLE.getString("QUIT"));
	}

	private String setFill(final char ch, final int length) {
		final StringBuffer ret = new StringBuffer();
		for (int i = 0; i < length; i++) {
			ret.append(ch);
		}
		return ret.toString();
	}

}
