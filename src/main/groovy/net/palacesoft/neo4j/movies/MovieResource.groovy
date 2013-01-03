package net.palacesoft.neo4j.movies

import com.sun.jersey.core.spi.factory.ResponseBuilderImpl
import groovy.json.JsonBuilder
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

    def neo4jTemplate


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

        println "#About to initialize using " + neoUrl

        GraphDatabase graphDb = new SpringRestGraphDatabase(neoUrl, neoUsername, neoPwd)

        neo4jTemplate = new Neo4jTemplate(graphDb)

    }


    private def getRecommendations(long movieId) {

        def cypherScript = """
                 START movie=node:vertices(movieId={movieId})
                 MATCH movie-->genera<--anotherMovie<-[ratedRel:rated]-person
                 WHERE ratedRel.stars > 3
                 RETURN anotherMovie.title as title, anotherMovie.movieId as id,
                 COUNT(anotherMovie) as count ORDER BY count(anotherMovie) DESC LIMIT 15;
                 """


        def result = neo4jTemplate.query(cypherScript, ["movieId": movieId])

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
            node = neo4jTemplate.query("START movie=node(${id}) RETURN movie;", null).to(RestNode.class).single()
        } else {
            node = neo4jTemplate.query("START n=node:vertices(title='${URLDecoder.decode(id, "utf-8")}') RETURN n;", null).to(RestNode.class).single()
        }


        def json = [:]
        if (node && node.hasProperty("title")) {
            json = [details_html: "<h2>${node.getProperty("title")}</h2>${getPoster(node)}",
                    data: [attributes: [getRecommendations(Long.parseLong(node.getProperty("movieId").toString()))], name: node.getProperty("title"), id: id]]
        }


        new ResponseBuilderImpl().entity(new JsonBuilder(json).toString()).status(200).build()


    }

    private def getPoster(RestNode node) {
        try {
            TmdbMovie tmdbMovie = new TmdbMovie(movieKey)
            Movie movie = tmdbMovie.search(node.getProperty("title").toString(), 1)[0]

            def movieUrl = "http://www.themoviedb.org/movie/${movie.id}"
            def poster = tmdbMovie.getPosterUrlForSize(movie.id, "w185")
            def rating = movie.vote_average
            def tagLine = movie.tagline
            def overview = movie.overview
            def certification = movie.status

            return "<a href='${movieUrl}' target='_blank'><img src='${poster}'><h3>${tagLine}</h3><p>Rating: ${rating} <br/>Rated: ${certification}</p><p>${overview}</p>"
        } catch (e) {
            //ignore
        }

    }
}
