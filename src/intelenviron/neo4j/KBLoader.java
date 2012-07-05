/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron.neo4j;

import intelenviron.Calais;
import intelenviron.KB;
import intelenviron.Session;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import mx.bigdata.jcalais.CalaisResponse;
import org.horrabin.horrorss.RssChannelBean;
import org.horrabin.horrorss.RssFeed;
import org.horrabin.horrorss.RssImageBean;
import org.horrabin.horrorss.RssItemBean;
import org.horrabin.horrorss.RssParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
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
    final KB kb;
    final static Twitter twitter = TwitterFactory.getSingleton();
    public static final Calais calais = new Calais();
    public static final RssParser rss = new RssParser();
    final public RelationshipType CREATES;
    final public RelationshipType MENTIONS;
    final public RelationshipType NEXT;

    public KBLoader(KB kb) {
        this.kb = kb;
        CREATES = kb.getType("creates");
        MENTIONS = kb.getType("mentions");
        NEXT = kb.getType("next");
    }

    public Node loadText(final String content) {
        return kb.getNode(Document.class, UUID.randomUUID().toString(), new Transactable() {

            @Override
            public void run(Node n) {
                n.setProperty("when", new Date().getTime());
                n.setProperty("content", content);
            }
            
        });
    }
    
    public static String summarize(String i, int maxLength) {
        final int previewLength = Math.min(maxLength, i.length());
        String x = i.substring(0, previewLength);                
        if (maxLength < i.length())
            return x + "...";                    
        return x;
    }
    
    public Node loadTextSentences(final String content) {
        URL target = null;
        
        final org.jsoup.nodes.Document doc = Jsoup.parse(content);
        final String text = doc.text();
        
        final Node page = kb.getNode(Document.class, UUID.randomUUID().toString(), new Transactable() {

            @Override
            public void run(Node n) {
                n.setProperty("when", new Date().getTime());
                n.setProperty("name", doc.title());
                n.setProperty("content", summarize(text, 256));
            }
            
        });
        
        
        if (target != null) {
//            Elements links = doc.select("a[href]");
//            for (Element e : links) {
//                e.prepend("{{a href=\"" + escape(cortexifyURL(target.getHost(), escape(e.attr("href")))) + "\" target=\"_blank\"}}");
//                e.append("{{/a}}");
//            }
        } else {
            Elements links = doc.select("a[href]");
            for (Element e : links) {
                e.prepend("{{a href=\"" + links.attr("href") + "\" target=\"_blank\"}}");
                e.append("{{/a}}");
            }
        }

        Elements imgs = doc.select("img[src]");
        for (Element e : imgs) {
            e.prepend("{{img src=\"" + e.attr("src") + "\"/}}");
        }
        final String liBreak = "{{br}}";
        for (Element e : doc.select("li")) {
            e.append(liBreak);
        }
        for (Element e : doc.select("tr")) {
            e.append(liBreak);
        }
        for (Element e : doc.select("hr")) {
            e.append(liBreak);
        }
        for (Element e : doc.select("br")) {
            e.append(liBreak);
        }


        String t = doc.text();
        t = t.replace(". ", ".\n");
        t = t.replace("? ", "?\n");
        t = t.replace("! ", "!\n");
        t = t.replace(liBreak, "\n");
        t = t.replace("{{", "<");
        t = t.replace("}}", ">");
        String[] sentences = t.split("\n");
        
        Node prevNode = page;
        for (String s : sentences) {
            s = s.trim();
            if (s.length() < 1) {
                continue;
            }
//            if (s.length() > maxSentenceLength) {
//                //...
//            }
            
//            //TODO proper string escaping.. this is a HACK!
//            frames.add("_f(\"" + frameEscape(s) + "\");");
            final String ss = s;
            final Node previous = prevNode;
            Node x = kb.getNode(Sentence.class, UUID.randomUUID().toString(), new Transactable() {
                @Override
                public void run(Node n) {
                    n.setProperty("content", ss);
                    kb.relateOnce(page, n, CREATES);

                    kb.relateOnce(previous, n, NEXT);

                }
                
            });
            prevNode = x;
            
        }
        return page;
        
    }

    public interface Transactable {

        public void run(Node n);
    }

    public static void addTrend(KB kb, final Trend t, final Date d, final Location loc) {
        Node n = kb.getNode(Trend.class, t.getName(), new Transactable() {

            @Override
            public void run(Node n) {
                n.setProperty("when", d.getTime());
                if (loc != null) {
                    if (loc.getName() != null) {
                        n.setProperty("where", loc.getName());
                    } else if (loc.getPlaceName() != null) {
                        n.setProperty("where", loc.getPlaceName());
                    }
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
                if (description != null) {
                    n.setProperty("description", description);
                }
                if (location != null) {
                    n.setProperty("location", location);
                }
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
    
    public static class Sentence {
    }

    public static class Paragraph {
    }

    public static Node getTag(KB kb, String tag) {
        Node n = kb.getNode(Tag.class, tag.toLowerCase());
        n.setProperty("name", tag);
        return n;
    }

    public static Node getMedia(KB kb, String url) {
        return kb.getNode(Media.class, url);
    }

    public Node addStatus(final KB kb, final Status s, final Node previous) {

        final Node author = getUser(kb, s.getUser());

        Node n = kb.getNode(Status.class, Long.toString(s.getId()), new Transactable() {

            @Override
            public void run(Node n) {
                n.setProperty("when", s.getCreatedAt().getTime());
                n.setProperty("content", s.getText());
                if (s.getGeoLocation() != null) {
                    n.setProperty("where", s.getGeoLocation().getLatitude() + "," + s.getGeoLocation().getLongitude());
                }
                for (org.neo4j.graphdb.Relationship r : author.getRelationships(CREATES)) {
                    if (r.getEndNode().getId() == n.getId()) {
                        r.delete();
                    }
                }
                kb.relateOnce(author, n, CREATES);

                if (previous != null) {
                    kb.relateOnce(previous, n, NEXT);
                }

                calais.apply(kb, KBLoader.this, n, s.getText());

            }
        });

        return n;

    }

    public Node addTweet(final KB kb, final Tweet s) {

        final Node author = getUser(kb, s);

        Node n = kb.getNode(Status.class, Long.toString(s.getId()), new Transactable() {

            @Override
            public void run(Node n) {
                n.setProperty("when", s.getCreatedAt().getTime());
                n.setProperty("content", s.getText());
                n.setProperty("url", "http://twitter.com/" + s.getFromUser() + "/status/" + Long.toString( s.getId() ));
                
                if (s.getGeoLocation() != null) {
                    n.setProperty("where", s.getGeoLocation().getLatitude() + "," + s.getGeoLocation().getLongitude());
                }
                for (org.neo4j.graphdb.Relationship r : author.getRelationships(CREATES)) {
                    if (r.getEndNode().getId() == n.getId()) {
                        r.delete();
                    }
                }
                kb.relateOnce(author, n, CREATES);

                if (s.getUserMentionEntities() != null) {
                    for (UserMentionEntity u : s.getUserMentionEntities()) {
                        final Node mentioned = getUser(kb, u.getId(), u.getScreenName(), u.getName(), null);
                        kb.relateOnce(n, mentioned, MENTIONS);
                    }
                }
                if (s.getHashtagEntities() != null) {
                    for (HashtagEntity h : s.getHashtagEntities()) {
                        final Node hashtag = getTag(kb, h.getText());
                        kb.relateOnce(n, hashtag, MENTIONS);
                    }
                }
                if (s.getMediaEntities() != null) {
                    for (MediaEntity me : s.getMediaEntities()) {
                        final Node media = getMedia(kb, me.getMediaURL().toString());
                        kb.relateOnce(n, media, MENTIONS);
                    }
                }

                calais.apply(kb, KBLoader.this, n, s.getText());
            }
        });

        return n;

    }

    public Node addTimeline(KB kb, String username) throws TwitterException {
        ResponseList<Status> tt = twitter.getUserTimeline(username);
        Node previousStatus = null;
        User user = null;
        for (int i = 0; i < tt.size(); i++) {
            Status s = tt.get(i);
            if (user==null) user = s.getUser();
            previousStatus = addStatus(kb, s, previousStatus);
        }
        return getUser(kb, user);
    }

    public Node addSearch(KB kb, String query) throws TwitterException {
        QueryResult tt = twitter.search(new Query(query));
        
        Node tag = getTag(kb, query);
        
        for (Tweet t : tt.getTweets()) {
            Node n = addTweet(kb, t);
            kb.relateOnce(tag, n, MENTIONS);
        }
        return tag;
    }

    public Node addRSS(String rssURL) throws Exception {

        final RssFeed feed = rss.load(rssURL);

        // Gets the channel information of the feed and 
        // display its title
        RssChannelBean channel = feed.getChannel();
        //System.out.println("Feed Title: " + channel.getTitle());

        // Gets the image of the feed and display the image URL
        RssImageBean image = feed.getImage();
        //System.out.println("Feed Image: " + image.getUrl());

        final Node n = kb.getNode(RssFeed.class, feed.getChannel().getLink(), new KBLoader.Transactable() {

            @Override
            public void run(Node n) {
                n.setProperty("name", feed.getChannel().getTitle());
                n.setProperty("content", feed.getChannel().getDescription());
            }
        });
        // Gets and iterate the items of the feed 
        List<RssItemBean> items = feed.getItems();
        
        Node previous = n;
        for (int i = 0; i < items.size(); i++) {
            final RssItemBean item = items.get(i);

//            kb.threads.submit(new Runnable() {
//
//                @Override
//                public void run() {
                    //System.out.println("Title: " + item.getTitle());
                    //System.out.println("Link : " + item.getLink());
                    //System.out.println("Desc.: " + item.getDescription());
                    final String cleanText = item.getDescription()!=null ? Jsoup.clean(item.getDescription(), Whitelist.simpleText()) : "";
                    

                    CalaisResponse cr = null;
                    
                    if (!cleanText.isEmpty() ) {
                        try {
                            cr = calais.analyze(cleanText);
                        } catch (Exception e) {
                            Calais.logger.severe(e.toString());
                        }
                    }


                    final CalaisResponse crr = cr;

                    System.out.println("adding: " + item);
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

                            if (crr != null) {
                                calais.apply(kb, KBLoader.this, n, crr);
                            }

                        }
                    });
                    kb.relateOnce(n, x, CREATES);
                    kb.relateOnce(previous, x, NEXT);
                    previous = x;
//                }
//            });

        }
        return n;


    }
//    public static void main(String[] args) throws Exception {
//        
////        ResponseList<Trends> tr = twitter.getDailyTrends();
////        for (int i = 0; i < tr.size(); i++) {
////            Trends tt = tr.get(i);
////            for (Trend ttt : tt.getTrends()) {
////                addTrend(kb, ttt, tt.getTrendAt(), tt.getLocation());
////            }
////        }
//        
//        
//        //addTimeline(kb, "enformable");
//        //addTimeline(kb, "automenta");
//        addSearch(kb, "artificial intelligence");
//        //System.out.println(kb.graph.index().getNodeAutoIndexer().getAutoIndex());
//        
//        kb.shutdown();
//        
//    }
}
