package netsiddev;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class SettingsDialog extends EscapeDialog {
	private Map<Integer, Integer> indexMapping = new HashMap<Integer, Integer>();
	
	private static Mixer.Info[] devices;
	
	private int currentDeviceIndex = 0;
	
	private SIDDeviceSettings settings;
	
	public SettingsDialog() {
		super(null, "Settings", true);
		
		settings = SIDDeviceSettings.getInstance();
		currentDeviceIndex = settings.getDeviceIndex();
		
		setLayout(new BorderLayout());
		
        URL url = this.getClass().getResource("jsidplay2.png"); 
        Image image = Toolkit.getDefaultToolkit().getImage(url);
        setIconImage(image);

		JPanel mainPanel = new JPanel();
		JPanel innerPanel = new JPanel();
		innerPanel.setBorder(BorderFactory.createTitledBorder("Audio Settings"));
        
		innerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		innerPanel.add(createLabel("Device:"));
		innerPanel.add(createDeviceSelectionBox());
		mainPanel.add(innerPanel);
		add(mainPanel);

		add(createOkButton(), BorderLayout.PAGE_END);
		
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pack();
		setMinimumSize(getSize());
		
		setLocation(getCenteredLocation());		
	}

	private Point getCenteredLocation() {
		Dimension d = getToolkit().getScreenSize();
		Dimension s = getSize();
		return new Point((d.width - s.width)/2, (d.height - s.height)/2); 
	}

	private JComponent createLabel(final String text) {
		return new JLabel(text);
	}

	private static class DeviceItemCompare implements Comparator<DeviceItem> {
		public int compare(DeviceItem d1, DeviceItem d2) {
			String devName1 = d1.getInfo().getName();
			String devName2 = d2.getInfo().getName();
			// Make sure the Primary Sound Driver is the first entry
			if ("Primary Sound Driver".equals(devName1)) {
				return -1;
			} if ("Primary Sound Driver".equals(devName2)) {
					return 1;
			} else {
				// group the device names by device type which is most of the times
				// between brackets at the end of the string
				int index = devName1.lastIndexOf('(');
				if (index >= 0) {
					devName1 = devName1.substring(index) + devName1;
				}
				index = devName2.lastIndexOf('(');
				if (index >= 0) {
					devName2 = devName2.substring(index) + devName2;
				}
				
				return devName1.compareTo(devName2);
			}
		}
	}		
	
	private class DeviceItem {
		private Info info;
		private Integer index;
		public Info getInfo() {
			return info;
		}
		public Integer getIndex() {
			return index;
		}
		public DeviceItem(final Integer index, final Info info) {
			this.index = index;
			this.info = info;
		}
	}		
	
	private JComponent createDeviceSelectionBox() {
		JComboBox comboBox = new JComboBox();

		indexMapping.clear();
		devices = AudioSystem.getMixerInfo();
		
		List<DeviceItem> deviceItems = new ArrayList<DeviceItem>();
		
		int deviceIndex = 0;
		for (Info info : devices) {
			Mixer mixer = AudioSystem.getMixer(info);
			Line.Info lineInfo = new Line.Info(SourceDataLine.class);
			if (mixer.isLineSupported(lineInfo))
			{
				DeviceItem deviceItem = new DeviceItem(deviceIndex, info);
				deviceItems.add(deviceItem);
			}
			deviceIndex++;
		}

		Collections.sort(deviceItems, new DeviceItemCompare());
		
		int comboBoxIndex = 0;
		for (DeviceItem deviceItem : deviceItems) {
			deviceIndex = deviceItem.getIndex();
			
			indexMapping.put(comboBoxIndex, deviceIndex);
			comboBox.addItem(deviceItem.getInfo().getName());

			if (deviceIndex == currentDeviceIndex) {
				comboBox.setSelectedIndex(comboBoxIndex);
			}
			
			comboBoxIndex++;
		}			
		
		comboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JComboBox cb = (JComboBox) e.getSource();
				int comboBoxIndex = cb.getSelectedIndex();
				int deviceIndex = indexMapping.get(comboBoxIndex);
				
				ClientContext.changeDevice(devices[deviceIndex]);
				
				currentDeviceIndex = deviceIndex;
				settings.saveDeviceIndex(currentDeviceIndex);
			}
		});
		
		comboBox.setMaximumRowCount(10);
		return comboBox;
	}

	private JComponent createOkButton() {
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				settings.saveDeviceIndex(currentDeviceIndex);
				setVisible(false);
			}
		});
		
		JPanel p = new JPanel();
		p.add(okButton);
		return p;
	}
}