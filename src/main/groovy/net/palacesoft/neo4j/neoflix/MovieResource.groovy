package net.palacesoft.neo4j.neoflix

import com.sun.jersey.core.spi.factory.ResponseBuilderImpl
import groovy.json.JsonBuilder
import net.palacesoft.neo4j.neoflix.dao.CypherDAO
import net.palacesoft.tmdb.TmdbMovie
import net.palacesoft.tmdb.model.Movie
import org.neo4j.rest.graphdb.entity.RestNode
import org.springframework.data.neo4j.core.GraphDatabase
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase
import org.springframework.data.neo4j.support.Neo4jTemplate

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response
import com.sun.jersey.spi.resource.Singleton
import javax.annotation.PostConstruct
import javax.ws.rs.Produces

@Path("/show")
@Singleton
class MovieResource {

    def movieKey

    CypherDAO dao


    @PostConstruct
    void init() {
        def neoUrl = System.getenv("NEO4J_URL")
        if (!neoUrl) {
            neoUrl = System.getProperty("NEO4J_URL")
        }
        neoUrl = neoUrl + "/db/data"

        def neoUsername = System.getenv("NEO4J_USERNAME")

        if (!neoUsername) {
            neoUsername = System.getProperty("NEO4J_USERNAME")
        }

        def neoPwd = System.getenv("NEO4J_PASSWORD")

        if (!neoPwd) {
            neoPwd = System.getProperty("NEO4J_PASSWORD")
        }

        movieKey = System.getenv("TMDB_KEY")

        if (!movieKey) {
            movieKey = System.getProperty("TMDB_KEY")
        }

        GraphDatabase graphDb = new SpringRestGraphDatabase(neoUrl, neoUsername, neoPwd)

        def neo4jTemplate = new Neo4jTemplate(graphDb)

        dao = new CypherDAO(neo4jTemplate)

    }


    private def getRecommendations(long movieId) {


        def result = dao.findRecommendationsById(movieId)

        if (!result) {

            return [id: movieId, name: "No Recommendations", values: [[id: movieId, name: "No Recommendations"]]]
        }

        def jsonResult = result.collect {
            [id: it.id, name: it.title]
        }

        return [id: movieId, name: "Recommendations", values: jsonResult]
    }

    @GET
    @Produces("application/json")
    public Response getMovie(@QueryParam("id") String id) {

        RestNode node
        if (id.isNumber()) {
            node = dao.findMovieById(id)
        } else {
            node = dao.findMovieByTitle(URLDecoder.decode(id, "utf-8"))
        }


        def json = [:]
        if (node && node.hasProperty("title")) {
            json = [details_html: "<h2>${node.getProperty("title")}</h2>${getPoster(node)}",
                    data: [attributes: [getRecommendations(node.getId())],
                            name: node.getProperty("title"), id: id]]
        }


        new ResponseBuilderImpl().entity(new JsonBuilder(json).toString()).status(200).build()


    }

    private def getPoster(RestNode node) {
        try {
            TmdbMovie tmdbMovie = new TmdbMovie(movieKey)
            Movie movie = tmdbMovie.search(node.getProperty("title").toString(), 1)[0]

            def movieUrl = movie.getUrl()
            def poster = tmdbMovie.getPosterUrlForMovieWithSize(movie.id, "w185")
            def rating = movie.vote_average
            def tagLine = movie.tagline
            def overview = movie.overview
            def certification = movie.status

            return "<a href='${movieUrl}' target='_blank'><img src='${poster}'><h3>${tagLine}</h3><p>Rating: ${rating} <br/>Rated: ${certification}</p><p>${overview}</p>"
        } catch (e) {
           print e.getMessage()
        }

    }
}
