package applet.entities.config;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;
import sidplay.ini.intf.IC1541Section;

@Embeddable
public class DbC1541Section implements IC1541Section {

	private boolean driveOn;

	@Override
	public boolean isDriveOn() {
		return this.driveOn;
	}

	@Override
	public void setDriveOn(boolean on) {
		this.driveOn = on;
	}

	private boolean driveSoundOn;

	@Override
	public boolean isDriveSoundOn() {
		return this.driveSoundOn;
	}

	@Override
	public void setDriveSoundOn(boolean on) {
		this.driveSoundOn = on;
	}

	private boolean parallelCable;

	@Override
	public boolean isParallelCable() {
		return this.parallelCable;
	}

	@Override
	public void setParallelCable(boolean on) {
		this.parallelCable = on;
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
	private ExtendImagePolicy extendImagePolicy;

	@Override
	public void setExtendImagePolicy(ExtendImagePolicy policy) {
		this.extendImagePolicy = policy;
	}

	@Override
	public ExtendImagePolicy getExtendImagePolicy() {
		return this.extendImagePolicy;
	}

	@Enumerated(EnumType.STRING)
	private FloppyType floppyType;

	@Override
	public void setFloppyType(FloppyType floppyType) {
		this.floppyType = floppyType;
	}

	@Override
	public FloppyType getFloppyType() {
		return floppyType;
	}

}
