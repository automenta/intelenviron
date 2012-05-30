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
import intelenviron.neo4j.KBNode;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

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
            final String name = x.getProperty("id", id).toString();

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
        get(new Route("/node/:id") {

            @Override
            public Object handle(Request rqst, Response rspns) {
                try {
                    Long id = Long.parseLong(rqst.params(":id"));
                    
                    String nodeData = gson.toJson(
                                NodeSummary.get(kb, id).withNeighbors(kb)
                            );
                    
                    htmlHeader(rspns);
                    StringBuffer commands = new StringBuffer();
                    commands.append("<script type='text/javascript'>");
                    commands.append("$(document).ready(function(){");
                    commands.append("  _n(" + nodeData + "); showNode(0);");
                    commands.append("});");
                    commands.append("</script>");
                    
                    getStaticBinaryFile("cortexit/cortexit.html", rspns.raw().getOutputStream(), commands.toString());
                    
                    return null;
                } catch (IOException ex) {
                    Logger.getLogger(KBWeb.class.getName()).log(Level.SEVERE, null, ex);
                    return ex.toString();
                }
            }            
        });
    }
    
    
}
