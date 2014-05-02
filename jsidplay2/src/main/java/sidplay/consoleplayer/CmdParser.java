package sidplay.consoleplayer;

import java.io.IOException;
import java.io.PrintStream;

import libsidplay.common.CPUClock;
import resid_builder.resid.ChipModel;
import sidplay.ConsolePlayer;

public class CmdParser {

	private ConsolePlayer player;
	private Integer frequency;

	public CmdParser(ConsolePlayer player) {
		this.player = player;
	}
	
	/**
	 * Parse command line arguments
	 * 
	 * @param argv
	 *            The command line arguments.
	 * @return
	 */
	public int args(final String[] argv) {
		int infile = -1;
		int i = 0;
		boolean err = false;

		if (argv.length == 0) {
			displayArgs(null);
			return -1;
		}

		// default arg options
		this.player.setOutput(Output.OUT_SOUNDCARD);
		this.player.setEmulation(Emulation.EMU_RESID);

		// parse command line arguments
		while (i < argv.length) {
			if (argv[i].charAt(0) == '-' && argv[i].length() > 1) {
				// help options
				if (argv[i].charAt(1) == 'h' || argv[i].equals("--help")) {
					displayArgs(null);
					return 0;
				} else if (argv[i].equals("--help-debug")) {
					displayDebugArgs();
					return 0;
				}

				else if (argv[i].charAt(1) == 'b') {
					final long time = parseTime(argv[i].substring(2));
					if (time == -1) {
						err = true;
					}
					this.player.setStartTime(time);
				} else if (argv[i].equals("-fd")) {
					// Override sidTune and enable the second sid
					this.player.setForceStereoTune(true);
				} else if (argv[i].startsWith("-f")) {
					if (argv[i].length() == 2) {
						err = true;
					}
					frequency = Integer.valueOf(argv[i].substring(2));
					this.player.setFrequency(frequency);
				}

				// New/No filter options
				else if (argv[i].startsWith("-nf")) {
					if (argv[i].length() == 3) {
						this.player.setDisableFilters();
					}
				}

				// Newer sid (8580)
				else if (argv[i].startsWith("-ns")) {
					switch (argv[i].charAt(3)) {
					case '1':
						this.player.setUserSidModel(ChipModel.MOS8580);
						break;
					// No new sid so use old one (6581)
					case '0':
						this.player.setUserSidModel(ChipModel.MOS6581);
						break;
					default:
						err = true;
					}
				}

				// Track options
				else if (argv[i].startsWith("-nols")) {
					this.player.setLoop(true);
					this.player.setSingle(true);
					this.player.setFirst(Integer.valueOf(argv[i].substring(4)));
				} else if (argv[i].startsWith("-ol")) {
					this.player.setLoop(true);
					this.player.setFirst(Integer.valueOf(argv[i].substring(3)));
				} else if (argv[i].startsWith("-os")) {
					this.player.setLoop(true);
					this.player.setFirst(Integer.valueOf(argv[i].substring(3)));
				} else if (argv[i].startsWith("-o")) {
					// User forgot track number ?
					if (argv[i].length() == 2) {
						err = true;
					}
					this.player.setFirst(Integer.valueOf(argv[i].substring(2)));
				}

				else if (argv[i].startsWith("-q")) {
					if (argv[i].length() == 2) {
						this.player.setQuietLevel(1);
					} else {
						this.player.setQuietLevel(Integer.valueOf(argv[i].substring(2)));
					}
				}

				else if (argv[i].startsWith("-t")) {
					final long time = parseTime(argv[i].substring(2));
					if (time == -1) {
						err = true;
					}
					this.player.setDefaultLength(time);
					this.player.setEnableDatabase(true);
				}

				// Video/Verbose Options
				else if (argv[i].equals("-vnf")) {
					this.player.setUserClockSpeed(CPUClock.NTSC);
				} else if (argv[i].equals("-vpf")) {
					this.player.setUserClockSpeed(CPUClock.PAL);
				} else if (argv[i].equals("-vn")) {
					this.player.setDefaultClockSpeed(CPUClock.NTSC);
				} else if (argv[i].equals("-vp")) {
					this.player.setDefaultClockSpeed(CPUClock.PAL);
				} else if (argv[i].startsWith("-v")) {
					if (argv[i].length() == 2) {
						this.player.setVerboseLevel(1);
					} else {
						this.player.setVerboseLevel(Integer.valueOf(argv[i].substring(2)));
					}
				}

				// File format conversions
				else if (argv[i].equals("-m")) {
					this.player.setOutput(Output.OUT_MP3);
					this.player.setOutputFilename(argv[++i]);
				} else if (argv[i].equals("-w") || argv[i].equals("--wav")) {
					this.player.setOutput(Output.OUT_WAV);
					this.player.setOutputFilename(argv[++i]);
				} else if (argv[i].equals("-lm")) {
					this.player.setOutput(Output.OUT_LIVE_MP3);
					i++;
					this.player.setOutputFilename(argv[i]);
				} else if (argv[i].equals("-lw") || argv[i].equals("-l")) {
					this.player.setOutput(Output.OUT_LIVE_WAV);
					i++;
					this.player.setOutputFilename(argv[i]);
				}

				// Hardware selection
				else if (argv[i].equals("--hardsid")) {
					this.player.setEmulation(Emulation.EMU_HARDSID);
					this.player.setOutput(Output.OUT_NULL);
				}

				// These are for debug
				else if (argv[i].equals("--none")) {
					this.player.setEmulation(Emulation.EMU_NONE);
					this.player.setOutput(Output.OUT_NULL);
				} else if (argv[i].equals("--nosid")) {
					this.player.setEmulation(Emulation.EMU_NONE);
				} else if (argv[i].equals("--cpu-debug")) {
					this.player.setDebug(true);
				}

				else {
					err = true;
				}

			} else {
				// Reading file name
				if (infile == -1) {
					this.player.setInFile(argv[i]);
				} else {
					err = true;
				}
			}

			if (err) {
				displayArgs(argv[i]);
				return -1;
			}

			i++; // next index
		}

		return 1;
	}

