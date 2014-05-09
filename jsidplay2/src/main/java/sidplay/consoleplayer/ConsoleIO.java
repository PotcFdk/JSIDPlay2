package sidplay.consoleplayer;

import java.io.IOException;

import libsidplay.Player;
import libsidplay.player.Timer;
import libsidplay.player.Track;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Model;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IConsoleSection;
import sidplay.ini.intf.IEmulationSection;

public class ConsoleIO {

	private IConfig config;
	private Player player;

	private boolean v1mute, v2mute, v3mute, filterEnable, quiet;
	private int verboseLevel;

	public ConsoleIO(Player player, boolean quiet,
			int verboseLevel) {
		this.config = player.getConfig();
		this.player = player;
		this.quiet = quiet;
		this.verboseLevel = verboseLevel;
	}

	public void menu(SidTune tune, Track track, Timer timer) {
		final IConsoleSection console = config.getConsole();
		final IEmulationSection emulation = config.getEmulation();
		filterEnable = emulation.isFilter();
		if (quiet) {
			return;
		}

		System.out.println(String.format("%c%s%c", console.getTopLeft(),
				setfill(console.getHorizontal(), 54), console.getTopRight()));
		System.out.println(String.format("%c%54s%c", console.getVertical(),
				" Java SIDPLAY - Music Player & C64 SID Chip Emulator  ",
				console.getVertical()));
		System.out.println(String.format("%c%s%c", console.getJunctionLeft(),
				setfill(console.getHorizontal(), 54),
				console.getJunctionRight()));
		if (tune.getInfo().numberOfInfoStrings > 0) {
			if (tune.getInfo().numberOfInfoStrings == 3) {
				System.out.println(String.format("%c Title        : %37s %c",
						console.getVertical(), tune.getInfo().infoString[0],
						console.getVertical()));
				System.out.println(String.format("%c Author       : %37s %c",
						console.getVertical(), tune.getInfo().infoString[1],
						console.getVertical()));
				System.out.println(String.format("%c Released     : %37s %c",
						console.getVertical(), tune.getInfo().infoString[2],
						console.getVertical()));
			} else {
				for (int i = 0; i < tune.getInfo().numberOfInfoStrings; i++) {
					System.out
							.println(String.format("%c Description  : %37s %c",
									console.getVertical(),
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
			System.out.println(String.format("%c File format  : %37s %c",
					console.getVertical(), tune.getInfo().getClass()
							.getSimpleName(), console.getVertical()));
			System.out.println(String.format("%c Filename(s)  : %37s %c",
					console.getVertical(), tune.getInfo().file.getName(),
					console.getVertical()));
		}
		System.out.print(String.format("%c Playlist     : ",
				console.getVertical()));
		{ // This will be the format used for playlists
			int i = 1;
			if (!config.getSidplay2().isSingle()) {
				i = track.getSelected();
				i -= track.getFirst() - 1;
				if (i < 1) {
					i += track.getSongs();
				}
			}
			System.out.println(String.format("%37s %c",
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
							+ (config.getSidplay2().isLoop() ? " [LOOPING]"
									: ""), console.getVertical()));
		}
		if (verboseLevel > 0) {
			System.out.println(String.format("%c%s%c", console.getBottomLeft(),
					setfill(console.getHorizontal(), 54),
					console.getBottomRight()));
			System.out.println(String.format("%c Song Speed   : %37s %c",
					console.getVertical(),
					tune.getSongSpeed(track.getSelected()),
					console.getVertical()));
		}
		System.out.print(String.format("%c Song Length  : ",
				console.getVertical()));
		if (timer.getStop() != 0) {
			final String time = String.format("%02d:%02d",
					(timer.getStop() / 60 % 100), (timer.getStop() % 60));
			System.out.print(String.format("%37s %c", "" + time,
					console.getVertical()));
		} else {
			System.out.print(String.format("%37s %c", "FOREVER",
					console.getVertical()));
		}
		System.out.println();
		if (verboseLevel > 0) {
			System.out.println(String.format("%c%s%c", console.getBottomLeft(),
					setfill(console.getHorizontal(), 54),
					console.getBottomRight()));
			StringBuffer line = new StringBuffer();
			// Display PSID Driver location
			line.append("DRIVER = ");
			if (tune.getInfo().determinedDriverAddr == 0) {
				line.append("NOT PRESENT");
			} else {
				line.append(String.format("$%04x",
						tune.getInfo().determinedDriverAddr));
				line.append(String.format("-$%04x",
						tune.getInfo().determinedDriverAddr
								+ tune.getInfo().determinedDriverLength - 1));
			}
			if (tune.getInfo().playAddr == 0xffff) {
				line.append(String.format(", SYS = $%04x",
						tune.getInfo().initAddr));
			} else {
				line.append(String.format(", INIT = $%04x",
						tune.getInfo().initAddr));
			}

			System.out.println(String.format("%c Addresses    : %37s %c",
					console.getVertical(), line.toString(),
					console.getVertical()));
			line = new StringBuffer();
			line.append(String.format("LOAD   = $%04x", tune.getInfo().loadAddr));
			line.append(String.format("-$%04x",
					tune.getInfo().loadAddr + tune.getInfo().c64dataLen - 1));
			if (tune.getInfo().playAddr != 0xffff) {
				line.append(String.format(", PLAY = $%04x",
						tune.getInfo().playAddr));
			}
			System.out.println(String.format("%c              : %37s %c",
					console.getVertical(), line.toString(),
					console.getVertical()));

			line = new StringBuffer();
			line.append(String.format("Filter = %s", (filterEnable ? "Yes"
					: "No")));
			/* XXX ignores 2nd SID */
			line.append(String.format(", Model = %s",
					(tune.getInfo().sid1Model == Model.MOS8580 ? "8580"
							: "6581")));
			System.out.println(String.format("%c SID Details  : %37s %c",
					console.getVertical(), line.toString(),
					console.getVertical()));

			if (verboseLevel > 1) {
				line = new StringBuffer();
				System.out.println(String.format("%c Delay        : %37s %c",
						console.getVertical(), line.toString(),
						console.getVertical()));
			}
		}

		System.out
				.println(String.format("%c%s%c", console.getBottomLeft(),
						setfill(console.getHorizontal(), 54),
						console.getBottomRight()));
		System.out.println("keyboard control (press enter after command):");
		System.out.println("< > - play previous/next tune");
		System.out.println(", . - normal/faster speed");
		System.out.println("p   - pause/continue player");
		System.out.println("h e - play first/last tune");
		System.out.println("1   - mute voice 1");
		System.out.println("2   - mute voice 2");
		System.out.println("3   - mute voice 3");
		System.out.println("4   - mute voice 1 (stereo)");
		System.out.println("5   - mute voice 2 (stereo)");
		System.out.println("6   - mute voice 3 (stereo)");
		System.out.println("f   - enable/disable filter");
		System.out.println("q   - quit player");
	}

	private String setfill(final char ch, final int length) {
		final StringBuffer ret = new StringBuffer();
		for (int i = 0; i < length; i++) {
			ret.append(ch);
		}
		return ret.toString();
	}

	public void decodeKeys() {
		try {
			if (System.in.available() == 0) {
				return;
			}
			final int key = System.in.read();
			switch (key) {
			case 'h':
				this.player.selectFirstTrack();
				break;

			case 'e':
				this.player.selectLastTrack();
				break;

			case '>':
				this.player.nextSong();
				break;

			case '<':
				this.player.previousSong();
				break;

			case '.':
				this.player.fastForward();
				break;

			case ',':
				this.player.normalSpeed();
				break;

			case 'p':
				this.player.pause();
				break;

			case '1':
				v1mute = !v1mute;
				this.player.setMute(0, 0, v1mute);
				break;

			case '2':
				v2mute = !v2mute;
				this.player.setMute(0, 1, v2mute);
				break;

			case '3':
				v3mute = !v3mute;
				this.player.setMute(0, 2, v3mute);
				break;

			case '4':
				v1mute = !v1mute;
				this.player.setMute(1, 0, v1mute);
				break;

			case '5':
				v2mute = !v2mute;
				this.player.setMute(1, 1, v2mute);
				break;

			case '6':
				v3mute = !v3mute;
				this.player.setMute(1, 2, v3mute);
				break;

			case 'f': {
				filterEnable ^= true;
				this.player.setFilterEnable(filterEnable);
				break;
			}

			case 'q':
				this.player.quit();
				break;

			default:
				break;
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

}
