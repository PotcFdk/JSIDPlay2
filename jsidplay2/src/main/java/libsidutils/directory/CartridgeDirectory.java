package libsidutils.directory;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import libsidplay.components.cart.Cartridge.CRTType;

/**
 * Pseudo directory to display cartridge contents.
 * 
 * @author ken
 *
 */
public class CartridgeDirectory extends Directory {

	public CartridgeDirectory(File file) throws IOException {
		try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
			final byte[] header = new byte[0x40];
			dis.readFully(header);
			CRTType type = CRTType.getType(header);
			// directory title: cartridge type
			title = type.toString().replace('_', '-').getBytes(ISO_8859_1);
			// directory id: size in KB
			id = String.valueOf(file.length() >> 10).getBytes(ISO_8859_1);
		}
	}

}
