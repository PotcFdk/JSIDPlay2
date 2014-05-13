package libsidutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class SIDDumpConfiguration {
	private static final String FILE_NAME = "siddump.xml";

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

	public static class SIDDumpPlayer {
		private final String name;
		private final Collection<SIDDumpReg> regs;

		public SIDDumpPlayer(String name) {
			this.name = name;
			this.regs = new ArrayList<SIDDumpReg>();
		}

		public String getName() {
			return name;
		}

		public Collection<SIDDumpReg> getRegs() {
			return regs;
		}

		@Override
		public String toString() {
			return getName();
		}

		private void add(String reg) {
			regs.add(SIDDumpReg.valueOf(reg));
		}

	}

	private ArrayList<SIDDumpPlayer> fPlayers;

	public SIDDumpConfiguration() throws IOException, ParserConfigurationException,
			SAXException {
		configure();
	}

	public ArrayList<SIDDumpPlayer> getPlayers() {
		return fPlayers;
	}

	private void configure() throws ParserConfigurationException, SAXException,
			IOException {
		try (InputStream is = getInputStream()) {
			DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
			fac.setIgnoringElementContentWhitespace(true);
			fac.setIgnoringComments(true);
			DocumentBuilder builder = fac.newDocumentBuilder();
			parse(builder.parse(is));
		}
	}

	private InputStream getInputStream() throws FileNotFoundException {
		for (final String s : new String[] { System.getProperty("user.dir"),
				System.getProperty("user.home"), }) {
			File file = new File(s, FILE_NAME);
			if (file.exists()) {
				System.out.println("Using SIDDump file: "
						+ file.getAbsolutePath());
				return new FileInputStream(file);
			}
		}
		System.out.println("Using internal SIDDump file: " + FILE_NAME);
		return SIDDumpConfiguration.class.getResourceAsStream(FILE_NAME);
	}

	private void parse(Document siddump) {
		Element root = siddump.getDocumentElement();
		NodeList player = root.getElementsByTagName("PLAYER");
		fPlayers = new ArrayList<SIDDumpPlayer>();
		for (int i = 0; i < player.getLength(); i++) {
			fPlayers.add(createPlayer((Element) player.item(i)));
		}
	}

	private SIDDumpPlayer createPlayer(Element playerElement) {
		NamedNodeMap atts = playerElement.getAttributes();
		Node name = atts.getNamedItem("name");
		if (name != null) {
			SIDDumpPlayer player = new SIDDumpPlayer(name.getNodeValue());
			NodeList regs = playerElement.getElementsByTagName("REGS");
			NodeList reg = ((Element) regs.item(0)).getElementsByTagName("REG");
			for (int i = 0; i < reg.getLength(); i++) {
				if (reg.item(i).getFirstChild() != null) {
					player.add(reg.item(i).getFirstChild().getNodeValue());
				}
			}
			return player;
		}
		throw new RuntimeException("Invalid SIDDump configuration!");
	}
}
