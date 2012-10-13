package net.palacesoft.movies;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.neo4j.rest.graphdb.query.RestGremlinQueryEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:applicationContext.xml")
public class Neo4jTest {

    @Autowired
    GraphDatabase graphDatabase;

    @Test
    public void gremlinTest() {

        Neo4jTemplate neo = new Neo4jTemplate(graphDatabase);

        RestGremlinQueryEngine restGremlinQueryEngine = new RestGremlinQueryEngine(((RestGraphDatabase)graphDatabase).getRestAPI());

        neo.execute("g.addVertex([name:'pierre',location:'belgium'])", null).to(Node.class).single();

        Node pierre = restGremlinQueryEngine.query("g.V.find{it.name='pierre'}", null).to(Node.class).single();

        assertThat(pierre.getProperty("name").toString(), is("pierre"));

        neo.execute("g.removeVertex(g.V.find{it.name='pierre'})", null);


        Node res = restGremlinQueryEngine.query("g.V.find{it.name='pierre'}", null).to(Node.class).singleOrNull();

        System.out.println(res);

        /*Node nod = neo.execute("g.v(id)",
                map("id", 1)).to(Node.class).single();


        System.out.println(nod.getProperty("name"));


        Iterator result = neo.execute("g.V.name", null).to(String.class).iterator();


        while (result.hasNext()) {
            System.out.println(result.next().toString());
        }*/

    }
}
