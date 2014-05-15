package sidplay.consoleplayer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.ResourceBundle;

import libsidplay.Player;
import libsidplay.player.Timer;
import libsidplay.player.PlayList;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Model;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IConsoleSection;

public class ConsoleIO {

	private static final ResourceBundle BUNDLE = ResourceBundle
			.getBundle("sidplay.consoleplayer.ConsolePlayer");

	private IConfig config;

	private String filename;

	private boolean v1mute, v2mute, v3mute, v4mute, v5mute, v6mute;

	public ConsoleIO(IConfig config, String filename) {
		this.config = config;
		this.filename = filename;
	}

	public void menu(Player player, int verboseLevel, boolean quiet,
			PrintStream out) {
		if (quiet) {
			return;
		}
		final IConsoleSection console = config.getConsole();

		final SidTune tune = player.getTune();
		final PlayList playList = player.getPlayList();
		final Timer timer = player.getTimer();

		printTopLine(out, console);
		printHeading(out, console);
		printSeparatorLine(out, console);
		if (tune.getInfo().infoString.size() == 3) {
			printTitleAutorReleased(out, console, tune);
		} else {
			printDecription(out, console, tune);
		}
		if (tune.getInfo().infoString.size() > 0) {
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

	private void printTopLine(PrintStream out, final IConsoleSection console) {
		out.printf("%c%s%c\n", console.getTopLeft(),
				setFill(console.getHorizontal(), 54), console.getTopRight());
	}

	private void printHeading(PrintStream out, final IConsoleSection console) {
		out.printf("%c%52s  %c\n", console.getVertical(),
				BUNDLE.getString("HEADING"), console.getVertical());
	}

	private void printSeparatorLine(PrintStream out,
			final IConsoleSection console) {
		out.printf("%c%s%c\n", console.getJunctionLeft(),
				setFill(console.getHorizontal(), 54),
				console.getJunctionRight());
	}

	private void printTitleAutorReleased(PrintStream out,
			final IConsoleSection console, final SidTune tune) {
		Iterator<String> description = tune.getInfo().infoString.iterator();
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
			final IConsoleSection console, final SidTune tune) {
		for (String description : tune.getInfo().infoString) {
			out.printf("%c %-12s : %37s %c\n", console.getVertical(),
					BUNDLE.getString("DESCRIPTION"), description,
					console.getVertical());
		}
	}

	private void printFileDetails(PrintStream out,
			final IConsoleSection console, final SidTune tune) {
		out.printf("%c %-12s : %37s %c\n", console.getVertical(),
				BUNDLE.getString("FILE_FORMAT"), tune.getInfo().getClass()
						.getSimpleName(), console.getVertical());
		out.printf("%c %-12s : %37s %c\n", console.getVertical(),
				BUNDLE.getString("FILENAMES"), new File(filename).getName(),
				console.getVertical());
	}

	private void printPlaylist(PrintStream out, final IConsoleSection console,
			final SidTune tune, final PlayList playList) {
		int i = playList.getCurrentRelative();
		out.printf("%c %-12s : ", console.getVertical(),
				BUNDLE.getString("PLAYLIST"));
		StringBuffer trackList = new StringBuffer();
		trackList.append(i).append("/").append(playList.getLength());
		trackList.append(" (tune ").append(tune.getInfo().currentSong)
				.append("/").append(tune.getInfo().songs);
		trackList.append("[").append(tune.getInfo().startSong).append("])");
		if (config.getSidplay2().isLoop()) {
			trackList.append(" [" + BUNDLE.getString("LOOPING") + "]");
		}
		out.printf("%37s %c\n", trackList.toString(), console.getVertical());
	}

	private void printSongSpeedCIAorVBI(PrintStream out,
			final IConsoleSection console, final SidTune tune,
			final PlayList playList) {
		out.printf("%c %-12s : %37s %c\n", console.getVertical(),
				BUNDLE.getString("SONG_SPEED"),
				tune.getSongSpeed(playList.getCurrent()),
				console.getVertical());
	}

	private void printSongLength(PrintStream out,
			final IConsoleSection console, final Timer timer) {
		out.printf("%c %-12s : ", console.getVertical(),
				BUNDLE.getString("SONG_LENGTH"));
		if (timer.getStop() != 0) {
			String time = String.format("%02d:%02d",
					(timer.getStop() / 60 % 100), (timer.getStop() % 60));
			out.printf("%37s %c\n", time, console.getVertical());
		} else {
			out.printf("%37s %c\n", BUNDLE.getString("UNLIMITED"),
					console.getVertical());
		}
	}

	private void printHorizontalBottomLine(PrintStream out,
			final IConsoleSection console) {
		out.printf("%c%s%c\n", console.getBottomLeft(),
				setFill(console.getHorizontal(), 54), console.getBottomRight());
	}

	private void printAddresses(PrintStream out, final IConsoleSection console,
			final SidTune tune) {
		StringBuffer line = new StringBuffer();
		// Display PSID Driver location
		line.append(BUNDLE.getString("DRIVER_ADDR") + " = ");
		if (tune.getInfo().determinedDriverAddr == 0) {
			line.append(BUNDLE.getString("NOT_PRESENT"));
		} else {
			line.append(String.format("$%04x",
					tune.getInfo().determinedDriverAddr));
			line.append(String.format("-$%04x",
					tune.getInfo().determinedDriverAddr
							+ tune.getInfo().determinedDriverLength - 1));
		}
		if (tune.getInfo().playAddr == 0xffff) {
			line.append(String.format(", " + BUNDLE.getString("SYS")
					+ " = $%04x", tune.getInfo().initAddr));
		} else {
			line.append(String.format(", " + BUNDLE.getString("INIT")
					+ " = $%04x", tune.getInfo().initAddr));
		}

		out.printf("%c %-12s : %37s %c\n", console.getVertical(),
				BUNDLE.getString("ADDRESSES"), line.toString(),
				console.getVertical());
		line = new StringBuffer();
		line.append(String.format(BUNDLE.getString("LOAD") + " = $%04x",
				tune.getInfo().loadAddr));
		line.append(String.format("-$%04x",
				tune.getInfo().loadAddr + tune.getInfo().c64dataLen - 1));
		if (tune.getInfo().playAddr != 0xffff) {
			line.append(String.format(", %s = $%04x", BUNDLE.getString("PLAY"),
					tune.getInfo().playAddr));
		}
		out.printf("%c              : %37s %c\n", console.getVertical(),
				line.toString(), console.getVertical());
	}

	private void printSIDDetails(PrintStream out,
			final IConsoleSection console, final SidTune tune) {
		StringBuffer line = new StringBuffer(String.format(
				BUNDLE.getString("FILTER") + " = %s",
				Boolean.valueOf(config.getEmulation().isFilter())));
		/* XXX ignores 2nd SID */
		line.append(String.format(", " + BUNDLE.getString("MODEL") + " = %s",
				(tune.getInfo().sid1Model == Model.MOS8580 ? "8580" : "6581")));
		out.printf("%c %-12s : %37s %c\n", console.getVertical(),
				BUNDLE.getString("SID_DETAILS"), line.toString(),
				console.getVertical());
	}

	private void printKeyboardControls(PrintStream out) {
		out.println(BUNDLE.getString("KEYBOARD_CONTROLS"));
		out.println(BUNDLE.getString("FORWARD_REWIND"));
		out.println(BUNDLE.getString("NORMAL_FAST"));
		out.println(BUNDLE.getString("PAUSE_CONTINUE"));
		out.println(BUNDLE.getString("FIRST_LAST"));
		out.println(BUNDLE.getString("MUTE_1"));
		out.println(BUNDLE.getString("MUTE_2"));
		out.println(BUNDLE.getString("MUTE_3"));
		out.println(BUNDLE.getString("MUTE_4"));
		out.println(BUNDLE.getString("MUTE_5"));
		out.println(BUNDLE.getString("MUTE_6"));
		out.println(BUNDLE.getString("FILTER_ENABLE"));
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
			switch (key) {
			case 'h':
				player.selectFirstTrack();
				break;

			case 'e':
				player.selectLastTrack();
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

			case 'f': {
				boolean filterEnable = config.getEmulation().isFilter() ^ true;
				config.getEmulation().setFilter(filterEnable);
				player.configureSIDs(sid -> sid.setFilterEnable(filterEnable));
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
