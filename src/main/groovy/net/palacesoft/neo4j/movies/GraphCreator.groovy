package net.palacesoft.neo4j.movies

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.data.neo4j.support.Neo4jTemplate
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase

class GraphCreator extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {

        String host = "http://neoflix-groovy.herokuapp.com"


        String neoUrl = "http://8ebbd36fa:c2571ce69@b4f822dd9.hosted.neo4j.org:7071/db/data/"

        Neo4jTemplate neo4jTemplate = new Neo4jTemplate(new SpringRestGraphDatabase(neoUrl))


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
                                '${host}/movies.dat'.toURL().eachLine { def line ->
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

                                             '${host}/users.dat'.toURL().eachLine { def line ->
                                               def components = line.split('::');
                                               def userVertex = g.addVertex(['type':'User', 'userId':components[0].toInteger(), 'gender':components[1], 'age':components[2].toInteger()]);
                                               def occupation = occupations[components[3].toInteger()];
                                               def hits = g.idx(Tokens.T.v)[[occupation:occupation]].iterator();
                                               def occupationVertex = hits.hasNext() ? hits.next() : g.addVertex(['type':'Occupation', 'occupation':occupation]);
                                               g.addEdge(userVertex, occupationVertex, 'hasOccupation');
                                             }

                                             '${host}/ratings.dat'.toURL().eachLine {def line ->
                                               def components = line.split('::');
                                               def ratedEdge = g.addEdge(g.idx(Tokens.T.v)[[userId:components[0].toInteger()]].next(), g.idx(T.v)[[movieId:components[1].toInteger()]].next(), 'rated');
                                               ratedEdge.setProperty('stars', components[2].toInteger());
                                               }

                                              g.stopTransaction(TransactionalGraph.Conclusion.SUCCESS);"""

        neo4jTemplate.execute(script, null)


    }
}
