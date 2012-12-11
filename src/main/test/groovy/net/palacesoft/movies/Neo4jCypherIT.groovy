package net.palacesoft.movies;


import org.junit.Test
import org.junit.runner.RunWith
import org.neo4j.rest.graphdb.entity.RestNode
import org.springframework.data.neo4j.core.GraphDatabase
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase
import org.springframework.data.neo4j.support.Neo4jTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.data.neo4j.template.Neo4jOperations
import org.neo4j.rest.graphdb.util.QueryResult
import org.springframework.data.neo4j.rest.SpringRestResult
import org.springframework.data.neo4j.conversion.Result
import org.neo4j.helpers.collection.MapUtil

public class Neo4jCypherIT {

    GraphDatabase graphDb = new SpringRestGraphDatabase(System.getProperty("NEO4J_URL") + "/db/data")

    @Test
    public void checkDB() {

        Neo4jOperations neo = new Neo4jTemplate(graphDb)

        def result = neo.query("START a = node(100) RETURN a;", null).to(RestNode.class).single()


        println result.getProperty("title")


    }

    @Test
    public void getRecommendations() {
        Neo4jTemplate neo = new Neo4jTemplate(graphDb)


        def cypherScript = """
          START movie=node({node_id}) MATCH movie-[:hasGenera]-genera1, movie<--()-[ratedRel:rated]->anotherMovie-[:hasGenera]-genera2 WHERE ratedRel.stars > 3
          AND genera1.genera = genera2.genera
          RETURN DISTINCT anotherMovie.title as title, anotherMovie.movieId as movieId,COUNT(anotherMovie) as count
          ORDER BY count(anotherMovie)
          DESC LIMIT 25;
        """


        RestNode node = neo.query("START n=node:vertices(title='Matrix, The (1999)') RETURN n;", null).to(RestNode.class).single()

        int movieId =  node.getProperty("movieId") as Integer;

        def result = neo.query(cypherScript, MapUtil.map("node_id", movieId))


        if (!result){
            println "[{id: ${movieId},name: No Recommendations, values:[{id:${movieId}, name:No Recommendations}]}]"
        }

        def jsonResult = result.collect() {
            "{id:${it.movieId},name:${it.title}}"
        }

        println "[{id:${movieId} ,name:Recommendations,values:${jsonResult.join(",")} }]"


        //should be
        // [{id:1 ,name:Recommendations,values:{id:3076,name:Toy Story 2 (1999)},{id:2317,name:Bug's Life  A (1998)},{id:3713,name:Chicken Run (2000)},{id:2103,name:American Tail  An (1986)},{id:1081,name:Aladdin and the King of Thieves (1996)},{id:2316,name:Rugrats Movie  The (1998)},{id:2104,name:American Tail},{id:3716,name:Adventures of Rocky and Bullwinkle  The (2000)},{id:3573,name:Saludos Amigos (1943)} }]


    }

}
