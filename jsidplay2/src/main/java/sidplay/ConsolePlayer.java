package sidplay;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import libsidplay.Player;
import libsidplay.player.State;
import libsidutils.SidDatabase;
import libsidutils.cpuparser.CPUParser;
import sidplay.consoleplayer.CmdParser;
import sidplay.consoleplayer.ConsoleIO;
import sidplay.ini.IniConfig;

public class ConsolePlayer {
	private final CmdParser cmdParser;
	private final Player player;

	public ConsolePlayer() {
		this.player = new Player(new IniConfig(true));
		this.cmdParser = new CmdParser(player.getConfig());
	}

	public void run(String[] args) {
		if (!cmdParser.args(args)) {
			System.exit(1);
		}
		player.setDebug(cmdParser.isDebug() ? CPUParser.getInstance() : null);
		player.setTune(cmdParser.getTune());
		player.setDriverSettings(cmdParser.createDriverSettings());
		player.getTimer().setStart(cmdParser.getStartTime());
		player.getConfig().getEmulation()
				.setFilter(!cmdParser.isDisableFilters());

		// Select the desired track
		// and also mark the play-list start
		player.getTrack().setFirst(
				player.getTune().selectSong(cmdParser.getFirst()));
		player.getTrack().setSelected(player.getTrack().getFirst());
		player.getTrack().setSongs(player.getTune().getInfo().songs);
		if (player.getConfig().getSidplay2().isSingle()) {
			player.getTrack().setSongs(1);
		}
		if (player.getConfig().getSidplay2().getUserPlayLength() == 0) {
			String hvscRoot = player.getConfig().getSidplay2().getHvsc();
			if (hvscRoot != null) {
				File file = new File(hvscRoot, SidDatabase.SONGLENGTHS_FILE);
				try (FileInputStream input = new FileInputStream(file)) {
					player.setSidDatabase(new SidDatabase(input));
				} catch (IOException e) {
					// silently ignored!
				}
			}
			if (cmdParser.isRecordMode()
					&& (!player.getConfig().getSidplay2().isEnableDatabase() || player
							.getSongLength(player.getTune()) <= 0)) {
				System.err
						.println("ERROR: unknown song length in record mode"
								+ " (please use option -t or configure song length database)");
				System.exit(1);
			}
		}
		ConsoleIO consoleIO = new ConsoleIO(player.getConfig(), player,
				cmdParser.getQuietLevel(), cmdParser.getVerboseLevel());
		player.setMenuHook((obj) -> consoleIO.menu(player.getTune(),
				player.getTrack(), player.getTimer()));

		player.setInteractivityHook((obj) -> {
			try {
				if (cmdParser.getQuietLevel() < 2
						&& (player.stateProperty().get() == State.PAUSED || System.in
								.available() != 0)) {
					consoleIO.decodeKeys();
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		});
		player.startC64();
	}

	public static void main(final String[] args) {
		new ConsolePlayer().run(args);
	}

}
