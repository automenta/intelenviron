/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

import intelenviron.neo4j.KBLoader.Transactable;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.rest.graphdb.RestGraphDatabase;

/**
 *
 * @author me
 */
public class KB {
    public static final String ID = "id";
    
    public final GraphDatabaseService graph;
    private Index<Node> nodeIndex;
    
    public RelationshipType getType(final Class c) {
        return new RelationshipType() {
            @Override
            public String name() {
                return c.getSimpleName();
            }
        };
    }
    public RelationshipType getType(final String r) {
        return new RelationshipType() {
            @Override
            public String name() {
                return r;
            }
        };
    }

//    private Traverser getInstances(final Class c){
//        TraversalDescription td = Traversal.description()
//                .breadthFirst()
//                .relationships( getType(c), Direction.OUTGOING )
//                .evaluator( Evaluators.excludeStartPosition() );
//        return td.traverse( graph.getReferenceNode() );
//    }
    
    public static Relationship relateOnce(final Node from, final Node to, final RelationshipType type) {
        for (final Relationship r : from.getRelationships(type)) {
            if (r.getEndNode().getId() == to.getId())
                return r;
        }
        return from.createRelationshipTo(to, type);
    }
    
    public Node newNode(Class c, String id) {        
        Node n = graph.createNode();
        relateOnce(graph.getReferenceNode(), n, getType(c));
        n.setProperty(ID, c.getSimpleName() + "." +id);
        
        //nodeIndex.putIfAbsent(n, ID, id);
        return n;
    }
    
    public Node getNode(String id) {
        return nodeIndex.get(ID, id).getSingle();
    }
    
    public KB(String location) {
        graph = new GraphDatabaseFactory().newEmbeddedDatabase(location);
    }
    
    public KB() {
        RestGraphDatabase rg;
        graph = rg = new RestGraphDatabase("http://localhost:7474/db/data");        
        
        
        if (graph.index()!=null) {
            nodeIndex = graph.index().forNodes( "node_auto_index" );            
        }
        
        //        Node x = n.createNode();
        //        x.setProperty("x", "x");
        //        n.getReferenceNode().createRelationshipTo(x, TestNeo4JConnection.RelTypes.NODE );

    }

    public Node getNode(Class c, String id) {
        return getNode(c, id, null);        
        //return getNode(c.getSimpleName() + "." + id);
    }

    public Node getNode(Class c, String id, Transactable transactable) {

        Node n = getNode(c.getSimpleName() + "." + id);
        if (n != null) {
            if (transactable!=null) {
                Transaction tx = graph.beginTx();
                try {
                    transactable.run(n);
                    tx.success();
                }
                finally {
                    tx.finish();
                }
            }
            return n;
        }
        
        Transaction tx = graph.beginTx();
        try
        {
            n = newNode(c, id);
            
            if (transactable!=null)
                transactable.run(n);
            
            tx.success();
        }
        finally {
            tx.finish();
        }
        
        return n;
    }

    public void shutdown() {
        graph.shutdown();
    }

    
    
}
