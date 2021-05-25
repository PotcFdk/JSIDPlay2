package builder.exsid;

import com.sun.jna.Library;

public interface ExSID extends Library {

	int exSID_init();

	void exSID_exit();

	void exSID_reset(byte level);

	HardwareModel exSID_hwmodel();

	short exSID_hwversion();

	int exSID_clockselect(ClockSelect clockSelect);

	int exSID_audio_op(AudioOp audioOp);

	void exSID_chipselect(ChipSelect chipSelect);

	void exSID_delay(int delay);

	void exSID_clkdwrite(int delay, byte addr, byte data);

	byte exSID_clkdread(int delay, byte addr);

	String exSID_error_str();

}
