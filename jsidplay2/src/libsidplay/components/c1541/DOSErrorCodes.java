package libsidplay.components.c1541;

/**
 * 
 * CBM DOS 2.6 Input Processor Error Codes.
 * 
 * @author Ken Händel
 * 
 */
public enum DOSErrorCodes {

	/**
	 * OK (no error): The last command has been executed successfully.
	 */
	CBMDOS_IPE_OK(0),
	/**
	 * 01 FILES SCRATCHED (no error): The last SCRATCH command has been executed
	 * successfully. A further number reports the number of scratched files.
	 * This is just a control command.
	 */
	CBMDOS_IPE_FILES_SCRATCHED(1),
	/**
	 * 20 READ ERROR (block header not found): The header of a data block could
	 * not be found by the disk controller. An invalid sector number or a
	 * garbaged block header can cause this error.
	 */
	CBMDOS_IPE_READ_ERROR_BNF(20),
	/**
	 * 21 READ ERROR (SYNC was not detected): A sync marker on a track could not
	 * be detected within the tolerated time window. This error can occur, if
	 * the disk is defect or the read/write head is out of adjustment. An
	 * unformatted disk can cause this error, too.
	 */
	CBMDOS_IPE_READ_ERROR_SYNC(21),
	/**
	 * 22 READ ERROR (data block not found): The data block after the header
	 * could not be identified. Maybe the data block was formatted erroneously.
	 * The data header marker does not match the contents of the zero page
	 * address $38.
	 */
	CBMDOS_IPE_READ_ERROR_DATA(22),
	/**
	 * 23 READ ERROR (checksum error in data block): The checksum of the data
	 * block does not match with the read data. The block could be read
	 * successfully, though the disk could be damaged. A grounding problem of
	 * the floppy can cause this as well.
	 */
	CBMDOS_IPE_READ_ERROR_CHK(23),
	/**
	 * 24 READ ERROR (error during GCR-recoding): During the recoding process of
	 * a data block invalid values occurred. Just think of the fact, that more
	 * than 9 '1' bits or more than 2 '0' bits can not occur as defined in the
	 * GCR format. A grounding problem of the floppy can cause this as well.
	 */
	CBMDOS_IPE_READ_ERROR_GCR(24),
	/**
	 * 25 WRITE ERROR (error during verify): During the comparison of a recently
	 * written block with the buffer a difference was detected. This is most
	 * probably a hint to a defect of the disk.
	 */
	CBMDOS_IPE_WRITE_ERROR_VER(25),
	/**
	 * 26 WRITE PROTECT ON: (A write protected disk could not be written), you
	 * should remove the write protection.
	 */
	CBMDOS_IPE_WRITE_PROTECT_ON(26),
	/**
	 * 27 READ ERROR (checksum error in block header): During the check of the
	 * header checksum it was detected a difference. This can be caused by a
	 * defect disk or a grounding problem of the floppy.
	 */
	CBMDOS_IPE_READ_ERROR_BCHK(27),
	/**
	 * 28 WRITE ERROR (block too long): After writing a data block a sync marker
	 * could not been detected within the tolerated time window. This can be
	 * caused by an erroneously formatted disk. Maybe the data block has
	 * overwritten the following SYNC marker or a hardware defect is the cause.
	 */
	CBMDOS_IPE_WRITE_ERROR_BIG(28),
	/**
	 * 29 DISK ID MISMATCH: An uninitialized disk has been accessed. This error
	 * can also be caused by a defect block header.
	 */
	CBMDOS_IPE_DISK_ID_MISMATCH(29),
	/**
	 * 30 SYNTAX ERROR (common syntax error): A command that has been send could
	 * not be found or there was an error with the given parameters.
	 */
	CBMDOS_IPE_COMMON_SYNTAX_ERROR(30),
	/**
	 * 31 SYNTAX ERROR (invalid command): The DOS could not recognize a command.
	 * Maybe a blank in front of the command has caused this, since the command
	 * is expected at the first position.
	 */
	CBMDOS_IPE_INVALID_COMMAND(31),
	/**
	 * 32 SYNTAX ERROR (command line too long): A command length has exceeded 58
	 * characters and was sent to the floppy.
	 */
	CBMDOS_IPE_COMMAND_LINE_TOO_LONG(32),
	/**
	 * 33 SYNTAX ERROR (invalid use of a joker sign): A joker sign within a
	 * filename was not allowed for the command that was sent.
	 */
	CBMDOS_IPE_INVALID_USE_OF_A_JOKER(33),
	/**
	 * 34 SYNTAX ERROR (filename not found): The DOS could not find a filename,
	 * that was expected. Maybe the delimiting character ':' is missing.
	 */
	CBMDOS_IPE_FILENAME_NOT_FOUND(34),
	/**
	 * 39 SYNTAX ERROR (invalid command): The DOS could not recognize a command.
	 */
	CBMDOS_IPE_INVALID_COMMAND2(39),
	/**
	 * 50 RECORD NOT PRESENT: A REL file marker was positioned behind the last
	 * record. This can safely be ignored, if the next access is a write and if
	 * so, the REL file is extended automatically. An additional meaning during
	 * the use of &-files indicates a wrong checksum.
	 */
	CBMDOS_IPE_RECORD_NOT_PRESENT(50),
	/**
	 * 51 OVERFLOW IN RECORD: There was an attempt to write more than the
	 * expected count of characters into a record. During the use of '&' files
	 * it means an unexpected byte count in the next section.
	 */
	CBMDOS_IPE_OVERFLOW_IN_RECORD(51),
	/**
	 * 52 FILE TOO LARGE: Disk full. There was an attempt to expand a record of
	 * a REL file.
	 */
	CBMDOS_IPE_FILE_TOO_LARGE(52),
	/**
	 * 60 WRITE FILE OPEN: There was an attempt to read an unclosed file.
	 */
	CBMDOS_IPE_WRITE_FILE_OPEN(60),
	/**
	 * 61 FILE NOT OPEN: A file has been accessed before it was opened by DOS.
	 */
	CBMDOS_IPE_FILE_NOT_OPEN(61),
	/**
	 * 62 FILE NOT FOUND: The requested file is not available on the current
	 * disk.
	 */
	CBMDOS_IPE_FILE_NOT_FOUND(62),
	/**
	 * 63 FILE EXISTS: An already existing file was attempted to be written.
	 */
	CBMDOS_IPE_FILE_EXISTS(63),
	/**
	 * 64 FILE TYPE MISMATCH: The requested file type does not match the file
	 * type present in the directory.
	 */
	CBMDOS_IPE_FILE_TYPE_MISMATCH(64),
	/**
	 * 65 NO BLOCK: A block should be occupied by the use of a B-A command. This
	 * error indicates, that the block was already occupied. The track and
	 * sector number are assign with the next free block. If all blocks are
	 * already occupied, track and sector are set to zero.
	 */
	CBMDOS_IPE_NO_NLOCK(65),
	/**
	 * 66 ILLEGAL TRACK OR SECTOR: An illegal track or sector has been tried to
	 * access.
	 */
	CBMDOS_IPE_ILLEGAL_TRACK(66),
	/**
	 * 67 ILLEGAL TRACK OR SECTOR: The link of the data block does point to an
	 * illegal track or sector.
	 */
	CBMDOS_IPE_ILLEGAL_TRACK_OR_SECTOR(67),
	/**
	 * 70 NO CHANNEL: Either all channels are used or there was an attempt to
	 * re-use an already used channel.
	 */
	CBMDOS_IPE_NO_CHANNEL(70),
	/**
	 * 71 DIR ERROR (BAM is corrupt). The number of occupied blocks plus the
	 * number of free blocks does not match the sum of all blocks.
	 */
	CBMDOS_IPE_DIR_ERROR(71),
	/**
	 * 72 DISK FULL: All blocks are occupied or directory is full, which has a
	 * maximum of 144 entries C1541).
	 */
	CBMDOS_IPE_DISK_FULL(72),
	/**
	 * 73 CBM DOS V2.6 1541 (start-up or error message): There was an attempt to
	 * use a disk of an unknown format or the floppy has been turned on.
	 */
	CBMDOS_IPE_STARTUP_MESSAGE(73),
	/**
	 * 74 DRIVE NOT READY: There was no disk in drive, when there was an
	 * attempt to access it.
	 */
	CBMDOS_IPE_NOT_READY(74);

