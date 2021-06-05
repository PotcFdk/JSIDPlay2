package builder.exsid;

import com.sun.jna.Library;

public interface ExSID extends Library {

	/**
	 * Returns a string describing the last recorded error.
	 * 
	 * @return error message (max 256 bytes long).
	 */
	String exSID_error_str();

	/**
	 * Device init routine. Must be called once before any operation is attempted on
	 * the device. Opens first available device, and sets various parameters:
	 * baudrate, parity, flow control and USB latency, and finally clears the RX and
	 * TX buffers.
	 * 
	 * @return 0 on success, !0 otherwise.
	 */
	int exSID_init();

	/**
	 * Device exit routine. Must be called to release the device. Resets the SIDs
	 * and clears RX/TX buffers, releases all resources allocated in exSID_init().
	 */
	void exSID_exit();

	/**
	 * SID reset routine. Performs a hardware reset on the SIDs.
	 * 
	 * <B>Note:</B> since the reset procedure in firmware will stall the device,
	 * reset forcefully waits for enough time before resuming execution via a call
	 * to usleep();
	 * 
	 * @param volume volume to set the SIDs to after reset.
	 */
	void exSID_reset(byte volume);

	/**
	 * exSID+ clock selection routine. Selects between PAL, NTSC and 1MHz clocks.
	 * 
	 * <B>Note:</B> upon clock change the hardware resync itself and resets the
	 * SIDs, which takes approximately 50us: this function waits for enough time
	 * before resuming execution via a call to usleep(); Output should be muted
	 * before execution
	 * 
	 * @param clock clock selector value.
	 * @return execution status
	 */
	int exSID_clockselect(ClockSelect clock);

	/**
	 * exSID+ audio operations routine. Selects the audio mixing / muting option.
	 * Only implemented in exSID+ devices.
	 * 
	 * <B>Warning:</B> all these operations (excepting unmuting obviously) will mute
	 * the output by default.
	 * 
	 * <B>Note:</B> no accounting for SID cycles consumed.
	 * 
	 * @param operation audio operation value.
	 * @return execution status
	 */
	int exSID_audio_op(AudioOp operation);

	/**
	 * SID chipselect routine. Selects which SID will play the tunes. If neither
	 * CHIP0 or CHIP1 is chosen, both SIDs will operate together. Accounts for
	 * elapsed cycles.
	 * 
	 * @param chip SID selector value.
	 */
	void exSID_chipselect(ChipSelect chip);

	/**
	 * Device hardware model. Queries the driver for the hardware model currently
	 * identified.
	 * 
	 * @return hardware model, null on error.
	 */
	HardwareModel exSID_hwmodel();

	/**
	 * Hardware and firmware version of the device. Queries the device for the
	 * hardware revision and current firmware version and returns both in the form
	 * of a 16bit integer: MSB is an ASCII character representing the hardware
	 * revision (e.g. 0x42 = "B"), and LSB is a number representing the firmware
	 * version in decimal integer. Does NOT account for elapsed cycles.
	 * 
	 * @return version information as described above.
	 */
	short exSID_hwversion();

	/**
	 * Cycle accurate delay routine. Applies the most efficient strategy to delay
	 * for cycles SID clocks while leaving enough lead time for an I/O operation.
	 * 
	 * @param cycles how many SID clocks to loop for.
	 */
	void exSID_delay(int cycles);

	/**
	 * Timed write routine, attempts cycle-accurate writes. This function will be
	 * cycle-accurate provided that no two consecutive reads or writes are less than
	 * write_cycles apart and the leftover delay is &lt;= max_adj SID clock cycles.
	 * 
	 * @param cycles how many SID clocks to wait before the actual data write.
	 * @param addr   target address.
	 * @param data   data to write at that address.
	 */
	void exSID_clkdwrite(int cycles, byte addr, byte data);

	/**
	 * BLOCKING Timed read routine, attempts cycle-accurate reads. The following
	 * description is based on exSID (standard). This function will be
	 * cycle-accurate provided that no two consecutive reads or writes are less than
	 * XS_CYCIO apart and leftover delay is &lt;= max_adj SID clock cycles. Read
	 * result will only be available after a full XS_CYCIO, giving clkdread() the
	 * same run time as clkdwrite(). There's a 2-cycle negative adjustment in the
	 * code because that's the actual offset from the write calls ('/' denotes
	 * falling clock edge latch), which the following ASCII tries to illustrate:
	 * <br />
	 * Write looks like this in firmware:
	 * 
	 * <pre>
	 * &gt; ...|_/_|...
	 * ...end of data byte read | cycle during which write is enacted / next cycle | etc... <br />
	 * </pre>
	 * 
	 * Read looks like this in firmware:
	 * 
	 * <pre>
	 * &gt; ...|_|_|_/_|_|...
	 * ...end of address byte read | 2 cycles for address processing | cycle during which SID is read /
	 *	then half a cycle later the CYCCHR-long data TX starts, cycle completes | another cycle | etc...
	 * </pre>
	 *
	 * This explains why reads happen a relative 2-cycle later than then should with
	 * respect to writes. <B>Note:</B> The actual time the read will take to
	 * complete depends on the USB bus activity and settings. It *should* complete
	 * in XS_USBLAT ms, but not less, meaning that read operations are bound to
	 * introduce timing inaccuracy. As such, this function is only really provided
	 * as a proof of concept but SHOULD BETTER BE AVOIDED.
	 * 
	 * @param cycles how many SID clocks to wait before the actual data read.
	 * @param addr   target address.
	 * @return data read from address.
	 */
	byte exSID_clkdread(int cycles, byte addr);

}
