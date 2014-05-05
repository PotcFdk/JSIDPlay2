package sidplay.consoleplayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;

import libsidplay.DriverSettings;
import libsidplay.Emulation;
import libsidplay.common.CPUClock;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import resid_builder.resid.ChipModel;
import sidplay.audio.Output;
import sidplay.ini.intf.IConfig;

public class CmdParser {

	private IConfig config;

	private SidTune tune;
	private Output output;
	private String outputFilename;
	private Emulation emulation;
	private int startTime;
	private boolean disableFilters;
	private int first;
	private int quietLevel;
	private int verboseLevel;
	private boolean debug;

	private boolean recordMode;

	public CmdParser(IConfig config) {
		this.config = config;
	}

	/**
	 * Parse command line arguments
	 * 
	 * @param argv
	 *            The command line arguments.
	 * @return
	 */
	public boolean args(final String[] argv) {
		int i = 0;
		boolean err = false;

		int userPlayLength = 0;

		String filename = null;
		if (argv.length == 0) {
			displayArgs(null);
			return false;
		}

		// default arg options
		output = Output.OUT_SOUNDCARD;
		emulation = Emulation.EMU_RESID;

		// parse command line arguments
		while (i < argv.length) {
			if (argv[i].charAt(0) == '-' && argv[i].length() > 1) {
				// help options
				if (argv[i].charAt(1) == 'h' || argv[i].equals("--help")) {
					displayArgs(null);
					return true;
				} else if (argv[i].equals("--help-debug")) {
					displayDebugArgs();
					return true;
				}

				else if (argv[i].charAt(1) == 'b') {
					startTime = parseTime(argv[i].substring(2));
					if (startTime == -1) {
						err = true;
					}
				} else if (argv[i].equals("-fd")) {
					// Override sidTune and enable the second sid
					config.getEmulation().setForceStereoTune(true);
				} else if (argv[i].startsWith("-f")) {
					if (argv[i].length() == 2) {
						err = true;
					}
					config.getAudio().setFrequency(
							Integer.valueOf(argv[i].substring(2)));
				}

				// New/No filter options
				else if (argv[i].startsWith("-nf")) {
					if (argv[i].length() == 3) {
						disableFilters = true;
					}
				}

				// Newer sid (8580)
				else if (argv[i].startsWith("-ns")) {
					switch (argv[i].charAt(3)) {
					case '1':
						config.getEmulation()
								.setUserSidModel(ChipModel.MOS8580);
						break;
					// No new sid so use old one (6581)
					case '0':
						config.getEmulation()
								.setUserSidModel(ChipModel.MOS6581);
						break;
					default:
						err = true;
					}
				}

				// Track options
				else if (argv[i].startsWith("-ols")) {
					config.getSidplay2().setLoop(true);
					config.getSidplay2().setSingle(true);
					first = Integer.valueOf(argv[i].substring(4));
				} else if (argv[i].startsWith("-ol")) {
					config.getSidplay2().setLoop(true);
					config.getSidplay2().setSingle(false);
					first = Integer.valueOf(argv[i].substring(3));
				} else if (argv[i].startsWith("-os")) {
					config.getSidplay2().setLoop(false);
					config.getSidplay2().setSingle(true);
					first = Integer.valueOf(argv[i].substring(3));
				} else if (argv[i].startsWith("-o")) {
					// User forgot track number ?
					if (argv[i].length() == 2) {
						err = true;
					}
					config.getSidplay2().setLoop(false);
					config.getSidplay2().setSingle(false);
					first = Integer.valueOf(argv[i].substring(2));
				}

				else if (argv[i].startsWith("-q")) {
					if (argv[i].length() == 2) {
						quietLevel = 1;
					} else {
						quietLevel = Integer.valueOf(argv[i].substring(2));
					}
				}

				else if (argv[i].startsWith("-t")) {
					userPlayLength = parseTime(argv[i].substring(2));
					if (userPlayLength == -1) {
						err = true;
					}
					config.getSidplay2().setUserPlayLength(userPlayLength);
				}

				// Video/Verbose Options
				else if (argv[i].equals("-vnf")) {
					config.getEmulation().setUserClockSpeed(CPUClock.NTSC);
				} else if (argv[i].equals("-vpf")) {
					config.getEmulation().setUserClockSpeed(CPUClock.PAL);
				} else if (argv[i].equals("-vn")) {
					config.getEmulation().setDefaultClockSpeed(CPUClock.NTSC);
				} else if (argv[i].equals("-vp")) {
					config.getEmulation().setDefaultClockSpeed(CPUClock.PAL);
				} else if (argv[i].startsWith("-v")) {
					if (argv[i].length() == 2) {
						verboseLevel = 1;
					} else {
						verboseLevel = Integer.valueOf(argv[i].substring(2));
					}
				}

				// File format conversions
				else if (argv[i].equals("-m")) {
					output = Output.OUT_MP3;
					outputFilename = argv[++i];
					recordMode = true;
				} else if (argv[i].equals("-w") || argv[i].equals("--wav")) {
					output = Output.OUT_WAV;
					outputFilename = argv[++i];
					recordMode = true;
				} else if (argv[i].equals("-lm")) {
					output = Output.OUT_LIVE_MP3;
					i++;
					outputFilename = argv[i];
					recordMode = true;
				} else if (argv[i].equals("-lw") || argv[i].equals("-l")) {
					output = Output.OUT_LIVE_WAV;
					outputFilename = argv[++i];
					recordMode = true;
				}

				// Hardware selection
				else if (argv[i].equals("--hardsid")) {
					emulation = Emulation.EMU_HARDSID;
					output = Output.OUT_NULL;
				}

				// These are for debug
				else if (argv[i].equals("--none")) {
					emulation = Emulation.EMU_NONE;
					output = Output.OUT_NULL;
				} else if (argv[i].equals("--nosid")) {
					emulation = Emulation.EMU_NONE;
				} else if (argv[i].equals("--cpu-debug")) {
					debug = true;
				}

				else {
					err = true;
				}

			} else {
				// Reading file name
				filename = argv[i];
			}

			if (err) {
				displayArgs(argv[i]);
				return false;
			}

			i++; // next index
		}

		if (userPlayLength != 0 && startTime >= userPlayLength) {
			System.err.println("ERROR: Start time exceeds song length!");
			return false;
		}
		// Can only loop if not creating audio files
		if (recordMode) {
			config.getSidplay2().setLoop(false);
		}
		if (filename == null) {
			return false;
		}
		try {
			try (InputStream stream = new URL(filename).openConnection()
					.getInputStream()) {
				// load from URL
				tune = SidTune.load(stream);
			} catch (MalformedURLException e) {
				// load from file
				tune = SidTune.load(new File(filename));
			}
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
			return false;
		}
		tune.setOutputFilename(outputFilename);
		return true;
	}

	/**
	 * Convert time from integer.
	 * 
	 * @param str
	 *            The time string to parse.
	 * @return The time as an integer.
	 */
	private int parseTime(final String str) {
		int sep;
		int _time;

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
			_time = val * 60;
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
				+ config.getAudio().getFrequency()
				+ ")"
				+ "\n"
				+ " -fd          force dual sid environment"
				+ "\n"

				+ " -nf[filter]  no/new SID filter emulation"
				+ "\n"
				+ " -ns[0|1]     (no) MOS 8580 waveforms (default: from tune or cfg)"
				+ "\n"

				+ " -o[l|s]<num>      start track (default: preset), looping and/or single track"
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

	public SidTune getTune() {
		return tune;
	}

	public DriverSettings createDriverSettings() {
		return new DriverSettings(output, emulation);
	}

	public int getStartTime() {
		return startTime;
	}

	public boolean isDisableFilters() {
		return disableFilters;
	}

	public int getFirst() {
		return first;
	}

	public int getQuietLevel() {
		return quietLevel;
	}

	public int getVerboseLevel() {
		return verboseLevel;
	}

	public boolean isDebug() {
		return debug;
	}

	public boolean isRecordMode() {
		return recordMode;
	}
}
