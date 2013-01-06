package net.palacesoft.neoflix

import net.palacesoft.neo4j.neoflix.dao.GremlinDAO

import org.junit.Before;
import org.junit.Test
import org.springframework.data.neo4j.core.GraphDatabase
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase
import org.springframework.data.neo4j.support.Neo4jTemplate
import org.springframework.data.neo4j.template.Neo4jOperations

public class Neo4jGremlinIT {

    GraphDatabase graphDb = new SpringRestGraphDatabase(System.getProperty("NEO4J_URL") + "/db/data")
    GremlinDAO dao

    @Before
    public void setup() {
        Neo4jOperations neo4jTemplate = new Neo4jTemplate(graphDb)
        dao = new GremlinDAO(neo4jTemplate)
    }

    @Test
    public void can_find_movie_by_title() {

        def result = dao.findMovieByTitle("Toy Story (1995)")

        assert result.getProperty("title") == "Toy Story (1995)"
    }


    @Test
    public void can_find_movie_by_id() {

        def result = dao.findMovieById(100)

        assert result
    }

    @Test
    public void can_get_recommendations() {

        def result = dao.findRecommendationsById(100)

        if (!result || !result.iterator().hasNext()) {
            println "[{id: ${100},name: No Recommendations, values:[{id:${100}, name:No Recommendations}]}]"
        }

        def jsonResult = result.collect {
            "{id:${it.key.toString().split(":")[0]},name:${it.key.toString().split(":")[1]}}"
        }

        println "[{id:${100} ,name:Recommendations,values:${jsonResult.join(",")} }]"

    }

}
