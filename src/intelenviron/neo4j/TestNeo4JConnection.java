/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.rest.graphdb.RestGraphDatabase;

/**
 * http://m2.neo4j.org/content/repositories/snapshots/org/neo4j/
 * https://github.com/neo4j/java-rest-binding/tree/master/src/test/java/org/neo4j/rest/graphdb
 * @author me
 */
public class TestNeo4JConnection {
    private static enum RelTypes implements RelationshipType
    {
        NODE,
    }

    public static void main(String[] args) {
        RestGraphDatabase n = new RestGraphDatabase("http://localhost:7474/db/data");        
        
        Node x = n.createNode();
        x.setProperty("x", "x");
        n.getReferenceNode().createRelationshipTo(x, RelTypes.NODE );
        

//        tx.success();
        
        
    }
//    public static void main(String[] args) {
//        
//        final GraphDatabaseService n = new GraphDatabaseFactory().newEmbeddedDatabase("/tmp/neo");
//        Runtime.getRuntime().addShutdownHook(new Thread() {
//
//            @Override
//            public void run() {
//                n.shutdown();
//            }
//        });
//        
//        
//        Transaction tx = n.beginTx();
//        try
//        {
//            Node x = n.createNode();
//            x.setProperty("x", "x");
//            n.getReferenceNode().createRelationshipTo(x, RelTypes.NODE );
//            
//            tx.success();
//        }
//        finally {
//            tx.finish();
//        }
//        
//        Iterator<Relationship> r = n.getReferenceNode().getRelationships(RelTypes.NODE).iterator();
//        while (r.hasNext()) {
//            Relationship rr = r.next();
//            System.out.println(rr.getStartNode() + " " + rr.getEndNode());
//        }
//        
//        
//        n.shutdown();
//        
//    }
}
