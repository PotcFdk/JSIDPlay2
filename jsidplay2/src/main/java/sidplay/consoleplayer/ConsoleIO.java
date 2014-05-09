package sidplay.consoleplayer;

import java.io.IOException;
import java.util.ResourceBundle;

import libsidplay.Player;
import libsidplay.player.Timer;
import libsidplay.player.Track;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Model;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IConsoleSection;

public class ConsoleIO {

	private static final ResourceBundle BUNDLE = ResourceBundle
			.getBundle("sidplay.consoleplayer.ConsolePlayer");

	private IConfig config;

	private boolean v1mute, v2mute, v3mute, quiet;
	private int verboseLevel;

	public ConsoleIO(IConfig config, boolean quiet, int verboseLevel) {
		this.config = config;
		this.quiet = quiet;
		this.verboseLevel = verboseLevel;
	}

	public void menu(Player player) {
		if (quiet) {
			return;
		}
		final IConsoleSection console = config.getConsole();

		final SidTune tune = player.getTune();
		final Track track = player.getTrack();
		final Timer timer = player.getTimer();

		System.out.println(String.format("%c%s%c", console.getTopLeft(),
				setfill(console.getHorizontal(), 54), console.getTopRight()));
		System.out.println(String.format("%c%54s%c", console.getVertical(), " "
				+ BUNDLE.getString("HEADING") + "  ", console.getVertical()));
		System.out.println(String.format("%c%s%c", console.getJunctionLeft(),
				setfill(console.getHorizontal(), 54),
				console.getJunctionRight()));
		if (tune.getInfo().numberOfInfoStrings > 0) {
			if (tune.getInfo().numberOfInfoStrings == 3) {
				System.out.println(String.format("%c %-12s : %37s %c",
						console.getVertical(), BUNDLE.getString("TITLE"),
						tune.getInfo().infoString[0], console.getVertical()));
				System.out.println(String.format("%c %-12s : %37s %c",
						console.getVertical(), BUNDLE.getString("AUTHOR"),
						tune.getInfo().infoString[1], console.getVertical()));
				System.out.println(String.format("%c %-12s : %37s %c",
						console.getVertical(), BUNDLE.getString("AUTHOR"),
						tune.getInfo().infoString[2], console.getVertical()));
			} else {
				for (int i = 0; i < tune.getInfo().numberOfInfoStrings; i++) {
					System.out
							.println(String.format("%c %-12s : %37s %c",
									console.getVertical(),
									BUNDLE.getString("DESCRIPTION"),
									tune.getInfo().infoString[i],
									console.getVertical()));
				}
			}
			System.out.println(String.format("%c%s%c",
					console.getJunctionLeft(),
					setfill(console.getHorizontal(), 54),
					console.getJunctionRight()));
		}
		if (verboseLevel != 0) {
			System.out.println(String.format("%c %-12s : %37s %c",
					console.getVertical(), BUNDLE.getString("FILE_FORMAT"),
					tune.getInfo().getClass().getSimpleName(),
					console.getVertical()));
			System.out.println(String.format("%c %-12s : %37s %c",
					console.getVertical(), BUNDLE.getString("FILENAMES"),
					tune.getInfo().file.getName(), console.getVertical()));
		}
		System.out.print(String.format("%c %-12s : ", console.getVertical(),
				BUNDLE.getString("PLAYLIST")));
		{ // This will be the format used for playlists
			int i = 1;
			if (!config.getSidplay2().isSingle()) {
				i = track.getSelected();
				i -= track.getFirst() - 1;
				if (i < 1) {
					i += track.getSongs();
				}
			}
			System.out.println(String.format(
					"%37s %c",
					i
							+ "/"
							+ track.getSongs()
							+ " (tune "
							+ tune.getInfo().currentSong
							+ "/"
							+ tune.getInfo().songs
							+ "["
							+ tune.getInfo().startSong
							+ "])"
							+ (config.getSidplay2().isLoop() ? " ["
									+ BUNDLE.getString("LOOPING") + "]" : ""),
					console.getVertical()));
		}
		if (verboseLevel > 0) {
			System.out.println(String.format("%c%s%c", console.getBottomLeft(),
					setfill(console.getHorizontal(), 54),
					console.getBottomRight()));
			System.out.println(String.format("%c %-12s : %37s %c",
					console.getVertical(), BUNDLE.getString("SONG_SPEED"),
					tune.getSongSpeed(track.getSelected()),
					console.getVertical()));
		}
		System.out.print(String.format("%c %-12s : ", console.getVertical(),
				BUNDLE.getString("SONG_LENGTH")));
		if (timer.getStop() != 0) {
			final String time = String.format("%02d:%02d",
					(timer.getStop() / 60 % 100), (timer.getStop() % 60));
			System.out.print(String.format("%37s %c", "" + time,
					console.getVertical()));
		} else {
			System.out.print(String.format("%37s %c",
					BUNDLE.getString("UNLIMITED"), console.getVertical()));
		}
		System.out.println();
		if (verboseLevel > 0) {
			System.out.println(String.format("%c%s%c", console.getBottomLeft(),
					setfill(console.getHorizontal(), 54),
					console.getBottomRight()));
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

			System.out.println(String.format("%c %-12s : %37s %c",
					console.getVertical(), BUNDLE.getString("ADDRESSES"),
					line.toString(), console.getVertical()));
			line = new StringBuffer();
			line.append(String.format(BUNDLE.getString("LOAD") + " = $%04x",
					tune.getInfo().loadAddr));
			line.append(String.format("-$%04x",
					tune.getInfo().loadAddr + tune.getInfo().c64dataLen - 1));
			if (tune.getInfo().playAddr != 0xffff) {
				line.append(String.format(", %s = $%04x",
						BUNDLE.getString("PLAY"), tune.getInfo().playAddr));
			}
			System.out.println(String.format("%c              : %37s %c",
					console.getVertical(), line.toString(),
					console.getVertical()));

			line = new StringBuffer();
			line.append(String.format(BUNDLE.getString("FILTER") + " = %s",
					(config.getEmulation().isFilter() ? "Yes" : "No")));
			/* XXX ignores 2nd SID */
			line.append(String.format(", " + BUNDLE.getString("MODEL")
					+ " = %s",
					(tune.getInfo().sid1Model == Model.MOS8580 ? "8580"
							: "6581")));
			System.out.println(String.format("%c %-12s : %37s %c",
					console.getVertical(), BUNDLE.getString("SID_DETAILS"),
					line.toString(), console.getVertical()));

		}

		System.out
				.println(String.format("%c%s%c", console.getBottomLeft(),
						setfill(console.getHorizontal(), 54),
						console.getBottomRight()));
		System.out.println(BUNDLE.getString("KEYBOARD_CONTROLS"));
		System.out.println(BUNDLE.getString("FORWARD_REWIND"));
		System.out.println(BUNDLE.getString("NORMAL_FAST"));
		System.out.println(BUNDLE.getString("PAUSE_CONTINUE"));
		System.out.println(BUNDLE.getString("FIRST_LAST"));
		System.out.println(BUNDLE.getString("MUTE_1"));
		System.out.println(BUNDLE.getString("MUTE_2"));
		System.out.println(BUNDLE.getString("MUTE_3"));
		System.out.println(BUNDLE.getString("MUTE_4"));
		System.out.println(BUNDLE.getString("MUTE_5"));
		System.out.println(BUNDLE.getString("MUTE_6"));
		System.out.println(BUNDLE.getString("FILTER_ENABLE"));
		System.out.println(BUNDLE.getString("QUIT"));
	}

	private String setfill(final char ch, final int length) {
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
				player.nextSong();
				break;

			case '<':
				player.previousSong();
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
				player.setMute(0, 0, v1mute);
				break;

			case '2':
				v2mute = !v2mute;
				player.setMute(0, 1, v2mute);
				break;

			case '3':
				v3mute = !v3mute;
				player.setMute(0, 2, v3mute);
				break;

			case '4':
				v1mute = !v1mute;
				player.setMute(1, 0, v1mute);
				break;

			case '5':
				v2mute = !v2mute;
				player.setMute(1, 1, v2mute);
				break;

			case '6':
				v3mute = !v3mute;
				player.setMute(1, 2, v3mute);
				break;

			case 'f': {
				boolean filterEnable = config.getEmulation().isFilter();
				config.getEmulation().setFilter(filterEnable ^ true);
				player.setFilterEnable(filterEnable);
				break;
			}

			case 'q':
				player.quit();
				break;

			default:
				break;
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

}
