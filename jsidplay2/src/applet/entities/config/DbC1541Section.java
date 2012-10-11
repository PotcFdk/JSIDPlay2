package applet.entities.config;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;
import sidplay.ini.intf.IC1541Section;

@Embeddable
public class DbC1541Section implements IC1541Section {

	private boolean isDriveOn;

	@Override
	public boolean isDriveOn() {
		return this.isDriveOn;
	}

	@Override
	public void setDriveOn(boolean on) {
		this.isDriveOn = on;
	}

	private boolean isDriveSoundOn;

	@Override
	public boolean isDriveSoundOn() {
		return this.isDriveSoundOn;
	}

	@Override
	public void setDriveSoundOn(boolean on) {
		this.isDriveSoundOn = on;
	}

	private boolean isParallelCable;

	@Override
	public boolean isParallelCable() {
		return this.isParallelCable;
	}

	@Override
	public void setParallelCable(boolean on) {
		this.isParallelCable = on;
	}

	private boolean isRamExpansionEnabled0;

	@Override
	public boolean isRamExpansionEnabled0() {
		return this.isRamExpansionEnabled0;
	}

	@Override
	public void setRamExpansion0(boolean on) {
		this.isRamExpansionEnabled0 = on;
	}

	private boolean isRamExpansionEnabled1;

	@Override
	public boolean isRamExpansionEnabled1() {
		return this.isRamExpansionEnabled1;
	}

	@Override
	public void setRamExpansion1(boolean on) {
		this.isRamExpansionEnabled1 = on;
	}

	private boolean isRamExpansionEnabled2;

	@Override
	public boolean isRamExpansionEnabled2() {
		return this.isRamExpansionEnabled2;
	}

	@Override
	public void setRamExpansion2(boolean on) {
		this.isRamExpansionEnabled2 = on;
	}

	private boolean isRamExpansionEnabled3;

	@Override
	public boolean isRamExpansionEnabled3() {
		return this.isRamExpansionEnabled3;
	}

	@Override
	public void setRamExpansion3(boolean on) {
		this.isRamExpansionEnabled3 = on;
	}

	private boolean isRamExpansionEnabled4;

	@Override
	public boolean isRamExpansionEnabled4() {
		return this.isRamExpansionEnabled4;
	}

	@Override
	public void setRamExpansion4(boolean on) {
		this.isRamExpansionEnabled4 = on;
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
