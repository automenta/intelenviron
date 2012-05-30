/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron.neo4j;

import intelenviron.Calais;
import intelenviron.KB;
import intelenviron.Session;
import java.util.Date;
import java.util.List;
import mx.bigdata.jcalais.CalaisResponse;
import org.horrabin.horrorss.RssChannelBean;
import org.horrabin.horrorss.RssFeed;
import org.horrabin.horrorss.RssImageBean;
import org.horrabin.horrorss.RssItemBean;
import org.horrabin.horrorss.RssParser;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import twitter4j.*;

/**
 *
 * @author me
 */
public class KBLoader {
    static {
        Session.init();
    }

    final static Twitter twitter = TwitterFactory.getSingleton();
    final static KB kb = new KB(); //new KB("/home/me/neo4j-enterprise-1.8.M02/data");
    public static final Calais calais = new Calais();
    public static final RssParser rss = new RssParser();

    final public static RelationshipType AUTHOR = kb.getType("authors");
    final public static RelationshipType MENTIONS = kb.getType("mentions");
    
    
    
    public interface Transactable {
        public void run(Node n);
    }
    
    public static void addTrend(KB kb, final Trend t, final Date d, final Location loc) {
        Node n = kb.getNode(Trend.class, t.getName(), new Transactable() {

            @Override
            public void run(Node n) {
                n.setProperty("when", d.getTime());
                if (loc!=null) {
                    if (loc.getName()!=null)
                        n.setProperty("where", loc.getName());
                    else if (loc.getPlaceName()!=null)
                        n.setProperty("where", loc.getPlaceName());
                }
            }
            
        });
    }
    public static Node getUser(KB kb, final Tweet author) {        
        return getUser(kb, author.getFromUserId(), author.getFromUser(), null, null);
    }
    
    public static Node getUser(KB kb, final User u) {
        return getUser(kb, u.getId(), u.getName(), u.getDescription(), u.getLocation());
    }
    
    public static Node getUser(KB kb, long id, final String name, final String description, final String location) {
        Node us = kb.getNode(User.class, Long.toString(id), new Transactable() {

            @Override
            public void run(Node n) {
                n.setProperty("name", name);
                if (description!=null)
                    n.setProperty("description", description);
                if (location!=null)
                    n.setProperty("location", location);
            }
            
        });
        return us;
        
    }
    public static class Tag {
        
    }
    public static class Media {
    
    }
    public static class Document {
    
    }
    
    public static Node getTag(KB kb, String tag) {
        return kb.getNode(Tag.class, tag.toLowerCase());
    }
    public static Node getMedia(KB kb, String url) {
        return kb.getNode(Media.class, url);
    }
    
    public static void addStatus(final KB kb, final Status s) {
        
        final Node author = getUser(kb, s.getUser());
        
        Node n = kb.getNode(Status.class, Long.toString(s.getId()), new Transactable() {

            @Override
            public void run(Node n) {
                n.setProperty("when", s.getCreatedAt().getTime());
                n.setProperty("content", s.getText());
                if (s.getGeoLocation()!=null)
                    n.setProperty("where", s.getGeoLocation().getLatitude()+ "," + s.getGeoLocation().getLongitude());
                for (org.neo4j.graphdb.Relationship r : author.getRelationships( AUTHOR )) {
                    if (r.getEndNode().getId() == n.getId())
                        r.delete();
                }
                kb.relateOnce(author, n, AUTHOR);
                
                calais.apply(kb, n, s.getText());
                
            }
            
        });
        
        
    }
    public static void addTweet(final KB kb, final Tweet s) {
        
        final Node author = getUser(kb, s);
        
        Node n = kb.getNode(Status.class, Long.toString(s.getId()), new Transactable() {

            @Override
            public void run(Node n) {
                n.setProperty("when", s.getCreatedAt().getTime());
                n.setProperty("content", s.getText());
                if (s.getGeoLocation()!=null)
                    n.setProperty("where", s.getGeoLocation().getLatitude()+ "," + s.getGeoLocation().getLongitude());
                for (org.neo4j.graphdb.Relationship r : author.getRelationships( AUTHOR )) {
                    if (r.getEndNode().getId() == n.getId())
                        r.delete();
                }
                kb.relateOnce(author, n, AUTHOR);

                if (s.getUserMentionEntities()!=null)
                    for (UserMentionEntity u : s.getUserMentionEntities()) {
                        final Node mentioned = getUser(kb, u.getId(), u.getScreenName(), u.getName(), null);
                        kb.relateOnce(n, mentioned, MENTIONS);
                    }
                if (s.getHashtagEntities()!=null)
                    for (HashtagEntity h : s.getHashtagEntities()) {
                        final Node hashtag = getTag(kb, h.getText());
                        kb.relateOnce(n, hashtag, MENTIONS);
                    }
                if (s.getMediaEntities()!=null)
                    for (MediaEntity me : s.getMediaEntities()) {
                        final Node media = getMedia(kb, me.getMediaURL().toString());
                        kb.relateOnce(n, media, MENTIONS);
                    }

                calais.apply(kb, n, s.getText());
            }
            
        });
        
        
    }
    
