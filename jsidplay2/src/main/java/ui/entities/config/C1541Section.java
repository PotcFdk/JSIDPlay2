package ui.entities.config;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;
import sidplay.ini.intf.IC1541Section;

@Embeddable
public class C1541Section implements IC1541Section {

	private BooleanProperty driveOnProperty = new SimpleBooleanProperty();
	
	@Override
	public boolean isDriveOn() {
		return driveOnProperty.get();
	}

	@Override
	public void setDriveOn(boolean driveOn) {
		driveOnProperty.set(driveOn);
	}

	public BooleanProperty driveOnProperty() {
		return driveOnProperty;
	}
	
	private BooleanProperty driveSoundOnProperty = new SimpleBooleanProperty();
	
	@Override
	public boolean isDriveSoundOn() {
		return driveSoundOnProperty.get();
	}

	@Override
	public void setDriveSoundOn(boolean driveSoundOn) {
		driveSoundOnProperty.set(driveSoundOn);
	}

	public BooleanProperty driveSoundOnProperty() {
		return driveSoundOnProperty;
	}
	
	private BooleanProperty parallelCableProperty = new SimpleBooleanProperty();
	
	@Override
	public boolean isParallelCable() {
		return parallelCableProperty.get();
	}

	@Override
	public void setParallelCable(boolean parallelCable) {
		parallelCableProperty.set(parallelCable);
	}

	public BooleanProperty parallelCableProperty() {
		return parallelCableProperty;
	}
	
	private boolean ramExpansionEnabled0;

	@Override
	public boolean isRamExpansionEnabled0() {
		return this.ramExpansionEnabled0;
	}

	@Override
	public void setRamExpansionEnabled0(boolean on) {
		this.ramExpansionEnabled0 = on;
	}

	private boolean ramExpansionEnabled1;

	@Override
	public boolean isRamExpansionEnabled1() {
		return this.ramExpansionEnabled1;
	}

	@Override
	public void setRamExpansionEnabled1(boolean on) {
		this.ramExpansionEnabled1 = on;
	}

	private boolean ramExpansionEnabled2;

	@Override
	public boolean isRamExpansionEnabled2() {
		return this.ramExpansionEnabled2;
	}

	@Override
	public void setRamExpansionEnabled2(boolean on) {
		this.ramExpansionEnabled2 = on;
	}

	private boolean ramExpansionEnabled3;

	@Override
	public boolean isRamExpansionEnabled3() {
		return this.ramExpansionEnabled3;
	}

	@Override
	public void setRamExpansionEnabled3(boolean on) {
		this.ramExpansionEnabled3 = on;
	}

	private boolean ramExpansionEnabled4;

	@Override
	public boolean isRamExpansionEnabled4() {
		return this.ramExpansionEnabled4;
	}

	@Override
	public void setRamExpansionEnabled4(boolean on) {
		this.ramExpansionEnabled4 = on;
	}

	private ExtendImagePolicy extendImagePolicy = ExtendImagePolicy.EXTEND_ACCESS;

	@Override
	public void setExtendImagePolicy(ExtendImagePolicy policy) {
		this.extendImagePolicy = policy;
	}

	@Override
	@Enumerated(EnumType.STRING)
	public ExtendImagePolicy getExtendImagePolicy() {
		return this.extendImagePolicy;
	}

	private FloppyType floppyType = FloppyType.C1541;

	@Override
	public void setFloppyType(FloppyType floppyType) {
		this.floppyType = floppyType;
	}

	@Enumerated(EnumType.STRING)
	@Override
	public FloppyType getFloppyType() {
		return floppyType;
	}

}
