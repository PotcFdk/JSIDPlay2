package sidplay.consoleplayer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.ResourceBundle;

import libsidplay.Player;
import libsidplay.common.ChipModel;
import libsidplay.player.PlayList;
import libsidplay.player.Timer;
import libsidplay.sidtune.SidTune;
import sidplay.ini.IniConfig;
import sidplay.ini.IniConsoleSection;
import sidplay.ini.intf.IEmulationSection;

public class ConsoleIO {

	private static final ResourceBundle BUNDLE = ResourceBundle
			.getBundle("sidplay.consoleplayer.ConsolePlayer");

	private IniConfig config;

	private String filename;

	private boolean v1mute, v2mute, v3mute, v4mute, v5mute, v6mute, v7mute,
			v8mute, v9mute;

	public ConsoleIO(IniConfig config, String filename) {
		this.config = config;
		this.filename = filename;
	}

	public void menu(Player player, int verboseLevel, boolean quiet,
			PrintStream out) {
		if (quiet) {
			return;
		}
		final IniConsoleSection console = config.getConsole();

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

	private void printTopLine(PrintStream out, final IniConsoleSection console) {
		out.printf("%c%s%c\n", console.getTopLeft(),
				setFill(console.getHorizontal(), 54), console.getTopRight());
	}

	private void printHeading(PrintStream out, final IniConsoleSection console) {
		out.printf("%c%52s  %c\n", console.getVertical(),
				BUNDLE.getString("HEADING"), console.getVertical());
	}

	private void printSeparatorLine(PrintStream out,
			final IniConsoleSection console) {
		out.printf("%c%s%c\n", console.getJunctionLeft(),
				setFill(console.getHorizontal(), 54),
				console.getJunctionRight());
	}

	private void printTitleAutorReleased(PrintStream out,
			final IniConsoleSection console, final SidTune tune) {
		Iterator<String> description = tune.getInfo().getInfoString()
				.iterator();
		out.printf("%c %-12s : %37s %c\n", console.getVertical(),
				BUNDLE.getString("TITLE"), description.next(),
				console.getVertical());
		out.printf("%c %-12s : %37s %c\n", console.getVertical(),
				BUNDLE.getString("AUTHOR"), description.next(),
				console.getVertical());
		out.printf("%c %-12s : %37s %c\n", console.getVertical(),
				BUNDLE.getString("RELEASED"), description.next(),
				console.getVertical());
	}

	private void printDecription(PrintStream out,
			final IniConsoleSection console, final SidTune tune) {
		for (String description : tune.getInfo().getInfoString()) {
			out.printf("%c %-12s : %37s %c\n", console.getVertical(),
					BUNDLE.getString("DESCRIPTION"), description,
					console.getVertical());
		}
	}

	private void printFileDetails(PrintStream out,
			final IniConsoleSection console, final SidTune tune) {
		out.printf("%c %-12s : %37s %c\n", console.getVertical(),
				BUNDLE.getString("FILE_FORMAT"), tune.getInfo().getClass()
						.getSimpleName(), console.getVertical());
		out.printf("%c %-12s : %37s %c\n", console.getVertical(),
				BUNDLE.getString("FILENAMES"), new File(filename).getName(),
				console.getVertical());
	}

	private void printPlaylist(PrintStream out,
			final IniConsoleSection console, final SidTune tune,
			final PlayList playList) {
		int trackNum = playList.getTrackNum();
		out.printf("%c %-12s : ", console.getVertical(),
				BUNDLE.getString("PLAYLIST"));
		StringBuffer trackList = new StringBuffer();
		trackList.append(trackNum).append("/").append(playList.getLength());
		trackList.append(" (tune ").append(tune.getInfo().getCurrentSong())
				.append("/").append(tune.getInfo().getSongs());
		trackList.append("[").append(tune.getInfo().getStartSong())
				.append("])");
		if (config.getSidplay2Section().isLoop()) {
			trackList.append(" [" + BUNDLE.getString("LOOPING") + "]");
		}
		out.printf("%37s %c\n", trackList.toString(), console.getVertical());
	}

	private void printSongSpeedCIAorVBI(PrintStream out,
			final IniConsoleSection console, final SidTune tune,
			final PlayList playList) {
		out.printf("%c %-12s : %37s %c\n", console.getVertical(),
				BUNDLE.getString("SONG_SPEED"),
				tune.getSongSpeed(playList.getCurrent()), console.getVertical());
	}

	private void printSongLength(PrintStream out,
			final IniConsoleSection console, final Timer timer) {
		out.printf("%c %-12s : ", console.getVertical(),
				BUNDLE.getString("SONG_LENGTH"));
		if (timer.getEnd() != 0) {
			String time = String.format("%02d:%02d",
					(timer.getEnd() / 60 % 100), (timer.getEnd() % 60));
			out.printf("%37s %c\n", time, console.getVertical());
		} else {
			out.printf("%37s %c\n", BUNDLE.getString("UNLIMITED"),
					console.getVertical());
		}
	}

	private void printHorizontalBottomLine(PrintStream out,
			final IniConsoleSection console) {
		out.printf("%c%s%c\n", console.getBottomLeft(),
				setFill(console.getHorizontal(), 54), console.getBottomRight());
	}

	private void printAddresses(PrintStream out,
			final IniConsoleSection console, final SidTune tune) {
		StringBuffer line = new StringBuffer();
		// Display PSID Driver location
		line.append(BUNDLE.getString("DRIVER_ADDR") + " = ");
		if (tune.getInfo().getDeterminedDriverAddr() == 0) {
			line.append(BUNDLE.getString("NOT_PRESENT"));
		} else {
			line.append(String.format("$%04x", tune.getInfo()
					.getDeterminedDriverAddr()));
			line.append(String.format("-$%04x", tune.getInfo()
					.getDeterminedDriverAddr()
					+ tune.getInfo().getDeterminedDriverLength() - 1));
		}
		if (tune.getInfo().getPlayAddr() == 0xffff) {
			line.append(String.format(", " + BUNDLE.getString("SYS")
					+ " = $%04x", tune.getInfo().getInitAddr()));
		} else {
			line.append(String.format(", " + BUNDLE.getString("INIT")
					+ " = $%04x", tune.getInfo().getInitAddr()));
		}

		out.printf("%c %-12s : %37s %c\n", console.getVertical(),
				BUNDLE.getString("ADDRESSES"), line.toString(),
				console.getVertical());
		line = new StringBuffer();
		line.append(String.format(BUNDLE.getString("LOAD") + " = $%04x", tune
				.getInfo().getLoadAddr()));
		line.append(String.format("-$%04x", tune.getInfo().getLoadAddr()
				+ tune.getInfo().getC64dataLen() - 1));
		if (tune.getInfo().getPlayAddr() != 0xffff) {
			line.append(String.format(", %s = $%04x", BUNDLE.getString("PLAY"),
					tune.getInfo().getPlayAddr()));
		}
		out.printf("%c              : %37s %c\n", console.getVertical(),
				line.toString(), console.getVertical());
	}

	private void printSIDDetails(PrintStream out,
			final IniConsoleSection console, final SidTune tune) {
		StringBuffer line = new StringBuffer();
		IEmulationSection emulation = config.getEmulationSection();
		line.append(BUNDLE.getString("FILTER")
				+ (emulation.isFilter() ? " = on, " : " = off, "));
		ChipModel chipModel = ChipModel.getChipModel(emulation, tune, 0);
		line.append(String.format(BUNDLE.getString("MODEL") + " = %s",
				chipModel));
		if (SidTune.isSIDUsed(emulation, tune, 1)) {
			ChipModel stereoModel = ChipModel.getChipModel(emulation, tune, 1);
			line.append(String.format("(%s)", stereoModel));
		}
		out.printf("%c %-12s : %37s %c\n", console.getVertical(),
				BUNDLE.getString("SID_DETAILS"), line.toString(),
				console.getVertical());

		if (SidTune.isSIDUsed(emulation, tune, 2)) {
			ChipModel thirdModel = ChipModel.getChipModel(emulation, tune, 2);
			line.setLength(0);
			line.append(String.format("(%s)", thirdModel));
			out.printf("%c %-12s : %37s %c\n", console.getVertical(),
					BUNDLE.getString("SID_DETAILS"), line.toString(),
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

	public void decodeKeys(Player player) {
		try {
			if (System.in.available() == 0) {
				return;
			}
			final int key = System.in.read();
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
				player.fastForward();
				break;

			case ',':
				player.normalSpeed();
				break;

			case 'p':
				player.pause();
				break;

			case '1':
				v1mute = !v1mute;
				player.configureSID(0, sid -> sid.setVoiceMute(0, v1mute));
				break;

			case '2':
				v2mute = !v2mute;
				player.configureSID(0, sid -> sid.setVoiceMute(1, v2mute));
				break;

			case '3':
				v3mute = !v3mute;
				player.configureSID(0, sid -> sid.setVoiceMute(2, v3mute));
				break;

			case '4':
				v4mute = !v4mute;
				player.configureSID(1, sid -> sid.setVoiceMute(0, v4mute));
				break;

			case '5':
				v5mute = !v5mute;
				player.configureSID(1, sid -> sid.setVoiceMute(1, v5mute));
				break;

			case '6':
				v6mute = !v6mute;
				player.configureSID(1, sid -> sid.setVoiceMute(2, v6mute));
				break;

			case '7':
				v7mute = !v7mute;
				player.configureSID(2, sid -> sid.setVoiceMute(0, v7mute));
				break;

			case '8':
				v8mute = !v8mute;
				player.configureSID(2, sid -> sid.setVoiceMute(1, v8mute));
				break;

			case '9':
				v9mute = !v9mute;
				player.configureSID(2, sid -> sid.setVoiceMute(2, v9mute));
				break;

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

}
