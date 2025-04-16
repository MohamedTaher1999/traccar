package org.traccar.api.resource;
import jakarta.annotation.security.PermitAll;
import org.traccar.api.BaseResource;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("hello")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HelloResource extends BaseResource {


    @PermitAll
    @GET
    public Response get() {
        return Response.ok("Mohamed Salem").build();
    }
}