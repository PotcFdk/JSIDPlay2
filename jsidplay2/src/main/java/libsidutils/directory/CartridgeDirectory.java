package libsidutils.directory;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import libsidplay.components.cart.Cartridge.CRTType;

public class CartridgeDirectory {

	public static Directory getDirectory(File file) throws IOException {
		Directory dir = new Directory();
		try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
			final byte[] header = new byte[0x40];
			dis.readFully(header);
			CRTType type = CRTType.getType(header);
			// directory title: cartridge type
			dir.setTitle(type.toString().replace('_', '-').getBytes(ISO_8859_1));
			// directory id: size in KB
			dir.setId(String.valueOf(file.length() >> 10).getBytes(ISO_8859_1));
		}
		return dir;
	}

}
