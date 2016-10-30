package netsiddev_builder;

enum NetSIDCommand {
	CMD_FLUSH(0),
	CMD_TRY_SET_SID_COUNT(1),
	CMD_MUTE(2),
	CMD_TRY_RESET(3),
	
	CMD_TRY_DELAY(4),
	CMD_TRY_WRITE(5),
	CMD_TRY_READ(6),
	GET_VERSION(7),
	
	TRY_SET_SAMPLING(8),
	TRY_SET_CLOCKING(9),
	GET_CONFIG_COUNT(10),
	GET_CONFIG_INFO(11),
	
	SET_SID_POSITION(12),
	SET_SID_LEVEL(13),
	TRY_SET_SID_MODEL(14);

	byte cmd;

	private NetSIDCommand(int cmd) {
		this.cmd = (byte) cmd;
	}
}