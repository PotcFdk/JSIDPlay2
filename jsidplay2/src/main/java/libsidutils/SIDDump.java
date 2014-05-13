package libsidutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SIDDump {
	protected static final String FILE_NAME = "siddump.xml";

	public enum SIDDumpReg {
		FREQ_LO_1(0x00), FREQ_HI_1(0x01), PULSE_LO_1(0x02), PULSE_HI_1(0x03), WAVEFORM_1(
				0x04), ATTACK_DECAY_1(0x05), SUSTAIN_RELEASE_1(0x06), FREQ_LO_2(
				0x07), FREQ_HI_2(0x08), PULSE_LO_2(0x09), PULSE_HI_2(0x0a), WAVEFORM_2(
				0x0b), ATTACK_DECAY_2(0x0c), SUSTAIN_RELEASE_2(0x0d), FREQ_LO_3(
				0x0e), FREQ_HI_3(0x0f), PULSE_LO_3(0x10), PULSE_HI_3(0x11), WAVEFORM_3(
				0x12), ATTACK_DECAY_3(0x13), SUSTAIN_RELEASE_3(0x14), FILTERFREQ_LO(
				0x15), FILTERFREQ_HI(0x16), FILTERCTRL(0x17), VOL(0x18);

		private byte register;

		private SIDDumpReg(int register) {
			this.register = (byte) register;
		}

		public byte getRegister() {
			return register;
		}
	}

	public static class Player {
		private final String fName;
		private final ArrayList<SIDDumpReg> fRegs;

		public Player(String name) {
			this.fName = name;
			this.fRegs = new ArrayList<SIDDumpReg>();
		}

		public String getName() {
			return fName;
		}

		public SIDDumpReg[] getRegs() {
			return fRegs.toArray(new SIDDumpReg[fRegs.size()]);
		}

		@Override
		public String toString() {
			return getName();
		}

		private void add(String reg) {
			fRegs.add(SIDDumpReg.valueOf(reg));
		}

	}

	private ArrayList<Player> fPlayers;

	public SIDDump() throws IOException, ParserConfigurationException,
			SAXException {
		open();
	}

	public ArrayList<Player> getPlayers() {
		return fPlayers;
	}

	private InputStream getConfigInputStream() throws IOException {
		File file = null;
		for (final String s : new String[] { System.getProperty("user.dir"),
				System.getProperty("user.home"), }) {
			File configPlace = new File(s, FILE_NAME);
			if (configPlace.exists()) {
				file = new File(configPlace.getParent(), FILE_NAME);
				break;
			}
		}
		// default directory
		if (file == null) {
			file = new File(System.getProperty("user.home"), FILE_NAME);
		}

		// at least use internal file
		if (!file.exists()) {
			System.out.println("Using internal SIDDump file: " + FILE_NAME);
			return SIDDump.class.getResourceAsStream(FILE_NAME);
		} else {
			System.out.println("Using SIDDump file: " + file.getAbsolutePath());
			return new FileInputStream(file);
		}
	}

	private void open() throws ParserConfigurationException, SAXException,
			IOException {
		try (InputStream is = getConfigInputStream()) {
			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			fac.setIgnoringElementContentWhitespace(true);
			fac.setIgnoringComments(true);
			DocumentBuilder builder = fac.newDocumentBuilder();
			parse(builder.parse(is));
		}
	}

	private void parse(Document siddump) {
		Element root = siddump.getDocumentElement();
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
