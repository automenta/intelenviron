/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

import com.google.common.base.Predicate;
import com.google.gson.Gson;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import org.neo4j.graphdb.Node;
import spark.Request;
import spark.Response;
import spark.Route;
import static spark.Spark.*;
import static intelenviron.Web.*;
import intelenviron.neo4j.KBLoader;
import intelenviron.neo4j.KBNode;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.rest.graphdb.traversal.RestTraversal;

/**
 *
 * @author me
 */
public class KBWeb {
    private final KB kb;
    private final Gson gson = new Gson();
    
    public KBWeb() {
        this(new KB());
    }

    public static class RelationshipSummary {
        public final long id;
        public final String name;

        public RelationshipSummary(long id, String name) {
            this.id = id;
            this.name = name;
        }
        
    }
    
    
    public static class NodeSummary {
        public final long id;
        public final String name;
        public final Date modified;
        public List<NodeSummary> ins = null;
        public List<NodeSummary> outs = null;
        public RelationshipSummary via = null;
        private transient final Node node;
        private Map<String,String> prop = null;

        public NodeSummary(Node node, long id, String name, Date modified) {
            this.node = node;
            this.id = id;
            this.name = name;
            this.modified = modified;
        }
        
        public static NodeSummary get(Node x, Predicate<Node> include) {
            if (include!=null)
                if (!include.apply(x))
                    return null;
            
            final String id = Long.toString(x.getId());
            final String name = x.getProperty("name", x.getProperty("id", id)).toString();

            Date d = null;
            Object o = x.getProperty("when", null);
            if (o!=null) {
                if (o instanceof Long) {
                    Long datelong = (Long)o;
                    d = new Date(datelong);
                }
                else if (o instanceof String) {
                    String datestring = (String)o;
                    d = new Date(Date.parse(datestring));
                }
            }

            return new NodeSummary(x, x.getId(), name, d );            
        }
        
        public static NodeSummary get(KB kb, long id)  {
            Node n = kb.getNode(id);
            return get(n, null);
        }
                
        public NodeSummary withProperties() {
            prop = new HashMap();
            for (String key : node.getPropertyKeys()) {
                 prop.put(key, node.getProperty(key).toString());
            }
            return this;
        }
        public NodeSummary withNeighbors(KB kb) {
            ins = new LinkedList();
            outs = new LinkedList();
            
            for (Relationship r : node.getRelationships(Direction.INCOMING)) {
                ins.add(NodeSummary.get(kb, r.getStartNode().getId()).via(r));
            }
            
            for (Relationship r : node.getRelationships(Direction.OUTGOING)) {
                outs.add(NodeSummary.get(kb, r.getEndNode().getId()).via(r));
            }
                    
            return this;
        }
        
        public NodeSummary via(Relationship r) {
            this.via = new RelationshipSummary(r.getId(), r.getType().name());
            return this;
        }
    }
    
    
    public Object getNodeSummariesJSON(Request rqst, Response rspns, final Predicate<Node> include) {
        final Iterable<Node> n = kb.getNodes();

        final Collection<NodeSummary> c = new LinkedList();

        for (final Node x : n) {
            c.add(NodeSummary.get(x, include));
        }
        jsonHeader(rspns);
        return gson.toJson(c);

    }
    
