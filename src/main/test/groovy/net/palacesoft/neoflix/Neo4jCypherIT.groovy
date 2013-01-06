package net.palacesoft.neoflix

import net.palacesoft.neo4j.neoflix.dao.CypherDAO

import org.junit.Before;
import org.junit.Test
import org.springframework.data.neo4j.core.GraphDatabase
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase
import org.springframework.data.neo4j.support.Neo4jTemplate
import org.springframework.data.neo4j.template.Neo4jOperations

public class Neo4jCypherIT {

    GraphDatabase graphDb = new SpringRestGraphDatabase(System.getProperty("NEO4J_URL") + "/db/data")
    CypherDAO cypherDAO
    int movieId = 200


    @Before
    public void setup() {
        Neo4jOperations neo4jTemplate = new Neo4jTemplate(graphDb)
        cypherDAO = new CypherDAO(neo4jTemplate)
    }

    @Test
    public void can_find_movie_by_id() {

        def result = cypherDAO.findMovieById(movieId)


        assert result.getProperty("title")

    }

    @Test
    public void can_find_movie_by_title() {

           def result = cypherDAO.findMovieByTitle("Toy Story (1995)")

           assert result.getProperty("title") == "Toy Story (1995)"
       }

    @Test
    public void can_get_recommendations() {

        def result = cypherDAO.findRecommendationsById(movieId)


        if (!result) {
            println "[{id: ${movieId},name: No Recommendations, values:[{id:${movieId}, name:No Recommendations}]}]"
        }

        def jsonResult = result.collect {
            "{id:${it.id},name:${it.title}}"
        }.join(",")

        println "[{id:${movieId} ,name:Recommendations,values:${jsonResult} }]"

    }

}
