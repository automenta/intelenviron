/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron.neo4j;

import org.neo4j.graphdb.Node;

/**
 *
 * @author me
 */
public class KBNode {
    private final Node node;

    public KBNode(Node n) {
        this.node = n;
    }
    
    public String getType() {
        String i = node.getProperty("id", "").toString();
        if (i.equals(""))
            return "";
        else if (i.contains("."))
            return i.substring(0, i.indexOf("."));
        else
            return "";
        
    }
    
    
    
    
}