    public KBWeb(final KB kb) {
        this.kb = kb;
        get(new Route("/") {

            @Override
            public Object handle(Request rqst, Response rspns) {
                htmlHeader(rspns);
                try {
                    getStaticBinaryFile("index.html", rspns);
                    return null;
                } catch (IOException ex) {
                    Logger.getLogger(KBWeb.class.getName()).log(Level.SEVERE, null, ex);
                    return ex.toString();
                }
                        
            }
            
        });
        
        get(new Route("/nodes/json") {
            @Override public Object handle(Request rqst, Response rspns) {
                return getNodeSummariesJSON(rqst, rspns, null);
            }
        });
        get(new Route("/tags/json") {
            @Override public Object handle(Request rqst, Response rspns) {
                return getNodeSummariesJSON(rqst, rspns, new Predicate<Node>() {
                    @Override
                    public boolean apply(Node t) {
                        final KBNode k = new KBNode(t);
                        return (k.getType().equals("Tag"));
                    }                    
                });
            }
        });
        get(new Route("/node/:id/json") {

            @Override
            public Object handle(Request rqst, Response rspns) {
                Long id = Long.parseLong(rqst.params(":id"));

                String nodeData = gson.toJson(
                            NodeSummary.get(kb, id).withNeighbors(kb).withProperties()
                        );

                jsonHeader(rspns);
                return nodeData;
            }            
        });
        
        getStatic("/add", "add.html");
        
        get(new Route("/node/:id") {

            @Override
            public Object handle(Request rqst, Response rspns) {
                try {
                    Long id = Long.parseLong(rqst.params(":id"));
                    
                    String nodeData = gson.toJson(
                                NodeSummary.get(kb, id).withNeighbors(kb).withProperties()
                            );
                    
                    htmlHeader(rspns);
                    StringBuffer commands = new StringBuffer();
                    commands.append("<script type='text/javascript'>");
                    commands.append("$(document).ready(function(){");
                    commands.append("  _n(" + nodeData + "); showNode(0);");
                    commands.append("});");
                    commands.append("</script>");
                    
                    getStaticBinaryFile("cortexit/cortexit.html", rspns, commands.toString());
                    
                    return null;
                } catch (IOException ex) {
                    Logger.getLogger(KBWeb.class.getName()).log(Level.SEVERE, null, ex);
                    return ex.toString();
                }
            }            
        });
        
        
        get(new Route("/add/rss/*") {

            @Override
            public Object handle(Request rqst, Response rspns) {
                htmlHeader(rspns);
                
                String url = rqst.pathInfo();
                String rssURL = url.substring("/read/rss/".length() );
                

                try {
                    PrintStream ps = new PrintStream(rspns.raw().getOutputStream());
                    try {
                        ps.println("Loading: " + rssURL + "...");
                        //TODO add a callback for live response
                        Node n = new KBLoader(kb).loadRSS(rssURL);
                        ps.println(" <a href='/node/" + n.getId() + "'>Finished</a>. " );
                    } catch (Exception ex) {
                        Logger.getLogger(KBWeb.class.getName()).log(Level.SEVERE, null, ex);
                        ps.println(" Error: " + ex.toString());
                        return ex.toString();
                    }
                    ps.close();
                }
                catch (IOException e) {
                    
                }
                
                return null;
            }
            
        });
        post(new Route("/add/text") {

            @Override
            public Object handle(Request rqst, Response rspns) {
                htmlHeader(rspns);
                
                String content = rqst.queryParams("content");

                return content;

//                try {
//                    PrintStream ps = new PrintStream(rspns.raw().getOutputStream());
//                    try {
//                        ps.println("Loading: " + rssURL + "...");
//                        //TODO add a callback for live response
//                        Node n = new KBLoader(kb).loadRSS(rssURL);
//                        ps.println(" <a href='/node/" + n.getId() + "'>Finished</a>. " );
//                    } catch (Exception ex) {
//                        Logger.getLogger(KBWeb.class.getName()).log(Level.SEVERE, null, ex);
//                        ps.println(" Error: " + ex.toString());
//                        return ex.toString();
//                    }
//                    ps.close();
//                }
//                catch (IOException e) {
//                    
//                }
                
                //return null;
            }
            
        });
        
        get(new Route("/graph/:nodeid") {

            @Override
            public Object handle(Request rqst, Response rspns) {
                try {
                    Long id = Long.parseLong(rqst.params(":nodeid"));
                    htmlHeader(rspns);
                    String commands = "<script type='text/javascript'>thisNode(" + id + ");</script>";
                    
                    getStaticBinaryFile("graphvis.html", rspns, commands);
                    return null;
                } catch (IOException ex) {
                    Logger.getLogger(KBWeb.class.getName()).log(Level.SEVERE, null, ex);
                    return null;
                }
                
            }
            
        });
        
        get(new Route("/graph/:nodeid/js") {
            private Traverser getNeighbors( final Node x )
            {
                return new RestTraversal().breadthFirst().maxDepth(2).traverse(x);
            }
            
            @Override
            public Object handle(Request rqst, Response rspns) {
                String s = "";
                Long id = Long.parseLong(rqst.params(":nodeid"));
                
                Map<Long, Node> neighbors = new HashMap();
                try {
                    Iterator<Node> ii = getNeighbors( kb.getNode(id) ).nodes().iterator();
                    while (ii.hasNext()) {
                        Node xn = ii.next();
                        neighbors.put(xn.getId(), xn);
                    }
                    
                }
                catch (Exception e) {
                    return null;
                }
                        

                Set<Relationship> r = new HashSet();

                //manipulates sigmaInst instance 's' to construct graph
                s += "var initGraph = function(s) {";
                for (Node f : neighbors.values()) {
//                    s.addNode('n'+i,{
//                    'x': Math.random(),
//                    'y': Math.random(),
//                    'size': 0.5+4.5*Math.random(),
//                    'color': cluster['color'],
//                    'cluster': cluster['id']
//                    });

                    double x = Math.random();
                    double y = Math.random();
                    
                    String params = "'x': " + x + "," + "'y': " + y + "," + 
                            "'size': 0.5+4.5*Math.random()";
                    
                    s += "s.addNode('n" + f.getId() + "', {" + params + "});";
                    for (Relationship rr : f.getRelationships()) {
                        r.add(rr);
                    }
                }
                for (Relationship rr : r) {
                    long start = rr.getStartNode().getId();
                    long end = rr.getEndNode().getId();
                    if (neighbors.containsKey(start) && neighbors.containsKey(end)) {
                        String eID = start + "-" + end;
                        s += "s.addEdge('" + eID + "','n" + start + "','n" + end + "');";
                    }
                }
                
                s += "}";
                
                jsHeader(rspns);
                return s;
            }            
        });
    }
    
    
}
