package server.netsiddev;

public interface SIDDeviceUIPart {

	default String getBundleName() {
		return getClass().getName();
	}

}
