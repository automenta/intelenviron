/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

import java.util.logging.Level;
import java.util.logging.Logger;
import twitter4j.*;

/**
 *
 * @author me
 */
public abstract class TwitterUpdater {
    public final String query;
    private long lastTweet = -1;

    public TwitterUpdater(String query, final int periodSeconds) {
                
        this.query = query;

        new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    
                    update();
                    
                    try {
                        Thread.sleep(periodSeconds * 1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TwitterUpdater.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            }
            
        }).start();
    }
 
    public void update() {
        try {
            Twitter t = TwitterFactory.getSingleton();
            final Query query = new Query(this.query);
            if (lastTweet!=-1)
                query.setSinceId(lastTweet);
            
            QueryResult results = t.search(query);
            
            for (Tweet tw : results.getTweets()) {
                onTweet(tw);
                lastTweet = tw.getId();
            }
        } catch (TwitterException ex) {
            Logger.getLogger(TwitterUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    abstract public void onTweet(Tweet t);
    
}
