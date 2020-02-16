package libsidplay.sidtune;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Header has been extended for 'RSID' format<BR>
 * 
 * The following changes are present:
 * <UL>
 * <LI>id = 'RSID'
 * <LI>version = 2 or 3 only
 * <LI>play, load and speed reserved 0
 * <LI>psid specific flag reserved 0
 * <LI>init cannot be under ROMS/IO
 * <LI>load cannot be less than 0x0801 (start of basic)
 * </UL>
 * all values big-endian
 * 
 * @author Ken Händel
 * 
 */
public class PSidHeader {
	static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

	public static final int SIZE = 124;
	public static final int DATA_OFFSET_FIELD = 6;

	public PSidHeader(final byte[] header) {
		final ByteBuffer buffer = ByteBuffer.wrap(header);

		buffer.get(id);
		version = buffer.getShort();
		data = buffer.getShort();
		load = buffer.getShort();
		init = buffer.getShort();
		play = buffer.getShort();
		songs = buffer.getShort();
		start = buffer.getShort();
		speed = buffer.getInt();
		buffer.get(name);
		buffer.get(author);
		buffer.get(released);
		if (version >= 2) {
			flags = buffer.getShort();
			/* 2B */
			relocStartPage = buffer.get();
			relocPages = buffer.get();
		}
		if (version >= 3) {
			/* 2SID */
			sidChip2MiddleNybbles = buffer.get();
		}
		if (version >= 4) {
			/* 3SID */
			sidChip3MiddleNybbles = buffer.get();
		}
	}

	PSidHeader() {
	}

	public byte[] getArray() {
		final ByteBuffer buffer = ByteBuffer.allocate(SIZE);
		buffer.put(id);
		buffer.putShort(version);
		buffer.putShort(data);
		buffer.putShort(load);
		buffer.putShort(init);
		buffer.putShort(play);
		buffer.putShort(songs);
		buffer.putShort(start);
		buffer.putInt(speed);
		buffer.put(name);
		buffer.put(author);
		buffer.put(released);
		if (version >= 2) {
			buffer.putShort(flags);
			buffer.put(relocStartPage);
			buffer.put(relocPages);
		}
		if (version >= 3) {
			buffer.put(sidChip2MiddleNybbles);
		}
		if (version >= 4) {
			buffer.put(sidChip3MiddleNybbles);
		}
		return buffer.array();
	}

	/**
	 * Magic (PSID or RSID)
	 */
	byte[] id = new byte[4];

	/**
	 * 0x0001, 0x0002, 0x0003 or 0x0004
	 */
	short version;

	/**
	 * 16-bit offset to binary data in file
	 */
	short data;

	/**
	 * 16-bit C64 address to load file to
	 */
	short load;

	/**
	 * 16-bit C64 address of init subroutine
	 */
	short init;

	/**
	 * 16-bit C64 address of play subroutine
	 */
	short play;

	/**
	 * number of songs
	 */
	short songs;

	/**
	 * start song out of [1..256]
	 */
	short start;

	/**
	 * 32-bit speed info:<BR>
	 * 
	 * bit: 0=50 Hz, 1=CIA 1 Timer A (default: 60 Hz)
	 */
	int speed;

	/**
	 * ASCII strings, 31 characters long and terminated by a trailing zero For
	 * version 0x0003, all 32 chars can be used without zero termination. if less
	 * than 32 chars are used then it should be terminated with a trailing zero
	 */
	byte name[] = new byte[32];

	/**
	 * ASCII strings, 31 characters long and terminated by a trailing zero For
	 * version 0x0003, all 32 chars can be used without zero termination. if less
	 * than 32 chars are used then it should be terminated with a trailing zero
	 */
	byte author[] = new byte[32];

	/**
	 * ASCII strings, 31 characters long and terminated by a trailing zero For
	 * version 0x0003, all 32 chars can be used without zero termination. if less
	 * than 32 chars are used then it should be terminated with a trailing zero
	 */
	byte released[] = new byte[32];

	/**
	 * only version 0x0002+
	 */
	short flags;

	/**
	 * only version 0x0002+
	 */
	byte relocStartPage;

	/**
	 * only version 0x0002+
	 */
	byte relocPages;

	/**
	 * only version 0x0003 to indicate second SID chip address
	 */
	byte sidChip2MiddleNybbles;

	/**
	 * only version 0x0004 to indicate third SID chip address
	 */
	byte sidChip3MiddleNybbles;

	static String getString(byte[] info) {
		try (Scanner sc = new Scanner(new String(info, ISO_8859_1))) {
			return sc.useDelimiter("\0").next();
		} catch (NoSuchElementException e) {
			return "<?>";
		}
	}

	public String getName() {
		return getString(name);
	}
	
	public String getAuthor() {
		return getString(author);
	}
	
	public String getReleased() {
		return getString(released);
	}
}