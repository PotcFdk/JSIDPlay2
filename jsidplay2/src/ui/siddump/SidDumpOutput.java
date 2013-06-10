package ui.siddump;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SidDumpOutput {
	private StringProperty time = new SimpleStringProperty();
	private StringProperty freq1 = new SimpleStringProperty();
	private StringProperty note1 = new SimpleStringProperty();
	private StringProperty wf1 = new SimpleStringProperty();
	private StringProperty adsr1 = new SimpleStringProperty();
	private StringProperty pul1 = new SimpleStringProperty();
	private StringProperty freq2 = new SimpleStringProperty();
	private StringProperty note2 = new SimpleStringProperty();
	private StringProperty wf2 = new SimpleStringProperty();
	private StringProperty adsr2 = new SimpleStringProperty();
	private StringProperty pul2 = new SimpleStringProperty();
	private StringProperty freq3 = new SimpleStringProperty();
	private StringProperty note3 = new SimpleStringProperty();
	private StringProperty wf3 = new SimpleStringProperty();
	private StringProperty adsr3 = new SimpleStringProperty();
	private StringProperty pul3 = new SimpleStringProperty();
	private StringProperty fcut = new SimpleStringProperty();
	private StringProperty rc = new SimpleStringProperty();
	private StringProperty typ = new SimpleStringProperty();
	private StringProperty v = new SimpleStringProperty();

	public String getTime() {
		return time.get();
	}

	public void setTime(String value) {
		time.set(value);
	}

	public String getFreq1() {
		return freq1.get();
	}

	public void setFreq1(String value) {
		freq1.set(value);
	}

	public String getFreq2() {
		return freq2.get();
	}

	public void setFreq2(String value) {
		freq2.set(value);
	}

	public String getFreq3() {
		return freq3.get();
	}

	public void setFreq3(String value) {
		freq3.set(value);
	}

	public String getFreq(int c) {
		switch (c) {
		case 0:
			return freq1.get();
		case 1:
			return freq2.get();
		case 2:
			return freq3.get();

		default:
			return null;
		}
	}

	public void setFreq(String value, int c) {
		switch (c) {
		case 0:
			freq1.set(value);
			break;
		case 1:
			freq2.set(value);
			break;
		case 2:
			freq3.set(value);
			break;

		default:
			break;
		}
	}

	public String getNote1() {
		return note1.get();
	}

	public void setNote1(String value) {
		note1.set(value);
	}

	public String getNote2() {
		return note2.get();
	}

	public void setNote2(String value) {
		note2.set(value);
	}

	public String getNote3() {
		return note3.get();
	}

	public void setNote3(String value) {
		note3.set(value);
	}

	public String getNote(int c) {
		switch (c) {
		case 0:
			return note1.get();
		case 1:
			return note2.get();
		case 2:
			return note3.get();

		default:
			return null;
		}
	}

	public void setNote(String value, int c) {
		switch (c) {
		case 0:
			note1.set(value);
			break;
		case 1:
			note2.set(value);
			break;
		case 2:
			note3.set(value);
			break;

		default:
			break;
		}
	}

	public String getWf1() {
		return wf1.get();
	}

	public void setWf1(String value) {
		wf1.set(value);
	}

	public String getWf2() {
		return wf2.get();
	}

	public void setWf2(String value) {
		wf2.set(value);
	}

	public String getWf3() {
		return wf3.get();
	}

	public void setWf3(String value) {
		wf3.set(value);
	}

	public String getWf(int c) {
		switch (c) {
		case 0:
			return wf1.get();
		case 1:
			return wf2.get();
		case 2:
			return wf3.get();

		default:
			return null;
		}
	}

	public void setWf(String value, int c) {
		switch (c) {
		case 0:
			wf1.set(value);
			break;
		case 1:
			wf2.set(value);
			break;
		case 2:
			wf3.set(value);
			break;

		default:
			break;
		}
	}

	public String getAdsr1() {
		return adsr1.get();
	}

	public void setAdsr1(String value) {
		adsr1.set(value);
	}

	public String getAdsr2() {
		return adsr2.get();
	}

	public void setAdsr2(String value) {
		adsr2.set(value);
	}

	public String getAdsr3() {
		return adsr3.get();
	}

	public void setAdsr3(String value) {
		adsr3.set(value);
	}

	public String getAdsr(int c) {
		switch (c) {
		case 0:
			return adsr1.get();
		case 1:
			return adsr2.get();
		case 2:
			return adsr3.get();

		default:
			return null;
		}
	}

	public void setAdsr(String value, int c) {
		switch (c) {
		case 0:
			adsr1.set(value);
			break;
		case 1:
			adsr2.set(value);
			break;
		case 2:
			adsr3.set(value);
			break;

		default:
			break;
		}
	}

	public String getPul1() {
		return pul1.get();
	}

	public void setPul1(String value) {
		pul1.set(value);
	}

	public String getPul2() {
		return pul2.get();
	}

	public void setPul2(String value) {
		pul2.set(value);
	}

	public String getPul3() {
		return pul3.get();
	}

	public void setPul3(String value) {
		pul3.set(value);
	}

	public String getPul(int c) {
		switch (c) {
		case 0:
			return pul1.get();
		case 1:
			return pul2.get();
		case 2:
			return pul3.get();

		default:
			return null;
		}
	}

	public void setPul(String value, int c) {
		switch (c) {
		case 0:
			pul1.set(value);
			break;
		case 1:
			pul2.set(value);
			break;
		case 2:
			pul3.set(value);
			break;

		default:
			break;
		}
	}

	public String getFcut() {
		return fcut.get();
	}

	public void setFcut(String value) {
		fcut.set(value);
	}

	public String getRc() {
		return rc.get();
	}

	public void setRc(String value) {
		rc.set(value);
	}

	public String getTyp() {
		return typ.get();
	}

	public void setTyp(String value) {
		typ.set(value);
	}

	public String getV() {
		return v.get();
	}

	public void setV(String value) {
		v.set(value);
	}
}
