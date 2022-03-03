package builder.jhardsid;

import javax.usb.UsbDevice;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbServices;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;

public class HotplugListener implements UsbServicesListener {

	@Override
	public void usbDeviceAttached(UsbServicesEvent event) {
		UsbDevice device = event.getUsbDevice();
		System.out.println(getDeviceInfo(device) + " was added to the bus.");
	}

	@Override
	public void usbDeviceDetached(UsbServicesEvent event) {
		UsbDevice device = event.getUsbDevice();
		System.out.println(getDeviceInfo(device) + " was removed from the bus.");
	}

	private String getDeviceInfo(UsbDevice device) {
		try {
			short idVendor = device.getUsbDeviceDescriptor().idVendor();
			short idProduct = device.getUsbDeviceDescriptor().idProduct();
//			String product = device.getProductString();
//			String serial = device.getSerialNumberString();
			return device.toString() + " (" + idVendor + ") (" + idProduct + ")";
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return "Unknown USB device";
	}

	public static void main(String[] args) throws SecurityException, UsbException, InterruptedException {
		UsbServices services = UsbHostManager.getUsbServices();
		services.addUsbServicesListener(new HotplugListener());
		// Keep this program from exiting immediately
		Thread.sleep(500000);
	}

}