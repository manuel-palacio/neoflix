package net.palacesoft.movies;


import org.junit.Test
import org.junit.runner.RunWith
import org.neo4j.rest.graphdb.entity.RestNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.neo4j.core.GraphDatabase
import org.springframework.data.neo4j.support.Neo4jTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:applicationContext.xml")
public class Neo4jTest {

    @Autowired
    GraphDatabase graphDatabase

    @Test
    public void checkDB() {

        Neo4jTemplate neo = new Neo4jTemplate(graphDatabase)

        def result = neo.execute("g.idx('vertices')[[type:'Movie']].count()", null).to(Integer.class).single()


        println result


    }

    @Test
    public void getRecommendations() {
        Neo4jTemplate neo = new Neo4jTemplate(graphDatabase)
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


        RestNode node = neo.execute("g.idx(Tokens.T.v)[[title:'Toy Story (1995)']].next();", null).to(RestNode.class).single()

        int id =  node.getProperty("movieId") as Integer;

        def result = neo.execute(script, ["node_id": node.getId()]).to(Map.class).single()

        if (!result || result.empty) {
            println "[{id: ${id},name: No Recommendations, values:[{id:${id}, name:No Recommendations}]}]"
        }

        def jsonResult = result.collect() {
            "{id:${it.key.toString().split(":")[0]},name:${it.key.toString().split(":")[1]}}"
        }

        println "[{id:${id} ,name:Recommendations,values:${jsonResult.join(",")} }]"

    }

}
