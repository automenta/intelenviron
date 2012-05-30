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
//        KBLoader.loadRSS("http://enformable.com/category/foia/feed/");
//        KBLoader.loadRSS("http://enformable.com/feed/");
        KBLoader.loadRSS("https://news.google.com/news/feeds?pz=1&cf=all&ned=us&hl=en&q=Nuclear&output=rss");
//        KBLoader.addTimeline(kb, "enformable");
    }
}
