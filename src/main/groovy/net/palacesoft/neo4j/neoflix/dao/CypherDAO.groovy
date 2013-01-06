package net.palacesoft.neo4j.neoflix.dao

import org.neo4j.rest.graphdb.entity.RestNode
import org.springframework.data.neo4j.conversion.Result
import org.springframework.data.neo4j.support.Neo4jTemplate

class CypherDAO {

    private Neo4jTemplate neo4jTemplate

    CypherDAO(Neo4jTemplate neo4jTemplate) {
        this.neo4jTemplate = neo4jTemplate
    }

    RestNode findMovieById(id) {
        neo4jTemplate.query("START movie=node(${id}) RETURN movie;", null).to(RestNode.class).single()
    }

    RestNode findMovieByTitle(title) {
        neo4jTemplate.query("START n=node:vertices(title='${URLDecoder.decode(title, "utf-8")}') RETURN n;",
                            null).to(RestNode.class).single()
    }

    Result findRecommendationsById(movieId) {
        def cypherScript = """
                        START movie=node({movieId})
                        MATCH movie-->genera<--anotherMovie<-[ratedRel:rated]-person
                        WHERE ratedRel.stars > 3
                        RETURN anotherMovie.title as title, anotherMovie.movieId as id,
                        COUNT(anotherMovie) as count ORDER BY count(anotherMovie) DESC LIMIT 15;
                        """


        neo4jTemplate.query(cypherScript, ["movieId": movieId])

    }
}