	/**
	 * Error code.
	 */
	private int errorCode;

	/**
	 * Create new DOS error code.
	 * 
	 * @param rc
	 *            error code
	 */
	DOSErrorCodes(final int rc) {
		this.errorCode = rc;
	}

	/**
	 * Map D64 error block info to DOS error code.
	 * 
	 * @param d64ErrorCode
	 *            D64 error info
	 * @return DOS error code
	 */
	public static final DOSErrorCodes valueOf(final int d64ErrorCode) {
		switch (d64ErrorCode) {
		case 0x0:
		case 0x1:
			return CBMDOS_IPE_OK;
		case 0x2:
			return CBMDOS_IPE_READ_ERROR_BNF;
		case 0x3:
			return CBMDOS_IPE_READ_ERROR_SYNC;
		case 0x4:
			return CBMDOS_IPE_READ_ERROR_DATA;
		case 0x5:
			return CBMDOS_IPE_READ_ERROR_CHK;
		case 0x7:
			return CBMDOS_IPE_WRITE_ERROR_VER;
		case 0x8:
			return CBMDOS_IPE_WRITE_PROTECT_ON;
		case 0x9:
			return CBMDOS_IPE_READ_ERROR_BCHK;
		case 0xA:
			return CBMDOS_IPE_WRITE_ERROR_BIG;
		case 0xB:
			return CBMDOS_IPE_DISK_ID_MISMATCH;
		case 0xF:
			return CBMDOS_IPE_NOT_READY;
		case 0x10:
			return CBMDOS_IPE_READ_ERROR_GCR;
		default:
			return CBMDOS_IPE_OK;
		}
	}
	
	/**
	 * Get error code.
	 * 
	 * @return error code
	 */
	public int getErrorCode() {
		return errorCode;
	}
}