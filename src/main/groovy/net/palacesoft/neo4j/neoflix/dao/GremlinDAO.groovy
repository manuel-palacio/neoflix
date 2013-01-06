package net.palacesoft.neo4j.neoflix.dao

import org.neo4j.rest.graphdb.entity.RestNode
import org.springframework.data.neo4j.conversion.Result
import org.springframework.data.neo4j.support.Neo4jTemplate

class GremlinDAO {

    private Neo4jTemplate neo4jTemplate

    GremlinDAO(Neo4jTemplate neo4jTemplate) {
        this.neo4jTemplate = neo4jTemplate
    }

    RestNode findMovieById(id) {
        neo4jTemplate.execute("g.v(${id});", null).to(RestNode.class).single()
    }

    RestNode findMovieByTitle(title) {
        neo4jTemplate.execute("g.idx(Tokens.T.v)[[title:'${title}']].next();", null).to(RestNode.class).single()
    }

    Map findRecommendationsById(movieId) {
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


        neo4jTemplate.execute(script, ["node_id": movieId]).to(Map.class).single()
    }
}
