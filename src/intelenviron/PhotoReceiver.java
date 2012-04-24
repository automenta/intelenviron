/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package intelenviron;

import twitter4j.MediaEntity;
import twitter4j.Tweet;

/**
 *
 * @author me
 */
public class PhotoReceiver extends TwitterUpdater {

    public PhotoReceiver(String hashTag) {
        super(hashTag, 180);
    }

    @Override
    public void onTweet(Tweet t) {
        MediaEntity[] medias = t.getMediaEntities();
        if (medias!=null) {
            if (medias.length > 0) {
                for (MediaEntity me : medias) {
                    System.out.println(t + " has photo " + me);
                }

            }
        }
    }

        
}
