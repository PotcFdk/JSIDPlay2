package netsiddev;

import java.util.Comparator;

public class AudioDeviceCompare implements
		Comparator<AudioDevice> {

	private String primaryDeviceName = "Primary Sound Driver";

	@Override
	public int compare(AudioDevice d1, AudioDevice d2) {
		String devName1 = d1.getInfo().getName();
		String devName2 = d2.getInfo().getName();
		// Make sure the Primary Sound Driver is the first entry
		if (primaryDeviceName.equals(devName1)) {
			return -1;
		}
		if (primaryDeviceName.equals(devName2)) {
			return 1;
		} else {
			// group the device names by device type which is most of the
			// times between brackets at the end of the string
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
	
	public void setPrimaryDeviceName(String primaryDeviceName) {
		this.primaryDeviceName = primaryDeviceName;
	}
}