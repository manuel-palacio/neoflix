package net.palacesoft.neo4j.movies

import com.sun.jersey.core.spi.factory.ResponseBuilderImpl
import groovy.json.JsonBuilder
import net.palacesoft.tmdb.TmdbMovie
import net.palacesoft.tmdb.model.Movie
import org.neo4j.rest.graphdb.entity.RestNode
import org.springframework.data.neo4j.core.GraphDatabase
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase
import org.springframework.data.neo4j.support.Neo4jTemplate

import javax.annotation.PostConstruct
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

@Path("/show")
class MovieResource {

    def movieKey

    def neo4jTemplate


    @PostConstruct
    void init() {
        def neoUrl = System.getenv("NEO4J_URL");
        if (neoUrl == null) {
            neoUrl = System.getProperty("NEO4J_URL")
        }

        neoUrl = neoUrl + "/db/data"


        movieKey = System.getenv("TMDB_KEY");
        // If env var not set, try reading from Java "system properties"
        if (movieKey == null) {
            movieKey = System.getProperty("TMDB_KEY");
        }

        GraphDatabase graphDb = new SpringRestGraphDatabase(neoUrl)

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
            node = neo4jTemplate.query("START n=node:vertices(title='${URLDecoder.decode(id)}') RETURN n;", null).to(RestNode.class).single()
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
            int movieId = tmdbMovie.search(node.getProperty("title").toString(), 1)[0].id

            def movieUrl = "http://www.themoviedb.org/movie/${movieId}"
            def poster = tmdbMovie.getPosterUrlForSize(movieId, "w185")
            Movie movie = tmdbMovie.getMovie(movieId) //TODO should not need this
            def rating = movie.vote_average
            def tagLine = movie.tagline
            def overview = movie.overview
            def certification = movie.status

            return "<a href='${movieUrl}' target='_blank'><img src='${poster}'><h3>${tagLine}</h3><p>Rating: ${rating} <br/>Rated: ${certification}</p><p>${overview}</p>"
        } catch (e) {
            //ignore
        }

        return ""
    }
}
