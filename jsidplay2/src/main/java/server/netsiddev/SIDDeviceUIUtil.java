package server.netsiddev;

import java.util.ResourceBundle;

public class SIDDeviceUIUtil {

	private ResourceBundle bundle;

	protected void parse(final SIDDeviceUIPart part) {
		bundle = ResourceBundle.getBundle(part.getBundleName());
	}

	protected ResourceBundle getBundle() {
		return bundle;
	}

}