    public static void addTimeline(KB kb, String username) throws TwitterException {
        ResponseList<Status> tt = twitter.getUserTimeline(username);
        for (int i = 0; i < tt.size(); i++) {
            Status s = tt.get(i);
            addStatus(kb, s);
        }
    }
    public static void addSearch(KB kb, String query) throws TwitterException {
        QueryResult tt = twitter.search(new Query(query));
        for (Tweet t : tt.getTweets()) {
            addTweet(kb, t);
        }
    }

    public static void loadRSS(String rssURL) {
        try {

            final RssFeed feed = rss.load(rssURL);

            // Gets the channel information of the feed and 
            // display its title
            RssChannelBean channel = feed.getChannel();
            System.out.println("Feed Title: " + channel.getTitle());

            // Gets the image of the feed and display the image URL
            RssImageBean image = feed.getImage();
            System.out.println("Feed Image: " + image.getUrl());

            Node n = kb.getNode(RssFeed.class, feed.getChannel().getLink(), new KBLoader.Transactable() {

                @Override
                public void run(Node n) {
                    n.setProperty("name", feed.getChannel().getTitle());
                    n.setProperty("content", feed.getChannel().getDescription());
                }
            });
            // Gets and iterate the items of the feed 
            List<RssItemBean> items = feed.getItems();
            for (int i = 0; i < items.size(); i++) {
                final RssItemBean item = items.get(i);
                //System.out.println("Title: " + item.getTitle());
                //System.out.println("Link : " + item.getLink());
                //System.out.println("Desc.: " + item.getDescription());
                final String cleanText = Jsoup.clean(item.getDescription(), Whitelist.simpleText());

                final CalaisResponse cr = calais.analyze(cleanText);


                Node x = kb.getNode(Document.class, item.getLink(), new KBLoader.Transactable() {

                    @Override
                    public void run(Node n) {
                        n.setProperty("name", item.getTitle());
                        n.setProperty("url", item.getLink());
                        n.setProperty("content", item.getDescription());
                        n.setProperty("content.text", cleanText);
                        n.setProperty("when", item.getPubDate().getTime());
                        n.setProperty("category", item.getCategory());
                        n.setProperty("author", item.getAuthor());

                        calais.apply(kb, n, cr);

                    }
                });
                kb.relateOnce(n, x, KBLoader.AUTHOR);
            }

        } catch (Exception e) {
            // Something to do if an exception occurs
        }

    }
    
    public static void main(String[] args) throws Exception {
        
//        ResponseList<Trends> tr = twitter.getDailyTrends();
//        for (int i = 0; i < tr.size(); i++) {
//            Trends tt = tr.get(i);
//            for (Trend ttt : tt.getTrends()) {
//                addTrend(kb, ttt, tt.getTrendAt(), tt.getLocation());
//            }
//        }
        
        
        //addTimeline(kb, "enformable");
        //addTimeline(kb, "automenta");
        addSearch(kb, "artificial intelligence");
        //System.out.println(kb.graph.index().getNodeAutoIndexer().getAutoIndex());
        
        kb.shutdown();
        
    }
}
