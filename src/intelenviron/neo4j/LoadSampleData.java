/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron.neo4j;

import intelenviron.KB;
import intelenviron.Session;

/**
 *
 * @author me
 */
public class LoadSampleData {

    static {
        Session.init();
    }
    
    public static final KB kb = new KB(); //new KB("/home/me/neo4j-enterprise-1.8.M02/data");


    public static void main(String[] args) throws Exception {
        
        new KBLoader(kb).addTimeline(kb, "greatdismal");
        new KBLoader(kb).addTimeline(kb, "cstross");
        
        new KBLoader(kb).loadRSS("http://www.scoop.it/t/natural-language-programming/rss.xml");
//        new KBLoader(kb).loadRSS("http://www.scoop.it/t/food-fill/rss.xml");
        new KBLoader(kb).loadRSS("http://www.scoop.it/t/artificial-g-intelligence/rss.xml");
//        new KBLoader(kb).loadRSS("http://www.scoop.it/t/soul-fill/rss.xml");

        new KBLoader(kb).loadRSS("http://rss.cnn.com/rss/cnn_topstories.rss");
        new KBLoader(kb).loadRSS("http://www.sciencedaily.com/rss/computers_math/artificial_intelligence.xml");        
        
        kb.shutdown();
    }
}
