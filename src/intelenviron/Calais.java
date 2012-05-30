/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

import intelenviron.neo4j.KBLoader;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import mx.bigdata.jcalais.CalaisConfig;
import mx.bigdata.jcalais.CalaisObject;
import mx.bigdata.jcalais.CalaisResponse;
import mx.bigdata.jcalais.rest.CalaisRestClient;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 *
 * @author SeH
 */
public class Calais {

    private final CalaisRestClient client;
    private final CalaisConfig config;

    public Calais() {
        this(Session.get("opencalais.key"));
    }

    public Calais(String apiKey) {
        super();

        this.client = new CalaisRestClient(apiKey);

        config = new CalaisConfig();
        config.set(CalaisConfig.ConnParam.CONNECT_TIMEOUT, 10000);
        config.set(CalaisConfig.ConnParam.READ_TIMEOUT, 10000);
        config.set(CalaisConfig.ProcessingParam.CONTENT_TYPE, "text/html");

    }

    public CalaisResponse analyze(URL u) throws IOException {
        return client.analyze(u, config);
    }

    public CalaisResponse analyze(String s) throws IOException {
        return client.analyze(s, config);
    }

    public static void main(String[] args) throws IOException {
        Session.init();

        CalaisResponse x = new Calais().analyze("");
        System.out.println(x.getInfo());
        for (CalaisObject o : x.getEntities()) {
            System.out.println("Entity: " + o);
        }
        for (CalaisObject o : x.getTopics()) {
            System.out.println("Topics: " + o);
        }
        for (CalaisObject o : x.getRelations()) {
            System.out.println("Relations: " + o);
        }
        for (CalaisObject o : x.getSocialTags()) {
            System.out.println("Social Tags: " + o);
        }

    }

    public void apply(final KB kb, final KBLoader kbLoader, Node n, CalaisResponse cr) {
        n.setProperty("calais.info", cr.getInfo());

        for (CalaisObject o : cr.getEntities()) {
            //System.out.println("Entity: " + o);   
            String entityName = o.getField("name");
            Node tag = KBLoader.getTag(kb, entityName);
            Relationship t = kb.relateOnce(n, tag, kbLoader.MENTIONS);
            double relevance = Double.parseDouble(o.getField("relevance"));
            t.setProperty("relevance", relevance);
            t.setProperty("type", o.getField("_type"));

        }
        for (CalaisObject o : cr.getTopics()) {
            String categoryName = o.getField("categoryName");
            Node cat = KBLoader.getTag(kb, categoryName);
            
            Relationship t = kb.relateOnce(n, cat, kbLoader.MENTIONS);
            if (o.getField("score")!=null)
                t.setProperty("relevance", Double.parseDouble(o.getField("score")));
            //System.out.println("Topics: " + o);
        }
        for (CalaisObject o : cr.getRelations()) {
            //System.out.println("Relations: " + o);
        }
        for (CalaisObject o : cr.getSocialTags()) {
            //System.out.println("Social Tags: " + o);     
            String tagName = o.getField("name");
            Node tag = KBLoader.getTag(kb, tagName);
            kb.relateOnce(n, tag, kbLoader.MENTIONS);
        }
    }

    public void apply(KB kb, KBLoader kbLoader, Node n, String text) {
        try {
            apply(kb, kbLoader, n, analyze(text));
        } catch (IOException ex) {
            Logger.getLogger(Calais.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
