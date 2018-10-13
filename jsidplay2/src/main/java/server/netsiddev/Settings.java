package server.netsiddev;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

public class Settings extends SIDDeviceStage {
	private static final long serialVersionUID = 1L;

	private JComboBox<AudioDevice> audioDevice;

	private JCheckBox allowExternalConnections;

	private JCheckBox digiBoost;

	private JButton okButton;

	private Vector<AudioDevice> audioDevices;

	private SIDDeviceSettings settings;

	public Settings() {
		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints gridBagConstants = new GridBagConstraints();
		gridBagConstants.weightx = 1;
		gridBagConstants.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstants.gridwidth = GridBagConstraints.REMAINDER;
		gridBagConstants.insets.set(10, 10, 10, 10);

		JPanel audioPane = new JPanel();
		audioPane.setLayout(new BoxLayout(audioPane, BoxLayout.X_AXIS));
		audioPane.setAlignmentX(Component.CENTER_ALIGNMENT);

		TitledBorder audioBorder = new TitledBorder(util.getBundle().getString("AUDIO_SETTINGS"));
		audioPane.setBorder(audioBorder);

		audioPane.add(new JLabel(util.getBundle().getString("DEVICE")));
		audioDevices = new Vector<>();
		audioDevice = new JComboBox<AudioDevice>(audioDevices);
		audioDevice.addActionListener(event -> setAudioDevice());
		audioPane.add(audioDevice);

		getContentPane().add(audioPane, gridBagConstants);

		JPanel connectionPane = new JPanel();
		connectionPane.setLayout(new BoxLayout(connectionPane, BoxLayout.X_AXIS));
		connectionPane.setAlignmentX(Component.CENTER_ALIGNMENT);

		TitledBorder connectionBorder = new TitledBorder(util.getBundle().getString("CONNECTION_SETTINGS"));
		connectionPane.setBorder(connectionBorder);

		connectionPane.add(new JLabel(util.getBundle().getString("ALLOW_EXTERNAL_CONNECTIONS")));
		allowExternalConnections = new JCheckBox();
		allowExternalConnections.addActionListener(event -> setAllowExternalConnections());
		connectionPane.add(allowExternalConnections);

		getContentPane().add(connectionPane, gridBagConstants);

		JPanel emulationPane = new JPanel();
		emulationPane.setLayout(new BoxLayout(emulationPane, BoxLayout.LINE_AXIS));
		emulationPane.setAlignmentX(Component.CENTER_ALIGNMENT);

		TitledBorder emulationBorder = new TitledBorder(util.getBundle().getString("EMULATION_SETTINGS"));
		emulationPane.setBorder(emulationBorder);

		emulationPane.add(new JLabel(util.getBundle().getString("DIGI_BOOST")));
		digiBoost = new JCheckBox();
		digiBoost.addActionListener(event -> setDigiBoost());
		emulationPane.add(digiBoost);

		getContentPane().add(emulationPane, gridBagConstants);

		okButton = new JButton(util.getBundle().getString("OK"));
		okButton.addActionListener(event -> okPressed());

		getContentPane().add(okButton, gridBagConstants);

		initialize();
	}

	private void initialize() {
		settings = SIDDeviceSettings.getInstance();
		AudioDeviceCompare cmp = new AudioDeviceCompare();
		AudioDevice selectedAudioDeviceItem = null;
		int deviceIndex = 0;
		for (Info info : AudioSystem.getMixerInfo()) {
			Mixer mixer = AudioSystem.getMixer(info);
			Line.Info lineInfo = new Line.Info(SourceDataLine.class);
			if (mixer.isLineSupported(lineInfo)) {
				AudioDevice audioDeviceItem = new AudioDevice(deviceIndex, info);
				audioDevices.add(audioDeviceItem);
				if (deviceIndex == 0) {
					// first device name is the primary device driver which can
					// be translated on some systems
					cmp.setPrimaryDeviceName(info.getName());
				}
				if (audioDeviceItem.getIndex() == settings.getDeviceIndex()) {
					selectedAudioDeviceItem = audioDeviceItem;
				}
			}
			deviceIndex++;
		}
		Collections.sort(audioDevices, cmp);
		audioDevice.setSelectedItem(selectedAudioDeviceItem);
		allowExternalConnections.setSelected(settings.getAllowExternalConnections());
		digiBoost.setSelected(settings.getDigiBoostEnabled());
	}

	public void open() throws IOException {
		okButton.requestFocus();
		super.open();
	}

	private void setAudioDevice() {
		AudioDevice device = (AudioDevice) audioDevice.getSelectedItem();
		if (device != null) {
			ClientContext.changeDevice(device.getInfo());
			settings.saveDeviceIndex(device.getIndex());
		}
	}

	private void setAllowExternalConnections() {
		boolean isAllowExternalConnections = allowExternalConnections.isSelected();
		settings.saveAllowExternalConnections(isAllowExternalConnections);
		ClientContext.applyConnectionConfigChanges();
	}

	private void setDigiBoost() {
		boolean isDigiBoost = digiBoost.isSelected();
		ClientContext.setDigiBoost(isDigiBoost);
		settings.saveDigiBoost(isDigiBoost);
	}

	private void okPressed() {
		settings.saveDeviceIndex(settings.getDeviceIndex());
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

}
