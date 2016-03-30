package libsidutils.directory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import libsidplay.components.cart.Cartridge.CRTType;

public class CartridgeDirectory {
	private static final Charset ISO88591 = Charset.forName("ISO-8859-1");

	public static Directory getDirectory(File file) throws IOException {
		Directory dir = new Directory();
		try (DataInputStream dis = new DataInputStream(
				new FileInputStream(file))) {
			final byte[] header = new byte[0x40];
			dis.readFully(header);
			CRTType type = CRTType.getType(header);
			// directory title: cartridge type
			dir.setTitle(type.toString().replace('_', '-').getBytes(ISO88591));
			// directory id: size in KB
			dir.setId(String.valueOf(file.length() >> 10).getBytes(ISO88591));
		}
		return dir;
	}

}
