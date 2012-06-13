/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron.neo4j;

import intelenviron.KB;
import intelenviron.Session;
import org.horrabin.horrorss.*;

/**
 *
 * @author me
 */
public class LoadSampleData {

    static {
        Session.init();
    }
    public static final RssParser rss = new RssParser();
    public static final KB kb = new KB(); //new KB("/home/me/neo4j-enterprise-1.8.M02/data");


    public static void main(String[] args) throws Exception {
        
        new KBLoader(kb).addTimeline(kb, "automenta");
        new KBLoader(kb).addTimeline(kb, "cstross");
        new KBLoader(kb).addTimeline(kb, "greatdismal");

        new KBLoader(kb).loadRSS("http://rss.cnn.com/rss/cnn_topstories.rss");
        new KBLoader(kb).loadRSS("http://www.sciencedaily.com/rss/computers_math/artificial_intelligence.xml");        
        
        kb.shutdown();
    }
}
