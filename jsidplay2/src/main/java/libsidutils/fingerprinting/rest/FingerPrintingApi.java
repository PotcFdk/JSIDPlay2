package libsidutils.fingerprinting.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;

public interface FingerPrintingApi {

	@PUT
	@Path("/insert-tune")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	IdBean insertTune(MusicInfoBean musicInfoBean);

	@PUT
	@Path("/insert-hashes")
	@Consumes({ MediaType.APPLICATION_JSON })
	void insertHashes(HashBeans hashBeans);

	@POST
	@Path("/hash")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	HashBeans findAllHashes(IntArrayBean intArray);

	@POST
	@Path("/tune")
	@Consumes({ MediaType.APPLICATION_JSON })
	@Produces({ MediaType.APPLICATION_JSON })
	MusicInfoBean findTune(SongNoBean songNoBean);

}
