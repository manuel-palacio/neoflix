package net.palacesoft.neo4j.movies

import com.sun.jersey.core.spi.factory.ResponseBuilderImpl
import com.sun.jersey.spi.resource.Singleton

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.core.Response
import javax.annotation.PostConstruct
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase
import org.springframework.data.neo4j.support.Neo4jTemplate
import org.springframework.data.neo4j.core.GraphDatabase

@Path("/movie")
@Singleton
class MovieResource {

    def neoUrl = System.getProperty("NEO4J_URL")

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

        def script = """g.setMaxBufferSize(1000)
                        '${localhost}/movies.dat'.toURL().eachLine { def line ->
                             def components = line.split('::')
                             def movieVertex = g.addVertex(['type':'Movie', 'movieId':components[0].toInteger(), 'title':components[1]])
                               components[2].split('|').each { def genera ->
                                def hits = g.idx(Tokens.T.v)[[genera:genera]].iterator() def generaVertex = hits.hasNext() ? hits.next() : g.addVertex(['type':'Genera', 'genera':genera])
                                g.addEdge(movieVertex, generaVertex, 'hasGenera')
                                }
                             }
                             occupations = [0:'other', 1:'academic/educator', 2:'artist', 3:'clerical/admin', 4:'college/grad student', 5:'customer service',
                                      6:'doctor/health care', 7:'executive/managerial', 8:'farmer',
                                      9:'homemaker', 10:'K-12 student', 11:'lawyer', 12:'programmer',
                                       13:'retired', 14:'sales/marketing', 15:'scientist', 16:'self-employed',
                                       17:'technician/engineer', 18:'tradesman/craftsman', 19:'unemployed', 20:'writer']

                                     '${localhost}/users.dat'.toURL().eachLine { def line ->
                                       def components = line.split('::')
                                       def userVertex = g.addVertex(['type':'User', 'userId':components[0].toInteger(), 'gender':components[1], 'age':components[2].toInteger()])
                                       def occupation = occupations[components[3].toInteger()]
                                       def hits = g.idx(Tokens.T.v)[[occupation:occupation]].iterator()
                                       def occupationVertex = hits.hasNext() ? hits.next() : g.addVertex(['type':'Occupation', 'occupation':occupation])
                                       g.addEdge(userVertex, occupationVertex, 'hasOccupation')
                                     }

                                     '${localhost}/ratings.dat'.toURL().eachLine {def line ->
                                       def components = line.split('::')
                                       def ratedEdge = g.addEdge(g.idx(Tokens.T.v)[[userId:components[0].toInteger()]].next(), g.idx(T.v)[[movieId:components[1].toInteger()]].next(), 'rated')
                                       ratedEdge.setProperty('stars', components[2].toInteger())
                                       }

                                      g.stopTransaction(TransactionalGraph.Conclusion.SUCCESS)"""

        neo4jTemplate.execute(script, null)

    }


    @GET
    @Path("{id}")
    public Response getMovie(@PathParam("id") String id) {

        new ResponseBuilderImpl().entity("hello").status(200).build()

    }
}
