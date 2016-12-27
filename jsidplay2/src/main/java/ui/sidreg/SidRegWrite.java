package ui.sidreg;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SidRegWrite {
	private LongProperty absCycles = new SimpleLongProperty();
	private LongProperty relCycles = new SimpleLongProperty();;
	private IntegerProperty baseAddr = new SimpleIntegerProperty();
	private StringProperty description = new SimpleStringProperty();
	private StringProperty value = new SimpleStringProperty();

	public SidRegWrite() {
	}

	public SidRegWrite(Long absCycles, Long relCycles, Integer baseAddr, String description, String value) {
		setAbsCycles(absCycles);
		setRelCycles(relCycles);
		setBaseAddr(baseAddr);
		setDescription(description);
		setValue(value);
	}

	public Long getAbsCycles() {
		return absCycles.get();
	}

	public void setAbsCycles(Long value) {
		absCycles.set(value);
	}

	public Long getRelCycles() {
		return relCycles.get();
	}

	public void setRelCycles(Long value) {
		relCycles.set(value);
	}

	public Integer getBaseAddr() {
		return baseAddr.get();
	}

	public void setBaseAddr(Integer value) {
		baseAddr.set(value);
	}

	public String getDescription() {
		return description.get();
	}

	public void setDescription(String value) {
		description.set(value);
	}

	public String getValue() {
		return value.get();
	}

	public void setValue(String value) {
		this.value.set(value);
	}

}
