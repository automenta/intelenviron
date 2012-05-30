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
public class TestAddRSS {

    static {
        Session.init();
    }
    public static final RssParser rss = new RssParser();
    public static final KB kb = new KB(); //new KB("/home/me/neo4j-enterprise-1.8.M02/data");


    public static void main(String[] args) throws Exception {
        new KBLoader(kb).loadRSS("http://rss.cnn.com/rss/cnn_topstories.rss");
//        KBLoader.loadRSS("https://news.google.com/news/feeds?pz=1&cf=all&ned=us&hl=en&q=Nuclear&output=rss");
        //new KBLoader(kb).addTimeline(kb, "marianasoffer");
        
        new KBLoader(kb).addTimeline(kb, "automenta");
        new KBLoader(kb).addTimeline(kb, "enformable");
        
    }
}
