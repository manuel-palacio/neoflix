package net.palacesoft.neo4j.movies

import com.sun.jersey.core.spi.factory.ResponseBuilderImpl
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.neo4j.rest.graphdb.entity.RestNode
import org.springframework.data.neo4j.core.GraphDatabase
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase
import org.springframework.data.neo4j.support.Neo4jTemplate

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

@Path("/show")
class MovieResource {

    def neoUrl = System.getProperty("NEO4J_URL") + "/db/data"

    def movieKey = System.getProperty("TMDB_KEY")


    GraphDatabase graphDb = new SpringRestGraphDatabase(neoUrl)


    private def getRecommendations(long id) {
        def neo4jTemplate = new Neo4jTemplate(graphDb)

        def script = """
                m = [:];
                x = [] as Set;
                v = g.v(node_id);
                v.
                out('hasGenera').
                aggregate(x).
                back(2).
                inE('rated').
                filter{it.getProperty('stars') > 3}.
                outV.
                outE('rated').
                filter{it.getProperty('stars') > 3}.
                inV.
                filter{it != v}.
                filter{it.out('hasGenera').toSet().equals(x)}.
                groupCount(m){"\${it.id}:\${it.title.replaceAll(',',' ')}\"}.iterate();

                m.sort{a,b -> b.value <=> a.value}[0..24];"""


        Map result = neo4jTemplate.execute(script, ["node_id": id]).to(Map.class).single()

        if (!result || result.empty) {

            return [id: id, name: "No Recommendations", values: [[id: id, name: "No Recommendations"]]]
        }

        def jsonResult = result.collect {
            [id: it.key.toString().split(":")[0], name: it.key.toString().split(":")[1]]
        }

        return [id: id, name: "Recommendations", values: jsonResult]
    }

    @GET
    public Response getMovie(@QueryParam("id") String id) {
        def neo4jTemplate = new Neo4jTemplate(graphDb)

        RestNode node
        if (id.isNumber()) {
            node = neo4jTemplate.execute("g.v(${id});", null).to(RestNode.class).single()
        } else {
            node = neo4jTemplate.execute("g.idx(Tokens.T.v)[[title:'${URLDecoder.decode(id)}']].next();", null).to(RestNode.class).single()
        }


        def json = [:]
        if (node && node.hasProperty("title")) {
            json = [details_html: "<h2>${node.getProperty("title")}</h2>${getPoster(node)}",
                    data: [attributes: [getRecommendations(node.getId())], name: node.getProperty("title"), id: id]]
        }


        new ResponseBuilderImpl().entity(new JsonBuilder(json).toString()).status(200).build()


    }

    private def getPoster(RestNode node) {
        def movieResponse = new JsonSlurper()

        def result = movieResponse.parseText(new URL("http://api.themoviedb.org/3/search/movie?api_key=${movieKey}&query=${URLEncoder.encode(node.getProperty("title").toString())}").text)

        def movieUrl = "http://www.themoviedb.org/movie/${result.results.id[0]}"
        def poster = "http://cf2.imgobject.com/t/p/w185${result.results.poster_path[0]}"
        def rating = result.results.vote_average[0]

        def details = movieResponse.parseText(new URL("http://api.themoviedb.org/3/movie/${result.results.id[0]}?api_key=${movieKey}").text)
        def tagLine = details.tagline
        def certification = ""
        def overview = details.overview



        "<a href='${movieUrl}' target='_blank'><img src='${poster}'><h3>${tagLine}</h3><p>Rating: ${rating} <br/>Rated: ${certification}</p><p>${overview}</p>"
    }
}
