package server.netsiddev;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Vector;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public class Settings extends SIDDeviceStage {
	private static final long serialVersionUID = 1L;

	private JComboBox<AudioDevice> audioDevice;

	private JComboBox<Integer> audioBuffer;

	private JCheckBox allowExternalConnections, digiBoost, whatsSidEnable;

	private JTextField whatsSidURl, whatsSidUsername;

	private JPasswordField whatsSidPassword;

	private JFormattedTextField whatsSidCaptureTime, whatsSidMatchRetryTime, whatsSidMinimumRelativeConfidence;

	private JButton okButton;

	private Vector<AudioDevice> audioDevices;

	private Vector<Integer> audioBufferSizes;

	private SIDDeviceSettings settings;

	@SuppressWarnings("unchecked")
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

		JLabel deviceLabel = new JLabel(util.getBundle().getString("DEVICE"));
		deviceLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		audioPane.add(deviceLabel);

		audioDevices = new Vector<>();
		audioDevice = new JComboBox<>(audioDevices);
		audioDevice.setRenderer(new ItemRenderer());
		audioDevice.addActionListener(event -> setAudioDevice());
		audioDevice.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		audioPane.add(audioDevice);

		JLabel audioBufferSizeLabel = new JLabel(util.getBundle().getString("AUDIO_BUFFER_SIZE"));
		audioBufferSizeLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		audioPane.add(audioBufferSizeLabel);

		audioBufferSizes = new Vector<>(Arrays.asList(1024, 2048, 4096, 8192, 16384));
		audioBuffer = new JComboBox<>(audioBufferSizes);
		audioBuffer.setRenderer(new ItemRenderer());
		audioBuffer.addActionListener(event -> setAudioBuffer());
		audioBuffer.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		audioPane.add(audioBuffer);

		getContentPane().add(audioPane, gridBagConstants);

		JPanel connectionPane = new JPanel();
		connectionPane.setLayout(new BoxLayout(connectionPane, BoxLayout.X_AXIS));
		connectionPane.setAlignmentX(Component.CENTER_ALIGNMENT);

		TitledBorder connectionBorder = new TitledBorder(util.getBundle().getString("CONNECTION_SETTINGS"));
		connectionPane.setBorder(connectionBorder);

		JLabel allowExternalConnectionsLabal = new JLabel(util.getBundle().getString("ALLOW_EXTERNAL_CONNECTIONS"));
		allowExternalConnectionsLabal.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		connectionPane.add(allowExternalConnectionsLabal);
		allowExternalConnections = new JCheckBox();
		allowExternalConnections.addActionListener(event -> setAllowExternalConnections());
		connectionPane.add(allowExternalConnections);

		getContentPane().add(connectionPane, gridBagConstants);

		JPanel emulationPane = new JPanel();
		emulationPane.setLayout(new BoxLayout(emulationPane, BoxLayout.LINE_AXIS));
		emulationPane.setAlignmentX(Component.CENTER_ALIGNMENT);

		TitledBorder emulationBorder = new TitledBorder(util.getBundle().getString("EMULATION_SETTINGS"));
		emulationPane.setBorder(emulationBorder);

		JLabel digiBoostLabal = new JLabel(util.getBundle().getString("DIGI_BOOST"));
		digiBoostLabal.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		emulationPane.add(digiBoostLabal);
		digiBoost = new JCheckBox();
		digiBoost.addActionListener(event -> setDigiBoost());
		emulationPane.add(digiBoost);

		getContentPane().add(emulationPane, gridBagConstants);

		JPanel whatsSidPane = new JPanel();
		whatsSidPane.setLayout(new BoxLayout(whatsSidPane, BoxLayout.Y_AXIS));
		whatsSidPane.setAlignmentX(Component.CENTER_ALIGNMENT);

		TitledBorder whatsSidBorder = new TitledBorder(util.getBundle().getString("WHATSSID_SETTINGS"));
		whatsSidPane.setBorder(whatsSidBorder);

		whatsSidEnable = new JCheckBox();
		whatsSidEnable.setText(util.getBundle().getString("WHATSSID_ENABLE"));
		whatsSidEnable.addActionListener(event -> setWhatsSidEnable());
		whatsSidPane.add(whatsSidEnable);

		JLabel urlLabel = new JLabel(util.getBundle().getString("WHATSSID_URL"));
		urlLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		whatsSidPane.add(urlLabel);

		whatsSidURl = new JTextField();
		whatsSidURl.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				setWhatsSidUrl();
			}
		});
		whatsSidURl.addActionListener(event -> setWhatsSidUrl());
		whatsSidURl.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		whatsSidPane.add(whatsSidURl);

		JLabel usernameLabel = new JLabel(util.getBundle().getString("WHATSSID_USERNAME"));
		usernameLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		whatsSidPane.add(usernameLabel);

		whatsSidUsername = new JTextField();
		whatsSidUsername.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				setWhatsSidUsername();
			}
		});
		whatsSidUsername.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		whatsSidPane.add(whatsSidUsername);

		JLabel passwordLabel = new JLabel(util.getBundle().getString("WHATSSID_PASSWORD"));
		passwordLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		whatsSidPane.add(passwordLabel);

		whatsSidPassword = new JPasswordField();
		whatsSidPassword.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				setWhatsSidPassword();
			}
		});
		whatsSidPassword.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		whatsSidPane.add(whatsSidPassword);

		JLabel captureTimeLabel = new JLabel(util.getBundle().getString("WHATSSID_CAPTURE_TIME"));
		captureTimeLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		whatsSidPane.add(captureTimeLabel);

		whatsSidCaptureTime = new JFormattedTextField(NumberFormat.getIntegerInstance());
		whatsSidCaptureTime.setColumns(2);
		whatsSidCaptureTime.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				setWhatsSidCaptureTime();
			}
		});
		whatsSidCaptureTime.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		whatsSidPane.add(whatsSidCaptureTime);

		JLabel matchRetryTimeLabel = new JLabel(util.getBundle().getString("WHATSSID_MATCH_RETRY_TIME"));
		matchRetryTimeLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		whatsSidPane.add(matchRetryTimeLabel);

		whatsSidMatchRetryTime = new JFormattedTextField(NumberFormat.getIntegerInstance());
		whatsSidMatchRetryTime.setColumns(2);
		whatsSidMatchRetryTime.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				setWhatsSidMatchRetryTime();
			}
		});
		whatsSidMatchRetryTime.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		whatsSidPane.add(whatsSidMatchRetryTime);

		JLabel minimumRelativeConfidenceLabel = new JLabel(
				util.getBundle().getString("WHATSSID_MINIMUM_RELATIVE_CONFIDENCE"));
		minimumRelativeConfidenceLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		whatsSidPane.add(minimumRelativeConfidenceLabel);

		whatsSidMinimumRelativeConfidence = new JFormattedTextField(NumberFormat.getNumberInstance());
		whatsSidMinimumRelativeConfidence.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				setWhatsSidMinimumRelativeConfidence();
			}
		});
		whatsSidMinimumRelativeConfidence.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		whatsSidPane.add(whatsSidMinimumRelativeConfidence);

		getContentPane().add(whatsSidPane, gridBagConstants);

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
		audioBuffer.setSelectedItem(settings.getAudioBufferSize());
		whatsSidEnable.setSelected(settings.isWhatsSidEnable());
		whatsSidURl.setText(settings.getWhatsSidUrl());
		whatsSidUsername.setText(settings.getWhatsSidUsername());
		whatsSidPassword.setText(settings.getWhatsSidPassword());
		whatsSidCaptureTime.setValue(settings.getWhatsSidCaptureTime());
		whatsSidMatchRetryTime.setValue(settings.getWhatsSidMatchRetryTime());
		whatsSidMinimumRelativeConfidence.setValue(settings.getWhatsSidMinimumRelativeConfidence());
	}

	@Override
	public void open() {
		SwingUtilities.invokeLater(() -> okButton.requestFocusInWindow());

		super.open();
	}

	private void setAudioDevice() {
		AudioDevice device = (AudioDevice) audioDevice.getSelectedItem();
		if (device != null) {
			ClientContext.changeDevice(device.getInfo());
			settings.saveDeviceIndex(device.getIndex());
		}
	}

	private void setAudioBuffer() {
		Integer audioBufferSize = (Integer) audioBuffer.getSelectedItem();
		if (audioBufferSize != null) {
			ClientContext.setAudioBufferSize(audioBufferSize);
			settings.saveAudioBufferSize(audioBufferSize);
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

	private void setWhatsSidEnable() {
		boolean whatsSidEnable = this.whatsSidEnable.isSelected();
		settings.saveWhatsSidEnable(whatsSidEnable);
	}

	private void setWhatsSidUrl() {
		String whatsSidUrl = this.whatsSidURl.getText();
		settings.saveWhatsSidUrl(whatsSidUrl);
	}

	private void setWhatsSidUsername() {
		String whatsSidUserName = this.whatsSidUsername.getText();
		settings.saveWhatsSidUsername(whatsSidUserName);
	}

	private void setWhatsSidPassword() {
		String whatsSidPasswd = new String(this.whatsSidPassword.getPassword());
		settings.saveWhatsSidPassword(whatsSidPasswd);
	}

	private void setWhatsSidCaptureTime() {
		Number whatsSidCaptureTime = (Number) this.whatsSidCaptureTime.getValue();
		settings.saveWhatsSidCaptureTime(whatsSidCaptureTime.intValue());
	}

	private void setWhatsSidMatchRetryTime() {
		Number whatsSidMatchRetryTime = (Number) this.whatsSidMatchRetryTime.getValue();
		settings.saveWhatsSidMatchRetryTime(whatsSidMatchRetryTime.intValue());
	}

	private void setWhatsSidMinimumRelativeConfidence() {
		Number whatsSidMinimumRelativeConfidence = (Number) this.whatsSidMinimumRelativeConfidence.getValue();
		settings.saveWhatsSidMinimumRelativeConfidence(whatsSidMinimumRelativeConfidence.doubleValue());
	}

	private void okPressed() {
		settings.saveDeviceIndex(settings.getDeviceIndex());
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	private class ItemRenderer extends BasicComboBoxRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		@SuppressWarnings("rawtypes")
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			JComponent comp = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);

			comp.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
			return this;
		}
	}
}
