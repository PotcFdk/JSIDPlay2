package sidplay.consoleplayer;

import java.io.IOException;

import libsidplay.sidtune.SidTune.Model;
import libsidplay.sidtune.SidTuneInfo;
import sidplay.ConsolePlayer;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IConsoleSection;
import sidplay.ini.intf.IEmulationSection;

public class ConsoleIO {

	private ConsolePlayer player;

	private boolean v1mute, v2mute, v3mute;

	private boolean filterEnable;

	public ConsoleIO(ConsolePlayer player) {
		this.player = player;
	}
	
	public void menu(IConfig iniCfg,
			SidTuneInfo tuneInfo) {
		final IConsoleSection console = iniCfg.getConsole();
		final IEmulationSection emulation = iniCfg.getEmulation();
		filterEnable = emulation.isFilter();
		if (this.player.getQuietLevel() > 1) {
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
		if (tuneInfo.numberOfInfoStrings > 0) {
			if (tuneInfo.numberOfInfoStrings == 3) {
				System.out.println(String.format("%c Title        : %37s %c",
						console.getVertical(), tuneInfo.infoString[0],
						console.getVertical()));
				System.out.println(String.format("%c Author       : %37s %c",
						console.getVertical(), tuneInfo.infoString[1],
						console.getVertical()));
				System.out.println(String.format("%c Released     : %37s %c",
						console.getVertical(), tuneInfo.infoString[2],
						console.getVertical()));
			} else {
				for (int i = 0; i < tuneInfo.numberOfInfoStrings; i++) {
					System.out.println(String.format(
							"%c Description  : %37s %c", console.getVertical(),
							tuneInfo.infoString[i], console.getVertical()));
				}
			}
			System.out.println(String.format("%c%s%c",
					console.getJunctionLeft(),
					setfill(console.getHorizontal(), 54),
					console.getJunctionRight()));
		}
		if (this.player.getVerboseLevel() != 0) {
			System.out.println(String.format("%c File format  : %37s %c",
					console.getVertical(), tuneInfo.getClass().getSimpleName(),
					console.getVertical()));
			System.out.println(String.format("%c Filename(s)  : %37s %c",
					console.getVertical(), tuneInfo.file.getName(),
					console.getVertical()));
		}
		System.out.print(String.format("%c Playlist     : ",
				console.getVertical()));
		{ // This will be the format used for playlists
			int i = 1;
			if (!this.player.isSingle()) {
				i = this.player.getSelected();
				i -= this.player.getFirst() - 1;
				if (i < 1) {
					i += this.player.getSongs();
				}
			}
			System.out.println(String.format("%37s %c", i + "/" + this.player.getSongs()
					+ " (tune " + tuneInfo.currentSong + "/" + tuneInfo.songs
					+ "[" + tuneInfo.startSong + "])"
					+ (this.player.isLoop() ? " [LOOPING]" : ""), console.getVertical()));
		}
		if (this.player.getVerboseLevel() > 0) {
			System.out.println(String.format("%c%s%c", console.getBottomLeft(),
					setfill(console.getHorizontal(), 54),
					console.getBottomRight()));
			System.out.println(String.format("%c Song Speed   : %37s %c",
					console.getVertical(), this.player.getSongSpeed(this.player.getSelected()),
					console.getVertical()));
		}
		System.out.print(String.format("%c Song Length  : ",
				console.getVertical()));
		if (this.player.getStop() != 0) {
			final String time = String.format("%02d:%02d",
					(this.player.getStop() / 60 % 100), (this.player.getStop() % 60));
			System.out.print(String.format("%37s %c", "" + time,
					console.getVertical()));
		} else if (this.player.isValid()) {
			System.out.print(String.format("%37s %c", "FOREVER",
					console.getVertical()));
		} else {
			System.out.print(String.format("%37s %c", "UNKNOWN",
					console.getVertical()));
		}
		System.out.println();
		if (this.player.getVerboseLevel() > 0) {
			System.out.println(String.format("%c%s%c", console.getBottomLeft(),
					setfill(console.getHorizontal(), 54),
					console.getBottomRight()));
			StringBuffer line = new StringBuffer();
			// Display PSID Driver location
			line.append("DRIVER = ");
			if (tuneInfo.determinedDriverAddr == 0) {
				line.append("NOT PRESENT");
			} else {
				line.append(String.format("$%04x",
						tuneInfo.determinedDriverAddr));
				line.append(String.format("-$%04x",
						tuneInfo.determinedDriverAddr
								+ tuneInfo.determinedDriverLength - 1));
			}
			if (tuneInfo.playAddr == 0xffff) {
				line.append(String.format(", SYS = $%04x", tuneInfo.initAddr));
			} else {
				line.append(String.format(", INIT = $%04x", tuneInfo.initAddr));
			}

			System.out.println(String.format("%c Addresses    : %37s %c",
					console.getVertical(), line.toString(),
					console.getVertical()));
			line = new StringBuffer();
			line.append(String.format("LOAD   = $%04x", tuneInfo.loadAddr));
			line.append(String.format("-$%04x", tuneInfo.loadAddr
					+ tuneInfo.c64dataLen - 1));
			if (tuneInfo.playAddr != 0xffff) {
				line.append(String.format(", PLAY = $%04x", tuneInfo.playAddr));
			}
			System.out.println(String.format("%c              : %37s %c",
					console.getVertical(), line.toString(),
					console.getVertical()));

			line = new StringBuffer();
			line.append(String.format("Filter = %s", (filterEnable ? "Yes"
					: "No")));
			/* XXX ignores 2nd SID */
			line.append(String.format(", Model = %s",
					(tuneInfo.sid1Model == Model.MOS8580 ? "8580" : "6581")));
			System.out.println(String.format("%c SID Details  : %37s %c",
					console.getVertical(), line.toString(),
					console.getVertical()));

			if (this.player.getVerboseLevel() > 1) {
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
