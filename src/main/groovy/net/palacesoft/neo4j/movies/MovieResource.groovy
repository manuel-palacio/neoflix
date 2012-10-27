package net.palacesoft.neo4j.movies

import com.sun.jersey.core.spi.factory.ResponseBuilderImpl
import com.sun.jersey.spi.resource.Singleton
import groovy.json.JsonSlurper
import org.neo4j.rest.graphdb.entity.RestNode
import org.springframework.data.neo4j.core.GraphDatabase
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase
import org.springframework.data.neo4j.support.Neo4jTemplate

import javax.annotation.PostConstruct
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response
import groovy.json.JsonBuilder

@Path("/show")
@Singleton
class MovieResource {

    def neoUrl = System.getProperty("NEO4J_URL")

    def movieKey = System.getProperty("TMDB_KEY")


    GraphDatabase graphDb = new SpringRestGraphDatabase(neoUrl != null ? neoUrl : "http://localhost:7474/db/data")

    def neo4jTemplate = new Neo4jTemplate(graphDb)


    def localhost = "http://localhost:8080"

    @PostConstruct
    void createGraph() {

        if (neo4jTemplate.execute("g.idx('vertices')[[type:'Movie']].count()", null).to(Integer.class).single() > 0) {
            return
        }


        if (neo4jTemplate.execute("g.indices", null).toList().empty) {
            neo4jTemplate.execute("g.createAutomaticIndex('vertices', Vertex.class, null)", null)
            if (neo4jTemplate.execute("g.V.count()", null).to(Integer.class).single() > 0) {
                neo4jTemplate.execute("AutomaticIndexHelper.reIndexElements(g, g.idx('vertices'), g.V)", null)
            }
        }

        def script = """g.setMaxBufferSize(1000);
                        '${localhost}/movies.dat'.toURL().eachLine { def line ->
                             def components = line.split('::');
                             def movieVertex = g.addVertex(['type':'Movie', 'movieId':components[0].toInteger(), 'title':components[1]]);
                             components[2].split('|').each { def genera ->
                                def hits = g.idx(Tokens.T.v)[[genera:genera]].iterator();
                                def generaVertex = hits.hasNext() ? hits.next() : g.addVertex(['type':'Genera', 'genera':genera]);
                                g.addEdge(movieVertex, generaVertex, 'hasGenera');
                                }
                             }
                             occupations = [0:'other', 1:'academic/educator', 2:'artist', 3:'clerical/admin', 4:'college/grad student', 5:'customer service',
                                      6:'doctor/health care', 7:'executive/managerial', 8:'farmer',
                                      9:'homemaker', 10:'K-12 student', 11:'lawyer', 12:'programmer',
                                       13:'retired', 14:'sales/marketing', 15:'scientist', 16:'self-employed',
                                       17:'technician/engineer', 18:'tradesman/craftsman', 19:'unemployed', 20:'writer'];

                                     '${localhost}/users.dat'.toURL().eachLine { def line ->
                                       def components = line.split('::');
                                       def userVertex = g.addVertex(['type':'User', 'userId':components[0].toInteger(), 'gender':components[1], 'age':components[2].toInteger()]);
                                       def occupation = occupations[components[3].toInteger()];
                                       def hits = g.idx(Tokens.T.v)[[occupation:occupation]].iterator();
                                       def occupationVertex = hits.hasNext() ? hits.next() : g.addVertex(['type':'Occupation', 'occupation':occupation]);
                                       g.addEdge(userVertex, occupationVertex, 'hasOccupation');
                                     }

                                     '${localhost}/ratings.dat'.toURL().eachLine {def line ->
                                       def components = line.split('::');
                                       def ratedEdge = g.addEdge(g.idx(Tokens.T.v)[[userId:components[0].toInteger()]].next(), g.idx(T.v)[[movieId:components[1].toInteger()]].next(), 'rated');
                                       ratedEdge.setProperty('stars', components[2].toInteger());
                                       }

                                      g.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);"""

        neo4jTemplate.execute(script, null)

    }

    private def getRecommendations(long id) {
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
        RestNode node
        if (id.isNumber()) {
            node = neo4jTemplate.execute("g.v(${id});", null).to(RestNode.class).single()
        } else {
            node = neo4jTemplate.execute("g.idx(Tokens.T.v)[[title:'${URLDecoder.decode(id)}']].next();", null).to(RestNode.class).single()

        }


        def json = [:]
        if (node) {
            json = [details_html: "<h2>${getName(node)}</h2>${getPoster(node)}",
                    data: [attributes: [getRecommendations(node.getId())], name: getName(node), id: id]]
        }


        new ResponseBuilderImpl().entity(new JsonBuilder(json).toString()).status(200).build()


    }

    private def getPoster(RestNode node) {
        if (node.getProperty("type") == "Movie") {
            def movieResponse = new JsonSlurper()

            def result = movieResponse.parseText(new URL("http://api.themoviedb.org/3/search/movie?api_key=${movieKey}&query=${URLEncoder.encode(node.getProperty("title").toString())}").text)

            def movieUrl = "http://www.themoviedb.org/movie/${result.results.id[0]}"
            def poster = "http://cf2.imgobject.com/t/p/w185${result.results.poster_path[0]}"
            def tagLine = ""
            def rating = result.results.vote_average[0]
            def certification = ""
            def overview = ""



            return "<a href='${movieUrl}' target='_blank'><img src='${poster}'><h3>${tagLine}</h3><p>Rating: ${rating} <br/>Rated: ${certification}</p><p>${overview}</p>"
        }

        ""
    }

    private def getName(RestNode node) {
        def type = node.getProperty("type")

        switch (type) {
            case "Movie": return node.getProperty("title")
            case "Occupation": return node.getProperty("occupation")
            case "User": return """"{"${node.getProperty("userId")}"} "Gender": "${node.getProperty("gender")}" "Age": "${node.getProperty("age")}"}"""
            case "Genera": return node.getProperty("genera")
        }


    }
}
