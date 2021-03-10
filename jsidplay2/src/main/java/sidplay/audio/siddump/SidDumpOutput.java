package sidplay.audio.siddump;

public class SidDumpOutput {

	private String time;
	private String freq1, note1, wf1, adsr1, pul1;
	private String freq2, note2, wf2, adsr2, pul2, freq3, note3, wf3, adsr3, pul3;
	private String fcut, rc, typ, v;

	public String getTime() {
		return time;
	}

	public void setTime(String value) {
		time = value;
	}

	public String getFreq1() {
		return freq1;
	}

	public void setFreq1(String value) {
		freq1 = value;
	}

	public String getFreq2() {
		return freq2;
	}

	public void setFreq2(String value) {
		freq2 = value;
	}

	public String getFreq3() {
		return freq3;
	}

	public void setFreq3(String value) {
		freq3 = value;
	}

	public String getFreq(int c) {
		switch (c) {
		case 0:
			return freq1;
		case 1:
			return freq2;
		case 2:
			return freq3;

		default:
			return null;
		}
	}

	public void setFreq(String value, int c) {
		switch (c) {
		case 0:
			freq1 = value;
			break;
		case 1:
			freq2 = value;
			break;
		case 2:
			freq3 = value;
			break;

		default:
			break;
		}
	}

	public String getNote1() {
		return note1;
	}

	public void setNote1(String value) {
		note1 = value;
	}

	public String getNote2() {
		return note2;
	}

	public void setNote2(String value) {
		note2 = value;
	}

	public String getNote3() {
		return note3;
	}

	public void setNote3(String value) {
		note3 = value;
	}

	public String getNote(int c) {
		switch (c) {
		case 0:
			return note1;
		case 1:
			return note2;
		case 2:
			return note3;

		default:
			return null;
		}
	}

	public void setNote(String value, int c) {
		switch (c) {
		case 0:
			note1 = value;
			break;
		case 1:
			note2 = value;
			break;
		case 2:
			note3 = value;
			break;

		default:
			break;
		}
	}

	public String getWf1() {
		return wf1;
	}

	public void setWf1(String value) {
		wf1 = value;
	}

	public String getWf2() {
		return wf2;
	}

	public void setWf2(String value) {
		wf2 = value;
	}

	public String getWf3() {
		return wf3;
	}

	public void setWf3(String value) {
		wf3 = value;
	}

	public String getWf(int c) {
		switch (c) {
		case 0:
			return wf1;
		case 1:
			return wf2;
		case 2:
			return wf3;

		default:
			return null;
		}
	}

	public void setWf(String value, int c) {
		switch (c) {
		case 0:
			wf1 = value;
			break;
		case 1:
			wf2 = value;
			break;
		case 2:
			wf3 = value;
			break;

		default:
			break;
		}
	}

	public String getAdsr1() {
		return adsr1;
	}

	public void setAdsr1(String value) {
		adsr1 = value;
	}

	public String getAdsr2() {
		return adsr2;
	}

	public void setAdsr2(String value) {
		adsr2 = value;
	}

	public String getAdsr3() {
		return adsr3;
	}

	public void setAdsr3(String value) {
		adsr3 = value;
	}

	public String getAdsr(int c) {
		switch (c) {
		case 0:
			return adsr1;
		case 1:
			return adsr2;
		case 2:
			return adsr3;

		default:
			return null;
		}
	}

	public void setAdsr(String value, int c) {
		switch (c) {
		case 0:
			adsr1 = value;
			break;
		case 1:
			adsr2 = value;
			break;
		case 2:
			adsr3 = value;
			break;

		default:
			break;
		}
	}

	public String getPul1() {
		return pul1;
	}

	public void setPul1(String value) {
		pul1 = value;
	}

	public String getPul2() {
		return pul2;
	}

	public void setPul2(String value) {
		pul2 = value;
	}

	public String getPul3() {
		return pul3;
	}

	public void setPul3(String value) {
		pul3 = value;
	}

	public String getPul(int c) {
		switch (c) {
		case 0:
			return pul1;
		case 1:
			return pul2;
		case 2:
			return pul3;

		default:
			return null;
		}
	}

	public void setPul(String value, int c) {
		switch (c) {
		case 0:
			pul1 = value;
			break;
		case 1:
			pul2 = value;
			break;
		case 2:
			pul3 = value;
			break;

		default:
			break;
		}
	}

	public String getFcut() {
		return fcut;
	}

	public void setFcut(String value) {
		fcut = value;
	}

	public String getRc() {
		return rc;
	}

	public void setRc(String value) {
		rc = value;
	}

	public String getTyp() {
		return typ;
	}

	public void setTyp(String value) {
		typ = value;
	}

	public String getV() {
		return v;
	}

	public void setV(String value) {
		v = value;
	}
}