	/**
	 * Convert time from integer.
	 * 
	 * @param str
	 *            The time string to parse.
	 * @return The time as an integer.
	 */
	private long parseTime(final String str) {
		int sep;
		long _time;

		// Check for empty string
		if (str.length() == 0) {
			return -1;
		}

		sep = str.lastIndexOf(':');
		if (sep == -1) {
			// User gave seconds
			_time = Integer.valueOf(str);
		} else {
			// Read in MM:SS format
			int val;
			val = Integer.valueOf(str.substring(0, sep));
			if (val < 0 || val > 99) {
				return -1;
			}
			_time = (long) val * 60;
			val = Integer.valueOf(str.substring(sep + 1));
			if (val < 0 || val > 59) {
				return -1;
			}
			_time += val;
		}

		return _time;
	}

	@SuppressWarnings("resource")
	private void displayArgs(final String arg) {
		final PrintStream out = arg != null ? System.err : System.out;

		if (arg != null) {
			out.println("Option Error: " + arg);
		} else {
			out.println("Syntax: java -jar jsidplay2_console.jar [-<option>...] <datafile>");
		}

		out.println("Options:" + "\n" + " --help|-h    display this screen"
				+ "\n" + " --help-debug debug help menu" + "\n"
				+ " -b<num>      set start time in [m:]s format (default 0)"
				+ "\n"

				+ " -f<num>      set frequency in Hz (default: "
				+ frequency
				+ ")"
				+ "\n"
				+ " -fd          force dual sid environment"
				+ "\n"

				+ " -nf[filter]  no/new SID filter emulation"
				+ "\n"
				+ " -ns[0|1]     (no) MOS 8580 waveforms (default: from tune or cfg)"
				+ "\n"

				+ " -o<l|s>      looping and/or single track"
				+ "\n"
				+ " -o<num>      start track (default: preset)"
				+ "\n"

				+ " -t<num>      set play length in [m:]s format (0 is endless)"
				+ "\n"

				+ " -<v[level]|q>       verbose (level=0,1,2) or quiet (no time display) output"
				+ "\n"
				+ " -v[p|n][f]   set VIC PAL/NTSC clock speed (default: defined by song)"
				+ "\n"
				+ "              Use 'f' to force the clock by preventing speed fixing"
				+ "\n"

				+ " -w name     create wav file"
				+ "\n -m name     create mp3 file"
				+ "\n -lm name    create mp3 file and Java Sound"
				+ "\n -lw name    create wav file and Java Sound");
		out.println(" --hardsid enable hardsid support\n");
		out.println("\n"
		// Changed to new homepage address
				+ "Home Page: http://jsidplay2.sourceforge.net/" + "\n");
		System.out.println("<press return>");
		try {
			System.in.read();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private void displayDebugArgs() {
		final PrintStream out = System.out;

		out.println("Debug Options:" + "\n"
				+ " --cpu-debug   display cpu register and assembly dumps"
				+ "\n" + " --delay=<num> simulate c64 power on delay" + "\n"

				+ " --wav<file>   wav file output device" + "\n"
				+ " --none        no audio output device" + "\n"
				+ " --nosid       no sid emulation" + "\n");
		System.out.println("<press return>");
		try {
			System.in.read();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

}
