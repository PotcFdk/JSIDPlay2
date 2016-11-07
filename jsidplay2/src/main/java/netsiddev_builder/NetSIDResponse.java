package netsiddev_builder;

enum NetSIDResponse {
	OK(0), BUSY(1), ERROR(2), READ(3), VERSION(4), COUNT(5), INFO(6),;

	int resp;

	private NetSIDResponse(int response) {
		this.resp = response;
	}
}