package libsidutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SIDDump {
	protected static final String FILE_NAME = "siddump.xml";

	//
	// SID registers
	//

	public static final int FREQ_LO_1 = 0x00;

	public static final int FREQ_HI_1 = 0x01;

	public static final int PULSE_LO_1 = 0x02;

	public static final int PULSE_HI_1 = 0x03;

	public static final int WAVEFORM_1 = 0x04;

	public static final int ATTACK_DECAY_1 = 0x05;

	public static final int SUSTAIN_RELEASE_1 = 0x06;

	public static final int FREQ_LO_2 = 0x07;

	public static final int FREQ_HI_2 = 0x08;

	public static final int PULSE_LO_2 = 0x09;

	public static final int PULSE_HI_2 = 0x0a;

	public static final int WAVEFORM_2 = 0x0b;

	public static final int ATTACK_DECAY_2 = 0x0c;

	public static final int SUSTAIN_RELEASE_2 = 0x0d;

	public static final int FREQ_LO_3 = 0x0e;

	public static final int FREQ_HI_3 = 0x0f;

	public static final int PULSE_LO_3 = 0x10;

	public static final int PULSE_HI_3 = 0x11;

	public static final int WAVEFORM_3 = 0x12;

	public static final int ATTACK_DECAY_3 = 0x13;

	public static final int SUSTAIN_RELEASE_3 = 0x14;

	public static final int FILTERFREQ_LO = 0x15;

	public static final int FILTERFREQ_HI = 0x16;

	public static final int FILTERCTRL = 0x17;

	public static final int VOL = 0x18;

	public static class Player {
		private final String fName;
		private final ArrayList<Byte> fRegs;

		public Player(String name) {
			this.fName = name;
			this.fRegs = new ArrayList<Byte>();
		}

		public String getName() {
			return fName;
		}

		public Byte[] getBytes() {
			return fRegs.toArray(new Byte[fRegs.size()]);
		}

		void add(String reg) {
			fRegs.add(name2reg(reg));
		}

		private Byte name2reg(String reg) {
			if ("VOL".equals(reg)) {
				return VOL;

			} else if ("ATTACK_DECAY_3".equals(reg)) {
				return ATTACK_DECAY_3;
			} else if ("SUSTAIN_RELEASE_3".equals(reg)) {
				return SUSTAIN_RELEASE_3;
			} else if ("WAVEFORM_3".equals(reg)) {
				return WAVEFORM_3;
			} else if ("PULSE_LO_3".equals(reg)) {
				return PULSE_LO_3;
			} else if ("PULSE_HI_3".equals(reg)) {
				return PULSE_HI_3;
			} else if ("FREQ_LO_3".equals(reg)) {
				return FREQ_LO_3;
			} else if ("FREQ_HI_3".equals(reg)) {
				return FREQ_HI_3;

			} else if ("ATTACK_DECAY_2".equals(reg)) {
				return ATTACK_DECAY_2;
			} else if ("SUSTAIN_RELEASE_2".equals(reg)) {
				return SUSTAIN_RELEASE_2;
			} else if ("WAVEFORM_2".equals(reg)) {
				return WAVEFORM_2;
			} else if ("PULSE_LO_2".equals(reg)) {
				return PULSE_LO_2;
			} else if ("PULSE_HI_2".equals(reg)) {
				return PULSE_HI_2;
			} else if ("FREQ_LO_2".equals(reg)) {
				return FREQ_LO_2;
			} else if ("FREQ_HI_2".equals(reg)) {
				return FREQ_HI_2;

			} else if ("ATTACK_DECAY_1".equals(reg)) {
				return ATTACK_DECAY_1;
			} else if ("SUSTAIN_RELEASE_1".equals(reg)) {
				return SUSTAIN_RELEASE_1;
			} else if ("WAVEFORM_1".equals(reg)) {
				return WAVEFORM_1;
			} else if ("PULSE_LO_1".equals(reg)) {
				return PULSE_LO_1;
			} else if ("PULSE_HI_1".equals(reg)) {
				return PULSE_HI_1;
			} else if ("FREQ_LO_1".equals(reg)) {
				return FREQ_LO_1;
			} else if ("FREQ_HI_1".equals(reg)) {
				return FREQ_HI_1;

			} else if ("FILTERFREQ_LO".equals(reg)) {
				return FILTERFREQ_LO;
			} else if ("FILTERFREQ_HI".equals(reg)) {
				return FILTERFREQ_HI;
			} else if ("FILTERCTRL".equals(reg)) {
				return FILTERCTRL;
			} else {
				throw new RuntimeException("Invalid siddump database entry: " + reg);
			}
		}
	}

	private Document siddump;

	ArrayList<Player> fPlayers;

	public SIDDump() {
		try {
			open(findConfigFile());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public ArrayList<Player> getPlayers() {
		return fPlayers;
	}

	private InputStream findConfigFile() {
		InputStream is = null;
		String path = System.getProperty("user.dir");
		String configPath;

		if (path == null) {
			path = "";
		}

		configPath = String.format("%s/%s", path, FILE_NAME);

		// Opens an existing file or creates a new one
		File sidDumpFile = new File(configPath.replace("\\", "/"));
		if (!sidDumpFile.exists()) {
			path = System.getProperty("user.home");
			if (path == null) {
				path = "";
			}

			configPath = String.format("%s/%s", path, FILE_NAME);
			sidDumpFile = new File(configPath.replace("\\", "/"));
			if (!sidDumpFile.exists()) {

				is = SIDDump.class.getResourceAsStream(FILE_NAME);
				System.out.println("Using internal SIDDump file: libsidutils/" + FILE_NAME);
				return is;
			}
		}
		try {
			is = new FileInputStream(sidDumpFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.out.println("Using SIDDump file: " + sidDumpFile.getAbsolutePath());

		return is;
	}

	private void open(final InputStream is) throws Exception {
		try {
			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			fac.setIgnoringElementContentWhitespace(true);
			fac.setIgnoringComments(true);
			DocumentBuilder builder = fac.newDocumentBuilder();
			siddump = builder.parse(is);

			parse();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void parse() {
		Element root = siddump.getDocumentElement();
		if (root == null) {
			return;
		}

		NodeList player = root.getElementsByTagName("PLAYER");
		fPlayers = new ArrayList<Player>();
		for (int i = 0; i < player.getLength(); i++) {
			createPlayer((Element) player.item(i));
		}
	}

	private void createPlayer(Element player) {
		NamedNodeMap atts = player.getAttributes();
		Node name = atts.getNamedItem("name");
		if (name != null) {
			Player pl = new Player(name.getNodeValue());
			NodeList regs = player.getElementsByTagName("REGS");
			if (regs.getLength() == 0) {
				return;
			}
			NodeList reg = ((Element) regs.item(0)).getElementsByTagName("REG");
			for (int i = 0; i < reg.getLength(); i++) {
				if (reg.item(i).getFirstChild() != null) {
					pl.add(reg.item(i).getFirstChild().getNodeValue());
				}
			}
			fPlayers.add(pl);
		}
	}
}
