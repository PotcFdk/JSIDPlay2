package sidplay.audio.siddump;

import static libsidutils.siddump.SIDDumpConfiguration.SIDDumpReg.ATTACK_DECAY_1;
import static libsidutils.siddump.SIDDumpConfiguration.SIDDumpReg.FREQ_HI_1;
import static libsidutils.siddump.SIDDumpConfiguration.SIDDumpReg.FREQ_LO_1;
import static libsidutils.siddump.SIDDumpConfiguration.SIDDumpReg.PULSE_HI_1;
import static libsidutils.siddump.SIDDumpConfiguration.SIDDumpReg.PULSE_LO_1;
import static libsidutils.siddump.SIDDumpConfiguration.SIDDumpReg.SUSTAIN_RELEASE_1;
import static libsidutils.siddump.SIDDumpConfiguration.SIDDumpReg.WAVEFORM_1;

public class Channel {

	public static final String NOTE_NAME[] = { "C-0", "C#0", "D-0", "D#0", "E-0", "F-0", "F#0", "G-0", "G#0", "A-0",
			"A#0", "B-0", "C-1", "C#1", "D-1", "D#1", "E-1", "F-1", "F#1", "G-1", "G#1", "A-1", "A#1", "B-1", "C-2",
			"C#2", "D-2", "D#2", "E-2", "F-2", "F#2", "G-2", "G#2", "A-2", "A#2", "B-2", "C-3", "C#3", "D-3", "D#3",
			"E-3", "F-3", "F#3", "G-3", "G#3", "A-3", "A#3", "B-3", "C-4", "C#4", "D-4", "D#4", "E-4", "F-4", "F#4",
			"G-4", "G#4", "A-4", "A#4", "B-4", "C-5", "C#5", "D-5", "D#5", "E-5", "F-5", "F#5", "G-5", "G#5", "A-5",
			"A#5", "B-5", "C-6", "C#6", "D-6", "D#6", "E-6", "F-6", "F#6", "G-6", "G#6", "A-6", "A#6", "B-6", "C-7",
			"C#7", "D-7", "D#7", "E-7", "F-7", "F#7", "G-7", "G#7", "A-7", "A#7", "B-7" };

	private int freq, pulse, adsr, wave, note;

	public int getFreq() {
		return freq;
	}

	public int getPulse() {
		return pulse;
	}

	public int getAdsr() {
		return adsr;
	}

	public int getWave() {
		return wave;
	}

	public int getNote() {
		return note;
	}

	public void setNote(int note) {
		this.note = note;
	}

	public void read(int channel, byte[] registers) {
		freq = registers[FREQ_LO_1.getRegister() + 7 * channel] & 0xff
				| (registers[FREQ_HI_1.getRegister() + 7 * channel] & 0xff) << 8;
		pulse = (registers[PULSE_LO_1.getRegister() + 7 * channel] & 0xff
				| (registers[PULSE_HI_1.getRegister() + 7 * channel] & 0xff) << 8) & 0xfff;
		wave = registers[WAVEFORM_1.getRegister() + 7 * channel] & 0xff;
		adsr = registers[SUSTAIN_RELEASE_1.getRegister() + 7 * channel] & 0xff
				| (registers[ATTACK_DECAY_1.getRegister() + 7 * channel] & 0xff) << 8;

	}

	public void assign(Channel channel) {
		adsr = channel.adsr;
		freq = channel.freq;
		note = channel.note;
		pulse = channel.pulse;
		wave = channel.wave;

	}

	public String getNote(boolean prevChannelNote) {
		if (prevChannelNote) {
			return String.format("(%s %02X)", NOTE_NAME[note], note | 0x80);
		} else {
			return String.format(" %s %02X ", NOTE_NAME[note], note | 0x80);
		}
	}
}
