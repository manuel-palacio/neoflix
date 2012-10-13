package net.palacesoft.neo4j.movies

import com.sun.jersey.core.spi.factory.ResponseBuilderImpl
import com.sun.jersey.spi.resource.Singleton

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.Response

@Path("/movie")
@Singleton
class MovieResource {

    @GET
    @Path("{id}")
    public Response getMovie(@PathParam("id") String id) {

        new ResponseBuilderImpl().entity("hello").status(200).build()

    }
}
