package ui.entities.config;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;
import sidplay.ini.intf.IC1541Section;

@Embeddable
public class C1541Section implements IC1541Section {

	private boolean driveOn;
	
	@Transient
	@XmlTransient
	private BooleanProperty driveOnProperty;
	
	@Override
	public boolean isDriveOn() {
		if (driveOnProperty == null) {
			driveOnProperty = new SimpleBooleanProperty();
			driveOnProperty.set(driveOn);
		}
		return driveOnProperty.get();
	}

	@Override
	public void setDriveOn(boolean driveOn) {
		isDriveOn();
		driveOnProperty.set(driveOn);
		this.driveOn = driveOn;
	}

	public BooleanProperty driveOnProperty() {
		isDriveOn();
		return driveOnProperty;
	}
	
	private boolean driveSoundOn;
	
	@Transient
	@XmlTransient
	private BooleanProperty driveSoundOnProperty;
	
	@Override
	public boolean isDriveSoundOn() {
		if (driveSoundOnProperty == null) {
			driveSoundOnProperty = new SimpleBooleanProperty();
			driveSoundOnProperty.set(driveSoundOn);
		}
		return driveSoundOnProperty.get();
	}

	@Override
	public void setDriveSoundOn(boolean driveSoundOn) {
		isDriveSoundOn();
		driveSoundOnProperty.set(driveSoundOn);
		this.driveSoundOn = driveSoundOn;
	}

	public BooleanProperty driveSoundOnProperty() {
		isDriveSoundOn();
		return driveSoundOnProperty;
	}
	
	private boolean parallelCable;
	
	@Transient
	@XmlTransient
	private BooleanProperty parallelCableProperty;
	
	@Override
	public boolean isParallelCable() {
		if (parallelCableProperty == null) {
			parallelCableProperty = new SimpleBooleanProperty();
			parallelCableProperty.set(parallelCable);
		}
		return parallelCableProperty.get();
	}

	@Override
	public void setParallelCable(boolean parallelCable) {
		isParallelCable();
		parallelCableProperty.set(parallelCable);
		this.parallelCable = parallelCable;
	}

	public BooleanProperty parallelCableProperty() {
		isParallelCable();
		return parallelCableProperty;
	}
	
	private boolean ramExpansionEnabled0;

	@Override
	public boolean isRamExpansionEnabled0() {
		return this.ramExpansionEnabled0;
	}

	@Override
	public void setRamExpansion0(boolean on) {
		this.ramExpansionEnabled0 = on;
	}

	private boolean ramExpansionEnabled1;

	@Override
	public boolean isRamExpansionEnabled1() {
		return this.ramExpansionEnabled1;
	}

	@Override
	public void setRamExpansion1(boolean on) {
		this.ramExpansionEnabled1 = on;
	}

	private boolean ramExpansionEnabled2;

	@Override
	public boolean isRamExpansionEnabled2() {
		return this.ramExpansionEnabled2;
	}

	@Override
	public void setRamExpansion2(boolean on) {
		this.ramExpansionEnabled2 = on;
	}

	private boolean ramExpansionEnabled3;

	@Override
	public boolean isRamExpansionEnabled3() {
		return this.ramExpansionEnabled3;
	}

	@Override
	public void setRamExpansion3(boolean on) {
		this.ramExpansionEnabled3 = on;
	}

	private boolean ramExpansionEnabled4;

	@Override
	public boolean isRamExpansionEnabled4() {
		return this.ramExpansionEnabled4;
	}

	@Override
	public void setRamExpansion4(boolean on) {
		this.ramExpansionEnabled4 = on;
	}

	@Enumerated(EnumType.STRING)
	private ExtendImagePolicy extendImagePolicy = ExtendImagePolicy.EXTEND_ACCESS;

	@Override
	public void setExtendImagePolicy(ExtendImagePolicy policy) {
		this.extendImagePolicy = policy;
	}

	@Override
	public ExtendImagePolicy getExtendImagePolicy() {
		return this.extendImagePolicy;
	}

	@Enumerated(EnumType.STRING)
	private FloppyType floppyType = FloppyType.C1541;

	@Override
	public void setFloppyType(FloppyType floppyType) {
		this.floppyType = floppyType;
	}

	@Override
	public FloppyType getFloppyType() {
		return floppyType;
	}

}
